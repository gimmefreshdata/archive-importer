node {
    stage 'checkout'
        checkout scm

    stage 'configure'
        def archiveUrl = readFile('archiveName.txt')
        if (!archiveUrl) {
            error('no archive url specified in [archiveName.txt]')
        }

    stage 'download'
        sh "wget --quiet \"${archiveUrl}\" -O tmp.zip"
        sh "unzip tmp.zip -d dwca"

    stage 'verify'
        fileExists 'dwca/meta.xml'

    stage 'archive'
        archive 'dwca/*'

}
