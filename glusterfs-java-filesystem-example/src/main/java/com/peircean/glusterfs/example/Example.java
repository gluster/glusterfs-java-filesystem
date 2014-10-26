package com.peircean.glusterfs.example;

import com.peircean.glusterfs.GlusterFileSystem;
import com.peircean.glusterfs.GlusterPath;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        Properties properties = new Properties();
        properties.load(Example.class.getClassLoader().getResourceAsStream("example.properties"));

        String vagrantBox = properties.getProperty("glusterfs.server");
        String volname = properties.getProperty("glusterfs.volume");

        System.out.println(getProvider("gluster").toString());

        String mountUri = "gluster://" + vagrantBox + ":" + volname + "/";
        String testUri = "gluster://" + vagrantBox + ":" + volname + "/baz";
        Path mountPath = Paths.get(new URI(mountUri));

        FileSystem fileSystem = FileSystems.newFileSystem(new URI(mountUri), null);
        FileStore store = fileSystem.getFileStores().iterator().next();
        System.out.println("TOTAL SPACE: " + store.getTotalSpace());
        System.out.println("USABLE SPACE: " + store.getUsableSpace());
        System.out.println("UNALLOCATED SPACE: " + store.getUnallocatedSpace());
        System.out.println(fileSystem.toString());

        String hidden = "/foo/.bar";
        boolean isHidden = fileSystem.provider().isHidden(new GlusterPath(((GlusterFileSystem) fileSystem), hidden));
        System.out.println("Is " + hidden + " hidden? " + isHidden);

        hidden = "/foo/bar";
        isHidden = fileSystem.provider().isHidden(new GlusterPath(((GlusterFileSystem) fileSystem), hidden));
        System.out.println("Is " + hidden + " hidden? " + isHidden);

        Set<PosixFilePermission> posixFilePermissions = PosixFilePermissions.fromString("rw-rw-rw-");
        FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(posixFilePermissions);

        Path glusterPath = Paths.get(new URI(testUri));
        System.out.println(glusterPath.getClass());
        System.out.println(glusterPath);
        System.out.println(glusterPath.getFileSystem().toString());

        try {
            Files.createFile(glusterPath, attrs);
            System.out.println("File created");
        } catch (IOException e) {
            System.out.println("File exists, created at " + Files.getLastModifiedTime(glusterPath));
        }
        String hello = "Hello, ";
        Files.write(glusterPath, hello.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        String world = "world!";
        Files.write(glusterPath, world.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        long bazSize = Files.size(glusterPath);
        System.out.println("SIZE: " + bazSize);
        byte[] readBytes = Files.readAllBytes(glusterPath);
        System.out.println(hello + world + " == " + new String(readBytes));
        System.out.println("Last modified: " + Files.getLastModifiedTime(glusterPath) + " (should be now)");
        fileSystem.provider().checkAccess(glusterPath, AccessMode.READ, AccessMode.WRITE);
        System.out.println("Can read & write file");
        try {
            fileSystem.provider().checkAccess(glusterPath, AccessMode.EXECUTE);
            System.out.println("Uh oh, file is executable, that's bad.");
        } catch (AccessDeniedException e) {
            System.out.println("Can't execute file, that's good.");
        }

        Path symlinkPath = Paths.get(new URI(mountUri + "symlink"));
        Path symlinkTarget = Paths.get(new URI(mountUri + "symlinktarget"));
        Files.createSymbolicLink(symlinkPath, symlinkTarget);
        System.out.println("SYMLINK: " + symlinkPath.toString() + " => " + Files.readSymbolicLink(symlinkPath));

        Path copyPath = glusterPath.resolveSibling("copy");
//        Files.createFile(copyPath, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-rw-rw-")));
        Files.copy(glusterPath, copyPath, StandardCopyOption.REPLACE_EXISTING);
        long copySize = Files.size(copyPath);
        System.out.println("Source and copy are " + (bazSize == copySize ? "" : "NOT") + " equal.");

        try {
            Files.newDirectoryStream(mountPath.resolve("bazzzzz"));
        } catch (NotDirectoryException e) {
            System.out.println("Can't list directory of a file, good.");
        }
        DirectoryStream.Filter<? super Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return entry.endsWith("1");
            }
        };
        DirectoryStream<Path> stream = Files.newDirectoryStream(mountPath, filter);
        System.out.println("Mount contents:");

        for (Path p : stream) {
            System.out.println(p.toString());
        }

        filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return entry.endsWith("a");
            }
        };
        stream = Files.newDirectoryStream(mountPath, filter);
        System.out.println("Mount contents:");

        for (Path p : stream) {
            System.out.println(p.toString());
        }

        stream = Files.newDirectoryStream(mountPath);
        System.out.println("Mount contents:");

        PathMatcher matcher = fileSystem.getPathMatcher("glob:**/*z");
        for (Path p : stream) {
            System.out.println(p.toString());
            if (matcher.matches(p)) {
                System.out.println(" **** MATCH ****");
            }
        }

        stream = Files.newDirectoryStream(mountPath, "*z");
        System.out.println("Mount contents:");

        for (Path p : stream) {
            System.out.println(p.toString());
        }

        WatchService watchService = fileSystem.newWatchService();
        Path one = Paths.get(new URI("gluster://" + vagrantBox + ":" + volname + "/one"));

        System.out.println("STARTSWITH empty: " + one.startsWith("/"));
        one.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        for (int i = 0; i < 10; i++) {
            WatchKey take = watchService.poll(1, TimeUnit.SECONDS);
            if (null == take) {
                continue;
            }
            List<WatchEvent<?>> events = take.pollEvents();
            for (WatchEvent e : events) {
                Path path = (Path) e.context();
                Path absolutePath = one.resolve(path).toAbsolutePath();
                boolean exists = Files.exists(absolutePath);
                System.out.println("EXISTS? " + exists);
                if (exists) {
                    System.out.println("SIZE: " + Files.size(absolutePath));
                }
                System.out.println(absolutePath);
                System.out.println(e.toString());
            }
            take.reset();
        }

        fileSystem.close();
    }
}
