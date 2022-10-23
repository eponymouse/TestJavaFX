TestJavaFX
===

TestJavaFX is a library to help with testing JavaFX applications.

**This library is currently in the very early stages of development and is not yet ready for use.**

Related libraries
---

TestFX is an existing fully-featured library for testing JavaFX applications, 
although at the time of writing (Oct 2022) it has not been updated recently
(since May 2021).  This library has an API inspired by TestFX.  The primary 
differences between this library and TestFX are:

 * This library is much clearer on which activities run on or off the JavaFX 
   thread.
 * This library features support for waiting/retrying for prerequisite conditions,
   which helps tests to be less flaky.
