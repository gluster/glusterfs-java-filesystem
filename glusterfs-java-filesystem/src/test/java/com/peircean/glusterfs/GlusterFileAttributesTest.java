package com.peircean.glusterfs;

import junit.framework.TestCase;
import com.peircean.libgfapi_jni.internal.structs.stat;
import org.junit.Test;

import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class GlusterFileAttributesTest extends TestCase {

    public static final int MODE = 0100777;
    public static final int UID = 1000;
    public static final int GID = 1001;
    public static final long SIZE = 1234l;
    public static final long ATIME = 1373464796l;
    public static final long CTIME = 1373464797l;
    public static final long MTIME = 1373464798l;
    public static final long INODE = 4452352l;
    private GlusterFileAttributes attrib = new GlusterFileAttributes(MODE, UID, GID, SIZE, ATIME, CTIME, MTIME, INODE);

    @Test
    public void testOwner() {
        assertEquals(String.valueOf(UID), attrib.owner().getName());
    }

    @Test
    public void testGroup() {
        assertEquals(String.valueOf(GID), attrib.group().getName());
    }

    @Test
    public void testModified() {
        assertEquals(FileTime.fromMillis(MTIME * 1000), attrib.lastModifiedTime());
    }

    @Test
    public void testCreated() {
        assertEquals(FileTime.fromMillis(CTIME * 1000), attrib.creationTime());
    }

    @Test
    public void testAccessed() {
        assertEquals(FileTime.fromMillis(ATIME * 1000), attrib.lastAccessTime());
    }

    @Test
    public void testPermissions() {
        Set<PosixFilePermission> expected = PosixFilePermissions.fromString("rwxrwxrwx");
        Set<PosixFilePermission> permissions = attrib.permissions();
        assertEquals(expected, permissions);
    }

    @Test
    public void testParseAttributes() {
        Set<PosixFilePermission> posixFilePermissions = PosixFilePermissions.fromString("rwxrwxrwx");
        FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(posixFilePermissions);
        int mode = GlusterFileAttributes.parseAttrs(attrs);
        assertEquals(0777, mode);
    }


    @Test
    public void testIsRegular() {
        assertTrue(attrib.isRegularFile());
    }

    @Test
    public void testIsSymbolic() {
        attrib = new GlusterFileAttributes(0120777, UID, GID, SIZE, ATIME, CTIME, MTIME, INODE);
        assertTrue(attrib.isSymbolicLink());
    }

    @Test
    public void testIsDirectory() {
        attrib = new GlusterFileAttributes(0040777, UID, GID, SIZE, ATIME, CTIME, MTIME, INODE);
        assertTrue(attrib.isDirectory());
    }

    @Test
    public void testIsOther() {
        attrib = new GlusterFileAttributes(0000777, UID, GID, SIZE, ATIME, CTIME, MTIME, INODE);
        assertTrue(attrib.isOther());
    }

    @Test
    public void testSize() {
        assertEquals(SIZE, attrib.size());
    }

    @Test
    public void testFileKey() {
        assertEquals(INODE, attrib.fileKey());
    }

    @Test
    public void testFromStat() {
        stat stat = new stat();
        stat.st_size = SIZE;
        stat.st_gid = GID;
        stat.st_uid = UID;
        stat.st_mode = MODE;
        stat.st_ino = INODE;
        stat.atime = ATIME;
        stat.mtime = MTIME;
        stat.ctime = CTIME;

        GlusterFileAttributes attr = GlusterFileAttributes.fromStat(stat);

        assertEquals(stat.st_size, attr.getSize());
        assertEquals(stat.st_uid, attr.getUid());
        assertEquals(stat.st_gid, attr.getGid());
        assertEquals(stat.st_mode, attr.getMode());
        assertEquals(stat.st_ino, attr.getInode());
        assertEquals(stat.atime, attr.getAtime());
        assertEquals(stat.mtime, attr.getMtime());
        assertEquals(stat.ctime, attr.getCtime());

    }
}
