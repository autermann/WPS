package org.n52.wps.algorithm.annotation.binding;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

import org.n52.wps.algorithm.descriptor.BoundDescriptor;

import com.google.common.base.Objects;
import com.google.common.primitives.Primitives;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public abstract class DataBinding<M extends AccessibleObject & Member, D extends BoundDescriptor> extends AnnotationBinding<M> {

    private D descriptor;

    public DataBinding(M member) {
        super(member);
    }

    public void setDescriptor(D descriptor) {
        this.descriptor = descriptor;
    }

    public D getDescriptor() {
        return descriptor;
    }

    public abstract Type getMemberType();

    public Type getType() {
        return getMemberType();
    }

    public Type getPayloadType() {
        Type type = getType();
        if (isTypeEnum()) {
            return String.class;
        }
        if (type instanceof Class<?>) {
            Class<?> inputClass = (Class<?>) type;
            if (inputClass.isPrimitive()) {
                return Primitives.wrap(inputClass);
            }
        }
        return type;
    }

    public boolean isTypeEnum() {
        Type inputType = getType();
        return (inputType instanceof Class<?>) &&
               ((Class<?>) inputType).isEnum();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("member", getMember())
                .add("descriptor", this.descriptor)
                .toString();
    }
}
