node {
    stage 'checkout'
        checkout scm

    stage 'configure'
        def matcher = readFile('README.md') =~ "url:\\s+(http:.*)\\s+"
        def archiveUrl = matcher ? matcher[0][1] : null
        if (!archiveUrl) {
            error('no archive url specified in README.md')
        }

    stage 'download'
        sh "wget --quiet \"${archiveUrl}\" -O tmp.zip"
        sh "unzip tmp.zip -d dwca"

    stage 'verify'
        fileExists 'dwca/meta.xml'

    stage 'archive'
        archive 'dwca/*'

}
