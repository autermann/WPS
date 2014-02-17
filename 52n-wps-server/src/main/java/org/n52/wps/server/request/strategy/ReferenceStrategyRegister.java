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
package org.n52.wps.server.request.strategy;

import java.util.LinkedList;

import net.opengis.wps.x100.InputType;

import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;

public class ReferenceStrategyRegister {
	protected final LinkedList<IReferenceStrategy> registeredStrategies = new LinkedList<>();

    private ReferenceStrategyRegister(){
    }
	
	public void registerStrategy(IReferenceStrategy strategy){
        registeredStrategies.addFirst(strategy);
    }

    public ReferenceInputStream resolveReference(InputType input)
            throws ExceptionReport {
        IReferenceStrategy strategy = findStrategy(input);
        if (strategy == null) {
            throw new NoApplicableCodeException("No ReferenceStrategy found to fetch %s",
                                                input.getIdentifier().getStringValue());
        }
        return strategy.fetchData(input);
    }

    private IReferenceStrategy findStrategy(InputType input) {
        for (IReferenceStrategy strategy : registeredStrategies) {
            if (strategy.isApplicable(input)) {
                return strategy;
            }
        }
        return null;
    }

    public static ReferenceStrategyRegister getInstance() {
        return ReferenceStrategyRegisterHolder.INSTANCE;
    }

    private static class ReferenceStrategyRegisterHolder {
        private static final ReferenceStrategyRegister INSTANCE;

        static {
            INSTANCE = new ReferenceStrategyRegister();
            INSTANCE.registerStrategy(new DefaultReferenceStrategy());
            INSTANCE.registerStrategy(new WCS111XMLEmbeddedBase64OutputReferenceStrategy());
        }

        private ReferenceStrategyRegisterHolder() {
        }
    }
}
