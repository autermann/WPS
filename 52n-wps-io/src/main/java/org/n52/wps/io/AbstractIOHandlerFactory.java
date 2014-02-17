package org.n52.wps.io;


import org.n52.wps.commons.Format;

import com.google.common.base.Predicate;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public abstract class AbstractIOHandlerFactory {

    protected static Predicate<IOHandler> supports(Class<?> dataBinding) {
        return new SupportsDataBinding(dataBinding);
    }

    protected static Predicate<IOHandler> supports(Format format) {
        return new SupportsFormat(format);
    }

    private static class SupportsDataBinding implements Predicate<IOHandler> {
        private final Class<?> dataBinding;
        SupportsDataBinding(Class<?> dataBinding) { this.dataBinding = dataBinding; }
        @Override public boolean apply(IOHandler input) {
            return input.isSupportedDataBinding(dataBinding);
        }
    }

    private static class SupportsFormat implements Predicate<IOHandler> {
        private final Format format;
        SupportsFormat(Format format) { this.format = format; }
        @Override public boolean apply(IOHandler input) {
            return input.isSupportedFormat(format);
        }
    }
}
