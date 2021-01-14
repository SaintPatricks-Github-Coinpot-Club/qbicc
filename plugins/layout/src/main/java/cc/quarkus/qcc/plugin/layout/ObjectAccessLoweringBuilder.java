package cc.quarkus.qcc.plugin.layout;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.MemoryAccessMode;
import cc.quarkus.qcc.graph.MemoryAtomicityMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.CompoundType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.FieldElement;

/**
 *
 */
public class ObjectAccessLoweringBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public ObjectAccessLoweringBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public Value typeIdOf(final Value value) {
        return readInstanceField(value, Layout.get(ctxt).getObjectClassField(), JavaAccessMode.PLAIN);
    }

    public Value arrayLength(final Value array) {
        if (array.getType() instanceof ReferenceType) {
            return readInstanceField(array, Layout.get(ctxt).getArrayLengthField(), JavaAccessMode.PLAIN);
        }
        return super.arrayLength(array);
    }

    public Value readArrayValue(final Value array, final Value index, final JavaAccessMode mode) {
        ValueType arrayType = array.getType();
        if (arrayType instanceof ReferenceType) {
            ReferenceType arrayRefType = (ReferenceType) arrayType;
            Layout layout = Layout.get(ctxt);
            FieldElement contentField = layout.getArrayContentField(arrayRefType.getUpperBound());
            if (contentField == null) {
                // punt
                return super.readArrayValue(array, index, mode);
            }
            Layout.LayoutInfo layoutInfo = layout.getInstanceLayoutInfo(contentField.getEnclosingType());
            CompoundType.Member member = layoutInfo.getMember(contentField);
            MemoryAtomicityMode atomicityMode = mode == JavaAccessMode.VOLATILE ? MemoryAtomicityMode.ACQUIRE : MemoryAtomicityMode.UNORDERED;
            return pointerLoad(add(memberPointer(valueConvert(array, layoutInfo.getCompoundType().getPointer()), member), index), MemoryAccessMode.PLAIN, atomicityMode);
        } else {
            return super.readArrayValue(array, index, mode);
        }
    }

    public Node writeArrayValue(final Value array, final Value index, final Value value, final JavaAccessMode mode) {
        ValueType arrayType = array.getType();
        if (arrayType instanceof ReferenceType) {
            ReferenceType arrayRefType = (ReferenceType) arrayType;
            Layout layout = Layout.get(ctxt);
            FieldElement contentField = layout.getArrayContentField(arrayRefType.getUpperBound());
            if (contentField == null) {
                // punt
                return super.readArrayValue(array, index, mode);
            }
            Layout.LayoutInfo layoutInfo = layout.getInstanceLayoutInfo(contentField.getEnclosingType());
            CompoundType.Member member = layoutInfo.getMember(contentField);
            MemoryAtomicityMode atomicityMode = mode == JavaAccessMode.VOLATILE ? MemoryAtomicityMode.ACQUIRE : MemoryAtomicityMode.UNORDERED;
            return pointerStore(add(memberPointer(valueConvert(array, layoutInfo.getCompoundType().getPointer()), member), index), value, MemoryAccessMode.PLAIN, atomicityMode);
        } else {
            return super.writeArrayValue(array, index, value, mode);
        }
    }

    public Value readInstanceField(final Value instance, final FieldElement fieldElement, final JavaAccessMode mode) {
        ValueType instanceType = instance.getType();
        if (instanceType instanceof ReferenceType) {
            Layout layout = Layout.get(ctxt);
            Layout.LayoutInfo info = layout.getInstanceLayoutInfo(fieldElement.getEnclosingType());
            CompoundType.Member member = info.getMember(fieldElement);
            MemoryAtomicityMode atomicityMode = mode == JavaAccessMode.VOLATILE ? MemoryAtomicityMode.ACQUIRE : MemoryAtomicityMode.UNORDERED;
            return pointerLoad(memberPointer(valueConvert(instance, info.getCompoundType().getPointer()), member), MemoryAccessMode.PLAIN, atomicityMode);
        } else if (instanceType instanceof ClassObjectType) {
            // todo: value
            ctxt.error(getLocation(), "Value types not yet supported");
            return ctxt.getLiteralFactory().literalOfNull();
        } else {
            ctxt.error(getLocation(), "Read instance field on a non-object");
            return ctxt.getLiteralFactory().literalOfNull();
        }
    }

    public Node writeInstanceField(final Value instance, final FieldElement fieldElement, final Value value, JavaAccessMode mode) {
        ValueType instanceType = instance.getType();
        if (instanceType instanceof ReferenceType) {
            Layout layout = Layout.get(ctxt);
            Layout.LayoutInfo info = layout.getInstanceLayoutInfo(fieldElement.getEnclosingType());
            CompoundType.Member member = info.getMember(fieldElement);
            if (mode == JavaAccessMode.DETECT) {
                mode = fieldElement.isVolatile() ? JavaAccessMode.VOLATILE : JavaAccessMode.PLAIN;
            }
            MemoryAtomicityMode atomicityMode = mode == JavaAccessMode.VOLATILE ? MemoryAtomicityMode.RELEASE : MemoryAtomicityMode.UNORDERED;
            return pointerStore(memberPointer(valueConvert(instance, info.getCompoundType().getPointer()), member), value, MemoryAccessMode.PLAIN, atomicityMode);
        } else if (instanceType instanceof ClassObjectType) {
            // todo: value
            ctxt.error(getLocation(), "Value types not yet supported");
            return nop();
        } else {
            ctxt.error(getLocation(), "Write instance field on a non-object");
            return nop();
        }
    }
}