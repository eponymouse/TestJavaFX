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
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.ArrayMatching;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class KeyboardTest extends ApplicationTest
{
    private Stage stage;
    private final ArrayList<KeyEvent> keyEvents = new ArrayList<>();

    @Override
    public void start(Stage primaryStage)
    {
        // Add a delay between tests to avoid old events coming through:
        sleep(1000);
        
        this.stage = primaryStage;
        this.stage.setScene(new Scene(new Region()));
        this.stage.getScene().addEventFilter(KeyEvent.ANY, e -> {
            synchronized (this)
            {
                keyEvents.add(e);
            }
        });
        this.stage.show();
    }
    
    public synchronized void stop()
    {
        keyEvents.clear();
    }
    
    public synchronized KeyEvent[] getKeyEvents()
    {
        return this.keyEvents.toArray(KeyEvent[]::new);
    }
    
    private static String elaborate(String s)
    {
        return "\"" + s + "\" [" + s.codePoints().mapToObj(Integer::toString).collect(Collectors.joining(", ")) + "]";
    }
    
    public static Matcher<KeyEvent> isKey(KeyCode keyCode)
    {
        return new CustomTypeSafeMatcher<KeyEvent>("Has key " + keyCode)
        {
            @Override
            public boolean matchesSafely(KeyEvent actual)
            {
                return actual.getCode() == keyCode;
            }

            @Override
            protected void describeMismatchSafely(KeyEvent item, Description mismatchDescription)
            {
                mismatchDescription.appendText("had key ").appendValue(item.getCode());
            }
        };
    }

    public static Matcher<KeyEvent> isText(String text)
    {
        return new CustomTypeSafeMatcher<KeyEvent>("Has text " + elaborate(text))
        {
            @Override
            public boolean matchesSafely(KeyEvent actual)
            {
                return Objects.equals(actual.getText(), text);
            }

            @Override
            protected void describeMismatchSafely(KeyEvent item, Description mismatchDescription)
            {
                mismatchDescription.appendText("had text " + elaborate(item.getText()));
            }
        };
    }

    public static Matcher<KeyEvent> isCharacter(String character)
    {
        return new CustomTypeSafeMatcher<KeyEvent>("Has character " + elaborate(character))
        {
            @Override
            public boolean matchesSafely(KeyEvent actual)
            {
                return Objects.equals(actual.getCharacter(), character);
            }

            @Override
            protected void describeMismatchSafely(KeyEvent item, Description mismatchDescription)
            {
                mismatchDescription.appendText("had character " + elaborate(item.getCharacter()));
            }
        };
    }

    public static Matcher<Event> isType(EventType<?> eventType)
    {
        return new CustomTypeSafeMatcher<Event>("Has type " + eventType)
        {
            @Override
            public boolean matchesSafely(Event actual)
            {
                return actual.getEventType() == eventType;
            }

            @Override
            protected void describeMismatchSafely(Event item, Description mismatchDescription)
            {
                mismatchDescription.appendText("had type ").appendValue(item.getEventType());
            }
        };
    }

    @Test
    public void testSingleKey()
    {
        push(KeyCode.A);
        MatcherAssert.assertThat(getKeyEvents(), ArrayMatching.arrayContaining(
            Matchers.allOf(isKey(KeyCode.A), isCharacter("\0"), isText("a"), isType(KeyEvent.KEY_PRESSED)),
            Matchers.allOf(isKey(KeyCode.UNDEFINED), isCharacter("a"), isText(""), isType(KeyEvent.KEY_TYPED)),
            Matchers.allOf(isKey(KeyCode.A), isCharacter("\0"), isText("a"), isType(KeyEvent.KEY_RELEASED))
        ));
    }

    @Test
    public void testSingleKey2()
    {
        press(KeyCode.A);
        release(KeyCode.A);
        MatcherAssert.assertThat(getKeyEvents(), ArrayMatching.arrayContaining(
                Matchers.allOf(isKey(KeyCode.A), isCharacter("\0"), isText("a"), isType(KeyEvent.KEY_PRESSED)),
                Matchers.allOf(isKey(KeyCode.UNDEFINED), isCharacter("a"), isText(""), isType(KeyEvent.KEY_TYPED)),
                Matchers.allOf(isKey(KeyCode.A), isCharacter("\0"), isText("a"), isType(KeyEvent.KEY_RELEASED))
        ));
    }

    @Test
    public void testSingleNonTypableKey()
    {
        push(KeyCode.ESCAPE);
        MatcherAssert.assertThat(getKeyEvents(), ArrayMatching.arrayContaining(
            Matchers.allOf(isKey(KeyCode.ESCAPE), isCharacter("\0"), Matchers.anyOf(isText(Character.toString(27)), isText("")), isType(KeyEvent.KEY_PRESSED)),
            Matchers.allOf(isKey(KeyCode.UNDEFINED), Matchers.anyOf(isCharacter(""), isCharacter(Character.toString(27))), isText(""), isType(KeyEvent.KEY_TYPED)),
            Matchers.allOf(isKey(KeyCode.ESCAPE), isCharacter("\0"), Matchers.anyOf(isText(Character.toString(27)), isText("")), isType(KeyEvent.KEY_RELEASED))
        ));
    }

    @Test
    public void testMultipleKeys1()
    {
        push(KeyCode.SHIFT, KeyCode.A);
        MatcherAssert.assertThat(getKeyEvents(), ArrayMatching.arrayContaining(
                Matchers.allOf(isKey(KeyCode.SHIFT), isCharacter("\0"), isText(""), isType(KeyEvent.KEY_PRESSED)),
                Matchers.allOf(isKey(KeyCode.A), isCharacter("\0"), Matchers.anyOf(isText("A"), isText("a")), isType(KeyEvent.KEY_PRESSED)),
                Matchers.allOf(isKey(KeyCode.UNDEFINED), isCharacter("A"), isText(""), isType(KeyEvent.KEY_TYPED)),
                Matchers.allOf(isKey(KeyCode.A), isCharacter("\0"), Matchers.anyOf(isText("A"), isText("a")), isType(KeyEvent.KEY_RELEASED)),
                Matchers.allOf(isKey(KeyCode.SHIFT), isCharacter("\0"), isText(""), isType(KeyEvent.KEY_RELEASED))
        ));
    }

    @Test
    public void testMultipleKeys2()
    {
        push(KeyCode.A, KeyCode.SHIFT);
        MatcherAssert.assertThat(getKeyEvents(), ArrayMatching.arrayContaining(
                Matchers.allOf(isKey(KeyCode.A), isCharacter("\0"), isText("a"), isType(KeyEvent.KEY_PRESSED)),
                Matchers.allOf(isKey(KeyCode.UNDEFINED), isCharacter("a"), isText(""), isType(KeyEvent.KEY_TYPED)),
                Matchers.allOf(isKey(KeyCode.SHIFT), isCharacter("\0"), isText(""), isType(KeyEvent.KEY_PRESSED)),
                Matchers.allOf(isKey(KeyCode.SHIFT), isCharacter("\0"), isText(""), isType(KeyEvent.KEY_RELEASED)),
                Matchers.allOf(isKey(KeyCode.A), isCharacter("\0"), isText("a"), isType(KeyEvent.KEY_RELEASED))
        ));
    }
}
