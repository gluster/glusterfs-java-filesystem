package com.peircean.glusterfs;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class GlusterPathTest extends TestCase {
    @Mock
    private GlusterFileSystem mockFileSystem;
    @Mock
    private GlusterPath mockGlusterPath;

    @Before
    public void setUp() {
        doReturn("/").when(mockFileSystem).getSeparator();
    }

    @Test
    public void testConstruct() {
        GlusterPath p = new GlusterPath(mockFileSystem, "/foo/bar");
        assertEquals(2, p.getParts().length);

        p = new GlusterPath(mockFileSystem, "foo/bar");
        assertEquals(2, p.getParts().length);

        p = new GlusterPath(mockFileSystem, "foo/bar/");
        assertEquals(2, p.getParts().length);

        p = new GlusterPath(mockFileSystem, "/foo/bar/");
        assertEquals(2, p.getParts().length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstruct_noFileSystem() {
        Path p = new GlusterPath(null, "");
    }

//    @Test(expected = InvalidPathException.class)
//    public void testConstruct_emptyPath() {
//        Path p = new GlusterPath(mockFileSystem, "");
//    }

    @Test(expected = InvalidPathException.class)
    public void testConstruct_nullPath() {
        Path p = new GlusterPath(mockFileSystem, null);
    }

    @Test
    public void testConstruct_internal() {
        String[] parts = {"a", "b"};
        GlusterPath p = new GlusterPath(mockFileSystem, parts, true);
        assertEquals(parts, p.getParts());
        assertEquals(mockFileSystem, p.getFileSystem());
    }

    @Test
    public void testIsAbsolute() {
        Path p = new GlusterPath(mockFileSystem, "foo/bar");
        assertFalse(p.isAbsolute());

        p = new GlusterPath(mockFileSystem, "/foo/bar");
        assertTrue(p.isAbsolute());
    }

    @Test
    public void testGetRoot_absolute() {
        List<Path> paths = new LinkedList<Path>();
        GlusterPath root = new GlusterPath(mockFileSystem, "/");
        paths.add(root);
        doReturn(paths).when(mockFileSystem).getRootDirectories();

        Path p = new GlusterPath(mockFileSystem, "/foo/bar");
        Path returned = p.getRoot();

        verify(mockFileSystem).getRootDirectories();
        assertEquals(root, returned);
    }

    @Test
    public void testGetRoot_relative() {
        Path p = new GlusterPath(mockFileSystem, "foo/bar");
        Path returned = p.getRoot();

        verify(mockFileSystem, times(0)).getRootDirectories();
        assertEquals(null, returned);
    }

    @Test
    public void testGetFilesystem() {
        Path p = new GlusterPath(mockFileSystem, "foo/bar");
        assertTrue(p.getFileSystem() == mockFileSystem);
    }

    @Test
    public void testToUri() throws URISyntaxException {
        GlusterFileSystemProvider mockProvider = mock(GlusterFileSystemProvider.class);
        String scheme = GlusterFileSystemProvider.GLUSTER;
        doReturn(scheme).when(mockProvider).getScheme();
        doReturn(mockProvider).when(mockFileSystem).provider();

        String host = "localhost";
        doReturn(host).when(mockFileSystem).getHost();

        String volname = "foo";
        doReturn(volname).when(mockFileSystem).getVolname();
        
        String path = "/foo/bar";
        GlusterPath p = spy(new GlusterPath(mockFileSystem, path));
        doReturn(mockFileSystem).when(p).getFileSystem();
        doReturn(path).when(p).toString();

        String authority = host + ":" + volname;
        URI expected = new URI(scheme, authority, path, null, null);
        assertEquals(expected, p.toUri());
    }

    @Test
    public void testGetFileName() {
        Path p = new GlusterPath(mockFileSystem, "/");
        assertEquals(null, p.getFileName());

        p = new GlusterPath(mockFileSystem, "/bar");
        assertEquals(new GlusterPath(mockFileSystem, "bar"), p.getFileName());

        p = new GlusterPath(mockFileSystem, "foo/bar");
        assertEquals(new GlusterPath(mockFileSystem, "bar"), p.getFileName());
    }

    @Test
    public void testGetParent() {
        List<Path> roots = new LinkedList<Path>();
        roots.add(mockGlusterPath);
        doReturn(roots).when(mockFileSystem).getRootDirectories();

        Path p = new GlusterPath(mockFileSystem, "/");
        assertEquals(mockGlusterPath, p.getParent());

        p = new GlusterPath(mockFileSystem, "/bar");
        assertEquals(mockGlusterPath, p.getParent());

        p = new GlusterPath(mockFileSystem, "/bar/baz");
        assertEquals(new GlusterPath(mockFileSystem, "/bar"), p.getParent());

        p = new GlusterPath(mockFileSystem, "foo/bar");
        assertEquals(new GlusterPath(mockFileSystem, "foo"), p.getParent());

        p = new GlusterPath(mockFileSystem, "bar");
        assertEquals(null, p.getParent());

        verify(mockFileSystem, times(2)).getRootDirectories();
    }

    @Test
    public void testGetNameCount() {
        Path p = new GlusterPath(mockFileSystem, "/");
        assertEquals(0, p.getNameCount());

        p = new GlusterPath(mockFileSystem, "/bar");
        assertEquals(1, p.getNameCount());

        p = new GlusterPath(mockFileSystem, "/foo/bar");
        assertEquals(2, p.getNameCount());

        p = new GlusterPath(mockFileSystem, "foo/bar");
        assertEquals(2, p.getNameCount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetName_noName() {
        Path p = new GlusterPath(mockFileSystem, "/");
        p.getName(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetName_negative() {
        Path p = new GlusterPath(mockFileSystem, "/foo");
        p.getName(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetName_excessive() {
        Path p = new GlusterPath(mockFileSystem, "/foo");
        p.getName(4);
    }

    @Test
    public void testGetName() {
        Path p = new GlusterPath(mockFileSystem, "/foo/bar/baz");
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"foo"}, true), p.getName(0));
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"foo", "bar"}, true), p.getName(1));
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"foo", "bar", "baz"}, true), p.getName(2));

        p = new GlusterPath(mockFileSystem, "foo/bar/baz");
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"foo"}, false), p.getName(0));
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"foo", "bar"}, false), p.getName(1));
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"foo", "bar", "baz"}, false), p.getName(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpath_noName() {
        Path p = new GlusterPath(mockFileSystem, "/");
        p.getName(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpath_negativeStart() {
        Path p = new GlusterPath(mockFileSystem, "/foo");
        p.subpath(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpath_negativeEnd() {
        Path p = new GlusterPath(mockFileSystem, "/foo");
        p.subpath(0, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpath_excessiveStart() {
        Path p = new GlusterPath(mockFileSystem, "/foo");
        p.subpath(4, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpath_excessiveEnd() {
        Path p = new GlusterPath(mockFileSystem, "/foo");
        p.subpath(0, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubpath_Inverted() {
        Path p = new GlusterPath(mockFileSystem, "/foo");
        p.subpath(2, 1);
    }

    @Test
    public void testSubpath() {
        Path p = new GlusterPath(mockFileSystem, "/foo/bar/baz");
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"foo"}, true), p.subpath(0, 1));
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"bar"}, true), p.subpath(1, 2));
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"bar", "baz"}, true), p.subpath(1, 3));

        p = new GlusterPath(mockFileSystem, "foo/bar/baz");
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"foo"}, false), p.subpath(0, 1));
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"bar"}, false), p.subpath(1, 2));
        assertEquals(new GlusterPath(mockFileSystem, new String[]{"bar", "baz"}, false), p.subpath(1, 3));
    }

    @Test
    public void testStartsWith() {
        Path p1 = new GlusterPath(mockFileSystem, "/");
        assertTrue(p1.startsWith(p1));

        Path p2 = new GlusterPath(mockFileSystem, "/foo");
        assertFalse(p1.startsWith(p2));

        assertTrue(p2.startsWith(p1));

        p1 = new GlusterPath(mockFileSystem, "/bar");
        assertFalse(p1.startsWith(p2));
    }

    @Test
    public void testStartsWith_String() {
        Path p1 = new GlusterPath(mockFileSystem, "/");
        assertTrue(p1.startsWith("/"));

        Path p2 = new GlusterPath(mockFileSystem, "/foo");
        assertFalse(p1.startsWith("/foo"));

        assertTrue(p2.startsWith("/"));

        p1 = new GlusterPath(mockFileSystem, "/bar");
        assertFalse(p1.startsWith("/foo"));
    }

    @Test
    public void testEndsWith() {
        Path p1 = new GlusterPath(mockFileSystem, "/");
        assertTrue(p1.endsWith(p1));

        Path p2 = new GlusterPath(mockFileSystem, "/foo");
        assertFalse(p1.endsWith(p2));

        p1 = new GlusterPath(mockFileSystem, "/foo");
        assertTrue(p1.endsWith(p2));

        p2 = new GlusterPath(mockFileSystem, "foo");
        assertTrue(p1.endsWith(p2));

        p1 = new GlusterPath(mockFileSystem, "foo/bar");
        p2 = new GlusterPath(mockFileSystem, "bar");
        assertTrue(p1.endsWith(p2));

        p2 = new GlusterPath(mockFileSystem, "/bar");
        assertFalse(p1.endsWith(p2));
    }

    @Test
    public void testNormalize() {
        Path p = new GlusterPath(mockFileSystem, "foo//bar");
        assertEquals(new GlusterPath(mockFileSystem, "foo/bar"), p.normalize());

        p = new GlusterPath(mockFileSystem, "foo/./bar");
        assertEquals(new GlusterPath(mockFileSystem, "foo/bar"), p.normalize());

        p = new GlusterPath(mockFileSystem, "foo/././bar");
        assertEquals(new GlusterPath(mockFileSystem, "foo/bar"), p.normalize());

        p = new GlusterPath(mockFileSystem, "foo/baz/../bar");
        assertEquals(new GlusterPath(mockFileSystem, "foo/bar"), p.normalize());

        p = new GlusterPath(mockFileSystem, "foo/baz/../baz/../bar");
        assertEquals(new GlusterPath(mockFileSystem, "foo/bar"), p.normalize());
    }

    @Test
    public void testResolve() {
        Path path = new GlusterPath(mockFileSystem, "/");
        Path otherPath = new GlusterPath(mockFileSystem, "/bar");

        assertEquals(otherPath, path.resolve(otherPath));

        otherPath = new GlusterPath(mockFileSystem, "bar");
        assertEquals(new GlusterPath(mockFileSystem, "/bar"), path.resolve(otherPath));

        otherPath = new GlusterPath(mockFileSystem, "");
        assertEquals(path, path.resolve(otherPath));
    }

    @Test
    public void testResolve_string() {
        Path path = new GlusterPath(mockFileSystem, "/");

        assertEquals(new GlusterPath(mockFileSystem, "/bar"), path.resolve("/bar"));

        assertEquals(new GlusterPath(mockFileSystem, "/bar"), path.resolve("bar"));

        assertEquals(path, path.resolve(""));

        path = new GlusterPath(mockFileSystem, "/foo");
        assertEquals(new GlusterPath(mockFileSystem, "/foo/bar"), path.resolve("bar"));
    }

    @Test
    public void testResolveSibling() {
        Path p = new GlusterPath(mockFileSystem, "foo/bar");
        Path other = new GlusterPath(mockFileSystem, "baz");
        Path finalPath = p.resolveSibling(other);
        assertEquals(new GlusterPath(mockFileSystem, "foo/baz"), finalPath);
    }

    @Test
    public void testResolveSibling_string() {
        Path p = new GlusterPath(mockFileSystem, "foo/bar");
        Path finalPath = p.resolveSibling("baz");
        assertEquals(new GlusterPath(mockFileSystem, "foo/baz"), finalPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRelativize_otherIsRelative() {
        Path p = new GlusterPath(mockFileSystem, "/foo/bar");
        Path other = new GlusterPath(mockFileSystem, "foo/baz");
        p.relativize(other);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRelativize_thisIsRelative() {
        Path p = new GlusterPath(mockFileSystem, "foo/bar");
        Path other = new GlusterPath(mockFileSystem, "/foo/baz");
        p.relativize(other);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRelativize_bothAreRelative() {
        Path p = new GlusterPath(mockFileSystem, "foo/bar");
        Path other = new GlusterPath(mockFileSystem, "foo/baz");
        p.relativize(other);
    }

    @Test
    public void testRelativize() {
        Path p = new GlusterPath(mockFileSystem, "/foo/bar");
        Path other = new GlusterPath(mockFileSystem, "/foo/baz");

        Path relativePath = p.relativize(other);
        assertEquals(new GlusterPath(mockFileSystem, "../baz"), relativePath);

        other = new GlusterPath(mockFileSystem, "/foo/bar/baz");
        relativePath = p.relativize(other);
        assertEquals(new GlusterPath(mockFileSystem, "baz"), relativePath);

        p = new GlusterPath(mockFileSystem, "/foo/bar");
        other = new GlusterPath(mockFileSystem, "/foo");
        relativePath = p.relativize(other);
        assertEquals(new GlusterPath(mockFileSystem, ".."), relativePath);

        p = new GlusterPath(mockFileSystem, "/foo/bar");
        other = new GlusterPath(mockFileSystem, "/baz");
        relativePath = p.relativize(other);
        assertEquals(new GlusterPath(mockFileSystem, "../../baz"), relativePath);

        p = new GlusterPath(mockFileSystem, "/foo");
        other = new GlusterPath(mockFileSystem, "/bar/baz");
        relativePath = p.relativize(other);
        assertEquals(new GlusterPath(mockFileSystem, "../bar/baz"), relativePath);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testToAbsolutePath_whenRelative() {
        Path p = new GlusterPath(mockFileSystem, "foo");
        p.toAbsolutePath();
    }

    @Test
    public void testToAbsolutePath() {
        Path p = new GlusterPath(mockFileSystem, "/foo");
        assertEquals(p, p.toAbsolutePath());
    }

    @Test
    public void testIterator() {
        Path p = new GlusterPath(mockFileSystem, "/foo/bar");
        Iterator<Path> it = p.iterator();
        assertEquals(new GlusterPath(mockFileSystem, "foo"), it.next());
        assertEquals(new GlusterPath(mockFileSystem, "bar"), it.next());

        p = new GlusterPath(mockFileSystem, "/");
        it = p.iterator();
        assertFalse(it.hasNext());
    }

    @Test(expected = ClassCastException.class)
    public void testCompareTo_differentProvider() {
        Path path = new GlusterPath(mockFileSystem, "");
        Path zipfile = Paths.get("/codeSamples/zipfs/zipfstest.zip");
        path.compareTo(zipfile);
    }

    @Test
    public void testCompareTo() {
        Path p = new GlusterPath(mockFileSystem, "/foo");
        Path other = new GlusterPath(mockFileSystem, "/bar");
        assertTrue(p.compareTo(other) > 0);

        p = new GlusterPath(mockFileSystem, "/bar");
        other = new GlusterPath(mockFileSystem, "/foo");
        assertTrue(p.compareTo(other) < 0);

        p = new GlusterPath(mockFileSystem, "/foo");
        other = new GlusterPath(mockFileSystem, "/foo");
        assertEquals(0, p.compareTo(other));

        p = new GlusterPath(mockFileSystem, "/");
        other = new GlusterPath(mockFileSystem, "/foo");
        assertTrue(p.compareTo(other) < 0);

        p = new GlusterPath(mockFileSystem, "/foo");
        other = new GlusterPath(mockFileSystem, "/");
        assertTrue(p.compareTo(other) > 0);
    }

    @Test
    public void testToString_whenPathString() {
        String pathString = "/bar/baz";
        Path p = new GlusterPath(mockFileSystem, pathString);
        String filesystemString = "gluster://127.0.2.1:foo";
        doReturn(filesystemString).when(mockFileSystem).toString();
        assertEquals(pathString, p.toString());
    }

    @Test
    public void testToString_whenNoPathString() {
        Path p = new GlusterPath(mockFileSystem, new String[]{"a", "b"}, true);
        assertEquals("/a/b", p.toString());
    }

    @Test
    public void testGetString_whenPathString() {
        String string = "/foo/bar";
        GlusterPath path = new GlusterPath(mockFileSystem, string);
        path.setParts(new String[]{"a", "b"});
        assertEquals(string, path.getString());
    }

    @Test
    public void testGetString_whenNoPathString() {
        GlusterPath path = new GlusterPath(mockFileSystem, new String[]{"a", "b"}, false);
        assertEquals("a/b", path.getString());
    }

    @Test
    public void testRegisterWatchService() throws IOException {
        GlusterPath path = spy(new GlusterPath(mockFileSystem, new String[]{"foo", "bar"}, true));
        GlusterWatchService mockWatchService = mock(GlusterWatchService.class);
        doNothing().when(path).guardRegisterWatchService(mockWatchService);

        WatchEvent.Kind[] kinds = new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY};

        doNothing().when(path).guardRegisterWatchDirectory();

        WatchKey mockKey = mock(WatchKey.class);
        doReturn(mockKey).when(mockWatchService).registerPath(path, kinds);

        WatchKey watchKey = path.register(mockWatchService, kinds);

        assertEquals(mockKey, watchKey);

        verify(path).guardRegisterWatchService(mockWatchService);
        verify(path).guardRegisterWatchDirectory();
        verify(mockWatchService).registerPath(path, kinds);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGuardRegisterWatchService() {
        GlusterPath path = spy(new GlusterPath(mockFileSystem, new String[]{}, false));
        WatchService mockWatchService = mock(WatchService.class);

        path.guardRegisterWatchService(mockWatchService);
    }

}

