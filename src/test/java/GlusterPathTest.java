import com.peircean.glusterfs.GlusterPath;
import junit.framework.TestCase;
import org.junit.Test;

import java.nio.file.Path;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class GlusterPathTest extends TestCase {
    @Test
    public void testIsAbsolute() {
        Path p = new GlusterPath();
        assertFalse(p.isAbsolute());
    }
}
