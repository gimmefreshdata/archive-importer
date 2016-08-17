node {
    stage 'checkout'
        checkout scm

    stage 'configure'
        def matcher = readFile('README.md') =~ /url:(.*)$/
        if (!matcher.matches()) {
            error('no archive url specified in README.md')
        }
        def archiveUrl = matcher[0][1]

    stage 'download'
        sh "wget --quiet \"${archiveUrl}\" -O tmp.zip"
        sh "unzip tmp.zip -d dwca"

    stage 'verify'
        fileExists 'dwca/meta.xml'

    stage 'archive'
        archive 'dwca/*'

}
