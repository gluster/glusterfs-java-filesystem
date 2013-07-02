import com.peircean.glusterfs.example.Example;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class ExampleTest extends TestCase {

    @Test
    public void testGetProvider() {
        Example.getProvider("gluster");
    }
}
