package org.qbicc.graph;

import java.util.HashSet;
import java.util.Set;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.ZeroInitializerLiteral;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.Element;
import org.qbicc.type.definition.element.ExecutableElement;
import io.smallrye.common.constraint.Assert;

public final class PhiValue extends AbstractValue implements PinnedNode {
    private final ValueType type;
    private final BlockLabel blockLabel;

    PhiValue(final Node callSite, final ExecutableElement element, final int line, final int bci, final ValueType type, final BlockLabel blockLabel) {
        super(callSite, element, line, bci);
        this.type = type;
        this.blockLabel = blockLabel;
    }

    public Value getValueForInput(final Terminator input) {
        return ((AbstractTerminator) Assert.checkNotNullParam("input", input)).getOutboundValue(this);
    }

    public void setValueForTerminator(final CompilationContext ctxt, final Element element, final Terminator input, Value value) {
        Assert.checkNotNullParam("value", value);
        ValueType expected = getType();
        ValueType actual = value.getType();
        if (! expected.isImplicitlyConvertibleFrom(actual)) {
            if (value instanceof ZeroInitializerLiteral) {
                value = ctxt.getLiteralFactory().zeroInitializerLiteralOfType(expected);
            } else {
                ctxt.warning(element, this, "Invalid input value for phi: expected %s, got %s", expected, actual);
            }
        }
        if (! ((AbstractTerminator) input).registerValue(this, value)) {
            ctxt.error(element, this, "Phi already has a value for block %s", input.getTerminatedBlock());
            return;
        }
    }

    public void setValueForBlock(final CompilationContext ctxt, final Element element, final BasicBlock input, final Value value) {
        setValueForTerminator(ctxt, element, input.getTerminator(), value);
    }

    public void setValueForBlock(final CompilationContext ctxt, final Element element, final BlockLabel input, final Value value) {
        setValueForBlock(ctxt, element, BlockLabel.getTargetOf(input), value);
    }

    /**
     * Get all of the possible non-phi values for this phi.
     *
     * @return the set of possible values (not {@code null})
     */
    public Set<Value> getPossibleValues() {
        HashSet<Value> possibleValues = new HashSet<>();
        getPossibleValues(possibleValues, new HashSet<>());
        return possibleValues;
    }

    private void getPossibleValues(Set<Value> current, Set<PhiValue> visited) {
        if (visited.add(this)) {
            BasicBlock pinnedBlock = getPinnedBlock();
            Set<BasicBlock> incoming = pinnedBlock.getIncoming();
            for (BasicBlock basicBlock : incoming) {
                if (basicBlock.isReachable()) {
                    Value value = getValueForInput(basicBlock.getTerminator());
                    if (value instanceof PhiValue) {
                        ((PhiValue) value).getPossibleValues(current, visited);
                    } else {
                        current.add(value);
                    }
                }
            }
        }
    }

    public ValueType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public BlockLabel getPinnedBlockLabel() {
        return blockLabel;
    }

    int calcHashCode() {
        // every phi is globally unique
        return System.identityHashCode(this);
    }

    public boolean equals(final Object other) {
        // every phi is globally unique
        return this == other;
    }
}