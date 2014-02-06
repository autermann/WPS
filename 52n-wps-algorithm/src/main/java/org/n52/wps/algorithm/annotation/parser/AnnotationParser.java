/**
 * ﻿Copyright (C) 2007
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.wps.algorithm.annotation.parser;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;

import org.n52.wps.algorithm.annotation.AlgorithmAnnotationException;
import org.n52.wps.algorithm.annotation.binding.AnnotationBinding;

/**
 *
 * @author tkunicki
 */
public abstract class AnnotationParser<A extends Annotation, M extends AccessibleObject & Member, B extends AnnotationBinding<M>> {
    
    public B parse(M member) throws AlgorithmAnnotationException {
        A annotation = member.getAnnotation(getSupportedAnnotation());
        B binding = parse(annotation, member);
        binding.validate();
        return binding;
    }
    
    protected abstract B parse(A annotation, M member)
            throws AlgorithmAnnotationException;
    
    public abstract Class<? extends A> getSupportedAnnotation();
}
