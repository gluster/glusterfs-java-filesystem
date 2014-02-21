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
        DirectoryStream<Path> paths;
        try {
            paths = Files.newDirectoryStream(path);
        } catch (IOException e) {
            return false;
        }
        List<Path> files = new LinkedList<>();
        boolean newEvents = false;
        for (Path f : paths) {
            newEvents |= processExistingFile(files, f);
        }
        for (Path f : events.keySet()) {
            newEvents |= checkDeleted(files, f);
        }
        return newEvents;
    }

    boolean processExistingFile(List<Path> files, Path f) {
        if (Files.isDirectory(f)) {
            return false;
        }
        files.add(f);

        long lastModified;
        try {
            lastModified = Files.getLastModifiedTime(f).toMillis();
        } catch (IOException e) {
            return false;
        }

        GlusterWatchEvent event = events.get(f);
        if (null != event) {
            return checkModified(event, lastModified);
        } else {
            return checkCreated(f, lastModified);
        }
    }

    boolean checkDeleted(List<Path> files, Path f) {
        GlusterWatchEvent event = events.get(f);
        if (!files.contains(f) &&
                !StandardWatchEventKinds.ENTRY_DELETE.name().equals(event.kind().name())) {
            event.setLastModified((new Date()).getTime());
            event.setKind(StandardWatchEventKinds.ENTRY_DELETE);
            event.setCount(event.getCount() + 1);
            return true;
        }
        return false;
    }

    boolean checkCreated(Path f, long lastModified) {
        GlusterWatchEvent event = new GlusterWatchEvent(f.getFileName());
        event.setLastModified(lastModified);
        events.put(f, event);
        return (lastModified > lastPolled);
    }

    boolean checkModified(GlusterWatchEvent event, long lastModified) {
        if (lastModified > event.getLastModified()) {
            event.setLastModified(lastModified);
            if (event.kind().name().equals(StandardWatchEventKinds.ENTRY_DELETE.name())) {
                event.setKind(StandardWatchEventKinds.ENTRY_CREATE);
                event.setCount(0);
            } else {
                event.setKind(StandardWatchEventKinds.ENTRY_MODIFY);
                event.setCount(event.getCount() + 1);
            }
            return true;
        }
        return false;
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
        return findPendingEvents();
    }

    LinkedList<WatchEvent<?>> findPendingEvents() {
        long maxModifiedTime = lastPolled;
        LinkedList<WatchEvent<?>> pendingEvents = new LinkedList<>();
        for (Path p : events.keySet()) {
            long lastModified = queueEventIfPending(pendingEvents, p);
            maxModifiedTime = Math.max(maxModifiedTime, lastModified);
        }
        lastPolled = maxModifiedTime;
        return pendingEvents;
    }

    private long queueEventIfPending(LinkedList<WatchEvent<?>> pendingEvents, Path p) {
        GlusterWatchEvent e = events.get(p);
        long lastModified = e.getLastModified();
        if (lastModified > lastPolled && kindsContains(e.kind())) {
            pendingEvents.add(e);
        }
        return lastModified;
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
