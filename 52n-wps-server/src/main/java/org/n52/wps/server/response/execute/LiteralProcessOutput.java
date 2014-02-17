package org.n52.wps.server.response.execute;


import net.opengis.wps.x100.LiteralDataType;
import net.opengis.wps.x100.OutputDataType;

import org.n52.wps.commons.OwsCodeType;
import org.n52.wps.commons.OwsLanguageString;
import org.n52.wps.io.LiteralDataFactory;
import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.io.data.binding.literal.AbstractLiteralDataBinding;

import com.google.common.base.Strings;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class LiteralProcessOutput extends ProcessOutput {

    private final String type;

    public LiteralProcessOutput(OwsCodeType identifier,
                                OwsLanguageString title,
                                OwsLanguageString abstrakt,
                                ILiteralData payload, String type) {
        super(identifier, title, abstrakt, payload);
        this.type = Strings.emptyToNull(type);
    }

    public String getType() {
        return type;
    }

    @Override
    protected void encodeData(OutputDataType output) {

        ILiteralData literalData = (ILiteralData) getPayload();
        LiteralDataType xbLiteralData = output.addNewData().addNewLiteralData();

        if (type != null) {
            xbLiteralData.setDataType(getType());
        }

        String v = LiteralDataFactory.toString(getType(), literalData);
        xbLiteralData.setStringValue(v);

        if (literalData instanceof AbstractLiteralDataBinding) {
            AbstractLiteralDataBinding uomBinding
                    = (AbstractLiteralDataBinding) literalData;
            String uom = uomBinding.getUnitOfMeasurement();
            if (uom != null && !uom.isEmpty()) {
                xbLiteralData.setUom(uom);
            }
        }
    }

}
