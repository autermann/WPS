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
 *       • Apache License, version 2.0
 *       • Apache Software License, version 1.0
 *       • GNU Lesser General Public License, version 3
 *       • Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       • Common Development and Distribution License (CDDL), version 1.0
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
package org.n52.wps.server.algorithm.test;

import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataInput;
import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.algorithm.annotation.LiteralDataOutput;
import org.n52.wps.io.data.binding.complex.GenericXMLDataBinding;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Algorithm(version = "1.0.0", title = "Echo process", abstrakt = "A simple echo process for complex and literal data - get what you give.")
public class EchoProcess extends AbstractAnnotatedAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(EchoProcess.class);

    private List<XmlObject> complexInput;
    private List<String> literalInput;

    private XmlObject complexOutput;
    private String literalOutput;

    @Execute
    public void echo() {
        log.debug("Running echo process");

        if (complexInput != null && complexInput.size() > 0)
            complexOutput = complexInput.get(0);
        else
            log.debug("No complex inputs.");

        if (literalInput != null && literalInput.size() > 0)
            literalOutput = literalInput.get(0);
        else
            log.debug("No literal input");

        log.debug("Finished echo process, literal output is '{}', complex output is : {}", literalOutput, complexOutput);
    }

    @ComplexDataOutput(identifier = "complexOutput", binding = GenericXMLDataBinding.class)
    public XmlObject getComplexOutput() {
        return complexOutput;
    }

    @LiteralDataOutput(identifier = "literalOutput")
    public String getLiteralOutput() {
        return literalOutput;
    }

    @ComplexDataInput(binding = GenericXMLDataBinding.class, identifier = "complexInput", minOccurs = 0, maxOccurs = 1)
    public void setComplexInput(List<XmlObject> complexInput) {
        this.complexInput = complexInput;
    }

    @LiteralDataInput(identifier = "literalInput", minOccurs = 0, maxOccurs = 1)
    public void setLiteralInput(List<String> literalInput) {
        this.literalInput = literalInput;
    }

}
