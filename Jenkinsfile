node {
    echo 'Hello from Pipeline'
    checkout scm
    sh '''ls'''
    def matcher = readFile('README.md') =~ 'url:(.*)$'
    def archiveUrl = matcher ? matcher[0][1] : null
    if (!archiveUrl) {
        error('no archive url specified in README.md')
    }
    sh "wget --quiet \"${archiveUrl}\" -O tmp.zip"
    sh '''unzip tmp.zip'''
    fileExists 'meta.xml'
    echo 'Goodbye from pipeline'
}
