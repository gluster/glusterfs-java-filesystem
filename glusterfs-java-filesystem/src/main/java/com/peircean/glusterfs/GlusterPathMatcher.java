package com.peircean.glusterfs;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

class GlusterPathMatcher implements PathMatcher {
    Pattern pattern;

    public GlusterPathMatcher(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(Path path) {
        return pattern.matcher(path.toString()).matches();
    }
}
