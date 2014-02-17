/**
 * Copyright (C) 2007 - 2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       * Apache License, version 2.0
 *       * Apache Software License, version 1.0
 *       * GNU Lesser General Public License, version 3
 *       * Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       * Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.wps.io.data.binding.bbox;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;

import org.n52.wps.io.data.IBBOXData;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;

import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.Coordinate;

public class GTReferenceEnvelope implements IBBOXData {
    private final Envelope gtEnvelope;
    private final String crs;

    public GTReferenceEnvelope(Object llx, Object lly,
                               Object upx, Object upy,
                               String crs) throws ExceptionReport {

        try {
            double minx = Double.parseDouble(llx.toString());
            double miny = Double.parseDouble(lly.toString());
            double maxx = Double.parseDouble(upx.toString());
            double maxy = Double.parseDouble(upy.toString());
            Coordinate ll = new Coordinate(minx, miny);
            Coordinate ur = new Coordinate(maxx, maxy);
            com.vividsolutions.jts.geom.Envelope jtsEnvelope = new com.vividsolutions.jts.geom.Envelope(ll, ur);
            this.crs = crs;
            if (crs == null) {
                this.gtEnvelope = new ReferencedEnvelope(jtsEnvelope, null);
            } else {
                this.gtEnvelope = new ReferencedEnvelope(jtsEnvelope, CRS.decode(crs));
            }

        } catch (NumberFormatException | MismatchedDimensionException | FactoryException e) {
            throw new NoApplicableCodeException("Error while creating BoundingBox").causedBy(e);
        }
    }

    public GTReferenceEnvelope(Envelope envelope) {
        this.gtEnvelope = Preconditions.checkNotNull(envelope);
        if (envelope.getCoordinateReferenceSystem() != null &&
            !envelope.getCoordinateReferenceSystem().getIdentifiers().isEmpty()) {
            this.crs = envelope.getCoordinateReferenceSystem().getIdentifiers().iterator().next().toString();
        } else {
            this.crs = null;
        }
    }

    @Override
    public Envelope getPayload() {
        return gtEnvelope;
    }

    @Override
    public Class<ReferencedEnvelope> getSupportedClass() {
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
