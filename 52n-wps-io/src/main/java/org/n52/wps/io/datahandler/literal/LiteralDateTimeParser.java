package org.n52.wps.io.datahandler.literal;

import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.LiteralDateTimeBinding;
import org.n52.wps.server.InvalidParameterValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Autermann
 */
public class LiteralDateTimeParser extends AbstractLiteralDataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiteralDateTimeParser.class);
    private static final DatatypeFactory DATATYPE_FACTORY;
    static {
        DatatypeFactory datatypeFactory = null;
        try {
            // Is this thread safe?
            // bah, a checked exception on factory instantiation?
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ex) {
            LOGGER.error("Error creating DatatypeFactory", ex);
        }
        DATATYPE_FACTORY = datatypeFactory;
    }

    public LiteralDateTimeParser(String uri) {
        super(Date.class, LiteralDateTimeBinding.class, uri);
    }

    @Override
    public ILiteralData parse(String value) throws
                                                   InvalidParameterValueException {
        try {
            return new LiteralDateTimeBinding(DATATYPE_FACTORY.newXMLGregorianCalendar(value)
                    .toGregorianCalendar().getTime());
        } catch (Exception e) {
            LOGGER.error("Could not parse xs:dateTime or xs:date data", e);
            return null;
        }
    }

}
