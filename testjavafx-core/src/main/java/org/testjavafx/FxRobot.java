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

import org.testjavafx.node.NodeQuery;
import com.google.common.collect.ImmutableList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Window;
import javafx.util.Duration;
import org.apache.commons.lang3.SystemUtils;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * FxRobot is the central class of TestJavaFX that implements all the methods
 * to fake GUI events and perform GUI queries.  The simplest way to use it in
 * a test is to extend it.
 * 
 * <p>Generally, you test JavaFX GUIs from a new thread, NOT from the FX thread.
 * If you test from the FX thread then the event handlers for keyboard, mouse, 
 * etc, cannot run because your test code is blocking the FX thread.  Each of the
 * methods describes (where relevant) the effect of running the code on or off
 * the FX thread.  Many of the methods will block until they can run some code
 * on the FX thread.
 * 
 * <p>Note that FxRobot has some internal state, so it is strongly recommended to
 * only have one instance in use at any one time.  If you have multiple instances
 * in parallel then they will interfere with each other's state.
 * 
 * <p>All of FxRobot's public methods are overrides of the FxRobotInterface interface
 * (and its superclasses).  The documentation of all the methods is automatically
 * inherited from those classes.
 */
public class FxRobot implements FxRobotInterface
{
    /** The actual JavaFX robot we use to fake events. */
    private final Robot actualRobot;
    /** The set of currently pressed (held down) keyboard keys. */
    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    /** The set of currently pressed (held down) mouse buttons. */
    private final Set<MouseButton> pressedButtons = EnumSet.noneOf(MouseButton.class);

    /** The last window that was focused.  Only read/modified on FX thread. */
    private WeakReference<Window> lastFocusedWindow = null;

    /**
     * Construct a new instance.  This can either be called on the FX thread
     * or another thread -- but if the latter, it will block until the FX thread
     * is free in order to create some of the internals.
     * 
     * <p>If the FX toolkit has not been initialised before calling this constructor,
     * it will be launched by {@link FxThreadUtils}.
     */
    public FxRobot()
    {
        actualRobot = FxThreadUtils.syncFx(Robot::new);
    }

