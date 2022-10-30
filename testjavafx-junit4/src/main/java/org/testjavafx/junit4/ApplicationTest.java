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
package org.testjavafx.junit4;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.testjavafx.FxRobot;
import org.testjavafx.FxThreadUtils;

/**
 * A base class to use for JUnit 4 testing.  Extend this class,
 * optionally override the start method, and then write @Test
 * methods.  By default in JUnit 4, a new instance of the class
 * will be created for each @Test method (and thus start will
 * be called before each @Test method).
 */
public abstract class ApplicationTest extends FxRobot
{
    /** Internal initialisation of the test.  Called automatically. */
    @Before
    public final void internalBefore()
    {
        FxThreadUtils.syncFx(() -> {
            Platform.setImplicitExit(false);
            new JUnitApplication(this).start(new Stage());
            return null;
        });
        sleep(500);
    }

    /** Interal tidy-up of the tests.  Called automatically. */
    @After
    public final void internalAfter()
    {
        FxThreadUtils.syncFx(() -> {
            release(new KeyCode[0]);
            release(new MouseButton[0]);
        });
        // Let all the release events come through before stopping:
        sleep(500);
        FxThreadUtils.syncFx(this::stop);
        listWindows().forEach(w -> FxThreadUtils.syncFx(w::hide));
    }

    /**
     * Initialise the test class (like {@link javafx.application.Application#start(Stage)}.
     *
     * @param primaryStage A stage you can use for testing
     * @throws Exception Method is allowed to throw checked exceptions.
     */
    public void start(Stage primaryStage) throws Exception
    {
    }

    /**
     * Cleanup after the test.
     */
    public void stop()
    {
    }
}
