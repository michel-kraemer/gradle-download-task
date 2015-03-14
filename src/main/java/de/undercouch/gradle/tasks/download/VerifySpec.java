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

package de.undercouch.gradle.tasks.download;

import java.io.File;

/**
 * An interface for checksum checks
 * @author Michel Kraemer
 */
public interface VerifySpec {
    /**
     * Sets the file to verify
     * @param src the file (either a filename or a {@link java.io.File} object)
     */
    void src(Object src);
    
    /**
     * Set the algorithm to use to compute the checksum. Defaults to "MD5" (see
     * the <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#MessageDigest">list
     * of algorithm names</a> for more information).
     * @param algorithm the name of the algorithm
     */
    void algorithm(String algorithm);
    
    /**
     * Set the actual checksum to verify against
     * @param checksum the checksum (in hex)
     */
    void checksum(String checksum);
    
    /**
     * @return the file to verify
     */
    File getSrc();
    
    /**
     * @return the algorithm to use to compute the checksum
     */
    String getAlgorithm();
    
    /**
     * @return the actual checksum to verify against (in hex)
     */
    String getChecksum();
}
