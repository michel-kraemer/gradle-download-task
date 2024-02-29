/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.6.0"
}

import de.undercouch.gradle.tasks.download.Download

/**
 * Download a file conditionally using its ETag
 */
tasks.register<Download>("downloadFile") {
    src("https://repo.maven.apache.org/maven2/org/citationstyles/styles/1.0/styles-1.0.jar")
    dest(layout.buildDirectory)
    onlyIfModified(true)
    useETag(true)
}

defaultTasks("downloadFile")
