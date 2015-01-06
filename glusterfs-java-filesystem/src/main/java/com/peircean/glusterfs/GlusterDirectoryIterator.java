package com.peircean.glusterfs;

import com.peircean.libgfapi_jni.internal.GLFS;
import com.peircean.libgfapi_jni.internal.structs.dirent;
import lombok.Data;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

@Data
class GlusterDirectoryIterator<T> implements Iterator<GlusterPath> {
    private GlusterDirectoryStream stream;
    private DirectoryStream.Filter<? super Path> filter;
    private dirent current, next;
    private GlusterPath nextPath;

    @Override
    public boolean hasNext() {
        advance();
        if (null != filter) {
            try {
                while (next.d_ino != 0 && !filter.accept(nextPath)) {
                    advance();
                }
            } catch (IOException e) {
                current = null;
                next = null;
                return false;
            }
        }

        if (next != null && next.d_ino == 0) {
            current = null;
            next = null;
            return false;
        }

        return true;
    }

    void advance() {
        String name;
        do {
            current = new dirent();
            long nextPtr = dirent.malloc(dirent.SIZE_OF);
            GLFS.glfs_readdir_r(stream.getDirHandle(), current, nextPtr);

            next = new dirent();
            dirent.memmove(next, nextPtr, dirent.SIZE_OF);
            dirent.free(nextPtr);

            name = current.getName();
            nextPath = (GlusterPath) stream.getDir().resolve(name);
        } while (name.equals(".") || name.equals(".."));
    }

    @Override
    public GlusterPath next() {
        if (nextPath == null) {
            throw new NoSuchElementException("No more entries");
        }

        return nextPath;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
