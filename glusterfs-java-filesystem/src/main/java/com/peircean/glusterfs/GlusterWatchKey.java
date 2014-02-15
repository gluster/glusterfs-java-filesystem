package com.peircean.glusterfs;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Data
@EqualsAndHashCode(of = "path")
public class GlusterWatchKey implements WatchKey {
    private boolean valid = true;
    private boolean ready = true;
    Map<Path, GlusterWatchEvent> events = new HashMap<>();
    final private GlusterPath path;
    @NonNull
    private WatchEvent.Kind[] kinds;
    private long lastPolled = (new Date()).getTime();

    @Override
    public boolean isValid() {
        return valid;
    }

    public boolean update() {
        List<Path> files = new LinkedList<>();
        boolean newEvents = false;
        try {
            DirectoryStream<Path> paths = Files.newDirectoryStream(path);
            for (Path f : paths) {
                if (!Files.isDirectory(f)) {
                    files.add(f);
                    GlusterWatchEvent event = events.get(f);
                    long lastModified = Files.getLastModifiedTime(f).toMillis();
                    if (null != event) {
                        if (lastModified > event.getLastModified()) {
                            event.setLastModified(lastModified);
                            event.setKind(StandardWatchEventKinds.ENTRY_MODIFY);
                            event.setCount(event.getCount() + 1);
                            newEvents = true;
                        }
                    } else {
                        event = new GlusterWatchEvent(f);
                        event.setLastModified(lastModified);
                        events.put(f, event);
                        if (lastModified > lastPolled) {
                            newEvents = true;
                        }
                    }
                }
            }
            for (Path f : events.keySet()) {
                GlusterWatchEvent event = events.get(f);
                if (!files.contains(f) &&
                        !StandardWatchEventKinds.ENTRY_DELETE.name().equals(event.kind().name())) {
                    event.setLastModified((new Date()).getTime());
                    event.setKind(StandardWatchEventKinds.ENTRY_DELETE);
                    event.setCount(event.getCount() + 1);
                    newEvents = true;
                }
            }
            return newEvents;
        } catch (IOException e) {
            return false;
        }
    }

    boolean kindsContains(WatchEvent.Kind kind) {
        for (WatchEvent.Kind k : kinds) {
            if (k.name().equals(kind.name())) {
                return true;
            }
        }
        return false;
    }

    @Override
    synchronized public List<WatchEvent<?>> pollEvents() {
        if (!ready) {
            return new LinkedList<>();
        }
        ready = false;
        long maxModifiedTime = lastPolled;
        LinkedList<WatchEvent<?>> pendingEvents = new LinkedList<>();
        for (Path p : events.keySet()) {
            GlusterWatchEvent e = events.get(p);
            long lastModified = e.getLastModified();
            if (lastModified > lastPolled && kindsContains(e.kind())) {
                pendingEvents.add(e);
            }
            maxModifiedTime = Math.max(maxModifiedTime, lastModified);
        }
        lastPolled = maxModifiedTime;
        return pendingEvents;
    }

    @Override
    synchronized public boolean reset() {
        if (!valid || ready) {
            return false;
        } else {
            ready = true;
            return true;
        }
    }

    @Override
    public void cancel() {
        valid = false;
    }

    @Override
    public Watchable watchable() {
        return path;
    }
}
