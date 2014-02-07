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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.n52.wps.commons.Format;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.ArrayDataBinding;

/**
 * 
 * @author Bastian Schaeffer; Matthias Mueller, TU Dresden
 *
 */
public class WCPSGenerator extends AbstractGenerator {
	
	public WCPSGenerator(){
		super(ArrayDataBinding.class);
	}
	
	@Override
	public InputStream generateStream(IData data, Format format) throws IOException {

//		// check for correct request before returning the stream
//		if (!(this.isSupportedGenerate(data.getSupportedClass(), mimeType, schema))){
//			throw new IOException("I don't support the incoming datatype");
//		}
		
		List<byte[]> wcpsoutput = ((ArrayDataBinding)data).getPayload();
		File tempFile = File.createTempFile("wcps", ".bin");
		this.finalizeFiles.add(tempFile);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            for (byte[] currentArray : wcpsoutput){
                fos.write(currentArray);
            }
            fos.flush();
        }
		return new FileInputStream(tempFile);
	}
}
