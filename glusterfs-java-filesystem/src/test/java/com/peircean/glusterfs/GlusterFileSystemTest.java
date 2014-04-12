package com.peircean.glusterfs;

import com.peircean.glusterfs.borrowed.GlobPattern;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Pattern.class, GlobPattern.class, GlusterFileSystem.class})
public class GlusterFileSystemTest extends TestCase {
    public static final long VOLPTR = 1234l;
    public static final String VOLNAME = "testvol";
    public static final String HOST = "123.45.67.89";
    @Mock
    private GlusterFileSystemProvider mockFileSystemProvider;
    @Mock
    private GlusterPath mockPath;
    @Mock
    private GlusterPathMatcher mockMatcher;

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
        doReturn(0).when(mockFileSystemProvider).close(VOLPTR);
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

    @Test(expected = IllegalArgumentException.class)
    public void testGetPathMatcher_whenBadInput() {
        fileSystem.getPathMatcher("foo");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetPathMatcher_whenBadSyntax() {
        fileSystem.getPathMatcher("foo:bar");
    }

    @Test(expected = PatternSyntaxException.class)
    public void testGetPathMatcher_whenBadExpression() {
        fileSystem.getPathMatcher("regex:[");
    }

    @Test
    public void testGetPathMatcher_whenGlob() throws Exception {
        pathMatcherHelper(true);
    }

    @Test
    public void testGetPathMatcher_whenRegex() throws Exception {
        pathMatcherHelper(false);
    }

    void pathMatcherHelper(boolean glob) throws Exception {
        PowerMockito.mockStatic(GlobPattern.class);
        Pattern pattern = PowerMockito.mock(Pattern.class);
        Matcher matcher = PowerMockito.mock(Matcher.class);

        String globPattern = "someglob";
        if (glob) {
            mockStatic(GlobPattern.class);
            when(GlobPattern.compile(globPattern)).thenReturn(pattern);
        } else {
            mockStatic(Pattern.class);
            when(Pattern.compile(globPattern)).thenReturn(pattern);
        }

        when(pattern.matcher(globPattern)).thenReturn(matcher);
        PowerMockito.whenNew(GlusterPathMatcher.class).withArguments(pattern).thenReturn(mockMatcher);

        String syntax;
        if (glob) {
            syntax = "glob";
        } else {
            syntax = "regex";
        }
        PathMatcher pathMatcher = fileSystem.getPathMatcher(syntax + ":" + globPattern);

        assertEquals(mockMatcher, pathMatcher);

        verifyNew(GlusterPathMatcher.class).withArguments(pattern);

        if (glob) {
            verifyStatic();
            GlobPattern.compile(globPattern);
        } else {
            verifyStatic();
            Pattern.compile(globPattern);
        }
    }
}
