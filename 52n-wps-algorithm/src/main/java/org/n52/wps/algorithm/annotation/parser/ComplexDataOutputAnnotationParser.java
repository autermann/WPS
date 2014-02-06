package org.n52.wps.algorithm.annotation.parser;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.binding.OutputBinding;
import org.n52.wps.algorithm.annotation.binding.OutputFieldBinding;
import org.n52.wps.algorithm.annotation.binding.OutputMethodBinding;
import org.n52.wps.algorithm.descriptor.ComplexDataOutputDescriptor;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public abstract class ComplexDataOutputAnnotationParser<M extends AccessibleObject & Member, B extends OutputBinding<M>>
        extends OutputAnnotationParser<ComplexDataOutput, M, B> {

    public static final ComplexDataOutputAnnotationParser<Method, OutputBinding<Method>> METHOD
            = new ComplexDataOutputAnnotationParser<Method, OutputBinding<Method>>() {

                @Override
                protected OutputBinding<Method> createBinding(Method member) {
                    return new OutputMethodBinding(member);
                }

            };

    public static final ComplexDataOutputAnnotationParser<Field, OutputBinding<Field>> FIELD
            = new ComplexDataOutputAnnotationParser<Field, OutputBinding<Field>>() {

                @Override
                protected OutputBinding<Field> createBinding(Field member) {
                    return new OutputFieldBinding(member);
                }
            };

    @Override
    protected B parse(ComplexDataOutput annotation, M member) {
        B annotatedBinding = createBinding(member);
        ComplexDataOutputDescriptor descriptor
                = ComplexDataOutputDescriptor
                .builder(annotation.identifier(), annotation.binding())
                .title(annotation.title()).abstrakt(annotation.abstrakt())
                .build();
        annotatedBinding.setDescriptor(descriptor);
        return annotatedBinding;
    }

    @Override
    public Class<? extends ComplexDataOutput> getSupportedAnnotation() {
        return ComplexDataOutput.class;
    }
}
