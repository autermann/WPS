package org.n52.wps.server.request;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.n52.wps.commons.Format;
import org.n52.wps.commons.FormatPermutation;
import org.n52.wps.server.ExceptionReport;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Christian Autermann
 */
public class FormatHandlerTest {
    private final String mimeType = "mimeType";
    private final String encoding = "encoding";
    private final String schema = "schema";
    private final String altMimeType = mimeType + "2";
    private final String altSchema = schema + "2";
    private final String altEncoding = encoding + "2";

    private final Format defaultFormat = new Format("default" + mimeType,
                                                    "default" + encoding,
                                                    "default" + schema);

    private final Format expected                   = new Format(mimeType, encoding, schema);
    private final Format withoutMimeType            = new Format(null,     encoding, schema);
    private final Format withoutEncoding            = new Format(mimeType, null,     schema);
    private final Format withoutSchema              = new Format(mimeType, encoding, null  );
    private final Format withoutEncodingAndSchema   = new Format(mimeType, null,     null  );
    private final Format withoutMimeTypeAndEncoding = new Format(null,     null,     schema);
    private final Format withoutMimeTypeAndSchema   = new Format(null,     encoding, null  );
    private final Format empty                      = new Format(null,     null,     null  );

    @Rule
    public final ErrorCollector errors = new ErrorCollector();

    @Test
    public void testSelectWithAltMimeTypeAndSchema() throws ExceptionReport {
        Set<Format> supported = Sets.newHashSet(
                defaultFormat,
                expected,
                expected.withMimeType(altMimeType).withSchema(altSchema)
        );
        FormatHandler handler = new FormatHandler(defaultFormat, supported);
        errors.checkThat(handler.select(expected), is(expected));
        errors.checkThat(handler.select(withoutMimeType), is(expected));
        errors.checkThat(handler.select(withoutEncoding), is(expected));
        errors.checkThat(handler.select(withoutSchema), is(expected));
        errors.checkThat(handler.select(withoutEncodingAndSchema), is(expected));
        errors.checkThat(handler.select(withoutMimeTypeAndEncoding), is(expected));
        errors.checkThat(handler.select(withoutMimeTypeAndSchema), is(nullValue()));
        errors.checkThat(handler.select(empty), is(defaultFormat));

    }

    @Test
    public void testSelectWithAltEncodingAndSchema() {
        Set<Format> supported = Sets.newHashSet(
                defaultFormat,
                expected,
                expected.withEncoding(altEncoding).withSchema(altSchema)
        );
        FormatHandler handler = new FormatHandler(defaultFormat, supported);
        errors.checkThat(handler.select(expected), is(expected));
        errors.checkThat(handler.select(withoutMimeType), is(expected));
        errors.checkThat(handler.select(withoutEncoding), is(expected));
        errors.checkThat(handler.select(withoutSchema), is(expected));
        errors.checkThat(handler.select(withoutEncodingAndSchema), is(nullValue()));
        errors.checkThat(handler.select(withoutMimeTypeAndEncoding), is(expected));
        errors.checkThat(handler.select(withoutMimeTypeAndSchema), is(expected));
        errors.checkThat(handler.select(empty), is(defaultFormat));
    }

    @Test
    public void testSelectWithAltMimeTypeAndEncoding() {
        Set<Format> supported = Sets.newHashSet(
                defaultFormat,
                expected,
                expected.withMimeType(altMimeType).withEncoding(altEncoding)
        );
        FormatHandler handler = new FormatHandler(defaultFormat, supported);
        errors.checkThat(handler.select(expected), is(expected));
        errors.checkThat(handler.select(withoutMimeType), is(expected));
        errors.checkThat(handler.select(withoutEncoding), is(expected));
        errors.checkThat(handler.select(withoutSchema), is(expected));
        errors.checkThat(handler.select(withoutEncodingAndSchema), is(expected));
        errors.checkThat(handler.select(withoutMimeTypeAndEncoding), is(nullValue()));
        errors.checkThat(handler.select(withoutMimeTypeAndSchema), is(expected));
        errors.checkThat(handler.select(empty), is(defaultFormat));
    }

    @Test
    public void testSelectWithAltSchema() {
        Set<Format> supported = Sets.newHashSet(
                defaultFormat,
                expected,
                expected.withSchema(altSchema)
        );
        FormatHandler handler = new FormatHandler(defaultFormat, supported);
        errors.checkThat(handler.select(expected), is(expected));
        errors.checkThat(handler.select(withoutMimeType), is(expected));
        errors.checkThat(handler.select(withoutEncoding), is(expected));
        errors.checkThat(handler.select(withoutSchema), is(nullValue()));
        errors.checkThat(handler.select(withoutEncodingAndSchema), is(nullValue()));
        errors.checkThat(handler.select(withoutMimeTypeAndEncoding), is(expected));
        errors.checkThat(handler.select(withoutMimeTypeAndSchema), is(nullValue()));
        errors.checkThat(handler.select(empty), is(defaultFormat));
    }

