/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.1.1"
}

import de.undercouch.gradle.tasks.download.Download

/**
 * Download multiple files to a directory
 */
tasks.register<Download>("downloadFiles") {
    src(listOf(
        "https://repo.maven.apache.org/maven2/org/citationstyles/styles/1.0/styles-1.0.jar",
        "https://repo.maven.apache.org/maven2/org/eclipse/jetty/jetty-server/9.1.3.v20140225/jetty-server-9.1.3.v20140225-javadoc.jar"
    ))
    dest(buildDir)
    overwrite(true)
}

defaultTasks("downloadFiles")
