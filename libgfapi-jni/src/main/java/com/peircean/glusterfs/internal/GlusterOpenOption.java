package com.peircean.glusterfs.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.fusesource.hawtjni.runtime.*;

import java.util.LinkedList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(of = "options", includeFieldNames = false)
@JniClass
public class GlusterOpenOption {
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int O_APPEND;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int O_SYNC;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int O_DSYNC;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int O_TRUNC;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int O_RDONLY;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int O_WRONLY;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int O_RDWR;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int O_CREAT;
    @JniField(flags = {FieldFlag.CONSTANT})
    public static int O_EXCL;
    @Getter
    private int value;
    private List<String> options = new LinkedList<String>();

    static {
        GLFS.LIBRARY.load();
        init();
    }
    
    @JniMethod(flags = {MethodFlag.CONSTANT_INITIALIZER})
    private static final native void init();

    public static GlusterOpenOption READ() {
        GlusterOpenOption o = new GlusterOpenOption();
        o.value = O_RDONLY;
        o.options.add("read");
        return o;
    }

    public static GlusterOpenOption WRITE() {
        GlusterOpenOption o = new GlusterOpenOption();
        o.value = O_WRONLY;
        o.options.add("write");
        return o;
    }

    public static GlusterOpenOption READWRITE() {
        GlusterOpenOption o = new GlusterOpenOption();
        o.value = O_RDWR;
        o.options.add("readwrite");
        return o;
    }

    public GlusterOpenOption create() {
        value |= O_CREAT;
        options.add("create");
        return this;
    }

    public GlusterOpenOption createNew() {
        value |= O_CREAT | O_EXCL;
        options.add("create_new");
        return this;
    }

    public GlusterOpenOption truncate() {
        value |= O_TRUNC;
        options.add("truncate");
        return this;
    }

    public GlusterOpenOption append() {
        value |= O_APPEND;
        options.add("append");
        return this;
    }

    public GlusterOpenOption dsync() {
        value |= O_DSYNC;
        options.add("dsync");
        return this;
    }

    public GlusterOpenOption sync() {
        value |= O_SYNC;
        options.add("sync");
        return this;
    }
}
