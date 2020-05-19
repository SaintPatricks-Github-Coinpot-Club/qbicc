package cc.quarkus.qcc.tool.llvm;

/**
 *
 */
public interface OptTool extends LlvmTool {
    default String getToolName() {
        return "LLVM Optimizer";
    }

    default String getProgramName() {
        return "opt";
    }

    OptInvoker newInvoker();
}
