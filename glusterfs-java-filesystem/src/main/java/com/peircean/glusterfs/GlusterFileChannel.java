package com.peircean.glusterfs;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fusesource.glfsjni.internal.GLFS;
import org.fusesource.glfsjni.internal.GlusterOpenOption;
import org.fusesource.glfsjni.internal.structs.stat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class GlusterFileChannel extends FileChannel {
    public static final Map<StandardOpenOption, Integer> optionMap = new HashMap<StandardOpenOption, Integer>();

    static {
        optionMap.put(StandardOpenOption.APPEND, GlusterOpenOption.O_APPEND);
        optionMap.put(StandardOpenOption.CREATE, GlusterOpenOption.O_CREAT);
        optionMap.put(StandardOpenOption.CREATE_NEW, GlusterOpenOption.O_CREAT | GlusterOpenOption.O_EXCL);
        optionMap.put(StandardOpenOption.DSYNC, GlusterOpenOption.O_DSYNC);
        optionMap.put(StandardOpenOption.READ, GlusterOpenOption.O_RDONLY);
        optionMap.put(StandardOpenOption.WRITE, GlusterOpenOption.O_RDWR);
        optionMap.put(StandardOpenOption.TRUNCATE_EXISTING, GlusterOpenOption.O_TRUNC);
    }

    private GlusterFileSystem fileSystem;
    private GlusterPath path;
    private Set<? extends OpenOption> options;
    private FileAttribute<?> attrs[] = null;
    private long fileptr;

    void init(GlusterFileSystem fileSystem, Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        this.fileSystem = fileSystem;
        if (!path.isAbsolute()) {
            throw new IllegalStateException("Only absolute paths are supported at this time");
        }
        this.path = (GlusterPath) path;
        this.options = options;

        int flags = parseOptions(options);
        int mode = parseAttrs(attrs);

        System.out.println(options);

        String pathString = path.toUri().getPath();
        boolean createNew = options.contains(StandardOpenOption.CREATE_NEW);
        if (options.contains(StandardOpenOption.CREATE) || createNew) {
            fileptr = GLFS.glfs_creat(fileSystem.getVolptr(), pathString, flags, mode);
            System.out.println("CREATE: "+fileptr);
        }
        
        if (createNew && 0 == fileptr) {
            throw new FileAlreadyExistsException(path.toString());
        }
        
        if (0 >= fileptr) {
            fileptr = GLFS.glfs_open(fileSystem.getVolptr(), pathString, flags);
            System.out.println("OPEN: "+fileptr);
        }

        if (0 >= fileptr) {
            throw new IOException("Unable to create or open file '" + pathString + "' on volume '" + fileSystem.toString() + "'");
        }
    }

    int parseAttrs(FileAttribute<?>... attrs) {
        int mode = 0;
        for (FileAttribute a : attrs) {
            for (PosixFilePermission p : (Set<PosixFilePermission>) a.value()) {
                switch (p) {
                    case OTHERS_EXECUTE:
                        mode |= 0001;
                        break;
                    case OTHERS_WRITE:
                        mode |= 0002;
                        break;
                    case OTHERS_READ:
                        mode |= 0004;
                        break;
                    case GROUP_EXECUTE:
                        mode |= 0010;
                        break;
                    case GROUP_WRITE:
                        mode |= 0020;
                        break;
                    case GROUP_READ:
                        mode |= 0040;
                        break;
                    case OWNER_EXECUTE:
                        mode |= 0100;
                        break;
                    case OWNER_WRITE:
                        mode |= 0200;
                        break;
                    case OWNER_READ:
                        mode |= 0400;
                        break;
                }
            }
        }
        return mode;
    }

    int parseOptions(Set<? extends OpenOption> options) {
        int opt = 0;
        for (OpenOption o : options) {
            if (!optionMap.containsKey(o)) {
                throw new UnsupportedOperationException("Option " + o + " is not supported at this time");
            }
            opt |= optionMap.get(o);
        }
        return opt;
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        return 0;
    }

    @Override
    public long read(ByteBuffer[] byteBuffers, int i, int i2) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        byteBuffer.rewind();
        byte[] buf = new byte[byteBuffer.remaining()];
        byteBuffer.get(buf);
        return GLFS.glfs_write(fileptr, buf, buf.length, 0);
    }

    @Override
    public long write(ByteBuffer[] byteBuffers, int offset, int length) throws IOException {
        return 0;
    }

    @Override
    public long position() throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FileChannel position(long l) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long size() throws IOException {
        stat stat = new stat();
        int retval = GLFS.glfs_fstat(fileptr, stat);
        if (0 != retval) {
            throw new IOException("fstat failed");
        }
        return stat.st_size;
    }

    @Override
    public FileChannel truncate(long l) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void force(boolean b) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long transferTo(long l, long l2, WritableByteChannel writableByteChannel) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long transferFrom(ReadableByteChannel readableByteChannel, long l, long l2) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int read(ByteBuffer byteBuffer, long l) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int write(ByteBuffer byteBuffer, long l) throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MappedByteBuffer map(MapMode mapMode, long l, long l2) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FileLock lock(long l, long l2, boolean b) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FileLock tryLock(long l, long l2, boolean b) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void implCloseChannel() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