    @Test
    public void testSelectWithAltEncoding() {
        Set<Format> supported = Sets.newHashSet(
                defaultFormat,
                expected,
                expected.withEncoding(altEncoding)
        );

        FormatHandler handler = new FormatHandler(defaultFormat, supported);
        errors.checkThat(handler.select(expected), is(expected));
        errors.checkThat(handler.select(withoutMimeType), is(expected));
        errors.checkThat(handler.select(withoutEncoding), is(nullValue()));
        errors.checkThat(handler.select(withoutSchema), is(expected));
        errors.checkThat(handler.select(withoutEncodingAndSchema), is(nullValue()));
        errors.checkThat(handler.select(withoutMimeTypeAndEncoding), is(nullValue()));
        errors.checkThat(handler.select(withoutMimeTypeAndSchema), is(expected));
        errors.checkThat(handler.select(empty), is(defaultFormat));
    }

    @Test
    public void testSelectWithAltMimeType() {
        Set<Format> supported = Sets.newHashSet(
                defaultFormat,
                expected,
                expected.withMimeType(altMimeType)
        );

        FormatHandler handler = new FormatHandler(defaultFormat, supported);
        errors.checkThat(handler.select(expected), is(expected));
        errors.checkThat(handler.select(withoutMimeType), is(nullValue()));
        errors.checkThat(handler.select(withoutEncoding), is(expected));
        errors.checkThat(handler.select(withoutSchema), is(expected));
        errors.checkThat(handler.select(withoutEncodingAndSchema), is(expected));
        errors.checkThat(handler.select(withoutMimeTypeAndEncoding), is(nullValue()));
        errors.checkThat(handler.select(withoutMimeTypeAndSchema), is(nullValue()));
        errors.checkThat(handler.select(empty), is(defaultFormat));
    }

    @Test
    public void testSelectWithExpected() {
        Set<Format> supported = Sets.newHashSet(
                defaultFormat,
                expected
        );

        FormatHandler handler = new FormatHandler(defaultFormat, supported);
        errors.checkThat(handler.select(expected), is(expected));
        errors.checkThat(handler.select(withoutMimeType), is(expected));
        errors.checkThat(handler.select(withoutEncoding), is(expected));
        errors.checkThat(handler.select(withoutSchema), is(expected));
        errors.checkThat(handler.select(withoutEncodingAndSchema), is(expected));
        errors.checkThat(handler.select(withoutMimeTypeAndEncoding), is(expected));
        errors.checkThat(handler.select(withoutMimeTypeAndSchema), is(expected));
        errors.checkThat(handler.select(empty), is(defaultFormat));
    }

    @Test
    public void testSelectOnlyMimeType() {
        Format wildcard = new Format(mimeType);
        Set<Format> supported = Sets.newHashSet(
                defaultFormat,
                expected,
                wildcard
        );

        FormatHandler handler = new FormatHandler(defaultFormat, supported);
        errors.checkThat(handler.select(expected), is(expected));
        errors.checkThat(handler.select(withoutMimeType), is(expected));
        errors.checkThat(handler.select(withoutEncoding), is(expected));
        errors.checkThat(handler.select(withoutSchema), is(expected));
        errors.checkThat(handler.select(withoutEncodingAndSchema), is(nullValue()));
        errors.checkThat(handler.select(withoutMimeTypeAndEncoding), is(expected));
        errors.checkThat(handler.select(withoutMimeTypeAndSchema), is(expected));
        errors.checkThat(handler.select(empty), is(defaultFormat));
    }

    @Test
    public void testSelectWithPermutation() {
        Set<Format> supported = new FormatPermutation(
                Lists.newArrayList(mimeType, altMimeType),
                Lists.newArrayList(encoding, altEncoding),
                Lists.newArrayList(schema, altSchema)
        );

        FormatHandler handler = new FormatHandler(defaultFormat, supported);
        errors.checkThat(handler.select(expected), is(expected));
        errors.checkThat(handler.select(withoutMimeType), is(nullValue()));
        errors.checkThat(handler.select(withoutEncoding), is(nullValue()));
        errors.checkThat(handler.select(withoutSchema), is(nullValue()));
        errors.checkThat(handler.select(withoutEncodingAndSchema), is(nullValue()));
        errors.checkThat(handler.select(withoutMimeTypeAndEncoding), is(nullValue()));
        errors.checkThat(handler.select(withoutMimeTypeAndSchema), is(nullValue()));
        errors.checkThat(handler.select(empty), is(defaultFormat));
    }
}
