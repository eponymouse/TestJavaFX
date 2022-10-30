// javac complains on modules named junit4, see https://bugs.openjdk.org/browse/JDK-8216185
module com.eponymouse.testjavafx.junitFour {
    exports com.eponymouse.testjavafx.junit4;
    
    requires com.eponymouse.testjavafx.core;
    requires javafx.graphics;
    requires junit;
}
