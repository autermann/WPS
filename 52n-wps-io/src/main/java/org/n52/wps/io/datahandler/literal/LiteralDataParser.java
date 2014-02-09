package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.server.ExceptionReport;

/**
 * @author Christian Autermann
 */
public interface LiteralDataParser {

    Class<? extends ILiteralData> getBindingType();

    Class<?> getPayloadType();

    String getURI();

    ILiteralData parse(String value) throws ExceptionReport;

}
