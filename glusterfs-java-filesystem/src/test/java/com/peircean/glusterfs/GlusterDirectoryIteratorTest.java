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
        verify(mockFilter, never()).accept(fakeResultPath);
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
    public void testAdvance_whenNormalEntry() throws Exception {
        advanceHelper(0, true);
    }

    @Test
    public void testAdvance_skipSpecialEntryCurrent() throws Exception {
        advanceHelper(1, true);
    }

    @Test
    public void testAdvance_skipSpecialEntryParent() throws Exception {
        advanceHelper(1, false);
    }

    @Test
    public void testAdvance_skipSpecialEntryCurrentAndParent() throws Exception {
        advanceHelper(2, true);
    }

    /*
     * Type 0 = advancing on normal entry, so the boolean current value does not matter
     * Type 1 = advancing on either special entry "." or "..", differentiated by boolean current
     * Type 2 = advancing on both special entries "." and "..", so boolean current value does not matter
     */
    private void advanceHelper(int type, boolean current) throws Exception {
        doReturn(dirHandle).when(mockStream).getDirHandle();
        iterator.setStream(mockStream);
        switch (type) {
            case 1:
                PowerMockito.whenNew(dirent.class).withNoArguments().thenReturn(mockCurrentDirent, mockNextDirent,
                        mockCurrentDirent, mockNextDirent);
                break;
            case 2:
                PowerMockito.whenNew(dirent.class).withNoArguments().thenReturn(mockCurrentDirent, mockNextDirent,
                        mockCurrentDirent, mockNextDirent,
                        mockCurrentDirent, mockNextDirent);
                break;
            default:
                PowerMockito.whenNew(dirent.class).withNoArguments().thenReturn(mockCurrentDirent, mockNextDirent);
                break;
        }
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
        switch (type) {
            case 1:
                if (current) {
                    when(mockCurrentDirent.getName()).thenReturn(".", stringPath);
                } else {
                    when(mockCurrentDirent.getName()).thenReturn("..", stringPath);
                }
                break;
            case 2:
                when(mockCurrentDirent.getName()).thenReturn(".", "..", stringPath);
                break;
            default:
                when(mockCurrentDirent.getName()).thenReturn(stringPath);
                break;
        }
        when(mockPath.resolve(any(String.class))).thenReturn(fakeResultPath);

        iterator.advance();

        assertEquals(fakeResultPath, iterator.getNextPath());

        verify(mockPath).resolve(stringPath);
        switch (type) {
            case 1:
                verify(mockCurrentDirent, times(2)).getName();
                if (current) {
                    verify(mockPath).resolve(".");
                } else {
                    verify(mockPath).resolve("..");
                }
                verify(mockStream, times(2)).getDir();
                verify(mockStream, times(2)).getDirHandle();

                PowerMockito.verifyNew(dirent.class, times(4)).withNoArguments();
                PowerMockito.verifyStatic(times(2));
                dirent.memmove(mockNextDirent, nextPtr, dirent.SIZE_OF);
                PowerMockito.verifyStatic(times(2));
                dirent.free(nextPtr);
                break;
            case 2:
                verify(mockCurrentDirent, times(3)).getName();
                verify(mockPath).resolve(".");
                verify(mockPath).resolve("..");
                verify(mockStream, times(3)).getDir();
                verify(mockStream, times(3)).getDirHandle();

                PowerMockito.verifyNew(dirent.class, times(6)).withNoArguments();
                PowerMockito.verifyStatic(times(3));
                dirent.memmove(mockNextDirent, nextPtr, dirent.SIZE_OF);
                PowerMockito.verifyStatic(times(3));
                dirent.free(nextPtr);
                break;
            default:
                verify(mockCurrentDirent).getName();
                verify(mockStream).getDir();
                verify(mockStream).getDirHandle();

                PowerMockito.verifyNew(dirent.class, times(2)).withNoArguments();
                PowerMockito.verifyStatic();
                dirent.memmove(mockNextDirent, nextPtr, dirent.SIZE_OF);
                PowerMockito.verifyStatic();
                dirent.free(nextPtr);
                break;
        }
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
