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
package com.eponymouse.testjavafx;

import com.eponymouse.testjavafx.node.NodeQuery;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.stage.Window;

import java.util.List;
import java.util.function.Predicate;

/**
 * The main interface with all the methods available to
 * call to fake GUI events.
 * 
 * See the subclasses {@link FXRobotInterfaceKeyboard} and
 * {@link FxRobotInterfaceMouse} for documentation on keyboard
 * and mouse events respectively.
 * 
 * Any method that returns FxRobotInterface returns this
 * for easy chaining.
 */
public interface FxRobotInterface extends FXRobotInterfaceKeyboard<FxRobotInterface>, FxRobotInterfaceMouse<FxRobotInterface>
{
    /**
     * Sleep for the given number of milliseconds.
     * 
     * This method is safe to call on the FX thread,
     * although you probably don't want to block that
     * thread.
     */
    FxRobotInterface sleep(int milliseconds);

    /**
     * Gets a list of all currently showing JavaFX windows.
     * 
     * This method is safe to call on the FX thread.  If
     * called on another thread it waits to fetch the list
     * from the FX thread, and will block if the FX thread
     * is busy.
     */
    public List<Window> listWindows();

    public Window targetWindow();

    public NodeQuery lookup(String query);

    public NodeQuery lookup(Predicate<Node> nodePredicate);

    /**
     * Starts a NodeQuery search with the given nodes as an initial result set.
     * 
     * So calling from(myNodes).queryAll() is equivalent to calling
     * Set.of(myNodes).  But calling from(myNodes).lookup(".wide").queryAll() will find
     * all the nodes anywhere within myNodes with the style-class wide.
     * 
     * This method can be called from any thread.
     * 
     * @param useAsRoots The node or nodes to use as the start of the NodeQuery search.
     *                   Calling it with an empty list will give a NodeQuery with no results. 
     * @return The NodeQuery with the given nodes as the initial results.
     */
    public NodeQuery from(Node... useAsRoots);

    /**
     * Gets the centre of the given Node's bounds as
     * a coordinate on the screen.
     * 
     * This method is safe to call on the FX thread.  If
     * called on another thread it waits to access the bounds
     * on the FX thread, and will block if the FX thread
     * is busy.
     * 
     * @param node The node to fetch the centre coordinates for.
     * @return The centre of the node as screen coordinates.
     */
    public Point2D point(Node node);

    public default Point2D point(String query)
    {
        return point(lookup(query).queryWithRetry());
    }
}
