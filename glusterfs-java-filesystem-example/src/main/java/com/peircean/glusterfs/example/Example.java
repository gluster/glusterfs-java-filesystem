package com.peircean.glusterfs.example;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class Example {
    public static FileSystemProvider getProvider(String scheme) {
        for (FileSystemProvider fsp : FileSystemProvider.installedProviders()) {
            if (fsp.getScheme().equals(scheme)) {
                return fsp;
            }
        }
        throw new IllegalArgumentException("No provider found for scheme: " + scheme);
    }

    public static void main(String[] args) throws URISyntaxException, IOException {
        System.out.println(getProvider("gluster").toString());

        Path path = Paths.get("gluster://127.0.2.1:foo/");
        System.out.println(path.toString());
        FileSystem fileSystem = FileSystems.newFileSystem(new URI("gluster://127.0.2.1:fo/"), null);
        // Doesn't look like much but tcpdump verifies this really does establish a new glusterfs client connection
        System.out.println(fileSystem.toString());
        fileSystem.close();
    }
}
