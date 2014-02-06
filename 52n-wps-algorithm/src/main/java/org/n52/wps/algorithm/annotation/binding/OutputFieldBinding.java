package org.n52.wps.algorithm.annotation.binding;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.n52.wps.algorithm.annotation.AlgorithmAnnotationException;
import org.n52.wps.io.data.IData;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class OutputFieldBinding extends OutputBinding<Field> {
    public OutputFieldBinding(Field field) {
        super(field);
    }

    @Override
    public Type getMemberType() {
        return getMember().getGenericType();
    }

    @Override
    public void validate() throws AlgorithmAnnotationException {
        if (!checkModifier()) {
            throw new AlgorithmAnnotationException("Field %s with output annotation can't be used, not public.", getMember());
        }
        if (!checkType()) {
            throw new AlgorithmAnnotationException("Field %s with output annotation can't be used, unable to safely construct binding using field type", getMember());
        }
    }

    @Override
    public IData get(Object annotatedInstance) {
        Object value;
        try {
            value = getMember().get(annotatedInstance);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Internal error processing inputs", ex);
        }
        return value == null ? null : bindOutputValue(value);
    }

}
