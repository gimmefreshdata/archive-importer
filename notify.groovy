node {
    stage 'notify subscribers'
    dateString = sh([script: 'date --date \'1 week ago\' +%Y-%m-%d', returnStdout: true]).trim()
    sh "curl --verbose \"http://api.effechecka.org/notifyAll?addedAfter=$dateString\""
    
    stage 'cleanup dead links'
    sh "find /mnt/data/repository/gbif-idigbio.parquet -xtype l | xargs rm"
}