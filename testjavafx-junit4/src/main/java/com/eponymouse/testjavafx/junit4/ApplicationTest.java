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
package com.eponymouse.testjavafx.junit4;

import com.eponymouse.testjavafx.FxThreadUtils;
import com.eponymouse.testjavafx.impl.FxRobot;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;

public abstract class ApplicationTest extends FxRobot
{
    @Before
    public final void internalBefore() throws Exception {
        FxThreadUtils.syncFx(() -> {
            new JUnitApplication(this).start(new Stage());
            return null;
        });
    }

    @After
    public final void internalAfter() throws Exception {
        // TODO release keys and mouse buttons, cleanup windows
        FxThreadUtils.syncFx(this::stop);
    }

    public abstract void start(Stage primaryStage);
    
    public void stop()
    {
    }
}
