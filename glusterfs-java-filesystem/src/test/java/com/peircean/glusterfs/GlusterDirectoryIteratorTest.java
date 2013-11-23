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

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
    @Mock
    private DirectoryStream.Filter<? super Path> mockFilter;
    @Spy
    private GlusterDirectoryIterator iterator = new GlusterDirectoryIterator();
    private long dirHandle = 12345l;

    @Test
    public void testHasNext_whenFilter() throws Exception {
        iterator.setFilter(mockFilter);
        iterator.setNextPath(fakeResultPath);
        when(mockFilter.accept(fakeResultPath)).thenReturn(false).thenReturn(true);
        mockNextDirent.d_ino = 1;
        iterator.setNext(mockNextDirent);
        doNothing().when(iterator).advance();
        assertTrue(iterator.hasNext());
        verify(iterator, times(2)).advance();
        verify(mockFilter, times(2)).accept(fakeResultPath);
    }

    @Test
    public void testHasNext_whenFilter_andNoNext() throws Exception {
        iterator.setFilter(mockFilter);
        iterator.setNextPath(fakeResultPath);
        when(mockFilter.accept(fakeResultPath)).thenReturn(false).thenReturn(true);
        mockNextDirent.d_ino = 0;
        iterator.setNext(mockNextDirent);
        doNothing().when(iterator).advance();
        assertFalse(iterator.hasNext());
        verify(iterator).advance();
        verify(mockFilter).accept(fakeResultPath);
    }

    @Test
    public void testHasNext_whenNoFilter_andNoNext() throws Exception {
        mockNextDirent.d_ino = 0;
        iterator.setNext(mockNextDirent);
        doNothing().when(iterator).advance();
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testHasNext_whenNoFilter() throws Exception {
        mockNextDirent.d_ino = 1;
        iterator.setNext(mockNextDirent);
        doNothing().when(iterator).advance();
        assertTrue(iterator.hasNext());
    }

    @Test
    public void testAdvance() throws Exception {
        doReturn(dirHandle).when(mockStream).getDirHandle();
        iterator.setStream(mockStream);
        PowerMockito.whenNew(dirent.class).withNoArguments().thenReturn(mockCurrentDirent).thenReturn(mockNextDirent);

        long nextPtr = 4444l;
        PowerMockito.mockStatic(dirent.class);
        Mockito.when(dirent.malloc(dirent.SIZE_OF)).thenReturn(nextPtr);

        PowerMockito.doNothing().when(dirent.class);
        dirent.memmove(mockNextDirent, nextPtr, dirent.SIZE_OF);
        PowerMockito.doNothing().when(dirent.class);
        dirent.free(nextPtr);

        PowerMockito.mockStatic(GLFS.class);
        PowerMockito.when(GLFS.glfs_readdir_r(dirHandle, mockCurrentDirent, nextPtr)).thenReturn(0);

        Mockito.doReturn(mockPath).when(mockStream).getDir();
        String stringPath = "foo";
        Mockito.doReturn(stringPath).when(mockCurrentDirent).getName();
        Mockito.doReturn(fakeResultPath).when(mockPath).resolve(stringPath);

        iterator.advance();

        assertEquals(fakeResultPath, iterator.getNextPath());

        verify(mockCurrentDirent).getName();
        verify(mockPath).resolve(stringPath);
        verify(mockStream).getDir();


        verify(mockStream).getDirHandle();
        PowerMockito.verifyNew(dirent.class, times(2)).withNoArguments();
        PowerMockito.verifyStatic();
        dirent.memmove(mockNextDirent, nextPtr, dirent.SIZE_OF);
        dirent.free(nextPtr);
    }

    @Test(expected = NoSuchElementException.class)
    public void testNext_whenNoNext() {
        iterator.next();
    }

    @Test
    public void testNext() {
        iterator.setNextPath(fakeResultPath);
        GlusterPath path = iterator.next();
        assertEquals(fakeResultPath, path);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() {
        iterator.remove();
    }
}
