node {
    stage 'notify subscribers'
    dateString = sh([script: 'date --date \'1 week ago\' +%Y-%m-%d', returnStdout: true]).trim()
    sh "curl --verbose \"http://api.effechecka.org/notifyAll?addedAfter=$dateString\""
    
    stage 'cleanup broken links'
    brokenLinks = sh([script: "find /mnt/data/repository/gbif-idigbio.parquet -xtype l", returnStdout: true]).split('\n')

    for (brokenLink in brokenLinks) {
      if (brokenLink.size() > 0) { sh "rm ${brokenLink.trim()}" }
    }
}
