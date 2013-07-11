package com.peircean.glusterfs;

import lombok.AccessLevel;
import lombok.Getter;
import org.fusesource.glfsjni.internal.GLFS;
import org.fusesource.glfsjni.internal.structs.stat;
import org.fusesource.glfsjni.internal.structs.statvfs;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.fusesource.glfsjni.internal.GLFS.*;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class GlusterFileSystemProvider extends FileSystemProvider {

    public static final String GLUSTER = "gluster";
    public static final int GLUSTERD_PORT = 24007;
    public static final String TCP = "tcp";
    @Getter(AccessLevel.PACKAGE)
    private static Map<String, GlusterFileSystem> cache = new HashMap<String, GlusterFileSystem>();

    @Override
    public String getScheme() {
        return GLUSTER;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> stringMap) throws IOException {
        String authorityString = uri.getAuthority();
        String[] authority = parseAuthority(authorityString);

        String volname = authority[1];
        long volptr = glfsNew(volname);

        glfsSetVolfileServer(authority[0], volptr);

        glfsInit(authorityString, volptr);

//        GLFS.glfs_set_logging(volptr, "/tmp/gluster-java.log", 9);

        GlusterFileSystem fileSystem = new GlusterFileSystem(this, authority[0], volname, volptr);
        cache.put(authorityString, fileSystem);

        return fileSystem;
    }

    String[] parseAuthority(String authority) {
        if (!authority.contains(":")) {
            throw new IllegalArgumentException("URI must be of the form 'gluster://server:volume/path");
        }
        String[] aarr = authority.split(":");
        if (aarr.length != 2 || aarr[0].isEmpty() || aarr[1].isEmpty()) {
            throw new IllegalArgumentException("URI must be of the form 'gluster://server:volume/path");
        }

        return aarr;
    }

    long glfsNew(String volname) {
        long volptr = glfs_new(volname);
        if (0 == volptr) {
            throw new IllegalArgumentException("Failed to create new client for volume: " + volname);
        }
        return volptr;
    }

    void glfsSetVolfileServer(String host, long volptr) {
        int setServer = glfs_set_volfile_server(volptr, TCP, host, GLUSTERD_PORT);
        if (0 != setServer) {
            throw new IllegalArgumentException("Failed to set server address: " + host);
        }
    }

    void glfsInit(String authorityString, long volptr) {
        int init = glfs_init(volptr);
        if (0 != init) {
            throw new IllegalArgumentException("Failed to initialize glusterfs client: " + authorityString);
        }
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        if (!cache.containsKey(uri.getAuthority())) {
            throw new FileSystemNotFoundException("No cached filesystem for: " + uri.getAuthority());
        }
        return cache.get(uri.getAuthority());
    }

    @Override
    public Path getPath(URI uri) {
        if (!uri.getScheme().equals(getScheme())) {
            throw new IllegalArgumentException("No support for scheme: " + uri.getScheme());
        }
        try {
            FileSystem fileSystem = getFileSystem(uri);
            return fileSystem.getPath(uri.getPath());
        } catch (FileSystemNotFoundException e) {
        }

        try {
            return newFileSystem(uri, null).getPath(uri.getPath());
        } catch (IOException e) {
            throw new FileSystemNotFoundException("Unable to open a connection to " + uri.getAuthority());
        }
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> openOptions, FileAttribute<?>... fileAttributes) throws IOException {
        return newFileChannelHelper(path, openOptions, fileAttributes);
    }

    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return newFileChannelHelper(path, options, attrs);
    }

    FileChannel newFileChannelHelper(Path path, Set<? extends OpenOption> options, FileAttribute<?>[] attrs) throws IOException {
        GlusterFileChannel channel = new GlusterFileChannel();
        channel.init((GlusterFileSystem) getFileSystem(path.toUri()), path, options, attrs);
        return channel;
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path path, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return null;
    }

    @Override
    public void createDirectory(Path path, FileAttribute<?>... fileAttributes) throws IOException {

    }

    @Override
    public void delete(Path path) throws IOException {

    }

    @Override
    public void copy(Path path, Path path2, CopyOption... copyOptions) throws IOException {

    }

    @Override
    public void move(Path path, Path path2, CopyOption... copyOptions) throws IOException {

    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return false;
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return ((GlusterPath) path.getFileName()).getParts()[0].startsWith(".");
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... accessModes) throws IOException {

    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> vClass, LinkOption... linkOptions) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... linkOptions) throws IOException {
        if (type.equals(DosFileAttributes.class)) {
            throw new UnsupportedOperationException("DOS attributes are not supported, only PosixFileAttributes & its superinterfaces");
        }
        stat stat = new stat();

        boolean followSymlinks = true;
        for (LinkOption lo : linkOptions) {
            if (lo.equals(LinkOption.NOFOLLOW_LINKS)) {
                followSymlinks = false;
                break;
            }
        }

        if (followSymlinks) {
            GLFS.glfs_stat(((GlusterFileSystem) path.getFileSystem()).getVolptr(), path.toUri().getPath(), stat);
        } else {
            GLFS.glfs_lstat(((GlusterFileSystem) path.getFileSystem()).getVolptr(), path.toUri().getPath(), stat);
        }
        return (A) GlusterFileAttributes.fromStat(stat);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String s, LinkOption... linkOptions) throws IOException {
        return null;
    }

    @Override
    public void setAttribute(Path path, String s, Object o, LinkOption... linkOptions) throws IOException {

    }

    int close(long volptr) {
        return glfs_fini(volptr);
    }

    long getTotalSpace(long volptr) throws IOException {
        statvfs buf = new statvfs();
        GLFS.glfs_statvfs(volptr, "/", buf);
        return buf.f_bsize * buf.f_blocks;
    }

    long getUsableSpace(long volptr) throws IOException {
        statvfs buf = new statvfs();
        GLFS.glfs_statvfs(volptr, "/", buf);
        return buf.f_bsize * buf.f_bavail;
    }

    long getUnallocatedSpace(long volptr) throws IOException {
        statvfs buf = new statvfs();
        GLFS.glfs_statvfs(volptr, "/", buf);
        return buf.f_bsize * buf.f_bfree;
    }
}
