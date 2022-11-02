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

import javafx.geometry.Bounds;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Point2D;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;

/**
 * A set of methods for testing mouse interactions.
 * All the methods return this for easy chaining.
 *
 * <p>Calling these methods from the FX thread is safe
 * but the events will not have been processed on return
 * from the methods.  The intended use is calling from another
 * thread, where the methods should have been processed by
 * the time the method returns.  See {@link FxThreadUtils#waitForFxEvents()}
 * for more information.
 *
 * @param <T> The type of this object to be returned from all the methods.
 *            This will be {@link FxRobotInterface} if you use these methods via
 *            {@link FxRobotInterface} or {@link FxRobot}.
 */
public interface FxRobotInterfaceMouse<T extends FxRobotInterfaceMouse<T>>
{
    /**
     * Presses (and holds down) the given mouse buttons in the given order.
     * Calling with empty parameters is equivalent to calling with
     * MouseButton.PRIMARY.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after pressing all the buttons.
     *
     * @param buttons The buttons to press and keep held down.  Duplicate entries will
     *                have an undefined effect.
     * @return This object, for easy chaining.
     */
    public T press(MouseButton... buttons);

    /**
     * The mouse buttons to release.  Calling without parameters releases all the
     * mouse buttons that were being held down by previous calls to {@link #press(MouseButton...)}.
     *
     * <p>Calling with buttons that are not currently held down will have an undefined effect.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after releasing all the buttons.
     *
     * @param buttons The buttons to release.
     * @return This object, for easy chaining.
     */
    public T release(MouseButton... buttons);

    /**
     * Clicks on the centre of the given node, using the given mouse buttons.
     * Calling with empty mouse buttons will click using MouseButton.PRIMARY.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after releasing all the buttons.
     *
     * @param node The node to click on.  Note that if there any other nodes
     *             in front of this node, they may receive the click instead;
     *             the mouse is clicked at the centre of the node but does not
     *             guarantee that this event will receive the click.
     * @param mouseButtons The buttons to click.  If left empty, MouseButton.PRIMARY
     *                     will be clicked.
     * @return This object, for easy chaining.
     */
    public default T clickOn(Node node, MouseButton... mouseButtons)
    {
        if (node == null)
            throw new NullPointerException("Cannot click on null node");
        Point2D p = FxThreadUtils.syncFx(() -> {
            Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
            return new Point2D(screenBounds.getCenterX(), screenBounds.getCenterY());
        });
        return clickOn(p, mouseButtons);
    }

    /**
     * Clicks on the centre of the result of the query, using the given mouse buttons.
     * Calling with empty mouse buttons will click using MouseButton.PRIMARY.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after releasing all the buttons.
     *
     * @param query The query for the node to click on.  Note that if there any other nodes
     *             in front of this node, they may receive the click instead;
     *             the mouse is clicked at the centre of the node but does not
     *             guarantee that this event will receive the click.
     * @param mouseButtons The buttons to click.  If left empty, MouseButton.PRIMARY
     *                     will be clicked.
     * @return This object, for easy chaining.
     */
    public T clickOn(String query, MouseButton... mouseButtons);

    /**
     * Clicks on the given screen position, using the given mouse buttons.
     * Calling with empty mouse buttons will click using MouseButton.PRIMARY.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after releasing all the buttons.
     *
     * @param screenPosition The screen position to click on.
     * @param mouseButtons The buttons to click.  If left empty, MouseButton.PRIMARY
     *                     will be clicked.
     * @return This object, for easy chaining.
     */
    public default T clickOn(Point2D screenPosition, MouseButton... mouseButtons)
    {
        moveTo(screenPosition);
        return clickOn(mouseButtons);
    }

    /**
     * Clicks the mouse at the current position, using the given mouse buttons.
     * Calling with empty mouse buttons will click using MouseButton.PRIMARY.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after releasing all the buttons.
     *
     * @param mouseButtons The buttons to click.  If left empty, MouseButton.PRIMARY
     *                     will be clicked.
     * @return This object, for easy chaining.
     */
    public T clickOn(MouseButton... mouseButtons);

    /**
     * Moves to the centre of the result of the query using the default
     * {@link Motion}.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after moving.
     *
     * @param query The query to use, with retrying.
     * @return This object, for easy chaining.
     */
    public default T moveTo(String query)
    {
        return moveTo(query, Motion.DEFAULT());
    }

    /**
     * Moves to the given screen position using the default {@link Motion}.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after moving.
     *
     * @param screenPosition The screen position to move to.
     * @return This object, for easy chaining.
     */
    public default T moveTo(Point2D screenPosition)
    {
        return moveTo(screenPosition, Motion.DEFAULT());
    }

    /**
     * Moves to the given screen position using the default {@link Motion}.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after moving.
     *
     * @param screenX The screen X position to move to.
     * @param screenY The screen Y position to move to.
     * @return This object, for easy chaining.
     */
    public default T moveTo(double screenX, double screenY)
    {
        return moveTo(new Point2D(screenX, screenY));
    }

    /**
     * Moves by the given amount using the default {@link Motion}.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after moving.
     *
     * @param x The X amount to move by.
     * @param y The Y amount to move by.
     * @return This object, for easy chaining.
     */
    public T moveBy(double x, double y);

    /**
     * Moves to the centre of the result of the query using the given
     * {@link Motion}.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after moving.
     *
     * @param query The query to use, with retrying.
     * @param motion The motion to use for the movement.
     * @return This object, for easy chaining.
     */
    public T moveTo(String query, Motion motion);

    /**
     * Moves to the given screen position using the given
     * {@link Motion}.
     *
     * <p>Can be called from any thread.  Will call {@link FxThreadUtils#waitForFxEvents()}
     * after moving.
     *
     * @param screenPosition The screen position to move to.
     * @param motion The motion to use for the movement.
     * @return This object, for easy chaining.
     */
    public T moveTo(Point2D screenPosition, Motion motion);

