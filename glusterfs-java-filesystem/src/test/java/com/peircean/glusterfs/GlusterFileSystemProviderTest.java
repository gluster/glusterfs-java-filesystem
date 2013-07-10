package com.peircean.glusterfs;

import junit.framework.TestCase;
import org.fusesource.glfsjni.internal.GLFS;
import org.fusesource.glfsjni.internal.structs.stat;
import org.fusesource.glfsjni.internal.structs.statvfs;
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
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GLFS.class, GlusterFileSystemProvider.class, GlusterFileChannel.class, GlusterFileAttributes.class})
public class GlusterFileSystemProviderTest extends TestCase {

    public static final String SERVER = "hostname";
    public static final String VOLNAME = "testvol";
    @Mock
    private GlusterFileSystem mockFileSystem;
    @Mock
    private GlusterPath mockPath;
    @Mock
    private GlusterFileChannel mockChannel;
    @Spy
    private GlusterFileSystemProvider provider = new GlusterFileSystemProvider();

    @Test
    public void testGetScheme() {
        GlusterFileSystemProvider p = new GlusterFileSystemProvider();
        assertEquals("gluster", p.getScheme());
    }

    @Test
    public void testNewFileSystem() throws IOException, URISyntaxException {
        String authority = SERVER + ":" + VOLNAME;
        doReturn(new String[]{SERVER, VOLNAME}).when(provider).parseAuthority(authority);
        long volptr = 1234l;
        doReturn(volptr).when(provider).glfsNew(VOLNAME);
        doNothing().when(provider).glfsSetVolfileServer(SERVER, volptr);
        doNothing().when(provider).glfsInit(authority, volptr);
        URI uri = new URI("gluster://" + authority);
        FileSystem fileSystem = provider.newFileSystem(uri, null);
        verify(provider).parseAuthority(authority);
        verify(provider).glfsNew(VOLNAME);
        verify(provider).glfsSetVolfileServer(SERVER, volptr);
        verify(provider).glfsInit(authority, volptr);
        assertTrue(provider.getCache().containsKey(authority));
        assertEquals(fileSystem, provider.getCache().get(authority));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseAuthority_whenNoColon() {
        provider.parseAuthority("a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseAuthority_whenEmptyHost() {
        provider.parseAuthority(":b");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseAuthority_whenEmptyVolume() {
        provider.parseAuthority("a:");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseAuthority_whenBadSplit() {
        provider.parseAuthority("a:b:c");
    }

    @Test
    public void testParseAuthority() {
        String[] actual = provider.parseAuthority("a:b");
        assertEquals("a", actual[0]);
        assertEquals("b", actual[1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlfsNew_whenBad() {
        mockStatic(GLFS.class);
        when(GLFS.glfs_new(VOLNAME)).thenReturn(0l);
        provider.glfsNew(VOLNAME);
    }

    @Test
    public void testGlfsNew() {
        mockStatic(GLFS.class);
        when(GLFS.glfs_new(VOLNAME)).thenReturn(123l);
        long l = provider.glfsNew(VOLNAME);
        verifyStatic();
        GLFS.glfs_new(VOLNAME);
        assertEquals(l, 123l);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlfsSetVolfileServer_whenBad() {
        mockStatic(GLFS.class);
        String host = "123.45.67.89";
        when(GLFS.glfs_set_volfile_server(123l, "tcp", host, 24007)).thenReturn(-1);
        provider.glfsSetVolfileServer(host, 123l);
    }

    @Test
    public void testGlfsSetVolfileServer() {
        mockStatic(GLFS.class);
        String host = "123.45.67.89";
        when(GLFS.glfs_set_volfile_server(123l, "tcp", host, 24007)).thenReturn(0);
        provider.glfsSetVolfileServer(host, 123l);
        verifyStatic();
        GLFS.glfs_set_volfile_server(123l, "tcp", host, 24007);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlfsInit_whenBad() {
        mockStatic(GLFS.class);
        String host = "123.45.67.89";
        when(GLFS.glfs_init(123l)).thenReturn(-1);
        provider.glfsInit(host, 123l);
    }

    @Test
    public void testGlfsInit() {
        mockStatic(GLFS.class);
        String host = "123.45.67.89";
        when(GLFS.glfs_init(123l)).thenReturn(0);
        provider.glfsInit(host, 123l);
        verifyStatic();
        GLFS.glfs_init(123l);
    }

    @Test(expected = FileSystemNotFoundException.class)
    public void testGetFileSystem_whenNotFound() throws URISyntaxException {
        provider.getFileSystem(new URI("gluster://foo:bar/baz"));
    }

    @Test
    public void testGetFileSystem() throws URISyntaxException {
        provider.getCache().put("foo:bar", mockFileSystem);
        assertEquals(mockFileSystem, provider.getFileSystem(new URI("gluster://foo:bar/baz")));
    }

    @Test
    public void testNewFileChannel() throws Exception {
        FileAttribute<?>[] attrs = new FileAttribute[0];
        Set<? extends OpenOption> opts = new HashSet<OpenOption>();
        doReturn(mockChannel).when(provider).newFileChannelHelper(mockPath, opts, attrs);
        FileChannel fileChannel = provider.newFileChannel(mockPath, opts, attrs);
        verify(provider).newFileChannelHelper(mockPath, opts, attrs);
        assertEquals(mockChannel, fileChannel);
    }

    @Test
    public void testNewByteChannel() throws Exception {
        FileAttribute<?>[] attrs = new FileAttribute[0];
        Set<? extends OpenOption> opts = new HashSet<OpenOption>();
        doReturn(mockChannel).when(provider).newFileChannelHelper(mockPath, opts, attrs);
        ByteChannel fileChannel = provider.newByteChannel(mockPath, opts, attrs);
        verify(provider).newFileChannelHelper(mockPath, opts, attrs);
        assertEquals(mockChannel, fileChannel);
    }

    @Test
    public void testNewFileChannelHelper() throws Exception {
        Set<? extends OpenOption> options = new HashSet<OpenOption>();
        FileAttribute<?> attrs[] = new FileAttribute[0];
        URI uri = new URI("foo://bar/baz");
        doReturn(uri).when(mockPath).toUri();
        doReturn(mockFileSystem).when(provider).getFileSystem(uri);
        whenNew(GlusterFileChannel.class).withNoArguments().thenReturn(mockChannel);
        doNothing().when(mockChannel).init(mockFileSystem, mockPath, options, attrs);
        FileChannel channel = provider.newFileChannel(mockPath, options, attrs);
        verify(mockChannel).init(mockFileSystem, mockPath, options, attrs);
        verify(provider).getFileSystem(uri);
        verify(mockPath).toUri();
        verifyNew(GlusterFileChannel.class).withNoArguments();
        assertEquals(mockChannel, channel);
    }

    @Test(expected = FileSystemNotFoundException.class)
    public void testGetPath_whenCantCreateFilesystem() throws URISyntaxException, IOException {
        URI uri = new URI("gluster://foo:bar/baz");
        doThrow(FileSystemNotFoundException.class).when(provider).getFileSystem(uri);
        doThrow(IOException.class).when(provider).newFileSystem(uri, null);
        provider.getPath(uri);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPath_whenSchemeIsUnknown() throws URISyntaxException {
        URI uri = new URI("fluster://foo:bar/baz");
        provider.getPath(uri);
    }
    
    @Test
    public void testGetPath_whenCached() throws URISyntaxException, IOException {
        URI uri = new URI("gluster://foo:bar/baz");
        provider.getCache().put(uri.getAuthority(), mockFileSystem);
        doReturn(mockPath).when(mockFileSystem).getPath(uri.getPath());
        Path path = provider.getPath(uri);
        assertEquals(mockPath, path);
        verify(provider, times(0)).newFileSystem(uri, null);
        verify(mockFileSystem).getPath(uri.getPath());
    }
    
    @Test
    public void testGetPath_whenNotCached() throws URISyntaxException, IOException {
        URI uri = new URI("gluster://foo:bar/baz");
        doThrow(FileSystemNotFoundException.class).when(provider).getFileSystem(uri);
        doReturn(mockFileSystem).when(provider).newFileSystem(uri, null);
        doReturn(mockPath).when(mockFileSystem).getPath(uri.getPath());
        Path path = provider.getPath(uri);
        assertEquals(mockPath, path);
        verify(provider).newFileSystem(uri, null);
        verify(mockFileSystem).getPath(uri.getPath());
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testReadAttributes_whenDosAttributes() throws IOException {
        provider.readAttributes(mockPath, DosFileAttributes.class);
    }

    @Test
    public void testReadAttributes_followLinks() throws Exception {
        long volptr = 1234l;
        String path = "/foo/bar";
        URI uri = new URI("gluster://foo:bar" + path);

        doReturn(mockFileSystem).when(mockPath).getFileSystem();
        doReturn(volptr).when(mockFileSystem).getVolptr();
        doReturn(uri).when(mockPath).toUri();
        
        stat stat = new stat();
        whenNew(stat.class).withNoArguments().thenReturn(stat);

        mockStatic(GlusterFileAttributes.class);
        GlusterFileAttributes fakeAttributes = new GlusterFileAttributes(123, 234, 345, 12345l, 222111l, 121212l, 212121);
        when(GlusterFileAttributes.fromStat(stat)).thenReturn(fakeAttributes);

        mockStatic(GLFS.class);
        when(GLFS.glfs_stat(volptr, path, stat)).thenReturn(0);

        GlusterFileAttributes attributes = provider.readAttributes(mockPath, GlusterFileAttributes.class);

        assertEquals(fakeAttributes, attributes);

        verify(mockPath).getFileSystem();
        verify(mockFileSystem).getVolptr();
        verify(mockPath).toUri();

        verifyNew(stat.class).withNoArguments();

        verifyStatic();
        GLFS.glfs_stat(volptr, path, stat);

        verifyStatic();
        GlusterFileAttributes.fromStat(stat);
    }

    @Test
    public void testReadAttributes_dontFollowLinks() throws Exception {
        long volptr = 1234l;
        String path = "/foo/bar";
        URI uri = new URI("gluster://foo:bar" + path);
        
        doReturn(mockFileSystem).when(mockPath).getFileSystem();
        doReturn(volptr).when(mockFileSystem).getVolptr();
        doReturn(uri).when(mockPath).toUri();

        stat stat = new stat();
        whenNew(stat.class).withNoArguments().thenReturn(stat);

        mockStatic(GlusterFileAttributes.class);
        GlusterFileAttributes fakeAttributes = new GlusterFileAttributes(123, 234, 345, 12345l, 222111l, 121212l, 212121);
        when(GlusterFileAttributes.fromStat(stat)).thenReturn(fakeAttributes);

        mockStatic(GLFS.class);
        when(GLFS.glfs_lstat(volptr, path, stat)).thenReturn(0);

        GlusterFileAttributes attributes = provider.readAttributes(mockPath, GlusterFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

        assertEquals(fakeAttributes, attributes);

        verify(mockPath).getFileSystem();
        verify(mockFileSystem).getVolptr();
        verify(mockPath).toUri();

        verifyNew(stat.class).withNoArguments();

        verifyStatic();
        GLFS.glfs_lstat(volptr, path, stat);

        verifyStatic();
        GlusterFileAttributes.fromStat(stat);
    }

    @Test
    public void testGetTotalSpace() throws Exception {
        mockStatic(GLFS.class);
        long volptr = 1234l;
        String path = "/";
        statvfs buf = new statvfs();
        buf.f_bsize = 2;
        buf.f_blocks = 1000000l;
        whenNew(statvfs.class).withNoArguments().thenReturn(buf);
        when(GLFS.glfs_statvfs(volptr, path, buf)).thenReturn(0);
        long totalSpace = provider.getTotalSpace(volptr);
        verifyStatic();
        GLFS.glfs_statvfs(volptr, path, buf);
        verifyNew(statvfs.class).withNoArguments();
        assertEquals(buf.f_bsize * buf.f_blocks, totalSpace);
    }

    @Test
    public void testGetUsableSpace() throws Exception {
        mockStatic(GLFS.class);
        long volptr = 1234l;
        String path = "/";
        statvfs buf = new statvfs();
        buf.f_bsize = 2;
        buf.f_bavail = 1000000l;
        whenNew(statvfs.class).withNoArguments().thenReturn(buf);
        when(GLFS.glfs_statvfs(volptr, path, buf)).thenReturn(0);
        long usableSpace = provider.getUsableSpace(volptr);
        verifyStatic();
        GLFS.glfs_statvfs(volptr, path, buf);
        verifyNew(statvfs.class).withNoArguments();
        assertEquals(buf.f_bsize * buf.f_bavail, usableSpace);
    }

    @Test
    public void testGetUnallocatedSpace() throws Exception {
        mockStatic(GLFS.class);
        long volptr = 1234l;
        String path = "/";
        statvfs buf = new statvfs();
        buf.f_bsize = 2;
        buf.f_bfree = 1000000l;
        whenNew(statvfs.class).withNoArguments().thenReturn(buf);
        when(GLFS.glfs_statvfs(volptr, path, buf)).thenReturn(0);
        long unallocatedSpace = provider.getUnallocatedSpace(volptr);
        verifyStatic();
        GLFS.glfs_statvfs(volptr, path, buf);
        verifyNew(statvfs.class).withNoArguments();
        assertEquals(buf.f_bsize * buf.f_bfree, unallocatedSpace);
    }

}
