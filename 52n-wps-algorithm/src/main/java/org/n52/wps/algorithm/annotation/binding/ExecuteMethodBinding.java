package org.n52.wps.algorithm.annotation.binding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.n52.wps.algorithm.annotation.AlgorithmAnnotationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class ExecuteMethodBinding extends AnnotationBinding<Method> {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ExecuteMethodBinding.class);

    public ExecuteMethodBinding(Method method) {
        super(method);
    }

    @Override
    public void validate() throws AlgorithmAnnotationException {
        if (!checkModifier()) {
            throw new AlgorithmAnnotationException("Method %s with Execute annotation can't be used, not public.", getMember());
        }
        // eh, do we really need to care about this?
        if (!getMember().getReturnType().equals(void.class)) {
            throw new AlgorithmAnnotationException("Method {} with Execute annotation can't be used, return type not void", getMember());
        }
        if (getMember().getParameterTypes().length != 0) {
            throw new AlgorithmAnnotationException("Method {} with Execute annotation can't be used, method parameter count is > 0.", getMember());
        }
    }

    public void execute(Object annotatedInstance) {
        try {
            getMember().invoke(annotatedInstance);
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            throw new RuntimeException("Internal error executing process", ex);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            throw new RuntimeException(cause.getMessage(), cause);
        }
    }

}
