node {
    stage 'update'
        sh "wget --no-cache --quiet https://raw.githubusercontent.com/gimmefreshdata/archive-importer/master/archives.groovy -O archives.groovy"
        archives = load 'archives.groovy'
        archives.updateMonitors(archiveUrl)
}
