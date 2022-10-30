/*
 * TestJavaFX: Testing for JavaFX applications
 * Copyright (c) Neil Brown, 2022.
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package org.testjavafx;

import org.testjavafx.node.NodeQuery;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

// package-visible
class NodeQueryImpl implements NodeQuery
{
    // Will only be run on FX thread:
    private final Supplier<ImmutableList<Node>> allRoots;

    NodeQueryImpl(Supplier<ImmutableList<Node>> allRoots)
    {
        this.allRoots = allRoots;
    }

    @Override
    public NodeQuery lookup(String query)
    {
        return new NodeQueryImpl(() -> allRoots.get().stream().flatMap(n -> n.lookupAll(query).stream()).collect(ImmutableList.toImmutableList()));
    }

    @Override
    public NodeQuery lookup(Predicate<Node> nodePredicate)
    {
        return new NodeQueryImpl(() -> allRoots.get().stream().flatMap(n -> applyPredicateThoroughly(n, nodePredicate)).collect(ImmutableList.toImmutableList()));
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
        return FxThreadUtils.syncFx(() -> {
            ImmutableList<Node> roots = allRoots.get();
            return roots.isEmpty() ? null : (T)roots.get(0);
        });
    }

    @Override
    public <T extends Node> Set<T> queryAll()
    {
        return FxThreadUtils.syncFx(() -> {
            Set<T> s = Sets.newIdentityHashSet();
            s.addAll((List) allRoots.get());
            return Collections.unmodifiableSet(s);
        });
    }

    @Override
    public NodeQuery filter(Predicate<Node> nodePredicate)
    {
        return new NodeQueryImpl(() -> allRoots.get().stream().filter(nodePredicate).collect(ImmutableList.toImmutableList()));
    }
}
