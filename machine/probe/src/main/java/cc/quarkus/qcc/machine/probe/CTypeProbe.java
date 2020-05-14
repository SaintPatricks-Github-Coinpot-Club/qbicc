package cc.quarkus.qcc.machine.probe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

import cc.quarkus.qcc.machine.object.ObjectFile;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.tool.CCompiler;
import cc.quarkus.qcc.machine.tool.CompilationFailureException;
import cc.quarkus.qcc.machine.tool.CompilerInvokerBuilder;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import cc.quarkus.qcc.machine.tool.process.OutputDestination;

/**
 *
 */
public class CTypeProbe {
    private final String name;
    private final Qualifier qualifier;
    private final List<String> preproc = new ArrayList<>();
    private final Map<String, Class<?>> members = new HashMap<>();

    public CTypeProbe(final Qualifier qualifier, final String name) {
        this.qualifier = qualifier;
        this.name = name;
    }

    public void addMember(String memberName, Class<?> expectedType) {
        members.put(memberName, expectedType);
    }

    public void include(String includeStr) {
        preproc.add("#include " + includeStr);
    }

    public void define(String sym, String value) {
        preproc.add("#define " + sym + " " + value);
    }

    public void define(String sym) {
        preproc.add("#define " + sym);
    }

    public Result runProbe(CCompiler compiler, ObjectFileProvider objectFileProvider) throws IOException {
        final CompilerInvokerBuilder ib = compiler.invocationBuilder();
        StringBuilder b = new StringBuilder();
        b.append("#include <stddef.h>\n");
        for (String str : preproc) {
            b.append(str).append('\n');
        }
        UnaryOperator<StringBuilder> type;
        if (qualifier == Qualifier.STRUCT) {
            type = struct(name, UnaryOperator.identity());
        } else if (qualifier == Qualifier.UNION) {
            type = union(name, UnaryOperator.identity());
        } else {
            assert qualifier == Qualifier.NONE;
            type = literal(name);
        }
        // guard with sizeof(size_t) in a (hopefully) 64-bit holder
        final UnaryOperator<StringBuilder> size_t = literal("size_t");
        decl(literal("unsigned long long"), "size_t_size", sizeof(size_t)).apply(b);
        b.append('\n');
        final UnaryOperator<StringBuilder> _Bool = literal("_Bool");
        decl(_Bool, "is_signed", isSigned(deref(zeroPtrTo(type)))).apply(b);
        b.append('\n');
        decl(_Bool, "is_unsigned", isUnsigned(deref(zeroPtrTo(type)))).apply(b);
        b.append('\n');
        decl(_Bool, "is_floating", isFloating(deref(zeroPtrTo(type)))).apply(b);
        b.append('\n');
        decl(size_t, "overall_size", sizeof(type)).apply(b);
        decl(size_t, "overall_align", alignof(type)).apply(b);
        b.append('\n');
        for (Map.Entry<String, Class<?>> entry : members.entrySet()) {
            final String memberName = entry.getKey();
            decl(size_t, "sizeof_" + memberName, sizeof(memberOf(type, memberName))).apply(b);
            decl(size_t, "offsetof_" + memberName, offsetof(type, memberName)).apply(b);
            decl(_Bool, "is_signed_" + memberName, isSigned(memberOf(type, memberName))).apply(b);
            decl(_Bool, "is_unsigned_" + memberName, isUnsigned(memberOf(type, memberName))).apply(b);
            decl(_Bool, "is_floating_" + memberName, isFloating(memberOf(type, memberName))).apply(b);
        }
        ib.setMessageHandler(new ToolMessageHandler() {
            public void handleMessage(final Level level, final String file, final int line, final int column, final String message) {
                System.out.println(level + ": " + file + ":" + line + " -> " + message);
            }
        });
        final Path path = Files.createTempFile("qcc-probe-", "." + objectFileProvider.getObjectType().objectSuffix());
        ib.setOutputPath(path);
        OutputDestination od = ib.build();
        try {
            InputSource.from(b).transferTo(od);
        } catch (CompilationFailureException e) {
            // no result
            return null;
        }
        // read back the symbol info
        try (ObjectFile objectFile = objectFileProvider.openObjectFile(path)) {
            long size_t_size = objectFile.getSymbolValueAsLong("size_t_size");
            boolean isLong = size_t_size == 8;
            long overallSize = isLong ? objectFile.getSymbolValueAsLong("overall_size") : objectFile.getSymbolValueAsInt("overall_size");
            long overallAlign = isLong ? objectFile.getSymbolValueAsLong("overall_align") : objectFile.getSymbolValueAsInt("overall_align");
            boolean signed = objectFile.getSymbolValueAsByte("is_signed") != 0;
            boolean unsigned = objectFile.getSymbolValueAsByte("is_unsigned") != 0;
            boolean floating = objectFile.getSymbolValueAsByte("is_floating") != 0;
            Map<String, Result.Info> infos = new HashMap<>();
            for (Map.Entry<String, Class<?>> entry : members.entrySet()) {
                final String memberName = entry.getKey();
                final long memberSize = isLong ? objectFile.getSymbolValueAsLong("sizeof_" + memberName) : objectFile.getSymbolValueAsInt("sizeof_" + memberName);
                final long memberOffset = isLong ? objectFile.getSymbolValueAsLong("offsetof_" + memberName) : objectFile.getSymbolValueAsInt("offsetof_" + memberName);
                final boolean memberSigned = objectFile.getSymbolValueAsByte("is_signed_" + memberName) != 0;
                final boolean memberUnsigned = objectFile.getSymbolValueAsByte("is_unsigned_" + memberName) != 0;
                final boolean memberFloating = objectFile.getSymbolValueAsByte("is_floating_" + memberName) != 0;
                final Result.Info info = new Result.Info(memberSize, memberOffset, memberSigned, memberUnsigned, memberFloating);
                infos.put(memberName, info);
            }
            return new Result(overallSize, overallAlign, signed, unsigned, floating, infos);
        }
    }

