package com.peircean.glusterfs;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchKey;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.*;

//import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GlusterWatchService.class, Thread.class})
public class GlusterWatchServiceTest {

    GlusterWatchService watchService = spy(new GlusterWatchService());

    @Test
    public void testRegisterPath() {

    }

    @Test
    public void testClose() {

    }

    @Test
    public void testPoll() {

    }

    @Test(expected = ClosedWatchServiceException.class)
    public void testPollTimeout_whenClosed() {
        long timeout = 150L;
        TimeUnit unit = TimeUnit.MILLISECONDS;
        doReturn(timeout).when(watchService).timeoutToMillis(timeout, unit);
        watchService.setRunning(false);
        watchService.poll(timeout, unit);
    }

//    @Test
//    public void testPollTimeout() throws InterruptedException {
////        GlusterWatchService watchService = PowerMockito.spy(new GlusterWatchService());
//        long timeout = 150L;
//        TimeUnit unit = TimeUnit.MILLISECONDS;
//        doReturn(timeout).when(watchService).timeoutToMillis(timeout, unit);
//
//        WatchKey mockKey = mock(WatchKey.class);
//        PowerMockito.when(watchService.poll()).thenReturn(null).thenReturn(mockKey);
////        PowerMockito.when(watchService.poll()).thenReturn(mockKey);
////        doReturn(null).doReturn(mockKey).when(watchService).poll();
//
//        PowerMockito.spy(Thread.class);
//        PowerMockito.doThrow(new InterruptedException()).when(Thread.class);
//        Thread.sleep(GlusterWatchService.PERIOD);
//
//        WatchKey key = watchService.poll(timeout, unit);
//
//        assertEquals(mockKey, key);
//
//        Mockito.verify(watchService, Mockito.times(2)).poll();
//        Mockito.verify(watchService).timeoutToMillis(timeout, unit);
//
//        PowerMockito.verifyStatic();
//        Thread.sleep(GlusterWatchService.PERIOD);
//    }

    @Test(expected = ClosedWatchServiceException.class)
    public void testTake_whenClosed() {
        watchService.setRunning(false);
        watchService.take();
    }

    @Test
    public void testTake() throws Exception {
        WatchKey mockKey = mock(WatchKey.class);
//        PowerMockito.when(watchService.poll()).thenReturn(null).thenReturn(mockKey);
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
