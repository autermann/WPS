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

    private static ProcessIDRegistry instance = new ProcessIDRegistry();
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
