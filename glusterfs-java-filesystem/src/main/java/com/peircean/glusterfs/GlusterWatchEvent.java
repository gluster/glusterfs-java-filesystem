package com.peircean.glusterfs;

import lombok.Data;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

@Data
public class GlusterWatchEvent implements WatchEvent<Path> {
    final private Path path;
    private Kind<Path> kind = StandardWatchEventKinds.ENTRY_CREATE;
    private int count = 0;
    private long lastModified;

    public Kind<Path> kind() {
        return kind;
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public Path context() {
        return path;
    }
}
