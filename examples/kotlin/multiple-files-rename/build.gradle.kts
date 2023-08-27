/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.5.0"
}

import de.undercouch.gradle.tasks.download.Download

/**
 * Define files to download and destination file names
 */
val urls by extra(mapOf(
    "http://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=HEAD" to "config.guess",
    "http://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub;hb=HEAD" to "config.sub"
))

tasks.register<Download>("downloadFiles") {
    src(urls.keys)
    dest(layout.buildDirectory)
    eachFile {
        name = urls[sourceURL.toString()]
    }
}

defaultTasks("downloadFiles")
