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

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.testjavafx.FxThreadUtils;
import org.testjavafx.Motion;
import org.testjavafx.junit4.ApplicationTest;

import java.util.concurrent.atomic.AtomicReference;

public class MenuDialogTest extends ApplicationTest
{
    private Stage parent;
    private Button parentButton;
    private Modality modality = Modality.APPLICATION_MODAL;
    private AtomicReference<String> result = new AtomicReference<>("");
    
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        super.start(primaryStage);
        parent = primaryStage;
        parentButton = new Button("Click me");
        MenuItem open = new MenuItem("Open");
        open.setOnAction(e -> {
            TextInputDialog textInputDialog = new TextInputDialog();
            textInputDialog.initModality(modality);
            textInputDialog.getEditor().getStyleClass().add("fake-file-chooser-dialog");
            if (parent != null)
                textInputDialog.initOwner(parent);
            textInputDialog.setOnShown(ev -> Platform.runLater(() -> textInputDialog.getEditor().requestFocus()));
            result.set(textInputDialog.showAndWait().orElse(null));
        });
        parentButton.setOnAction(e -> {
            new ContextMenu(open).show(parentButton, 0, 0);
        });
        BorderPane root = new BorderPane(parentButton);
        parent.setScene(new Scene(root));
        parent.show();
    }

    @Test
    public void test1()
    {
        MatcherAssert.assertThat(targetWindow(), Matchers.equalTo(FxThreadUtils.syncFx(() -> parentButton.getScene().getWindow())));
        openMenu();
        write("AB");
        tap(KeyCode.ENTER);
        MatcherAssert.assertThat(result.get(), Matchers.equalTo("AB"));
        MatcherAssert.assertThat(targetWindow(), Matchers.equalTo(FxThreadUtils.syncFx(() -> parentButton.getScene().getWindow())));
    }

    private void openMenu()
    {
        clickOn("Click me").moveTo("Open", Motion.HORIZONTAL_FIRST).clickOn();
    }

    @Test
    public void test1b()
    {
        modality = Modality.WINDOW_MODAL;
        test1();
    }

    @Test
    public void test1c()
    {
        modality = Modality.NONE;
        test1();
    }
}
