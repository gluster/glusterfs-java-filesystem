package com.peircean.glusterfs;

import com.peircean.libgfapi_jni.internal.GLFS;
import com.peircean.libgfapi_jni.internal.GlusterOpenOption;
import com.peircean.libgfapi_jni.internal.UtilJNI;
import com.peircean.libgfapi_jni.internal.structs.stat;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class GlusterFileChannel extends FileChannel {
	public static final Map<StandardOpenOption, Integer> optionMap = new HashMap<StandardOpenOption, Integer>();
	public static final Map<PosixFilePermission, Integer> perms = new HashMap<PosixFilePermission, Integer>();

	static {
		optionMap.put(StandardOpenOption.APPEND, GlusterOpenOption.O_APPEND);
		optionMap.put(StandardOpenOption.CREATE, GlusterOpenOption.O_CREAT);
		optionMap.put(StandardOpenOption.CREATE_NEW, GlusterOpenOption.O_CREAT | GlusterOpenOption.O_EXCL);
		optionMap.put(StandardOpenOption.DSYNC, GlusterOpenOption.O_DSYNC);
		optionMap.put(StandardOpenOption.READ, GlusterOpenOption.O_RDONLY);
		optionMap.put(StandardOpenOption.WRITE, GlusterOpenOption.O_RDWR);
		optionMap.put(StandardOpenOption.TRUNCATE_EXISTING, GlusterOpenOption.O_TRUNC);

		perms.put(PosixFilePermission.OTHERS_EXECUTE, 0001);
		perms.put(PosixFilePermission.OTHERS_WRITE, 0002);
		perms.put(PosixFilePermission.OTHERS_READ, 0004);
		perms.put(PosixFilePermission.GROUP_EXECUTE, 0010);
		perms.put(PosixFilePermission.GROUP_WRITE, 0020);
		perms.put(PosixFilePermission.GROUP_READ, 0040);
		perms.put(PosixFilePermission.OWNER_EXECUTE, 0100);
		perms.put(PosixFilePermission.OWNER_WRITE, 0200);
		perms.put(PosixFilePermission.OWNER_READ, 0400);
	}

	private GlusterFileSystem fileSystem;
	private GlusterPath path;
	private Set<? extends OpenOption> options = new HashSet<>();
	private FileAttribute<?> attrs[] = null;
	private long fileptr;
	private long position;
	private boolean closed = false;

	void init(GlusterFileSystem fileSystem, Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		this.fileSystem = fileSystem;
		if (!path.isAbsolute()) {
			throw new IllegalStateException("Only absolute paths are supported at this time");
		}
		this.path = (GlusterPath) path;
		this.options = options;

		int flags = parseOptions(options);
		int mode = GlusterFileAttributes.parseAttrs(attrs);

		String pathString = path.toUri().getPath();
		boolean createNew = options.contains(StandardOpenOption.CREATE_NEW);
		if (options.contains(StandardOpenOption.CREATE) || createNew) {
			fileptr = GLFS.glfs_creat(fileSystem.getVolptr(), pathString, flags, mode);
		}

		if (createNew && 0 == fileptr) {
			throw new FileAlreadyExistsException(path.toString());
		}

		if (0 >= fileptr) {
			fileptr = GLFS.glfs_open(fileSystem.getVolptr(), pathString, flags);
		}

		if (0 >= fileptr) {
			throw new IOException("Unable to create or open file '" + pathString + "' on volume '" + fileSystem.toString() + "'");
		}
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
		guardClosed();
		byte[] bytes = byteBuffer.array();
		long read = GLFS.glfs_read(fileptr, bytes, bytes.length, 0);
		if (read < 0) {
			throw new IOException(UtilJNI.strerror());
		}
		position += read;
		byteBuffer.position((int)read);
		return (int) read;
	}

	@Override
	public long read(ByteBuffer[] byteBuffers, int offset, int length) throws IOException {
		guardClosed();
		if (length < 0 || length > byteBuffers.length - offset) {
			throw new IndexOutOfBoundsException("Length provided is invalid.");
		}
		if (offset < 0 || offset > byteBuffers.length) {
			throw new IndexOutOfBoundsException("Offset provided is invalid.");
		}
		if (!options.contains(StandardOpenOption.READ)) {
			throw new NonReadableChannelException();
		}

		long totalRead = 0L;
		try {
			totalRead = readHelper(byteBuffers, offset, length);
		} finally {
			if (totalRead > 0) {
				position += totalRead;
			}
		}
		return totalRead;
	}

	long readHelper(ByteBuffer[] byteBuffers, int offset, int length) throws IOException {
		long totalRead = 0L;
		boolean endOfStream = false;
		for (int i = offset; i < length + offset && !endOfStream; i++) {
			byte[] bytes = byteBuffers[i].array();
			int remaining;
			while ((remaining = byteBuffers[i].remaining()) > 0) {
				long read = GLFS.glfs_read(fileptr, bytes, remaining, 0);
				if (read < 0) {
					throw new IOException(UtilJNI.strerror());
				}
				totalRead += read;
				byteBuffers[i].position((int) read);
				if (0 == read) {
					endOfStream = true;
					break;
				}
			}
		}

		if (endOfStream && totalRead == 0) {
			return -1;
		}

		return totalRead;
	}

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        guardClosed();
        byte[] buf = byteBuffer.array();
        int written = GLFS.glfs_write(fileptr, buf, buf.length, 0);
        if (written < 0) {
            throw new IOException(UtilJNI.strerror());
        }
        position += written;
        byteBuffer.position(written);
        return written;
    }

    @Override
    public long write(ByteBuffer[] byteBuffers, int offset, int length) throws IOException {
        guardClosed();
        if (offset < 0 || offset > byteBuffers.length) {
            throw new IndexOutOfBoundsException("Offset provided is invalid.");
        }
        if (length < 0 || length > byteBuffers.length - offset) {
            throw new IndexOutOfBoundsException("Length provided is invalid");
        }
        if (!options.contains(StandardOpenOption.WRITE)) {
            throw new NonWritableChannelException();
        }

        long totalWritten = 0L;

        for (int i = offset; i < length + offset; i++) {
            int remaining = byteBuffers[i].remaining();
            while (remaining > 0) {
                byte[] bytes = byteBuffers[i].array();
                int written = GLFS.glfs_write(fileptr, bytes, remaining, 0);
                if (written < 0) {
                    throw new IOException();
                }
                position += written;
                byteBuffers[i].position(written);
                totalWritten += written;
                remaining = byteBuffers[i].remaining();
            }
        }
        return totalWritten;
    }

	@Override
	public long position() throws IOException {
		guardClosed();
		return position;
	}

	@Override
	public FileChannel position(long offset) throws IOException {
		guardClosed();
		if (offset < 0) {
			throw new IllegalArgumentException("offset can't be negative");
		}
		int whence = 0; //SEEK_SET
		int seek = GLFS.glfs_lseek(fileptr, offset, whence);
		position = offset;
		return this;
	}

	void guardClosed() throws ClosedChannelException {
		if (closed) {
			throw new ClosedChannelException();
		}
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
		guardClosed();
		int fsync = GLFS.glfs_fsync(fileptr);
		if (0 != fsync) {
			throw new IOException("Unable to fsync");
		}
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
	public int read(ByteBuffer byteBuffer, long position) throws IOException {
		guardClosed();
		if (position < 0) {
			throw new IllegalArgumentException();
		}
		if (!options.contains(StandardOpenOption.READ)) {
			throw new NonReadableChannelException();
		}
		if (position >= size()) {
			return -1;
		}
		int whence = 0; //SEEK_SET
		int seek = GLFS.glfs_lseek(fileptr, position, whence);
		if (seek < 0) {
			throw new IOException();
		}
		byte[] bytes = byteBuffer.array();
		long read = GLFS.glfs_read(fileptr, bytes, bytes.length, 0);

		if (0 > read) {
			throw new IOException();
		}

		seek = GLFS.glfs_lseek(fileptr, this.position, whence);

		if (0 > seek) {
			throw new IOException(UtilJNI.strerror());
		}

		return (int) read;
	}

    @Override
    public int write(ByteBuffer byteBuffer, long position) throws IOException {
        guardClosed();
        if (position < 0) {
            throw new IllegalArgumentException();
        }
        if (!options.contains(StandardOpenOption.WRITE)) {
            throw new NonWritableChannelException();
        }
        if (position >= size()) {
            byte[] bytes = byteBuffer.array();
            byte[] temp = Arrays.copyOf(bytes, bytes.length + (int) (position - size()));
            byteBuffer = ByteBuffer.wrap(temp);
        }
        int whence = 0; //SEEK_SET
        int seek = GLFS.glfs_lseek(fileptr, position, whence);
        if (seek < 0) {
            throw new IOException();
        }
        byte[] bytes = byteBuffer.array();
        long written = GLFS.glfs_write(fileptr, bytes, bytes.length, 0);
        seek = GLFS.glfs_lseek(fileptr, this.position, whence);
        if (seek < 0) {
            throw new IOException();
        }
        return (int) written;
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
		if (!closed) {
			int close = GLFS.glfs_close(fileptr);
			if (0 != close) {
				throw new IOException("Close returned nonzero");
			}
			closed = true;
		}
	}

}
