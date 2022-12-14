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
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.apache.commons.lang3.SystemUtils;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.testjavafx.node.NodeQuery;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
    private @MonotonicNonNull WeakReference<ImmutableList<Window>> lastFocusedWindows = null;

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
        text.chars().forEach(c -> {
            Scene scene = getFocusedSceneForWriting();
            final KeyCode keyCode;
            switch (c)
            {
                case '\n': keyCode = KeyCode.ENTER; break;
                case '\t': keyCode = KeyCode.TAB; break;
                default: keyCode = KeyCode.UNDEFINED; break;
            }
            FxThreadUtils.asyncFx(() -> Event.fireEvent(getEventTarget(scene), createKeyEvent(KeyEvent.KEY_PRESSED, keyCode, "")));
            FxThreadUtils.asyncFx(() -> Event.fireEvent(getEventTarget(scene), createKeyEvent(KeyEvent.KEY_TYPED, KeyCode.UNDEFINED, Character.toString(c))));
            FxThreadUtils.asyncFx(() -> Event.fireEvent(getEventTarget(scene), createKeyEvent(KeyEvent.KEY_RELEASED, keyCode, "")));
            FxThreadUtils.waitForFxEvents();
            sleep(millisecondDelay);
        });
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    private Scene getFocusedSceneForWriting()
    {
        Window targetWindow = targetWindow();
        if (targetWindow == null)
            throw new IllegalStateException("No focused window");
        Scene scene = targetWindow.getScene();
        if (scene == null)
            throw new IllegalStateException("Focused window " + targetWindow + " has a null Scene");
        return scene;
    }

    @Override
    public @Nullable Window targetWindow()
    {
        List<Window> focusedWindows = focusedWindows();
        // We could just return but if the user meant to write, they meant to write:
        if (focusedWindows.isEmpty())
            return null;
        
        if (focusedWindows.size() > 1)
        {
            // Hmm, we have multiple focused windows.  Need to work out which one has a focused node:
            ArrayList<Window> windowsWithAFocusOwner = FxThreadUtils.<ArrayList<Window>>syncFx(() -> {
                ArrayList<Window> ws = new ArrayList<>(focusedWindows.stream().filter(w -> w.getScene() != null && w.getScene().getFocusOwner() != null && w.getScene().getFocusOwner().isFocused()).collect(Collectors.toList()));
                ws.removeIf(parent -> ws.stream().anyMatch(child -> isOwnerOf(child, parent)));
                return ws;
            });
            
            if (windowsWithAFocusOwner.size() == 1)
                return windowsWithAFocusOwner.get(0);
            else if (windowsWithAFocusOwner.isEmpty())
                return null;
            else 
            {
                // This can happen in cases like you open a context menu, select an item
                // and it shows a dialog.  In Monocle the context menu (a popup window)
                // still counts as focused, the dialog is focused, but they do not
                // have a direct parent-child relation (they have a common ancestor
                // in the window that showed them, assuming the dialog has its owner
                // set correctly).
                
                // It's hard to think of an exact rule for a general case but we handle this
                // specific case by deprioritising context menu:
                Collections.sort(windowsWithAFocusOwner, Comparator.comparing(w -> w instanceof ContextMenu ? 1 : 0));

                return windowsWithAFocusOwner.get(0);
            }
        }
        else
            // Guaranteed to be at least a size 1 list at this point:
            return focusedWindows.get(0);
    }

    @Override
    public Window targetWindowOrThrow() throws RuntimeException
    {
        List<Window> focusedWindows = focusedWindows();
        // We could just return but if the user meant to write, they meant to write:
        if (focusedWindows.isEmpty())
            throw new IllegalStateException("Cannot write as no focused window found.  Candidates: " +
                FxThreadUtils.syncFx(() -> Window.getWindows().stream().map(w2 -> {
                    Scene scene = w2.getScene();
                    return w2 + "-focused=" + w2.isFocused()
                            + "-owner=" + retrieveOwnerOf(w2)        
                            + "-sceneFocusOwner=" + (scene == null ? "none" : scene.getFocusOwner())
                            + "-focusOwnerFocused=" + (scene != null && scene.getFocusOwner() != null ? scene.getFocusOwner().isFocused() : "NA");
                }).collect(Collectors.joining(", ")))
            );
        if (focusedWindows.size() > 1)
        {
            // Hmm, we have multiple focused windows.  Need to work out which one has a focused node:
            List<Window> windowsWithAFocusOwner = new ArrayList<>(focusedWindows.stream().filter(w -> w.getScene() != null && w.getScene().getFocusOwner() != null && w.getScene().getFocusOwner().isFocused()).collect(Collectors.toList()));
            windowsWithAFocusOwner.removeIf(parent -> windowsWithAFocusOwner.stream().anyMatch(child -> isOwnerOf(child, parent)));
            if (windowsWithAFocusOwner.size() == 1)
                focusedWindows = windowsWithAFocusOwner;
            else if (windowsWithAFocusOwner.isEmpty())
                throw new IllegalStateException("Multiple focused windows found but none with a node with focus; unclear which one to type into: " + focusedWindows.stream().map(Objects::toString).collect(Collectors.joining(" or ")));
            else
                throw new IllegalStateException("Multiple focused windows found and multiple with a node with focus; unclear which one to type into: " + windowsWithAFocusOwner.stream().map(Objects::toString).collect(Collectors.joining(" or ")));
        }
        // Guaranteed to be a size 1 list at this point:
        return focusedWindows.get(0);
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
        Point2D point = point(query);
        if (point != null)
            return moveTo(point, motion);
        else
            throw new IllegalStateException("No node matching \"" + query + "\" found.");
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
    public List<Window> focusedWindows()
    {
        return FxThreadUtils.<ImmutableList<Window>>syncFx(() -> {
            List<Window> windows = Window.getWindows();
            ImmutableList<Window> focused = windows.stream().filter(Window::isFocused).collect(ImmutableList.toImmutableList());
            
            if (!focused.isEmpty())
            {
                // Remember this:
                this.lastFocusedWindows = new WeakReference<>(focused);
                return focused;
            }
            // No focused window; is the most recent one around?
            ImmutableList<Window> lastFocused = this.lastFocusedWindows == null ? null : this.lastFocusedWindows.get();
            if (lastFocused != null)
            {
                // Only count windows that are still showing:
                lastFocused = lastFocused.stream().filter(Window::isShowing).collect(ImmutableList.toImmutableList());
                if (!lastFocused.isEmpty())
                    return lastFocused;
            }
            // Is there only one window?
            if (windows.size() == 1)
                return ImmutableList.copyOf(windows); // Note we don't remember this, as it's only a default guess
            // Give up:
            return ImmutableList.of();
        });
    }

    // package-visible
    static <R> Optional<R> implRetryUntilPresent(Supplier<Optional<R>> supplier)
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
                Optional<R> r = FxThreadUtils.syncFx(supplier::get);
                if (r.isPresent())
                    return r;
            }
        }
        else
        {
            // We suspend ourselves by entering a nested event loop, then spawn
            // a new thread to call retryUntil:
            final Object uniqueKey = new Object();
            new Thread(() -> {
                Supplier<Optional<R>> r = () -> Optional.empty();
                try
                {
                    Optional<R> val = implRetryUntilPresent(supplier);
                    r = () -> val;
                }
                catch (Throwable t)
                {
                    r = () -> {throw t;};
                }
                finally
                {
                    Supplier<Optional<R>> rFinal = r;
                    Platform.runLater(() -> Platform.exitNestedEventLoop(uniqueKey, rFinal));
                }
            }).start();
            return ((Supplier<Optional<R>>)Platform.enterNestedEventLoop(uniqueKey)).get();
        }
        return Optional.empty();
    }

    @Override
    public FxRobot retryUntil(BooleanSupplier check)
    {
        if (implRetryUntilPresent(() -> check.getAsBoolean() ? Optional.of("") : Optional.empty()).isEmpty())
            throw new RuntimeException("retryUntil() condition was not satisfied even after retries");
        return this;
    }

    @Override
    public <R> R retryUntilNonNull(Supplier<R> check)
    {
        Optional<R> r = implRetryUntilPresent(() -> Optional.ofNullable(check.get()));
        return r.orElseThrow(() -> new RuntimeException("retryUntilNonNull() returned null even after retries"));
    }

    @Override
    public <R> R retryUntilPresent(Supplier<Optional<R>> check)
    {
        Optional<R> r = implRetryUntilPresent(() -> check.get());
        return r.orElseThrow(() -> new RuntimeException("retryUntilPresent() returned empty even after retries"));
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
        return FxThreadUtils.syncFx(() -> fetchWindowsByProximityTo(focusedWindows()));
    }

    // Only call on FX thread
    private List<Window> fetchWindowsByProximityTo(List<Window> targetWindows)
    {
        return orderWindowsByProximityTo(targetWindows, listWindows());
    }

    // Only call on FX thread
    private List<Window> orderWindowsByProximityTo(List<Window> targetWindows, List<Window> windows)
    {
        List<Window> copy = new ArrayList<>(windows);
        copy.sort(Comparator.comparingInt(w -> targetWindows.stream().mapToInt(tw -> calculateWindowProximityTo(tw, w)).min().orElse(2)));
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
    private boolean isOwnerOf(Window descendant, Window ancestor)
    {
        Window ownerWindow = retrieveOwnerOf(descendant);
        if (ownerWindow == ancestor)
        {
            return true;
        }
        return ownerWindow != null && isOwnerOf(ownerWindow, ancestor);
    }

    // Only call on FX thread
    private @Nullable Window retrieveOwnerOf(Window window)
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
    public @Nullable Point2D point(String query)
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
