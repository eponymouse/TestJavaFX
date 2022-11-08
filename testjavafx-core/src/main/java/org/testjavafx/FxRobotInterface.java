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

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.stage.Window;

/**
 * The main interface with all the methods available to
 * call to fake GUI events.
 *
 * <p>See the subclasses:
 * <ul>
 *     <li>{@link FxRobotInterfaceKeyboard} for keyboard events</li>
 *     <li>{@link FxRobotInterfaceMouse} for mouse events</li>
 *     <li>{@link FxRobotInterfaceWindow} for window queries</li>
 *     <li>{@link FxRobotInterfaceQuery} for querying nodes in the scene graph</li>
 * </ul>
 *
 * <p>Any method (including it the superclasses) that returns
 * FxRobotInterface returns this object for easy chaining.
 */
public interface FxRobotInterface extends FxRobotInterfaceKeyboard<FxRobotInterface>, FxRobotInterfaceMouse<FxRobotInterface>, FxRobotInterfaceWindow<FxRobotInterface>, FxRobotInterfaceQuery<FxRobotInterface>
{
    /**
     * Sleep for the given number of milliseconds.
     *
     * <p>This method is safe to call on the FX thread,
     * although you probably don't want to block that
     * thread.
     *
     * @param milliseconds The number of milliseconds to sleep for.
     * @return This object for easy chaining.
     */
    public FxRobotInterface sleep(int milliseconds);

    /**
     * Gets our best guess at the current target window.  Due to
     * the implementation of JavaFX, especially with Monocle, it is
     * possible that there are multiple focused windows.  This usually
     * occurs because one window is showing child dialogs and/or popups
     * (and those might show further dialogs, etc).  We do our best to
     * traverse the window hierarchy and work out which window actually
     * has focus based on it being focused and having a focus owner.  However
     * it is possible this method might return a popup child or parent
     * dialog that is the incorrect window.  If you want more control
     * you may want to call {@link #focusedWindows()} and resolve the case
     * yourself where there are multiple focused windows.
     *
     * @return Null if no windows are focused, otherwise the best guess at the current focused window.
     */
    public Window targetWindow() throws RuntimeException;

    /**
     * Like {@link #targetWindow()} but in cases where that method
     * would return null or make a best guess, this method throws a
     * descriptive exception instead.
     *
     * @return The focused window if there is a single clear candidate.
     * @throws RuntimeException If there are multiple focused windows that cannot be distinguished, or no window has focus.
     */
    public Window targetWindowOrThrow() throws RuntimeException;


    /**
     * Gets the centre of the given Node's bounds as
     * a coordinate on the screen.
     *
     * <p>This method is safe to call on the FX thread.  If
     * called on another thread it waits to access the bounds
     * on the FX thread, and will block if the FX thread
     * is busy.
     *
     * @param node The node to fetch the centre coordinates for.
     * @return The centre of the node as screen coordinates, according
     *         to its bounds.
     */
    public Point2D point(Node node);

    /**
     * Looks up the given node that then calculates its centre on the
     * screen using its bounds.  Equivalent to calling:
     *
     * <p><code>point(lookup(query).queryWithRetry());</code>
     *
     * <p>If no such node is found (even with the retry), null is returned.
     * If multiple nodes match the query, an arbitrary node is chosen.
     *
     * @param query The query to use to find the node.
     * @return The centre (screen position) of the first found node's bounds, or null
     *         if no such node is found.
     */
    public Point2D point(String query);

}
