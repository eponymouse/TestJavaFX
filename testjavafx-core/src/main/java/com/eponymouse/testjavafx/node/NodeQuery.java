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
package com.eponymouse.testjavafx.node;

import javafx.application.Platform;
import javafx.scene.Node;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A NodeQuery represents a search for a node (or set of nodes).  It
 * is created using methods in {@link com.eponymouse.testjavafx.FxRobot}.
 * 
 * It represents a potential search; nothing is actually executed until
 * the query methods are run.  This allows for thread-safety (all queries are
 * ultimately run on the FX thread) and for the {@link #queryWithRetry()} method
 * that retries if the node is not found.
 */
public interface NodeQuery
{
    /**
     * Finds the first instance of a Node that matches this query, unsafely
     * casts it to type T and returns it.
     * 
     * This method can be run on the FX thread or off it.  If run off the FX thread
     * it blocks waiting to run the query on the FX thread.
     * 
     * @return The first Node found or null if no nodes are found.
     * @param <T> The type to cast the result to (use Node if you don't want this cast)
     */
    public <T extends Node> T query();

    /**
     * Finds all the instances of Node that match this query, unsafely
     * cast them to type T and returns them.  
     * 
     * This method can be run on the FX thread or off it.  If run off the FX thread
     * it blocks waiting to run the query on the FX thread.
     * 
     * @return A set (using identity, not hashCode()) of all the found nodes.
     * @param <T> The type to cast the result to (use Node if you don't want this cast)
     */
    public <T extends Node> Set<T> queryAll();

    /**
     * Searches for all nodes matching the given query within the nodes already
     * found by this search.  Note that this descends all children to find results
     * anywhere in the tree.  If you want to just filter the results without this
     * descent, use filter() instead.
     * 
     * A query is a CSS selector as used by {@link Node#lookup(String)} -- it can be
     * a name (without decoration), a style class (with "." prefix), an id (with "#" prefix),
     * one of those with a pseudoclass (with a ":" prefix), and a combination using
     * spaces or ">" to chain them.
     * 
     * Note that plain text cannot be used to match the text of an item.
     * 
     * @param query A query, as described above.
     * @return A new NodeQuery object representing this new search.  Note that the
     *         current object is unmodified and you must use the return value if you
     *         want to use the new search created by this method.
     */
    public NodeQuery lookup(String query);

    /**
     * Searches all nodes found and all their children to match any nodes that
     * satisfy the given predicate.  If you want to just filter the results without this
     * descent, use filter() instead.
     * @param nodePredicate The predicate that nodes must match.
     * @return A new NodeQuery object representing this new search.  Note that the
     *         current object is unmodified and you must use the return value if you
     *         want to use the new search created by this method. 
     */
    public NodeQuery lookup(Predicate<Node> nodePredicate);

    /**
     * Identical to {@link #query()} but missing results are returned as Optional.empty()
     * rather than null.  Implemented as Optional.ofNullable(query())
     */
    public default <T extends Node> Optional<T> tryQuery()
    {
        return Optional.ofNullable(query());
    }

    /**
     * Like {@link #query()} but if no such node is found, it is retried every 100ms
     * for 5 seconds.  Either the first found node in that period is returned,
     * or null if there was still no such node after 5 seconds.
     * 
     * This method is useful if you want to locate a node that may appear in
     * response to a GUI event that you just triggered, and you want to avoid
     * an arbitrary sleep to wait for the loading of the new GUI state.
     * 
     * Note that the method is safe to call on the FX thread, but in that case
     * it will not retry, it will instead act like calling query().
     * 
     * @return The found node, cast unsafely to T
     * @param <T> The desired/expected return type of the query.
     */
    public default <T extends Node> T queryWithRetry()
    {
        T t = query();
        if (!Platform.isFxApplicationThread())
        {
            for (int retries = 50; t == null && retries >= 0; retries--)
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                }
                t = query();
            }
        }
        return t;
    }

    /**
     * Filters the current results (without searching any further for new results)
     * to only retain those that match the given predicate.
     * @param nodePredicate Only nodes that return true from this predicate will be retained
     * @return A new NodeQuery object representing the filtered search.  Note that the
     *         current object is unmodified and you must use the return value if you
     *         want to use the new filtered search created by this method. 
     */
    public NodeQuery filter(Predicate<Node> nodePredicate);


    /**
     * Filter, named for compatibility with TestFX.
     */
    public default <T extends Node> NodeQuery match(Predicate<T> nodePredicate)
    {
        return filter((Predicate<Node>) nodePredicate);
    }
}
