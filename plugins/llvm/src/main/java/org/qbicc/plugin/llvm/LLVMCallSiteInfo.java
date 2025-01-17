package org.qbicc.plugin.llvm;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LLVMCallSiteInfo {
    // This is a rough estimate based on number of callsites for an app with empty main().
    // A better way would be to collect this stat during compilation and use it to initialize the list capacity.
    private static final int INITIAL_LIST_SIZE = 100000;
    private final ArrayList<Node> nodeList = new ArrayList<>(INITIAL_LIST_SIZE);
    public static final AttachmentKey<LLVMCallSiteInfo> KEY = new AttachmentKey<>();

    private LLVMCallSiteInfo() {}

    public static LLVMCallSiteInfo get(CompilationContext ctxt) {
        return ctxt.computeAttachmentIfAbsent(KEY, LLVMCallSiteInfo::new);
    }

    public void mapStatepointIdToNode(int statepointId, Node node) {
        synchronized (nodeList) {
            if (statepointId >= nodeList.size()) {
                for (int i = nodeList.size(); i <= statepointId; i++) {
                    nodeList.add(i, null);
                }
            }
            nodeList.set(statepointId, node);
        }
    }

    public Node getNodeForStatepointId(int id) { return nodeList.get(id); }
}
