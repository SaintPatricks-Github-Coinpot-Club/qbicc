package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * The class object for a given type ID value.
 */
public final class ClassOf extends AbstractValue implements UnaryValue {
    private final Value input;
    private final ReferenceType type;

    ClassOf(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value input, final ReferenceType type) {
        super(callSite, element, line, bci);
        this.input = input;
        this.type = type;
    }

    public Value getInput() {
        return input;
    }

    public ReferenceType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(ClassOf.class, input);
    }

    public boolean equals(final Object other) {
        return other instanceof ClassOf && equals((ClassOf) other);
    }

    public boolean equals(final ClassOf other) {
        return this == other || other != null
            && input.equals(other.input);
    }
}