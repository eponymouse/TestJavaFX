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
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public class TypingTest extends ApplicationTest
{
    private TextField field;

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        field = new TextField();
        primaryStage.setScene(new Scene(field));
        primaryStage.show();
        primaryStage.requestFocus();
        field.requestFocus();
    }
    
    @Test
    public void testSimple()
    {
        testString("Simple");
    }

    @Test
    public void testAccent()
    {
        testString("Foö");
    }
    
    @Test
    public void testMove()
    {
        write("bar");
        push(KeyCode.HOME);
        write("foo");
        MatcherAssert.assertThat(getText(), Matchers.equalTo("foobar"));
    }

    private void testString(String s)
    {
        write(s);
        MatcherAssert.assertThat(getText(), Matchers.equalTo(s));
    }

    private String getText()
    {
        return FxThreadUtils.syncFx(() -> field.getText());
    }
}
