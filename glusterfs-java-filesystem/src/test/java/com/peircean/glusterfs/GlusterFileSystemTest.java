package com.peircean.glusterfs;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.util.Iterator;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class GlusterFileSystemTest extends TestCase {
    public static final long VOLPTR = 1234l;
    public static final String VOLNAME = "testvol";
    public static final String HOST = "123.45.67.89";
    @Mock
    private GlusterFileSystemProvider mockFileSystemProvider;
    @Mock
    private GlusterPath mockPath;
    private GlusterFileSystem fileSystem;

    @Before
    public void setUp() {
        fileSystem = new GlusterFileSystem(mockFileSystemProvider, HOST, VOLNAME, VOLPTR);
    }

    @Test
    public void testProvider() {
        assertEquals(mockFileSystemProvider, fileSystem.provider());
    }

    @Test(expected = IOException.class)
    public void testClose_whenFailing() throws IOException {
        doReturn(11).when(mockFileSystemProvider).close(VOLPTR);
        fileSystem.close();
    }

    @Test
    public void testClose() throws IOException {
        doReturn(-1).when(mockFileSystemProvider).close(VOLPTR);
        fileSystem.close();
        verify(mockFileSystemProvider).close(VOLPTR);
        assertEquals(-1, fileSystem.getVolptr());
    }

    @Test
    public void testIsOpen_whenOpen() {
        assertEquals(true, fileSystem.isOpen());
    }

    @Test
    public void testIsOpen_whenClosed() {
        fileSystem.setVolptr(-1);
        assertEquals(false, fileSystem.isOpen());
    }

    @Test
    public void testIsReadOnly() {
        assertFalse(fileSystem.isReadOnly());
    }

    @Test
    public void testGetSeparator() {
        assertEquals("/", fileSystem.getSeparator());
    }

    @Test
    public void testGetRootDirectories() {
        Iterable<Path> pi = fileSystem.getRootDirectories();
        Iterator<Path> iterator = pi.iterator();
        Path path = new GlusterPath(fileSystem, "/");
        assertEquals(path, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGetFileStores() {
        GlusterFileStore correctStore = new GlusterFileStore(fileSystem);
        Iterable<FileStore> stores = fileSystem.getFileStores();
        Iterator<FileStore> iterator = stores.iterator();
        assertEquals(correctStore, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testGetPath() {
        Path correctPath = new GlusterPath(fileSystem, "/foo/bar/baz");
        Path returnedPath = fileSystem.getPath("/foo", "bar", "baz");
        assertEquals(correctPath, returnedPath);
    }

    @Test
    public void testToString() {
        doReturn("gluster").when(mockFileSystemProvider).getScheme();
        String string = "gluster://" + HOST + ":" + VOLNAME;
        assertEquals(string, fileSystem.toString());
        verify(mockFileSystemProvider).getScheme();
    }
}
