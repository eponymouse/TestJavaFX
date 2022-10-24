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

import com.eponymouse.testjavafx.FxRobotInterface;
import com.eponymouse.testjavafx.FxThreadUtils;
import com.google.common.collect.ImmutableList;
import javafx.scene.input.KeyCode;
import javafx.scene.robot.Robot;
import javafx.stage.Window;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class FxRobot implements FxRobotInterface
{
    private final Robot actualRobot = FxThreadUtils.syncFx(Robot::new);
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    
    @Override
    public FxRobotInterface push(KeyCode... keyCodes)
    {
        ImmutableList<KeyCode> order = ImmutableList.copyOf(keyCodes);
        order.forEach(c -> FxThreadUtils.asyncFx(() -> actualRobot.keyPress(c)));
        order.reverse().forEach(c -> FxThreadUtils.asyncFx(() -> actualRobot.keyRelease(c)));
        FxThreadUtils.asyncFx(() -> pressedKeys.removeAll(order));
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface press(KeyCode... keyCodes)
    {
        ImmutableList<KeyCode> order = ImmutableList.copyOf(keyCodes);
        order.forEach(c -> FxThreadUtils.asyncFx(() -> actualRobot.keyPress(c)));
        FxThreadUtils.asyncFx(() -> pressedKeys.addAll(order));
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public FxRobotInterface release(KeyCode... keyCodes)
    {
        ImmutableList<KeyCode> order = keyCodes.length == 0 ? FxThreadUtils.syncFx(() -> ImmutableList.copyOf(pressedKeys)) : ImmutableList.copyOf(keyCodes);
        order.forEach(c -> FxThreadUtils.asyncFx(() -> actualRobot.keyRelease(c)));
        FxThreadUtils.asyncFx(() -> pressedKeys.removeAll(order));
        FxThreadUtils.waitForFxEvents();
        return this;
    }

    @Override
    public List<Window> listWindows()
    {
        return FxThreadUtils.syncFx(() -> ImmutableList.copyOf(Window.getWindows()));
    }
}
