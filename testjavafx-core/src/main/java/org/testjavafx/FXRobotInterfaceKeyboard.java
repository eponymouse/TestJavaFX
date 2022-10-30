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
 * Calling these methods from the FX thread is safe
 * but the events will not have been processed on return
 * from the methods.  The intended use is calling from another
 * thread, where the methods should have been processed by
 * the time the method returns.  See {@link FxThreadUtils#waitForFxEvents()}
 * for more information.
 */
public interface FXRobotInterfaceKeyboard<T extends FXRobotInterfaceKeyboard<T>>
{
    /**
     * Presses all the keys in order, then releases
     * them all in reverse order, then calls
     * {@link FxThreadUtils#waitForFxEvents()}.
     */
    public T push(KeyCode... keyCodes);

    /**
     * Presses all the keys in the given order, then calls
     * {@link FxThreadUtils#waitForFxEvents()}.
     * 
     * Note that depending on the operating system, keys
     * pressed may generate repeated key typed events until
     * they are released.
     */
    public T press(KeyCode... keyCodes);

    /**
     * Releases all the keys in the given order, then calls
     * {@link FxThreadUtils#waitForFxEvents()}.  Calling with no arguments releases
     * all keys held down by previously calling press.
     */
    public T release(KeyCode... keyCodes);

    /**
     * Writes the given character using a key press/typed/released
     * event sequence, then calls {@link FxThreadUtils#waitForFxEvents()}.
     * @param c The character to write
     */
    public default T write(char c)
    {
        return write(Character.toString(c));
    }

    /**
     * Writes the given String, one character at a time,
     * using a key press/typed/released event sequence,
     * then calls {@link FxThreadUtils#waitForFxEvents()}.
     * @param text The text to write.
     */
    public default T write(String text)
    {
        return write(text, 0);
    }

    /**
     * Writes the given String, one character at a time,
     * using a key press/typed/released event sequence,
     * then calls {@link FxThreadUtils#waitForFxEvents()}.
     * It sleeps for the given number of milliseconds
     * between each character.
     * 
     * @param text The text to write.
     * @param millisecondDelay The number of milliseconds to wait between each character.
     */
    public T write(String text, int millisecondDelay);
}
