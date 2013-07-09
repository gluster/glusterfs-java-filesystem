package com.peircean.glusterfs;

import junit.framework.TestCase;
import org.fusesource.glfsjni.internal.GLFS;
import org.fusesource.glfsjni.internal.GlusterOpenOption;
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

}
