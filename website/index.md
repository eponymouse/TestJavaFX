TestJavaFX
===

Testing programs is a good idea, and modern Continuous Integration (CI) servers like Github Actions or Gitlab CI make it easy to run automated tests.  However, testing GUI program automatically is difficult.  Inspired by an earlier library, <a href="https://github.com/TestFX/TestFX">TestFX</a>, TestJavaFX uses a similar API to support testing <a href="https://openjfx.io/">JavaFX</a> programs.

For example, imagine you want to test writing in a couple of text fields and clicking OK:

    clickOn("#firstname");
    write("Scott");
    tap(KeyCode.TAB);
    write("Summers");
    clickOn("Ok");
    waitUntil(() -> notShowing("Ok"));

For more methods, see the core <a href="/latest-testjavafx-core/org.testjavafx.core/org/testjavafx/FxRobotInterface.html">FxRobotInterface</a> class.

Add dependency
---

TODO Gradle/Maven once released

Javadoc
---

 * <a href="/latest-testjavafx-core/">Core module</a>
 * <a href="/latest-testjavafx-junit4/">JUnit 4 module</a>

Running the tests headless
---
By default tests run in headed mode, meaning they run live on your screen and control your keyboard and mouse.  **This is dangerous!**  If a notification pops up, TestJavaFX could click on it and type text into the response.  You probably want to run your tests headless.  (My favourite example of this is a failed test for deleting text that missed the window and clicked on my IDE, pressed Ctrl-A and deleted the source code for that test.  Revenge!)
There are several ways to run the tests headless, described on other pages:

 - <a href="{% link running-with-xvfb.md %}">Running headless on Linux (incl. Github Actions) using Xvfb (and recording a video of the test)</a>.
 - TODO instructions for running on Monocle

I especially recommend the first option on Linux as the video is very useful for debugging test failures.

Integrating with test frameworks (JUnit, etc)
---

TODO

Comparison to TestFX
---

<a href="https://github.com/TestFX/TestFX">TestFX</a> is an existing fully-featured library for testing JavaFX applications,
although at the time of writing (Oct 2022) it has not been updated recently
(since May 2021).  This library, Test*Java*FX has an API inspired by TestFX.  The primary
differences between this library and TestFX are:

* This library is much clearer on which activities run on or off the JavaFX
  thread and tries to ensure thread safety.  Some parts of TestFX are opaquely thread unsafe (e.g. the lookup function queries the JavaFX scene graph off-thread).
* This library changes some of the window targeting and query functionality to be
  clearer (queries are built then run in one go, not run piecemeal).
* This library (inspired by <a href="https://www.cypress.io/">Cypress</a>) features support for waiting/retrying for prerequisite conditions,
  which helps tests to be less flaky.

At the moment the TestJavaFX API is roughly a subset of TestFX.  Essentially, I implemented the parts I needed for my own projects, and TestFX feels like it has a large API with all possible variants -- in some places I've deliberately implemented a smaller subset to keep the number of methods down.  Feel free to file an issue or pull request if you require more methods from TestFX.
