node {
    stage 'update monitors'
        sh "wget -N --no-cache --quiet https://raw.githubusercontent.com/gimmefreshdata/archive-importer/master/archives.groovy -O archives.groovy"
        archives = load 'archives.groovy'
        archives.updateMonitors()
}
