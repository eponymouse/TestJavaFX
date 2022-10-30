import org.testjavafx.FxRobot;

/**
 * This is the core module of TestJavaFX.  TestJavaFX is a library that helps
 * you fake keyboard events and mouse clicks and perform GUI queries in order
 * to test applications made with <a href="https://openjfx.io/">the JavaFX GUI 
 * framework</a>.
 * 
 * To use TestJavaFX in JUnit 4, we recommend using the JUnit 4 module.  To
 * use the framework generically, create an instance of (and/or extend) the
 * {@link FxRobot} class.
 */
module org.testjavafx.core {
    exports org.testjavafx;
 
    requires com.google.common;
    requires javafx.controls;
    requires javafx.graphics;
    requires org.apache.commons.lang3;
}
