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
package org.n52.wps.io;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

public class SchemaRepository {
    private static final Map<String, String> repository = Maps.newConcurrentMap();
    private static final Map<String, String> gmlNamespaces = Maps.newConcurrentMap();

    public static String getSchemaLocation(String namespaceURI) {
        return repository.get(namespaceURI);

    }

    public static void registerSchemaLocation(String namespaceURI,
                                              String schemaLocation) {
        Preconditions.checkNotNull(namespaceURI, "namespaceURI");
        Preconditions.checkNotNull(schemaLocation, "schemaLocation");

        repository.put(namespaceURI, schemaLocation);
    }

    public static void registerGMLVersion(String namespaceURI,
                                          String gmlNamespace) {
        Preconditions.checkNotNull(namespaceURI, "namespaceURI");
        Preconditions.checkNotNull(gmlNamespace, "gmlNamespace");
        gmlNamespaces.put(namespaceURI, gmlNamespace);

    }

    public static String getGMLNamespaceForSchema(String namespace) {
        return gmlNamespaces.get(namespace);
    }
}
