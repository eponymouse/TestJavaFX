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

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * An internal helper class.
 */
class JUnitApplication extends Application
{
    /** The user application to delegate to. */
    private final ApplicationTest userApplication;

    JUnitApplication(ApplicationTest userApplication)
    {
        this.userApplication = userApplication;
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        userApplication.start(primaryStage);
    }

    @Override
    public void stop() throws Exception
    {
        userApplication.stop();
    }
}
