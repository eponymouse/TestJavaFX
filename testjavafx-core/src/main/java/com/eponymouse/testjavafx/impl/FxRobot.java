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
package com.eponymouse.testjavafx.impl;

import com.eponymouse.testjavafx.FxRobotInterface;
import com.eponymouse.testjavafx.FxThreadUtils;
import com.google.common.collect.ImmutableList;
import javafx.scene.input.KeyCode;
import javafx.scene.robot.Robot;

import java.util.concurrent.ExecutionException;

public class FxRobot implements FxRobotInterface
{
    private final Robot actualRobot;
    {
        try
        {
            actualRobot = FxThreadUtils.syncFx(Robot::new);
        }
        catch (ExecutionException | InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void push(KeyCode... keyCodes)
    {
        ImmutableList<KeyCode> order = ImmutableList.copyOf(keyCodes);
        order.forEach(actualRobot::keyPress);
        order.reverse().forEach(actualRobot::keyRelease);
    }
}
