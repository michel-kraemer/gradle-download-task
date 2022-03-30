/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.0.4"
}

import de.undercouch.gradle.tasks.download.Download

/**
 * Define files to download and destination file names
 */
val src by extra(mapOf(
    "http://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=HEAD" to "config.guess",
    "http://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub;hb=HEAD" to "config.sub"
))

/**
 * ALTERNATIVE 1: call the download extension in a for loop
 */
tasks.register("downloadMultipleFiles1") {
    doLast {
        for (s in src) {
            download.run {
                src(s.key)
                dest(File("$buildDir/alternative1", s.value))
            }
        }
    }
}

/**
 * ALTERNATIVE 2: create multiple tasks
 */
val downloadMultipleFiles2 by tasks.creating

for ((i, s) in src.entries.withIndex()) {
    tasks.register<Download>("downloadMultipleFiles2_${i}") {
        src(s.key)
        dest(File("$buildDir/alternative2", s.value))
    }
    downloadMultipleFiles2.dependsOn("downloadMultipleFiles2_${i}")
}

defaultTasks("downloadMultipleFiles1", "downloadMultipleFiles2")
