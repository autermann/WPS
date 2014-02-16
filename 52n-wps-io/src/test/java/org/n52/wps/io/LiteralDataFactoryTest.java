package org.n52.wps.io;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.n52.wps.io.LiteralDataFactory.getBindingForPayloadType;
import static org.n52.wps.io.LiteralDataFactory.getBindingForType;
import static org.n52.wps.io.LiteralDataFactory.getPayloadTypeForBindingType;
import static org.n52.wps.io.LiteralDataFactory.getPayloadTypeForType;
import static org.n52.wps.io.LiteralDataFactory.getTypeForPayloadType;
import static org.n52.wps.io.LiteralDataFactory.getTypeforBindingType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import org.n52.wps.io.data.binding.literal.LiteralAnyURIBinding;
import org.n52.wps.io.data.binding.literal.LiteralBase64BinaryBinding;
import org.n52.wps.io.data.binding.literal.LiteralBigDecimalBinding;
import org.n52.wps.io.data.binding.literal.LiteralBigIntegerBinding;
import org.n52.wps.io.data.binding.literal.LiteralBooleanBinding;
import org.n52.wps.io.data.binding.literal.LiteralByteBinding;
import org.n52.wps.io.data.binding.literal.LiteralDateTimeBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralFloatBinding;
import org.n52.wps.io.data.binding.literal.LiteralIntBinding;
import org.n52.wps.io.data.binding.literal.LiteralLongBinding;
import org.n52.wps.io.data.binding.literal.LiteralShortBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;

