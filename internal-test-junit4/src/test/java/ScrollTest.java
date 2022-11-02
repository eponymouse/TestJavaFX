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
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public class ScrollTest extends ApplicationTest
{
    private ScrollPane scrollPane;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Rectangle bigRect = new Rectangle(10_000, 10_000);
        bigRect.setCache(false);
        scrollPane = new ScrollPane(bigRect);
        scrollPane.setMaxWidth(600);
        scrollPane.setMaxHeight(600);
        primaryStage.setScene(new Scene(scrollPane));
        primaryStage.show();
    }

    @Test
    public void vertTest()
    {
        clickOn(point(scrollPane));
        MatcherAssert.assertThat(scrollPane.getVvalue(), Matchers.equalTo(0.0));
        MatcherAssert.assertThat(scrollPane.getHvalue(), Matchers.equalTo(0.0));
        scroll(SystemUtils.IS_OS_MAC_OSX ? -1 : 1);
        MatcherAssert.assertThat(scrollPane.getVvalue(), Matchers.greaterThan(0.0));
        MatcherAssert.assertThat(scrollPane.getHvalue(), Matchers.equalTo(0.0));
        // Single scroll may not do it so scroll twice to be sure:
        scroll(SystemUtils.IS_OS_MAC_OSX ? 2 : -2);
        MatcherAssert.assertThat(scrollPane.getVvalue(), Matchers.equalTo(0.0));
    }

    @Test
    public void horizTest()
    {
        clickOn(point(scrollPane));
        MatcherAssert.assertThat(scrollPane.getHvalue(), Matchers.equalTo(0.0));
        MatcherAssert.assertThat(scrollPane.getVvalue(), Matchers.equalTo(0.0));
        scrollHorizontal(SystemUtils.IS_OS_MAC_OSX ? -1 : 1);
        MatcherAssert.assertThat(scrollPane.getHvalue(), Matchers.greaterThan(0.0));
        MatcherAssert.assertThat(scrollPane.getVvalue(), Matchers.equalTo(0.0));
    }
}
