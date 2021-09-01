// Copyright 2015-2019 Michel Kraemer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package de.undercouch.gradle.tasks.download

import de.undercouch.gradle.tasks.download.internal.Helper.tryGetProvider
import de.undercouch.gradle.tasks.download.internal.ProjectApiHelper
import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * Verifies a file's integrity by calculating its checksum.
 * @author Michel Kraemer
 */
class VerifyAction(project: Project?) : VerifySpec {

    /**
     * Creates a new verify action
     * @param project the project to be built
     */
    private val projectApi: ProjectApiHelper = ProjectApiHelper.newInstance(project!!)
    override var src: File? = null
        private set
    override var algorithm: String? = "MD5"
        private set
    override var checksum: String? = null
        private set

    private fun toHex(barr: ByteArray): String {
        val result = StringBuilder()
        for (b in barr)
            result.append(String.format("%02X", b))
        return result.toString()
    }

    /**
     * Starts verifying
     * @throws IOException if the file could not verified
     * @throws NoSuchAlgorithmException if the given algorithm is not available
     */
    @Throws(IOException::class, NoSuchAlgorithmException::class)
    fun execute() {
        requireNotNull(src) { "Please provide a file to verify" }
        requireNotNull(algorithm) { "Please provide the algorithm to use to calculate the checksum" }
        requireNotNull(checksum) { "Please provide a checksum to verify against" }

        // calculate file's checksum
        val md = MessageDigest.getInstance(algorithm)
        var calculatedChecksum: String
        FileInputStream(src!!).use { fis ->
            val buf = ByteArray(1024)
            var read: Int
            while (fis.read(buf).also { read = it } != -1)
                md.update(buf, 0, read)
            calculatedChecksum = toHex(md.digest())
        }

        // verify checksum
        if (!calculatedChecksum.equals(checksum, ignoreCase = true)) {
            val expected = checksum!!.toLowerCase(Locale.getDefault())
            val got = calculatedChecksum.toLowerCase(Locale.getDefault())
            throw GradleException("Invalid checksum for file '${src!!.name}'. Expected $expected but got $got.")
        }
    }

    override fun src(src: Any?) {
        var src = src
        if (src is Closure<*>)
        //lazily evaluate closure
            src = src.call()
        src = src.tryGetProvider()
        if (src is CharSequence)
            src = projectApi.file(src.toString())
        if (src is File)
            this.src = src
        else
            throw IllegalArgumentException("Verification source must either be a CharSequence or a File")
    }

    override fun algorithm(algorithm: String?) {
        this.algorithm = algorithm
    }

    override fun checksum(checksum: String?) {
        this.checksum = checksum
    }
}