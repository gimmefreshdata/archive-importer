node {
    stage 'notify subscribers'
    dateString = sh([script: 'date --date \'1 week ago\' +%Y-%m-%d', returnStdout: true]).trim()
    sh "curl --verbose \"http://api.effechecka.org/notifyAll?addedAfter=$dateString\""
    
    stage 'cleanup dead links'
    brokenLinks = sh([script: "find /mnt/data/repository/gbif-idigbio.parquet -xtype l", returnStdout: true]).split('\n')

    echo "found [${brokenLinks.size()}] broken links"
    brokenLinks.each {
      brokenLink -> if (brokenLink.size() > 0) { sh "rm ${brokenLink.trim}" }
    }
}
