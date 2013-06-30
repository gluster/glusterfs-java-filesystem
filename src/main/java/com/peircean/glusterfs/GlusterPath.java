package com.peircean.glusterfs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Iterator;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class GlusterPath implements Path {
    @Override
    public FileSystem getFileSystem() {
        return null;
    }

    @Override
    public boolean isAbsolute() {
        return false;
    }

    @Override
    public Path getRoot() {
        return null;
    }

    @Override
    public Path getFileName() {
        return null;
    }

    @Override
    public Path getParent() {
        return null;
    }

    @Override
    public int getNameCount() {
        return 0;
    }

    @Override
    public Path getName(int i) {
        return null;
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
        return null;
    }

    @Override
    public WatchKey register(WatchService watchService, WatchEvent.Kind<?>... kinds) throws IOException {
        return null;
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
