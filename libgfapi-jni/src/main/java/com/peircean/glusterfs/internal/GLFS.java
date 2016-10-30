/*
    Copyright (c) 2013 Louis Zuckerman All rights reserved.
    
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are
    met:
    
       * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
       * Redistributions in binary form must reproduce the above
    copyright notice, this list of conditions and the following disclaimer
    in the documentation and/or other materials provided with the
    distribution.
       * Neither the names of the authors nor the names of 
    contributors may be used to endorse or promote products derived from
    this software without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
    "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
    LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
    A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
    OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
    SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
    LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.peircean.glusterfs.internal;

import com.peircean.glusterfs.internal.structs.dirent;
import com.peircean.glusterfs.internal.structs.stat;
import com.peircean.glusterfs.internal.structs.statvfs;
import org.fusesource.hawtjni.runtime.JniArg;
import org.fusesource.hawtjni.runtime.JniClass;
import org.fusesource.hawtjni.runtime.JniMethod;
import org.fusesource.hawtjni.runtime.Library;

import static org.fusesource.hawtjni.runtime.ArgFlag.NO_IN;
import static org.fusesource.hawtjni.runtime.ArgFlag.NO_OUT;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a> & <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@JniClass
public class GLFS {

    public static final Library LIBRARY = new Library("libgfapi-jni", GLFS.class);

    static {
        GLFS.LIBRARY.load();
    }

    @JniMethod(cast = "glfs_t *")
    public static final native long glfs_new(
            @JniArg(cast = "const char*") String volname
    );

    @JniMethod
    public static final native int glfs_set_volfile_server(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char*") String transport,
            @JniArg(cast = "const char*") String host,
            int port
    );

    @JniMethod
    public static final native int glfs_set_logging(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char*") String logfile,
            int loglevel);

    @JniMethod
    public static final native int glfs_init(
            @JniArg(cast = "glfs_t *") long fs
    );

    @JniMethod
    public static final native int glfs_fini(
            @JniArg(cast = "glfs_t *") long fs
    );

    @JniMethod(cast = "glfs_fd_t *")
    public static final native long glfs_open(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char*") String path,
            int flags);

    @JniMethod(cast = "glfs_fd_t *")
    public static final native long glfs_creat(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char*") String path,
            int flags,
            int mode);

    @JniMethod
    public static final native int glfs_close(
            @JniArg(cast = "glfs_fd_t *") long fd);

    @JniMethod
    public static final native int glfs_fsync(
            @JniArg(cast = "glfs_fd_t *") long fd);

    @JniMethod(cast = "glfs_t *")
    public static final native long glfs_from_glfd(
            @JniArg(cast = "glfs_fd_t *") long fd);

    @JniMethod
    public static final native int glfs_write(
            @JniArg(cast = "glfs_fd_t *") long fd,
            @JniArg(cast = "const char*", flags = NO_OUT) byte[] buf,
            int count,
            int flags);

    @JniMethod(cast = "ssize_t")
    public static final native long glfs_read(
            @JniArg(cast = "glfs_fd_t *") long fd,
            @JniArg(cast = "void *", flags = NO_IN) byte[] buf,
            @JniArg(cast = "size_t") long count,
            int flags
    );

    @JniMethod(cast = "off_t")
    public static final native int glfs_lseek(
            @JniArg(cast = "glfs_fd_t *") long fd,
            @JniArg(cast = "off_t") long offset,
            int whence
    );

    @JniMethod
    public static final native int glfs_unlink(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String path);

//    int glfs_statvfs (glfs_t *fs, const char *path, struct statvfs *buf);

    @JniMethod
    public static final native int glfs_statvfs(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String path,
            @JniArg(flags = NO_IN) statvfs buf);


    //    int glfs_stat (glfs_t *fs, const char *path, struct stat *buf);
    @JniMethod
    public static final native int glfs_stat(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String path,
            @JniArg(flags = NO_IN) stat buf);

    //    int glfs_lstat (glfs_t *fs, const char *path, struct stat *buf);
    @JniMethod
    public static final native int glfs_lstat(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String path,
            @JniArg(flags = NO_IN) stat buf);

    //    int glfs_fstat (glfs_fd_t *fd, struct stat *buf);
    @JniMethod
    public static final native int glfs_fstat(
            @JniArg(cast = "glfs_fd_t *") long fd,
            @JniArg(flags = NO_IN) stat buf);

    //    int glfs_access (glfs_t *fs, const char *path, int mode);
    @JniMethod
    public static final native int glfs_access(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String path,
            int mode
    );

    //    int glfs_mkdir(glfs_t *fs, const char *path, int mode);
    @JniMethod
    public static final native int glfs_mkdir(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String path,
            int mode
    );

    //    glfs_fd_t *glfs_opendir (glfs_t *fs, const char *path);
    @JniMethod(cast = "glfs_fd_t *")
    public static final native long glfs_opendir(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String path
    );

    //    int glfs_readdir_r (glfs_fd_t *fd, struct dirent *dirent, struct dirent **result);
    @JniMethod
    public static final native int glfs_readdir_r(
            @JniArg(cast = "glfs_fd_t *") long fd,
            @JniArg(flags = NO_IN) dirent dirent,
            @JniArg(cast = "struct dirent **") long result
    );

    //    long glfs_telldir (glfs_fd_t *fd);
    @JniMethod
    public static final native long glfs_telldir(
            @JniArg(cast = "glfs_fd_t *") long fd
    );

    //    void glfs_seekdir (glfs_fd_t *fd, long offset);
    @JniMethod
    public static final native void glfs_seekdir(
            @JniArg(cast = "glfs_fd_t *") long fd,
            long offset
    );

    //    int glfs_closedir (glfs_fd_t *fd);
    @JniMethod
    public static final native int glfs_closedir(
            @JniArg(cast = "glfs_fd_t *") long fd
    );

    //    int glfs_rmdir(glfs_t *fs, const char *path);
    @JniMethod
    public static final native int glfs_rmdir(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String path
    );

    //    int glfs_rename (glfs_t *fs, const char *oldpath, const char *newpath);
    @JniMethod
    public static final native int glfs_rename(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String oldpath,
            @JniArg(cast = "const char *") String newpath
    );

    //  int glfs_chmod (glfs_t *fs, const char *path, int mode);
    public static final native int glfs_chmod(
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String path,
            int mode
    );

    //    int glfs_symlink (glfs_t *fs, const char *oldpath, const char *newpath);
    @JniMethod
    public static final native int glfs_symlink (
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String oldpath,
            @JniArg(cast = "const char *") String newpath
    );

    //    int glfs_readlink (glfs_t *fs, const char *path, char *buf, size_t bufsiz);
    @JniMethod
    public static final native int glfs_readlink (
            @JniArg(cast = "glfs_t *") long fs,
            @JniArg(cast = "const char *") String path,
            @JniArg(cast = "char *", flags = NO_IN) byte[] buf,
            @JniArg(cast = "size_t") long bufsiz
    );
}
