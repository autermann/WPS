package org.n52.wps.commons;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.xmlbeans.XmlException;
import org.junit.rules.ExternalResource;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class WPSConfigRule extends ExternalResource {
    private Class<?> c;
    private String path;

    public WPSConfigRule(String path) {
        this(WPSConfigRule.class, path);
    }

    public WPSConfigRule(Class<?> clazz, String path) {
        this.c = clazz;
        this.path = path;
    }

    @Override
    protected void before()
            throws IOException, XmlException {
        try (InputStream in = c.getResourceAsStream(path);
             BufferedInputStream bin = new BufferedInputStream(in)) {
            WPSConfig.forceInitialization(bin);
        }
    }

    public boolean isDataHandlerActive(Class<?> handler) {
        if (handler == null) {
            return false;
        }
        String className = handler.getClass().getName();
        return WPSConfig.getInstance().isGeneratorActive(className) ||
               WPSConfig.getInstance().isParserActive(className);
    }
}
