package org.n52.wps.algorithm.annotation.parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;

import org.n52.wps.algorithm.annotation.binding.InputBinding;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public abstract class InputAnnotationParser<A extends Annotation, M extends AccessibleObject & Member, B extends InputBinding<M>> extends DataAnnotationParser<A, M, B> {
}
