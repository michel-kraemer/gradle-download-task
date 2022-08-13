/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.1.1"
}

import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify

/**
 * Download a single file to a directory
 */
tasks.register<Download>("downloadFile") {
    src("http://www.example.com/index.html")
    dest(buildDir)
}

tasks.register<Verify>("verifyFile") {
    dependsOn("downloadFile")
    src(File(buildDir, "index.html"))
    algorithm("MD5")
    checksum("84238dfc8092e5d9c0dac8ef93371a07")
}

defaultTasks("verifyFile")
