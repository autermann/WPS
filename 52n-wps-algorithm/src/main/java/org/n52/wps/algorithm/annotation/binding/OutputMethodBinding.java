package org.n52.wps.algorithm.annotation.binding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.n52.wps.algorithm.annotation.AlgorithmAnnotationException;
import org.n52.wps.io.data.IData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class OutputMethodBinding extends OutputBinding<Method> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutputMethodBinding.class);
    public OutputMethodBinding(Method method) {
        super(method);
    }

    @Override
    public Type getMemberType() {
        return getMember().getGenericReturnType();
    }

    @Override
    public void validate() throws AlgorithmAnnotationException {
        Method method = getMember();
        if (method.getParameterTypes().length != 0) {
            throw new AlgorithmAnnotationException("Method %s with output annotation can't be used, parameter count != 0", getMember());
        }
        if (!checkModifier()) {
            throw new AlgorithmAnnotationException("Method %s with output annotation can't be used, not public", getMember());
        }
        if (!checkType()) {
            throw new AlgorithmAnnotationException("Method %s with output annotation can't be used, unable to safely construct binding using method return type", getMember());
        }
    }

    @Override
    public IData get(Object annotatedInstance) {
        Object value;
        try {
            value = getMember().invoke(annotatedInstance);
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            throw new RuntimeException("Internal error processing inputs", ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new RuntimeException(cause.getMessage(), cause);
        }
        return value == null ? null : bindOutputValue(value);
    }

}
