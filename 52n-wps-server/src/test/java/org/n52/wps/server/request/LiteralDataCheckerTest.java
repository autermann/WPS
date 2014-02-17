package org.n52.wps.server.request;

import static org.hamcrest.Matchers.is;

import net.opengis.ows.x11.AllowedValuesDocument.AllowedValues;
import net.opengis.ows.x11.RangeType;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.LiteralInputType;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import org.n52.wps.io.data.binding.literal.LiteralStringBinding;

import com.google.common.collect.Lists;

/**
 * @author Christian Autermann
 */
public class LiteralDataCheckerTest {

    @Rule
    public final ErrorCollector errors = new ErrorCollector();

    @Test
    public void testAlphabet() {
        LiteralDataChecker checker = new LiteralDataChecker(createInputDescription());
        errors.checkThat(checker.apply(new LiteralStringBinding("a")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("b")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("c")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("d")), is(false));
        errors.checkThat(checker.apply(new LiteralStringBinding("e")), is(false));
        errors.checkThat(checker.apply(new LiteralStringBinding("f")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("g")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("h")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("i")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("j")), is(false));
        errors.checkThat(checker.apply(new LiteralStringBinding("k")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("l")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("m")), is(false));
        errors.checkThat(checker.apply(new LiteralStringBinding("n")), is(false));
        errors.checkThat(checker.apply(new LiteralStringBinding("o")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("p")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("q")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("r")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("s")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("t")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("u")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("v")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("w")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("x")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("y")), is(true));
        errors.checkThat(checker.apply(new LiteralStringBinding("z")), is(true));
    }

    private InputDescriptionType createInputDescription() {
        InputDescriptionType idt = InputDescriptionType.Factory.newInstance();
        LiteralInputType lit = idt.addNewLiteralData();
        AllowedValues allowedValues = lit.addNewAllowedValues();
        allowedValues.addNewValue().setStringValue("a");
        allowedValues.addNewValue().setStringValue("b");
        allowedValues.addNewValue().setStringValue("c");
        RangeType r1 = allowedValues.addNewRange();
        r1.addNewMinimumValue().setStringValue("f");
        r1.addNewMaximumValue().setStringValue("i");
        RangeType r2 = allowedValues.addNewRange();
        r2.setRangeClosure(Lists.newArrayList("closed-open"));
        r2.addNewMinimumValue().setStringValue("k");
        r2.addNewMaximumValue().setStringValue("m");
        RangeType r3 = allowedValues.addNewRange();
        r3.addNewMinimumValue().setStringValue("o");
        return idt;
    }
}
