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
import javafx.stage.Stage;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class StartTest extends ApplicationTest
{
    private final AtomicInteger startCount = new AtomicInteger(0);
    private final AtomicInteger stopCount = new AtomicInteger(0);
    
    @Override
    public void start(Stage primaryStage)
    {
        startCount.incrementAndGet();
    }

    @Override
    public void stop()
    {
        stopCount.incrementAndGet();
    }

    @Test
    public void test1()
    {
        assertEquals(1, startCount.get());
        assertEquals(0, stopCount.get());
    }

    @Test
    public void test2()
    {
        // Count should still be one as we should be the same instance and only started once:
        assertEquals(1, startCount.get());
        // We should not have been stopped:
        assertEquals(0, stopCount.get());
    }
}
