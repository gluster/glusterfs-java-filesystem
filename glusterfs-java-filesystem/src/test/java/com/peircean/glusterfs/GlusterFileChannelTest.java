package com.peircean.glusterfs;

import com.peircean.libgfapi_jni.internal.GLFS;
import com.peircean.libgfapi_jni.internal.GlusterOpenOption;
import com.peircean.libgfapi_jni.internal.structs.stat;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GLFS.class, GlusterFileChannel.class})
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
        doReturn(mode).when(channel).parseAttrs(attrs);

        PowerMockito.mockStatic(GLFS.class);
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
        verify(channel).parseAttrs(attrs);

        if (null != option) {
            PowerMockito.verifyStatic();
            GLFS.glfs_creat(volptr, path, flags, mode);
        } else {
            PowerMockito.verifyStatic();
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
    public void testParseAttributes() {
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrwxrwx");
        FileAttribute<Set<PosixFilePermission>> attribute = PosixFilePermissions.asFileAttribute(permissions);
        int mode = channel.parseAttrs(attribute);
        assertEquals(0777, mode);
    }

    @Test
    public void testRead1Arg() throws IOException {
        doNothing().when(channel).guardClosed();
        doNothing().when(channel).guardReadable();
        long fileptr = 1234l;
        channel.setFileptr(fileptr);

        byte[] bytes = new byte[]{'a', 'b', 'c'};
        long bufferLength = bytes.length;
        long offset = 4;
        channel.setPosition(offset);

        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_read(fileptr, bytes, bufferLength, 0)).thenReturn(bufferLength);

        doReturn(bytes).when(mockBuffer).array();

        int read = channel.read(mockBuffer);

        assertEquals(bufferLength, read);

        verify(channel).guardClosed();
        verify(channel).guardReadable();
        verify(mockBuffer).array();
        assertEquals(bufferLength + offset, channel.getPosition());

        PowerMockito.verifyStatic();
        GLFS.glfs_read(fileptr, bytes, bufferLength, 0);
    }

    @Test
    public void testWrite1Arg() throws IOException {
        doNothing().when(channel).guardClosed();
        long fileptr = 1234l;
        channel.setFileptr(fileptr);

        byte[] bytes = new byte[]{'a', 'b'};
        int bufferLength = bytes.length;

        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_write(fileptr, bytes, bufferLength, 0)).thenReturn(bufferLength);

        doReturn(bytes).when(mockBuffer).array();
        doReturn(null).when(mockBuffer).position(bufferLength);

        int written = channel.write(mockBuffer);

        assertEquals(bufferLength, written);

        verify(channel).guardClosed();
        verify(mockBuffer).array();
        verify(mockBuffer).position(bufferLength);

        PowerMockito.verifyStatic();
        GLFS.glfs_write(fileptr, bytes, bufferLength, 0);
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

    @Test(expected = NonReadableChannelException.class)
    public void testGuardReadable_whenNotReadable() {
        channel.guardReadable();
    }

    @Test
    public void testGuardReadable_whenReadable() {
        Set<OpenOption> openOptions = new HashSet<>();
        openOptions.add(StandardOpenOption.READ);
        channel.setOptions(openOptions);

        channel.guardReadable();
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

        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_lseek(fileptr, position, 0)).thenReturn(0);
        FileChannel returnedChannel = channel.position(position);

        verify(channel).guardClosed();
        assertEquals(channel, returnedChannel);
        assertEquals(position, channel.getPosition());

        PowerMockito.verifyStatic();
        GLFS.glfs_lseek(fileptr, position, 0);
    }
    
    @Test(expected = IOException.class)
    public void testForce_whenFailing() throws IOException {
        long fileptr = 1234l;
        channel.setFileptr(fileptr);
        
        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_fsync(fileptr)).thenReturn(-1);
        
        channel.force(true);
    }
    
    @Test
    public void testForce() throws IOException {
        doNothing().when(channel).guardClosed();
        long fileptr = 1234l;
        channel.setFileptr(fileptr);

        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_fsync(fileptr)).thenReturn(0);

        channel.force(true);
        verify(channel).guardClosed();
        PowerMockito.verifyStatic();
        GLFS.glfs_fsync(fileptr);
    }

    @Test(expected = IOException.class)
    public void testImplCloseChannel_whenFailing() throws IOException {
        long fileptr = 1234l;
        channel.setFileptr(fileptr);

        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_close(fileptr)).thenReturn(1);

        channel.implCloseChannel();
    }

    @Test
    public void testImplCloseChannel_whenAlreadyClosed() throws IOException {
        channel.setClosed(true);
        long fileptr = 1234l;
        channel.setFileptr(fileptr);

        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_close(fileptr)).thenReturn(0);

        channel.implCloseChannel();

        assertTrue(channel.isClosed());

        PowerMockito.verifyStatic(never());
        GLFS.glfs_close(fileptr);
    }

    @Test
    public void testImplCloseChannel() throws IOException {
        long fileptr = 1234l;
        channel.setFileptr(fileptr);

        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_close(fileptr)).thenReturn(0);

        channel.implCloseChannel();

        assertTrue(channel.isClosed());

        PowerMockito.verifyStatic();
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

        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_fstat(fileptr, stat)).thenReturn(0);

        long size = channel.size();

        assertEquals(actualSize, size);

        PowerMockito.verifyStatic();
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

        PowerMockito.mockStatic(GLFS.class);
        when(GLFS.glfs_fstat(fileptr, stat)).thenReturn(-1);

        long size = channel.size();
    }

}
