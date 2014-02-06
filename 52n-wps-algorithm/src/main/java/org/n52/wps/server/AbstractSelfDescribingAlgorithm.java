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
package org.n52.wps.server;

import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import net.opengis.ows.x11.DomainMetadataType;
import net.opengis.wps.x100.CRSsType;
import net.opengis.wps.x100.ComplexDataCombinationsType;
import net.opengis.wps.x100.ComplexDataDescriptionType;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.LiteralInputType;
import net.opengis.wps.x100.LiteralOutputType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType.DataInputs;
import net.opengis.wps.x100.ProcessDescriptionType.ProcessOutputs;
import net.opengis.wps.x100.ProcessDescriptionsDocument;
import net.opengis.wps.x100.ProcessDescriptionsDocument.ProcessDescriptions;
import net.opengis.wps.x100.SupportedCRSsType;
import net.opengis.wps.x100.SupportedCRSsType.Default;
import net.opengis.wps.x100.SupportedComplexDataInputType;
import net.opengis.wps.x100.SupportedComplexDataType;

import org.n52.wps.commons.Format;
import org.n52.wps.io.GeneratorFactory;
import org.n52.wps.io.IGenerator;
import org.n52.wps.io.IOHandler;
import org.n52.wps.io.IParser;
import org.n52.wps.io.ParserFactory;
import org.n52.wps.io.data.IBBOXData;
import org.n52.wps.io.data.IComplexData;
import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.server.observerpattern.IObserver;
import org.n52.wps.server.observerpattern.ISubject;

