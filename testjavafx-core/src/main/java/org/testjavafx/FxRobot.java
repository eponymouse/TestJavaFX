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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.HorizontalDirection;
import javafx.geometry.Point2D;
import javafx.geometry.VerticalDirection;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.apache.commons.lang3.SystemUtils;
import org.testjavafx.node.NodeQuery;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
 * <p>Note that FxRobot has some internal state to track the GUI state, so it is
 * strongly recommended to only have one instance in use at any one time.  If you
 * have multiple instances in parallel then they will interfere with each other's
 * state.
 *
 * <p>All of FxRobot's public methods are overrides of the FxRobotInterface interface
 * (and its superclasses).  The documentation of all the methods is automatically
 * inherited from those classes.
 * 
 * <p>Be careful if you override any of these methods
 * yourself as some of them call each other and rely on the default behaviour.
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
    public FxRobot tap(KeyCode... keyCodes)
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
    public FxRobot press(KeyCode... keyCodes)
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
    public FxRobot release(KeyCode... keyCodes)
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
    public FxRobot write(String text, int millisecondDelay)
    {
        Window w = focusedWindow();
        Scene scene = w.getScene();
        text.chars().forEach(c -> {
            FxThreadUtils.asyncFx(() -> Event.fireEvent(getEventTarget(scene), createKeyEvent(KeyEvent.KEY_PRESSED, KeyCode.UNDEFINED, "")));
            FxThreadUtils.asyncFx(() -> Event.fireEvent(getEventTarget(scene), createKeyEvent(KeyEvent.KEY_TYPED, KeyCode.UNDEFINED, Character.toString(c))));
            FxThreadUtils.asyncFx(() -> Event.fireEvent(getEventTarget(scene), createKeyEvent(KeyEvent.KEY_RELEASED, KeyCode.UNDEFINED, "")));
            FxThreadUtils.waitForFxEvents();
            sleep(millisecondDelay);
        });
        FxThreadUtils.waitForFxEvents();
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
    public NodeQuery from(Window window)
    {
        return new NodeQueryImpl(() -> ImmutableList.of(window.getScene().getRoot()));
    }

    @Override
    public FxRobot press(MouseButton... buttons)
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
    public FxRobot release(MouseButton... buttons)
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
    public FxRobot clickOn(String query, MouseButton... mouseButtons)
    {
        Node node = lookup(query).queryWithRetry();
        if (node == null)
            throw new RuntimeException("No node found to click for query: \"" + query + "\"");
        clickOn(point(node), mouseButtons);
        return this;
    }

    @Override
    public FxRobot clickOn(MouseButton... mouseButtons)
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
    public FxRobot moveTo(Point2D screenPosition, Motion motion)
    {
        Preconditions.checkNotNull(screenPosition, "moveTo->screenPosition must not be null");
        
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
    public FxRobot moveTo(String query, Motion motion)
    {
        return moveTo(point(query), motion);
    }

    @Override
    public FxRobot moveBy(double x, double y)
    {
        return (FxRobot)moveTo(FxThreadUtils.syncFx(actualRobot::getMousePosition).add(x, y));
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
    public FxRobot scroll(int verticalAmount)
    {
        FxThreadUtils.syncFx(() -> actualRobot.mouseWheel(verticalAmount));
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobot scrollHorizontal(int horizontalAmount)
    {
        press(KeyCode.SHIFT);
        FxThreadUtils.syncFx(() -> actualRobot.mouseWheel(horizontalAmount));
        release(KeyCode.SHIFT);
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public Window focusedWindow()
    {
        return FxThreadUtils.<Window>syncFx(() -> {
            List<Window> windows = Window.getWindows();
            List<Window> focused = new ArrayList<>(windows.stream().filter(Window::isFocused).collect(Collectors.toList()));
            // It seems that (only in Monocle?) multiple windows can claim to
            // have focus when a main window shows sub-dialogs, so we have to manually
            // try to work out the real focused window:
            if (focused.size() > 1)
            {
                // Remove any windows claiming to be focused which have a child
                // window that is focused:
                focused.removeIf(w -> focused.stream().anyMatch(parent -> parent instanceof Stage && ((Stage) parent).getOwner() == w));
            }
            if (focused.size() == 1)
            {
                // Remember this:
                this.lastFocusedWindow = new WeakReference<>(focused.get(0));
                return focused.get(0);
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
    
    @Override
    public FxRobot waitUntil(BooleanSupplier check)
    {
        if (!Platform.isFxApplicationThread())
        {
            for (int retries = 80; retries >= 0; retries--)
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                    // Just cancel the sleep, we'll go round and retry anyway
                }
                if (FxThreadUtils.syncFx(check::getAsBoolean))
                    return this;
            }
        }
        else
        {
            if (check.getAsBoolean())
                return this;
        }
        throw new RuntimeException("waitUntil() condition was not satisfied even after retries");
    }

    @Override
    public boolean isShowing(String query)
    {
        return lookup(query).query() != null;
    }

    @Override
    public boolean isFocused(String query)
    {
        return FxThreadUtils.syncFx(() -> lookup(query).queryAll().stream().anyMatch(Node::isFocused));
    }

    @Override
    public BooleanSupplier showing(String query)
    {
        return () -> isShowing(query);
    }

    @Override
    public BooleanSupplier focused(String query)
    {
        return () -> isFocused(query);
    }

    @Override
    public BooleanSupplier not(BooleanSupplier booleanSupplier)
    {
        return () -> !booleanSupplier.getAsBoolean();
    }

    @Override
    public BooleanSupplier and(BooleanSupplier... booleanSuppliers)
    {
        return () -> Arrays.stream(booleanSuppliers).allMatch(BooleanSupplier::getAsBoolean);
    }

    @Override
    public List<Window> listTargetWindows()
    {
        return FxThreadUtils.syncFx(() -> fetchWindowsByProximityTo(focusedWindow()));
    }

    // Only call on FX thread
    private List<Window> fetchWindowsByProximityTo(Window targetWindow)
    {
        return orderWindowsByProximityTo(targetWindow, listWindows());
    }

    // Only call on FX thread
    private List<Window> orderWindowsByProximityTo(Window targetWindow, List<Window> windows)
    {
        List<Window> copy = new ArrayList<>(windows);
        copy.sort(Comparator.comparingInt(w -> calculateWindowProximityTo(targetWindow, w)));
        return Collections.unmodifiableList(copy);
    }

    // Only call on FX thread
    private int calculateWindowProximityTo(Window targetWindow, Window window)
    {
        if (window == targetWindow)
        {
            return 0;
        }
        if (isOwnerOf(window, targetWindow))
        {
            return 1;
        }
        return 2;
    }

    // Only call on FX thread
    private boolean isOwnerOf(Window window, Window targetWindow)
    {
        Window ownerWindow = retrieveOwnerOf(window);
        if (ownerWindow == targetWindow)
        {
            return true;
        }
        return ownerWindow != null && isOwnerOf(ownerWindow, targetWindow);
    }

    // Only call on FX thread
    private Window retrieveOwnerOf(Window window)
    {
        if (window instanceof Stage)
        {
            return ((Stage) window).getOwner();
        }
        if (window instanceof PopupWindow)
        {
            return ((PopupWindow) window).getOwnerWindow();
        }
        return null;
    }

    @Override
    public List<Window> listWindows()
    {
        return FxThreadUtils.syncFx(() -> ImmutableList.copyOf(Window.getWindows()));
    }

    @Override
    public FxRobotInterface push(KeyCode... keyCodes)
    {
        return tap(keyCodes);
    }

    @Override
    public FxRobotInterface write(char c)
    {
        return write(Character.toString(c));
    }

    @Override
    public FxRobotInterface write(String text)
    {
        return write(text, 0);
    }

    @Override
    public FxRobotInterface sleep(int milliseconds)
    {
        try
        {
            Thread.sleep(milliseconds);
        }
        catch (InterruptedException e)
        {
            // Just cancel the sleep, I guess
        }
        return this;
    }

    @Override
    @Deprecated
    public Window targetWindow()
    {
        return focusedWindow();
    }

    @Override
    public Point2D point(String query)
    {
        Node node = lookup(query).queryWithRetry();
        if (node == null)
            return null;
        else
            return point(node);
    }

    @Override
    public FxRobotInterface clickOn(Node node, MouseButton... mouseButtons)
    {
        if (node == null)
            throw new NullPointerException("Cannot click on null node");
        Point2D p = FxThreadUtils.syncFx(() -> {
            Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
            return new Point2D(screenBounds.getCenterX(), screenBounds.getCenterY());
        });
        return clickOn(p, mouseButtons);
    }

    @Override
    public FxRobotInterface clickOn(Point2D screenPosition, MouseButton... mouseButtons)
    {
        moveTo(screenPosition);
        return clickOn(mouseButtons);
    }

    @Override
    public FxRobotInterface moveTo(String query)
    {
        return moveTo(query, Motion.DEFAULT());
    }

    @Override
    public FxRobotInterface moveTo(Point2D screenPosition)
    {
        return moveTo(screenPosition, Motion.DEFAULT());
    }

    @Override
    public FxRobotInterface moveTo(double screenX, double screenY)
    {
        return moveTo(new Point2D(screenX, screenY));
    }

    @Override
    public FxRobotInterface drag(MouseButton... mouseButtons)
    {
        return press(mouseButtons);
    }

    @Override
    public FxRobotInterface drag(Point2D from, MouseButton... mouseButtons)
    {
        moveTo(from);
        return press(mouseButtons);
    }

    @Override
    public FxRobotInterface drop()
    {
        return release(new MouseButton[0]);
    }

    @Override
    public FxRobotInterface dropTo(Point2D to)
    {
        moveTo(to, Motion.STRAIGHT_LINE);
        return release(new MouseButton[0]);
    }

    @Override
    public FxRobotInterface scroll(VerticalDirection verticalDirection)
    {
        return scroll(1, verticalDirection);
    }

    @Override
    public FxRobotInterface scroll(int amount, VerticalDirection verticalDirection)
    {
        return scroll(verticalDirection == VerticalDirection.DOWN ? amount : -amount);
    }

    @Override
    public FxRobotInterface scroll(HorizontalDirection horizontalDirection)
    {
        return scroll(1, horizontalDirection);
    }

    @Override
    public FxRobotInterface scroll(int amount, HorizontalDirection horizontalDirection)
    {
        return scroll(horizontalDirection == HorizontalDirection.RIGHT ? amount : -amount);
    }
}
