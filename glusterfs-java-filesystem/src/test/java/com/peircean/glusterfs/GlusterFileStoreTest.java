package com.peircean.glusterfs;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class GlusterFileStoreTest extends TestCase {

    @Mock
    private GlusterFileSystem mockFileSystem;
    @Mock
    private GlusterFileSystemProvider mockProvider;
    private GlusterFileStore fileStore;

    @Before
    public void setUp() {
        doReturn(mockProvider).when(mockFileSystem).provider();
        fileStore = new GlusterFileStore(mockFileSystem);
    }

    @Test
    public void testName() {
        String volname = "testvol";
        doReturn(volname).when(mockFileSystem).getVolname();
        assertEquals(volname, fileStore.name());
    }

    @Test
    public void testType() {
        assertEquals("glusterfs", fileStore.type());
    }

    @Test
    public void testIsReadOnly() {
        assertFalse(fileStore.isReadOnly());
    }

    @Test
    public void testGetTotalSpace() throws IOException {
        long volptr = 4321l;
        long space = 1234l;
        doReturn(volptr).when(mockFileSystem).getVolptr();
        doReturn(space).when(mockProvider).getTotalSpace(volptr);
        long totalSpace = fileStore.getTotalSpace();
        verify(mockProvider).getTotalSpace(volptr);
        verify(mockFileSystem).getVolptr();
        assertEquals(space, totalSpace);
    }

    @Test
    public void testGetUsableSpace() throws IOException {
        long volptr = 4321l;
        long space = 1234l;
        doReturn(volptr).when(mockFileSystem).getVolptr();
        doReturn(space).when(mockProvider).getUsableSpace(volptr);
        long usableSpace = fileStore.getUsableSpace();
        verify(mockProvider).getUsableSpace(volptr);
        verify(mockFileSystem).getVolptr();
        assertEquals(space, usableSpace);
    }

    @Test
    public void testGetUnallocatedSpace() throws IOException {
        long volptr = 4321l;
        long space = 1234l;
        doReturn(volptr).when(mockFileSystem).getVolptr();
        doReturn(space).when(mockProvider).getUnallocatedSpace(volptr);
        long unallocatedSpace = fileStore.getUnallocatedSpace();
        verify(mockProvider).getUnallocatedSpace(volptr);
        verify(mockFileSystem).getVolptr();
        assertEquals(space, unallocatedSpace);
    }


}
