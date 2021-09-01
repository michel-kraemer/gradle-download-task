// Copyright 2015 Michel Kraemer
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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.security.NoSuchAlgorithmException

/**
 * Verifies a file's integrity by calculating its checksum.
 * <pre>
 * task verifyFile(type: Verify) {
 * file new File(buildDir, "myfile.txt")
 * algorithm 'MD5'
 * checksum '694B2863621FCDBBBA2777BF329C056C' // expected checksum (hex)
 * }
</pre> *
 * @author Michel Kraemer
 */
class Verify : DefaultTask(), VerifySpec {

    private val action: VerifyAction = VerifyAction(project)

    /**
     * Starts verifying
     * @throws IOException if the file could not be verified
     * @throws NoSuchAlgorithmException if the given algorithm is not available
     */
    @TaskAction
    @Throws(IOException::class, NoSuchAlgorithmException::class)
    fun verify() = action.execute()

    override fun src(src: Any?) = action.src(src)

    override fun algorithm(algorithm: String?) = action.algorithm(algorithm)

    override fun checksum(checksum: String?) = action.checksum(checksum)

    @get:InputFile
    override val src: File?
        get() = action.src

    @get:Optional
    @get:Input
    override val algorithm: String?
        get() = action.algorithm

    @get:Input
    override val checksum: String?
        get() = action.checksum
}