/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.6.0"
}

import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify

/**
 * Download a single file to a directory
 */
val downloadFile by tasks.registering(Download::class) {
    src("http://www.example.com/index.html")
    dest(layout.buildDirectory)
}

val verifyFile by tasks.registering(Verify::class) {
    dependsOn(downloadFile)
    src(downloadFile.get().outputs.files.singleFile)
    algorithm("MD5")
    checksum("84238dfc8092e5d9c0dac8ef93371a07")
}

defaultTasks(verifyFile.get())
