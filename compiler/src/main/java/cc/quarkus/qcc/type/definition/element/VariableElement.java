package cc.quarkus.qcc.type.definition.element;

import java.util.List;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;
import cc.quarkus.qcc.type.generic.TypeSignature;
import io.smallrye.common.constraint.Assert;

/**
 * An element representing a variable of some kind.
 */
public abstract class VariableElement extends AnnotatedElement implements NamedElement {
    private final String name;
    private final TypeDescriptor typeDescriptor;
    private final TypeSignature typeSignature;
    private final TypeAnnotationList visibleTypeAnnotations;
    private final TypeAnnotationList invisibleTypeAnnotations;
    private volatile ValueType type;

    VariableElement(Builder builder) {
        super(builder);
        this.name = builder.name;
        this.typeDescriptor = Assert.checkNotNullParam("builder.typeDescriptor", builder.typeDescriptor);
        this.typeSignature = Assert.checkNotNullParam("builder.typeSignature", builder.typeSignature);
        this.visibleTypeAnnotations = builder.visibleTypeAnnotations;
        this.invisibleTypeAnnotations = builder.invisibleTypeAnnotations;
    }

    public String getName() {
        return name;
    }

    public TypeDescriptor getTypeDescriptor() {
        return typeDescriptor;
    }

    public TypeSignature getTypeSignature() {
        return typeSignature;
    }

    public TypeAnnotationList getVisibleTypeAnnotations() {
        return visibleTypeAnnotations;
    }

    public TypeAnnotationList getInvisibleTypeAnnotations() {
        return invisibleTypeAnnotations;
    }

    /**
     * Get or resolve the type of the variable.  This may cause classes to be loaded, resolved, and/or initialized
     * recursively.
     *
     * @param classContext the class context (must not be {@code null})
     * @param signatureContext the signature context (must not be {@code null})
     * @return the resolved type of the variable
     */
    public ValueType getType(ClassContext classContext, List<ParameterizedSignature> signatureContext) {
        ValueType type = this.type;
        if (type == null) {
            this.type = type = resolveTypeDescriptor(classContext, signatureContext);
        }
        return type;
    }

    ValueType resolveTypeDescriptor(ClassContext classContext, List<ParameterizedSignature> signatureContext) {
        return classContext.resolveTypeFromDescriptor(
                        getTypeDescriptor(),
                        signatureContext,
                        getTypeSignature(),
                        getVisibleTypeAnnotations(),
                        getInvisibleTypeAnnotations());
    }

    public boolean isFinal() {
        return hasAllModifiersOf(ClassFile.ACC_FINAL);
    }

    public boolean hasClass2Type() {
        return getTypeDescriptor().isClass2();
    }

    public static abstract class Builder extends AnnotatedElement.Builder implements NamedElement.Builder {
        private String name;
        private TypeDescriptor typeDescriptor;
        private TypeSignature typeSignature;
        private TypeAnnotationList visibleTypeAnnotations = TypeAnnotationList.empty();
        private TypeAnnotationList invisibleTypeAnnotations = TypeAnnotationList.empty();

        Builder() {}

        public void setName(final String name) {
            this.name = name;
        }

        public void setDescriptor(TypeDescriptor typeDescriptor) {
            this.typeDescriptor = Assert.checkNotNullParam("typeDescriptor", typeDescriptor);
        }

        public void setSignature(TypeSignature typeSignature) {
            this.typeSignature = Assert.checkNotNullParam("typeSignature", typeSignature);
        }

        public void setVisibleTypeAnnotations(TypeAnnotationList annotations) {
            this.visibleTypeAnnotations = Assert.checkNotNullParam("annotations", annotations);
        }

        public void setInvisibleTypeAnnotations(TypeAnnotationList annotations) {
            this.invisibleTypeAnnotations = Assert.checkNotNullParam("annotations", annotations);
        }

        public abstract VariableElement build();
    }
}
