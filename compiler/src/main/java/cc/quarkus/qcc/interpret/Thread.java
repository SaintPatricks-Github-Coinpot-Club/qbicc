package cc.quarkus.qcc.interpret;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.EndNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.graph.node.PhiNode;
import cc.quarkus.qcc.graph.node.RegionNode;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.ControlValue;
import cc.quarkus.qcc.graph.type.EndValue;
import cc.quarkus.qcc.graph.type.StartValue;
import cc.quarkus.qcc.graph.type.Value;

public class Thread implements Context {

    public Thread() {

    }

    public EndValue execute(Graph graph, StartValue arguments) {
        pushContext();
        set(graph.getStart(), arguments);

        //Set<Node<?>> ready = new HashSet<>();
        List<Node<?>> waiting = new ArrayList<>();
        Deque<Node<?>> worklist = new ArrayDeque<>();
        Set<Node<?>> next = new HashSet<>();
        ControlNode<?> prevControl = null;
        ControlNode<?> nextControl = graph.getStart();
        next.addAll(graph.getStart().getSuccessors());

        ControlNode<?> control = null;

        //ready.add( graph.getStart() );
        try {
            while ( ! next.isEmpty() ) {
                prevControl = control;
                control = nextControl;
                ControlNode<?> curControl = control;
                worklist.addAll(next.stream().filter(e->e.getControlPredecessors().contains(curControl)).collect(Collectors.toList()));
                next.clear();
                while (!worklist.isEmpty()) {
                    Node<?> cur = worklist.pop();
                    if (!isReady(prevControl, cur)) {
                        waiting.add(cur);
                        continue;
                    }

                    Value<?> val = getValue(prevControl, cur);
                    if (val != null) {
                        set(cur, val);
                        if ( val instanceof EndValue ) {
                            return (EndValue) val;
                        }
                        if ( cur instanceof ControlNode ) {
                            nextControl = (ControlNode<?>) cur;
                        }
                        next.addAll(cur.getSuccessors());
                        ListIterator<Node<?>> iter = waiting.listIterator();
                        while (iter.hasNext()) {
                            Node<?> each = iter.next();
                            if (isReady(prevControl, each)) {
                                //ready.add(each);
                                worklist.push(each);
                                iter.remove();
                            }
                        }
                    }
                }
            }
            //set(graph.getStart(), arguments);
            //graph.getStart().accept(this);
        } finally {
            popContext();
        }
        return null;
    }

    protected Value<?> getValue(ControlNode<?> discriminator, Node<?> node) {
        if ( node instanceof PhiNode ) {
            return ((PhiNode<?>) node).getValue(discriminator).getValue(peekContext());
        }
        return node.getValue(peekContext());
    }

    protected boolean isReady(ControlNode<?> discriminator, Node<?> node) {
        if ( node instanceof RegionNode ) {
            return isRegionReady((RegionNode) node);
        }
        if ( node instanceof PhiNode ) {
            return isPhiReady(discriminator, (PhiNode<?>) node);
        }
        Optional<Node<?>> firstMissing = node.getPredecessors().stream().filter(e -> get(e) == null).findFirst();
        return ! firstMissing.isPresent();
    }

    protected boolean isRegionReady(RegionNode node) {
        Optional<Node<?>> firstFound = node.getPredecessors().stream().filter(e -> get(e) != null).findFirst();
        return firstFound.isPresent();
    }

    protected boolean isPhiReady(ControlNode<?> discriminator, PhiNode<?> node) {
        return peekContext().get(node.getValue(discriminator)) != null;
        //return false;
    }

    protected void pushContext() {
        this.callStack.push( new ThreadContext());
    }

    protected void popContext() {
        this.callStack.pop();
    }

    protected ThreadContext peekContext() {
        return this.callStack.peek();
    }

    @Override
    public void set(Node<?> node, Value<?> value) {
        peekContext().set(node, value);
    }

    @Override
    public Value<?> get(Node<?> node) {
        return peekContext().get(node);
    }

    private Deque<ThreadContext> callStack = new ArrayDeque<>();
    //private Set<Node<?>> ready = new HashSet<>();
    //private List<Node<?>> waiting = new ArrayList<>();
}