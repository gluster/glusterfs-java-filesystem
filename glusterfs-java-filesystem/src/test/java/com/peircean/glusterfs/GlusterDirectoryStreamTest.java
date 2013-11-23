package com.peircean.glusterfs;

import com.peircean.libgfapi_jni.internal.GLFS;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GlusterDirectoryStream.class, GLFS.class})
public class GlusterDirectoryStreamTest {
    private long dirHandle = 12345l;

    private long fsHandle = 56789l;

    @Mock
    private GlusterDirectoryIterator mockIterator;

    @Mock
    private GlusterPath mockPath;

    @Mock
    private GlusterFileSystem mockFileSystem;

    @Mock
    private DirectoryStream.Filter<? super Path> mockFilter;

    @Spy
    private GlusterDirectoryStream stream = new GlusterDirectoryStream();

    @Test(expected = IllegalStateException.class)
    public void testOpenDirectory_whenAlreadyOpen() {
        stream.setDirHandle(1);
        stream.open(mockPath);
    }

    @Test
    public void testOpenDirectory() {
        String fakePath = "/foo/baz";
        doReturn(mockFileSystem).when(mockPath).getFileSystem();
        doReturn(fsHandle).when(mockFileSystem).getVolptr();
        doReturn(fakePath).when(mockPath).getString();

        mockStatic(GLFS.class);
        when(GLFS.glfs_opendir(fsHandle, fakePath)).thenReturn(dirHandle);

        stream.open(mockPath);

        assertEquals(dirHandle, stream.getDirHandle());
        assertEquals(false, stream.isClosed());
        assertEquals(mockPath, stream.getDir());
        verify(mockPath).getFileSystem();
        verify(mockFileSystem).getVolptr();
        verify(mockPath).getString();
        verifyStatic();
        GLFS.glfs_opendir(fsHandle, fakePath);
    }

    @Test(expected = IllegalStateException.class)
    public void testIterator_whenAlreadyIterating() {
        stream.setIterator(mockIterator);
        stream.iterator();
    }

    @Test(expected = IllegalStateException.class)
    public void testIterator_whenClosed() {
        stream.setClosed(true);
        stream.iterator();
    }

    @Test
    public void testIterator() throws Exception {
        stream.setClosed(false);
        stream.setFilter(mockFilter);
        whenNew(GlusterDirectoryIterator.class).
                withNoArguments().thenReturn(mockIterator);
        stream.setDirHandle(dirHandle);
        Iterator<Path> iterator = stream.iterator();
        verifyNew(GlusterDirectoryIterator.class).withNoArguments();
        assertEquals(mockIterator, iterator);
        assertEquals(mockIterator, stream.getIterator());
        verify(mockIterator).setStream(stream);
        verify(mockIterator).setFilter(mockFilter);
    }

    @Test
    public void testClose_whenAlreadyClosed() throws IOException {
        stream.setDirHandle(dirHandle);
        stream.setClosed(true);
        mockStatic(GLFS.class);

        stream.close();

        verifyStatic(Mockito.never());
        GLFS.glfs_close(dirHandle);
    }

    @Test
    public void testClose() throws IOException {
        stream.setDirHandle(dirHandle);
        mockStatic(GLFS.class);
        Mockito.when(GLFS.glfs_close(dirHandle)).thenReturn(0);

        stream.close();

        verifyStatic();
        GLFS.glfs_close(dirHandle);
    }

}
