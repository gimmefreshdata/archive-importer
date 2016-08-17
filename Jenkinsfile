node {
    echo 'Hello from Pipeline'
    sh '''cat README.md'''
    sh '''wget --quiet "https://www.dropbox.com/s/znvdammow4jiogj/NEON.zip?dl=1" -O tmp.zip'''
    sh '''unzip tmp.zip'''
    sh '''cat meta.xml'''
    echo 'Goodbye from pipeline'
}
