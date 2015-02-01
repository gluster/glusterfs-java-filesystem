package com.peircean.glusterfs;

import com.peircean.libgfapi_jni.internal.GLFS;
import com.peircean.libgfapi_jni.internal.structs.stat;
import com.peircean.libgfapi_jni.internal.structs.statvfs;
import lombok.AccessLevel;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

import static com.peircean.libgfapi_jni.internal.GLFS.*;

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
        if (!Files.isDirectory(path)) {
            throw new NotDirectoryException("Not a directory! " + path.toString());
        }
        GlusterPath glusterPath = (GlusterPath) path;
        GlusterDirectoryStream stream = new GlusterDirectoryStream();
        stream.setFileSystem(glusterPath.getFileSystem());
        stream.open(glusterPath);
        stream.setFilter(filter);

        return stream;
    }

    @Override
    public void createDirectory(Path path, FileAttribute<?>... fileAttributes) throws IOException {
        if (Files.exists(path)) {
            throw new FileAlreadyExistsException(path.toString());
        }
        if (!Files.exists(path.getParent())) {
            throw new IOException();
        }

        int mode = 0775;

        if (fileAttributes.length > 0) {
            mode = GlusterFileAttributes.parseAttrs(fileAttributes);
        }

        int ret = GLFS.glfs_mkdir(((GlusterFileSystem) path.getFileSystem()).getVolptr(), path.toString(), mode);

        if (ret < 0) {
            throw new IOException(path.toString());
        }
    }

    @Override
    public void delete(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
        if (Files.isDirectory(path)) {
            if(!directoryIsEmpty(path)) {
                throw new DirectoryNotEmptyException(path.toString());
            }

            int ret = GLFS.glfs_rmdir(((GlusterFileSystem)path.getFileSystem()).getVolptr(), path.toString());

            if (ret < 0) {
                throw new IOException(path.toString());
            }
        } else {
            int ret = GLFS.glfs_unlink(((GlusterFileSystem) path.getFileSystem()).getVolptr(), path.toString());

            if (ret < 0) {
                throw new IOException(path.toString());
            }
        }
    }

    @Override
    public void copy(Path path, Path path2, CopyOption... copyOptions) throws IOException {
        guardAbsolutePath(path);
        guardAbsolutePath(path2);
        guardFileExists(path);

        boolean targetExists = Files.exists(path2);
        if (targetExists && isSameFile(path, path2)) {
            return;
        }

        boolean overwrite = false;
        boolean copyAttributes = false;
        for (CopyOption co : copyOptions) {
            if (StandardCopyOption.ATOMIC_MOVE.equals(co)) {
                throw new UnsupportedOperationException("Atomic move not supported");
            }
            if (StandardCopyOption.REPLACE_EXISTING.equals(co)) {
                overwrite = true;
            }
            if (StandardCopyOption.COPY_ATTRIBUTES.equals(co)) {
                copyAttributes = true;
            }
        }

        if (!overwrite && targetExists) {
            throw new FileAlreadyExistsException("Target " + path2 + " exists and REPLACE_EXISTING not specified");
        }
        if (Files.isDirectory(path2) && !directoryIsEmpty(path2)) {
            throw new DirectoryNotEmptyException("Target not empty: " + path2);
        }
        if (Files.isDirectory(path)) {
            Files.createDirectory(path2);
        } else {
            Files.createFile(path2, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-rw-r--")));
            copyFileContent(path, path2);
            if (copyAttributes) {
                copyFileAttributes(path, path2);
            }
        }
    }

    void copyFileAttributes(Path path, Path path2) throws IOException {
        stat stat = new stat();
        long volptr = ((GlusterFileSystem) path.getFileSystem()).getVolptr();
        int retStat = glfs_stat(volptr, path.toString(), stat);
        int retChmod = 0;
        if (0664 != stat.st_mode) {
            retChmod = GLFS.glfs_chmod(volptr, path2.toString(), stat.st_mode);
        }
        if (retStat < 0 || retChmod < 0) {
            throw new IOException("Could not copy file attributes.");
        }
    }

    void copyFileContent(Path path, Path path2) throws IOException {
        Set<StandardOpenOption> options = new HashSet<>();
        options.add(StandardOpenOption.READ);

        //TODO: investigate 8kB byte array limitation
        byte[] readBytes = new byte[8192];
        FileChannel channel = newFileChannel(path, options);
        ByteBuffer readBuffer = ByteBuffer.wrap(readBytes);

        boolean writtenTo = false;
        int read = channel.read(readBuffer);

        while (read > 0) {
            byte[] writeBytes = Arrays.copyOf(readBytes, read);
            if (!writtenTo) {
                Files.write(path2, writeBytes, StandardOpenOption.TRUNCATE_EXISTING);
                writtenTo = true;
            } else {
                Files.write(path2, writeBytes, StandardOpenOption.APPEND);
            }
            read = channel.read(readBuffer);
        }
        channel.close();
    }

    boolean directoryIsEmpty(Path path) throws IOException {
        try (DirectoryStream<Path> stream = newDirectoryStream(path, null)) {
            if (stream.iterator().hasNext()) {
                return false;
            }
            return true;
        }
    }

    @Override
    public void move(Path path, Path path2, CopyOption... copyOptions) throws IOException {
        guardAbsolutePath(path);
        guardAbsolutePath(path2);
        guardFileExists(path);
        if (Files.exists(path2) && isSameFile(path, path2)) {
            return;
        }

        boolean overwrite = false;
        for (CopyOption co : copyOptions) {
            if (StandardCopyOption.ATOMIC_MOVE.equals(co)) {
                throw new AtomicMoveNotSupportedException(path.toString(), path2.toString(), "Atomic move not supported");
            }
            if (StandardCopyOption.REPLACE_EXISTING.equals(co)) {
                overwrite = true;
            }
        }

        FileSystem fileSystem = path.getFileSystem();

        if (!overwrite && Files.exists(path2)) {
            throw new FileAlreadyExistsException("Target " + path2 + " exists and REPLACE_EXISTING not specified");
        }
        if (Files.isDirectory(path2) && !directoryIsEmpty(path2)) {
            throw new DirectoryNotEmptyException("Target not empty: " + path2);
        }
        if (!fileSystem.equals(path2.getFileSystem())) {
            throw new UnsupportedOperationException("Can not move file to a different file system");
        }
        GLFS.glfs_rename(((GlusterFileSystem) fileSystem).getVolptr(), ((GlusterPath) path).getString(), ((GlusterPath) path2).getString());
    }

    void guardFileExists(Path path) throws NoSuchFileException {
        if (!Files.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
    }

    void guardAbsolutePath(Path p) {
        if (!p.isAbsolute()) {
            throw new UnsupportedOperationException("Relative paths not supported: " + p);
        }
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        if (path.equals(path2)) {
            return true;
        }
        if (!path.getFileSystem().equals(path2.getFileSystem())) { //if file system differs, then we don't need to check provider; we know the files differ
            return false;
        }
        guardFileExists(path);
        guardFileExists(path2);

        stat stat1 = statPath(path);
        stat stat2 = statPath(path2);

        return stat1.st_ino == stat2.st_ino;
    }

    stat statPath(Path path) throws IOException {
        stat stat = new stat();
        String pathString = ((GlusterPath) path).getString();
        int ret = GLFS.glfs_stat(((GlusterFileSystem) path.getFileSystem()).getVolptr(),
                pathString, stat);
        if (ret != 0) {
            throw new IOException("Stat failed for " + pathString);
        }
        return stat;
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return ((GlusterPath) path.getFileName()).getParts()[0].startsWith(".");
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        if (Files.exists(path)) {
            return path.getFileSystem().getFileStores().iterator().next();
        } else {
            throw new NoSuchFileException(path.toString());
        }
    }

    @Override
    public void checkAccess(Path path, AccessMode... accessModes) throws IOException {
        long volptr = ((GlusterFileSystem) path.getFileSystem()).getVolptr();
        String pathString = ((GlusterPath) path).getString();

        stat stat = new stat();
        int ret = GLFS.glfs_lstat(volptr, pathString, stat);

        if (-1 == ret) {
            throw new NoSuchFileException("");
        }

        for (AccessMode m : accessModes) {
            int access = GLFS.glfs_access(volptr, pathString, modeInt(m));
            if (-1 == access) {
                throw new AccessDeniedException(pathString);
            }
        }

    }

    private int modeInt(AccessMode m) {
        switch (m) {
            case EXECUTE:
                return 1;
            case WRITE:
                return 2;
            case READ:
                return 4;
        }
        return -1;
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> vClass, LinkOption... linkOptions) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... linkOptions) throws IOException {
//        if (!type.isAssignableFrom(GlusterFileAttributes.class)) { // Why doesn't this work when type is GlusterFileAttributes.class?!
        if (type.equals(DosFileAttributes.class)) {
            throw new UnsupportedOperationException(type + " attribute type is not supported, only PosixFileAttributes & its superinterfaces");
        }
        stat stat = new stat();

        boolean followSymlinks = true;
        for (LinkOption lo : linkOptions) {
            if (lo.equals(LinkOption.NOFOLLOW_LINKS)) {
                followSymlinks = false;
                break;
            }
        }
        int ret;
        String pathString = ((GlusterPath) path).getString();
        if (followSymlinks) {
            ret = GLFS.glfs_stat(((GlusterFileSystem) path.getFileSystem()).getVolptr(), pathString, stat);
        } else {
            ret = GLFS.glfs_lstat(((GlusterFileSystem) path.getFileSystem()).getVolptr(), pathString, stat);
        }

        if (-1 == ret) {
            throw new NoSuchFileException("");
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

    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        String pathString = link.toString();
        if (!Files.isSymbolicLink(link)) {
            throw new NotLinkException(pathString);
        }

        stat stat = new stat();
        GlusterFileSystem fileSystem = (GlusterFileSystem) link.getFileSystem();
        long volptr = fileSystem.getVolptr();
        int statReturn = GLFS.glfs_lstat(volptr, pathString, stat);
        if (0 != statReturn) {
            throw new IOException("Unable to get size of symlink " + pathString);
        }

        long length = (int) stat.st_size;
        byte[] content = new byte[(int)length];
        int readReturn = GLFS.glfs_readlink(volptr, pathString, content, content.length);
        if (-1 == readReturn) {
            throw new IOException("Unable to read symlink " + pathString);
        }
        
        return new GlusterPath(fileSystem, new String(content));
    }

    @Override
    public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
        String linkPath = link.toString();
        if (Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileAlreadyExistsException(linkPath);
        }
        if (null != attrs && attrs.length > 0) {
            throw new UnsupportedOperationException("glfs_symlink does not support atomic mode/perms");
        }
        GlusterFileSystem fileSystem = (GlusterFileSystem) link.getFileSystem();
        long volptr = fileSystem.getVolptr();
        int ret = GLFS.glfs_symlink(volptr, target.toString(), linkPath);
        if (0 != ret) {
            throw new IOException("Unknown error creating symlink: " + linkPath);
        }
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
