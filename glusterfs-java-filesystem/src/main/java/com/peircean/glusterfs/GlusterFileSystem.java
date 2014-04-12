package com.peircean.glusterfs;

import com.peircean.glusterfs.borrowed.GlobPattern;
import lombok.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@EqualsAndHashCode(exclude = {"provider", "volptr"}, callSuper = false)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class GlusterFileSystem extends FileSystem {
    private static final String SEPARATOR = "/";
    @NonNull
    private final GlusterFileSystemProvider provider;
    @NonNull
    private final String host;
    @NonNull
    private final String volname;
    @NonNull
    private long volptr;

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            int fini = provider.close(volptr);
            if (0 != fini) {
                throw new IOException("Unable to close filesystem: " + volname);
            }
            volptr = -1;
        }
    }

    @Override
    public boolean isOpen() {
        return volptr > 0;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        GlusterPath root = new GlusterPath(this, "/");
        List<Path> list = new ArrayList<Path>(1);
        list.add(root);
        return Collections.unmodifiableList(list);
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        GlusterFileStore store = new GlusterFileStore(this);
        List<FileStore> stores = new ArrayList<FileStore>(1);
        stores.add(store);
        return Collections.unmodifiableList(stores);
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Path getPath(String s, String... strings) {
        boolean absolute = s.startsWith("/");
        if (absolute) {
            s = s.substring(1);
        }
        String[] parts;
        if (null != strings && strings.length > 0) {
            parts = new String[1 + strings.length];
            parts[0] = s;
            System.arraycopy(strings, 0, parts, 1, strings.length);
        } else {
            parts = new String[]{s};
        }
        return new GlusterPath(this, parts, absolute);
    }

    @Override
    public PathMatcher getPathMatcher(String s) {
        if (!s.contains(":")) {
            throw new IllegalArgumentException("PathMatcher requires input syntax:expression");
        }
        String[] parts = s.split(":", 2);
        Pattern pattern;
        if ("glob".equals(parts[0])) {
            pattern = GlobPattern.compile(parts[1]);
        } else if ("regex".equals(parts[0])) {
            pattern = Pattern.compile(parts[1]);
        } else {
            throw new UnsupportedOperationException("Unknown PathMatcher syntax: " + parts[0]);
        }

        return new GlusterPathMatcher(pattern);
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        return new GlusterWatchService();
    }

    public String toString() {
        return provider.getScheme() + "://" + host + ":" + volname;
    }
}

