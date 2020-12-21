package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.ValueVisitor;
import cc.quarkus.qcc.type.MethodHandleType;
import cc.quarkus.qcc.type.ValueType;

/**
 * A literal representing a method handle.
 */
public final class MethodHandleLiteral extends Literal {
    private final MethodHandleType type;
    private final int referenceKind;
    private final int referenceIndex; // TODO: this should be the actual information, not the cpIndex.

    MethodHandleLiteral(MethodHandleType type, int kind, final int reference) {
        this.type = type;
        this.referenceKind = kind;
        this.referenceIndex = reference;
    }

    public boolean equals(final Literal other) {
        return other instanceof MethodHandleLiteral && equals((MethodHandleLiteral) other);
    }

    public boolean equals(final MethodHandleLiteral other) {
        return this == other || other != null && referenceKind == other.referenceKind && referenceIndex == other.referenceIndex;
    }

    public int hashCode() {
        return Integer.hashCode(referenceKind) * 19 + Integer.hashCode(referenceIndex);
    }

    public ValueType getType() {
      return this.type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}