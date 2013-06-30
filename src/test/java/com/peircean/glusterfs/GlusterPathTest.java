package com.peircean.glusterfs;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class GlusterPathTest extends TestCase {
    @Mock
    private GlusterFileSystem mockFileSystem;
    
    @Mock
    private GlusterPath mockGlusterPath;

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

    @Test(expected = IllegalArgumentException.class)
    public void testConstruct_emptyPath() {
        Path p = new GlusterPath(mockFileSystem, "");
    }

    @Test(expected = IllegalArgumentException.class)
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
}
