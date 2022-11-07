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
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.testjavafx.FxThreadUtils;
import org.testjavafx.junit4.ApplicationTest;

import java.util.concurrent.Callable;

public class DialogTest extends ApplicationTest
{
    private Stage parent;
    private TextField parentField;
    private TextField childField;
    private Modality modality = Modality.APPLICATION_MODAL;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        super.start(primaryStage);
        parent = primaryStage;
        childField = new TextField();
        parentField = new TextField();
        showPopupOnFocus(parentField);
        showPopupOnFocus(childField);
        parent.setScene(new Scene(new Group(parentField)));
        parent.setOnShown(e -> parentField.requestFocus());
        parentField.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                Dialog<Void> d = new Dialog<Void>();
                d.initOwner(parent);
                d.initModality(modality);
                d.getDialogPane().setContent(new Group(childField));
                // Unnecessary when there's no buttons:
                //d.setOnShown(e -> Platform.runLater(childField::requestFocus));
                d.showAndWait();
            }
        });
        parent.show();
    }

    private void showPopupOnFocus(TextField f)
    {
        Popup p = new Popup();
        p.getContent().add(new Label("Popup"));
        f.focusedProperty().addListener(new ChangeListener<Boolean>()
            {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
                {
                    p.show(f.getScene().getWindow());
                }
            }
        );
    }

    @Test
    public void test1()
    {
        write("A");
        write("B");
        write("C");
        MatcherAssert.assertThat(FxThreadUtils.syncFx((Callable<String>)parentField::getText), Matchers.equalTo("A"));
        MatcherAssert.assertThat(FxThreadUtils.syncFx((Callable<String>)childField::getText), Matchers.equalTo("BC"));
    }
    
    @Test
    public void test2()
    {
        write("ABC");
        MatcherAssert.assertThat(FxThreadUtils.syncFx((Callable<String>)parentField::getText), Matchers.equalTo("A"));
        MatcherAssert.assertThat(FxThreadUtils.syncFx((Callable<String>)childField::getText), Matchers.equalTo("BC"));
    }

    @Test
    public void test1b()
    {
        modality = Modality.WINDOW_MODAL;
        write("A");
        write("B");
        write("C");
        MatcherAssert.assertThat(FxThreadUtils.syncFx((Callable<String>)parentField::getText), Matchers.equalTo("A"));
        MatcherAssert.assertThat(FxThreadUtils.syncFx((Callable<String>)childField::getText), Matchers.equalTo("BC"));
    }

    @Test
    public void test2b()
    {
        modality = Modality.WINDOW_MODAL;
        write("ABC");
        MatcherAssert.assertThat(FxThreadUtils.syncFx((Callable<String>)parentField::getText), Matchers.equalTo("A"));
        MatcherAssert.assertThat(FxThreadUtils.syncFx((Callable<String>)childField::getText), Matchers.equalTo("BC"));
    }
}
