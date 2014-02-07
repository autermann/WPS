/**
 * Copyright (C) 2006 - 2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       * Apache License, version 2.0
 *       * Apache Software License, version 1.0
 *       * GNU Lesser General Public License, version 3
 *       * Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       * Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.wps.commons;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;

import com.google.common.collect.AbstractIterator;

/**
 * A {@link Iterable}, that uses several other {@code Iterable}s to create a
 * permutation of values.
 *
 * @author Christian Autermann
 */
public abstract class PermutatingIterator<T> extends AbstractIterator<T> {
    private final Iterable[] iterables;
    private final Iterator[] iterators;
    private final Object[] values;
    private final int length;
    private boolean first = true;

    /**
     * Creates a new {@code PermutatingIterator} from the supplied
     * {@code Iterable}. There has to be at least one {@code Iterable}.
     *
     * @param iterables the {@code Iterable}s to permutate
     */
    public PermutatingIterator(Iterable... iterables) {
        checkArgument(iterables != null && iterables.length >= 1);
        this.iterables = iterables;
        this.length = this.iterables.length;
        this.iterators = new Iterator<?>[this.length];
        this.values = new Object[this.length];
    }

    @Override
    protected T computeNext() {
        if (this.first) {
            this.first = false;
            // init iterators and values
            for (int i = 0; i < this.length; ++i) {
                this.iterators[i] = this.iterables[i].iterator();
                if (!this.iterators[i].hasNext()) {
                    return endOfData();
                }
                this.values[i] = this.iterators[i].next();
            }
            return create(this.values);
        } else {
            // iterate over the from end to beginning
            for (int i = this.length - 1; i >= 0; --i) {
                // find a iterator that is able to proceed
                if (this.iterators[i].hasNext()) {
                    this.values[i] = this.iterators[i].next();
                    // reset all following iterators
                    for (int j = i + 1; j < this.length; ++j) {
                        this.iterators[j] = this.iterables[j].iterator();
                        this.values[j] = this.iterators[j].next();
                    }
                    return create(this.values);
                }
            }
        }
        return endOfData();
    }

    /**
     * Create a new object from the supplied permutation. The order of values is
     * the same as the supplied order of {@link Iterable}s.
     *
     * @param values the values of this permutation
     *
     * @return the created value
     */
    protected abstract T create(Object[] values);
}
