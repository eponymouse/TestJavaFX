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
import com.eponymouse.testjavafx.junit4.ApplicationTest;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.stream.IntStream;

public class NodeQueryTest extends ApplicationTest
{
    private VBox center;
    private VBox bottom;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        // A border pane with two vboxes each with an hbox, each with several buttons:
        primaryStage.setScene(new Scene(new BorderPane(
            center = makeVBox(),
            makeVBox(),
            null,
            bottom = makeVBox(),
            null
        )));
        primaryStage.show();
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
    public void someButtons2()
    {
        MatcherAssert.assertThat(from(center, bottom).lookup(".but-2x3").queryAll(), Matchers.hasSize(2));
    }
}
