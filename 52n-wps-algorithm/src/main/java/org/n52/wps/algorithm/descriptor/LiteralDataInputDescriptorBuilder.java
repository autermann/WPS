package org.n52.wps.algorithm.descriptor;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.n52.wps.io.BasicXMLTypeFactory;
import org.n52.wps.io.data.ILiteralData;

/**
 * @author Christian Autermann
 * @param <B> the type of this builder
 */
public abstract class LiteralDataInputDescriptorBuilder<B extends LiteralDataInputDescriptorBuilder<B>>
        extends InputDescriptorBuilder<B> {

    private final String dataType;
    private String defaultValue;
    private List<String> allowedValues;

    protected LiteralDataInputDescriptorBuilder(String identifier, Class<? extends ILiteralData> binding) {
        super(identifier, binding);
        this.dataType = checkNotNull(BasicXMLTypeFactory.getXMLDataTypeforBinding(binding),
                                     "Unable to resolve XML DataType for binding class %s", binding);
    }

    public B defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return self();
    }

    public <E extends Enum<E>> B allowedValues(Class<E> allowedValues) {
        Enum[] constants = allowedValues.getEnumConstants();
        List<String> names = new ArrayList<>(constants.length);
        for (Enum<?> constant : constants) {
            names.add(constant.name());
        }
        return allowedValues(names);
    }

    public B allowedValues(String[] allowedValues) {
        return allowedValues(Arrays.asList(allowedValues));
    }

    public B allowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
        return self();
    }

    @Override
    public LiteralDataInputDescriptor build() {
        return new LiteralDataInputDescriptor(this);
    }

    protected String getDataType() {
        return dataType;
    }

    protected String getDefaultValue() {
        return defaultValue;
    }

    protected List<String> getAllowedValues() {
        return allowedValues;
    }

}
