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

import javafx.application.Platform;
import javafx.scene.Node;
import org.testjavafx.node.NodeQuery;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Methods for querying the scene graph. 
 *
 * @param <T> The type of this object to be returned from all the methods.
 *            This will be {@link FxRobotInterface} if you use these methods via
 *            {@link FxRobotInterface} or {@link FxRobot}.
 */
public interface FxRobotInterfaceQuery<T extends FxRobotInterfaceQuery<T>>
{
    /**
     * Start a node query using the given query in all nodes in all showing windows.
     * See {@link NodeQuery} for more information on node queries.
     *
     * <p>This method can be called from any thread.
     *
     * @param query The query (e.g. CSS selector) to use for the query.
     * @return The NodeQuery corresponding to executing this query in the future.
     */
    public NodeQuery lookup(String query);

    /**
     * Start a node query by (at the time of executing the query) filtering
     * all nodes in all showing windows according to the given predicate.
     *
     * <p>This method can be called from any thread.  The given predicate will
     * only be run on the FX thread, and will not have been run at the time of return
     * from this function (see {@link NodeQuery} for more information on queries).
     *
     * @param nodePredicate The test to apply to all nodes, in any windows,
     *                      anywhere down the tree.  Will be run on the FX thread.
     * @return A query that will execute the given predicatee on all nodes,
     *         in any windows, anywhere down the tree.
     */
    public NodeQuery lookup(Predicate<Node> nodePredicate);

    /**
     * Starts a NodeQuery search with the given nodes as an initial result set.
     *
     * <p>So calling from(myNodes).queryAll() is equivalent to calling
     * Set.of(myNodes).  But calling from(myNodes).lookup(".wide").queryAll() will find
     * all the nodes anywhere within myNodes with the style-class wide.
     *
     * <p>This method can be called from any thread.
     *
     * @param useAsRoots The node or nodes to use as the start of the NodeQuery search.
     *                   Calling it with an empty list will give a NodeQuery with no results.
     * @return The NodeQuery with the given nodes as the initial results.
     */
    public NodeQuery from(Node... useAsRoots);

    /**
     * Waits until the given supplier returns true, by repeatedly retrying
     * every 100ms for 8 seconds.  The supplier is run on the FX thread.
     * 
     * <p>If this method is run on the FX thread, it uses a nested event
     * loop (see {@link Platform#enterNestedEventLoop(Object)}) in order
     * to let the FX thread process events while we retry from another
     * thread.  Without this, the retrying would do nothing because we'd
     * block the FX thread, preventing any changes from happening.
     *
     * <p>If the condition still does not return true after all the retries,
     * a {@link RuntimeException} (or some subclass) will be thrown.  A
     * return without exception indicates that the check did return true.
     *
     * <p>Useful suppliers include methods like {@link #showing(String)},
     *
     * @param check The check to run on the FX thread.
     * @return This, for easy chaining.
     */
    public T retryUntil(BooleanSupplier check);

    /**
     * Waits until the given supplier returns a non-null value, by repeatedly retrying
     * every 100ms for 8 seconds.
     *
     * <p>If this method is run on the FX thread, it uses a nested event
     * loop (see {@link Platform#enterNestedEventLoop(Object)}) in order
     * to let the FX thread process events while we retry from another
     * thread.  Without this, the retrying would do nothing because we'd
     * block the FX thread, preventing any changes from happening.
     *
     * <p>If the condition still does not return non-null after all the retries,
     * a {@link RuntimeException} (or some subclass) will be thrown.  A
     * return without exception will therefore be non-null.
     *
     * <p>Useful suppliers include methods like {@link NodeQuery#query()},
     * although in that particular case {@link NodeQuery#queryWithRetry()}
     * already does exactly that.
     *
     * @param check The check to run on the FX thread.
     * @param <R> The return type that will be returned by the supplier 
     *           (and then returned by this method if non-null)
     * @return This, for easy chaining.
     */
    public <R> R retryUntilNonNull(Supplier<R> check);

    /**
     * Waits until the given supplier returns a present (non-empty) Optional value,
     * by repeatedly retrying every 100ms for 8 seconds.
     *
     * <p>If this method is run on the FX thread, it uses a nested event
     * loop (see {@link Platform#enterNestedEventLoop(Object)}) in order
     * to let the FX thread process events while we retry from another
     * thread.  Without this, the retrying would do nothing because we'd
     * block the FX thread, preventing any changes from happening.
     *
     * <p>If the condition still does not return non-empty after all the retries,
     * a {@link RuntimeException} (or some subclass) will be thrown.  A
     * return without exception will therefore be non-null.
     *
     * <p>Useful suppliers include methods like {@link NodeQuery#tryQuery()},
     * although in that particular case {@link NodeQuery#queryWithRetry()}
     * already does exactly that.
     *
     * @param check The check to run on the FX thread.
     * @param <R> The return type that will be returned by the supplier in an optional 
     *            (and then returned by this method if present)
     * @return This, for easy chaining.
     */
    public <R> R retryUntilPresent(Supplier<Optional<R>> check);

    /**
     * An instant query (without retrying) for whether a node exists in a showing window.
     * Note that it does
     * not check if the node itself is visible, just whether it exists in the scene graph
     * of a showing window.
     *
     * <p>Safe for using from any thread, but off the FX thread it will block
     * until it can run on the FX thread.
     *
     * @param query The query to run via {@link #lookup(String)}
     * @return True if the query finds at least one node, false if no nodes are found.
     */
    public boolean isShowing(String query);

    /**
     * An instant query (without retrying) for whether a node is focused.
     *
     * <p>Safe for using from any thread, but off the FX thread it will block
     * until it can run on the FX thread.
     *
     * @param query The query to run via {@link #lookup(String)}
     * @return True if the query finds any nodes that are focused, false if no nodes are found or if none of the results are focused.
     */
    public boolean isFocused(String query);

    /**
     * Version of {@link #isShowing(String)}  for use with {@link #retryUntil(BooleanSupplier)}.
     * For example:
     * 
     * <code>waitUntil(showing("Cancel"))</code>
     *
     * @param query The query to run via {@link #lookup(String)}
     * @return A BooleanSupplier that will return true if the query finds at least one node, false if no nodes are found.
     */
    public BooleanSupplier showing(String query);

    /**
     * Version of {@link #isFocused(String)}  for use with {@link #retryUntil(BooleanSupplier)}.
     * For example:
     *
     * <code>waitUntil(focused("Cancel"))</code>
     *
     * @param query The query to run via {@link #lookup(String)}
     * @return A BooleanSupplier that will return true if the query finds any node that is focused, false if no nodes are found that are focused.
     */
    public BooleanSupplier focused(String query);

    /**
     * Negate the boolean supplier.  Useful in combination with methods
     * like {@link #showing(String)}, if you call:
     * 
     * <code>waitUntil(not(showing("Cancel")));</code>
     *
     * @param booleanSupplier The supplier to negate
     * @return The negated version of the passed supplier.
     */
    public BooleanSupplier not(BooleanSupplier booleanSupplier);

    /**
     * Create a {@link BooleanSupplier} that is true only if all the
     * given boolean suppliers return true.
     *
     * @param booleanSuppliers The list of suppliers to check
     * @return A new BooleanSupplier that checks that all the given suppliers return true.
     */
    public BooleanSupplier and(BooleanSupplier... booleanSuppliers);
}
