package cc.quarkus.qcc.plugin.lowering;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.generic.TypeSignature;

public class ThrowExceptionHelper {
    private static final AttachmentKey<ThrowExceptionHelper> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final FieldElement unwindExceptionField;
    private final MethodElement raiseExceptionMethod;

    private ThrowExceptionHelper(final CompilationContext ctxt) {
        this.ctxt = ctxt;

        /* Inject a field "unwindException" of type Unwind$_Unwind_Exception in j.l.Thread */
        FieldElement.Builder builder = FieldElement.builder();
        builder.setName("unwindException");
        ClassContext classContext = ctxt.getBootstrapClassContext();
        ClassTypeDescriptor desc = ClassTypeDescriptor.synthesize(classContext, "cc/quarkus/qcc/runtime/unwind/Unwind$struct__Unwind_Exception");
        builder.setDescriptor(desc);
        builder.setSignature(TypeSignature.synthesize(classContext, desc));
        builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_HIDDEN);
        DefinedTypeDefinition jltDefined = classContext.findDefinedType("java/lang/Thread");
        builder.setEnclosingType(jltDefined);
        FieldElement field = builder.build();
        jltDefined.validate().injectField(field);
        unwindExceptionField = field;

        /* Get the symbol to Unwind#_Unwind_RaiseException */
        DefinedTypeDefinition unwindDefined = classContext.findDefinedType("cc/quarkus/qcc/runtime/unwind/Unwind");
        ValidatedTypeDefinition unwindValidated = unwindDefined.validate();
        int index = unwindValidated.findMethodIndex(e -> e.getName().equals("_Unwind_RaiseException"));
        raiseExceptionMethod = unwindValidated.getMethod(index);
    }

    public static ThrowExceptionHelper get(CompilationContext ctxt) {
        ThrowExceptionHelper helper = ctxt.getAttachment(KEY);
        if (helper == null) {
            helper = new ThrowExceptionHelper(ctxt);
            ThrowExceptionHelper appearing = ctxt.putAttachmentIfAbsent(KEY, helper);
            if (appearing != null) {
                helper = appearing;
            }
        }
        return helper;
    }

    public FieldElement getUnwindExceptionField() {
        return this.unwindExceptionField;
    }

    public MethodElement getRaiseExceptionMethod() {
        return this.raiseExceptionMethod;
    }
}