/**
 * @author Christian Autermann
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class LiteralDataFactoryTest {
    @Rule
    public final ErrorCollector errors = new ErrorCollector();

    @Test
    public void testTypeToBinding() {
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_ANY_URI), is(equalTo((Class)LiteralAnyURIBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_BASE64_BINARY), is(equalTo((Class)LiteralBase64BinaryBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_BOOLEAN), is(equalTo((Class)LiteralBooleanBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_BYTE), is(equalTo((Class)LiteralByteBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_DATE), is(equalTo((Class)LiteralDateTimeBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_DATE_TIME), is(equalTo((Class)LiteralDateTimeBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_DECIMAL), is(equalTo((Class)LiteralBigDecimalBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_DOUBLE), is(equalTo((Class)LiteralDoubleBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_FLOAT), is(equalTo((Class)LiteralFloatBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_INT), is(equalTo((Class)LiteralIntBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_INTEGER), is(equalTo((Class)LiteralBigIntegerBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_LONG), is(equalTo((Class)LiteralLongBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_SHORT), is(equalTo((Class)LiteralShortBinding.class)));
        errors.checkThat((Class) getBindingForType(LiteralDataFactory.XS_STRING), is(equalTo((Class)LiteralStringBinding.class)));
    }

    @Test
    public void testBindingToType() {
        errors.checkThat(getTypeforBindingType(LiteralAnyURIBinding.class), is(LiteralDataFactory.XS_ANY_URI));
        errors.checkThat(getTypeforBindingType(LiteralBase64BinaryBinding.class), is(LiteralDataFactory.XS_BASE64_BINARY));
        errors.checkThat(getTypeforBindingType(LiteralBooleanBinding.class), is(LiteralDataFactory.XS_BOOLEAN));
        errors.checkThat(getTypeforBindingType(LiteralByteBinding.class), is(LiteralDataFactory.XS_BYTE));
        errors.checkThat(getTypeforBindingType(LiteralDateTimeBinding.class), is(LiteralDataFactory.XS_DATE_TIME));
        errors.checkThat(getTypeforBindingType(LiteralBigDecimalBinding.class), is(LiteralDataFactory.XS_DECIMAL));
        errors.checkThat(getTypeforBindingType(LiteralDoubleBinding.class), is(LiteralDataFactory.XS_DOUBLE));
        errors.checkThat(getTypeforBindingType(LiteralFloatBinding.class), is(LiteralDataFactory.XS_FLOAT));
        errors.checkThat(getTypeforBindingType(LiteralIntBinding.class), is(LiteralDataFactory.XS_INT));
        errors.checkThat(getTypeforBindingType(LiteralBigIntegerBinding.class), is(LiteralDataFactory.XS_INTEGER));
        errors.checkThat(getTypeforBindingType(LiteralLongBinding.class), is(LiteralDataFactory.XS_LONG));
        errors.checkThat(getTypeforBindingType(LiteralShortBinding.class), is(LiteralDataFactory.XS_SHORT));
        errors.checkThat(getTypeforBindingType(LiteralStringBinding.class), is(LiteralDataFactory.XS_STRING));
    }

    @Test
    public void testPayloadToBinding() {
        errors.checkThat((Class) getBindingForPayloadType(Double.class), is(equalTo((Class) LiteralDoubleBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(double.class), is(equalTo((Class) LiteralDoubleBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(Float.class), is(equalTo((Class) LiteralFloatBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(float.class), is(equalTo((Class) LiteralFloatBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(Integer.class), is(equalTo((Class) LiteralIntBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(int.class), is(equalTo((Class) LiteralIntBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(Long.class), is(equalTo((Class) LiteralLongBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(long.class), is(equalTo((Class) LiteralLongBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(Byte.class), is(equalTo((Class) LiteralByteBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(byte.class), is(equalTo((Class) LiteralByteBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(Short.class), is(equalTo((Class) LiteralShortBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(short.class), is(equalTo((Class) LiteralShortBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(Boolean.class), is(equalTo((Class) LiteralBooleanBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(boolean.class), is(equalTo((Class) LiteralBooleanBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(BigDecimal.class), is(equalTo((Class) LiteralBigDecimalBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(BigInteger.class), is(equalTo((Class) LiteralBigIntegerBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(String.class), is(equalTo((Class) LiteralStringBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(Date.class), is(equalTo((Class) LiteralDateTimeBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(byte[].class), is(equalTo((Class) LiteralBase64BinaryBinding.class)));
        errors.checkThat((Class) getBindingForPayloadType(URI.class), is(equalTo((Class) LiteralAnyURIBinding.class)));
    }

    @Test
    public void testBindingToPayload() {
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralAnyURIBinding.class), is(equalTo((Class) URI.class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralBase64BinaryBinding.class), is(equalTo((Class) byte[].class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralBooleanBinding.class), is(equalTo((Class) Boolean.class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralByteBinding.class), is(equalTo((Class) Byte.class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralDateTimeBinding.class), is(equalTo((Class) Date.class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralBigDecimalBinding.class), is(equalTo((Class) BigDecimal.class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralDoubleBinding.class), is(equalTo((Class) Double.class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralFloatBinding.class), is(equalTo((Class) Float.class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralIntBinding.class), is(equalTo((Class) Integer.class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralBigIntegerBinding.class), is(equalTo((Class) BigInteger.class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralLongBinding.class),is(equalTo((Class) Long.class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralShortBinding.class), is(equalTo((Class) Short.class)));
        errors.checkThat((Class) getPayloadTypeForBindingType(LiteralStringBinding.class), is(equalTo((Class) String.class)));
    }

    @Test
    public void testPayloadToType() {
        errors.checkThat(getTypeForPayloadType(Double.class), is(equalTo(LiteralDataFactory.XS_DOUBLE)));
        errors.checkThat(getTypeForPayloadType(double.class), is(equalTo(LiteralDataFactory.XS_DOUBLE)));
        errors.checkThat(getTypeForPayloadType(Float.class), is(equalTo(LiteralDataFactory.XS_FLOAT)));
        errors.checkThat(getTypeForPayloadType(float.class), is(equalTo(LiteralDataFactory.XS_FLOAT)));
        errors.checkThat(getTypeForPayloadType(Integer.class), is(equalTo(LiteralDataFactory.XS_INT)));
        errors.checkThat(getTypeForPayloadType(int.class), is(equalTo(LiteralDataFactory.XS_INT)));
        errors.checkThat(getTypeForPayloadType(Long.class), is(equalTo(LiteralDataFactory.XS_LONG)));
        errors.checkThat(getTypeForPayloadType(long.class), is(equalTo(LiteralDataFactory.XS_LONG)));
        errors.checkThat(getTypeForPayloadType(Byte.class), is(equalTo(LiteralDataFactory.XS_BYTE)));
        errors.checkThat(getTypeForPayloadType(byte.class), is(equalTo(LiteralDataFactory.XS_BYTE)));
        errors.checkThat(getTypeForPayloadType(Short.class), is(equalTo(LiteralDataFactory.XS_SHORT)));
        errors.checkThat(getTypeForPayloadType(short.class), is(equalTo(LiteralDataFactory.XS_SHORT)));
        errors.checkThat(getTypeForPayloadType(Boolean.class), is(equalTo(LiteralDataFactory.XS_BOOLEAN)));
        errors.checkThat(getTypeForPayloadType(boolean.class), is(equalTo(LiteralDataFactory.XS_BOOLEAN)));
        errors.checkThat(getTypeForPayloadType(BigDecimal.class), is(equalTo(LiteralDataFactory.XS_DECIMAL)));
        errors.checkThat(getTypeForPayloadType(BigInteger.class), is(equalTo(LiteralDataFactory.XS_INTEGER)));
        errors.checkThat(getTypeForPayloadType(String.class), is(equalTo(LiteralDataFactory.XS_STRING)));
        errors.checkThat(getTypeForPayloadType(Date.class), is(equalTo(LiteralDataFactory.XS_DATE_TIME)));
        errors.checkThat(getTypeForPayloadType(byte[].class), is(equalTo(LiteralDataFactory.XS_BASE64_BINARY)));
        errors.checkThat(getTypeForPayloadType(URI.class), is(equalTo(LiteralDataFactory.XS_ANY_URI)));
    }

    @Test
    public void testTypeToPayload() {
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_ANY_URI), is(equalTo((Class)URI.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_BASE64_BINARY), is(equalTo((Class)byte[].class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_BOOLEAN), is(equalTo((Class)Boolean.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_BYTE), is(equalTo((Class)Byte.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_DATE), is(equalTo((Class)Date.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_DATE_TIME), is(equalTo((Class)Date.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_DECIMAL), is(equalTo((Class)BigDecimal.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_DOUBLE), is(equalTo((Class)Double.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_FLOAT), is(equalTo((Class)Float.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_INT), is(equalTo((Class)Integer.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_INTEGER), is(equalTo((Class)BigInteger.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_LONG), is(equalTo((Class)Long.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_SHORT), is(equalTo((Class)Short.class)));
        errors.checkThat((Class) getPayloadTypeForType(LiteralDataFactory.XS_STRING), is(equalTo((Class)String.class)));
    }
}
