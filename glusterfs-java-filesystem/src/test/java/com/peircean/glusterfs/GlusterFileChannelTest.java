package com.peircean.glusterfs;

import com.peircean.libgfapi_jni.internal.GLFS;
import com.peircean.libgfapi_jni.internal.GlusterOpenOption;
import com.peircean.libgfapi_jni.internal.structs.stat;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GLFS.class, GlusterFileChannel.class, GlusterFileAttributes.class})
public class GlusterFileChannelTest extends TestCase {

	@Mock
	private GlusterPath mockPath;
	@Mock
	private GlusterFileSystem mockFileSystem;
	@Mock
	private ByteBuffer mockBuffer;
	@Spy
	private GlusterFileChannel channel = new GlusterFileChannel();

	@Test(expected = IllegalStateException.class)
	public void testNewFileChannel_whenNotAbsolutePath() throws IOException, URISyntaxException {
		doReturn(false).when(mockPath).isAbsolute();
		initTestHelper(null, false, false);
	}

	@Test
	public void testNewFileChannel_whenCreate() throws IOException, URISyntaxException {
		doReturn(true).when(mockPath).isAbsolute();
		initTestHelper(StandardOpenOption.CREATE, true, false);
	}

	@Test
	public void testNewFileChannel_whenCreateNew() throws IOException, URISyntaxException {
		doReturn(true).when(mockPath).isAbsolute();
		initTestHelper(StandardOpenOption.CREATE_NEW, true, false);
	}

	@Test(expected = FileAlreadyExistsException.class)
	public void testNewFileChannel_whenCreateNew_andFileAlreadyExists() throws IOException, URISyntaxException {
		doReturn(true).when(mockPath).isAbsolute();
		initTestHelper(StandardOpenOption.CREATE_NEW, false, false);
	}

	@Test
	public void testNewFileChannel_whenNotCreating() throws IOException, URISyntaxException {
		doReturn(true).when(mockPath).isAbsolute();
		initTestHelper(null, false, true);
	}

	@Test(expected = IOException.class)
	public void testNewFileChannel_whenFailing() throws IOException, URISyntaxException {
		doReturn(true).when(mockPath).isAbsolute();
		initTestHelper(null, false, false);
	}

	private void initTestHelper(StandardOpenOption option, boolean created, boolean opened) throws IOException, URISyntaxException {
		Set<StandardOpenOption> options = new HashSet<StandardOpenOption>();
		options.add(StandardOpenOption.WRITE);
		if (null != option) {
			options.add(option);
		}

		Set<PosixFilePermission> posixFilePermissions = PosixFilePermissions.fromString("rw-rw-rw-");
		FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(posixFilePermissions);

		int mode = 0666;
		int flags = GlusterOpenOption.WRITE().create().getValue();
		long volptr = 1234l;
		String path = "/foo/bar";
		long createptr = created ? 4321l : 0;
		long openptr = opened ? 4321l : 0;
		URI pathUri = new URI("gluster://server:volume" + path);
		doReturn(volptr).when(mockFileSystem).getVolptr();
		doReturn(pathUri).when(mockPath).toUri();
		doReturn(flags).when(channel).parseOptions(options);
        mockStatic(GlusterFileAttributes.class);
		when(GlusterFileAttributes.parseAttrs(attrs)).thenReturn(mode);

		mockStatic(GLFS.class);
		if (null != option) {
			when(GLFS.glfs_creat(volptr, path, flags, mode)).thenReturn(createptr);
		} else {
			when(GLFS.glfs_open(volptr, path, flags)).thenReturn(openptr);
		}

		channel.init(mockFileSystem, mockPath, options, attrs);

		assertEquals(mockFileSystem, channel.getFileSystem());
		assertEquals(mockPath, channel.getPath());
		assertEquals(options, channel.getOptions());
		assertEquals(null, channel.getAttrs());

		verify(mockFileSystem).getVolptr();
		verify(mockPath).toUri();
		verify(channel).parseOptions(options);
        verifyStatic();
		GlusterFileAttributes.parseAttrs(attrs);

		if (null != option) {
			verifyStatic();
			GLFS.glfs_creat(volptr, path, flags, mode);
		} else {
			verifyStatic();
			GLFS.glfs_open(volptr, path, flags);
		}
	}

