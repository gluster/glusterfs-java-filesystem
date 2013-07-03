package com.peircean.glusterfs;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class GlusterFileStore extends FileStore {
    public static final String GLUSTERFS = "glusterfs";
    @NonNull
    private GlusterFileSystem fileSystem;

    @Override
    public String name() {
        return fileSystem.getVolname();
    }

    @Override
    public String type() {
        return GLUSTERFS;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public long getTotalSpace() throws IOException {
        GlusterFileSystemProvider provider = (GlusterFileSystemProvider) fileSystem.provider();
        return provider.getTotalSpace(fileSystem.getVolptr());
    }

    @Override
    public long getUsableSpace() throws IOException {
        GlusterFileSystemProvider provider = (GlusterFileSystemProvider) fileSystem.provider();
        return provider.getUsableSpace(fileSystem.getVolptr());
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        GlusterFileSystemProvider provider = (GlusterFileSystemProvider) fileSystem.provider();
        return provider.getUnallocatedSpace(fileSystem.getVolptr());
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> aClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsFileAttributeView(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> vClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttribute(String s) throws IOException {
        throw new UnsupportedOperationException();
    }
}
