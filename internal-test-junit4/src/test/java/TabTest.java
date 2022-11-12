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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.scene.control.Skinnable;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.testjavafx.FxThreadUtils;
import org.testjavafx.junit4.ApplicationTest;

public class TabTest extends ApplicationTest
{
    private TextField field1;
    private TextField field2;
    private Modality modality = Modality.APPLICATION_MODAL;
    private boolean showPopups = false;
    private TextField field3;
    private TextField field4;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        super.start(primaryStage);
        field1 = new TextField("A");
        field1.getStyleClass().add("field1");
        field2 = new TextField("B");
        field2.getStyleClass().add("field2");
        
        field3 = new TextField("C");
        field3.getStyleClass().add("field3");
        field4 = new TextField("D");
        field4.getStyleClass().add("field4");
        
        Button showDialog = new Button("Show dialog");
        showDialog.setOnAction(e -> {
            Dialog<Void> d = new Dialog();
            d.initOwner(primaryStage);
            d.initModality(modality);
            d.getDialogPane().setContent(new VBox(
                field3,
                field4
            ));
            d.getDialogPane().getButtonTypes().setAll(ButtonType.OK);
            if (showPopups)
            {
                showPopupOnFocus(field3);
                showPopupOnFocus(field4);
            }
            d.showAndWait();
        });
        primaryStage.setScene(new Scene(new VBox(field1, field2, showDialog)));
        primaryStage.show();
    }

    private static void showPopupOnFocus(final TextField field)
    {
        field.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                if (newValue)
                {
                    PopupControl popupControl = new PopupControl();
                    popupControl.setSkin(new Skin<Skinnable>()
                    {
                        @Override
                        public Skinnable getSkinnable()
                        {
                            return popupControl;
                        }

                        @Override
                        public Node getNode()
                        {
                            return new Label("Label for " + field);
                        }

                        @Override
                        public void dispose()
                        {
                        }
                    });
                    popupControl.show(field, 0, 0);
                }
            }
        });
    }

    @Test
    public void test()
    {
        Assert.assertTrue(FxThreadUtils.syncFx(field1::isFocused));
        Assert.assertFalse(FxThreadUtils.syncFx(field2::isFocused));
        tap(KeyCode.TAB);
        Assert.assertFalse(FxThreadUtils.syncFx(field1::isFocused));
        Assert.assertTrue(FxThreadUtils.syncFx(field2::isFocused));
        tap(KeyCode.TAB);
        Assert.assertFalse(FxThreadUtils.syncFx(field1::isFocused));
        Assert.assertFalse(FxThreadUtils.syncFx(field2::isFocused));
        // Go past button and back round:
        tap(KeyCode.TAB);
        Assert.assertTrue(FxThreadUtils.syncFx(field1::isFocused));
        Assert.assertFalse(FxThreadUtils.syncFx(field2::isFocused));
        MatcherAssert.assertThat(FxThreadUtils.syncFx(() -> field1.getText()), Matchers.equalTo("A"));
        MatcherAssert.assertThat(FxThreadUtils.syncFx(() -> field2.getText()), Matchers.equalTo("B"));
    }

    @Test
    public void testDialog()
    {
        clickOn("Show dialog");
        // Focus starts on button:
        Assert.assertFalse(FxThreadUtils.syncFx(field3::isFocused));
        Assert.assertFalse(FxThreadUtils.syncFx(field4::isFocused));
        tap(KeyCode.TAB);
        Assert.assertTrue(FxThreadUtils.syncFx(field3::isFocused));
        Assert.assertFalse(FxThreadUtils.syncFx(field4::isFocused));
        tap(KeyCode.TAB);
        Assert.assertFalse(FxThreadUtils.syncFx(field3::isFocused));
        Assert.assertTrue(FxThreadUtils.syncFx(field4::isFocused));
        // Back to button:
        tap(KeyCode.TAB);
        Assert.assertFalse(FxThreadUtils.syncFx(field3::isFocused));
        Assert.assertFalse(FxThreadUtils.syncFx(field4::isFocused));
        // Go past button and back round:
        tap(KeyCode.TAB);
        Assert.assertTrue(FxThreadUtils.syncFx(field3::isFocused));
        Assert.assertFalse(FxThreadUtils.syncFx(field4::isFocused));
        MatcherAssert.assertThat(FxThreadUtils.syncFx(() -> field3.getText()), Matchers.equalTo("C"));
        MatcherAssert.assertThat(FxThreadUtils.syncFx(() -> field4.getText()), Matchers.equalTo("D"));
    }

    @Test
    public void testDialogWindowModal()
    {
        modality = Modality.WINDOW_MODAL;
        testDialog();
    }

    @Test
    public void testDialogNonModal()
    {
        modality = Modality.NONE;
        testDialog();
    }

    @Test
    public void testDialogWithPopup()
    {
        showPopups = true;
        testDialog();
    }

    @Test
    public void testDialogWindowModalWithPopup()
    {
        showPopups = true;
        testDialogWindowModal();
    }

    @Test
    public void testDialogNonModalWithPopup()
    {
        showPopups = true;
        testDialogNonModal();
    }
}
