TestJavaFX
===

Description goes here

Running the tests 
---
By default tests run in headed mode, meaning they run live on your screen and control your keyboard and mouse.  **This is dangerous!**  If a notification pops up, TestJavaFX could click on it and type text into the response.  You probably want to run your tests headless.  (My favourite example of this is a failed test for deleting text that missed the window and clicked on my IDE, pressed Ctrl-A and deleted the source code for that test.  Revenge!)
There are several ways to run the tests headless, described on other pages:

 - <a href="{% link running-with-xvfb.md %}">Running headless on Linux (incl. Github Actions) using Xvfb</a>.
 - TODO running on Monocle

Integrating with test frameworks (JUnit, etc)
---

TODO
