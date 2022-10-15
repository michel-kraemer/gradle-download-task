/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.3.0"
}

import de.undercouch.gradle.tasks.download.Download

/**
 * Download a single file using a temporary name. Rename it afterwards.
 */
tasks.register<Download>("downloadFile") {
    src("https://repo.maven.apache.org/maven2/org/citationstyles/styles/1.0/styles-1.0.jar")
    dest(File(buildDir, "styles-1.0.jar"))
    overwrite(true)
    tempAndMove(true)
}

defaultTasks("downloadFile")
