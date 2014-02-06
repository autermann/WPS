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
package org.n52.wps.algorithm.annotation.binding;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import org.n52.wps.algorithm.annotation.AlgorithmAnnotationException;

/**
 *
 * @author tkunicki
 * @param <M> the binding type
 */
public abstract class AnnotationBinding<M extends AccessibleObject & Member> {

    // for example, a type reprecenting the <? extends Object> for types of List<? extends Object> or List
    public static final Type NOT_PARAMETERIZED_TYPE = new WildcardType() {
        @Override
        public Type[] getUpperBounds() {
            return new Type[] { Object.class };
        }

        @Override
        public Type[] getLowerBounds() {
            return new Type[0];
        }
    };

    private final M member;

    public AnnotationBinding(M member) {
        this.member = member;
    }

    public M getMember() {
        return member;
    }

    protected boolean checkModifier() {
        return (getMember().getModifiers() & Modifier.PUBLIC) != 0;
    }

    public abstract void validate() throws AlgorithmAnnotationException;
}
