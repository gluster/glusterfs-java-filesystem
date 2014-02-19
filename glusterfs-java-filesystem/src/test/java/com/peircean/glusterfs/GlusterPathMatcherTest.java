package com.peircean.glusterfs;

import com.peircean.glusterfs.borrowed.GlobPattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Pattern.class, Matcher.class, GlusterPathMatcher.class, GlobPattern.class})
public class GlusterPathMatcherTest {
    @Mock
    private GlusterPath mockPath;

    @Test
    public void testPathMatcherMatches_whenTrue() throws URISyntaxException {
        matchesHelper(true);
    }

    @Test
    public void testPathMatcherMatches_whenFalse() throws URISyntaxException {
        matchesHelper(false);
    }

    void matchesHelper(boolean result) throws URISyntaxException {
        Pattern pattern = PowerMockito.mock(Pattern.class);
        Matcher matcher = PowerMockito.mock(Matcher.class);
        String foo = "/foo";

        doReturn(foo).when(mockPath).toString();

        when(pattern.matcher(foo)).thenReturn(matcher);
        when(matcher.matches()).thenReturn(result);

        GlusterPathMatcher pathMatcher = new GlusterPathMatcher(pattern);

        boolean matches = pathMatcher.matches(mockPath);

        assertEquals(result, matches);

//        verify doesn't work with toString or PowerMock objects
//        verify(mockPath).toString();

//        verify(pattern).matcher(foo);
//        verify(matcher).matches();

    }
}
