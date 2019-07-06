Gradle Download Task [![CircleCI](https://circleci.com/gh/michel-kraemer/gradle-download-task.svg?style=shield)](https://circleci.com/gh/michel-kraemer/gradle-download-task) [![codecov](https://codecov.io/gh/michel-kraemer/gradle-download-task/branch/master/graph/badge.svg)](https://codecov.io/gh/michel-kraemer/gradle-download-task) [![Apache License, Version 2.0](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
====================

This is a simple download task for [Gradle](http://www.gradle.org/).
It displays progress information just as Gradle does when it retrieves
an artifact from a repository.

The plugin has been sucessfully tested with Gradle 2.0 up to 5.4.1.
It should work with newer versions as well.

<img width="559" src="https://raw.githubusercontent.com/michel-kraemer/gradle-download-task/e6bbe00dedd5e0bdaab12f4b1980bd51d22d10d1/gradle-download-task.gif">

Apply plugin configuration
--------------------------

### Gradle 2.1 and higher

```groovy
plugins {
    id "de.undercouch.download" version "4.0.0"
}
```

### Gradle 2.0

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'de.undercouch:gradle-download-task:4.0.0'
    }
}

apply plugin: 'de.undercouch.download'
```

Usage
-----

After you applied the plugin configuration (see above) you can use the `Download` task as follows:

```groovy
task downloadFile(type: Download) {
    src 'http://www.example.com/index.html'
    dest buildDir
}
```

By default, the plugin always performs a download even if the destination file
already exists. If you want to prevent files from being downloaded again, use
the `overwrite` flag (see [description below](#download-task)).

```groovy
task downloadFile(type: Download) {
    src 'http://www.example.com/index.html'
    dest buildDir
    overwrite false
}
```

As an alternative to the `Download` task, you may also use the `download`
extension to retrieve a file anywhere in your build script:

```groovy
task myTask << {
    //do something ...
    //... then download a file
    download {
        src 'http://www.example.com/index.html'
        dest buildDir
        overwrite false
    }
    //... do something else
}
```

Minimum requirements
--------------------

The plugin requires:

* Gradle 2.x or higher
* Java 7 or higher

If you need to run the plugin with Gradle 1.x or Java 6, use
gradle-download-task version 3.4.3.

Examples
--------

### Only download a file if it has been modified on the server

```groovy
task downloadFile(type: Download) {
    src 'http://www.example.com/index.html'
    dest buildDir
    onlyIfModified true
}
```

Note that this feature depends on the server and whether it supports the
`If-Modified-Since` request header and if it provides a `Last-Modified`
timestamp in its response.

### Sequentially download a list of files to a directory

```groovy
task downloadMultipleFiles(type: Download) {
    src([
        'http://www.example.com/index.html',
        'http://www.example.com/test.html'
    ])
    dest buildDir
}
```

Please note that you have to specify a directory as destination if you
download multiple files. Otherwise the plugin will fail.

### Download files from a directory

If you want to download all files from a directory and the server
provides a simple directory listing you can use the following code:

```groovy
task downloadDirectory {
    def dir = 'http://central.maven.org/maven2/de/undercouch/gradle-download-task/1.0/'
    def urlLister = new org.apache.ivy.util.url.ApacheURLLister()
    def files = urlLister.listFiles(new URL(dir))
    download {
       src files
       dest buildDir
    }
}
```

### Download and extract a ZIP file

To download and unpack a ZIP file you can combine the download task
plugin with Gradle's built-in support for ZIP files:

```groovy
task downloadZipFile(type: Download) {
    src 'https://github.com/michel-kraemer/gradle-download-task/archive/1.0.zip'
    dest new File(buildDir, '1.0.zip')
}

task downloadAndUnzipFile(dependsOn: downloadZipFile, type: Copy) {
    from zipTree(downloadZipFile.dest)
    into buildDir
}
```

### More examples

Please have a look at the `examples` directory for more code samples. You can
also read my blog post about
[common recipes for gradle-download-task](https://michelkraemer.com/recipes-for-gradle-download/).

Download task
-------------

The download task and the extension support the following properties.

### General

<dl>
<dt>src</dt>
<dd>The URL from which to retrieve the file. Can be a list of URLs if
multiple files shoud be downloaded. <em>(required)</em></dd>
<dt>dest</dt>
<dd>The file or directory where to store the file <em>(required)</em></dd>
<dt>quiet</dt>
<dd><code>true</code> if progress information should not be displayed
<em>(default: <code>false</code>)</em></dd>
<dt>overwrite</dt>
<dd><code>true</code> if existing files should be overwritten <em>(default:
<code>true</code>)</em></dd>
<dt>onlyIfModified (alias: onlyIfNewer)</dt>
<dd><code>true</code> if the file should only be downloaded if it
has been modified on the server since the last download <em>(default:
<code>false</code>)</em></dd>
</dl>

<em>Tip!</em> You may provide Groovy Closures to the `src` and `dest`
properties to calculate their value at runtime.

### Connection

<dl>
<dt>acceptAnyCertificate</dt>
<dd><code>true</code> if HTTPS certificate verification errors should be ignored
and any certificate (even an invalid one) should be accepted.
<em>(default: <code>false</code>)</em></dd>
<dt>compress</dt>
<dd><code>true</code> if compression should be used during download <em>(default:
<code>true</code>)</em></dd>
<dt>header</dt>
<dd>The name and value of a request header to set when making the download
request <em>(optional)</em></dd>
<dt>headers</dt>
<dd>A map of request headers to set when making the download
request <em>(optional)</em></dd>
<dt>connectTimeout</dt>
<dd>The maximum number of milliseconds to wait until a connection is established.
A value of <code>0</code> (zero) means infinite timeout. A negative value
is interpreted as undefined. <em>(default: <code>30 seconds</code>)</em></dd>
<dt>readTimeout</dt>
<dd>The maximum time in milliseconds to wait for data from the server.
A value of <code>0</code> (zero) means infinite timeout. A negative value
is interpreted as undefined. <em>(default: <code>30 seconds</code>)</em></dd>
<dt>retries</dt>
<dd>Specifies the maximum number of retry attempts if a request has failed. By default, requests are never retried and the task fails immediately if the first request does not succeed. If the value is greater than <code>0</code>, failed requests are retried regardless of the actual error. This includes failed connection attempts and file-not-found errors (404). A negative value means infinite retries. <em>(default: <code>0</code>)</em></dd>
</dl>

### Authentication

<dl>
<dt>username</dt>
<dd>The username for <code>Basic</code> or <code>Digest</code> authentication
<em>(optional)</em></dd>
<dt>password</dt>
<dd>The password for <code>Basic</code> or <code>Digest</code> authentication
<em>(optional)</em></dd>
<dt>authScheme</dt>
<dd>The authentication scheme to use (valid values are <code>Basic</code> and
<code>Digest</code>). If <code>username</code> and <code>password</code> are
set, the default value of this property will be <code>Basic</code>. Otherwise
this property has no default value. <em>(optional)</em></dd>
</dl>

### Advanced

<dl>
<dt>downloadTaskDir</dt>
<dd>The directory where the plugin stores information that should persist between builds. It will only be created if necessary. <em>(default: <code>${buildDir}/download-task</code>)</em></dd>
<dt>tempAndMove</dt>
<dd><code>true</code> if the file should be downloaded to a temporary location
and, upon successful execution, moved to the final location. If
<code>overwrite</code> is set to <code>false</code>, this flag is useful to
avoid partially downloaded files if Gradle is forcefully closed or the system
crashes. Note that the plugin always deletes partial downloads on connection
errors, regardless of the value of this flag. The default temporary location
can be configured with the <code>downloadTaskDir</code> property. <em>(default:
<code>false</code>)</em></dd>
<dt>useETag</dt>
<dd>Use this flag in combination with <code>onlyIfModified</code>. If both flags are <code>true</code> the plugin will check a file's timestamp as well as its entity tag (ETag) and only download it if it has been modified on the server since the last download. The plugin can differentiate between <a href="https://tools.ietf.org/html/rfc7232#section-2.1">strong and weak ETags</a>. Possible values are:
<dl>
<dt><code>false</code> <em>(default)</em></dt>
<dd>Do not use the ETag</dd>
<dt><code>true</code></dt>
<dd>Use the ETag but display a warning if it is weak</dd>
<dt><code>"all"</code></dt>
<dd>Use the ETag and do not display a warning if it is weak</dd>
<dt><code>"strongOnly"</code></dt>
<dd>Only use the ETag if it is strong</dd>
</dl></dd>
<dt>cachedETagsFile</dt>
<dd>The location of the file that keeps entity tags (ETags) received
from the server. <em>(default: <code>${downloadTaskDir}/etags.json</code>)</em></dd>
</dl>

Verify task
-----------

The plugin also provides a `Verify` task that can be used to check the integrity
of a downloaded file by calculating its checksum and comparing it to a
pre-defined value. The task succeeds if the file's checksum equals the
given value and fails if it doesn't.

Use the task as follows:

```groovy
task verifyFile(type: Verify) {
    src new File(buildDir, 'file.ext')
    algorithm 'MD5'
    checksum 'ce114e4501d2f4e2dcea3e17b546f339'
}
```

You can combine the download task and the verify task as follows:

```groovy
task downloadFile(type: Download) {
    src 'http://www.example.com/index.html'
    dest buildDir
}

task verifyFile(type: Verify, dependsOn: downloadFile) {
    src new File(buildDir, 'index.html')
    algorithm 'MD5'
    checksum '09b9c392dc1f6e914cea287cb6be34b0'
}
```

The verify task supports the following properties:

<dl>
<dt>src</dt>
<dd>The file to verify <em>(required)</em></dd>
<dt>checksum</dt>
<dd>The actual checksum to verify against <em>(required)</em></dd>
<dt>algorithm</dt>
<dd>The algorithm to use to compute the checksum. See the
<a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#MessageDigest">list of algorithm names</a>
for more information. <em>(default: <code>MD5</code>)</em></dd>
</dl>

Proxy configuration
-------------------

You can configure a proxy server by setting standard JVM system properties. The
plugin uses the same system properties as Gradle. You can set them in the build
script directly. For example, the proxy host can be set as follows:

```groovy
System.setProperty("http.proxyHost", "www.somehost.org");
```

Alternatively, you can set the properties in a `gradle.properties` file like
this:

```properties
systemProp.http.proxyHost=www.somehost.org
systemProp.http.proxyPort=8080
systemProp.http.proxyUser=userid
systemProp.http.proxyPassword=password
systemProp.http.nonProxyHosts=*.nonproxyrepos.com|localhost
```

Put this file in your project's root directory or in your Gradle home directory.

HTTPS is also supported:

```properties
systemProp.https.proxyHost=www.somehost.org
systemProp.https.proxyPort=8080
systemProp.https.proxyUser=userid
systemProp.https.proxyPassword=password
systemProp.https.nonProxyHosts=*.nonproxyrepos.com|localhost
```

Migrating from version 3.x to 4.x
---------------------------------

In gradle-download-task 4.x, we made the following breaking changes to the
API:

* The plugin now requires Gradle 2.x (or higher) and Java 7 (or higher)
* We removed the `timeout` property and introduced `connectTimeout` and
  `readTimeout` instead. This allows you to control the individual timeouts
  better. Also, it improves compatibility with Gradle 5.x, where all tasks have
  a `timeout` property by default. 
* The `credentials` property has been removed. The same applies to the
  possibility to pass instances of Apache HttpClient's `AuthScheme` to the
  `authScheme` property. The strings `Basic` and `Digest` are now the only
  accepted values. There is no replacement. If you need this functionality,
  please file an issue.
* The properties `requestInterceptor` and `responseInterceptor` have been
  removed. There is no replacement. Again, if you need this functionality,
  please file an issue.

License
-------

The plugin is licensed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