    /**
     * Starts a drag at the current mouse position.  This is actually a synonym for
     * {@link #press(MouseButton...)}.
     *
     * @param mouseButtons The buttons to press.  If left empty, MouseButton.PRIMARY
     *                     will be pressed.
     * @return This object, for easy chaining.
     */
    public default T drag(MouseButton... mouseButtons)
    {
        return press(mouseButtons);
    }

    /**
     * Moves the mouse to the given position then starts a drag by pressing the
     * given mouse buttons.  This is equivalent to calling {@link #moveTo(Point2D)}
     * followed by {@link #press(MouseButton...)}.  To drag <i>to</i> a specific position,
     * then call {@link #dropTo(Point2D)}.
     *
     * @param from The screen position to <i>start</i> the drag at.
     * @param mouseButtons The buttons to press.  If left empty, MouseButton.PRIMARY
     *                     will be pressed.
     * @return This object, for easy chaining.
     */
    public default T drag(Point2D from, MouseButton... mouseButtons)
    {
        moveTo(from);
        return press(mouseButtons);
    }

    /**
     * Drops the item at the current mouse position.  This is a synonym for
     * {@link #release(MouseButton...)} with empty arguments.
     *
     * @return This object, for easy chaining.
     */
    public default T drop()
    {
        return release(new MouseButton[0]);
    }

    /**
     * Drops the item at the given mouse position.  This is equivalent to calling
     * {@link #moveTo(Point2D)} followed by {@link #release(MouseButton...)}
     *
     * @param to The screen position to move to before releasing the buttons.
     * @return This object, for easy chaining.
     */
    public default T dropTo(Point2D to)
    {
        moveTo(to, Motion.STRAIGHT_LINE);
        return release(new MouseButton[0]);
    }

    /**
     * Scrolls by the given amount vertically.
     *
     * <p>Note that I believe the amount can be interpreted differently
     * on different platforms.  On macOS there is a setting to invert
     * the mouse wheel and I think that inverts what this parameter means.
     * At the moment, this is not corrected for.
     * 
     * <p>Note also that scrolling doesn't seem to work on Monocle.
     *
     * @param verticalAmount The amount to scroll by, positive is one direction,
     *                       negative the other.
     * @return This object, for easy chaining.
     */
    public T scroll(int verticalAmount);

    /**
     * Scrolls in the given vertical direction.
     *
     * <p>Note that I believe the amount can be interpreted differently
     * on different platforms.  On macOS there is a setting to invert
     * the mouse wheel and I think that inverts what this parameter means.
     * At the moment, this is not corrected for.
     * 
     * <p>Note also that scrolling doesn't seem to work on Monocle.
     *
     * @param verticalDirection The direction to scroll in.
     * @return This object, for easy chaining.
     */
    public default T scroll(VerticalDirection verticalDirection)
    {
        return scroll(1, verticalDirection);
    }

    /**
     * Scrolls the given amount in the given vertical direction.
     *
     * <p>Note that I believe the amount can be interpreted differently
     * on different platforms.  On macOS there is a setting to invert
     * the mouse wheel and I think that inverts what this parameter means.
     * At the moment, this is not corrected for.
     * 
     * <p>Note also that scrolling doesn't seem to work on Monocle.
     *
     * @param amount The amount to scroll by
     * @param verticalDirection The direction to scroll in.
     * @return This object, for easy chaining.
     */
    public default T scroll(int amount, VerticalDirection verticalDirection)
    {
        return scroll(verticalDirection == VerticalDirection.DOWN ? amount : -amount);
    }

    /**
     * Scrolls by the given amount horizontally.  This is done by holding
     * shift while scrolling, so the shift key will be released after this
     * method.
     *
     * <p>Note that I believe the amount can be interpreted differently
     * on different platforms.  On macOS there is a setting to invert
     * the mouse wheel and I think that inverts what this parameter means.
     * At the moment, this is not corrected for.
     * 
     * <p>Note also that scrolling doesn't seem to work on Monocle.
     *
     * @param horizontalAmount The amount to scroll by, positive is one direction,
     *                       negative the other.
     * @return This object, for easy chaining.
     */
    public T scrollHorizontal(int horizontalAmount);

    /**
     * Scrolls in the given direction horizontally.  This is done by holding
     * shift while scrolling, so the shift key will be released after this
     * method.
     *
     * <p>Note that I believe the amount can be interpreted differently
     * on different platforms.  On macOS there is a setting to invert
     * the mouse wheel and I think that inverts what this parameter means.
     * At the moment, this is not corrected for.
     * 
     * <p>Note also that scrolling doesn't seem to work on Monocle.
     *
     * @param horizontalDirection The direction to scroll in.
     * @return This object, for easy chaining.
     */
    public default T scroll(HorizontalDirection horizontalDirection)
    {
        return scroll(1, horizontalDirection);
    }

    /**
     * Scrolls the given amount in the given horizontal direction.  This is
     * done by holding shift while scrolling, so the shift key will be released
     * after this method.
     *
     * <p>Note that I believe the amount can be interpreted differently
     * on different platforms.  On macOS there is a setting to invert
     * the mouse wheel and I think that inverts what this parameter means.
     * At the moment, this is not corrected for.
     * 
     * <p>Note also that scrolling doesn't seem to work on Monocle.
     *
     * @param amount The amount to scroll by
     * @param horizontalDirection The direction to scroll in.
     * @return This object, for easy chaining.
     */
    public default T scroll(int amount, HorizontalDirection horizontalDirection)
    {
        return scroll(horizontalDirection == HorizontalDirection.RIGHT ? amount : -amount);
    }
}
