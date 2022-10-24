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

import com.eponymouse.testjavafx.FxThreadUtils;
import com.eponymouse.testjavafx.junit4.ApplicationTest;
import com.google.common.collect.ImmutableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.ArrayList;

public class ClickTest extends ApplicationTest
{
    private final ArrayList<Button> buttons = new ArrayList<>();
    private final ArrayList<Integer> clicks = new ArrayList<>();
    
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        super.start(primaryStage);
        for (int i = 0; i < 10; i++)
        {
            Button button = new Button("Button " + i);
            button.setId("button-" + i);
            int iFinal = i;
            button.setOnAction(e -> clicks.add(iFinal));
            buttons.add(button);
        }
        primaryStage.setScene(new Scene(new VBox(buttons.toArray(Node[]::new))));
        primaryStage.show();
    }

    @Test
    public void testClickDirect()
    {
        MatcherAssert.assertThat(getClicks(), Matchers.empty());
        clickOn(buttons.get(9));
        clickOn(buttons.get(2));
        MatcherAssert.assertThat(getClicks(), Matchers.equalTo(ImmutableList.of(9, 2)));
    }

    @Test
    public void testClickLookup()
    {
        MatcherAssert.assertThat(getClicks(), Matchers.empty());
        clickOn("#button-3");
        clickOn("#button-7");
        MatcherAssert.assertThat(getClicks(), Matchers.equalTo(ImmutableList.of(3, 7)));
    }

    private ImmutableList<Integer> getClicks()
    {
        return FxThreadUtils.syncFx(() -> ImmutableList.copyOf(clicks));
    }
}
