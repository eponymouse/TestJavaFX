/*
 * TestJavaFX: Testing for JavaFX applications
 * Copyright (c) TestFX contributors 2013-2019 and Neil Brown, 2022.
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

import javafx.stage.Window;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.testjavafx.node.NodeQuery;

import java.util.List;

/**
 * A set of methods for accessing windows.
 * All the methods return this for easy chaining.
 *
 * <p>Calling these methods from the FX thread is safe.
 *
 * <p>Note that currently, the window methods are slightly different than
 * TestFX.  TestFX allows explicit setting and querying of the target window,
 * which can be confusing because it conflates the idea of the focused window
 * with the idea of setting a specific target window.
 * In TestJavaFX if you want the focused windows, use {@link #focusedWindows()}
 * instead of the targetWindow() call to get the currently focused window.
 * If you want to target a specific window, use the {@link #from(Window)} call
 * to begin the query.
 *
 * <p>Note that some methods in this class are annotated @Nullable from
 * <a href="https://checkerframework.org/">The Checker Framework</a>.  If you
 * use this, you can take advantage of the annotations.  If you do not use this,
 * consider it extra documentation: anything marked with a @Nullable return may
 * return null; anything else will not.
 *
 * @param <T> The type of this object to be returned from all the methods.
 *            This will be {@link FxRobotInterface} if you use these methods via
 *            {@link FxRobotInterface} or {@link FxRobot}.
 */
public interface FxRobotInterfaceWindow<T extends FxRobotInterfaceWindow<T>>
{
    /**
     * Start a {@link NodeQuery} that will only look within the given window.
     *
     * @param window The window to restrict the NodeQuery to.
     * @return A new NodeQuery restricted to the given window.
     */
    public NodeQuery from(Window window);

    /**
     * List all the potential target windows, ordered by the most likely
     * window first.  Most likely here means that it is most closely related
     * to the currently focused window, within the window parent/child hierarchy.
     *
     * <p>This method is safe to call on the FX thread.  If called off the FX thread
     * it will block until it can perform the query on the FX thread.
     *
     * @return A list of all available windows, sorted by the closeness to
     *         {@link #focusedWindows()} within the window hierarchy.
     */
    public List<Window> listTargetWindows();

    /**
     * Gets a list of all currently showing JavaFX windows.
     *
     * <p>This is essentially a thread-safe way to call {@link Window#getWindows()}.
     *
     * <p>This method is safe to call on the FX thread.  If
     * called on another thread it waits to fetch the list
     * from the FX thread, and will block if the FX thread
     * is busy.
     *
     * @return A list of all currently showing JavaFX windows
     *         within the current application.
     */
    public List<Window> listWindows();

    /**
     * Get the currently focused windows.  The reason for the plural is that
     * if you show a popup window, JavaFX labels the parent window as focused
     * and all the popup windows as focused.  So the answer to which window
     * has focus can be none (e.g. if some other application has focus), one
     * (the most common case) or multiple (which should be related through
     * window ownership).
     * 
     * <p>There is an extra tweak for convenience.  Sometimes no window is focused, 
     * but in this case we generally want to access the most recent return of focusedWindows(),
     * so we return the values from the previous focusedWindows() call that
     * are still showing.  If none of this applies and there is only one window available
     * from {@link #listWindows()} we just return that as our best guess (but do not
     * remember it for the previous-call mechanism just mentioned).  If all else fails
     * we return the empty list.
     *
     * <p>This method is safe to call on the FX thread.  If called on another
     * thread it will block until it can its calculation on the FX thread.
     *
     * @return Our best guess at the current/recently-focused windows (see above) or
     *         empty if no such windows are available.  The return list is unmodifiable.
     */
    public List<Window> focusedWindows();

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
    public @Nullable Window targetWindow();

    /**
     * Like {@link #targetWindow()} but in cases where that method
     * would return null or make a best guess, this method throws a
     * descriptive exception instead.
     *
     * @return The focused window if there is a single clear candidate.
     * @throws RuntimeException If there are multiple focused windows that cannot be distinguished, or no window has focus.
     */
    public Window targetWindowOrThrow() throws RuntimeException;
}
