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
package org.n52.wps.algorithm.descriptor;

/**
 *
 * @author tkunicki
 */
public abstract class Descriptor {
    private final String identifier;
    private final String title;
    private final String abstrakt; // want 'abstract' but it's a java keyword

	protected Descriptor(DescriptorBuilder<?> builder) {
        this.identifier = builder.getIdentifier();
        this.title = builder.getTitle();
        this.abstrakt = builder.getAbstrakt();
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean hasTitle() {
        return title != null && title.length() > 0;
    }

    public String getTitle() {
        return title;
    }

    public boolean hasAbstract() {
        return abstrakt != null && abstrakt.length() > 0;
    }

    public String getAbstract() {
        return abstrakt;
    }
}
