package com.peircean.glusterfs.internal.structs;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.fusesource.hawtjni.runtime.ClassFlag;
import org.fusesource.hawtjni.runtime.JniClass;
import org.fusesource.hawtjni.runtime.JniField;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@ToString
@EqualsAndHashCode(exclude = "st_blksize")
@JniClass(flags = ClassFlag.STRUCT)
public class stat {
    
    public long st_dev;
    public long st_ino;
    public int st_mode;
    public int st_nlink;
    public int st_uid;
    public int st_gid;
    public long st_rdev;
    public long st_size;
    public int st_blksize;  //stat says 4096 as expected, lstat says 0, and fstat says 43, so this is excluded from equals() 
    public long st_blocks;
    
    //HawtJNI complains that the *nsec fields don't exist
    @JniField(accessor="st_atime")
    public long atime;
//    public long st_atime_nsec;
    @JniField(accessor="st_mtime")
    public long mtime;
//    public long st_mtime_nsec;
    @JniField(accessor="st_ctime")
    public long ctime;
//    public long st_ctime_nsec;

/*
    struct stat {
        unsigned long   st_dev;         /* Device.  
        unsigned long   st_ino;         /* File serial number.  
        unsigned int    st_mode;        /* File mode.  
        unsigned int    st_nlink;       /* Link count.  
        unsigned int    st_uid;         /* User ID of the file's owner.  
        unsigned int    st_gid;         /* Group ID of the file's group. 
        unsigned long   st_rdev;        /* Device number, if device.  
        unsigned long   __pad1;
        long            st_size;        /* Size of file, in bytes.  
        int             st_blksize;     /* Optimal block size for I/O.  
        int             __pad2;
        long            st_blocks;      /* Number 512-byte blocks allocated. 
        long            st_atime;       /* Time of last access.  
        unsigned long   st_atime_nsec;
        long            st_mtime;       /* Time of last modification.  
        unsigned long   st_mtime_nsec;
        long            st_ctime;       /* Time of last status change.  
        unsigned long   st_ctime_nsec;
        unsigned int    __unused4;
        unsigned int    __unused5;
    };
*/

}
