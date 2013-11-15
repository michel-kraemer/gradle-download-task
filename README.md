Gradle Download Task
====================

This is a simple download task for [Gradle](http://www.gradle.org/).
It displays progress information just as Gradle does when it retrieves
an artifact from a repository.

Usage
-----

First you need to include the plugin into your build file:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'de.undercouch:gradle-download-task:0.3'
    }
}
```

Then you can use the `Download` as follows:

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
apply plugin: 'download-task'

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

Properties
----------

The download task and the extension support the following properties

<dl>
<dt>src</dt>
<dd>The URL from which to retrieve the file <em>(required)</em></dd>
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
<dt>username</dt>
<dd>The username for <code>Basic</code> authentication <em>(optional)</em></dd>
<dt>password</dt>
<dd>The password for <code>Basic</code> authentication <em>(optional)</em></dd>
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
