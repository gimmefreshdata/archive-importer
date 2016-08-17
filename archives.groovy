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
}

def doImport(archiveUrl) {
    stage 'unpack'
        sh "rm -rf dwca"
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
            conversionComplete(submissionId)
        }

        if (conversionSuccess(submissionId)) {
            stage 'verify parquet'
                parquetDir = sh "ls -1 dwca | grep .*\\.parquet"
                if (!fileExists("dwca/${parquetDir}/_SUCCESS")) {
                    error("failed to find parquet files at [dwca/${parquetDir}]: did the conversion succeed?")
                }
            stage 'archive'
                archive 'dwca/**'
            stage 'link'
                dateString = new Date().format('YYYYMMdd')
                jobName = env.JOB_NAME
                symlinkName = "/mnt/data/repository/gbif-idigbio.parquet/source\\=${jobName}/date\\=${dateString}"
                archiveDir = "file:///mnt/data/jenkins/jobs/${env.JOB_NAME}/builds/${env.JOB_NUMBER}/archive/dwca/"
                parquetPath = "${archiveDir}${parquetDir}"
                echo "should link to parquet file ${parquetPath} to ${symlinkName}"
                sh "ln -s ${symlinkName} ${parquetPath}"
            stage 'notify'
                sh "wget http://${getHost()}/updateAll"
        } else {
            error("conversion to parquet failed for submission [${submissionId}]")
        }
}

def requestConversion() {
  sparkRequest = '''curl -X POST http://@@HOST@@:7077/v1/submissions/create --header "Content-Type:application/json;charset=UTF-8" --data '{
  "action" : "CreateSubmissionRequest",
  "appArgs" : [ "file:///mnt/data/jenkins/workspace/@@JOB_NAME@@/dwca/meta.xml" ],
  "appResource" : "file:///home/int/jobs/iDigBio-LD-assembly-1.5.1.jar",
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
    submissionResponse = sh sparkRequest.replace("@@JOB_NAME@@", env.JOB_NAME).replace("@@HOST@@", getHost())
    def submissionIdMatch = submissionResponse =~ 'submissionId"\\s+:\\s+"(.+)"'
    if (!submissionIdMatch) {
        error("submission failed: [${submissionReponse}])")
    }
    submissionIdMatch[0][1]
}

def conversionComplete(submissionId) {
    try {
        status = submissionStatus(submissionId)
        def driverStatusMatch = status =~ 'driverState"\\s+:\\s+"(FINISHED)"'
        echo "checking status ${status}"
        driverStatusMatch ? true : false
    } catch (err) {
        echo "failure in parquet conversion: [${err}]"
        false
    }
}


def getHost() {
    "api.effechecka.org"
}

def submissionStatus(submissionId) {
    sh "curl http://${getHost()}:7077/v1/submissions/status/${submissionId}"
}

def conversionSuccess(submissionId) {
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
