package com.peircean.glusterfs;

import lombok.Data;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class GlusterWatchService implements WatchService {
    public static final int MILLIS_PER_DAY = 86400000;
    public static final int MILLIS_PER_HOUR = 3600000;
    public static final int MILLIS_PER_MINUTE = 60000;
    public static final int MILLIS_PER_SECOND = 1000;
    public static long PERIOD = 100L;

    private Map<GlusterPath, WatchKey> paths = new HashMap<>();
    private boolean running = true;

    public WatchKey registerPath(GlusterPath path) {
        if (!running) {
            throw new ClosedWatchServiceException();
        }
        //Put a new WatchKey into the map under this path
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        running = false;
    }

    @Override
    public WatchKey poll() {
        // Find directory entries modified since last call
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey poll(long timeout, TimeUnit unit) {
        long timeoutMillis = timeoutToMillis(timeout, unit);
        long runs = 0;
        while (running) {
            WatchKey key = poll();
            if (key != null) {
                return key;
            }
            if ((runs * PERIOD) > timeoutMillis) {
                return null;
            }
            runs++;
            try {
                Thread.sleep(PERIOD);
            } catch (InterruptedException e) {
            }
        }
        throw new ClosedWatchServiceException();
    }

    long timeoutToMillis(long timeout, TimeUnit unit) {
        switch (unit) {
            case DAYS:
                return (timeout * MILLIS_PER_DAY);
            case HOURS:
                return (timeout * MILLIS_PER_HOUR);
            case MINUTES:
                return (timeout * MILLIS_PER_MINUTE);
            case SECONDS:
                return (timeout * MILLIS_PER_SECOND);
            case MILLISECONDS:
                return timeout;
            default: //MICROS & NANOS
                return -1;
        }
    }

    @Override
    public WatchKey take() {
        while (running) {
            WatchKey key = poll();
            if (key != null) {
                return key;
            }
            try {
                Thread.sleep(PERIOD);
            } catch (InterruptedException e) {
            }
        }
        throw new ClosedWatchServiceException();
    }
}
