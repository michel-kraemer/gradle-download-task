Gradle Download Task
====================

This is a simple download task for [Gradle](http://www.gradle.org/).
It displays progress information just as Gradle does when it retrieves
an artifact from a repository.

The plugin has been sucessfully tested with Gradle 1.0 up to 2.1.
It should work with newer versions as well.

Apply plugin configuration
--------------------------

### Gradle 2.1 and higher

```groovy
plugins {
    id "de.undercouch.download" version "1.1"
}
```

### Gradle 1.x and 2.0

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'de.undercouch:gradle-download-task:1.1'
    }
}

apply plugin: 'download-task'
```

Usage
-----

After you applied the plugin configuration (see above) you can use the `Download` task as follows:

```groovy
import de.undercouch.gradle.tasks.download.Download

task downloadFile(type: Download) {
    src 'http://www.example.com/index.html'
    dest buildDir
}
```

You can also use the download extension to retrieve a file anywhere
in your build script:

```groovy
task myTask << {
    //do something ...
    //... then download a file
    download {
        src 'http://www.example.com/index.html'
        dest buildDir
    }
    //... do something else
}
```

Examples
--------

Sequentially download a list of files to a directory:

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

To download and unpack a ZIP file use can combine the download task
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

Please have a look at the `examples` directory for more code samples.

Properties
----------

The download task and the extension support the following properties

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
<dt>onlyIfNewer</dt>
<dd><code>true</code> if the file should only be downloaded if it
has been modified on the server since the last download <em>(default:
<code>false</code>)</em></dd>
<dt>compress</dt>
<dd><code>true</code> if compression should be used during download <em>(default:
<code>true</code>)</em></dd>
<dt>username</dt>
<dd>The username for <code>Basic</code> authentication <em>(optional)</em></dd>
<dt>password</dt>
<dd>The password for <code>Basic</code> authentication <em>(optional)</em></dd>
<dt>header</dt>
<dd>The name and value of a request header to set when making the download
request <em>(optional)</em></dd>
<dt>headers</dt>
<dd>A map of request headers to set when making the download
request <em>(optional)</em></dd>
</dl>

License
-------

The toolchain is licensed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
