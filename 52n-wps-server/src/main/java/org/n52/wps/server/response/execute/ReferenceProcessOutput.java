package org.n52.wps.server.response.execute;


import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

import net.opengis.wps.x100.OutputDataType;
import net.opengis.wps.x100.OutputReferenceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.wps.ServerDocument.Server;
import org.n52.wps.commons.Format;
import org.n52.wps.commons.OwsCodeType;
import org.n52.wps.commons.OwsLanguageString;
import org.n52.wps.commons.WPSConfig;
import org.n52.wps.io.data.IComplexData;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.server.NoApplicableCodeException;
import org.n52.wps.server.RetrieveResultServlet;
import org.n52.wps.server.database.DatabaseFactory;


/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class ReferenceProcessOutput extends ComplexProcessOutput {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceProcessOutput.class);
    
    public ReferenceProcessOutput(OwsCodeType identifier, 
                                  OwsLanguageString title,
                                  OwsLanguageString abstrakt,
                                  IComplexData payload, Format format) {
        super(identifier, title, abstrakt, payload, format);
    }

    @Override
    protected void encodeData(OutputDataType xbOutput) throws ExceptionReport {
        String id = store();
        OutputReferenceType xbReference = xbOutput.addNewReference();
        getFormat().encodeTo(xbReference);
		xbReference.setHref(createURL(id));
    }

    private String store() throws ExceptionReport {
        try (InputStream stream = getGenerator().generate(getPayload(), getFormat())){
            String id = createID();
            String mimeType = getFormat().getMimeType().orNull();
            DatabaseFactory.getDatabase().storeRawData(id, stream, mimeType);
            return id;
        } catch (IOException e){
            LOGGER.error(e.getMessage(), e);
            throw new NoApplicableCodeException("Error while generating Complex Data out of the process result").causedBy(e);
        }
    }

    private String createID() {
//        return getExecutionId().toString() + "-" +
//               (getIdentifier().getCodeSpace().isPresent()
//                ? getIdentifier().getCodeSpace().get() + ":" : "") +
//               getIdentifier().getValue();
        return UUID.randomUUID().toString();
    }

    private String createURL(String id) {
        Server server = WPSConfig.getInstance().getWPSConfig().getServer();
        return String.format("http://%s:%s/%s?id=%s",
                             server.getHostname(), server.getHostport(),
                             RetrieveResultServlet.SERVLET_PATH, 
                             encodeURLParam(id));
    }

    private String encodeURLParam(String id) {
        try {
            return URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error("Error URL encoding string " + id, ex);
            return id;
        }
    }

}
