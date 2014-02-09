package org.n52.wps.server.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.opengis.ows.x11.DomainMetadataType;
import net.opengis.ows.x11.RangeType;
import net.opengis.ows.x11.ValueType;
import net.opengis.wps.x100.InputDescriptionType;

import org.n52.wps.io.LiteralDataFactory;
import org.n52.wps.io.data.ILiteralData;
import org.n52.wps.server.ExceptionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class LiteralDataChecker implements Predicate<ILiteralData> {
    public static final Logger LOGGER = LoggerFactory
            .getLogger(LiteralDataChecker.class);
    private final InputDescriptionType input;
    private final String dataType;
    private Predicate<ILiteralData> predicate;

    public LiteralDataChecker(InputDescriptionType input) {
        this.input = input;
        this.dataType = getDataType(input);
    }

    private String getDataType(InputDescriptionType inputDesc) {
        DomainMetadataType dataTypeDefinition = inputDesc.getLiteralData()
                .getDataType();
        String dt = dataTypeDefinition != null ? dataTypeDefinition
                .getReference() : null;
        if (dt == null) {
            return LiteralDataFactory.XS_STRING;
        } else if (dt.contains("http://www.w3.org/TR/xmlschema-2#")) {
            return dt.replace("http://www.w3.org/TR/xmlschema-2#", "xs:");
        } else {
            return dt;
        }
    }

    private Predicate<ILiteralData> createPredicate() throws ExceptionReport {

        if (input.getLiteralData().isSetAnyValue()) {
            return Predicates.alwaysTrue();
        }

        if (input.getLiteralData().isSetAllowedValues()) {
            return or(createPredicate(input.getLiteralData().getAllowedValues()
                    .getValueArray()),
                      createPredicate(input.getLiteralData().getAllowedValues()
                    .getRangeArray()));
        } else {
            return Predicates.alwaysTrue();
        }
    }

    @SuppressWarnings("unchecked")
    private Predicate<ILiteralData> createPredicate(RangeType[] values) throws
            ExceptionReport {
        if (values == null || values.length == 0) {
            return Predicates.alwaysTrue();
        }

        List<Predicate<ILiteralData>> predicates
                = new ArrayList<>(values.length);

        for (RangeType range : values) {
            predicates.add(createPredicate(range));
        }
        if (predicates.isEmpty()) {
            return Predicates.alwaysTrue();
        } else {
            return or(predicates);
        }
    }

    private Predicate<ILiteralData> createPredicate(RangeType range)
            throws ExceptionReport {
        ILiteralData min = null;
        ILiteralData max = null;

        final Class<? extends ILiteralData> type = LiteralDataFactory
                .getBindingForType(dataType);

        if (!Comparable.class.isAssignableFrom(type)) {
            LOGGER.warn("Can not compare {}", type);
            return Predicates.alwaysTrue();
        }
        if (range.isSetMaximumValue()) {
            max = LiteralDataFactory.create(dataType, range.getMaximumValue()
                    .getStringValue());
        }
        if (range.isSetMinimumValue()) {
            min = LiteralDataFactory.create(dataType, range.getMinimumValue()
                    .getStringValue());
        }
        if (min == null && max == null) {
            LOGGER.warn("Invalid range without min/max", min, max);
            return Predicates.alwaysTrue();
        }
        List<?> l = range.getRangeClosure();
        String rangeClosure;
        if (l == null || l.isEmpty()) {
            rangeClosure = "closed";
        } else {
            rangeClosure = (String) l.get(0);
        }

        final BoundType upperType;
        final BoundType lowerType;
        switch (rangeClosure) {
            case "open":
                lowerType = BoundType.OPEN;
                upperType = BoundType.OPEN;
                break;
            case "open-closed":
                lowerType = BoundType.OPEN;
                upperType = BoundType.CLOSED;
                break;
            case "closed-open":
                lowerType = BoundType.CLOSED;
                upperType = BoundType.OPEN;
                break;
            case "closed":
            default:
                lowerType = BoundType.CLOSED;
                upperType = BoundType.CLOSED;
        }
        @SuppressWarnings("rawtypes")
        final Range<Comparable> r;
        if (min == null) {
            if (max == null) {
                r = Range.<Comparable>all();
            } else {
                r = Range.upTo((Comparable) max, upperType);
            }
        } else if (max == null) {
            r = Range.downTo((Comparable) min, lowerType);
        } else {
            r = Range.range((Comparable) min,lowerType,
                            (Comparable) max, upperType);
        }
        return new RangePredicate(type, r);
    }

    private Predicate<ILiteralData> createPredicate(ValueType[] values)
            throws ExceptionReport {
        if (values == null || values.length == 0) {
            return Predicates.alwaysTrue();
        }
        List<Predicate<ILiteralData>> predicates
                = new ArrayList<>(values.length);
        for (ValueType value : values) {
            final ILiteralData allowedValue = LiteralDataFactory
                    .create(dataType, value.getStringValue());
            predicates.add(new AllowedValuePredicate(allowedValue));
        }
        return or(predicates);
    }

    @Override
    public boolean apply(ILiteralData input) {
        return getPredicate().apply(input);
    }

    public Predicate<ILiteralData> getPredicate() {
        if (predicate == null) {
            try {
                predicate = createPredicate();
            } catch (ExceptionReport ex) {
                LOGGER.warn("Could not create LiteralData predicate", ex);
                return Predicates.alwaysTrue();
            }
        }
        return predicate;
    }

    @Override
    public String toString() {
        return getPredicate().toString();
    }

    @SuppressWarnings("rawtypes")
    private static class RangePredicate implements Predicate<ILiteralData> {
        private final Class<? extends ILiteralData> type;
        private final Range<Comparable> r;

        RangePredicate(Class<? extends ILiteralData> type,
                       Range<Comparable> r) {
            this.type = type;
            this.r = r;
        }

        @Override
        public boolean apply(ILiteralData input) {
            if (type.isAssignableFrom(input.getClass())) {
                return r.contains((Comparable) input);
            } else {
                LOGGER.warn("Can not compare {} with {}", type, input);
                return false;
            }
        }

        @Override
        public String toString() {
            return "is in " + r;
        }

    }

    private static class AllowedValuePredicate implements
            Predicate<ILiteralData> {
        private final ILiteralData allowedValue;

        AllowedValuePredicate(ILiteralData allowedValue) {
            this.allowedValue = allowedValue;
        }

        @Override
        public boolean apply(ILiteralData input) {
            return input.getPayload().equals(allowedValue.getPayload());
        }

        @Override
        public String toString() {
            return "is '" + allowedValue.getPayload() + "'";
        }
    }

    public static Predicate<ILiteralData> or(
            List<? extends Predicate<ILiteralData>> components) {
        return new OrPredicate(components);
    }

    public static Predicate<ILiteralData> or(Predicate<ILiteralData> p1,
                                             Predicate<ILiteralData> p2) {
        return or(Arrays.asList(p1, p2));
    }

    private static class OrPredicate implements Predicate<ILiteralData> {
        private final List<? extends Predicate<ILiteralData>> components;

        OrPredicate(List<? extends Predicate<ILiteralData>> components) {
            this.components = components;
        }

        @Override
        public boolean apply(ILiteralData input) {
            for (int i = 0; i < components.size(); i++) {
                if (components.get(i).apply(input)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return Joiner.on(" or ").join(components);
        }

    }
}
