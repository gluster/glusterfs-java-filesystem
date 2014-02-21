# glusterfs-java-filesystem

This project aims to be a complete implementation of a Java7/NIO.2 FileSystem backed by
[GlusterFS](http://www.gluster.org/) via [libgfapi-jni](https://github.com/semiosis/libgfapi-jni)

[![Build Status](https://travis-ci.org/semiosis/glusterfs-java-filesystem.png?branch=master)](https://travis-ci.org/semiosis/glusterfs-java-filesystem)

# Use

The [Example.java](glusterfs-java-filesystem-example/src/main/java/com/peircean/glusterfs/example/Example.java) file in 
the glusterfs-java-filesystem-example project provides a demonstration of the capabilities of this project from a high 
level consumer's point of view.

`glusterfs-java-filesystem-example/src/main/java/com/peircean/glusterfs/example/Example.java`

The example program can be run with maven by executing `mvn exec:exec` in the glusterfs-java-filesystem-example directory.

# Roadmap

### TODO:

- Watch files for changes   
    Complete except for GlusterWatchKeyTest
- Advanced synchronous file I/O   
    Seeking & reading/writing a portion of a file
- Delete files
- Copy files
- Finish attribute support   
    Owner/group names & ability to change   
    More ways to set permissions
- Asychronous file I/O
- Better error reporting & handling
- Test coverage report   
    Blocked due to use of PowerMock

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

# Project License

Until further notice (made here and in LICENSE.txt) this project is licensed under the terms of the
3-clause BSD license, as written in LICENSE.txt.

The licensing is likely to change in the near future as the project matures.
