/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.0.1"
}

/**
 * Download a single file to a directory using a mirror server
 */
tasks.register("downloadFile") {
    doLast {
        val mirrors = mutableListOf(
            "https://repo.maven-non-existing.org/maven2/org/citationstyles/styles/1.0/styles-1.0.jar",
            "https://repo.maven.apache.org/maven2/org/citationstyles/styles/1.0/styles-1.0.jar",
            "https://repo.maven-non-existing2.org/maven2/org/citationstyles/styles/1.0/styles-1.0.jar"
        )
        while (true) {
            val mirror = mirrors.removeFirst()
            try {
                download.run {
                    src(mirror)
                    dest(buildDir)
                    overwrite(true)
                }
                break
            } catch (e: Exception) {
                if (mirrors.isEmpty()) {
                    throw e
                }
                logger.warn("Could not download file. Trying next mirror.")
            }
        }
    }
}

defaultTasks("downloadFile")
