package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.WordType;

/**
 *
 */
public final class Extend extends AbstractWordCastValue {
    Extend(final Value value, final WordType toType) {
        super(value, toType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}