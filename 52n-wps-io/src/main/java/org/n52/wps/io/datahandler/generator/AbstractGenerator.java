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
package org.n52.wps.io.datahandler.generator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.commons.codec.binary.Base64InputStream;
import org.n52.wps.commons.Format;
import org.n52.wps.io.AbstractIOHandler;
import org.n52.wps.io.IGenerator;
import org.n52.wps.io.data.IData;
import org.n52.wps.server.ExceptionReport;

/**
 * @author Matthias Mueller, TU Dresden
 *
 */
public abstract class AbstractGenerator extends AbstractIOHandler
        implements IGenerator {

    public AbstractGenerator(Set<Format> formats,
                             Set<Class<?>> dataTypes) {
        super(formats, dataTypes);
    }

    public AbstractGenerator(Set<Class<?>> dataTypes) {
        super(dataTypes);
    }

    public AbstractGenerator(Class<?>... dataTypes) {
        super(dataTypes);
    }

    @Override
    public InputStream generate(IData data, Format format)
            throws IOException, ExceptionReport {
        if (!format.hasEncoding()|| format.hasEncoding(DEFAULT_ENCODING)) {
            return generateStream(data, format);
        } else if (format.hasEncoding(ENCODING_BASE64)) {
            return new Base64InputStream(generate(data, format.withoutEncoding()), true);
        } else {
            throw new ExceptionReport("Unable to generate encoding " + format
                    .getEncoding().orNull(), ExceptionReport.NO_APPLICABLE_CODE);
        }
    }

    protected abstract InputStream generateStream(IData data, Format format)
            throws IOException, ExceptionReport;
}
