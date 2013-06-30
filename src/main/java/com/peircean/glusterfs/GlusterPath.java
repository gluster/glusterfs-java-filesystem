package com.peircean.glusterfs;

import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@Data
public class GlusterPath implements Path {
    public static final String SEPARATOR = "/";
    private GlusterFileSystem fileSystem;
    private String[] parts;
    private boolean absolute;

    public GlusterPath(GlusterFileSystem fileSystem, String path) {
        if (null == fileSystem) {
            throw new IllegalArgumentException("fileSystem can not be empty");
        }
        if (null == path || path.isEmpty()) {
            throw new IllegalArgumentException("path can not be empty");
        }
        this.fileSystem = fileSystem;

        String stripped = path;
        if (path.startsWith(SEPARATOR)) {
            absolute = true;
            stripped = stripped.substring(1);
        }
        if (stripped.endsWith(SEPARATOR)) {
            stripped.substring(0, stripped.length() - 1);
        }
        parts = stripped.split(SEPARATOR);
    }

    GlusterPath(GlusterFileSystem fileSystem, String[] parts, boolean absolute) {
        this.fileSystem = fileSystem;
        this.parts = parts;
        this.absolute = absolute;
    }

    @Override
    public boolean isAbsolute() {
        return absolute;
    }

    @Override
    public Path getRoot() {
        if (absolute) {
            return fileSystem.getRootDirectories().iterator().next();
        } else {
            return null;
        }
    }

    @Override
    public Path getFileName() {
        if (parts.length == 0 || parts[0].isEmpty()) {
            return null;
        } else {
            return new GlusterPath(fileSystem, parts[parts.length - 1]);
        }
    }

    @Override
    public Path getParent() {
        if (parts.length <= 1 || parts[0].isEmpty()) {
            if (absolute) {
                return getRoot();
            } else {
                return null;
            }
        } else {
            return new GlusterPath(fileSystem, Arrays.copyOfRange(parts, 0, parts.length - 1), absolute);
        }
    }

    @Override
    public int getNameCount() {
        if (parts.length <= 1 && parts[0].isEmpty()) {
            if (absolute) {
                return 0;
            } else {
                throw new IllegalStateException("Only the root path should have one empty part");
            }
        } else {
            return parts.length;
        }
    }

    @Override
    public Path getName(int i) {
        if (i < 0 || i >= parts.length || (0 == i && parts.length <= 1 && parts[0].isEmpty())) {
            throw new IllegalArgumentException("invalid name index");
        }
        return new GlusterPath(fileSystem, Arrays.copyOfRange(parts, 0, i + 1), absolute);
    }

    @Override
    public Path subpath(int i, int i2) {
        return null;
    }

    @Override
    public boolean startsWith(Path path) {
        return false;
    }

    @Override
    public boolean startsWith(String s) {
        return false;
    }

    @Override
    public boolean endsWith(Path path) {
        return false;
    }

    @Override
    public boolean endsWith(String s) {
        return false;
    }

    @Override
    public Path normalize() {
        return null;
    }

    @Override
    public Path resolve(Path path) {
        return null;
    }

    @Override
    public Path resolve(String s) {
        return null;
    }

    @Override
    public Path resolveSibling(Path path) {
        return null;
    }

    @Override
    public Path resolveSibling(String s) {
        return null;
    }

    @Override
    public Path relativize(Path path) {
        return null;
    }

    @Override
    public URI toUri() {
        return null;
    }

    @Override
    public Path toAbsolutePath() {
        return null;
    }

    @Override
    public Path toRealPath(LinkOption... linkOptions) throws IOException {
        return null;
    }

    @Override
    public File toFile() {
        return null;
    }

    @Override
    public WatchKey register(WatchService watchService, WatchEvent.Kind<?>[] kinds, WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(WatchService watchService, WatchEvent.Kind<?>... kinds) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Path> iterator() {
        return null;
    }

    @Override
    public int compareTo(Path path) {
        return 0;
    }
}
