/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.0.1"
}

import java.nio.file.Files
import de.undercouch.gradle.tasks.download.Download

val downloadToBuildDir by extra(true)

fun getMavenCentralUrl(): String {
    return "https://repo.maven.apache.org/maven2/"
}

fun getStylesJar(): String {
    return "org/citationstyles/styles/1.0/styles-1.0.jar"
}

/**
 * Download a single file to a directory. Use closures for the src and dest
 * property.
 */
tasks.register<Download>("downloadFile") {
    src {
        val mavenUrl = getMavenCentralUrl()
        val stylesJar = getStylesJar()
        mavenUrl + stylesJar
    }
    dest {
        if (downloadToBuildDir) {
            buildDir
        } else {
            Files.createTempDirectory("gradle-download-task").toFile()
        }
    }
    overwrite(true)
}

defaultTasks("downloadFile")
