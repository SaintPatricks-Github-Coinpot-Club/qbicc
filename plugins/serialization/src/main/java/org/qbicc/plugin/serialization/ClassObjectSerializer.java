package org.qbicc.plugin.serialization;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.object.Data;
import org.qbicc.object.Section;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayTables;
import org.qbicc.plugin.reachability.RTAInfo;
import org.qbicc.type.ArrayType;
import org.qbicc.type.Primitive;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.generic.TypeSignature;

/**
 * Constructs and emits an array of java.lang.Class references indexed by typeId.
 */
public class ClassObjectSerializer implements Consumer<CompilationContext> {
    @Override
    public void accept(CompilationContext ctxt) {
        BuildtimeHeap bth = BuildtimeHeap.get(ctxt);
        SupersDisplayTables tables = SupersDisplayTables.get(ctxt);
        RTAInfo rtaInfo = RTAInfo.get(ctxt);
        LoadedTypeDefinition jlc = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Class").load();
        Section section = ctxt.getImplicitSection(jlc);
        ReferenceType jlcRef = jlc.getType().getReference();
        ArrayType rootArrayType = ctxt.getTypeSystem().getArrayType(jlcRef, tables.get_number_of_typeids());

        // create the GlobalVariable for shared access to the Class array
        ArrayTypeDescriptor desc = ArrayTypeDescriptor.of(ctxt.getBootstrapClassContext(), jlc.getDescriptor());
        GlobalVariableElement.Builder builder = GlobalVariableElement.builder();
        builder.setName("qbicc_jlc_lookup_table");
        builder.setType(rootArrayType);
        builder.setEnclosingType(jlc);
        builder.setDescriptor(desc);
        builder.setSignature(TypeSignature.synthesize(ctxt.getBootstrapClassContext(), desc));
        GlobalVariableElement classArrayGlobal = builder.build();
        bth.setClassArrayGlobal(classArrayGlobal);

        // initialize the Class array by serializing java.lang.Class instances for all initialized types and primitive types
        Literal[] rootTable = new Literal[tables.get_number_of_typeids()];
        Arrays.fill(rootTable, ctxt.getLiteralFactory().zeroInitializerLiteralOfType(jlcRef));
        rtaInfo.visitInitializedTypes( ltd -> {
            SymbolLiteral cls = bth.serializeClassObject(ltd);
            section.declareData(null, cls.getName(), cls.getType()).setAddrspace(1);
            SymbolLiteral refToClass = ctxt.getLiteralFactory().literalOfSymbol(cls.getName(), cls.getType().getPointer().asCollected());
            rootTable[ltd.getTypeId()] = ctxt.getLiteralFactory().bitcastLiteral(refToClass, jlcRef);
        });

        Primitive.forEach(type -> {
            SymbolLiteral cls = bth.serializeClassObject(type);
            section.declareData(null, cls.getName(), cls.getType()).setAddrspace(1);
            SymbolLiteral refToClass = ctxt.getLiteralFactory().literalOfSymbol(cls.getName(), cls.getType().getPointer().asCollected());
            rootTable[type.getTypeId()] = ctxt.getLiteralFactory().bitcastLiteral(refToClass, jlcRef);
        });

        // Add the final data value for the constructed Class array
        section.addData(null, classArrayGlobal.getName(), ctxt.getLiteralFactory().literalOf(rootArrayType, List.of(rootTable)));
    }
}