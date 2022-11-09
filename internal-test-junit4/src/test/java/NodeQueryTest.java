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
import org.testjavafx.junit4.ApplicationTest;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.stream.IntStream;

public class NodeQueryTest extends ApplicationTest
{
    private VBox center;
    private VBox bottom;
    private Timeline timeline;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Button goldenButton = new Button("X");
        // A border pane with two vboxes each with an hbox, each with several buttons:
        primaryStage.setScene(new Scene(new BorderPane(
            center = makeVBox(),
            makeVBox(),
            goldenButton,
            bottom = makeVBox(),
            null
        )));
        primaryStage.show();
        timeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {goldenButton.getStyleClass().clear();}),
            new KeyFrame(Duration.seconds(2), e -> {goldenButton.getStyleClass().add("golden");})
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @Override
    public void stop()
    {
        timeline.stop();
    }

    private VBox makeVBox()
    {
        return withStyle("vert", new VBox(
            IntStream.range(0, 10).mapToObj(i ->
                withStyle("horiz horiz-" + i, new HBox(IntStream.range(0, 6).mapToObj(j -> withStyle("but but-" + i + "x" + j, new Button(i + "x" + j))).toArray(Node[]::new)))
            ).toArray(Node[]::new)
        ));
    }

    private <T extends Styleable> T withStyle(String style, T t)
    {
        t.getStyleClass().addAll(style.split(" "));
        return t;
    }
    
    @Test
    public void allButtons()
    {
        MatcherAssert.assertThat(lookup(".but").queryAll(), Matchers.hasSize(180));
    }

    @Test
    public void allButtons00()
    {
        MatcherAssert.assertThat(lookup(".but-0x0").queryAll(), Matchers.hasSize(3));
    }
    
    @Test
    public void someButtons()
    {
        MatcherAssert.assertThat(from(center).lookup(".but").queryAll(), Matchers.hasSize(60));
    }

    @Test
    public void someButtonsFiltered()
    {
        MatcherAssert.assertThat(lookup(".but").filter(n -> ((Button)n).getText().endsWith("3")).queryAll(), Matchers.hasSize(30));
    }

    @Test
    public void someButtons2()
    {
        MatcherAssert.assertThat(from(center, bottom).lookup(".but-2x3").queryAll(), Matchers.hasSize(2));
    }

    @Test
    public void someButtons2B()
    {
        MatcherAssert.assertThat(from(center, bottom).lookup(n -> n instanceof Button && ((Button)n).getText().equals("2x3")).queryAll(), Matchers.hasSize(2));
    }

    @Test
    public void someButtons2C()
    {
        MatcherAssert.assertThat(from(center, bottom).lookup("2x3").queryAll(), Matchers.hasSize(4));
        MatcherAssert.assertThat(from(center, bottom).lookup("2x3").query(), Matchers.instanceOf(Button.class));
    }
    
    @Test
    public void golden()
    {
        for (int i = 0; i < 10; i++)
        {
            MatcherAssert.assertThat("Loop " + i, lookup(".golden").queryWithRetry(), Matchers.notNullValue());
            sleep(1000);
        }
    }

    @Test
    public void golden2()
    {
        for (int i = 0; i < 10; i++)
        {
            long t = System.currentTimeMillis();
            retryUntil(showing(".golden"));
            t = System.currentTimeMillis() - t;
            MatcherAssert.assertThat("Loop " + i, t, Matchers.lessThan(2000L));
            sleep(1000);
        }
    }

    @Test
    public void golden3()
    {
        for (int i = 0; i < 10; i++)
        {
            long t = System.currentTimeMillis();
            retryUntil(not(showing(".golden")));
            t = System.currentTimeMillis() - t;
            MatcherAssert.assertThat("Loop " + i, t, Matchers.lessThan(2000L));
            sleep(1000);
        }
    }
}
