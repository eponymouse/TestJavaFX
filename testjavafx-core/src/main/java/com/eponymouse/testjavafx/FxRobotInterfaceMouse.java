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

import javafx.geometry.Bounds;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Point2D;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;

public interface FxRobotInterfaceMouse<T extends FxRobotInterfaceMouse<T>>
{
    public default T clickOn(Node node, MouseButton... mouseButtons)
    {
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

    public void scroll(int verticalAmount);
    
    public default void scroll(VerticalDirection verticalDirection)
    {
        scroll(verticalDirection == VerticalDirection.DOWN ? 1 : -1);
    }
    
    public void scrollHorizontal(int horizontalAmount);

    public default void scroll(HorizontalDirection horizontalDirection)
    {
        scroll(horizontalDirection == HorizontalDirection.RIGHT ? 1 : -1);
    }
}
