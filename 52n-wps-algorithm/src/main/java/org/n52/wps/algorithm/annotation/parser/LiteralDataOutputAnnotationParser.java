package org.n52.wps.algorithm.annotation.parser;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.n52.wps.algorithm.annotation.AlgorithmAnnotationException;
import org.n52.wps.algorithm.annotation.LiteralDataOutput;
import org.n52.wps.algorithm.annotation.binding.OutputBinding;
import org.n52.wps.algorithm.annotation.binding.OutputFieldBinding;
import org.n52.wps.algorithm.annotation.binding.OutputMethodBinding;
import org.n52.wps.algorithm.descriptor.LiteralDataOutputDescriptor;
import org.n52.wps.io.BasicXMLTypeFactory;
import org.n52.wps.io.data.ILiteralData;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public abstract class LiteralDataOutputAnnotationParser<M extends AccessibleObject & Member, B extends OutputBinding<M>>
        extends OutputAnnotationParser<LiteralDataOutput, M, B> {

    public static final LiteralDataOutputAnnotationParser<Method, OutputBinding<Method>> METHOD
            = new LiteralDataOutputAnnotationParser<Method, OutputBinding<Method>>() {
                @Override
                protected OutputBinding<Method> createBinding(Method member) {
                    return new OutputMethodBinding(member);
                }
            };

    public static final LiteralDataOutputAnnotationParser<Field, OutputBinding<Field>> FIELD
            = new LiteralDataOutputAnnotationParser<Field, OutputBinding<Field>>() {
                @Override
                protected OutputBinding<Field> createBinding(Field member) {
                    return new OutputFieldBinding(member);
                }
            };
 
    @Override
    protected B parse(LiteralDataOutput annotation, M member) 
            throws AlgorithmAnnotationException {
        B annotatedBinding = createBinding(member);
        // auto generate binding if it's not explicitly declared
        Type payloadType = annotatedBinding.getPayloadType();
        Class<? extends ILiteralData> binding = annotation.binding();
        if (binding == null || ILiteralData.class.equals(binding)) {
            if (payloadType instanceof Class<?>) {
                binding = BasicXMLTypeFactory.getBindingForPayloadType((Class<?>) payloadType);
                if (binding == null) {
                    throw new AlgorithmAnnotationException("Unable to locate binding class for %s; binding not found.", payloadType);
                }
            } else {
                throw new AlgorithmAnnotationException("Unable to determine binding class for %s; type must fully resolved to use auto-binding", payloadType);
            }
        }
        LiteralDataOutputDescriptor descriptor = LiteralDataOutputDescriptor
                .builder(annotation.identifier(), binding)
                .title(annotation.title())
                .abstrakt(annotation.abstrakt())
                .build();
        annotatedBinding.setDescriptor(descriptor);
        return annotatedBinding;
    }

    @Override
    public Class<? extends LiteralDataOutput> getSupportedAnnotation() {
        return LiteralDataOutput.class;
    }
}
