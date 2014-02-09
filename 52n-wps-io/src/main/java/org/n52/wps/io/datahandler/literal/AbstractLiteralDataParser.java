package org.n52.wps.io.datahandler.literal;

import org.n52.wps.io.data.ILiteralData;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * @author Christian Autermann
 */
public abstract class AbstractLiteralDataParser implements LiteralDataParser {

    final Class<? extends ILiteralData> bindingType;
    final Class<?> payloadType;
    final String uri;

    public AbstractLiteralDataParser(
                                     Class<?> payloadType,
                                     Class<? extends ILiteralData> bindingType,
                                     String uri) {
        this.bindingType = Preconditions.checkNotNull(bindingType);
        this.payloadType = Preconditions.checkNotNull(payloadType);
        this.uri = Preconditions.checkNotNull(Strings.emptyToNull(uri));
    }

    @Override
    public Class<? extends ILiteralData> getBindingType() {
        return this.bindingType;
    }

    @Override
    public Class<?> getPayloadType() {
        return this.payloadType;
    }

    @Override
    public String getURI() {
        return this.uri;
    }

}
