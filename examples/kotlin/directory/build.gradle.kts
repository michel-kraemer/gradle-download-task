/** 
 * Include Ivy into build script classpath. Only necessary for this example.
 */
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // only necessary for downloadDirectory task below
        classpath("org.apache.ivy:ivy:2.5.0")
    }
}

/**
 * Include the gradle-download-task plugin
 */
plugins {
    id("de.undercouch.download") version "5.1.2"
}

import de.undercouch.gradle.tasks.download.Download

/**
 * Download all files from a directory. Use Ivy's URL lister to
 * read the server's directory listing and then download all files
 * See 'buildscript' instruction above.
 */
tasks.register("downloadDirectory") {
    doLast {
        val dir = "https://repo.maven.apache.org/maven2/de/undercouch/gradle-download-task/4.1.2/"
        val urlLister = org.apache.ivy.util.url.ApacheURLLister()
        val files = urlLister.listFiles(java.net.URL(dir))
        download.run {
           src(files)
           dest(buildDir)
        }
    }
}

defaultTasks("downloadDirectory")
