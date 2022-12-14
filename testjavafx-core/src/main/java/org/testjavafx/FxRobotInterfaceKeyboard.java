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

import javafx.scene.input.KeyCode;

/**
 * A set of methods for testing keyboard interactions.
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
public interface FxRobotInterfaceKeyboard<T extends FxRobotInterfaceKeyboard<T>>
{
    /**
     * Taps the given keys.  A synonym for {@link #tap(KeyCode...)}, for compatibility with TestFX.
     * 
     * <p>Presses all the keys in order, then releases
     * them all in reverse order, then calls
     * {@link FxThreadUtils#waitForFxEvents()}.
     *
     * @param keyCodes The key codes to press then release.
     * @return This object, for easy chaining of methods.
     */
    public T push(KeyCode... keyCodes);

    /**
     * Taps the given keys.
     *
     * <p>Presses all the keys in order, then releases
     * them all in reverse order, then calls
     * {@link FxThreadUtils#waitForFxEvents()}.
     *
     * <p>If you pass {@link KeyCode#SHORTCUT} as a KeyCode,
     * this will map to {@link KeyCode#COMMAND} on Mac, and
     * {@link KeyCode#CONTROL} on other operating systems, so you
     * can perform a cross-platform copy command (for example)
     * by calling this method with:
     * <code>tap(KeyCode.SHORTCUT, KeyCode.C);</code>
     *
     * @param keyCodes The key codes to press then release.
     * @return This object, for easy chaining of methods.
     */
    public T tap(KeyCode... keyCodes);
    
    
    /**
     * Presses all the keys in the given order, then calls
     * {@link FxThreadUtils#waitForFxEvents()}.
     *
     * <p>Note that depending on the operating system, keys
     * pressed may generate repeated key pressed or typed events
     * until they are released.
     *
     * @param keyCodes The key codes to press.
     * @return This object, for easy chaining of methods.
     */
    public T press(KeyCode... keyCodes);

    /**
     * Releases all the keys in the given order, then calls
     * {@link FxThreadUtils#waitForFxEvents()}.  Calling with no arguments releases
     * all keys held down by previously calling press.
     *
     * @param keyCodes The key codes to release.
     * @return This object, for easy chaining of methods.
     */
    public T release(KeyCode... keyCodes);

    /**
     * Writes the given character using a key press/typed/released
     * event sequence, then calls {@link FxThreadUtils#waitForFxEvents()}.
     *
     * @param c The character to write.
     * @return This object, for easy chaining of methods.
     */
    public T write(char c);

    /**
     * Writes the given String, one character at a time,
     * using a key press/typed/released event sequence,
     * then calls {@link FxThreadUtils#waitForFxEvents()}.
     *
     * @param text The text to write.
     * @return This object, for easy chaining of methods.
     */
    public T write(String text);

    /**
     * Writes the given String, one character at a time,
     * using a key press/typed/released event sequence,
     * then calls {@link FxThreadUtils#waitForFxEvents()}.
     * It sleeps for the given number of milliseconds
     * between each character.
     *
     * <p>Note that there is already an inherent implementation
     * delay between each character as this method calls {@link FxThreadUtils#waitForFxEvents()}
     * between each character.  (Without this, all the character events
     * pile up and are not necessarily processed on return from this
     * method.)  So there's around 50-100ms between each character already,
     * even if you pass a low value like 1 or 5 as millisecondDelay.
     *
     * @param text The text to write.
     * @param millisecondDelay The number of milliseconds to wait between each character.
     * @return This object, for easy chaining of methods.
     */
    public T write(String text, int millisecondDelay);
}
