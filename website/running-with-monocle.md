---
title: "TestJavaFX"
---

Running with Monocle
===

Monocle is a JavaFX platform for running on tiny devices without a windowing system.  However it can also be (mis-)used to run headless tests on other operating systems.

Gluon do currently ship up to date Monocle builds for all platforms except Linux, but these artifacts are not on Maven/Gradle.  Therefore the easiest way to use it is to depend on TestFX's old Monocle build of JavaFX 12.  To do this, add this dependency to Gradle:

    testRuntimeOnly 'org.testfx:openjfx-monocle:jdk-12.0.1+2'

Then you will need to add some extra Java properties to your test task.  I usually make this conditional on a Gradle headless property:

    test {
        // .. any other config you have ..
        if ("true".equals(project.findProperty("headless"))) {
            jvmArgs += ['-Dtestjavafx.headless=true',
                        '-Dprism.order=sw',
                        '-Dprism.text=t2k',
                        '-Dglass.platform=Monocle',
                        '-Dmonocle.platform=Headless',
                        '-Dheadless.geometry=1920x1200-32']
        }
    }

You can vary the geometry, which is the size of the headless screen.

Running in parallel
---

Monocle runs one GUI per JVM, much like headed JavaFX.  If you want to run multiple Monocle tests in parallel you will need to run them in separate JVMs.  It may be worth exploring the Gradle option <a href="https://docs.gradle.org/current/dsl/org.gradle.api.tasks.testing.Test.html#org.gradle.api.tasks.testing.Test:maxParallelForks">maxParallelForks</a> for doing this. 

