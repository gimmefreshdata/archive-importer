node {
    stage 'update monitors'
        sh "rm -f archives.groovy"
        sh "wget -N --no-cache --quiet https://raw.githubusercontent.com/gimmefreshdata/archive-importer/master/archives.groovy -O archives.groovy"
        archives = load 'archives.groovy'
        archives.updateMonitors()
}
