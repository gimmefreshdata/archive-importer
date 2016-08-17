node {
    stage 'configure'
        def archiveUrl = 'http://www.inaturalist.org/observations/gbif-observations-dwca.zip'

    stage 'import'
        sh "wget --quiet https://raw.githubusercontent.com/gimmefreshdata/archive-importer/master/archives.groovy"
        archives = load 'archives.groovy'
        archives.importIfChanged(archiveUrl)
}
