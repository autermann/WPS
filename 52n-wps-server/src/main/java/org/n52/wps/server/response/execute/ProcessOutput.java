package org.n52.wps.server.response.execute;

import static com.google.common.base.Preconditions.checkNotNull;


import net.opengis.wps.x100.OutputDataType;

import org.n52.wps.commons.OwsCodeType;
import org.n52.wps.commons.OwsLanguageString;
import org.n52.wps.io.data.IData;
import org.n52.wps.server.ExceptionReport;


/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public abstract class ProcessOutput {
    private final OwsLanguageString title;
    private final OwsLanguageString abstrakt;
    private final OwsCodeType identifier;
    private final IData payload;

    public ProcessOutput(OwsCodeType identifier, 
                         OwsLanguageString title,
                         OwsLanguageString abstrakt,
                         IData payload) {
        this.identifier = checkNotNull(identifier);
        this.payload = checkNotNull(payload);
        this.title = title;
        this.abstrakt = abstrakt;
    }

    public IData getPayload() {
        return payload;
    }

    public OwsCodeType getIdentifier() {
        return identifier;
    }

    public void encodeTo(OutputDataType xbOutput) throws ExceptionReport {
        getIdentifier().encodeTo(xbOutput.addNewIdentifier());
        if (title != null) {
            title.encodeTo(xbOutput.addNewTitle());
        }
        if (abstrakt != null) {
            abstrakt.encodeTo(xbOutput.addNewAbstract());
        }
        encodeData(xbOutput);
    }

    protected abstract void encodeData(OutputDataType xbOutput)
            throws ExceptionReport;

}
