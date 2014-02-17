package org.n52.wps.commons;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

import net.opengis.ows.x11.LanguageStringType;

import com.google.common.base.Objects;
import com.google.common.base.Optional;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class OwsLanguageString {

    private final String language;
    private final String value;

    public OwsLanguageString(String language, String value) {
        this.language = emptyToNull(language);
        this.value = checkNotNull(emptyToNull(value));
    }

    public OwsLanguageString(String value) {
        this(null, value);
    }

    public Optional<String> getLanguage() {
        return Optional.fromNullable(this.language);
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.language, this.value);
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
               Objects.equal(this.getLanguage(), that.getCodeSpace());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues()
                .add("language", getLanguage().orNull())
                .add("value", getValue())
                .toString();
    }

    public void encodeTo(LanguageStringType xbLanguageString) {
        xbLanguageString.setStringValue(getValue());
        if (getLanguage().isPresent()) {
            xbLanguageString.setLang(getLanguage().get());
        }
    }

    public static OwsLanguageString of(LanguageStringType xbLanguageString) {
        return new OwsLanguageString(xbLanguageString.getLang(), xbLanguageString.getStringValue());
    }

}
