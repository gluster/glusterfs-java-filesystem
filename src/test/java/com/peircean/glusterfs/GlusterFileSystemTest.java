package com.peircean.glusterfs;

import static org.junit.Assert.*;
import org.junit.Test;

import java.nio.file.FileSystem;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class GlusterFileSystemTest {
    @Test
    public void testGetSeparator() {
        FileSystem g = new GlusterFileSystem();
        assertEquals("/", g.getSeparator());
    }
}
