package de.undercouch.gradle.tasks.download.internal

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion
import java.io.File

/**
 * Helper methods
 * @author Michel Kraemer
 */
object Helper {
    /**
     * Check if the given object is a [Directory]
     * @receiver the object
     * @return `true` if `obj` is a [Directory]
     */
    val Any?.isDirectory: Boolean
        get() = GradleVersion.current() > GradleVersion.version("4.1") && this is Directory

    /**
     * Convert the given [Directory] object to a [File]
     * @receiver the [Directory]
     * @return the [File]
     */
    fun Any.getFileFromDirectory(): File = (this as Directory).asFile

    /**
     * If the given object is a [Provider], get the provider's value and
     * return it. Otherwise, just return the object.
     * @param obj the object
     * @return the provider's value or the object
     */
    fun Any?.tryGetProvider(): Any? = when {
        this == null -> null
        // Provider class is only available in Gradle 4.0 or higher
        GradleVersion.current() > GradleVersion.version("4.0") && this is Provider<*> -> this.orNull
        else -> this
    }

    /**
     * Check if the given object is a [RegularFile]
     * @receiver the object
     * @return `true` if `obj` is a [RegularFile]
     */
    val Any?.isRegularFile: Boolean
        get() = GradleVersion.current() > GradleVersion.version("4.1") && this is RegularFile

    /**
     * Convert the given [RegularFile] object to a [File]
     * @receiver the [RegularFile] object
     * @return the [File]
     */
    fun Any?.getFileFromRegularFile(): File = (this as RegularFile).asFile
}

typealias ms = Int