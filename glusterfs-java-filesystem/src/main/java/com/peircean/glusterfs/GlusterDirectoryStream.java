package com.peircean.glusterfs;

import com.peircean.libgfapi_jni.internal.GLFS;
import lombok.Data;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

@Data
public class GlusterDirectoryStream implements DirectoryStream<Path> {
    private GlusterFileSystem fileSystem;
    private long dirHandle = 0;
    private GlusterDirectoryIterator iterator;
    private boolean closed = false;
    private GlusterPath dir;
    private DirectoryStream.Filter<? super Path> filter;

    @Override
    public Iterator<Path> iterator() {
        if (null != iterator || closed) {
            throw new IllegalStateException("Already iterating!");
        }
        GlusterDirectoryIterator iterator = new GlusterDirectoryIterator();
        iterator.setStream(this);
        iterator.setFilter(filter);
        this.iterator = iterator;
        return iterator;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            GLFS.glfs_close(dirHandle);
            closed = true;
        }
    }

    public void open(GlusterPath path) {
        dir = path;
        if (dirHandle == 0) {
            dirHandle = GLFS.glfs_opendir(path.getFileSystem().getVolptr(), path.getString());
        } else {
            throw new IllegalStateException("Already open!");
        }
    }
}
