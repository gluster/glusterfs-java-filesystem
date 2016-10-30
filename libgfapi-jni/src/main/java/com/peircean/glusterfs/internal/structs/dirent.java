package com.peircean.glusterfs.internal.structs;


import com.peircean.glusterfs.internal.GLFS;
import lombok.EqualsAndHashCode;
import org.fusesource.hawtjni.runtime.*;

@EqualsAndHashCode
@JniClass(flags = ClassFlag.STRUCT)
public class dirent {
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int DT_UNKNOWN;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int DT_FIFO;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int DT_CHR;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int DT_DIR;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int DT_BLK;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int DT_REG;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int DT_LNK;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int DT_SOCK;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int DT_WHT;
    @JniField(flags = {FieldFlag.CONSTANT}, accessor = "sizeof(struct dirent)")
    public static short SIZE_OF;
    //    __ino_t d_ino;
    @JniField(cast = "__ino_t")
    public long d_ino;
    //    __off_t d_off;
    @JniField(cast = "__off_t")
    public long d_off;
    //    unsigned short int d_reclen;
    public int d_reclen;
    //    unsigned char d_type;
    public byte d_type;
    //    char d_name[256];
    public byte[] d_name = new byte[256];

    public String getName() {
        return new String(d_name).split("\0")[0];
    }
    
    public static String typeOf(int type) {
        String text = "";
        if (DT_UNKNOWN == type) {
            text = "UNKNOWN";
        }
        if (DT_BLK == type) {
            text = "BLK";
        }
        if (DT_CHR == type) {
            text = "CHR";
        }
        if (DT_DIR == type) {
            text = "DIR";
        }
        if (DT_FIFO == type) {
            text = "FIFO";
        }
        if (DT_LNK == type) {
            text = "LNK";
        }
        if (DT_REG == type) {
            text = "REG";
        }
        if (DT_SOCK == type) {
            text = "SOCK";
        }
        if (DT_WHT == type) {
            text = "WHT";
        }
        return text + "(" + type + ")";
    }

    @JniMethod(flags = {MethodFlag.CONSTANT_INITIALIZER})
    private static final native void init();

    @JniMethod(cast = "void *")
    public static final native long malloc(
            @JniArg(cast = "size_t") long size);

    //void free(void *ptr);
    @JniMethod
    public static final native void free(
            @JniArg(cast = "void *") long ptr
    );

    @JniMethod
    public static final native void memmove(
            @JniArg(cast = "void *", flags = {ArgFlag.NO_IN, ArgFlag.CRITICAL}) dirent dest,
            @JniArg(cast = "const void *", flags = {ArgFlag.NO_OUT, ArgFlag.CRITICAL}) long src,
            @JniArg(cast = "size_t") long size);

    @JniMethod
    public static final native void memmove(
            @JniArg(cast = "void *", flags = {ArgFlag.NO_IN, ArgFlag.CRITICAL}) long dest,
            @JniArg(cast = "const void *", flags = {ArgFlag.NO_OUT, ArgFlag.CRITICAL}) dirent src,
            @JniArg(cast = "size_t") long size);

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TYPE: ").append(typeOf(d_type)).append("; ")
                .append("NAME: ").append(getName()).append("; ")
                .append("RECLEN: ").append(d_reclen).append("; ")
                .append("INO: ").append(d_ino).append("; ")
                .append("OFF:").append(d_off);
        return sb.toString();
    }

    static {
        GLFS.LIBRARY.load();
        init();
    }
}
