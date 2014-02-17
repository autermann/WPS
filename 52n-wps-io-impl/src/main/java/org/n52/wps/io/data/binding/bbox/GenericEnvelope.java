package org.n52.wps.io.data.binding.bbox;

import static com.google.common.base.Preconditions.checkArgument;

import org.n52.wps.io.data.IBBOXData;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class GenericEnvelope implements IBBOXData {
    private final double[] lowerCorner;
    private final double[] upperCorner;
    private final int dimensions;
    private final String crs;

    public GenericEnvelope(double[] lowerCorner, double[] upperCorner, String crs) {
        checkArgument(lowerCorner.length == upperCorner.length);
        this.lowerCorner = lowerCorner;
        this.upperCorner = upperCorner;
        this.dimensions = lowerCorner.length;
        this.crs = crs;
    }

    @Override
    public String getCRS() {
        return crs;
    }

    @Override
    public int getDimension() {
        return dimensions;
    }

    @Override
    public double[] getLowerCorner() {
        return lowerCorner;
    }

    @Override
    public double[] getUpperCorner() {
        return upperCorner;
    }

    @Override
    public GenericEnvelope getPayload() {
        return this;
    }

    @Override
    public Class<GenericEnvelope> getSupportedClass() {
        return GenericEnvelope.class;
    }
}
