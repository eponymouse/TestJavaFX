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

public interface FxRobotInterfaceMouse<T extends FxRobotInterfaceMouse<T>>
{
    public T press(MouseButton... buttons);

    public T release(MouseButton... buttons);

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

    public T clickOn(String query, MouseButton... mouseButtons);

    public default T clickOn(Point2D screenPosition, MouseButton... mouseButtons)
    {
        moveTo(screenPosition);
        return clickOn(mouseButtons);
    }

    public T clickOn(MouseButton... mouseButtons);

    public default T moveTo(String query)
    {
        return moveTo(query, Motion.DEFAULT());
    }

    public T moveTo(Node node);

    public default T moveTo(Point2D screenPosition)
    {
        return moveTo(screenPosition, Motion.DEFAULT());
    }

    public default T moveTo(double screenX, double screenY)
    {
        return moveTo(new Point2D(screenX, screenY));
    }

    public T moveBy(double x, double y);

    public T moveTo(String query, Motion motion);

    public T moveTo(Point2D screenPosition, Motion motion);

    /**
     * Starts a drag at the current mouse position.  This is a synonym for
     * {@link #press(MouseButton...)}
     *
     * @param mouseButtons
     * @return
     */
    public default T drag(MouseButton... mouseButtons)
    {
        return press(mouseButtons);
    }

    /**
     * Moves the mouse to the given position then starts a drag by pressing the
     * given mouse buttons.  This is equivalent to calling {@link #moveTo(Point2D)}
     * followed by {@link #press(MouseButton...)}.
     * 
     * @param from
     * @param mouseButtons
     * @return
     */
    public default T drag(Point2D from, MouseButton... mouseButtons)
    {
        moveTo(from);
        return press(mouseButtons);
    }

    /**
     * Drops the item at the current mouse position.  This is a synonym for
     * {@link #release(MouseButton...)}
     */
    public default T drop()
    {
        return release(new MouseButton[0]);
    }

    /**
     * Drops the item at the given mouse position.  This is equivalent to calling
     * {@link #moveTo(Point2D)} followed by {@link #release(MouseButton...)}
     * 
     * @param to
     * @return
     */
    public default T dropTo(Point2D to)
    {
        moveTo(to, Motion.STRAIGHT_LINE);
        return release(new MouseButton[0]);
    }
        

    public void scroll(int verticalAmount);
    
    public default void scroll(VerticalDirection verticalDirection)
    {
        scroll(1, verticalDirection);
    }

    public default void scroll(int amount, VerticalDirection verticalDirection)
    {
        scroll(verticalDirection == VerticalDirection.DOWN ? amount : -amount);
    }
    
    public void scrollHorizontal(int horizontalAmount);

    public default void scroll(HorizontalDirection horizontalDirection)
    {
        scroll(1, horizontalDirection);
    }
    
    public default void scroll(int amount, HorizontalDirection horizontalDirection)
    {
        scroll(horizontalDirection == HorizontalDirection.RIGHT ? amount : -amount);
    }
}