    private UnaryOperator<StringBuilder> isFloating(final UnaryOperator<StringBuilder> expr) {
        return generic(expr, csv(
            assocList(literal("float"), literal("1")),
            assocList(literal("double"), literal("1")),
            assocList(literal("long double"), literal("1")),
            assocList(literal("default"), literal("0")))
        );
    }

    private UnaryOperator<StringBuilder> isUnsigned(final UnaryOperator<StringBuilder> expr) {
        return generic(expr, csv(
            assocList(literal("unsigned char"), literal("1")),
            assocList(literal("unsigned short"), literal("1")),
            assocList(literal("unsigned int"), literal("1")),
            assocList(literal("unsigned long"), literal("1")),
            assocList(literal("unsigned long long"), literal("1")),
            assocList(literal("default"), literal("0")))
        );
    }

    private UnaryOperator<StringBuilder> isSigned(final UnaryOperator<StringBuilder> expr) {
        return generic(expr, csv(
            assocList(literal("signed char"), literal("1")),
            assocList(literal("signed short"), literal("1")),
            assocList(literal("signed int"), literal("1")),
            assocList(literal("signed long"), literal("1")),
            assocList(literal("signed long long"), literal("1")),
            assocList(literal("default"), literal("0")))
        );
    }

    private UnaryOperator<StringBuilder> literal(String str) {
        return sb -> sb.append(str);
    }

    private UnaryOperator<StringBuilder> assocList(UnaryOperator<StringBuilder> type, UnaryOperator<StringBuilder> expr) {
        return sb -> expr.apply(type.apply(sb).append(':').append(' '));
    }

