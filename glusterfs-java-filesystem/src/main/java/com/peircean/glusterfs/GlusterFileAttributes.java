package com.peircean.glusterfs;

import lombok.Data;
import org.fusesource.glfsjni.internal.structs.stat;

import java.nio.file.attribute.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class GlusterFileAttributes implements PosixFileAttributes {
    private static Map<Integer, PosixFilePermission> perms = new HashMap<Integer, PosixFilePermission>();

    static {
        perms.put(0001, PosixFilePermission.OTHERS_EXECUTE);
        perms.put(0002, PosixFilePermission.OTHERS_WRITE);
        perms.put(0004, PosixFilePermission.OTHERS_READ);
        perms.put(0010, PosixFilePermission.GROUP_EXECUTE);
        perms.put(0020, PosixFilePermission.GROUP_WRITE);
        perms.put(0040, PosixFilePermission.GROUP_READ);
        perms.put(0100, PosixFilePermission.OWNER_EXECUTE);
        perms.put(0200, PosixFilePermission.OWNER_WRITE);
        perms.put(0400, PosixFilePermission.OWNER_READ);
    }

    private final int mode, uid, gid;
    private final long size, atime, ctime, mtime;

    public static GlusterFileAttributes fromStat(stat stat) {
        return new GlusterFileAttributes(stat.st_mode, stat.st_uid, stat.st_gid, stat.st_size, 0, 0, 0);
    }

    @Override
    public UserPrincipal owner() {
        return new UserPrincipal() {
            @Override
            public String getName() {
                return String.valueOf(uid);
            }
        };
    }

    @Override
    public GroupPrincipal group() {
        return new GroupPrincipal() {
            @Override
            public String getName() {
                return String.valueOf(gid);
            }
        };
    }

    @Override
    public Set<PosixFilePermission> permissions() {
        Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>();
        for (int mask : perms.keySet()) {
            if (mask == (mode & mask)) {
                permissions.add(perms.get(mask));
            }
        }
        return permissions;
    }

    @Override
    public FileTime lastModifiedTime() {
        return FileTime.fromMillis(mtime);
    }

    @Override
    public FileTime lastAccessTime() {
        return FileTime.fromMillis(atime);
    }

    @Override
    public FileTime creationTime() {
        return FileTime.fromMillis(ctime);
    }

    @Override
    public boolean isRegularFile() {
        int mask = 0100000;
        return mask == (mode & mask);
    }

    @Override
    public boolean isDirectory() {
        int mask = 0040000;
        return mask == (mode & mask);
    }

    @Override
    public boolean isSymbolicLink() {
        int mask = 0120000;
        return mask == (mode & mask);
    }

    @Override
    public boolean isOther() {
        return !(isDirectory() || isRegularFile() || isSymbolicLink());
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public Object fileKey() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
