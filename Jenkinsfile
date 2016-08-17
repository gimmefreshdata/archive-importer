node {
    stage 'configure'
        def archiveUrl = 'https://www.dropbox.com/s/znvdammow4jiogj/NEON.zip?dl=1'

    stage 'download'
        sh "wget --quiet \"${archiveUrl}\" -O tmp.zip"
        sh "unzip tmp.zip -d dwca"

    stage 'verify'
        fileExists 'dwca/meta.xml'

    stage 'archive'
        archive 'dwca/*'
}