    private UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2) {
        return sb -> i2.apply(i1.apply(sb).append(','));
    }

    private UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2, UnaryOperator<StringBuilder> i3) {
        return csv(csv(i1, i2), i3);
    }

    private UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2, UnaryOperator<StringBuilder> i3, UnaryOperator<StringBuilder> i4) {
        return csv(csv(i1, i2), csv(i3, i4));
    }

    private UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2, UnaryOperator<StringBuilder> i3, UnaryOperator<StringBuilder> i4, UnaryOperator<StringBuilder> i5) {
        return csv(csv(i1, i2), csv(i3, i4), i5);
    }

    private UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2, UnaryOperator<StringBuilder> i3, UnaryOperator<StringBuilder> i4, UnaryOperator<StringBuilder> i5, UnaryOperator<StringBuilder> i6) {
        return csv(csv(i1, i2), csv(i3, i4), csv(i5, i6));
    }

    private UnaryOperator<StringBuilder> csv(UnaryOperator<StringBuilder> i1, UnaryOperator<StringBuilder> i2, UnaryOperator<StringBuilder> i3, UnaryOperator<StringBuilder> i4, UnaryOperator<StringBuilder> i5, UnaryOperator<StringBuilder> i6, UnaryOperator<StringBuilder> i7) {
        return csv(csv(i1, i2), csv(i3, i4), csv(i5, i6), i7);
    }

    private UnaryOperator<StringBuilder> generic(UnaryOperator<StringBuilder> object, UnaryOperator<StringBuilder> csv) {
        return sb -> csv.apply(object.apply(sb.append("_Generic(")).append(',')).append(')');
    }

    private UnaryOperator<StringBuilder> decl(UnaryOperator<StringBuilder> type, String name,
            UnaryOperator<StringBuilder> value) {
        return sb -> value.apply(type.apply(sb).append(' ').append(name).append(" = ")).append(";\n");
    }

    private UnaryOperator<StringBuilder> deref(UnaryOperator<StringBuilder> ptrExpr) {
        return sb -> ptrExpr.apply(sb.append('*'));
    }

    private UnaryOperator<StringBuilder> zeroPtrTo(UnaryOperator<StringBuilder> type) {
        return sb -> type.apply(sb.append("((")).append(" *) 0)");
    }

    private UnaryOperator<StringBuilder> memberOf(UnaryOperator<StringBuilder> type, String memberName) {
        return memberOfPtr(zeroPtrTo(type), memberName);
    }

    private UnaryOperator<StringBuilder> memberOfPtr(UnaryOperator<StringBuilder> ptrExpr, String memberName) {
        return sb -> ptrExpr.apply(sb).append("->").append(memberName);
    }

    private UnaryOperator<StringBuilder> union(String name, UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("union ").append(name));
    }

    private UnaryOperator<StringBuilder> struct(String name, UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("struct ").append(name));
    }

    private UnaryOperator<StringBuilder> sizeof(UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("sizeof(")).append(')');
    }

    private UnaryOperator<StringBuilder> alignof(UnaryOperator<StringBuilder> next) {
        return sb -> next.apply(sb.append("_Alignof(")).append(')');
    }

    private UnaryOperator<StringBuilder> offsetof(UnaryOperator<StringBuilder> type, String memberName) {
        return sb -> type.apply(sb.append("offsetof(")).append(',').append(memberName).append(')');
    }

    public static class Result {
        private final long size;
        private final long align;
        private final boolean signed;
        private final boolean unsigned;
        private final boolean floating;
        private final Map<String, Info> infos;

        Result(final long size, final long align, final boolean signed, final boolean unsigned, final boolean floating, final Map<String, Info> infos) {
            this.size = size;
            this.align = align;
            this.signed = signed;
            this.unsigned = unsigned;
            this.floating = floating;
            this.infos = infos;
        }

        public long getOverallSize() {
            return size;
        }

        public long getOverallAlign() {
            return align;
        }

        public long getMemberSize(String member) {
            return requireInfo(member).size;
        }

        public long getMemberOffset(String member) {
            return requireInfo(member).offset;
        }

        public boolean isSigned() {
            return signed;
        }

        public boolean isUnsigned() {
            return unsigned;
        }

        public boolean isFloating() {
            return floating;
        }

        private Info requireInfo(final String member) {
            final Info info = infos.get(member);
            if (info == null) {
                throw new NoSuchElementException(member);
            }
            return info;
        }

        static final class Info {
            private final long size;
            private final long offset;
            private final boolean signed;
            private final boolean unsigned;
            private final boolean floating;

            Info(final long size, final long offset, final boolean signed, final boolean unsigned, final boolean floating) {
                this.size = size;
                this.offset = offset;
                this.signed = signed;
                this.unsigned = unsigned;
                this.floating = floating;
            }
        }
    }

    public enum Qualifier {
        NONE,
        STRUCT,
        UNION,
        ;
    }

}