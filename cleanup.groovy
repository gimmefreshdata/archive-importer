node {
    stage 'cleanup broken links'
    sh([script: "find /mnt/data/repository/gbif-idigbio.parquet -xtype l | xargs rm -rf", returnStdout: true])
}
