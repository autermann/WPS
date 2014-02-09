package org.n52.wps.server.request;

import net.opengis.ows.x11.BoundingBoxType;

import org.n52.wps.io.data.IBBOXData;
import org.n52.wps.server.ExceptionReport;

/**
 * @author Christian Autermann
 */
public interface BoundingBoxParser {

    IBBOXData parse(BoundingBoxType xbBbox) throws ExceptionReport;
}
