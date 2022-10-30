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
package org.testjavafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A helper class with static methods for dealing with executing pieces of code
 * on the FX thread.
 *
 * <p>Note that when you call a method from this class, the FX toolkit will be
 * initialised if it has not already been initialised, by calling checkToolkitLaunched()
 * The primary Application will be a dummy application launched by this class.
 * If you do not want this, launch the FX toolkit yourself before using this
 * class.
 */
public class FxThreadUtils
{
    /**
     * Executes all the tasks on the FX thread.
     * Monotonic non-null.  Only access using the getter
     */
    private static ExecutorService fxExecutor;

    /** Keep track of whether the dummy app has been launched. */
    private static final AtomicBoolean dummyAppLaunched = new AtomicBoolean(false);
    /** A way to wait for the dummy app to have been started up fully. */
    private static final CompletableFuture<Boolean> dummyAppRunning = new CompletableFuture<>();

    /**
     * Waits for the FX events thread to look idle.
     *
     * <p>This is done by running an instant task on it five times, waiting 15ms
     * between each attempt.  This should generally allow all the pending
     * keyboard and mouse events to be processed, and any pending layouts
     * to be resolved.
     *
     * <p>This can be safely called on the FX thread, but it does nothing in that
     * case -- it effectively assumes that the FX thread is free by definition.
     * However, this means that not all of the pending GUI events may have
     * been processed.
     */
    public static void waitForFxEvents()
    {
        checkToolkitLaunched();
        if (Platform.isFxApplicationThread())
            return;
        Semaphore s = new Semaphore(0);
        for (int i = 0; i < 5; i++)
        {
            try
            {
                Platform.runLater(s::release);
                s.acquire();
                Thread.sleep(15);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private static ExecutorService getFxExecutor()
    {
        if (fxExecutor == null)
        {
            fxExecutor = new AbstractExecutorService()
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
        }
        return fxExecutor;
    }

    /**
     * This class is required to be public by JavaFX, but it is not
     * intended for use by anyone outside this class.
     */
    public static class DummyApplication extends Application
    {
        /** Construct a DummyApplication. */
        public DummyApplication()
        {
        }

        @Override
        public void start(Stage primaryStage) throws Exception
        {
            dummyAppRunning.complete(true);
        }
    }

    /**
     * Makes sure that the FX toolkit is running, if it has not already been
     * launched.  Blocks until it has been launched.
     *
     * <p>Safe to run from the FX thread, although if you're doing that, you'll know
     * that the toolkit has been launched.
     */
    public static void checkToolkitLaunched()
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
        catch (InterruptedException | ExecutionException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Queues the callable to run on the FX thread, but does not wait for it to execute.
     * You can wait for the result using the returned future.
     *
     * <p>>This is safe to call from any thread, including the FX thread, although if you
     * wait for the task immediately on the FX thread then you will get a deadlock because
     * you're blocking the thread it's waiting to run on.
     *
     * @param <T> The return type of the task.  See {@link #asyncFx(Runnable)} if you don't
     *            want to return anything.
     * @param fxTask The task with a return value to execute on the FX thread.
     * @return A future you can wait on for the result of running the task on the FX thread.
     */
    public static <T> Future<T> asyncFx(Callable<T> fxTask)
    {
        return getFxExecutor().submit(fxTask);
    }

    /**
     * Queues the runnable to run on the FX thread, but does not wait for it to execute.
     * You can wait for it to complete using the returned future.
     *
     * <p>>This is safe to call from any thread, including the FX thread, although if you
     * wait for the task immediately on the FX thread then you will get a deadlock because
     * you're blocking the thread it's waiting to run on.
     *
     * @param fxTask The task with no  return value to execute on the FX thread.
     * @return A future you can wait on for the task to have been run on the FX thread.
     */
    public static Future<?> asyncFx(Runnable fxTask)
    {
        return getFxExecutor().submit(fxTask);
    }

    /**
     * Run the given task on the FX thread and wait for it to complete.
     *
     * <p>This is safe to run on any thread.  If called on the FX thread, it executes
     * the task directly and returns the produced value.  If run on another thread,
     * it blocks until the FX thread is free to run the task, then runs it and returns
     * the produced value.
     *
     * @param <T> The return type of the task.  See {@link #syncFx(Runnable)} if you don't
     *            want to return anything.
     * @param fxTask The task to run on the FX thread.
     * @return The value produced by the task.
     */
    public static <T> T syncFx(Callable<T> fxTask)
    {
        checkToolkitLaunched();
        try
        {
            if (Platform.isFxApplicationThread())
                return fxTask.call();
            return getFxExecutor().submit(fxTask).get();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Run the given task on the FX thread and wait for it to complete.
     *
     * <p>This is safe to run on any thread.  If called on the FX thread, it executes
     * the task directly.  If run on another thread, it blocks until the FX thread is
     * free to run the task, then runs it and waits for it to finish.
     *
     * @param fxTask The task to run on the FX thread.
     */
    public static void syncFx(Runnable fxTask)
    {
        checkToolkitLaunched();
        try
        {
            if (Platform.isFxApplicationThread())
                fxTask.run();
            else
                getFxExecutor().submit(fxTask).get();
        }
        catch (ExecutionException | InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