public abstract class AbstractSelfDescribingAlgorithm
        extends AbstractAlgorithm implements ISubject {

    public static final String PROCESS_VERSION = "1.0.0";
    private final List<IObserver> observers = new ArrayList<>();
    private Object state = null;

    @Override
	protected ProcessDescriptionType initializeDescription() {
		ProcessDescriptionsDocument document = ProcessDescriptionsDocument.Factory.newInstance();
		ProcessDescriptions processDescriptions = document.addNewProcessDescriptions();
		ProcessDescriptionType processDescription = processDescriptions.addNewProcessDescription();
		processDescription.setStatusSupported(true);
		processDescription.setStoreSupported(true);
		processDescription.setProcessVersion(PROCESS_VERSION);
		
		//1. Identifier
		processDescription.addNewIdentifier().setStringValue(this.getClass().getName());
		processDescription.addNewTitle().setStringValue(this.getClass().getCanonicalName());
	
		//2. Inputs
		List<String> identifiers = this.getInputIdentifiers();
		DataInputs dataInputs = null;
		if(identifiers.size()>0){
			dataInputs = processDescription.addNewDataInputs();
		}
		
		for(String identifier : identifiers){
			InputDescriptionType dataInput = dataInputs.addNewInput();
			dataInput.setMinOccurs(getMinOccurs(identifier));
			dataInput.setMaxOccurs(getMaxOccurs(identifier));
			dataInput.addNewIdentifier().setStringValue(identifier);
			dataInput.addNewTitle().setStringValue(identifier);
			
			Class<?> inputDataTypeClass = this.getInputDataType(identifier);
			Class<?>[] interfaces = inputDataTypeClass.getInterfaces();
			
			//we have to add this because of the new AbstractLiteralDataBinding class 
			if(interfaces.length == 0){
				interfaces = inputDataTypeClass.getSuperclass().getInterfaces();
			}
			
			for(Class<?> implementedInterface : interfaces){
				if(implementedInterface.equals(ILiteralData.class)){
					LiteralInputType literalData = dataInput.addNewLiteralData();
					String inputClassType = "";
					
					Constructor<?>[] constructors = inputDataTypeClass.getConstructors();
					for(Constructor<?> constructor : constructors){
						Class<?>[] parameters = constructor.getParameterTypes();
						if(parameters.length==1){
							inputClassType	= parameters[0].getSimpleName();
						}
					}
					
					if(inputClassType.length()>0){
						DomainMetadataType datatype = literalData.addNewDataType();
						datatype.setReference("xs:"+inputClassType.toLowerCase());
						literalData.addNewAnyValue();		
					}
				}else if(implementedInterface.equals(IBBOXData.class)){
						SupportedCRSsType bboxData = dataInput.addNewBoundingBoxData();
						String[] supportedCRSAray = getSupportedCRSForBBOXInput(identifier);
						for(int i = 0; i<supportedCRSAray.length; i++){
							if(i==0){
								Default defaultCRS = bboxData.addNewDefault();
								defaultCRS.setCRS(supportedCRSAray[0]);
								if(supportedCRSAray.length==1){
									CRSsType supportedCRS = bboxData.addNewSupported();
									supportedCRS.addCRS(supportedCRSAray[0]);
								}
							}else{
								if(i==1){
									CRSsType supportedCRS = bboxData.addNewSupported();
									supportedCRS.addCRS(supportedCRSAray[1]);
								}else{
									bboxData.getSupported().addCRS(supportedCRSAray[i]);
								}
							}
						}
						
						
						
									
				}else if(implementedInterface.equals(IComplexData.class)){
					SupportedComplexDataInputType complexData = dataInput.addNewComplexData();					
					List<IParser> parsers = ParserFactory.getInstance().getAllParsers();
					List<IParser> foundParsers = new ArrayList<>();
					for(IParser parser : parsers) {
                        if (parser.isSupportedDataBinding(inputDataTypeClass)) {
                            foundParsers.add(parser);
                        }
					}
					
					addInputFormats(complexData, foundParsers);					

				}		
			}
		}
		
		//3. Outputs
		ProcessOutputs dataOutputs = processDescription.addNewProcessOutputs();
		List<String> outputIdentifiers = this.getOutputIdentifiers();
		for(String identifier : outputIdentifiers){
			OutputDescriptionType dataOutput = dataOutputs.addNewOutput();
			
			
			dataOutput.addNewIdentifier().setStringValue(identifier);
			dataOutput.addNewTitle().setStringValue(identifier);
			dataOutput.addNewAbstract().setStringValue(identifier);
			
			Class<?> outputDataTypeClass = this.getOutputDataType(identifier);
			Class<?>[] interfaces = outputDataTypeClass.getInterfaces();
			
			//we have to add this because of the new AbstractLiteralDataBinding class 
			if(interfaces.length == 0){
				interfaces = outputDataTypeClass.getSuperclass().getInterfaces();
			}
			for(Class<?> implementedInterface : interfaces){
					
				
				if(implementedInterface.equals(ILiteralData.class)){
					LiteralOutputType literalData = dataOutput.addNewLiteralOutput();
					String outputClassType = "";
					
					Constructor<?>[] constructors = outputDataTypeClass.getConstructors();
					for(Constructor<?> constructor : constructors){
						Class<?>[] parameters = constructor.getParameterTypes();
						if(parameters.length==1){
							outputClassType	= parameters[0].getSimpleName();
						}
					}
					
					if(outputClassType.length()>0){
						literalData.addNewDataType().setReference("xs:"+outputClassType.toLowerCase());
					}
				
				}else if(implementedInterface.equals(IBBOXData.class)){
					SupportedCRSsType bboxData = dataOutput.addNewBoundingBoxOutput();
					String[] supportedCRSAray = getSupportedCRSForBBOXOutput(identifier);
					for(int i = 0; i<supportedCRSAray.length; i++){
						if(i==0){
							Default defaultCRS = bboxData.addNewDefault();
							defaultCRS.setCRS(supportedCRSAray[0]);
							if(supportedCRSAray.length==1){
								CRSsType supportedCRS = bboxData.addNewSupported();
								supportedCRS.addCRS(supportedCRSAray[0]);
							}
						}else{
							if(i==1){
								CRSsType supportedCRS = bboxData.addNewSupported();
								supportedCRS.addCRS(supportedCRSAray[1]);
							}else{
								bboxData.getSupported().addCRS(supportedCRSAray[i]);
							}
						}
					}
					
				}else if(implementedInterface.equals(IComplexData.class)){
					
                    SupportedComplexDataType complexData = dataOutput.addNewComplexOutput();

                    List<IGenerator> generators = GeneratorFactory.getInstance().getAllGenerators();
                    List<IGenerator> foundGenerators = new ArrayList<>();
                    for(IGenerator generator : generators) {
                        if (generator.isSupportedDataBinding(outputDataTypeClass)) {
                            foundGenerators.add(generator);
                        }
					}
					
					addOutputFormats(complexData, foundGenerators);

				}		
			}
		}
		
		return document.getProcessDescriptions().getProcessDescriptionArray(0);
	}
	
	/**
	 * Override this class for BBOX input data to set supported mime types. The first one in the resulting array will be the default one.
	 * @param identifier ID of the input BBOXType
	 * @return
	 */
	public String[] getSupportedCRSForBBOXInput(String identifier){
		return new String[0];
	}
	
	/**
	 * Override this class for BBOX output data to set supported mime types. The first one in the resulting array will be the default one.
	 * @param identifier ID of the input BBOXType
	 * @return
	 */
	public String[] getSupportedCRSForBBOXOutput(String identifier){
		return new String[0];
	}
	
	public BigInteger getMinOccurs(String identifier){
		return BigInteger.ONE;
	}
	public BigInteger getMaxOccurs(String identifier){
		return BigInteger.ONE;
	}
	
	public abstract List<String> getInputIdentifiers();
	public abstract List<String> getOutputIdentifiers();
	
	public Object getState() {
	  return state;
	}

	public void update(Object state) {
	   this.state = state;
	   notifyObservers();
	}

	 public void addObserver(IObserver o) {
	   observers.add(o);
	 }

	 public void removeObserver(IObserver o) {
	   observers.remove(o);
	 }

	 public void notifyObservers() {
         for (IObserver o : this.observers) {
            o.update(this);
        }
    }

    @Override
    public List<String> getErrors() {
        List<String> errors = new ArrayList<>();
        return errors;
    }

	private void addInputFormats(SupportedComplexDataInputType complexData, List<IParser> foundParsers) {
        encodeFormats(complexData, foundParsers);
	}

	private void addOutputFormats(SupportedComplexDataType complexData, List<IGenerator> foundGenerators) {
        encodeFormats(complexData, foundGenerators);
    }

    private void encodeFormats(SupportedComplexDataType complexData,
                               List<? extends IOHandler> foundGenerators) {
        ComplexDataCombinationsType xbSupported = complexData.addNewSupported();
        for (IOHandler parser : foundGenerators) {
            for (Format format : parser.getSupportedFormats()) {
                ComplexDataDescriptionType xbFormat;
                if (complexData.getDefault() == null) {
                    xbFormat = complexData.addNewDefault().addNewFormat();
                } else {
                    xbFormat = xbSupported.addNewFormat();
                }
                if (format.getEncoding().isPresent()) {
                    xbFormat.setEncoding(format.getEncoding().get());
                }
                if (format.getSchema().isPresent()) {
                    xbFormat.setSchema(format.getSchema().get());
                }
                if (format.getMimeType().isPresent()) {
                    xbFormat.setMimeType(format.getMimeType().get());
                }
            }
        }
    }
}
