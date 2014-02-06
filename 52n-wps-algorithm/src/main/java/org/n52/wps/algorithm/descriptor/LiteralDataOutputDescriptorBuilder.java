package org.n52.wps.algorithm.descriptor;

import static com.google.common.base.Preconditions.checkNotNull;

import org.n52.wps.io.BasicXMLTypeFactory;
import org.n52.wps.io.data.ILiteralData;


/**
 * @author Christian Autermann
 * @param <B> the type of this builder
 */
public abstract class LiteralDataOutputDescriptorBuilder<B extends LiteralDataOutputDescriptorBuilder<B>>
        extends OutputDescriptorBuilder<B> {

    private final String dataType;

    protected LiteralDataOutputDescriptorBuilder(String identifier,
                                       Class<? extends ILiteralData> binding) {
        super(identifier, binding);
        this.dataType = checkNotNull(BasicXMLTypeFactory.getXMLDataTypeforBinding(binding),
                                     "Unable to resolve XML DataType for binding class %s", binding);
    }

    @Override
    public LiteralDataOutputDescriptor build() {
        return new LiteralDataOutputDescriptor(this);
    }

    protected String getDataType() {
        return dataType;
    }

}
