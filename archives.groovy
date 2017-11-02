def importIfChanged(archiveUrl) {

    echo "home: [${env.JENKINS_HOME}]"
    echo "build number: [${env.BUILD_NUMBER}]"
    echo "job name: [${env.JOB_NAME}]"

    stage 'download'
        sh "wget --quiet \"${archiveUrl}\" -O tmp.zip"

    stage 'changed'
        sh "sha1sum tmp.zip > new.sha1"
        def oldSha1 = fileExists('old.sha1') ? readFile('old.sha1') : ''
        def newSha1 = readFile 'new.sha1'
        if (oldSha1 == newSha1) {
            echo 'same file, nothing to do'
        } else {
            if (doImport(archiveUrl)) {
                sh "mv new.sha1 old.sha1"
            }

        }

    stage 'clean'
      sh "rm -rf tmp.zip"
      removeUnpackedArchive()
}

def removeUnpackedArchive() {
  sh "rm -rf dwca"
}

def doImport(archiveUrl) {
    stage 'unpack'
        removeUnpackedArchive()
        sh "unzip tmp.zip -d dwca"

    stage 'verify'
        def metaFilename = 'dwca/meta.xml'
        if (!fileExists(metaFilename)) {
            error("failed to find file [meta.xml] in ${archiveUrl}")
        }

    stage 'dwc2parquet'
        submissionId = requestConversion()
        waitUntil {
            echo 'checking status...'
            submissionComplete(submissionId)
        }

        if (submissionSuccess(submissionId)) {
            stage 'verify parquet'
                parquetDir = sh([script: "ls -1 dwca | grep .*\\.parquet", returnStdout: true]).trim()
                parquetSuccessfile = "dwca/${parquetDir}/_SUCCESS"
                if (!fileExists(parquetSuccessfile)) {
                    error("failed to find parquet success file at [${parquetSuccessfile}]: did the conversion succeed?")
                }
            stage 'archive'
                archive 'dwca/*.parquet/*'
            stage 'link'
                jobName = env.JOB_NAME
                sourceDir = "/mnt/data/repository/gbif-idigbio.parquet/source\\=${jobName}"
                sh "mkdir -p ${sourceDir}"
                
                dateString = sh([script: 'date +%Y%m%d', returnStdout: true]).trim()
                symlinkName = "${sourceDir}/date\\=${dateString}"
                archiveDir = "/mnt/data/jenkins/jobs/${env.JOB_NAME}/builds/${env.BUILD_NUMBER}/archive/dwca/"
                parquetPath = "${archiveDir}${parquetDir}"
                echo "should link to parquet file ${parquetPath} to ${symlinkName}"
                sh "ln -sFf ${parquetPath} ${symlinkName}"
        } else {
            error("conversion to parquet failed for submission [${submissionId}]")
        }
}

def updateMonitors() {
	submissionId = requestUpdate()
        waitUntil {
            echo 'waiting to complete...'
            submissionComplete(submissionId)
        }

	if (submissionSuccess(submissionId)) {
	  	echo 'succeeded to update monitors'
	} else {
		error 'failed to update monitors'
	}
}

def requestUpdate() {
  sparkRequest = '''curl -X POST http://@@HOST@@:7077/v1/submissions/create --header "Content-Type:application/json;charset=UTF-8" --data '{
"action" : "CreateSubmissionRequest",
  "appArgs" : [ "-f", "hdfs","-o", "hdfs:///guoda/data/monitor/","-c","\"hdfs:///guoda/data/gbif-idigbio.parquet\"","-t","\"hdfs:///guoda/data/traitbank/*.csv\"", "-a", "true" ],
  "appResource" : "https://s3-us-west-2.amazonaws.com/guoda/idigbio-spark/iDigBio-LD-assembly-@@VERSION@@.jar",
  "clientSparkVersion" : "1.6.1",
  "environmentVariables" : {
    "SPARK_ENV_LOADED" : "1"
  },
  "mainClass" : "OccurrenceCollectionGenerator",
  "sparkProperties" : {
    "spark.driver.supervise" : "false",
    "spark.app.name" : "updateAll",
    "spark.eventLog.enabled": "true",
    "spark.submit.deployMode" : "cluster",
    "spark.master" : "mesos://api.effechecka.org:7077",
    "spark.executor.memory" : "32g",
    "spark.driver.memory" : "8g",
    "spark.task.maxFailures" : 1  
  }
}'
'''
    request = sparkRequest.replace("@@HOST@@", getHost()).replace("@@VERSION@@", "1.5.8")
    submitRequest(request)
}

def submitRequest(request) {
    echo "submitting request ${request}" 
    submissionResponse = sh([script: request, returnStdout: true])
    // for some reason, submissionResponse is not defined if too many jobs are running and request is denied
    // see https://github.com/gimmefreshdata/source-idigbio/issues/3
    def submissionIdMatch = submissionResponse =~ 'submissionId"\\s+:\\s+"(.+)"'
    if (!submissionIdMatch) {
        error("submission failed: [${submissionReponse}])")
    }
    submissionIdMatch[0][1]
}

def requestConversion() {
  sparkRequest = '''curl --verbose -X POST http://@@HOST@@:7077/v1/submissions/create --header "Content-Type:application/json;charset=UTF-8" --data '{
  "action" : "CreateSubmissionRequest",
  "appArgs" : [ "file:///mnt/data/jenkins/workspace/@@JOB_NAME@@/dwca/meta.xml" ],
  "appResource" : "file:///home/int/jobs/iDigBio-LD-assembly-@@VERSION@@.jar",
  "clientSparkVersion" : "1.6.1",
  "environmentVariables" : {
    "SPARK_ENV_LOADED" : "1"
  },
  "mainClass" : "DarwinCoreToParquet",
  "sparkProperties" : {
    "spark.driver.supervise" : "false",
    "spark.app.name" : "dwc2parquet",
    "spark.eventLog.enabled": "true",
    "spark.submit.deployMode" : "cluster",
    "spark.master" : "mesos://@@HOST@@:7077",
    "spark.executor.memory" : "20g",
    "spark.driver.memory" : "6g",
    "spark.task.maxFailures" : 1
  }
}'
'''
    request = sparkRequest.replace("@@JOB_NAME@@", env.JOB_NAME).replace("@@HOST@@", getHost()).replace("@@VERSION@@", "latest")
    submitRequest(request)
}

def submissionComplete(submissionId) {
    try {
        status = submissionStatus(submissionId)
        def driverStatusMatch = status =~ 'driverState"\\s+:\\s+"((QUEUED)|(RUNNING))"'
        echo "checking status ${status}"
        driverStatusMatch ? false : true
    } catch (err) {
        echo "failure in parquet conversion: [${err}]"
        false
    }
}


def getHost() {
    "mesos07.acis.ufl.edu"
}

def submissionStatus(submissionId) {
    sh([script: "curl --silent http://${getHost()}:7077/v1/submissions/status/${submissionId}", returnStdout: true])
}

def submissionSuccess(submissionId) {
    try {
        status = submissionStatus(submissionId)
        def taskFinishedMatcher = status =~ '.*(TASK_FINISHED).*'
        taskFinishedMatcher ? true : false
    } catch (err) {
        echo "failure in parquet conversion: [${err}]"
        return false
    }
}

this
