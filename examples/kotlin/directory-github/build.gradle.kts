/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.5.0"
}

/**
 * Download all files from a directory in GitHub. Use the GitHub API to get the
 * directory's contents. Parse the result and download the files.
 */
tasks.register("downloadDirectoryGitHub") {
    doLast {
        // download directory listing via GitHub API
        val dir = "https://api.github.com/repos/michel-kraemer/gradle-download-task/contents/screencast"
        val contentsFile = layout.buildDirectory.file("directory_contents.json").get().asFile
        download.run {
           src(dir)
           dest(contentsFile)
        }

        // parse directory listing
        val contents = groovy.json.JsonSlurper().parse(contentsFile, "utf-8") as List<Map<Any, String>>
        val urls = contents.map { it["download_url"] }

        // download files
        download.run {
           src(urls)
           dest(layout.buildDirectory)
        }

        // delete downloaded directory listing
        contentsFile.delete()
    }
}

defaultTasks("downloadDirectoryGitHub")
