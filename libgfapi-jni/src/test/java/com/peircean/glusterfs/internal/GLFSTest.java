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
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Properties;

import static com.peircean.glusterfs.internal.GLFS.*;
import static com.peircean.glusterfs.internal.GLFS.glfs_rmdir;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * A Unit test for the GLFS class implementation.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a> & <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class GLFSTest {

    public static final String DIR_PATH = "/baz/";
    public static final String FILE_PATH = DIR_PATH + "bar";
    public static final String FILE_PATH_RENAMED = DIR_PATH + "bar2";
    public static final String HELLO_ = "hello ";
    public static final String WORLD = "world";
    public static final String SYMLINK = "/symlink";
    public static final String SYMLINK_TARGET = "/symlink_target";
    public static stat stat = new stat();
    private long vol;
    private long file;
    private long dir;
    private long dirpos;
    private Properties properties = null;

    private Properties getProperties() throws IOException {
        if (null == properties) {
            properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("testing.properties"));
        }
        return properties;
    }

    @Test
    public void testNew() throws IOException {
        String volname = getProperties().getProperty("glusterfs.volume");
        vol = glfs_new(volname);
        System.out.println("NEW: " + vol);
        assertTrue(0 < vol);
    }

    @Test(dependsOnMethods = "testNew")
    public void testSetlog() throws IOException {
        String logfile = getProperties().getProperty("client.logfile");
        int setlog = glfs_set_logging(vol, logfile, 20);
        System.out.println("SETLOG: " + setlog);
        assertEquals(0, setlog);
    }

    @Test(dependsOnMethods = "testSetlog")
    public void testServer() throws IOException {
        String address = getProperties().getProperty("glusterfs.server");

        int server = glfs_set_volfile_server(vol, "tcp", address, 24007);
        System.out.println("SERVER: " + server);
        assertEquals(0, server);
    }

    @Test(dependsOnMethods = "testServer")
    public void testInit() {
        int init = glfs_init(vol);
        System.out.println("INIT: " + init);
        assertEquals(0, init);
    }

    @Test(dependsOnMethods = "testInit")
    public void testMkdir() {
        int ret = glfs_mkdir(vol, DIR_PATH, 0777);
        System.out.println("CREATE STATUS: " + ret);
        assertEquals(0, ret);
    }

    @Test(dependsOnMethods = "testMkdir")
    public void testOpen_nonExisting() {
        file = glfs_open(vol, FILE_PATH, 0);
        System.out.println("OPEN: " + file);
        int errno = UtilJNI.errno();
        System.out.println("ERRNO: " + errno);
        String strerror = UtilJNI.strerror();
        System.out.println("STRERROR: " + strerror);
        assertEquals(0, file);
        assertEquals("No such file or directory", strerror);
        assertEquals(2, errno);
    }

    @Test(dependsOnMethods = "testOpen_nonExisting")
    public void testCreate() {
        int flags = GlusterOpenOption.READWRITE().createNew().getValue();
        System.out.println("OPENEx flags: " + Integer.toOctalString(flags));
        file = glfs_creat(vol, FILE_PATH, flags, 0666);
        System.out.println("CREAT: " + file);
        assertTrue(file > 0);
    }

    @Test(dependsOnMethods = "testCreate")
    public void testSymlink() {
        int sym = glfs_symlink(vol, SYMLINK_TARGET, SYMLINK);
        System.out.println("SYMLINK create: " + sym);
        assertEquals(0, sym);
    }

    @Test(dependsOnMethods = "testSymlink")
    public void testReadlink() {
        int length = SYMLINK_TARGET.length();
        byte[] content = new byte[length];
        long read = glfs_readlink(vol, SYMLINK, content, length);

        String readValue = new String(content);
        System.out.println("SYMLINK val: " + readValue);
        System.out.println("SYMLINK len: " + read);

        assertEquals(length, read);
        assertEquals(SYMLINK_TARGET, readValue);

    }

    @Test(dependsOnMethods = "testReadlink")
    public void testWriteNew() {
        int length = HELLO_.length();
        int write = glfs_write(file, HELLO_.getBytes(), length, 0);

        System.out.println("WRITE: " + write);

        assertEquals(length, write);
    }

    @Test(dependsOnMethods = "testWriteNew")
    public void testClose_new() {
        int close = glfs_close(file);
        System.out.println("CLOSE: " + close);
        assertEquals(0, close);
    }

    @Test(dependsOnMethods = "testClose_new")
    public void testOpen_existing() {
        int flags = GlusterOpenOption.READWRITE().append().getValue();
        System.out.println("OPENEx flags: " + Integer.toOctalString(flags));
        file = glfs_open(vol, FILE_PATH, flags);
        System.out.println("OPENEx: " + file);
        assertTrue(0 < file);
    }

    @Test(dependsOnMethods = "testOpen_existing")
    public void testWrite_existing() {
        int length = WORLD.length();
        int write = glfs_write(file, WORLD.getBytes(), length, 0);

        System.out.println("WRITEEx: " + write);

        assertEquals(length, write);
    }

    @Test(dependsOnMethods = "testWrite_existing")
    public void testSeek() {
        int seek = glfs_lseek(file, 0, 0);
        System.out.println("SEEK: " + seek);
        assertEquals(0, seek);
    }

    @Test(dependsOnMethods = "testSeek")
    public void testRead() {
        String helloWorld = HELLO_ + WORLD;
        int length = helloWorld.length();
        byte[] content = new byte[length];
        long read = glfs_read(file, content, length, 0);

        String readValue = new String(content);
        System.out.println("READ val: " + readValue);
        System.out.println("READ len: " + read);

        assertEquals(length, read);
        assertEquals(helloWorld, readValue);
    }

    @Test(dependsOnMethods = "testRead")
    public void testStats() {
        stat stat = new stat();
        int statR = GLFS.glfs_stat(vol, FILE_PATH, stat);
        stat lstat = new stat();
        int lstatR = GLFS.glfs_lstat(vol, FILE_PATH, lstat);
        stat fstat = new stat();
        int fstatR = GLFS.glfs_fstat(file, fstat);

        System.out.println("STATr: " + statR);
        System.out.println("LSTATr: " + lstatR);
        System.out.println("FSTATr: " + fstatR);
        System.out.println("STAT: " + stat);
        System.out.println("LSTAT: " + lstat);
        System.out.println("FSTAT: " + fstat);
        assertEquals(stat, lstat);
        assertEquals(lstat, fstat);
        assertEquals(4096, stat.st_blksize);
        assertEquals(11, stat.st_size);
        assertEquals(0100666, stat.st_mode);
    }

    @Test(dependsOnMethods = "testStats")
    public void testChmod() {
        int ret = glfs_chmod(vol, FILE_PATH, 0664);
        stat newStat = new stat();
        glfs_stat(vol, FILE_PATH, newStat);

        int newMode = newStat.st_mode;
        int oldMode = stat.st_mode;

        System.out.println("CHMOD " + (ret == 0 ? "success" : "failure"));
        System.out.println("CHMOD old mode: " + oldMode);
        System.out.println("CHMOD new mode: " + newMode);

        assertTrue(newMode != oldMode);
        assertEquals(newMode, 33204);
    }

    @Test(dependsOnMethods = "testChmod")
    public void testFromGlfd() {
        long glfs = glfs_from_glfd(file);
        System.out.println("GLFS_GLFD: " + glfs);
        assertEquals(vol, glfs);
    }

    @Test(dependsOnMethods = "testFromGlfd")
    public void testStatvfs() {
        statvfs buf = new statvfs();
        int statvfs = glfs_statvfs(vol, "/", buf);
        System.out.println("STATVFS: " + statvfs);
        System.out.println(buf.toString());
        assertEquals(0, statvfs);
        assertEquals(4096, buf.f_bsize);
    }

    @Test(dependsOnMethods = "testStatvfs")
    public void testFsync() {
        int fsync = glfs_fsync(file);
        System.out.println("FSYNC: " + fsync);
        assertEquals(0, fsync);
    }

    @Test(dependsOnMethods = "testFsync")
    public void testClose() {
        int close = glfs_close(file);
        System.out.println("CLOSE: " + close);
        assertEquals(0, close);
    }

    @Test(dependsOnMethods = "testClose")
    public void testAccess() {
        int acc = glfs_access(vol, FILE_PATH, 0777);
        System.out.println("ACCESS 777: " + acc);
        int errno = UtilJNI.errno();
        System.out.println("ERRNO: " + errno);
        String strerror = UtilJNI.strerror();
        System.out.println("STRERROR: " + strerror);
        assertEquals(-1, acc);
        assertEquals("Permission denied", strerror);
        assertEquals(13, errno);
        acc = glfs_access(vol, FILE_PATH, 0666);
        System.out.println("ACCESS 666: " + acc);
        assertEquals(0, acc);
        acc = glfs_access(vol, FILE_PATH, 0444);
        System.out.println("ACCESS 444: " + acc);
        assertEquals(0, acc);
        acc = glfs_access(vol, "/au4fh93hf293fa", 0444);
        System.out.println("ACCESS NX: " + acc);
        errno = UtilJNI.errno();
        System.out.println("ERRNO: " + errno);
        strerror = UtilJNI.strerror();
        System.out.println("STRERROR: " + strerror);
        assertEquals(-1, acc);
        assertEquals(2, errno);
        assertEquals("No such file or directory", strerror);
        assertEquals(2, errno);
    }

    @Test(dependsOnMethods = "testAccess")
    public void testOpendir() {
        dir = glfs_opendir(vol, "/");
        System.out.println("OPENDIR: " + dir);
        assertTrue(dir > 0);
    }

    @Test(dependsOnMethods = "testOpendir")
    public void testReaddir() {
        System.out.println("SIZEOF: " + dirent.SIZE_OF);
        dirent dirstruct, result;
        for (int i = 0; i < 15; i++) {
            dirstruct = new dirent();
            long next = dirent.malloc(dirent.SIZE_OF);
            System.out.println("NEXT: " + next);
            int read = glfs_readdir_r(dir, dirstruct, next); //crash
            assertEquals(0, read);
            if (dirstruct.d_off == 0) {
                System.out.println("End of list");
                break;
            }
            System.out.println("READDIR: " + read);
            System.out.println("DIRSTRUCT: " + dirstruct);

            result = new dirent();
            dirent.memmove(result, next, dirent.SIZE_OF);
            dirent.free(next);
        }
    }

    @Test(dependsOnMethods = "testReaddir")
    public void testTelldir() {
        dirpos = glfs_telldir(dir);
        System.out.println("TELLDIR: " + dirpos);
        assertTrue(dirpos > 0);
    }

    @Test(dependsOnMethods = "testTelldir")
    public void testSeekdir() {
        glfs_seekdir(dir, 1l);
    }

    @Test(dependsOnMethods = "testSeekdir")
    public void testClosedir() {
        long close = glfs_closedir(dir);
        System.out.println("CLOSEDIR: " + close);
        assertEquals(0, close);
    }


    @Test(dependsOnMethods = "testClosedir")
    public void testRename() {
        int rename = glfs_rename(vol, FILE_PATH, FILE_PATH_RENAMED);
        System.out.println("RENAME: " + rename);
        assertEquals(0, rename);
    }

    @Test(dependsOnMethods = "testRename")
    public void testRmdir_NotEmpty() {
        int ret = glfs_rmdir(vol, DIR_PATH);
        System.out.println("REMOVE STATUS: " + ret);
        int errno = UtilJNI.errno();
        System.out.println("ERRNO: " + errno);
        String strerror = UtilJNI.strerror();
        System.out.println("STRERROR: " + strerror);
        assertEquals(-1, ret);
        assertEquals("Directory not empty", strerror);
        assertEquals(39, errno);
    }

    @Test(dependsOnMethods = "testRmdir_NotEmpty")
    public void testUnlink() {
        int unl = glfs_unlink(vol, FILE_PATH_RENAMED);
        System.out.println("UNLINK: " + unl);
        assertEquals(0, unl);

        unl = glfs_unlink(vol, SYMLINK);
        System.out.println("UNLINK SYMLINK: " + unl);
        assertEquals(0, unl);
    }

    @Test(dependsOnMethods = "testUnlink")
    public void testUnlink_NonExisting() {
        int unl = glfs_unlink(vol, "/3q9g48hnaovcw802j039f");
        System.out.println("UNLINK NX: " + unl);
        int errno = UtilJNI.errno();
        System.out.println("ERRNO: " + errno);
        String strerror = UtilJNI.strerror();
        System.out.println("STRERROR: " + strerror);
        assertEquals(-1, unl);
        assertEquals("No such file or directory", strerror);
        assertEquals(2, errno);
    }

    @Test(dependsOnMethods = "testUnlink_NonExisting")
    public void testRmdir() {
        int ret = glfs_rmdir(vol, DIR_PATH);
        System.out.println("REMOVE STATUS: " + ret);
        assertEquals(0, ret);
    }

    @Test(dependsOnMethods = "testRmdir")
    public void testRmdir_NonExisting() {
        int ret = glfs_rmdir(vol, DIR_PATH);
        System.out.println("REMOVE STATUS: " + ret);
        int errno = UtilJNI.errno();
        System.out.println("ERRNO: " + errno);
        String strerror = UtilJNI.strerror();
        System.out.println("STRERROR: " + strerror);
        assertEquals(-1, ret);
        assertEquals("No such file or directory", strerror);
        assertEquals(2, errno);
    }

    @Test(dependsOnMethods = "testRmdir_NonExisting")
    public void testFini() {
        int fini = glfs_fini(vol);
        System.out.println("FINI: " + fini);
        assertEquals(0, fini);
    }

}
