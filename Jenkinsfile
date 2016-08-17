node {
    echo 'Hello from Pipeline'
    sh '''wget "https://www.dropbox.com/s/znvdammow4jiogj/NEON.zip?dl=1" -O tmp.zip'''
    sh '''unzip tmp.zip'''
    sh '''cat meta.xml'''
    echo 'Goodbye from pipeline'
}
