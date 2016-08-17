def importIfChanged(archiveUrl) {
    stage 'download'
        sh "wget --quiet \"${archiveUrl}\" -O tmp.zip"

    stage 'changed'
        sh "sha1sum tmp.zip > new.sha1"
        def oldSha1 = fileExists('old.sha1') ? readFile('old.sha1') : ''
        def newSha1 = readFile 'new.sha1'
        if (oldSha1 == newSha1) {
            echo 'same file, nothing to do'
        } else {
            stage 'unpack'
                sh "rm -rf dwca"
                sh "unzip tmp.zip -d dwca"

            stage 'verify'
                if (!fileExists('dwca/meta.xml')) {
                    error("failed to find file [meta.xml] in ${archiveUrl}")
                }

            stage 'archive'
                archive 'dwca/*'
                echo "home: [${env.JENKINS_HOME}]"
                echo "build number: [${env.BUILD_NUMBER}]"
                echo "job name: [${env.JOB_NAME}]"
                sh "mv new.sha1 old.sha1"
        }
}

this
