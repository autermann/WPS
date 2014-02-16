package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.server.ExceptionReport;

/**
 * Parser class for {@link ILiteralData}.
 *
 * @author Christian Autermann
 */
public interface LiteralDataParser {

    /**
     * Gets the {@link ILiteralData} binding class supported by this parser.
     *
     * @return the supported binding class
     */
    Class<? extends ILiteralData> getBindingType();

    /**
     * Gets the payload class supported by this parser.
     *
     * @return the payload type
     */
    Class<?> getPayloadType();

    /**
     * Gets the {@link URI} data type supported by this parser.
     *
     * @return the data type
     */
    String getURI();

    /**
     * Parses the supplied string value into a {@link ILiteralData}.
     *
     * @param value the value
     *
     * @return the {@code ILiteralData}
     *
     * @throws ExceptionReport if parsing of the supplied value fails
     */
    ILiteralData parse(String value)
            throws ExceptionReport;

}
