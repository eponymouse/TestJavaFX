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

import javafx.scene.input.KeyCode;

public interface FXRobotInterfaceKeyboard
{
    /**
     * Presses all the keys in order, then releases
     * them all in reverse order, then waits for
     * the FX events thread.
     */
    public void push(KeyCode... keyCodes);

    /**
     * Presses all the keys in the given order, then waits for
     * the FX events thread.
     */
    public void press(KeyCode... keyCodes);

    /**
     * Releases all the keys in the given order, then waits for
     * the FX events thread.
     */
    public void release(KeyCode... keyCodes);
}
