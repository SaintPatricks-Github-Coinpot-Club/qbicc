package cc.quarkus.qcc.plugin.constants;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 * A basic block builder which substitutes reads from constant static fields with the constant value of the field.
 */
public class ConstantBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ConstantBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value readStaticField(final FieldElement fieldElement, final ValueType type, final JavaAccessMode mode) {
        Value constantValue = Constants.get(ctxt).getConstantValue(fieldElement);
        return constantValue == null ? getDelegate().readStaticField(fieldElement, type, mode) : constantValue;
    }
}
