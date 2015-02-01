# glusterfs-java-filesystem

This project aims to be a complete implementation of a Java7/NIO.2 File System Provider backed by
[GlusterFS](http://www.gluster.org/) via [libgfapi-jni](https://github.com/semiosis/libgfapi-jni)

[![Build Status](https://travis-ci.org/semiosis/glusterfs-java-filesystem.png?branch=master)](https://travis-ci.org/semiosis/glusterfs-java-filesystem)

[![Test coverage](http://sonar.peircean.com:8008/sonar-status-image/?resource=com.peircean.glusterfs:glusterfs-java-filesystem)](http://sonar.peircean.com/dashboard/index/com.peircean.glusterfs:glusterfs-java-filesystem)

[Test Coverage & Code Quality](http://sonar.peircean.com/dashboard/index/com.peircean.glusterfs:glusterfs-java-filesystem)

Please let me know if you use this project, even if you're just checking it out, I'd like to hear from you.

I prefer to be contacted on IRC, Twitter, or a Github issue.  You can find me, semiosis, in #gluster on Freenode IRC.  My twitter handle is @pragmaticism.

Thanks!

# Use

## Adding to your maven project

    <dependencies>
        <dependency>
            <groupId>com.peircean.glusterfs</groupId>
            <artifactId>glusterfs-java-filesystem</artifactId>
            <version>1.0.4</version>
        </dependency>
    </dependencies>

## Adding to your non-maven project

The maven shade plugin can build a unified (shaded) JAR suitable for dropping in to the classpath of any JVM application.

You can build a "shaded" JAR by cloning the project and running the following command in the glusterfs-java-filesystem subdirectory:

    cd glusterfs-java-filesystem
    mvn package shade:shade

Maven will report the path of this shaded JAR.  You can run `export CLASSPATH=<path-to-shaded.jar>` in a terminal before running your other application.

Contact me (on IRC, Twitter, or in a Github issue) if you need help obtaining a JAR, if you can't, or don't want to, build it with maven yourself.

## Access GlusterFS volumes with the NIO.2 API

Once this library is in your classpath all you need to do in your code is access a GlusterFS URI, for example

    gluster://server:volume/path

## Example usage

A Vagrantfile in the root of this repository sets up a VM with a volume called *foo* at IP address *172.31.31.31* on a 
private network.

The [Example.java](glusterfs-java-filesystem-example/src/main/java/com/peircean/glusterfs/example/Example.java) file in 
the glusterfs-java-filesystem-example project provides a demonstration of the capabilities of this project from a high 
level consumer's point of view, it connects to the volume on the vagrant VM.

To run:

    cd glusterfs-java-filesystem-example
    vagrant up
    mvn exec:exec

# Roadmap

### TODO:

- Replace Example program with formal integration test suite
- Align project versions with glusterfs (this project & libgfapi-jni)
- Update watch service to use [libgfchangelog](https://github.com/gluster/glusterfs/blob/master/xlators/features/changelog/lib/examples/c/get-changes.c) (instead of polling)
- Finish attribute support   
    Owner/group names & ability to change   
    More ways to set permissions
- Finish integration testing for advanced synchronous file IO (reading/writing portion of file)
- Asychronous file I/O
- Better error reporting & handling (utilize UtilJNI.strerror() as part of IOException throws)
- Finish readSymbolicLink unit tests
- Publish test coverage report to Coveralls.io   
    [Blocked](https://github.com/trautonen/coveralls-maven-plugin/issues/36) due to use of Atlassian Clover
- Create hard links

### Completed:

- Connect to a GlusterFS volume using the NIO.2 API
- Basic synchronous file I/O   
    Read the contents of a file all at once   
    Write a chunk of bytes to a file all at once
- File attributes   
    See owner/group id, size, permissions, and last modified timestamp on files and directories   
    Set permissions
- Filesystem/volume stats   
    See the total, free, and usable bytes in a volume
- Directory listing (with filtering)
- Move/rename files
- Watch files for changes   
    Complete except for GlusterWatchKeyTest, in progress
- Create & Read symlinks (read tests incomplete)
- Publish test coverage & code quality reports to [SonarQube](http://sonar.peircean.com/dashboard/index/com.peircean.glusterfs:glusterfs-java-filesystem)
- Delete files
- Copy files
- Advanced synchronous file IO

# Contributing/Development

I'd appreciate your help with this project.  If you have any feedback at all please get in touch.  I'm interested in everything from gripes to pull requests.

# Project License

Until further notice (made here and in LICENSE.txt) this project is licensed under the terms of the
3-clause BSD license, as written in LICENSE.txt.

The licensing is likely to change in the near future as the project matures.

# Acknowledgements

- May G. & Ian H. for their hard work & dedication to improving this project.
- Atlassian for providing a free license for their most excellent Java code quality analyzer, [Clover](https://www.atlassian.com/software/clover/overview).
- All the open source projects we depend on: [GlusterFS](http://gluster.org/), [HawtJNI](https://github.com/fusesource/hawtjni), [Lombok](http://projectlombok.org/), [JUnit](http://junit.org/), [Mockito](https://code.google.com/p/mockito/), [PowerMock](https://code.google.com/p/powermock/), [TestNG](http://testng.org/doc/index.html), [sonarqube](http://www.sonarqube.org/), [Hadoop](http://hadoop.apache.org/) (whose Glob to Regex converter we borrowed), [Maven](http://maven.apache.org/) and all the Maven plugins, and of course [Java](https://www.java.com/).
