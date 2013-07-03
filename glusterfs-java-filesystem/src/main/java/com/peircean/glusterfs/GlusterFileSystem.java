package com.peircean.glusterfs;

import lombok.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
            if (-1 != fini) {
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
        return list;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public String toString() {
        return provider.getScheme() + "://" + host + ":" + volname;
    }
}
