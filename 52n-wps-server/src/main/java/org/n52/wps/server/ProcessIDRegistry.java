/**
 * Copyright (C) 2007 - 2014 52°North Initiative for Geospatial Open Source
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
package org.n52.wps.server;

import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.collect.Sets;

/**
 * @author Matthias Mueller, TU Dresden
 *
 */
public class ProcessIDRegistry {

    private static final ProcessIDRegistry instance = new ProcessIDRegistry();
    private final Set<String> ids = Sets.newHashSet();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock read = lock.readLock();
    private final Lock write = lock.writeLock();

    private ProcessIDRegistry() {
        //empty private constructor
    }

    public boolean add(String id) {
        write.lock();
        try {
            return ids.contains(id);
        } finally {
            write.unlock();
        }

    }

    public boolean remove(String id) {
        write.lock();
        try {
            return ids.remove(id);
        } finally {
            write.unlock();
        }

    }

    public boolean contains(String id) {
        read.lock();
        try {
            return ids.contains(id);
        } finally {
            read.unlock();
        }
    }

    public String[] getAll() {
        read.lock();
        try {
            return ids.toArray(new String[ids.size()]);
        } finally {
            read.unlock();
        }

    }

    protected void clear() {
        write.lock();
        try {
            ids.clear();
        } finally {
            write.unlock();
        }

    }

    public static ProcessIDRegistry getInstance() {
        return instance;
    }
}
