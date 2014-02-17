package org.n52.wps.commons;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

import net.opengis.ows.x11.CodeType;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class OwsCodeType {

    private final String codeSpace;
    private final String value;

    public OwsCodeType(String codeSpace, String value) {
        this.codeSpace = emptyToNull(codeSpace);
        this.value = checkNotNull(emptyToNull(value));
    }

    public OwsCodeType(String value) {
        this(null, value);
    }

    public Optional<String> getCodeSpace() {
        return Optional.fromNullable(this.codeSpace);
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.codeSpace, this.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OwsCodeType that = (OwsCodeType) obj;
        return Objects.equal(this.getValue(), that.getValue()) &&
               Objects.equal(this.getCodeSpace(), that.getCodeSpace());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues()
                .add("codeSpace", getCodeSpace().orNull())
                .add("value", getValue())
                .toString();
    }

    public void encodeTo(CodeType xbCodeType) {
        xbCodeType.setStringValue(getValue());
        if (getCodeSpace().isPresent()) {
            xbCodeType.setCodeSpace(getCodeSpace().get());
        }
    }

    public static OwsCodeType of(CodeType xbCodeType) {
        return new OwsCodeType(xbCodeType.getCodeSpace(), xbCodeType.getStringValue());
    }

}
