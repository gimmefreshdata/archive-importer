node {
    stage 'cleanup broken links'
    brokenLinks = sh([script: "find /mnt/data/repository/gbif-idigbio.parquet -xtype l", returnStdout: true]).split('\n')

    for (brokenLink in brokenLinks) {
      if (brokenLink.size() > 0) { echo "rm ${brokenLink}" }
    }
}
