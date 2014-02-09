package org.n52.wps.server.request;

import java.util.List;
import net.opengis.ows.x11.BoundingBoxType;

import org.n52.wps.io.data.IBBOXData;
import org.n52.wps.io.data.binding.bbox.GTReferenceEnvelope;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.InvalidParameterValueException;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class GeotoolsBoundingBoxParser implements BoundingBoxParser {

    @Override
    public IBBOXData parse(BoundingBoxType input) throws ExceptionReport {
        String crs = input.getCrs();
        List<?> lowerCorner = input.getLowerCorner();
        List<?> upperCorner = input.getUpperCorner();

        if (lowerCorner.size() != 2 || upperCorner.size() != 2) {
            throw new InvalidParameterValueException("Error while parsing the BBOX data");
        }
        return new GTReferenceEnvelope(
                lowerCorner.get(0), lowerCorner.get(1),
                upperCorner.get(0), upperCorner.get(1), crs);

    }

}
