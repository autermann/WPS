package org.n52.wps.server.response.execute;

import java.math.BigInteger;

import net.opengis.ows.x11.BoundingBoxType;
import net.opengis.wps.x100.OutputDataType;

import org.n52.wps.commons.OwsCodeType;
import org.n52.wps.commons.OwsLanguageString;
import org.n52.wps.io.data.IBBOXData;

import com.google.common.primitives.Doubles;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class BoundingBoxProcessOutput extends ProcessOutput {

    public BoundingBoxProcessOutput(OwsCodeType identifier,
                                    OwsLanguageString title,
                                    OwsLanguageString abstrakt,
                                    IBBOXData payload) {
        super(identifier, title, abstrakt, payload);
    }

    @Override
    protected void encodeData(OutputDataType xbOutput) {
        IBBOXData bbox = (IBBOXData) getPayload();
		BoundingBoxType xbBbox = xbOutput.addNewData().addNewBoundingBoxData();
		xbBbox.setLowerCorner(Doubles.asList(bbox.getLowerCorner()));
		xbBbox.setUpperCorner(Doubles.asList(bbox.getUpperCorner()));
		xbBbox.setDimensions(BigInteger.valueOf(bbox.getDimension()));
        if (bbox.getCRS() != null && !bbox.getCRS().isEmpty()){
			xbBbox.setCrs(bbox.getCRS());
		}
    }

}
