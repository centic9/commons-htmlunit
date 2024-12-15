[![Build Status](https://github.com/centic9/commons-htmlunit/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/centic9/commons-htmlunit/actions)
[![Gradle Status](https://gradleupdate.appspot.com/centic9/commons-htmlunit/status.svg?branch=master)](https://gradleupdate.appspot.com/centic9/commons-htmlunit/status)
[![Release](https://img.shields.io/github/release/centic9/commons-htmlunit.svg)](https://github.com/centic9/commons-htmlunit/releases)
[![GitHub release](https://img.shields.io/github/release/centic9/commons-htmlunit.svg?label=changelog)](https://github.com/centic9/commons-htmlunit/releases/latest)
[![Tag](https://img.shields.io/github/tag/centic9/commons-htmlunit.svg)](https://github.com/centic9/commons-htmlunit/tags)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.dstadler/commons-htmlunit/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/org.dstadler/commons-htmlunit) 
[![Maven Central](https://img.shields.io/maven-central/v/org.dstadler/commons-htmlunit.svg)](https://maven-badges.herokuapp.com/maven-central/org.dstadler/commons-htmlunit)

This is a small library of helpers that I find useful in projects where I use [HtmlUnit](http://htmlunit.sourceforge.net/).

It covers areas that I miss in the library itself.

## Contents

Here an (incomplete) list of bits and pieces in this lib:
* HtmlUnitUtils - Helper methods to create a WebClient and to fetch HTML-items from a page
* WebPageFileCache - A simple cache for web-pages to avoid fetching the same content again and again.

## Use it

### Gradle

    compile 'org.dstadler:commons-htmlunit:1.+'

## Change it

### Grab it

    git clone https://github.com/centic9/commons-htmlunit.git

### Build it and run tests

    cd commons-htmlunit
    ./gradlew check jacocoTestReport

### Release it

    ./gradlew --console=plain release && ./gradlew closeAndReleaseRepository

* This should automatically release the new version on MavenCentral
* Afterwards go to the [GitHub releases page](https://github.com/centic9/commons-htmlunit/releases) and add release-notes

## Support this project

If you find this library useful and would like to support it, you can [Sponsor the author](https://github.com/sponsors/centic9)

## Licensing

* commons-htmlunit is licensed under the [BSD 2-Clause License]

[BSD 2-Clause License]: https://www.opensource.org/licenses/bsd-license.php
