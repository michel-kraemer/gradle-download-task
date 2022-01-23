Gradle Download Task Examples
=============================

This directory contains a number of examples for gradle-download-task.

A more detailed description of the examples here can be found in the blog post
[10 recipes for gradle-download-task](http://www.michel-kraemer.com/recipes-for-gradle-download).

[simple](groovy/simple/build.gradle)
--------------------------------------

Download a single file.

[simple-extension](groovy/simple-extension/build.gradle)
------------------------------------------------

Download a single file using the DSL extension.

[multiple-files](groovy/multiple-files/build.gradle)
--------------------------------------------

Download multiple files.

[multiple-files-rename](groovy/multiple-files-rename/build.gradle)
--------------------------------------------------------

Download multiple files and specify destination file names for each of them.

[custom-header](groovy/custom-header/build.gradle)
------------------------------------------

Download single file and specify a custom HTTP header.

[directory](groovy/directory/build.gradle)
------------------------------------

Download all files from a directory.

[directory-github](groovy/directory-github/build.gradle)
------------------------------------

Download all files from a directory in GitHub. Use the GitHub API to get the
directory's contents. Parse the result and download the files.

[etag](groovy/etag/build.gradle)
--------------------------------

Download a file conditionally using its entity tag (ETag).

[lazy-src-and-dest](groovy/lazy-src-and-dest/build.gradle)
----------------------------------------------------

Download a single file to a directory. Use closures for the `src` and `dest`
property.

[mirrors](groovy/mirrors/build.gradle)
--------------------------------

Download a single file from a mirror server. Configure multiple mirror servers
and use the first one that is working.

[temp-rename](groovy/temp-rename/build.gradle)
--------------------------------------

Conditionally download a single file using a temporary name (`<filename>.part`).
Rename the file afterwards if the download was successful.

[unzip](groovy/unzip/build.gradle)
----------------------------

Download a ZIP file and extract its contents.

[verify](groovy/verify/build.gradle)
------------------------------

Download a file and verify its contents by calculating its checksum and
comparing it to a given value.

[verify-extension](groovy/verify-extension/build.gradle)
------------------------------------------------

Same as [verify](groovy/verify/build.gradle) but uses the `verifyChecksum` extension
instead of the `Verify` task.
