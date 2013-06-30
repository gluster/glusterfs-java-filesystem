package com.peircean.glusterfs;

import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@Data
public class GlusterPath implements Path {
    public static final String SEPARATOR = "/";
    private GlusterFileSystem fileSystem;
    private String[] parts;
    private String pathString;
    private boolean absolute;

    public GlusterPath(GlusterFileSystem fileSystem, String path) {
        if (null == fileSystem) {
            throw new IllegalArgumentException("fileSystem can not be empty");
        }
        if (null == path) {
            throw new InvalidPathException("", "path can not be null");
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
        if ((0 == i && parts.length <= 1 && parts[0].isEmpty())
                || i < 0 || i2 < 0
                || i >= parts.length || i2 > parts.length
                || i > i2) {
            throw new IllegalArgumentException("invalid indices");
        }
        return new GlusterPath(fileSystem, Arrays.copyOfRange(parts, i, i2), absolute);
    }

    @Override
    public boolean startsWith(Path path) {
        GlusterPath otherPath = (GlusterPath) path;
        if (this.equals(otherPath)) {
            return true;
        }
        if (otherPath.getParts().length > parts.length) {
            return false;
        }
        if (absolute && otherPath.isAbsolute() && otherPath.getParts()[0].isEmpty()) {
            return true;
        }
        String[] thisPrefix = Arrays.copyOfRange(parts, 0, otherPath.getParts().length);
        return ((absolute == otherPath.isAbsolute())
                && (Arrays.equals(thisPrefix, otherPath.getParts())));
    }

    @Override
    public boolean startsWith(String s) {
        return startsWith(new GlusterPath(fileSystem, s));
    }

    @Override
    public boolean endsWith(Path path) {
        GlusterPath otherPath = (GlusterPath) path;
        if (this.equals(otherPath)) {
            return true;
        }
        if (otherPath.getParts().length > parts.length) {
            return false;
        }
        if (absolute && otherPath.isAbsolute() && otherPath.getParts()[0].isEmpty()) {
            return true;
        }
        String[] thisSuffix = Arrays.copyOfRange(parts, parts.length - otherPath.getParts().length, parts.length);
        return ((false == otherPath.isAbsolute())
                && (Arrays.equals(thisSuffix, otherPath.getParts())));
    }

    @Override
    public boolean endsWith(String s) {
        return endsWith(new GlusterPath(fileSystem, s));
    }

    @Override
    public Path normalize() {
        List<String> newParts = new LinkedList<String>();
        for (String part : parts) {
            if (part.equals("..")) {
                newParts.remove(newParts.size() - 1);
            } else if (!part.equals(".") && !part.isEmpty()) {
                newParts.add(part);
            }
        }
        return new GlusterPath(fileSystem, newParts.toArray(new String[]{}), absolute);
    }

    @Override
    public Path resolve(Path path) {
        GlusterPath otherPath = (GlusterPath) path;
        if (!otherPath.getFileSystem().equals(fileSystem)) {
            throw new IllegalArgumentException("Can not resolve other path because it's on a different filesystem");
        }
        
        if (otherPath.isAbsolute() || (absolute && parts.length == 1 && parts[0].isEmpty())) {
            return new GlusterPath(fileSystem, otherPath.getParts(), true);
        }

        if (otherPath.getParts().length == 1 && otherPath.getParts()[0].isEmpty()) {
            return this;
        }
        String[] newParts = Arrays.copyOf(parts, parts.length + otherPath.getParts().length);
        System.arraycopy(otherPath.getParts(), 0, newParts, parts.length, otherPath.getParts().length);
        return new GlusterPath(fileSystem, newParts, absolute);
    }

    @Override
    public Path resolve(String s) {
        return resolve(new GlusterPath(fileSystem, s));
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
