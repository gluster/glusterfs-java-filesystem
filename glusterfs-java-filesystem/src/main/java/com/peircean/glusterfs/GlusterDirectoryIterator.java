package com.peircean.glusterfs;

import com.peircean.libgfapi_jni.internal.GLFS;
import com.peircean.libgfapi_jni.internal.structs.dirent;
import lombok.Data;

import java.util.Iterator;
import java.util.NoSuchElementException;

@Data
class GlusterDirectoryIterator<T> implements Iterator<GlusterPath> {
    private GlusterDirectoryStream stream;
    private dirent current, next;

    @Override
    public boolean hasNext() {
        current = new dirent();
        long nextPtr = dirent.malloc(dirent.SIZE_OF);
        GLFS.glfs_readdir_r(stream.getDirHandle(), current, nextPtr);

        next = new dirent();
        dirent.memmove(next, nextPtr, dirent.SIZE_OF);
        dirent.free(nextPtr);

        if (next != null && next.d_ino == 0) {
            current = null;
            next = null;
            return false;
        }

        return true;
    }

    @Override
    public GlusterPath next() {
        if (current == null) {
            throw new NoSuchElementException("No more entries");
        }

        String name = current.getName();
        return (GlusterPath) stream.getDir().resolve(name);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
