/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.4.0"
}

import de.undercouch.gradle.tasks.download.Download

/**
 * The following two tasks download a ZIP file and extract its
 * contents to the build directory
 */
val downloadZipFile by tasks.creating(Download::class) {
    src("https://github.com/michel-kraemer/gradle-download-task/archive/1.0.zip")
    dest(layout.buildDirectory.file("1.0.zip"))
}

tasks.register<Copy>("downloadAndUnzipFile") {
    dependsOn(downloadZipFile)
    from(zipTree(downloadZipFile.dest))
    into(layout.buildDirectory)
}

defaultTasks("downloadAndUnzipFile")
