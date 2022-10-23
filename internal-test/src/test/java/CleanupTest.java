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
import javafx.stage.Stage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

public class CleanupTest extends ApplicationTest
{
    private Stage stage;

    @Override
    public void start(Stage primaryStage)
    {
        // Check previous windows were cleaned up:
        MatcherAssert.assertThat(listWindows(), Matchers.equalTo(ImmutableList.of()));
        this.stage = primaryStage;
        primaryStage.show();
    }

    @Test
    public void test1()
    {
        MatcherAssert.assertThat(listWindows(), Matchers.equalTo(ImmutableList.of(stage)));
    }
    
    @Test
    public void test2()
    {
        MatcherAssert.assertThat(listWindows(), Matchers.equalTo(ImmutableList.of(stage)));
    }
}
