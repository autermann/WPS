package org.n52.wps.algorithm.annotation.parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;

import org.n52.wps.algorithm.annotation.binding.DataBinding;
import org.n52.wps.algorithm.descriptor.BoundDescriptor;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public abstract class DataAnnotationParser<A extends Annotation, M extends AccessibleObject & Member, B extends DataBinding<M, ? extends BoundDescriptor>>
        extends AnnotationParser<A, M, B> {

    protected abstract B createBinding(M member);
}
