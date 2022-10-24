package com.eponymouse.testjavafx;

import com.eponymouse.testjavafx.node.NodeQuery;
import com.google.common.collect.ImmutableList;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

class NodeQueryImpl implements NodeQuery
{
    private final ImmutableList<Node> allRoots;

    NodeQueryImpl(ImmutableList<Node> allRoots)
    {
        this.allRoots = allRoots;
    }

    @Override
    public NodeQuery lookup(String query)
    {
        return FxThreadUtils.syncFx(() -> new NodeQueryImpl(allRoots.stream().flatMap(n -> n.lookupAll(query).stream()).collect(ImmutableList.toImmutableList())));
    }

    @Override
    public NodeQuery lookup(Predicate<Node> nodePredicate)
    {
        return FxThreadUtils.syncFx(() -> new NodeQueryImpl(allRoots.stream().flatMap(n -> applyPredicateThoroughly(n, nodePredicate)).collect(ImmutableList.toImmutableList())));
    }

    private Stream<Node> applyPredicateThoroughly(Node n, Predicate<Node> nodePredicate)
    {
        Stream<Node> fromChildren;
        if (n instanceof Parent)
            fromChildren = ((Parent) n).getChildrenUnmodifiable().stream().flatMap(c -> applyPredicateThoroughly(c, nodePredicate));
        else
            fromChildren = Stream.empty();
        
        if (nodePredicate.test(n))
            return Stream.concat(Stream.of(n), fromChildren);
        else
            return fromChildren;
    }

    @Override
    public <T extends Node> T query()
    {
        return FxThreadUtils.syncFx(() -> (T)allRoots.get(0));
    }

    @Override
    public <T extends Node> List<T> queryAll()
    {
        return (List<T>)(List)allRoots;
    }
}
