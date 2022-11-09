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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.Test;
import org.testjavafx.FxThreadUtils;
import org.testjavafx.junit4.ApplicationTest;

public class RetryTest extends ApplicationTest
{
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        super.start(primaryStage);
        Button changingButton = new Button("X");
        // A border pane with two vboxes each with an hbox, each with several buttons:
        primaryStage.setScene(new Scene(new Group(changingButton)));
        primaryStage.show();
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {changingButton.getStyleClass().clear();}),
            new KeyFrame(Duration.seconds(2), e -> {changingButton.getStyleClass().add("present");})
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @Test
    public void retryFromOffThread()
    {
        for (int i = 0; i < 10; i++)
        {
            this.retryUntil(() -> lookup(".present").tryQuery().isPresent());
            // Sleep an awkward amount of time to end up somewhere else in the animation loop:
            sleep(1234);
        }
    }

    @Test
    public void retryOnThread()
    {
        for (int i = 0; i < 10; i++)
        {
            FxThreadUtils.syncFx(() -> this.retryUntil(() -> lookup(".present").tryQuery().isPresent()));
            // Sleep an awkward amount of time to end up somewhere else in the animation loop:
            sleep(1234);
        }
    }
}
