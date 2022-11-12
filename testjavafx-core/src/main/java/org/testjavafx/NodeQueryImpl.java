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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.scene.text.Text;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.testjavafx.node.NodeQuery;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A NodeQueryImpl is a (re-)runnable query to get a list of nodes.  It will
 * be (re-)run at some future time on the FX thread.  Filtering and subqueries
 * are done by composing these future computations.  Nothing is actually run
 * until {@link #query()} or {@link #queryWithRetry()} or {@link #queryAll()} are
 * called.
 */
// package-visible
final class NodeQueryImpl implements NodeQuery
{
    /**
     * Gets the roots (really, the results) of the current query.
     * Will only be run on FX thread.
     */
    private final Supplier<ImmutableList<Node>> allRoots;

    NodeQueryImpl(Supplier<ImmutableList<Node>> allRoots)
    {
        this.allRoots = allRoots;
    }

    @Override
    public NodeQuery lookup(String query)
    {
        if (query != null && (query.startsWith(".") || query.startsWith("#")))
            return new NodeQueryImpl(() -> allRoots.get().stream().flatMap(n -> n.lookupAll(query).stream()).collect(ImmutableList.toImmutableList()));
        else
            return lookup(n -> {
                if (n instanceof Labeled)
                    return Objects.equals(((Labeled)n).getText(), query);
                else if (n instanceof TextInputControl)
                    return Objects.equals(((TextInputControl)n).getText(), query);
                else if (n instanceof Text)
                    return Objects.equals(((Text)n).getText(), query);
                else
                    return false;
            });
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

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node> @Nullable T query()
    {
        return FxThreadUtils.<@Nullable T>syncFx(() -> {
            ImmutableList<Node> roots = allRoots.get();
            return roots.isEmpty() ? null : (T)roots.get(0);
        });
    }

    @SuppressWarnings("unchecked")
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

    @Override
    public <T extends Node> Optional<T> tryQuery()
    {
        return Optional.ofNullable(query());
    }

    @Override
    public <T extends Node> @Nullable T queryWithRetry()
    {
        return FxRobot.<T>implRetryUntilPresent(this::tryQuery).orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Node> NodeQuery match(Predicate<T> nodePredicate)
    {
        return filter((Node n) -> nodePredicate.test((T)n));
    }
}
