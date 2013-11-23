package com.peircean.glusterfs;

import com.peircean.libgfapi_jni.internal.GLFS;
import com.peircean.libgfapi_jni.internal.structs.dirent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GLFS.class, GlusterDirectoryIterator.class, dirent.class})
@SuppressStaticInitializationFor("com.peircean.libgfapi_jni.internal.structs.dirent")
public class GlusterDirectoryIteratorTest {

    @Mock
    private GlusterPath mockPath;
    @Mock
    private GlusterPath fakeResultPath;
    @Mock
    private GlusterFileSystem mockFileSystem;
    @Mock
    private GlusterDirectoryStream mockStream;
    @Mock
    private dirent mockCurrentDirent;
    @Mock
    private dirent mockNextDirent;
    @Spy
    private GlusterDirectoryIterator iterator = new GlusterDirectoryIterator();
    private long dirHandle = 12345l;

    @Test
    public void testHasNext_whenNoNext() throws Exception {
        helperTestHasNext(false);
    }

    @Test
    public void testHasNext() throws Exception {
        helperTestHasNext(true);
    }

    private void helperTestHasNext(boolean has) throws Exception {
        mockNextDirent.d_ino = (has ? 1 : 0);
        doReturn(dirHandle).when(mockStream).getDirHandle();
        iterator.setStream(mockStream);
        whenNew(dirent.class).withNoArguments().thenReturn(mockCurrentDirent).thenReturn(mockNextDirent);

        long nextPtr = 4444l;
        mockStatic(dirent.class);
        Mockito.when(dirent.malloc(dirent.SIZE_OF)).thenReturn(nextPtr);

        PowerMockito.doNothing().when(dirent.class);
        dirent.memmove(mockNextDirent, nextPtr, dirent.SIZE_OF);
        PowerMockito.doNothing().when(dirent.class);
        dirent.free(nextPtr);

        mockStatic(GLFS.class);
        PowerMockito.when(GLFS.glfs_readdir_r(dirHandle, mockCurrentDirent, nextPtr)).thenReturn(0);

        boolean ret = iterator.hasNext();

        assertTrue(ret == has);

        verify(mockStream).getDirHandle();
        verifyNew(dirent.class, times(2)).withNoArguments();
        verifyStatic();
        dirent.memmove(mockNextDirent, nextPtr, dirent.SIZE_OF);
        dirent.free(nextPtr);
    }

    @Test(expected = NoSuchElementException.class)
    public void testNext_whenNoNext() {
        iterator.next();
    }

    @Test
    public void testNext() {
        iterator.setStream(mockStream);
        iterator.setCurrent(mockCurrentDirent);

        Mockito.doReturn(mockPath).when(mockStream).getDir();
        String stringPath = "foo";
        Mockito.doReturn(stringPath).when(mockCurrentDirent).getName();
        Mockito.doReturn(fakeResultPath).when(mockPath).resolve(stringPath);

        GlusterPath path = iterator.next();

        assertEquals(fakeResultPath, path);

        verify(mockCurrentDirent).getName();
        verify(mockPath).resolve(stringPath);
        verify(mockStream).getDir();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() {
        iterator.remove();
    }
}
