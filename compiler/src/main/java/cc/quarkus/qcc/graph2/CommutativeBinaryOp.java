package cc.quarkus.qcc.graph2;

import org.objectweb.asm.Opcodes;

/**
 *
 */
public interface CommutativeBinaryOp extends BinaryOp {
    Kind getKind();
    void setKind(Kind kind);

    enum Kind {
        ADD,
        MULTIPLY,
        AND,
        OR,
        XOR,
        CMP_EQ,
        CMP_NE,
        ;

        public static Kind fromOpcode(int opcode) {
            switch (opcode) {
                case Opcodes.LADD:
                case Opcodes.FADD:
                case Opcodes.DADD:
                case Opcodes.IADD: {
                    return ADD;
                }
                case Opcodes.IFEQ:
                case Opcodes.IF_ACMPEQ:
                case Opcodes.IF_ICMPEQ: {
                    return CMP_EQ;
                }
                case Opcodes.IFNE:
                case Opcodes.IF_ACMPNE:
                case Opcodes.IF_ICMPNE: {
                    return CMP_NE;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
        }
    }
}