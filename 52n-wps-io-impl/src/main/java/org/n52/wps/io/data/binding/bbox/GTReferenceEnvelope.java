/**
 * ﻿Copyright (C) 2007
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.wps.io.data.binding.bbox;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.n52.wps.io.data.IBBOXData;
import org.opengis.geometry.Envelope;

import com.vividsolutions.jts.geom.Coordinate;

public class GTReferenceEnvelope implements IBBOXData {
    private static final long serialVersionUID = 1L;
    private final Envelope gtEnvelope;
    private final String crs;

    public GTReferenceEnvelope(Object llx,
                               Object lly,
                               Object upx,
                               Object upy,
                               String crs) {

        try {
            double llx_double = Double.parseDouble(llx.toString());
            double lly_double = Double.parseDouble(lly.toString());
            double upx_double = Double.parseDouble(upx.toString());
            double upy_double = Double.parseDouble(upy.toString());

            Coordinate ll = new Coordinate(llx_double, lly_double);
            Coordinate ur = new Coordinate(upx_double, upy_double);
            com.vividsolutions.jts.geom.Envelope jtsEnvelope = new com.vividsolutions.jts.geom.Envelope(ll, ur);
            this.crs = crs;
            if (crs == null) {
                this.gtEnvelope = new ReferencedEnvelope(jtsEnvelope, null);
            } else {
                this.gtEnvelope = new ReferencedEnvelope(jtsEnvelope, CRS.decode(crs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error while creating BoundingBox");
        }
    }

    public GTReferenceEnvelope(Envelope envelope) {
        this.gtEnvelope = envelope;
        if (envelope.getCoordinateReferenceSystem() != null &&
            !envelope.getCoordinateReferenceSystem().getIdentifiers().isEmpty()) {
            this.crs = envelope.getCoordinateReferenceSystem().getIdentifiers().iterator().next().toString();
        } else {
            this.crs = null;
        }
    }

    public Envelope getPayload() {
        return gtEnvelope;
    }

    public Class<?> getSupportedClass() {
        return ReferencedEnvelope.class;
    }

    @Override
    public String getCRS() {
        return this.crs;
    }

    @Override
    public int getDimension() {
        return this.gtEnvelope.getDimension();
    }

    @Override
    public double[] getLowerCorner() {
        return this.gtEnvelope.getLowerCorner().getCoordinate();
    }

    @Override
    public double[] getUpperCorner() {
        return this.gtEnvelope.getUpperCorner().getCoordinate();
    }

}
