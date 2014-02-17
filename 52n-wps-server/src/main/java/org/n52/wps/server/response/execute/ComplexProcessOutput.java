package org.n52.wps.server.response.execute;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.opengis.wps.x100.ComplexDataType;
import net.opengis.wps.x100.OutputDataType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.n52.wps.commons.Format;
import org.n52.wps.commons.OwsCodeType;
import org.n52.wps.commons.OwsLanguageString;
import org.n52.wps.io.GeneratorFactory;
import org.n52.wps.io.IGenerator;
import org.n52.wps.io.data.IComplexData;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;


/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class ComplexProcessOutput extends ProcessOutput {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexProcessOutput.class);
    private final Format format;

    public ComplexProcessOutput(OwsCodeType identifier,
                                OwsLanguageString title,
                                OwsLanguageString abstrakt,
                                IComplexData payload, Format format) {
        super(identifier, title, abstrakt, payload);
        this.format = checkNotNull(format);
    }

    public Format getFormat() {
        return this.format;
    }

    @Override
    protected void encodeData(OutputDataType xbOutput) throws ExceptionReport {
		try {
            IComplexData complexData = (IComplexData) getPayload();
            ComplexDataType xbComplexData = xbOutput.addNewData().addNewComplexData();
            try (InputStream stream = getGenerator().generate(complexData, getFormat())) {
                if (getFormat().getMimeType().get().toLowerCase().contains("xml")) {
                    xbComplexData.set(XmlObject.Factory.parse(stream));
                } else {
                    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document document = builder.newDocument();
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        IOUtils.copy(stream, baos);
                        Node dataNode = document.createTextNode(baos.toString());
                        xbComplexData.set(XmlObject.Factory.parse(dataNode));
                    }
                }
            }
            getFormat().encodeTo(xbComplexData);
		} catch(RuntimeException | IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new NoApplicableCodeException("Could not create Inline Complex Data from the process result").causedBy(e);
		} catch (XmlException e) {
			LOGGER.error(e.getMessage(), e);
			throw new NoApplicableCodeException("Could not create Inline Complex Data from the process result. Check encoding (base64 for inline binary data or UTF-8 for XML based data)").causedBy(e);
		} catch (ParserConfigurationException e) {
			LOGGER.error(e.getMessage(), e);
			throw new NoApplicableCodeException("Could not create Inline Base64 Complex Data from the process result").causedBy(e);
		}
    }

    protected IGenerator getGenerator() throws ExceptionReport {
        LOGGER.debug("Looking for matching Generator ... {}", format);
        IGenerator generator = GeneratorFactory.getInstance().getGenerator(getFormat(), getPayload().getClass());
        if (generator != null) {
            LOGGER.info("Using generator {} for format {}", generator.getClass().getName(), getFormat());
        } else {
            throw new NoApplicableCodeException("Could not find an appropriate generator for format %s and dataType %s for output",
                                                getFormat(), getPayload().getClass().getName());
        }
        return generator;
    }


}
