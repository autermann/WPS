package org.n52.wps.algorithm.annotation.binding;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

import org.n52.wps.algorithm.annotation.AlgorithmAnnotationException;
import org.n52.wps.io.data.IData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class InputFieldBinding extends InputBinding<Field> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InputFieldBinding.class);
    
    public InputFieldBinding(Field field) {
        super(field);
    }

    @Override
    public Type getMemberType() {
        return getMember().getGenericType();
    }

    @Override
    public void validate() throws AlgorithmAnnotationException {
        if (!checkModifier()) {
            throw new AlgorithmAnnotationException("Field %s with input annotation can't be used, not public.", getMember());
        }
        if (!(getDescriptor().getMaxOccurs().intValue() < 2 || isMemberTypeList())) {
            throw new AlgorithmAnnotationException("Field %s with input annotation can't be used, maxOccurs > 1 and field is not of type List", getMember());
        }
        if (!checkType()) {
            throw new AlgorithmAnnotationException("Field %s with input annotation can't be used, unable to safely assign field using binding payload type", getMember());
        }
    }

    @Override
    public void set(Object annotatedObject, List<IData> boundInputList) {
        try {
            getMember()
                    .set(annotatedObject, unbindInput(boundInputList));
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Internal error processing inputs", ex);
        }
    }

}
