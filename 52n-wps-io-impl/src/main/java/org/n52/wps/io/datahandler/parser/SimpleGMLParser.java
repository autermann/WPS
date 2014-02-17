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
package org.n52.wps.io.datahandler.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.opengis.examples.packet.GMLPacketDocument;
import net.opengis.examples.packet.PacketMemberType;
import net.opengis.examples.packet.PropertyType;
import net.opengis.examples.packet.PropertyType.Value;
import net.opengis.examples.packet.StaticFeatureType;
import net.opengis.gml.CoordType;
import net.opengis.gml.LineStringPropertyType;
import net.opengis.gml.LinearRingMemberType;
import net.opengis.gml.LinearRingType;
import net.opengis.gml.PointPropertyType;
import net.opengis.gml.PolygonPropertyType;

import org.apache.xmlbeans.XmlException;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.commons.Format;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


/**
 * This parser handles xml files compliant to gmlpacket.xsd
 * @author foerster
 *
 */
public class SimpleGMLParser extends AbstractParser {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleGMLParser.class);
	private SimpleFeatureType type;
	private SimpleFeatureBuilder featureBuilder;
	private final GeometryFactory geomFactory;

	public SimpleGMLParser() {
		super(GTVectorDataBinding.class);
		geomFactory = new GeometryFactory();
	}

    @Override
	public GTVectorDataBinding parse(InputStream stream, Format format)
            throws ExceptionReport {
		try {
            return parseXML(GMLPacketDocument.Factory.parse(stream));
		} catch(XmlException e) {
			throw new NoApplicableCodeException("Error while parsing XML").causedBy(e);
		} catch(IOException e) {
			throw new NoApplicableCodeException("Error transfering XML").causedBy(e);
		}
	}

	private GTVectorDataBinding parseXML(GMLPacketDocument doc) {
		int numberOfMembers = doc.getGMLPacket().getPacketMemberArray().length;
		List<SimpleFeature> simpleFeatureList = new ArrayList<>(numberOfMembers);
		for(int i = 0; i< numberOfMembers; i++) {
            PacketMemberType packetMember = doc.getGMLPacket().getPacketMemberArray(i);
			StaticFeatureType feature = packetMember.getStaticFeature();
			//at the start create the featureType and the featureBuilder
			if(i==0) {
				type = createFeatureType(feature);
				featureBuilder = new SimpleFeatureBuilder(type);
			}

			SimpleFeature newFeature = convertStaticFeature(feature);
			if (newFeature != null) {
				simpleFeatureList.add(newFeature);
			} else {
				LOGGER.debug("feature has no geometry, feature will not be included in featureCollection");
			}
		}

		SimpleFeatureCollection collection = new ListFeatureCollection(type, simpleFeatureList);

		return new GTVectorDataBinding(collection);
	}

	private SimpleFeature convertStaticFeature(StaticFeatureType staticFeature) {

		SimpleFeature feature = null;
		Geometry geom = null;
		if(staticFeature.isSetLineStringProperty()) {
			geom = convertToJTSGeometry(staticFeature.getLineStringProperty());
		} else if(staticFeature.isSetPointProperty()) {
			geom = convertToJTSGeometry(staticFeature.getPointProperty());
		} else if(staticFeature.isSetPolygonProperty()) {
			geom = convertToJTSGeometry(staticFeature.getPolygonProperty());
		}
		if(geom == null) {
			return null;
		}

		if(type.getAttributeCount()>1){
			if(staticFeature.sizeOfPropertyArray() > 0){
				ArrayList<Object> properties = new ArrayList<>(staticFeature.sizeOfPropertyArray());
				properties.add(geom);
				for (int i = 0; i < staticFeature.sizeOfPropertyArray(); i++) {
					PropertyType ptype = staticFeature.getPropertyArray(i);
					if(!ptype.getPropertyName().contains("geom")){
					Value v = ptype.getValue();
					properties.add(v.getStringValue());
					}
				}
				feature = featureBuilder.buildFeature(staticFeature.getFid(), properties.toArray());
			}

		}
		else {
		 feature = featureBuilder.buildFeature(staticFeature.getFid(), new Object[]{geom});
			}

		return feature;
	}

	private SimpleFeatureType createFeatureType(StaticFeatureType staticFeature) {
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("gmlPacketFeatures");

		if(staticFeature.isSetLineStringProperty()) {
			typeBuilder.add( "LineString", LineString.class);

		}
		else if(staticFeature.isSetPointProperty()) {
			typeBuilder.add( "Point", Point.class);
		}
		else if(staticFeature.isSetPolygonProperty()) {
			typeBuilder.add( "Polygon", Polygon.class);
		}

		if(staticFeature.sizeOfPropertyArray() > 0){
			for (int i = 0; i < staticFeature.sizeOfPropertyArray(); i++) {

				PropertyType t = staticFeature.getPropertyArray(i);
				if(!t.getPropertyName().contains("geom")) {
					typeBuilder.add(t.getPropertyName(),String.class);
				}
			}

		}
		return typeBuilder.buildFeatureType();
	}

	private Geometry convertToJTSGeometry(LineStringPropertyType lineString) {
		Geometry geom;
		if(lineString.getLineString().getCoordArray().length != 0) {
			CoordType[] xmlCoords = lineString.getLineString().getCoordArray();
			Coordinate[] coords = convertToJTSCoordinates(xmlCoords);
			if(coords.length == 0) {
				LOGGER.debug("feature does not include any geometry (LineString)");
				return null;
			}
			geom = geomFactory.createLineString(coords);
		}
		else if (lineString.getLineString().isSetCoordinates()) {
			throw new IllegalArgumentException("Element gml:coordinates is not supported yet");
		}
		else {
			LOGGER.debug("LineString has no coordinates");
			return null;
		}
		return geom;
	}

	private Geometry convertToJTSGeometry(PointPropertyType point) {
		Coordinate coord = convertToJTSCoordinate(point.getPoint().getCoord());
		return geomFactory.createPoint(coord);
	}

	private Geometry convertToJTSGeometry(PolygonPropertyType polygon) {
		LinearRingType outerRing = polygon.getPolygon().getOuterBoundaryIs().getLinearRing();
		LinearRing jtsOuterRing = convertToJTSLinearRing(outerRing);
		LinearRingMemberType[] innerRings = polygon.getPolygon().getInnerBoundaryIsArray();
		List<LinearRing> jtsInnerRings = new ArrayList<>(innerRings.length);
		for(LinearRingMemberType ring : innerRings) {
			if(ring.getLinearRing() != null) {
				jtsInnerRings.add(convertToJTSLinearRing(ring.getLinearRing()));
			}
		}
		return geomFactory.createPolygon(jtsOuterRing, jtsInnerRings.toArray(new LinearRing[jtsInnerRings.size()]));
	}

	private LinearRing convertToJTSLinearRing(LinearRingType linearRing) {
		Coordinate[] coords = convertToJTSCoordinates(linearRing.getCoordArray());
		return geomFactory.createLinearRing(coords);
	}

	/**
	 * expects Coordinates with X & Y or X & Y & Z
	 * @param coords
	 * @return
	 */
	private Coordinate[] convertToJTSCoordinates(CoordType[] coords) {
		List<Coordinate> coordList = new ArrayList<>(coords.length);
		for(CoordType coord : coords) {
			Coordinate coordinate = convertToJTSCoordinate(coord);
			coordList.add(coordinate);
		}
		return coordList.toArray(new Coordinate[coordList.size()]);
	}

	private Coordinate convertToJTSCoordinate(CoordType coord) {
		if(!coord.isSetZ()) {
			return new Coordinate(coord.getX().doubleValue(), coord.getY().doubleValue());
		}
		else {
			return new Coordinate(coord.getX().doubleValue(),
										coord.getY().doubleValue(),
										coord.getZ().doubleValue());
		}
	}

}
