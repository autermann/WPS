package org.n52.wps.server.grass.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class ConfigFileBuilder {

    private static final String LINE_SEP = System.getProperty("line.separator");
    private final List<BlockBuilder> blocks = new LinkedList<>();

    public BlockBuilder addBlock(String name) {
        BlockBuilder b = new BlockBuilder(name);
        blocks.add(b);
        return b;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        try {
            appendTo(builder);
        } catch (IOException ex) {
            /* won't happen */
        }
        return builder.toString();
    }

    public Appendable appendTo(Appendable appendable)
            throws IOException {
        Iterator<BlockBuilder> iter = blocks.iterator();
        if (iter.hasNext()) {
            iter.next().appendTo(appendable);
            while(iter.hasNext()) {
                appendable.append(ConfigFileBuilder.LINE_SEP);
                iter.next().appendTo(appendable);
            }
        }
        return appendable;
    }

    public static class BlockBuilder {
        private final String name;
        private final List<Line> lines = new LinkedList<>();

        private BlockBuilder(String name) {
            this.name = name;
        }

        public BlockBuilder add(String key, String... value) {
            lines.add(new Line(key, value));
            return this;
        }

        public Appendable appendTo(Appendable appendable)
                throws IOException {
            appendable.append('[').append(name).append(']');
            appendable.append(LINE_SEP);
            for (Line line : lines) {
                appendable.append(line.key).append('=');
                for (String valuePart : line.value) {
                    appendable.append(valuePart);
                }
                appendable.append(LINE_SEP);
            }
            return appendable;
        }

        private class Line {
            private final String key;
            private final String[] value;

            Line(String key, String[] value) {
                this.key = key;
                this.value = value;
            }
        }
    }
}