	@Test
	public void testParseOptions() {
		Set<StandardOpenOption> options = new HashSet<StandardOpenOption>();
		options.add(StandardOpenOption.APPEND);
		options.add(StandardOpenOption.WRITE);

		int result = channel.parseOptions(options);

		assertEquals(GlusterOpenOption.O_RDWR | GlusterOpenOption.O_APPEND, result);
	}

	@Test
	public void testRead1Arg() throws IOException {
		doNothing().when(channel).guardClosed();
		long fileptr = 1234l;
		channel.setFileptr(fileptr);

		byte[] bytes = new byte[]{'a', 'b', 'c'};
		long bufferLength = bytes.length;
		long offset = 4;
		channel.setPosition(offset);

		mockStatic(GLFS.class);
		when(GLFS.glfs_read(fileptr, bytes, bufferLength, 0)).thenReturn(bufferLength);

		doReturn(bytes).when(mockBuffer).array();
		doReturn(mockBuffer).when(mockBuffer).position((int) bufferLength);

		int read = channel.read(mockBuffer);

		assertEquals(bufferLength, read);

		verify(channel).guardClosed();
		verify(mockBuffer).array();
		verify(mockBuffer).position((int) bufferLength);
		assertEquals(bufferLength + offset, channel.getPosition());

		verifyStatic();
		GLFS.glfs_read(fileptr, bytes, bufferLength, 0);
	}

	@Test(expected = IOException.class)
	public void testRead1Arg_whenReadFails() throws IOException {
		doNothing().when(channel).guardClosed();
		long fileptr = 1234l;
		channel.setFileptr(fileptr);

		byte[] bytes = new byte[]{'a', 'b', 'c'};
		long bufferLength = bytes.length;
		long offset = 4;
		channel.setPosition(offset);

		mockStatic(GLFS.class);
		when(GLFS.glfs_read(fileptr, bytes, bufferLength, 0)).thenReturn(-1L);

		doReturn(bytes).when(mockBuffer).array();

		channel.read(mockBuffer);
	}

	@Test
	public void testRead3Arg() throws IOException {
		doNothing().when(channel).guardClosed();
		Set<StandardOpenOption> options = new HashSet<>();
		options.add(StandardOpenOption.READ);
		channel.setOptions(options);
		ByteBuffer[] buffers = new ByteBuffer[2];
		buffers[0] = mockBuffer;
		buffers[1] = mockBuffer;
		channel.setPosition(0);
		int offset = 0;
		int length = 2;
		doReturn(10L).when(channel).readHelper(buffers, offset, length);
		long read = channel.read(buffers, offset, length);

		verify(channel).readHelper(buffers, offset, length);
		verify(channel).guardClosed();
		assertEquals(channel.position(), read);
	}

