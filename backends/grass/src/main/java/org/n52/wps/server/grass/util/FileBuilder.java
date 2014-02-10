package org.n52.wps.server.grass.util;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class FileBuilder {

    private static final Joiner JOINER = Joiner.on(File.separator);
    private final List<String> segments = new LinkedList<>();

    private FileBuilder(String base) {
        this.segments.add(Preconditions.checkNotNull(base));
    }

    public FileBuilder add(String path) {
        this.segments.add(Preconditions.checkNotNull(path));
        return this;
    }

    public FileBuilder dir(String path) {
        return add(path);
    }

    public FileBuilder file(String name, String extension) {
        return add(Preconditions.checkNotNull(name) + " " +
                   Preconditions.checkNotNull(extension));
    }

    public String build() {
        return JOINER.join(segments);
    }

    public File buildFile() {
        return new File(build());
    }

    @Override
    public String toString() {
        return build();
    }

    public static FileBuilder create(File base) {
        return new FileBuilder(base.getAbsolutePath());
    }

    public static FileBuilder create(String base) {
        return new FileBuilder(base);
    }
}
