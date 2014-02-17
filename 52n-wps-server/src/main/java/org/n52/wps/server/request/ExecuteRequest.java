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
package org.n52.wps.server.request;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.opengis.ows.x11.ExceptionType;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.InputType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ResponseDocumentType;
import net.opengis.wps.x100.StatusType;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import org.n52.wps.commons.context.ExecutionContext;
import org.n52.wps.commons.context.ExecutionContextFactory;
import org.n52.wps.io.data.IComplexData;
import org.n52.wps.io.data.IData;
import org.n52.wps.server.AbstractTransactionalAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.IAlgorithm;
import org.n52.wps.server.InvalidParameterValueException;
import org.n52.wps.server.MissingParameterValueException;
import org.n52.wps.server.NoApplicableCodeException;
import org.n52.wps.server.RepositoryManager;
import org.n52.wps.server.WPSConstants;
import org.n52.wps.server.database.DatabaseFactory;
import org.n52.wps.server.observerpattern.IObserver;
import org.n52.wps.server.observerpattern.ISubject;
import org.n52.wps.server.response.ExecuteResponse;
import org.n52.wps.server.response.ExecuteResponseBuilder;
import org.n52.wps.server.response.Response;

/**
 * Handles an ExecuteRequest
 */
public class ExecuteRequest extends Request implements IObserver {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteRequest.class);
	private ExecuteDocument execDom;
	private Map<String, IData> returnResults;
	private final ExecuteResponseBuilder execRespType;
	
	

	/**
	 * Creates an ExecuteRequest based on a Document (HTTP_POST)
	 * 
	 * @param doc
	 *            The clients submission
	 * @throws ExceptionReport
	 */
	public ExecuteRequest(Document doc) throws ExceptionReport {
		super(doc);
		try {
			XmlOptions option = new XmlOptions();
			option.setLoadTrimTextBuffer();
			this.execDom = ExecuteDocument.Factory.parse(doc, option);
			if (this.execDom == null) {
				LOGGER.error("ExecuteDocument is null");
				throw new MissingParameterValueException("Error while parsing post data");
			}
		} catch (XmlException e) {
			throw new MissingParameterValueException("Error while parsing post data").causedBy(e);
		}

		// validate the client input
		validate();

		// create an initial response
		execRespType = new ExecuteResponseBuilder(this);
        
        storeRequest(execDom);
	}

	/*
	 * Creates an ExecuteRequest based on a Map (HTTP_GET). NOTE: Parameters are
	 * treated as non case sensitive. @param ciMap The client input @throws
	 * ExceptionReport
	 */
	public ExecuteRequest(CaseInsensitiveMap ciMap) throws ExceptionReport {
		super(ciMap);
        this.execDom = new KVPRequestTransformer(ciMap).transformExecute();
		// validate the client input
		validate();

		// create an initial response
		execRespType = new ExecuteResponseBuilder(this);

        storeRequest(ciMap);
	}

    /**
	 * Validates the client request
	 * 
	 * @return True if the input is valid, False otherwise
	 */
    @Override
	public boolean validate() throws ExceptionReport {
		// Identifier must be specified.
		/*
		 * Only for HTTP_GET: String identifier = getMapValue("identifier");
		 * 
		 * try{ // Specifies if all complex valued output(s) of this process
		 * should be stored by process // as web-accessible resources store =
		 * getMapValue("store").equals("true");
		 *  // Specifies if Execute operation response shall be returned quickly
		 * with status information status =
		 * getMapValue("status").equals("true"); }catch(ExceptionReport e){ //
		 * if parameters "store" or "status" are not included, they default to
		 * false; }
		 *  // just testing if the number of arguments is even... String[]
		 * diArray = getMapValue("DataInputs").split(","); if(diArray.length % 2 !=
		 * 0) { throw new ExceptionReport("Incorrect number of arguments for
		 * parameter dataInputs, please only a even number of parameter values",
		 * ExceptionReport.INVALID_PARAMETER_VALUE); }
		 * 
		 */
		if (!execDom.getExecute().getVersion().equals(WPSConstants.WPS_SERVICE_VERSION)) {
			throw new InvalidParameterValueException("Specified version is not supported.")
                    .locatedAt("version=" + getExecute().getVersion());
		}

		String identifier = getAlgorithmIdentifier();
		
		if(execDom.getExecute().getIdentifier() == null ||
           execDom.getExecute().getIdentifier().getStringValue() == null) {
			throw new MissingParameterValueException("No process identifier supplied.")
                    .locatedAt(WPSConstants.PARAMETER_IDENTIFIER);
		}
		
		// check if the algorithm is in our repository
		if (!RepositoryManager.getInstance().containsAlgorithm(
				identifier)) {
			throw new InvalidParameterValueException("Specified process identifier does not exist").locatedAt("identifier=" + identifier);
		}

		// validate if the process can be executed
		ProcessDescriptionType desc = RepositoryManager.getInstance().getProcessDescription(getAlgorithmIdentifier());
		// We need a description of the inputs for the algorithm
		if (desc == null) {
			LOGGER.warn("desc == null");
			return false;
		}

		// Get the inputdescriptions of the algorithm
		
		if(desc.getDataInputs()!=null){
			InputDescriptionType[] inputDescs = desc.getDataInputs().getInputArray();
		
		//prevent NullPointerException for zero input values in execute request (if only default values are used)
		InputType[] inputs;
		if(getExecute().getDataInputs()==null) {
            inputs=new InputType[0];
        } else {
            inputs = getExecute().getDataInputs().getInputArray();
            }
			
			// For each input supplied by the client
			for (InputType input : inputs) {
				boolean identifierMatched = false;
				// Try to match the input with one of the descriptions
				for (InputDescriptionType inputDesc : inputDescs) {
					// If found, then process:
					if (inputDesc.getIdentifier().getStringValue().equals(
							input.getIdentifier().getStringValue())) {
						identifierMatched = true;
						// If it is a literal value,
						if (input.getData() != null
								&& input.getData().getLiteralData() != null) {
							// then check if the desription is also of type literal
							if (inputDesc.getLiteralData() == null) {
								throw new ExceptionReport(
										"Inputtype LiteralData is not supported",
										ExceptionReport.INVALID_PARAMETER_VALUE);
							}
							// literalValue.getDataType ist optional
							if (input.getData().getLiteralData().getDataType() != null) {
								if (inputDesc.getLiteralData() != null)
									if (inputDesc.getLiteralData().getDataType() != null)
										if (inputDesc.getLiteralData().getDataType().getReference() != null)
											if (!input.getData().getLiteralData().getDataType().equals(inputDesc.getLiteralData().getDataType().getReference())) {
												throw new ExceptionReport(
														"Specified dataType is not supported "
																+ input
																		.getData()
																		.getLiteralData()
																		.getDataType()
																+ " for input "
																+ input
																		.getIdentifier()
																		.getStringValue(),
														ExceptionReport.INVALID_PARAMETER_VALUE);
											}
							}
						}
						// Excluded, because ProcessDescription validation should be
						// done on startup!
						// else if (input.getComplexValue() != null) {
						// if(ParserFactory.getInstance().getParser(input.getComplexValue().getSchema())
						// == null) {
						// LOGGER.warn("Request validation message: schema attribute
						// null, so the simple one will be used!");
						// }
						// }
						// else if (input.getComplexValueReference() != null) {
						// // we found a complexvalue input, try to get the parser.
						// if(ParserFactory.getInstance().getParser(input.getComplexValueReference().getSchema())
						// == null) {
						// LOGGER.warn("Request validation message: schema attribute
						// null, so the simple one will be used!");
						// }
						// }
						break;
					}
				}
				// if the identifier did not match one of the descriptions, it is
				// invalid
				if (!identifierMatched) {
					throw new ExceptionReport("Input Identifier is not valid: "
							+ input.getIdentifier().getStringValue(),
							ExceptionReport.INVALID_PARAMETER_VALUE,
							"input identifier");
				}
			}
		}
		return true;
	}

	/**
	 * Actually serves the Request.
	 * 
	 * @throws ExceptionReport
	 */
    @Override
	public Response call() throws ExceptionReport {
        IAlgorithm algorithm = null;
        Map<String, List<IData>> inputMap = null;
		try {
			ExecutionContext context;
			if (getExecute().isSetResponseForm()) {
				context = getExecute().getResponseForm().isSetRawDataOutput() ?
	                    new ExecutionContext(getExecute().getResponseForm().getRawDataOutput()) :
	                    new ExecutionContext(Arrays.asList(getExecute().getResponseForm().getResponseDocument().getOutputArray()));
			}
			else {
				context = new ExecutionContext();
			}
	
				// register so that any function that calls ExecuteContextFactory.getContext() gets the instance registered with this thread
			ExecutionContextFactory.registerContext(context);
			
			LOGGER.debug("started with execution");
            
			updateStatusStarted();
            
			// parse the input
			InputType[] inputs = new InputType[0];
			if( getExecute().getDataInputs()!=null){
				inputs = getExecute().getDataInputs().getInputArray();
			}
			InputHandler parser = new InputHandler.Builder(inputs, getAlgorithmIdentifier()).build();
			
			// we got so far:
			// get the algorithm, and run it with the clients input
		
			/*
			 * IAlgorithm algorithm =
			 * RepositoryManager.getInstance().getAlgorithm(getAlgorithmIdentifier());
			 * returnResults = algorithm.run((Map)parser.getParsedInputLayers(),
			 * (Map)parser.getParsedInputParameters());
			 */
			algorithm = RepositoryManager.getInstance().getAlgorithm(getAlgorithmIdentifier());
			
			if(algorithm instanceof ISubject){
				ISubject subject = (ISubject) algorithm;
				subject.addObserver(this);
				
			}
		
            if (algorithm instanceof AbstractTransactionalAlgorithm) {
                returnResults = ((AbstractTransactionalAlgorithm) algorithm).run(execDom);
            } else {
                inputMap = parser.getParsedInputData();
                returnResults = algorithm.run(inputMap);
            }

            List<String> errorList = algorithm.getErrors();
            if (errorList != null && !errorList.isEmpty()) {
                String errorMessage = errorList.get(0);
                LOGGER.error("Error reported while handling ExecuteRequest for " + getAlgorithmIdentifier() + ": " + errorMessage);
                updateStatusError(errorMessage);
            } else {
                updateStatusSuccess();
            }
		} catch(Throwable e) {
            String errorMessage = null;
            if (algorithm != null && algorithm.getErrors() != null && !algorithm.getErrors().isEmpty()) {
                errorMessage = algorithm.getErrors().get(0);
            }
            if (errorMessage == null) {
                errorMessage = e.toString();
            }
            if (errorMessage == null) {
                errorMessage = "UNKNOWN ERROR";
            }
            LOGGER.error("Exception/Error while executing ExecuteRequest for " + getAlgorithmIdentifier() + ": " + errorMessage);
            updateStatusError(errorMessage);
			if (e instanceof Error) {
                // This is required when catching Error
                throw (Error)e;
            }
            if (e instanceof ExceptionReport) {
                throw (ExceptionReport)e;
            } else {
                throw new NoApplicableCodeException("Error while executing the embedded process for: %s", getAlgorithmIdentifier()).causedBy(e);
            }
        } finally {
			//  you ***MUST*** call this or else you will have a PermGen ClassLoader memory leak due to ThreadLocal use
			ExecutionContextFactory.unregisterContext();
            if (algorithm instanceof ISubject) {
                ((ISubject)algorithm).removeObserver(this);
            }
            if (inputMap != null) {
                for(List<IData> l : inputMap.values()) {
                    for (IData d : l) {
                        if (d instanceof IComplexData) {
                            ((IComplexData)d).dispose();
                        }
                    }
                }
            }
            if (returnResults != null) {
                for (IData d : returnResults.values()) {
                    if (d instanceof IComplexData) {
                        ((IComplexData)d).dispose();
                    }
                }
            }
		}
        return new ExecuteResponse(this);
	}
    

	/**
	 * Gets the identifier of the algorithm the client requested
	 * 
	 * @return An identifier
	 */
	public String getAlgorithmIdentifier() {
		//Fix for bug https://bugzilla.52north.org/show_bug.cgi?id=906
		if(getExecute().getIdentifier() != null){
			return getExecute().getIdentifier().getStringValue();
		}
		return null;
	}
	
	/**
	 * Gets the Execute that is associated with this Request
	 * 
	 * @return The Execute
	 */
	public ExecuteDocument.Execute getExecute() {
		return execDom.getExecute();
	}

	public Map<String, IData> getAttachedResult() {
		return returnResults;
	}

	public boolean isStoreResponse() {
        return hasResponseForm() &&
               execDom.getExecute().getResponseForm().getRawDataOutput() == null &&
               hasResponseDocument() &&
               getResponseDocument().getStoreExecuteResponse();
	}

    public ResponseDocumentType getResponseDocument() {
        return execDom.getExecute().getResponseForm().getResponseDocument();
    }

    public boolean hasResponseDocument() {
        return getResponseDocument() != null;
    }

	public boolean isQuickStatus() {
        return hasResponseForm() &&
               execDom.getExecute().getResponseForm().getRawDataOutput() == null &&
               hasResponseDocument() &&
               getResponseDocument().getStatus();
	}

	public ExecuteResponseBuilder getExecuteResponseBuilder() {
		return this.execRespType;
	}

	public boolean isRawData() {
        return hasResponseForm() &&
               execDom.getExecute().getResponseForm().getRawDataOutput() != null;
	}


    public boolean isLineage() {
        return hasResponseForm() &&
               hasResponseDocument() &&
               getResponseDocument().getLineage();
    }

    public boolean hasResponseForm() {
        return execDom.getExecute().getResponseForm() != null;
    }
	
	public void update(ISubject subject) {
		Object state = subject.getState();
		LOGGER.info("Update received from Subject, state changed to : " + state);
		StatusType status = StatusType.Factory.newInstance();
		
		int percentage = 0;
		if (state instanceof Integer) {
			percentage = (Integer) state;
			status.addNewProcessStarted().setPercentCompleted(percentage);
		}else if(state instanceof String){
			status.addNewProcessStarted().setStringValue((String)state);
		}
		updateStatus(status);
	}
    
	public void updateStatusAccepted() {
		StatusType status = StatusType.Factory.newInstance();
		status.setProcessAccepted("Process Accepted");
		updateStatus(status);
	}
	
	public void updateStatusStarted() {
        StatusType status = StatusType.Factory.newInstance();
        status.addNewProcessStarted().setPercentCompleted(0);
        updateStatus(status);
    }
	
    public void updateStatusSuccess() {
        StatusType status = StatusType.Factory.newInstance();
        status.setProcessSucceeded("Process successful");
        updateStatus(status);
    }	
    
    public void updateStatusError(String errorMessage) {
		StatusType status = StatusType.Factory.newInstance();
		net.opengis.ows.x11.ExceptionReportDocument.ExceptionReport excRep = status
				.addNewProcessFailed().addNewExceptionReport();
		excRep.setVersion(WPSConstants.WPS_SERVICE_VERSION);
		ExceptionType excType = excRep.addNewException();
		excType.addNewExceptionText().setStringValue(errorMessage);
		excType.setExceptionCode(ExceptionReport.NO_APPLICABLE_CODE);
		updateStatus(status);
	}
	
	private void updateStatus(StatusType status) {
		getExecuteResponseBuilder().setStatus(status);
        try {
            getExecuteResponseBuilder().update();
            if (isStoreResponse()) {
                ExecuteResponse executeResponse = new ExecuteResponse(this);
                InputStream is = null;
                try {
                    is = executeResponse.getAsStream();
                    DatabaseFactory.getDatabase().storeResponse(getUniqueId().toString(), is);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        } catch (ExceptionReport e) {
            LOGGER.error("Update of process status failed.", e);
            throw new RuntimeException(e);
        }
	}
    
    private void storeRequest(ExecuteDocument executeDocument) {
        InputStream is = null;
        try {
            is = executeDocument.newInputStream();
            DatabaseFactory.getDatabase().insertRequest(
                    getUniqueId().toString(), is, true);
        } catch (Exception e) {
            LOGGER.error("Exception storing ExecuteRequest", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private void storeRequest(CaseInsensitiveMap map) {

        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(os);
             BufferedWriter bw = new BufferedWriter(osw)) {
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                String valueString;
                if (value instanceof String[]) {
                    valueString = ((String[]) value)[0];
                } else {
                    valueString = value.toString();
                }
                bw.append(key.toString()).append('=').append(valueString);
                bw.newLine();
            }
            bw.flush();
            DatabaseFactory.getDatabase().insertRequest(getUniqueId().toString(),
                        new ByteArrayInputStream(os.toByteArray()), false);
        } catch (Exception e) {
            LOGGER.error("Exception storing ExecuteRequest", e);
        }
    }
}
