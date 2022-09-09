/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.1.3"
}

import de.undercouch.gradle.tasks.download.Download

/**
 * Download a file and send a custom header
 */
tasks.register<Download>("downloadFile") {
    src("https://repo.maven.apache.org/maven2/org/citationstyles/styles/1.0/styles-1.0.jar")
    dest(buildDir)
    header("User-Agent", "gradle-download-task")
}

defaultTasks("downloadFile")
