package com.peircean.glusterfs;

import junit.framework.TestCase;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class GlusterFileSystemProviderTest extends TestCase {
    
    @Test
    public void testGetScheme() {
        GlusterFileSystemProvider p = new GlusterFileSystemProvider();
        assertEquals("gluster", p.getScheme());
    }
}
