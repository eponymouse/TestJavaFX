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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.testjavafx.FxThreadUtils;
import org.testjavafx.junit4.ApplicationTest;

import java.util.Optional;

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

    @Test
    public void retryFromOffThread2()
    {
        for (int i = 0; i < 10; i++)
        {
            this.retryUntilPresent(() -> lookup(".present").tryQuery());
            // Sleep an awkward amount of time to end up somewhere else in the animation loop:
            sleep(1234);
        }
    }

    @Test
    public void retryOnThread2()
    {
        for (int i = 0; i < 10; i++)
        {
            FxThreadUtils.syncFx(() -> this.retryUntilPresent(() -> lookup(".present").tryQuery()));
            // Sleep an awkward amount of time to end up somewhere else in the animation loop:
            sleep(1234);
        }
    }

    @Test
    public void retryFromOffThread3()
    {
        for (int i = 0; i < 10; i++)
        {
            this.retryUntilNonNull(() -> lookup(".present").query());
            // Sleep an awkward amount of time to end up somewhere else in the animation loop:
            sleep(1234);
        }
    }

    @Test
    public void retryOnThread3()
    {
        for (int i = 0; i < 10; i++)
        {
            FxThreadUtils.syncFx(() -> this.retryUntilNonNull(() -> lookup(".present").query()));
            // Sleep an awkward amount of time to end up somewhere else in the animation loop:
            sleep(1234);
        }
    }

    private void checkTimingAndException(Runnable r)
    {
        long start = System.currentTimeMillis();
        try
        {
            r.run();
            Assert.fail("No exception from failed retry");
        }
        catch (RuntimeException e)
        {
            long end = System.currentTimeMillis();
            MatcherAssert.assertThat((end - start), Matchers.allOf(Matchers.greaterThanOrEqualTo(8000L), Matchers.lessThanOrEqualTo(12000L)));
        }
        catch (Throwable t)
        {
            Assert.fail("Unexpected exception type: " + t);
        }
    }
    
    @Test
    public void retryFail1a()
    {
        checkTimingAndException(() -> retryUntil(() -> false));
    }

    @Test
    public void retryFail1b()
    {
        checkTimingAndException(() -> retryUntilNonNull(() -> null));
    }

    @Test
    public void retryFail1c()
    {
        checkTimingAndException(() -> retryUntilPresent(() -> Optional.empty()));
    }

    @Test
    public void retryFail2a()
    {
        checkTimingAndException(() -> FxThreadUtils.syncFx(() -> retryUntil(() -> false)));
    }

    @Test
    public void retryFail2b()
    {
        checkTimingAndException(() -> FxThreadUtils.syncFx(() -> retryUntilNonNull(() -> null)));
    }

    @Test
    public void retryFail2c()
    {
        checkTimingAndException(() -> FxThreadUtils.syncFx(() -> retryUntilPresent(() -> Optional.empty())));
    }
}
