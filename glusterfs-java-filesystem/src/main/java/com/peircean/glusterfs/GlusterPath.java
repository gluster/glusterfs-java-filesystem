package com.peircean.glusterfs;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@Data
@EqualsAndHashCode(exclude = "pathString")
public class GlusterPath implements Path {
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
        this.pathString = path;

        String stripped = path;
        if (path.startsWith(fileSystem.getSeparator())) {
            absolute = true;
            stripped = stripped.substring(1);
        }
        if (stripped.endsWith(fileSystem.getSeparator())) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }
        parts = stripped.split(fileSystem.getSeparator());
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
        return ((!otherPath.isAbsolute())
                && (Arrays.equals(thisSuffix, otherPath.getParts())));
    }

    @Override
    public boolean endsWith(String s) {
        return toString().endsWith(s);
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
        if (!fileSystem.equals(otherPath.getFileSystem())) {
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
        return getParent().resolve(path);
    }

    @Override
    public Path resolveSibling(String s) {
        return getParent().resolve(s);
    }

    @Override
    public Path relativize(Path path) {
        if (!fileSystem.equals(path.getFileSystem())) {
            throw new IllegalArgumentException("Can not relativize other path because it's on a different filesystem");
        }

        if (!this.isAbsolute() || !path.isAbsolute()) {
            throw new IllegalArgumentException("Can only relativize when both paths are absolute");
        }
        GlusterPath other = (GlusterPath) path;
        List<String> relativeParts = new LinkedList<String>();
        boolean stillCommon = true;
        int lastCommonName = -1;
        for (int i = 0; i < parts.length; i++) {
            if (i >= other.getParts().length) {
                for (int r = 0; r < other.getParts().length; r++) {
                    relativeParts.add("..");
                }
                break;
            }
            if (stillCommon && parts[i].equals(other.getParts()[i])) {
                lastCommonName = i;
            } else {
                stillCommon = false;
                relativeParts.add("..");
            }
        }
        for (int i = lastCommonName + 1; i < other.getParts().length; i++) {
            relativeParts.add(other.getParts()[i]);
        }
        return new GlusterPath(fileSystem, relativeParts.toArray(new String[]{}), false);
    }

    @Override
    public URI toUri() {
        try {
            GlusterFileSystem fs = getFileSystem();
            String authority = fs.getHost() + ":" + fs.getVolname();
            return new URI(fs.provider().getScheme(), authority, toString(), null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Path toAbsolutePath() {
        if (!absolute) {
            throw new UnsupportedOperationException();
        } else {
            return this;
        }
    }

    @Override
    public Path toRealPath(LinkOption... linkOptions) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(WatchService watchService, WatchEvent.Kind<?>[] kinds, WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("GlusterWatchService does not support modifiers at this time.");
    }

    @Override
    public WatchKey register(WatchService watchService, WatchEvent.Kind<?>... kinds) throws IOException {
        guardRegisterWatchService(watchService);
        guardRegisterWatchDirectory();

        return ((GlusterWatchService) watchService).registerPath(this, kinds);
    }

    void guardRegisterWatchDirectory() throws NotDirectoryException {
        if (!Files.isDirectory(this)) {
            throw new NotDirectoryException("GlusterWatchService can only watch directories.  Not a directory: " + this);
        }
    }

    void guardRegisterWatchService(WatchService watchService) {
        Class<? extends WatchService> watchServiceClass = watchService.getClass();
        if (!GlusterWatchService.class.equals(watchServiceClass)) {
            throw new UnsupportedOperationException("GlusterPaths can only be watched by GlusterWatchServices. WatchService given: " + watchServiceClass);
        }
    }

    @Override
    public Iterator<Path> iterator() {
        List<Path> list = new ArrayList<Path>(parts.length);
        if (parts.length >= 1 && !parts[0].isEmpty()) {
            for (String p : parts) {
                list.add(new GlusterPath(fileSystem, p));
            }
        }
        return Collections.unmodifiableList(list).iterator();
    }

    @Override
    public int compareTo(Path path) {
        if (!getClass().equals(path.getClass())) {
            throw new ClassCastException();
        }
        if (!fileSystem.equals(path.getFileSystem())) {
            throw new IllegalArgumentException("Can not compare other path because it's on a different filesystem");
        }
        GlusterPath other = (GlusterPath) path;
        String[] otherParts = other.getParts();
        for (int i = 0; i < Math.min(parts.length, otherParts.length); i++) {
            int c = parts[i].compareTo(otherParts[i]);
            if (c != 0) {
                return c;
            }
        }
        return parts.length - otherParts.length;
    }

    public String toString() {
        return /*fileSystem.toString() +*/ getString();
    }

    public String getString() {
        if (null != pathString) {
            return pathString;
        } else {
            StringBuilder sb = new StringBuilder((absolute ? fileSystem.getSeparator() : ""));
            for (String p : parts) {
                sb.append(p).append(fileSystem.getSeparator());
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }
    }
}
