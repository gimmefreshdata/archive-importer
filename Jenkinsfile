node {
    stage 'configure'
        def archiveUrl = 'https://www.dropbox.com/s/znvdammow4jiogj/NEON.zip?dl=1'

    stage 'download'
        sh "wget --quiet \"${archiveUrl}\" -O tmp.zip"

    stage 'changed'
        sh "sha1sum tmp.zip > new.sha1"
        def oldSha1 = fileExists('old.sha1') ? readFile 'old.sha1' : ''
        def newSha1 = readFile 'new.sha1'
        if (oldSha1 == newSha1) {
            echo 'same file, nothing to do'
        } else {
            stage 'unpack'
                sh "unzip tmp.zip -d dwca"

            stage 'verify'
                if (!fileExists 'dwca/meta.xml') {
                    error("failed to find file [meta.xml] in ${archiveUrl}")
                }

            stage 'archive'
                archive 'dwca/*'
                sh "mv new.sha1 old.sha1"
        }


}
