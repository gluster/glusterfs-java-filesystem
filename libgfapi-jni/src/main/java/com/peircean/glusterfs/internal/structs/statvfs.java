package com.peircean.glusterfs.internal.structs;

import lombok.ToString;
import org.fusesource.hawtjni.runtime.ClassFlag;
import org.fusesource.hawtjni.runtime.JniClass;
import org.fusesource.hawtjni.runtime.JniField;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@ToString
@JniClass(flags = ClassFlag.STRUCT)
public class statvfs {

    public long f_bsize;
    public long f_frsize;
    @JniField(cast = "__fsblkcnt_t")
    public long f_blocks;
    @JniField(cast = "__fsblkcnt_t")
    public long f_bfree;
    @JniField(cast = "__fsblkcnt_t")
    public long f_bavail;
    @JniField(cast = "__fsfilcnt_t")
    public long f_files;
    @JniField(cast = "__fsfilcnt_t")
    public long f_ffree;
    @JniField(cast = "__fsfilcnt_t")
    public long f_favail;
    public long f_fsid;
//    public int __f_unused;     // ?
    public long f_flag;
    public long f_namemax;
    public int[] __f_spare = new int[6];

/*
    unsigned long int f_bsize;
    unsigned long int f_frsize;
    __fsblkcnt_t f_blocks;
    __fsblkcnt_t f_bfree;
    __fsblkcnt_t f_bavail;
    __fsfilcnt_t f_files;
    __fsfilcnt_t f_ffree;
    __fsfilcnt_t f_favail;
    unsigned long int f_fsid;

    int __f_unused;     // ?

    unsigned long int f_flag;
    unsigned long int f_namemax;
    int __f_spare[6];

 */
}
