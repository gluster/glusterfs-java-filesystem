package com.peircean.glusterfs.internal;

import org.testng.annotations.Test;

import static org.junit.Assert.assertEquals;

public class GlusterOpenOptionTest {
    private int rd = 00000000;
    private int wr = 00000001;
    private int rdwr = 00000002;
    private int creat = 00000100;
    private int excl = 00000200;
    private int trunc = 00001000;
    private int append = 00002000;
    private int dsync = 00010000;
    private int sync = 04000000;
    private int all = append | creat | excl | trunc | dsync | sync;

    @Test
    public void testRead() {
        GlusterOpenOption openOption = GlusterOpenOption.READ();
        openOption.append().create().createNew().dsync().sync().truncate();
        assertEquals(rd | all, openOption.getValue());
    }

    @Test
    public void testWrite() {
        GlusterOpenOption openOption = GlusterOpenOption.WRITE();
        openOption.append().create().createNew().dsync().sync().truncate();
        assertEquals(wr | all, openOption.getValue());
    }

    @Test
    public void testReadWrite() {
        GlusterOpenOption openOption = GlusterOpenOption.READWRITE();
        openOption.append().create().createNew().dsync().sync().truncate();
        assertEquals(rdwr | all, openOption.getValue());
    }
}
