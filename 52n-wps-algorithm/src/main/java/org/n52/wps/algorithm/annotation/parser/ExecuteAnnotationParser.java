package org.n52.wps.algorithm.annotation.parser;

import java.lang.reflect.Method;

import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.binding.ExecuteMethodBinding;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class ExecuteAnnotationParser extends AnnotationParser<Execute, Method, ExecuteMethodBinding> {

    @Override
    public ExecuteMethodBinding parse(Execute annotation, Method member) {
        return new ExecuteMethodBinding(member);
    }

    @Override
    public Class<? extends Execute> getSupportedAnnotation() {
        return Execute.class;
    }

}
