package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;

import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.op.Instruction;

abstract class AbstractInstruction extends AbstractMetable implements Instruction {

    AbstractInstruction() {
    }

    public Instruction meta(final String name, final Value data) {
        super.meta(name, data);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        return target;
    }
}