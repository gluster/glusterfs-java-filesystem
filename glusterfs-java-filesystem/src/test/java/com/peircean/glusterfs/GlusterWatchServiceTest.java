package com.peircean.glusterfs;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GlusterWatchService.class, Thread.class})
public class GlusterWatchServiceTest {

    GlusterWatchService watchService = PowerMockito.spy(new GlusterWatchService());

    @Test(expected = ClosedWatchServiceException.class)
    public void testRegisterPath_whenNotRunning() {
        watchService.setRunning(false);
        GlusterPath mockPath = mock(GlusterPath.class);
        watchService.registerPath(mockPath);
    }

    @Test
    public void testRegisterPath() {
        watchService.setRunning(true);
        GlusterPath mockPath = mock(GlusterPath.class);
        WatchEvent.Kind mockKind = mock(WatchEvent.Kind.class);
        watchService.registerPath(mockPath, mockKind);
        GlusterWatchKey event = watchService.getPaths().iterator().next();
        assertEquals(mockPath, event.getPath());
        assertEquals(mockKind, event.getKinds()[0]);
    }

    @Test
    public void testRegisterPath_whenPathExists() {
        watchService.setRunning(true);
        GlusterPath mockPath = mock(GlusterPath.class);
        GlusterWatchKey keyFix = new GlusterWatchKey(mockPath, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY});
        watchService.getPaths().add(keyFix);
        watchService.registerPath(mockPath, StandardWatchEventKinds.ENTRY_CREATE);
        GlusterWatchKey event = watchService.getPaths().iterator().next();
        assertEquals(mockPath, event.getPath());
        assertEquals(StandardWatchEventKinds.ENTRY_CREATE, event.getKinds()[0]);

    }

    @Test
    public void testClose_whenNotRunning() throws IOException {
        watchService.setRunning(false);
        GlusterWatchKey mockKey = mock(GlusterWatchKey.class);
        watchService.getPaths().add(mockKey);
        watchService.close();
        Mockito.verify(mockKey, Mockito.never()).cancel();
    }

    @Test
    public void testClose() throws IOException {
        watchService.setRunning(true);
        GlusterWatchKey mockKey = mock(GlusterWatchKey.class);
        watchService.getPaths().add(mockKey);
        watchService.close();
        Mockito.verify(mockKey).cancel();
        assertFalse(watchService.isRunning());
    }

    @Test
    public void testPopPending_whenNonePending() {
        WatchKey key = watchService.popPending();
        assertEquals(null, key);
        assertEquals(0, watchService.getPendingPaths().size());
    }

    @Test
    public void testPopPending() {
        GlusterPath mockPath = mock(GlusterPath.class);
        GlusterWatchKey keyFix = new GlusterWatchKey(mockPath, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE});
        watchService.getPendingPaths().add(keyFix);
        WatchKey key = watchService.popPending();
        assertEquals(keyFix, key);
        assertEquals(0, watchService.getPendingPaths().size());
    }

    @Test(expected = ClosedWatchServiceException.class)
    public void testPoll_whenNotRunning() {
        watchService.setRunning(false);
        watchService.poll();
    }

    @Test
    public void testPoll_whenPending() {
        watchService.setRunning(true);
        GlusterWatchKey mockKey = mock(GlusterWatchKey.class);
        watchService.getPendingPaths().add(mockKey);
        WatchKey key = watchService.poll();
        assertEquals(mockKey, key);
    }

    @Test
    public void testPoll_whenReadyAndEvent() {
        watchService.setRunning(true);
        GlusterWatchKey mockKey = mock(GlusterWatchKey.class);
        watchService.getPaths().add(mockKey);
        doReturn(null).doReturn(mockKey).when(watchService).popPending();
        doReturn(true).when(mockKey).isValid();
        doReturn(true).when(mockKey).isReady();
        doReturn(true).when(mockKey).update();

        WatchKey key = watchService.poll();

        assertEquals(mockKey, key);

        Mockito.verify(mockKey).isValid();
        Mockito.verify(mockKey).isReady();
        Mockito.verify(mockKey).update();
    }

    @Test
    public void testPoll_whenReadyAndNoEvent() {
        watchService.setRunning(true);
        GlusterWatchKey mockKey = mock(GlusterWatchKey.class);
        watchService.getPaths().add(mockKey);
        doReturn(true).when(mockKey).isValid();
        doReturn(true).when(mockKey).isReady();
        doReturn(false).when(mockKey).update();

        WatchKey key = watchService.poll();

        assertEquals(null, key);

        Mockito.verify(mockKey).isValid();
        Mockito.verify(mockKey).isReady();
        Mockito.verify(mockKey).update();
    }

    @Test(expected = ClosedWatchServiceException.class)
    public void testPollTimeout_whenClosed() {
        long timeout = 150L;
        TimeUnit unit = TimeUnit.MILLISECONDS;
        doReturn(timeout).when(watchService).timeoutToMillis(timeout, unit);
        watchService.setRunning(false);
        watchService.poll(timeout, unit);
    }

    @Test
    public void testPollTimeout() throws InterruptedException {
        long timeout = 150L;
        TimeUnit unit = TimeUnit.MILLISECONDS;
//        doReturn(timeout).when(watchService).timeoutToMillis(timeout, unit);

        WatchKey mockKey = mock(WatchKey.class);
        PowerMockito.when(watchService.poll()).thenReturn(null).thenReturn(mockKey);

        PowerMockito.spy(Thread.class);
        PowerMockito.doThrow(new InterruptedException()).when(Thread.class);
        Thread.sleep(GlusterWatchService.PERIOD);

        WatchKey key = watchService.poll(timeout, unit);

        assertEquals(mockKey, key);

        Mockito.verify(watchService, Mockito.times(2)).poll();
//        Mockito.verify(watchService).timeoutToMillis(timeout, unit);

        PowerMockito.verifyStatic(Mockito.times(1));
        Thread.sleep(GlusterWatchService.PERIOD);
    }

    @Test(expected = ClosedWatchServiceException.class)
    public void testTake_whenClosed() {
        watchService.setRunning(false);
        watchService.take();
    }

    @Test
    public void testTake() throws Exception {
        WatchKey mockKey = mock(WatchKey.class);
        doReturn(null).doReturn(mockKey).when(watchService).poll();

        PowerMockito.spy(Thread.class);
        PowerMockito.doThrow(new InterruptedException()).when(Thread.class);
        Thread.sleep(GlusterWatchService.PERIOD);

        WatchKey key = watchService.take();

        assertEquals(mockKey, key);

        Mockito.verify(watchService, Mockito.times(2)).poll();

        PowerMockito.verifyStatic();
        Thread.sleep(GlusterWatchService.PERIOD);
    }

    @Test
    public void testTimeoutToMillis() {
        long time = 12345L;
        Assert.assertEquals(-1,
                watchService.timeoutToMillis(time, TimeUnit.NANOSECONDS));
        Assert.assertEquals(-1,
                watchService.timeoutToMillis(time, TimeUnit.MICROSECONDS));
        Assert.assertEquals(time,
                watchService.timeoutToMillis(time, TimeUnit.MILLISECONDS));
        Assert.assertEquals(time * GlusterWatchService.MILLIS_PER_SECOND,
                watchService.timeoutToMillis(time, TimeUnit.SECONDS));
        Assert.assertEquals(time * GlusterWatchService.MILLIS_PER_MINUTE,
                watchService.timeoutToMillis(time, TimeUnit.MINUTES));
        Assert.assertEquals(time * GlusterWatchService.MILLIS_PER_HOUR,
                watchService.timeoutToMillis(time, TimeUnit.HOURS));
        Assert.assertEquals(time * GlusterWatchService.MILLIS_PER_DAY,
                watchService.timeoutToMillis(time, TimeUnit.DAYS));
    }
}
