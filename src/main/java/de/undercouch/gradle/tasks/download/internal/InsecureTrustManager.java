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

package de.undercouch.gradle.tasks.download.internal;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * An insecure trust manager that accepts all certificates.
 * @author Punyashloka Biswal
 */
public class InsecureTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
        // accept all
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
        // accept all
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
