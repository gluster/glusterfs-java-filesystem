package com.peircean.glusterfs.internal;

import org.fusesource.hawtjni.runtime.ArgFlag;
import org.fusesource.hawtjni.runtime.JniArg;
import org.fusesource.hawtjni.runtime.JniClass;
import org.fusesource.hawtjni.runtime.JniMethod;

import static org.fusesource.hawtjni.runtime.MethodFlag.CONSTANT_GETTER;

@JniClass
public class UtilJNI {

    static {
        GLFS.LIBRARY.load();
    }

    @JniMethod(flags = {CONSTANT_GETTER})
    public static final native int errno();

    @JniMethod(cast = "char *", accessor = "strerror")
    public static final native long strerror_jni(int errnum);

    public static final native int strlen(
            @JniArg(cast = "const char *") long s);

    @JniMethod
    public static final native void memmove(
            @JniArg(cast = "void *", flags = {ArgFlag.NO_IN, ArgFlag.CRITICAL}) byte[] dest,
            @JniArg(cast = "const void *", flags = {ArgFlag.NO_OUT, ArgFlag.CRITICAL}) long src,
            @JniArg(cast = "size_t") long size);

    public static String strerror() {
        return string(strerror_jni(errno()));
    }

    public static String string(long ptr) {
        if (ptr == 0) {
            return null;
        }

        int len = strlen(ptr);
        byte[] chars = new byte[len];
        memmove(chars, ptr, len);
        return new String(chars);
    }

}
