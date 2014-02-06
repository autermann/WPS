package org.n52.wps.algorithm.annotation.parser;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.n52.wps.algorithm.annotation.ComplexDataInput;
import org.n52.wps.algorithm.annotation.binding.InputBinding;
import org.n52.wps.algorithm.annotation.binding.InputFieldBinding;
import org.n52.wps.algorithm.annotation.binding.InputMethodBinding;
import org.n52.wps.algorithm.descriptor.ComplexDataInputDescriptor;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 * @param <M>
 * @param <B>
 */
public abstract class ComplexDataInputAnnotationParser<M extends AccessibleObject & Member, B extends InputBinding<M>>
        extends InputAnnotationParser<ComplexDataInput, M, B> {

    public static final ComplexDataInputAnnotationParser<Field, InputBinding<Field>> FIELD
            = new ComplexDataInputAnnotationParser<Field, InputBinding<Field>>() {

                @Override
                protected InputBinding<Field> createBinding(Field member) {
                    return new InputFieldBinding(member);
                }

            };
    public static final ComplexDataInputAnnotationParser<Method, InputBinding<Method>> METHOD
            = new ComplexDataInputAnnotationParser<Method, InputBinding<Method>>() {

                @Override
                protected InputBinding<Method> createBinding(Method member) {
                    return new InputMethodBinding(member);
                }

            };

    @Override
    protected B parse(ComplexDataInput annotation, M member) {
        B annotatedBinding = createBinding(member);
        ComplexDataInputDescriptor descriptor
                = ComplexDataInputDescriptor
                .builder(annotation.identifier(), annotation.binding())
                .title(annotation.title()).abstrakt(annotation.abstrakt())
                .minOccurs(annotation.minOccurs())
                .maxOccurs(annotation.maxOccurs())
                .maximumMegaBytes(annotation.maximumMegaBytes()).build();
        annotatedBinding.setDescriptor(descriptor);
        return annotatedBinding;
    }

    @Override
    public Class<? extends ComplexDataInput> getSupportedAnnotation() {
        return ComplexDataInput.class;
    }

}
