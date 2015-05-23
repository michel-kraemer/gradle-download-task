Gradle Download Task Examples
=============================

This directory contains a number of examples for gradle-download-task.

The examples work with Gradle 2.1 and higher. If you need instructions
for older Gradle versions please have a look at the project's [readme file](../README.md).

[simpleTask.gradle](simpleTask.gradle)
--------------------------------------

Download a single file.

    gradle -b simpleTask.gradle

[simpleExtension.gradle](simpleExtension.gradle)
------------------------------------------------

Download a single file using the DSL extension.

    gradle -b simpleExtension.gradle

[multipleFiles.gradle](multipleFiles.gradle)
--------------------------------------------

Download multiple files.

    gradle -b multipleFiles.gradle

[customHeader.gradle](customHeader.gradle)
------------------------------------------

Download single file and specify a custom HTTP header.

    gradle -b customHeader.gradle

[directory.gradle](directory.gradle)
------------------------------------

Download all files from a directory.

    gradle -b directory.gradle

[unzip.gradle](unzip.gradle)
----------------------------

Download a ZIP file and extract its contents.

    gradle -b unzip.gradle

Run all examples
----------------

Useful for testing.

Bash:

    find . -maxdepth 1 -name '*.gradle' -type f -exec echo {} \; -exec gradle -b {} \;

Windows:

    FORFILES /M *.gradle /C "cmd /c if @isdir equ FALSE echo @file && gradle -b @file"
