// javac complains on modules named junit4, see https://bugs.openjdk.org/browse/JDK-8216185
module org.testjavafx.junitFour {
    exports org.testjavafx.junit4;
    
    requires org.testjavafx.core;
    requires javafx.graphics;
    requires junit;
}
