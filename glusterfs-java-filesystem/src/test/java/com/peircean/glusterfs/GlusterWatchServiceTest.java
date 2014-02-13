package com.peircean.glusterfs;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.TimeUnit;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GlusterWatchService.class, Thread.class})
public class GlusterWatchServiceTest {

    @Test
    public void testRegisterPath() {

    }

    @Test
    public void testClose() {

    }

    @Test
    public void testPoll() {

    }

    @Test
    public void testPollTimeout() {

    }

    @Test
    public void testTake() {

    }

    @Test
    public void testTimeoutToMillis() {
        long time = 12345L;
        Assert.assertEquals(-1,
                GlusterWatchService.timeoutToMillis(time, TimeUnit.NANOSECONDS));
        Assert.assertEquals(-1,
                GlusterWatchService.timeoutToMillis(time, TimeUnit.MICROSECONDS));
        Assert.assertEquals(time,
                GlusterWatchService.timeoutToMillis(time, TimeUnit.MILLISECONDS));
        Assert.assertEquals(time * GlusterWatchService.MILLIS_PER_SECOND,
                GlusterWatchService.timeoutToMillis(time, TimeUnit.SECONDS));
        Assert.assertEquals(time * GlusterWatchService.MILLIS_PER_MINUTE,
                GlusterWatchService.timeoutToMillis(time, TimeUnit.MINUTES));
        Assert.assertEquals(time * GlusterWatchService.MILLIS_PER_HOUR,
                GlusterWatchService.timeoutToMillis(time, TimeUnit.HOURS));
        Assert.assertEquals(time * GlusterWatchService.MILLIS_PER_DAY,
                GlusterWatchService.timeoutToMillis(time, TimeUnit.DAYS));
    }
}
