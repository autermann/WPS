package org.n52.wps.server.request;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;

import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.OutputDescriptionType;

import org.n52.wps.commons.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class FormatHandler {
    private static final Logger LOG = LoggerFactory
            .getLogger(FormatHandler.class);
    private final Format defaultFormat;
    private final Iterable<Format> supportedFormats;

    public FormatHandler(InputDescriptionType idt) {
        this(Format.getDefault(idt), Format.getSupported(idt));
    }


    public FormatHandler(OutputDescriptionType odt) {
        this(Format.getDefault(odt), Format.getSupported(odt));
    }

    public FormatHandler(Format defaultFormat,
                         Iterable<Format> supportedFormats) {
        this.defaultFormat = checkNotNull(defaultFormat);
        this.supportedFormats = checkNotNull(supportedFormats);
    }

    @SuppressWarnings("unchecked")
    public Format select(final Format dataFormat) {
        checkNotNull(dataFormat);
        if (dataFormat.hasMimeType()) {
            if (dataFormat.hasEncoding()) {
                if (dataFormat.hasSchema()) {
                    return find(Predicates.and(dataFormat.matchingMimeType(),
                                               dataFormat.matchingEncoding(),
                                               dataFormat.matchingSchema()));
                } else {
                    return find(Predicates.and(dataFormat.matchingMimeType(),
                                               dataFormat.matchingEncoding()));
                }
            } else {
                if (dataFormat.hasSchema()) {
                    return find(Predicates.and(dataFormat.matchingMimeType(),
                                               dataFormat.matchingSchema()));
                } else {
                    return find(dataFormat.matchingMimeType());
                }
            }
        } else {
            if (dataFormat.hasEncoding()) {
                if (dataFormat.hasSchema()) {
                    return find(Predicates.and(dataFormat.matchingEncoding(),
                                               dataFormat.matchingSchema()));
                } else {
                    return find(dataFormat.matchingEncoding());
                }
            } else {
                if (dataFormat.hasSchema()) {
                    return find(dataFormat.matchingSchema());
                } else {
                    return defaultFormat;
                }
            }
        }
    }

    private Format find(Predicate<Format> p) {
        Set<Format> found = new HashSet<>(1);
        if (p.apply(defaultFormat)) {
            found.add(defaultFormat);
        }
        for (Format format : supportedFormats) {
            if (p.apply(format)) {
                found.add(format);
            }
        }
        if (found.size() == 1) {
            return found.iterator().next();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("default", defaultFormat)
                .add("supported", supportedFormats)
                .toString();
    }

}
