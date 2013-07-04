package com.peircean.glusterfs;

import junit.framework.TestCase;
import org.fusesource.glfsjni.internal.GLFS;
import org.fusesource.glfsjni.internal.structs.statvfs;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GLFS.class, GlusterFileSystemProvider.class})
public class GlusterFileSystemProviderTest extends TestCase {

    public static final String SERVER = "hostname";
    public static final String VOLNAME = "testvol";
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
        provider.newFileSystem(uri, null);
        verify(provider).parseAuthority(authority);
        verify(provider).glfsNew(VOLNAME);
        verify(provider).glfsSetVolfileServer(SERVER, volptr);
        verify(provider).glfsInit(authority, volptr);
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

}
