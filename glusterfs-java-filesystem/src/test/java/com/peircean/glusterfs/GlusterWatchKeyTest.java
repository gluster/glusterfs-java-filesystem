package com.peircean.glusterfs;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;

import static junit.framework.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GlusterWatchKey.class, Files.class, LinkedList.class})
public class GlusterWatchKeyTest {
    @Mock
    GlusterPath mockPath;

    @Mock
    WatchEvent.Kind mockKind;

    WatchEvent.Kind[] mockKinds = new WatchEvent.Kind[]{mockKind};

    GlusterWatchKey key;

    @Before
    public void setUp() {
        key = PowerMockito.spy(new GlusterWatchKey(mockPath, mockKinds));
    }

    @Test
    public void testUpdate_whenIOException() throws IOException {
        PowerMockito.mockStatic(Files.class);
        when(Files.newDirectoryStream(mockPath)).thenThrow(new IOException());

        assertFalse(key.update());

        PowerMockito.verifyStatic();
        Files.newDirectoryStream(mockPath);
    }

    @Test
    public void testUpdate_whenNoEvents() throws Exception {
        updateHelper(UpdateTestMode.NONE);
    }

    @Test
    public void testUpdate_whenExistingEvent() throws Exception {
        updateHelper(UpdateTestMode.CHANGED);
    }

    @Test
    public void testUpdate_whenDeletedEvent() throws Exception {
        updateHelper(UpdateTestMode.DELETED);
    }

    void updateHelper(UpdateTestMode mode) throws Exception {
        boolean deleted = false;
        boolean existing = false;
        switch (mode) {
            case CHANGED:
                existing = true;
                break;
            case DELETED:
                deleted = true;
                break;
        }
        LinkedList<Path> mockPaths = new LinkedList<>();
        Path onePath = mock(Path.class);
        mockPaths.add(onePath);

        LinkedList<Path> mockFiles = mock(LinkedList.class);
        PowerMockito.whenNew(LinkedList.class).withNoArguments().thenReturn(mockFiles);

        Path mockEventPath = mock(Path.class);
        Set<Path> eventsKeys = new HashSet<Path>();
        eventsKeys.add(mockEventPath);
        HashMap<Path, GlusterWatchEvent> mockEvents = mock(HashMap.class);
        doReturn(eventsKeys).when(mockEvents).keySet();
        doReturn(deleted).when(key).checkDeleted(mockFiles, mockEventPath);
        key.setEvents(mockEvents);

        doReturn(existing).when(key).processExistingFile(mockFiles, onePath);

        DirectoryStream<Path> mockDirectoryStream = mock(DirectoryStream.class);
        doReturn(mockPaths.iterator()).when(mockDirectoryStream).iterator();

        PowerMockito.mockStatic(Files.class);
        when(Files.newDirectoryStream(mockPath)).thenReturn(mockDirectoryStream);

        switch (mode) {
            case CHANGED:
            case DELETED:
                assertTrue(key.update());
                break;
            default:
                assertFalse(key.update());
        }

        verify(mockFiles, never()).add(isA(Path.class));
        verify(mockDirectoryStream).iterator();
        verify(key).processExistingFile(mockFiles, onePath);
        verify(mockEvents).keySet();
        verify(key).checkDeleted(mockFiles, mockEventPath);

        PowerMockito.verifyStatic();
        Files.newDirectoryStream(mockPath);

        PowerMockito.verifyNew(LinkedList.class);
    }

    @Test
    public void testProcessExistingFile() {

    }

    @Test
    public void testCheckDeleted() {

    }

    @Test
    public void testCheckCreated() {

    }

    @Test
    public void testCheckModified() {

    }

    @Test
    public void testKindsContains() {

    }

    @Test
    public void testPollEvents_whenNotReady() {
        key.setReady(false);
        assertTrue(key.pollEvents().isEmpty());
    }

    @Test
    public void testPollEvents_whenReady() {
        key.setReady(true);

        LinkedList<WatchEvent<?>> mockEvents = mock(LinkedList.class);
        doReturn(mockEvents).when(key).findPendingEvents();

        List<WatchEvent<?>> events = key.pollEvents();
        assertEquals(mockEvents, events);

        assertFalse(key.isReady());

        verify(key).findPendingEvents();
    }

    @Test
    public void testFindPendingEvents() {

    }

    @Test
    public void testReset_whenInvalid() {
        key.setValid(false);
        Assert.assertFalse(key.reset());
    }

    @Test
    public void testReset_whenReady() {
        WatchEvent.Kind[] kinds = new WatchEvent.Kind[]{mock(WatchEvent.Kind.class)};
        GlusterPath path = mock(GlusterPath.class);

        GlusterWatchKey key = new GlusterWatchKey(path, kinds);
        key.setValid(true);
        key.setReady(true);
        Assert.assertFalse(key.reset());
    }

    @Test
    public void testCancel() {
        WatchEvent.Kind[] kinds = new WatchEvent.Kind[]{mock(WatchEvent.Kind.class)};
        GlusterPath path = mock(GlusterPath.class);

        GlusterWatchKey key = new GlusterWatchKey(path, kinds);
        key.setValid(true);
        key.setReady(false);
        assertTrue(key.reset());
        assertTrue(key.isReady());
    }

}

enum UpdateTestMode {
    NONE, CHANGED, DELETED
}