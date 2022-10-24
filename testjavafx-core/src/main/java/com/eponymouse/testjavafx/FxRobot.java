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

import com.eponymouse.testjavafx.FxRobotInterface;
import com.eponymouse.testjavafx.FxThreadUtils;
import com.eponymouse.testjavafx.node.NodeQuery;
import com.google.common.collect.ImmutableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Window;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class FxRobot implements FxRobotInterface
{
    private final Robot actualRobot = FxThreadUtils.syncFx(Robot::new);
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    
    @Override
    public FxRobotInterface push(KeyCode... keyCodes)
    {
        ImmutableList<KeyCode> order = ImmutableList.copyOf(keyCodes);
        order.forEach(c -> FxThreadUtils.asyncFx(() -> actualRobot.keyPress(c)));
        order.reverse().forEach(c -> FxThreadUtils.asyncFx(() -> actualRobot.keyRelease(c)));
        FxThreadUtils.asyncFx(() -> pressedKeys.removeAll(order));
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface press(KeyCode... keyCodes)
    {
        ImmutableList<KeyCode> order = ImmutableList.copyOf(keyCodes);
        order.forEach(c -> FxThreadUtils.asyncFx(() -> actualRobot.keyPress(c)));
        FxThreadUtils.asyncFx(() -> pressedKeys.addAll(order));
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface release(KeyCode... keyCodes)
    {
        ImmutableList<KeyCode> order = keyCodes.length == 0 ? FxThreadUtils.syncFx(() -> ImmutableList.copyOf(pressedKeys)) : ImmutableList.copyOf(keyCodes);
        order.forEach(c -> FxThreadUtils.asyncFx(() -> actualRobot.keyRelease(c)));
        FxThreadUtils.asyncFx(() -> pressedKeys.removeAll(order));
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    public FxRobotInterface sleep(int millisecondDelay)
    {
        try
        {
            Thread.sleep(millisecondDelay);
        }
        catch (InterruptedException e)
        {
        }
        return this;
    }

    @Override
    public List<Window> listWindows()
    {
        return FxThreadUtils.syncFx(() -> ImmutableList.copyOf(Window.getWindows()));
    }

    private Window focusedWindow()
    {
        return FxThreadUtils.syncFx(() -> Window.getWindows().stream().filter(Window::isFocused).findFirst().orElse(null));
    }

    @Override
    public NodeQuery lookup(String query)
    {
        Node root = FxThreadUtils.syncFx(() -> focusedWindow().getScene().getRoot());
        return from(root).lookup(query);
    }

    public NodeQuery from(Node... roots)
    {
        ImmutableList<Node> allRoots = ImmutableList.copyOf(roots);
        return new NodeQueryImpl(allRoots);
    }

    @Override
    public FxRobotInterface clickOn(String query, MouseButton... mouseButtons)
    {
        Node node = lookup(query).queryWithRetry();
        clickOn(FxThreadUtils.syncFx(() -> node.localToScreen(new Point2D(node.getBoundsInLocal().getCenterX(), node.getBoundsInLocal().getCenterY()))), mouseButtons);
        return this;
    }

    @Override
    public FxRobotInterface clickOn(MouseButton... mouseButtons)
    {
        MouseButton[] actualButtons = mouseButtons.length == 0 ? new MouseButton[] {MouseButton.PRIMARY} : mouseButtons;
        FxThreadUtils.syncFx(() -> actualRobot.mouseClick(actualButtons));
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface moveTo(Point2D screenPosition)
    {
        // TODO move gradually
        FxThreadUtils.syncFx(() -> actualRobot.mouseMove(screenPosition));
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface moveTo(String query)
    {
        return moveTo(point(query));
    }

    @Override
    public Point2D point(Node node)
    {
        return FxThreadUtils.syncFx(() -> {
            Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
            return new Point2D(screenBounds.getCenterX(), screenBounds.getCenterY());
        });
    }
}
