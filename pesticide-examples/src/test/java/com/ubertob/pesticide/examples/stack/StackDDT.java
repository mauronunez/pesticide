package com.ubertob.pesticide.examples.stack;

import com.ubertob.pesticide.DomainDrivenTest;
import org.junit.jupiter.api.TestFactory;

import java.time.LocalDate;
import java.util.HashSet;


public class StackDDT extends DomainDrivenTest<StackDomain> {
    public StackDDT() {
        super(StackDomain.allProtocols());
    }

    StackUser sabine = new StackUser("Sabine");

    @TestFactory
    public void push3Numbers() {
        ddtScenario(() -> atRise(
                withoutSetting,
                play(
                        sabine.pushANumber(4),
                        sabine.pushANumber(5),
                        sabine.pushANumber(6),
                        sabine.verifyStackSizeIs(3)
                )
        ));
    }

    @TestFactory
    public void pushAndPull() {
        ddtScenario(() -> atRise(
                withoutSetting,
                play(
                        sabine.pushANumber(4),
                        sabine.pushANumber(5),
                        sabine.pushANumber(6),
                        sabine.popANumber(6),
                        sabine.popANumber(5),
                        sabine.popANumber(4),
                        sabine.verifyStackSizeIs(0)
                )
        ));
    }


    @TestFactory
    public void testWorkInProgress() {
        ddtScenario(() -> atRise(
                setting(d -> {
                    d.pushNumber(5);
                    return d;
                }),
                wip(
                        play(
                                sabine.popANumber(4)
                        ), LocalDate.of(2100, 01, 01),
                        "Impossible Stack", new HashSet<>()
                )
        ));
    }


}
