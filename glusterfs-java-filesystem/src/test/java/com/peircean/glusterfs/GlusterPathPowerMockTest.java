package com.peircean.glusterfs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.file.Files;
import java.nio.file.NotDirectoryException;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GlusterPath.class, Files.class})
public class GlusterPathPowerMockTest {
    @Mock
    private GlusterFileSystem mockFileSystem;

    @Test(expected = NotDirectoryException.class)
    public void testGuardRegisterWatchDirectory_whenNotDirectory() throws NotDirectoryException {
        GlusterPath path = new GlusterPath(mockFileSystem, new String[]{"/foo"}, false);

        PowerMockito.mockStatic(Files.class);
        when(Files.isDirectory(path)).thenReturn(false);

        path.guardRegisterWatchDirectory();
    }

    @Test
    public void testGuardRegisterWatchDirectory() throws NotDirectoryException {
        GlusterPath path = new GlusterPath(mockFileSystem, new String[]{"/foo"}, false);

        PowerMockito.mockStatic(Files.class);
        when(Files.isDirectory(path)).thenReturn(true);

        path.guardRegisterWatchDirectory();

        PowerMockito.verifyStatic();
        Files.isDirectory(path);
    }
}