	@Test(expected = ClosedChannelException.class)
	public void testRead3Arg_whenClosed() throws IOException {
		channel.setClosed(true);
		ByteBuffer[] buffers = new ByteBuffer[2];
		buffers[0] = mockBuffer;
		buffers[1] = mockBuffer;
		int offset = 0;
		int length = 2;
		channel.read(buffers, offset, length);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testRead3Arg_whenLengthTooSmall() throws IOException {
		read3ArgLengthOffsetTestHelper(true, false);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testRead3Arg_whenLengthTooBig() throws IOException {
		read3ArgLengthOffsetTestHelper(true, true);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testRead3Arg_whenOffsetTooSmall() throws IOException {
		read3ArgLengthOffsetTestHelper(false, false);
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testRead3Arg_whenOffsetTooBig() throws IOException {
		read3ArgLengthOffsetTestHelper(false, true);
	}

	private void read3ArgLengthOffsetTestHelper(boolean testingLength, boolean testingTooBig) throws IOException {
		doNothing().when(channel).guardClosed();
		ByteBuffer[] buffers = new ByteBuffer[2];
		buffers[0] = mockBuffer;
		buffers[1] = mockBuffer;
		int offset = 0;
		int length = 2;

		if (testingLength) {
			if (testingTooBig) {
				length = 3;
			} else {
				length = -1;
			}
		} else {
			if (testingTooBig) {
				offset = 3;
			} else {
				offset = 1;
			}
		}

		channel.read(buffers, offset, length);
	}

	@Test(expected = NonReadableChannelException.class)
	public void testRead3Arg_whenNonReadableChannel() throws IOException {
		doNothing().when(channel).guardClosed();
		ByteBuffer[] buffers = new ByteBuffer[2];
		buffers[0] = mockBuffer;
		buffers[1] = mockBuffer;
		int offset = 0;
		int length = 2;
		channel.read(buffers, offset, length);
	}

	@Test
	public void testReadHelper() throws IOException {
		long fileptr = 1234L;
		channel.setFileptr(fileptr);

		ByteBuffer[] buffers = new ByteBuffer[2];
		buffers[0] = mockBuffer;
		buffers[1] = mockBuffer;
		int offset = 0;
		int length = 2;

		byte[] bytes = new byte[]{'h', 'e', 'l', 'l', 'o'};
		doReturn(bytes).when(mockBuffer).array();

		when(mockBuffer.remaining()).thenReturn(5, 0, 5, 0);

		mockStatic(GLFS.class);
		when(GLFS.glfs_read(fileptr, bytes, 5, 0)).thenReturn(5L);
		doReturn(mockBuffer).when(mockBuffer).position(5);

		channel.readHelper(buffers, offset, length);

		verify(mockBuffer, times(2)).position(5);
		verifyStatic(times(2));
		GLFS.glfs_read(fileptr, bytes, 5, 0);
		verify(mockBuffer, times(4)).remaining();
		verify(mockBuffer, times(2)).array();
	}

	@Test(expected = IOException.class)
	public void testReadHelper_whenReadFails() throws IOException {
		long fileptr = 1234L;
		channel.setFileptr(fileptr);

		ByteBuffer[] buffers = new ByteBuffer[2];
		buffers[0] = mockBuffer;
		buffers[1] = mockBuffer;
		int offset = 0;
		int length = 2;

		byte[] bytes = new byte[]{'h', 'e', 'l', 'l', 'o'};
		doReturn(bytes).when(mockBuffer).array();

		when(mockBuffer.remaining()).thenReturn(5, 0, 5, 0);

		mockStatic(GLFS.class);
		when(GLFS.glfs_read(fileptr, bytes, 5, 0)).thenReturn(-1L);

		channel.readHelper(buffers, offset, length);
	}

	@Test
	public void testReadHelper_whenEndOfStreamAndTotalReadZero() throws IOException {
		long fileptr = 1234L;
		channel.setFileptr(fileptr);

		ByteBuffer[] buffers = new ByteBuffer[2];
		buffers[0] = mockBuffer;
		buffers[1] = mockBuffer;
		int offset = 0;
		int length = 2;

		byte[] bytes = new byte[]{'h', 'e', 'l', 'l', 'o'};
		doReturn(bytes).when(mockBuffer).array();

		when(mockBuffer.remaining()).thenReturn(5, 0, 5, 0);

		mockStatic(GLFS.class);
		when(GLFS.glfs_read(fileptr, bytes, 5, 0)).thenReturn(0L);
		doReturn(mockBuffer).when(mockBuffer).position(0);

		long ret = channel.readHelper(buffers, offset, length);

		assertEquals(ret, -1);
		verify(mockBuffer).position(0);
		verifyStatic();
		GLFS.glfs_read(fileptr, bytes, 5, 0);
		verify(mockBuffer).remaining();
		verify(mockBuffer).array();
	}

	@Test
	public void testRead2Arg() throws IOException {
		long position = 5L;
		long fileptr = 1234L;
		long defaultPosition = 0L;
		channel.setFileptr(fileptr);
		channel.setPosition(defaultPosition);

		doNothing().when(channel).guardClosed();

		Set<StandardOpenOption> options = new HashSet<>();
		options.add(StandardOpenOption.READ);
		channel.setOptions(options);

		doReturn(10L).when(channel).size();

		mockStatic(GLFS.class);
		when(GLFS.glfs_lseek(fileptr, position, 0)).thenReturn(0);

		byte[] bytes = new byte[]{'h', 'e', 'l', 'l', 'o'};
		doReturn(bytes).when(mockBuffer).array();

		long expectedRet = 7L;
		when(GLFS.glfs_read(fileptr, bytes, bytes.length, 0)).thenReturn(expectedRet);

		when(GLFS.glfs_lseek(fileptr, defaultPosition, 0)).thenReturn(0);

		long ret = channel.read(mockBuffer, position);

		assertEquals(ret, expectedRet);
		verifyStatic();
		GLFS.glfs_lseek(fileptr, defaultPosition, 0);
		verifyStatic();
		GLFS.glfs_read(fileptr, bytes, bytes.length, 0);
		verify(mockBuffer).array();
		verifyStatic();
		GLFS.glfs_lseek(fileptr, position, 0);
		verify(channel).size();
		verify(channel).guardClosed();
	}

	@Test(expected = ClosedChannelException.class)
	public void testRead2Arg_whenClosed() throws IOException {
		long position = 5L;
		channel.setClosed(true);
		channel.read(mockBuffer, position);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRead2Arg_whenPositionNegative() throws IOException {
		long position = -1L;

		doNothing().when(channel).guardClosed();

		channel.read(mockBuffer, position);
	}

	@Test(expected = NonReadableChannelException.class)
	public void testRead2Arg_whenNonReadableChannel() throws IOException {
		long position = 5L;

		doNothing().when(channel).guardClosed();

		channel.read(mockBuffer, position);
	}

	@Test
	public void testRead2Arg_whenPositionTooBig() throws IOException {
		long position = 5L;

		doNothing().when(channel).guardClosed();

		Set<StandardOpenOption> options = new HashSet<>();
		options.add(StandardOpenOption.READ);
		channel.setOptions(options);

		doReturn(4L).when(channel).size();

		long expectedRet = -1L;

		long ret = channel.read(mockBuffer, position);

		assertEquals(ret, expectedRet);
		verify(channel).size();
		verify(channel).guardClosed();
	}

	@Test(expected = IOException.class)
	public void testRead2Arg_whenFirstSeekFails() throws IOException {
		long position = 5L;
		long fileptr = 1234L;
		channel.setFileptr(fileptr);

		doNothing().when(channel).guardClosed();

		Set<StandardOpenOption> options = new HashSet<>();
		options.add(StandardOpenOption.READ);
		channel.setOptions(options);

		doReturn(10L).when(channel).size();

		mockStatic(GLFS.class);
		when(GLFS.glfs_lseek(fileptr, position, 0)).thenReturn(-1);

		channel.read(mockBuffer, position);

		verifyStatic();
		GLFS.glfs_lseek(fileptr, position, 0);
		verify(channel).size();
		verify(channel).guardClosed();
	}

	@Test(expected = IOException.class)
	public void testRead2Arg_whenReadFails() throws IOException {
		long position = 5L;
		long fileptr = 1234L;
		long defaultPosition = 0L;
		channel.setFileptr(fileptr);
		channel.setPosition(defaultPosition);

		doNothing().when(channel).guardClosed();

		Set<StandardOpenOption> options = new HashSet<>();
		options.add(StandardOpenOption.READ);
		channel.setOptions(options);

		doReturn(10L).when(channel).size();

		mockStatic(GLFS.class);
		when(GLFS.glfs_lseek(fileptr, position, 0)).thenReturn(0);

		byte[] bytes = new byte[]{'h', 'e', 'l', 'l', 'o'};
		doReturn(bytes).when(mockBuffer).array();

		long expectedRet = -1;
		when(GLFS.glfs_read(fileptr, bytes, bytes.length, 0)).thenReturn(expectedRet);

		channel.read(mockBuffer, position);

		verifyStatic();
		GLFS.glfs_read(fileptr, bytes, bytes.length, 0);
		verify(mockBuffer).array();
		verifyStatic();
		GLFS.glfs_lseek(fileptr, position, 0);
		verify(channel).size();
		verify(channel).guardClosed();
	}

	@Test(expected = IOException.class)
	public void testRead2Arg_whenSecondSeekFails() throws IOException {
		long position = 5L;
		long fileptr = 1234L;
		long defaultPosition = 0L;
		channel.setFileptr(fileptr);
		channel.setPosition(defaultPosition);

		doNothing().when(channel).guardClosed();

		Set<StandardOpenOption> options = new HashSet<>();
		options.add(StandardOpenOption.READ);
		channel.setOptions(options);

		doReturn(10L).when(channel).size();

		mockStatic(GLFS.class);
		when(GLFS.glfs_lseek(fileptr, position, 0)).thenReturn(0);

		byte[] bytes = new byte[]{'h', 'e', 'l', 'l', 'o'};
		doReturn(bytes).when(mockBuffer).array();

		long expectedRet = 7L;
		when(GLFS.glfs_read(fileptr, bytes, bytes.length, 0)).thenReturn(expectedRet);

		when(GLFS.glfs_lseek(fileptr, defaultPosition, 0)).thenReturn(-1);

		channel.read(mockBuffer, position);

		verifyStatic();
		GLFS.glfs_lseek(fileptr, defaultPosition, 0);
		verifyStatic();
		GLFS.glfs_read(fileptr, bytes, bytes.length, 0);
		verify(mockBuffer).array();
		verifyStatic();
		GLFS.glfs_lseek(fileptr, position, 0);
		verify(channel).size();
		verify(channel).guardClosed();
	}

    @Test
    public void testWrite1Arg() throws IOException {
        doNothing().when(channel).guardClosed();
        long fileptr = 1234L;
        channel.setFileptr(fileptr);

        byte[] bytes = new byte[]{'a', 'b'};
        int bufferLength = bytes.length;

        mockStatic(GLFS.class);
        when(GLFS.glfs_write(fileptr, bytes, bufferLength, 0)).thenReturn(bufferLength);

        doReturn(bytes).when(mockBuffer).array();
        doReturn(null).when(mockBuffer).position(bufferLength);

        int written = channel.write(mockBuffer);

        assertEquals(bufferLength, written);

        verify(channel).guardClosed();
        verify(mockBuffer).array();
        verify(mockBuffer).position(bufferLength);

        verifyStatic();
        GLFS.glfs_write(fileptr, bytes, bufferLength, 0);
    }

    @Test
    public void testWrite3Arg() throws IOException {
        doNothing().when(channel).guardClosed();
        long fileptr = 1234L;
        channel.setFileptr(fileptr);
        Set<? extends OpenOption> mockOptions = Mockito.mock(Set.class);
        channel.setOptions(mockOptions);
        doReturn(true).when(mockOptions).contains(StandardOpenOption.WRITE);

        byte[] bytes1 = new byte[10];
        byte[] bytes2 = new byte[10];
        ByteBuffer buffer1 = ByteBuffer.wrap(bytes1);
        ByteBuffer buffer2 = ByteBuffer.wrap(bytes2);
        ByteBuffer[] buffers = new ByteBuffer[]{buffer1, buffer2};
        int length = 2;
        int offset = 0;

        mockStatic(GLFS.class);
        when(GLFS.glfs_write(fileptr, bytes1, 10, 0)).thenReturn(10);
        when(GLFS.glfs_write(fileptr, bytes2, 10, 0)).thenReturn(10);

        long ret = channel.write(buffers, offset, length);

        assertEquals(ret, 20);

        verifyStatic(times(2));
        GLFS.glfs_write(fileptr, bytes1, 10, 0);

        verify(mockOptions).contains(StandardOpenOption.WRITE);
        verify(channel).guardClosed();
    }

    @Test(expected = NonWritableChannelException.class)
    public void testWrite3Arg_whenChannelNotOpenedForWrite() throws IOException {
        doNothing().when(channel).guardClosed();
        long fileptr = 1234L;
        channel.setFileptr(fileptr);
        Set<? extends OpenOption> mockOptions = Mockito.mock(Set.class);
        channel.setOptions(mockOptions);
        doReturn(false).when(mockOptions).contains(StandardOpenOption.WRITE);

        int length = 2;
        int offset = 0;
        ByteBuffer buffer1 = Mockito.mock(ByteBuffer.class);
        ByteBuffer buffer2 = Mockito.mock(ByteBuffer.class);
        ByteBuffer[] buffers = new ByteBuffer[]{buffer1, buffer2};

        channel.write(buffers, offset, length);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWrite3Arg_whenLengthNegative() throws IOException {
        testWrite3Arg_helper(true, false);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWrite3Arg_whenLengthTooBig() throws IOException {
        testWrite3Arg_helper(true, true);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWrite3Arg_whenOffsetNegative() throws IOException {
        testWrite3Arg_helper(false, false);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWrite3Arg_whenOffsetTooBig() throws IOException {
        testWrite3Arg_helper(false, true);
    }

    private void testWrite3Arg_helper(boolean testingLength, boolean testingTooBig) throws IOException {
        doNothing().when(channel).guardClosed();

        int length;
        int offset;
        if (testingLength) {
            offset = 0;
            if (testingTooBig) {
                length = 3;
            } else {
                length = -1;
            }
        } else {
            length = 2;
            if (testingTooBig) {
                offset = 3;
            } else {
                offset = -1;
            }
        }
        ByteBuffer buffer1 = Mockito.mock(ByteBuffer.class);
        ByteBuffer buffer2 = Mockito.mock(ByteBuffer.class);
        ByteBuffer[] buffers = new ByteBuffer[]{buffer1, buffer2};

        channel.write(buffers, offset, length);
    }

    @Test(expected = ClosedChannelException.class)
    public void testWrite3Arg_whenClosedChannel() throws IOException {
        channel.setClosed(true);

        int length = 2;
        int offset = 0;
        ByteBuffer buffer1 = Mockito.mock(ByteBuffer.class);
        ByteBuffer buffer2 = Mockito.mock(ByteBuffer.class);
        ByteBuffer[] buffers = new ByteBuffer[]{buffer1, buffer2};

        channel.write(buffers, offset, length);
    }

    @Test
    public void testWrite2Arg_positionLessThanSize() throws IOException {
        testWrite2Arg_helper(false);
    }

    @Test
    public void testWrite2Arg_positionGreaterThanSize() throws IOException {
        testWrite2Arg_helper(true);
    }

    private void testWrite2Arg_helper(boolean testingPositionSize) throws IOException {
        doNothing().when(channel).guardClosed();
        long fileptr = 1234L;
        channel.setFileptr(fileptr);
        Set<? extends OpenOption> mockOptions = Mockito.mock(Set.class);
        channel.setOptions(mockOptions);
        doReturn(true).when(mockOptions).contains(StandardOpenOption.WRITE);
        channel.setPosition(0L);
        doReturn(3L).when(channel).size();

        byte[] bytes = new byte[10];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long position = 2L;

        if (testingPositionSize) {
            position = 3L;
            mockStatic(Arrays.class);
            when(Arrays.copyOf(bytes, bytes.length)).thenReturn(bytes);
            mockStatic(ByteBuffer.class);
            when(ByteBuffer.wrap(bytes)).thenReturn(buffer);
        }

        mockStatic(GLFS.class);
        when(GLFS.glfs_write(fileptr, bytes, 10, 0)).thenReturn(10);
        when(GLFS.glfs_lseek(fileptr, position, 0)).thenReturn(0);
        when(GLFS.glfs_lseek(fileptr, 0L, 0)).thenReturn(0);

        int ret = channel.write(buffer, position);

        assertEquals(ret, 10);

        verifyStatic();
        GLFS.glfs_lseek(fileptr, 0L, 0);
        verifyStatic();
        GLFS.glfs_lseek(fileptr, position, 0);
        verifyStatic();
        GLFS.glfs_write(fileptr, bytes, 10, 0);

        if (testingPositionSize) {
            verifyStatic();
            ByteBuffer.wrap(bytes);
            verifyStatic();
            Arrays.copyOf(bytes, bytes.length);
        }

        verify(channel, times(testingPositionSize ? 2 : 1)).size();
        verify(mockOptions).contains(StandardOpenOption.WRITE);
        verify(channel).guardClosed();
    }

    @Test(expected = IOException.class)
    public void testWrite2Arg_firstSeekFails() throws IOException {
        doNothing().when(channel).guardClosed();
        long fileptr = 1234L;
        channel.setFileptr(fileptr);
        Set<? extends OpenOption> mockOptions = Mockito.mock(Set.class);
        channel.setOptions(mockOptions);
        doReturn(true).when(mockOptions).contains(StandardOpenOption.WRITE);
        channel.setPosition(0L);
        doReturn(3L).when(channel).size();

        byte[] bytes = new byte[10];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long position = 2L;

        mockStatic(GLFS.class);
        when(GLFS.glfs_write(fileptr, bytes, 10, 0)).thenReturn(10);
        when(GLFS.glfs_lseek(fileptr, position, 0)).thenReturn(0);
        when(GLFS.glfs_lseek(fileptr, 0L, 0)).thenReturn(-1);

        channel.write(buffer, position);
    }

    @Test(expected = IOException.class)
    public void testWrite2Arg_secondSeekFails() throws IOException {
        doNothing().when(channel).guardClosed();
        long fileptr = 1234L;
        channel.setFileptr(fileptr);
        Set<? extends OpenOption> mockOptions = Mockito.mock(Set.class);
        channel.setOptions(mockOptions);
        doReturn(true).when(mockOptions).contains(StandardOpenOption.WRITE);
        channel.setPosition(0L);
        doReturn(3L).when(channel).size();

        byte[] bytes = new byte[10];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long position = 2L;

        mockStatic(GLFS.class);
        when(GLFS.glfs_write(fileptr, bytes, 10, 0)).thenReturn(10);
        when(GLFS.glfs_lseek(fileptr, position, 0)).thenReturn(-1);

        channel.write(buffer, position);
    }

    @Test(expected = NonWritableChannelException.class)
    public void testWrite2Arg_whenChannelNonWritable() throws IOException {
        doNothing().when(channel).guardClosed();
        Set<? extends OpenOption> mockOptions = Mockito.mock(Set.class);
        channel.setOptions(mockOptions);
        doReturn(false).when(mockOptions).contains(StandardOpenOption.WRITE);

        ByteBuffer buffer = Mockito.mock(ByteBuffer.class);
        long position = 2L;

        channel.write(buffer, position);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrite2Arg_positionInvalid() throws IOException {
        doNothing().when(channel).guardClosed();

        ByteBuffer buffer = Mockito.mock(ByteBuffer.class);
        long position = -1L;

        channel.write(buffer, position);
    }

    @Test(expected = ClosedChannelException.class)
    public void testWrite2Arg_whenChannelClosed() throws IOException {
        channel.setClosed(true);

        ByteBuffer buffer = Mockito.mock(ByteBuffer.class);
        long position = 0L;

        channel.write(buffer, position);
    }

	@Test
	public void testGuardClosed_whenNotClosed() throws ClosedChannelException {
		channel.setClosed(false);
		channel.guardClosed();
	}

	@Test(expected = ClosedChannelException.class)
	public void testGuardClosed_whenClosed() throws ClosedChannelException {
		channel.setClosed(true);
		channel.guardClosed();
	}

	@Test
	public void testGetPosition() throws IOException {
		doNothing().when(channel).guardClosed();
		long position = 12345l;
		channel.setPosition(position);
		long returnedPosition = channel.position();
		verify(channel).guardClosed();
		assertEquals(position, returnedPosition);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetPosition_whenNegative() throws IOException {
		long position = -1l;
		channel.position(position);
	}

	@Test
	public void testSetPosition() throws IOException {
		doNothing().when(channel).guardClosed();
		long fileptr = 123l;
		channel.setFileptr(fileptr);
		long position = 12345l;

		mockStatic(GLFS.class);
		when(GLFS.glfs_lseek(fileptr, position, 0)).thenReturn(0);
		FileChannel returnedChannel = channel.position(position);

		verify(channel).guardClosed();
		assertEquals(channel, returnedChannel);
		assertEquals(position, channel.getPosition());

		verifyStatic();
		GLFS.glfs_lseek(fileptr, position, 0);
	}

	@Test(expected = IOException.class)
	public void testForce_whenFailing() throws IOException {
		long fileptr = 1234l;
		channel.setFileptr(fileptr);

		mockStatic(GLFS.class);
		when(GLFS.glfs_fsync(fileptr)).thenReturn(-1);

		channel.force(true);
	}

	@Test
	public void testForce() throws IOException {
		doNothing().when(channel).guardClosed();
		long fileptr = 1234l;
		channel.setFileptr(fileptr);

		mockStatic(GLFS.class);
		when(GLFS.glfs_fsync(fileptr)).thenReturn(0);

		channel.force(true);
		verify(channel).guardClosed();
		verifyStatic();
		GLFS.glfs_fsync(fileptr);
	}

	@Test(expected = IOException.class)
	public void testImplCloseChannel_whenFailing() throws IOException {
		long fileptr = 1234l;
		channel.setFileptr(fileptr);

		mockStatic(GLFS.class);
		when(GLFS.glfs_close(fileptr)).thenReturn(1);

		channel.implCloseChannel();
	}

	@Test
	public void testImplCloseChannel_whenAlreadyClosed() throws IOException {
		channel.setClosed(true);
		long fileptr = 1234l;
		channel.setFileptr(fileptr);

		mockStatic(GLFS.class);
		when(GLFS.glfs_close(fileptr)).thenReturn(0);

		channel.implCloseChannel();

		assertTrue(channel.isClosed());

		verifyStatic(never());
		GLFS.glfs_close(fileptr);
	}

	@Test
	public void testImplCloseChannel() throws IOException {
		long fileptr = 1234l;
		channel.setFileptr(fileptr);

		mockStatic(GLFS.class);
		when(GLFS.glfs_close(fileptr)).thenReturn(0);

		channel.implCloseChannel();

		assertTrue(channel.isClosed());

		verifyStatic();
		GLFS.glfs_close(fileptr);
	}

	@Test
	public void testSize() throws Exception {
		long fileptr = 1234l;
		channel.setFileptr(fileptr);

		long actualSize = 321l;
		stat stat = new stat();
		stat.st_size = actualSize;

		PowerMockito.whenNew(stat.class).withNoArguments().thenReturn(stat);

		mockStatic(GLFS.class);
		when(GLFS.glfs_fstat(fileptr, stat)).thenReturn(0);

		long size = channel.size();

		assertEquals(actualSize, size);

		verifyStatic();
		GLFS.glfs_fstat(fileptr, stat);
	}

	@Test(expected = IOException.class)
	public void testSize_whenFailing() throws Exception {
		long fileptr = 1234l;
		channel.setFileptr(fileptr);

		long actualSize = 321l;
		stat stat = new stat();
		stat.st_size = actualSize;

		PowerMockito.whenNew(stat.class).withNoArguments().thenReturn(stat);

		mockStatic(GLFS.class);
		when(GLFS.glfs_fstat(fileptr, stat)).thenReturn(-1);

		long size = channel.size();
	}

}