    @Override
    public FxRobotInterface push(KeyCode... keyCodes)
    {
        ImmutableList<KeyCode> order = ImmutableList.copyOf(keyCodes);
        FxThreadUtils.syncFx(() -> {
            order.forEach(c -> actualRobot.keyPress(actualKey(c)));
            order.reverse().forEach(c -> actualRobot.keyRelease(actualKey(c)));
            pressedKeys.removeAll(order);
        });
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    private KeyCode actualKey(KeyCode c)
    {
        if (c == KeyCode.SHORTCUT)
            return SystemUtils.IS_OS_MAC ? KeyCode.COMMAND : KeyCode.CONTROL;
        else
            return c;
    }

    @Override
    public FxRobotInterface press(KeyCode... keyCodes)
    {
        ImmutableList<KeyCode> order = ImmutableList.copyOf(keyCodes);
        FxThreadUtils.syncFx(() -> {
            order.forEach(c -> actualRobot.keyPress(c));
            pressedKeys.addAll(order);
        });
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface release(KeyCode... keyCodes)
    {
        ImmutableList<KeyCode> order = keyCodes.length == 0 ? FxThreadUtils.syncFx(() -> ImmutableList.copyOf(pressedKeys)) : ImmutableList.copyOf(keyCodes);
        FxThreadUtils.syncFx(() -> {
            order.forEach(c -> actualRobot.keyRelease(c));
            pressedKeys.removeAll(order);
        });
        
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    // Taken from TestFX:
    @Override
    public FxRobotInterface write(String text, int millisecondDelay)
    {
        Window w = focusedWindow();
        Scene scene = w.getScene();
        text.chars().forEach(c -> {
            FxThreadUtils.asyncFx(() -> Event.fireEvent(getEventTarget(scene), createKeyEvent(KeyEvent.KEY_PRESSED, KeyCode.UNDEFINED, "")));
            FxThreadUtils.asyncFx(() -> Event.fireEvent(getEventTarget(scene), createKeyEvent(KeyEvent.KEY_TYPED, KeyCode.UNDEFINED, Character.toString(c))));
            FxThreadUtils.asyncFx(() -> Event.fireEvent(getEventTarget(scene), createKeyEvent(KeyEvent.KEY_RELEASED, KeyCode.UNDEFINED, "")));
            sleep(millisecondDelay);
        });
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface sleep(int millisecondDelay)
    {
        try
        {
            Thread.sleep(millisecondDelay);
        }
        catch (InterruptedException e)
        {
            // Just cancel the sleep, I guess
        }
        return this;
    }

    // Taken from TestFX:
    private KeyEvent createKeyEvent(EventType<KeyEvent> eventType, KeyCode keyCode, String character)
    {
        boolean pressed = eventType == KeyEvent.KEY_PRESSED;
        boolean isShiftDown = false;
        boolean isControlDown = false;
        boolean isAltDown = false;
        boolean isMetaDown = false;
        if (keyCode == KeyCode.SHIFT)
        {
            isShiftDown = pressed;
        }
        if (keyCode == KeyCode.CONTROL)
        {
            isControlDown = pressed;
        }
        if (keyCode == KeyCode.ALT)
        {
            isAltDown = pressed;
        }
        if (keyCode == KeyCode.META)
        {
            isMetaDown = pressed;
        }

        boolean typed = eventType == KeyEvent.KEY_TYPED;
        String keyChar = typed ? character : KeyEvent.CHAR_UNDEFINED;
        String keyText = typed ? "" : keyCode.getName();
        return new KeyEvent(eventType, keyChar, keyText, keyCode, isShiftDown, isControlDown, isAltDown, isMetaDown);
    }
    
    // Taken from TestFX:
    private EventTarget getEventTarget(Scene scene)
    {
        return scene.getFocusOwner() != null ? scene.getFocusOwner() : scene;
    }
    
        
    @Override
    public Window targetWindow()
    {
        return focusedWindow();
    }

    @Override
    public NodeQuery lookup(String query)
    {
        return new NodeQueryImpl(this::targetRoots).lookup(query);
    }

    private ImmutableList<Node> targetRoots()
    {
        return FxThreadUtils.syncFx(() -> listTargetWindows().stream().map(w -> w.getScene().getRoot()).collect(ImmutableList.toImmutableList()));
    }

    @Override
    public NodeQuery lookup(Predicate<Node> nodePredicate)
    {
        return new NodeQueryImpl(this::targetRoots).lookup(nodePredicate);
    }

    @Override
    public NodeQuery from(Node... roots)
    {
        ImmutableList<Node> allRoots = ImmutableList.copyOf(roots);
        return new NodeQueryImpl(() -> allRoots);
    }

    @Override
    public FxRobotInterface press(MouseButton... buttons)
    {
        MouseButton[] actualButtons = buttons.length == 0 ? new MouseButton[]{MouseButton.PRIMARY} : buttons;
        FxThreadUtils.syncFx(() -> {
            actualRobot.mousePress(actualButtons);
            pressedButtons.addAll(Arrays.asList(actualButtons));
        });
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface release(MouseButton... buttons)
    {
        FxThreadUtils.syncFx(() -> {
            if (buttons.length == 0)
            {
                actualRobot.mouseRelease(pressedButtons.toArray(MouseButton[]::new));
                pressedButtons.clear();
            }
            else
            {
                for (MouseButton button : buttons)
                {
                    if (pressedButtons.remove(button))
                        actualRobot.mouseRelease(button);
                }
            }
        });
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface clickOn(String query, MouseButton... mouseButtons)
    {
        Node node = lookup(query).queryWithRetry();
        if (node == null)
            throw new RuntimeException("No node found to click for query: \"" + query + "\"");
        clickOn(point(node), mouseButtons);
        return this;
    }

    @Override
    public FxRobotInterface clickOn(MouseButton... mouseButtons)
    {
        MouseButton[] actualButtons = mouseButtons.length == 0 ? new MouseButton[] {MouseButton.PRIMARY} : mouseButtons;
        FxThreadUtils.syncFx(() -> {
            // If the button was pressed, need to release it before we can click it:
            for (MouseButton b : actualButtons)
            {
                if (pressedButtons.remove(b))
                    actualRobot.mouseRelease(b);
            }
            actualRobot.mouseClick(actualButtons);
        });
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface moveTo(Point2D screenPosition, Motion motion)
    {
        if (motion == Motion.HORIZONTAL_FIRST)
        {
            moveTo(new Point2D(screenPosition.getX(), FxThreadUtils.syncFx(actualRobot::getMouseY)), Motion.STRAIGHT_LINE);
            moveTo(screenPosition, Motion.STRAIGHT_LINE);
        }
        else if (motion == Motion.VERTICAL_FIRST)
        {
            moveTo(new Point2D(FxThreadUtils.syncFx(actualRobot::getMouseX), screenPosition.getY()), Motion.STRAIGHT_LINE);
            moveTo(screenPosition, Motion.STRAIGHT_LINE);
        }
        
        if (motion == Motion.STRAIGHT_LINE && !Platform.isFxApplicationThread())
        {
            CompletableFuture<Boolean> f = new CompletableFuture<>();
            Platform.runLater(() -> {
                Point2D curPos = actualRobot.getMousePosition();
                double pixelsPerSecond = 500;
                // We make sure there's at least one keyframe:
                double seconds = Math.max(1.0/16.0, screenPosition.distance(curPos) / pixelsPerSecond);
                Timeline t = new Timeline();

                for (double s = 0; s < seconds; s += 1.0/32.0)
                {
                    double proportion = s / seconds;
                    t.getKeyFrames().add(new KeyFrame(Duration.seconds(s), e -> {
                        actualRobot.mouseMove(
                         curPos.getX() + (screenPosition.getX() - curPos.getX()) * proportion,
                         curPos.getY() + (screenPosition.getY() - curPos.getY()) * proportion
                        );
                    }));
                }
                        
                t.setOnFinished(e -> {
                    // Make sure to always end on exact position:
                    actualRobot.mouseMove(screenPosition);
                    f.complete(true);
                });
                t.play();
            });
            try
            {
                f.get(5, TimeUnit.SECONDS);
            }
            catch (ExecutionException | InterruptedException | TimeoutException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            FxThreadUtils.syncFx(() -> actualRobot.mouseMove(screenPosition));
        }
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface moveTo(String query, Motion motion)
    {
        return moveTo(point(query), motion);
    }

    @Override
    public FxRobotInterface moveTo(Node node)
    {
        return moveTo(point(node));
    }

    @Override
    public FxRobotInterface moveBy(double x, double y)
    {
        return moveTo(FxThreadUtils.syncFx(actualRobot::getMousePosition).add(x, y));
    }

    @Override
    public Point2D point(Node node)
    {
        if (node == null)
            throw new NullPointerException("Cannot get point for null node");
        return FxThreadUtils.syncFx(() -> {
            Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
            return new Point2D(screenBounds.getCenterX(), screenBounds.getCenterY());
        });
    }

    @Override
    public void scroll(int verticalAmount)
    {
        FxThreadUtils.syncFx(() -> actualRobot.mouseWheel(verticalAmount));
        FxThreadUtils.waitForFxEvents();
    }

    @Override
    public void scrollHorizontal(int horizontalAmount)
    {
        press(KeyCode.SHIFT);
        FxThreadUtils.syncFx(() -> actualRobot.mouseWheel(horizontalAmount));
        release(KeyCode.SHIFT);
        FxThreadUtils.waitForFxEvents();
    }

    @Override
    public Window focusedWindow()
    {
        return FxThreadUtils.<Window>syncFx(() -> {
            List<Window> windows = Window.getWindows();
            for (Window window : windows)
            {
                if (window.isFocused())
                {
                    // Remember this:
                    this.lastFocusedWindow = new WeakReference<>(window);
                    return window;
                }
            }
            // No focused window; is the most recent one around?
            Window lastFocused = this.lastFocusedWindow == null ? null : this.lastFocusedWindow.get();
            if (lastFocused != null)
                return lastFocused;
            // Is there only one window?
            if (windows.size() == 1)
                return windows.get(0); // Note we don't remember this, as it's only a default guess
            // Give up:
            return null;
        });
    }
}
