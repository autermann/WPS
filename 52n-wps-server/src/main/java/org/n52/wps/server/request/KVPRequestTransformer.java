package org.n52.wps.server.request;

import static org.n52.wps.server.request.Request.getMapValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.opengis.ows.x11.BoundingBoxType;
import net.opengis.wps.x100.ComplexDataType;
import net.opengis.wps.x100.DataInputsType;
import net.opengis.wps.x100.DocumentOutputDefinitionType;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.ExecuteDocument.Execute;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.InputReferenceType;
import net.opengis.wps.x100.InputType;
import net.opengis.wps.x100.LiteralDataType;
import net.opengis.wps.x100.OutputDefinitionType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.opengis.wps.x100.ResponseDocumentType;
import net.opengis.wps.x100.ResponseFormType;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import org.n52.wps.commons.Format;
import org.n52.wps.commons.OwsCodeType;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.InvalidParameterValueException;
import org.n52.wps.server.MissingParameterValueException;
import org.n52.wps.server.NoApplicableCodeException;
import org.n52.wps.server.RepositoryManager;
import org.n52.wps.server.VersionNegotiationFailedException;
import org.n52.wps.server.WPSConstants;
import org.n52.wps.util.XMLBeansHelper;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class KVPRequestTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KVPRequestTransformer.class);
    private static final String RAW_DATA_OUTPUT_PARAMETER = "RawDataOutput";
    private static final String RESPONSE_DOCUMENT_PARAMETER = "ResponseDocument";
    private static final String DATA_INPUTS_PARAMETER = "DataInputs";
    private static final String ATTRIBUTE_XLINK_HREF = "xlink:href";
    private static final String ATTRIBUTE_DATATYPE = "datatype";
    private static final String ATTRIBUTE_UOM = "uom";
    private static final String ATTRIBUTE_ENCODING = "encoding";
    private static final String ATTRIBUTE_MIME_TYPE = "mimeType";
    private static final String ATTRIBUTE_SCHEMA = "schema";
    private static final String ATTRIBUTE_HREF = "href";
    private final CaseInsensitiveMap ciMap;
    private ExecuteDocument execDom;

    public KVPRequestTransformer(CaseInsensitiveMap ciMap) {
        this.ciMap = ciMap;
    }

    public ExecuteDocument transformExecute()
            throws ExceptionReport {
        this.execDom = ExecuteDocument.Factory.newInstance();
        Execute execute = execDom.addNewExecute();
        execute.setVersion(parseVersion());
        OwsCodeType processID = parseProcessIdentifier();
        processID.encodeTo(execute.addNewIdentifier());
		
        ProcessDescriptionType description = getProcessDescription(processID);
        parseInputs(description);
        boolean status = parseStatus();
        boolean store = parseStoreExecuteResponse();
        // Handle ResponseDocument option
        String responseDocument = Request.getMapValue(RESPONSE_DOCUMENT_PARAMETER,ciMap, false);
        if (responseDocument != null) {
            parseResponseDocument(responseDocument, execute, status, store, description);
        }
        String rawData =Request.getMapValue(RAW_DATA_OUTPUT_PARAMETER, ciMap, false);
        if (rawData != null) {
            parseRawData(rawData, description, execute);
        }
        return execDom;
    }

    private void parseRawData(String rawData, ProcessDescriptionType description,
                              Execute execute)
            throws NoApplicableCodeException, InvalidParameterValueException {
        String[] rawDataparameters = rawData.split("@");
        String rawDataInput;
        if (rawDataparameters.length > 0) {
            rawDataInput = rawDataparameters[0];
        } else {
            rawDataInput = rawData;
        }
        OutputDescriptionType outputDesc = XMLBeansHelper.findOutputByID(rawDataInput, description.getProcessOutputs().getOutputArray());
        if (outputDesc == null) {
            throw new InvalidParameterValueException("Data output Identifier not supported: %s", rawData);
        }
        ResponseFormType responseForm = execute.addNewResponseForm();
        OutputDefinitionType output = responseForm.addNewRawDataOutput();
        output.addNewIdentifier().setStringValue(
                outputDesc.getIdentifier().getStringValue());
        
        if (rawDataparameters.length > 0) {
            for (String rawDataparameter : rawDataparameters) {
                int attributePos = rawDataparameter.indexOf('=');
                if (attributePos == -1 || attributePos + 1 >= rawDataparameter.length()) {
                    continue;
                }
                String attributeName = rawDataparameter.substring(0, attributePos);
                String attributeValue = rawDataparameter.substring(attributePos + 1);
                try{
                    attributeValue = URLDecoder.decode(attributeValue, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new NoApplicableCodeException("Something went wrong while trying to decode value of %s", attributeName).causedBy(e);
                }
                if (attributeName.equalsIgnoreCase(ATTRIBUTE_MIME_TYPE)) {
                    output.setMimeType(attributeValue);
                } else if (attributeName.equalsIgnoreCase(ATTRIBUTE_SCHEMA)) {
                    output.setSchema(attributeValue);
                } else if (attributeName.equalsIgnoreCase(ATTRIBUTE_ENCODING)) {
                    output.setEncoding(attributeValue);

                } else {
                    throw new InvalidParameterValueException("Attribute is not supported: %s", attributeName);
                }
            }
        }
    }

    private void parseResponseDocument(String responseDocument, Execute execute,
                                       boolean status, boolean store,
                                       ProcessDescriptionType description)
            throws NoApplicableCodeException, MissingParameterValueException {
        String[] outputs = responseDocument.split(";");
        ResponseDocumentType responseDoc = execute.addNewResponseForm()
                .addNewResponseDocument();
        responseDoc.setStatus(status);
        responseDoc.setStoreExecuteResponse(store);
        for (String outputID : outputs) {
            String[] outputDataparameters = outputID.split("@");
            String outputDataInput;
            if (outputDataparameters.length > 0) {
                outputDataInput = outputDataparameters[0];
            } else {
                outputDataInput = outputID;
            }
            outputDataInput = outputDataInput.replace("=", "");
            OutputDescriptionType outputDesc = XMLBeansHelper
                    .findOutputByID(outputDataInput, description.getProcessOutputs()
                            .getOutputArray());
            if (outputDesc == null) {
                throw new MissingParameterValueException("Data output Identifier not supported: %s", outputDataInput);
            }
            DocumentOutputDefinitionType output = responseDoc
                    .addNewOutput();
            output.addNewIdentifier().setStringValue(outputDataInput);

            for (int i = 1; i < outputDataparameters.length; i++) {
                int attributePos = outputDataparameters[i].indexOf('=');
                if (attributePos == -1
                        || attributePos + 1 >= outputDataparameters[i]
                                .length()) {
                    continue;
                }
                String attributeName = outputDataparameters[i].substring(0,
                                                                             attributePos);
                String attributeValue = outputDataparameters[i]
                        .substring(attributePos + 1);
                try{
                    attributeValue = URLDecoder.decode(attributeValue, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new NoApplicableCodeException("Something went wrong while trying to decode value of %s", attributeName).causedBy(e);
                }
                if (attributeName.equalsIgnoreCase(ATTRIBUTE_MIME_TYPE)) {
                    output.setMimeType(attributeValue);
                } else if (attributeName.equalsIgnoreCase(ATTRIBUTE_SCHEMA)) {
                    output.setSchema(attributeValue);
                } else if (attributeName.equalsIgnoreCase(ATTRIBUTE_ENCODING)) {
                    output.setEncoding(attributeValue);

                }
            }
        }
    }

    private boolean parseStatus() throws ExceptionReport {
        // retrieve status
        boolean status = false;
        String statusString = Request.getMapValue("status",ciMap, false);
        if (statusString != null) {
            status = Boolean.parseBoolean(statusString);
        }
        return status;
    }

    private boolean parseStoreExecuteResponse()
            throws ExceptionReport {
        boolean store = false;
        String storeString = Request.getMapValue("storeExecuteResponse",ciMap, false);
        if (storeString != null) {
            store = Boolean.parseBoolean(storeString);
        }
        return store;
    }

    private void parseInputs(ProcessDescriptionType description)
            throws NumberFormatException, ExceptionReport {
        DataInputsType dataInputs = this.execDom.getExecute().addNewDataInputs();
        String dataInputString = Request.getMapValue(DATA_INPUTS_PARAMETER, ciMap, true);
		dataInputString = dataInputString.replace("&amp;","&");
		String[] inputs = dataInputString.split(";");
        for (String inputString : inputs) {
            parseInput(inputString, description, dataInputs);

        }
    }

    private void parseInput(String inputString,
                            ProcessDescriptionType description,
                            DataInputsType dataInputs)
            throws MissingParameterValueException, ExceptionReport,
                   NumberFormatException {
        int position = inputString.indexOf('=');
        if (position == -1) {
            throw new MissingParameterValueException("No \"=\" supplied for attribute: %s", inputString);
        }
        //get name
        String key = inputString.substring(0, position);
        String value = null;

        if (key.length() + 1 < inputString.length()) {
            int valueDelimiter = inputString.indexOf('@');
            if (valueDelimiter != -1 && position + 1 < valueDelimiter) {
                value = inputString.substring(position + 1, valueDelimiter);
            } else {
                value = inputString.substring(position + 1);
            }
        }
        
        InputDescriptionType inputDesc = XMLBeansHelper.findInputByID(key, description.getDataInputs());
        if (inputDesc == null) {
            throw new MissingParameterValueException("Data Identifier not supported: %s", key);
        }
        if (value == null) {
            throw new MissingParameterValueException("No value provided for literal: %s", key);
        }
        InputType input = dataInputs.addNewInput();
        input.addNewIdentifier().setStringValue(key);
        // prepare attributes
        String encodingAttribute = null;
        String mimeTypeAttribute = null;
        String schemaAttribute = null;
        String hrefAttribute = null;
        String uom = null;
        String dataType = null;
        String[] inputItemstemp = inputString.split("@");
        String[] inputItems;
        if (inputItemstemp.length == 2) {
            inputItems = inputItemstemp[1].split("@");
        } else {
            inputItems = inputString.split("@");
        }
        if (inputItemstemp.length > 1) {
            for (String inputItem : inputItems) {
                int attributePos = inputItem.indexOf('=');
                if (attributePos == -1 ||
                        attributePos + 1 >= inputItem.length()) {
                    continue;
                }
                String attributeName = inputItem.substring(0, attributePos);
                String attributeValue = inputItem.substring(attributePos + 1);
                //attribute is input name
                if(attributeName.equals(key)){
                    continue;
                }
                try {
                    attributeValue = URLDecoder.decode(attributeValue, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new NoApplicableCodeException("Something went wrong while trying to decode value of %s", attributeName).causedBy(e);
                }
                if (attributeName.equalsIgnoreCase(ATTRIBUTE_ENCODING)) {
                    encodingAttribute = attributeValue;
                } else if (attributeName.equalsIgnoreCase(ATTRIBUTE_MIME_TYPE)) {
                    mimeTypeAttribute = attributeValue;
                } else if (attributeName.equalsIgnoreCase(ATTRIBUTE_SCHEMA)) {
                    schemaAttribute = attributeValue;
                } else if (attributeName.equalsIgnoreCase(ATTRIBUTE_HREF) | attributeName.equalsIgnoreCase(ATTRIBUTE_XLINK_HREF)) {
                    hrefAttribute = attributeValue;
                } else if (attributeName.equalsIgnoreCase(ATTRIBUTE_UOM)) {
                    uom = attributeValue;
                } else if (attributeName.equalsIgnoreCase(ATTRIBUTE_DATATYPE)) {
                    dataType = attributeValue;
                } else {
                    throw new InvalidParameterValueException("Attribute is not supported: %s", attributeName);
                }
            }
        }

        
        if (inputDesc.isSetComplexData()) {
            Format format = new Format(mimeTypeAttribute, encodingAttribute, schemaAttribute);
            // TODO: check for different attributes
            // handling ComplexReference
            if (hrefAttribute != null && !hrefAttribute.isEmpty()) {
                InputReferenceType reference = input.addNewReference();
                reference.setHref(hrefAttribute);
                format.encodeTo(reference);
            }
            // Handling ComplexData
            else {
                ComplexDataType data = input.addNewData().addNewComplexData();

                InputStream stream = new ByteArrayInputStream(value.getBytes());

                try {
                    data.set(XmlObject.Factory.parse(stream));
                } catch (XmlException | IOException e) {
                    LOGGER.warn("Could not parse value: {} as XMLObject. Trying to create text node.", value);
                    try {
                        Node textNode = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createTextNode(value);
                        data.set(XmlObject.Factory.parse(textNode));
                    } catch (ParserConfigurationException | XmlException e1) {
                        throw new NoApplicableCodeException("Exception while trying to parse value: %s", value).causedBy(e);
                    }
                }
                format.encodeTo(data);
            }

        } else if (inputDesc.isSetLiteralData()) {
            LiteralDataType data = input.addNewData().addNewLiteralData();
            data.setStringValue(value);
            if(uom != null){
                data.setUom(uom);
            }
            if(dataType != null){
                data.setDataType(dataType);
            }
        } else if (inputDesc.isSetBoundingBoxData()) {
            parseBoundingBox(inputDesc.getIdentifier().getStringValue(),
                             value, input.addNewData().addNewBoundingBoxData());
        }
    }

    private boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void parseBoundingBox(String id,
                                  String value,
                                  BoundingBoxType data)
            throws NumberFormatException, InvalidParameterValueException {
        final String[] values = value.split(",");
        if (values.length < 4) {
            throw new InvalidParameterValueException("Invalid number of bbox values %s", id);
        }

        String crs = null;
        int dim = -1;
        int len = values.length;
        if (values.length > 4) {
            if (!isNumber(values[values.length - 1])) {
                crs = values[values.length - 1];
                len = values.length - 1;
            } else if (values.length > 5 && !isNumber(values[values.length - 2])) {
                dim = Integer.parseInt(values[values.length - 1]);
                crs = values[values.length - 2];
                len = values.length - 2;
            }
        }

        if ((len & 1) == 0) {
            throw new InvalidParameterValueException("Invalid number of bbox values %s", id);
        }
        if (dim < 0) {
            dim = len >> 1;
        } else if (dim != (len >> 1)) {
            throw new InvalidParameterValueException("Invalid dimension of bbox %s", id);
        }

        if (crs != null) {
            data.setCrs(crs);
        }
        List<String> list = Arrays.asList(values);
        data.setLowerCorner(list.subList(0,   dim));
        data.setUpperCorner(list.subList(dim, len));
        data.setDimensions(BigInteger.valueOf((long) dim));
    }

    private ProcessDescriptionType getProcessDescription(OwsCodeType processID) {
        return RepositoryManager.getInstance().getProcessDescription(processID.getValue());
    }

    private OwsCodeType parseProcessIdentifier()
            throws ExceptionReport {
        String id = Request.getMapValue(WPSConstants.PARAMETER_IDENTIFIER, ciMap, true);
        if (!RepositoryManager.getInstance().containsAlgorithm(id)) {
            throw new InvalidParameterValueException("Process %s does not exist", id);
        }
        return new OwsCodeType(id);
    }

    private String parseVersion() throws ExceptionReport {
        String version = getMapValue(WPSConstants.PARAMETER_VERSION, ciMap, true);
        if (!version.equals(WPSConstants.WPS_SERVICE_VERSION)) {
            throw new VersionNegotiationFailedException("request version is not supported: %s", version);
        }
        return version;
    }
}
