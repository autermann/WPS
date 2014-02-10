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
public class PathBuilder {

    private static final Joiner JOINER = Joiner.on(":");
    private final List<String> segments = new LinkedList<>();

    public PathBuilder add(String path) {
        this.segments.add(Preconditions.checkNotNull(path));
        return this;
    }

    public PathBuilder add(FileBuilder b) {
        return add(b.build());
    }

    public PathBuilder add(File f) {
        return add(f.getAbsolutePath());
    }

    public String build() {
        return JOINER.join(segments);
    }

    @Override
    public String toString() {
        return build();
    }

    public static PathBuilder create(File base) {
        return new PathBuilder().add(base);
    }

    public static PathBuilder create(String base) {
        return new PathBuilder().add(base);
    }

    public static PathBuilder create() {
        return new PathBuilder();
    }

}
