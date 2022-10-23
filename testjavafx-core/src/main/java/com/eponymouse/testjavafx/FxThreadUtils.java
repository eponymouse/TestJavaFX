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
package com.eponymouse.testjavafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FxThreadUtils
{
    private final static ExecutorService fxExecutor = new AbstractExecutorService()
    {        
        @Override
        public void shutdown()
        {
        }

        @Override
        public List<Runnable> shutdownNow()
        {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown()
        {
            return false;
        }

        @Override
        public boolean isTerminated()
        {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
        {
            return false;
        }

        @Override
        public void execute(Runnable command)
        {
            checkToolkitLaunched();
            Platform.runLater(command);
        }
    };


    private final static AtomicBoolean dummyAppLaunched = new AtomicBoolean(false);
    private final static CompletableFuture<Boolean> dummyAppRunning = new CompletableFuture<>();

    public static class DummyApplication extends Application
    {
        public DummyApplication()
        {
        }

        @Override
        public void start(Stage primaryStage) throws Exception
        {
            dummyAppRunning.complete(true);
        }
    }
    
    private static void checkToolkitLaunched()
    {
        if (!dummyAppLaunched.getAndSet(true))
        {
            // Initialise FX toolkit:
            new Thread(() -> Application.launch(DummyApplication.class)).start();
        }
        try
        {
            dummyAppRunning.get();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Queues the callable to run on the FX thread, but does not wait for it to execute.
     * You can wait for the result using the returned future.
     * @param fxTask
     * @return
     * @param <T>
     */
    public static <T> Future<T> asyncFx(Callable<T> fxTask)
    {
        return fxExecutor.submit(fxTask);
    }

    public static Future<?> asyncFx(Runnable fxTask)
    {
        return fxExecutor.submit(fxTask);
    }

    public static <T> T syncFx(Callable<T> fxTask) throws ExecutionException, InterruptedException
    {
        return fxExecutor.submit(fxTask).get();
    }

    public static void syncFx(Runnable fxTask) throws ExecutionException, InterruptedException
    {
        fxExecutor.submit(fxTask).get();
    }
}
