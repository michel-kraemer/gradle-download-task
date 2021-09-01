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
package de.undercouch.gradle.tasks.download.internal

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * An insecure trust manager that accepts all certificates.
 * @author Punyashloka Biswal
 */
class InsecureTrustManager : X509TrustManager {

    override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) {
        // accept all
    }

    override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) {
        // accept all
    }

    override fun getAcceptedIssuers(): Array<X509Certificate>? = null
}