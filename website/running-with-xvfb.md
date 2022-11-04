---
title: "TestJavaFX"
---

Running with Xvfb
===

Xvfb is a headless X server that's available on Linux.  It is a useful way to run JavaFX tests on Linux, especially in CI systems like Github Actions.

You can use xvfb-run but it's sometimes awkward to wrap that around your gradle run, e.g. if using gradle-build-action on Github Actions.  For this reason I find it easier to start Xvfb before the tests.  As an example of a Github Actions task, you can run:

    - name: Initialise
      run: | 
        sudo apt-get update
        sudo apt-get install xvfb icewm
        Xvfb :42 -screen 0 1920x1200x24 &
        sleep 5
        DISPLAY=:42.0 icewm &
    - name: Build with Gradle
      uses: gradle/gradle-build-action
      env:
        DISPLAY: ":42.0"

The choice of display is arbitrary but needs to match across all three places.  I have found that parts of JavaFX testing (e.g. menu screen positions) can be buggy without a window manager, hence the use of `icewm` as a simple window manager.  The sleep is needed to let `Xvfb` startup properly before launching `icewm`.

An example full configuration is available (including the screen recording described next) in TestJavaFX itself: <a href="https://github.com/eponymouse/TestJavaFX/blob/main/.github/workflows/build-and-test.yaml">build-and-test.yaml</a>. 

Recording video of the tests
---

If you run your tests on a remote system (e.g. with Github Actions) then it can be frustrating to debug test failures.  Sometimes the failure will not reproduce on your machine or it may take a long time to run.  It is useful in this case to see what happened exactly during the test.  Xvfb is good for this as it allows us to record the screen with ffmpeg.

To do this, add the following lines to the initialise task shown above:
    
    sudo apt install ffmpeg
    ffmpeg -nostdin -y -video_size 1920x1200 -framerate 8 -f x11grab -i :42.0 -codec:v libx264rgb -preset ultrafast recording.mp4 > ffmpeg.log 2>&1 &

This records at 8 FPS with minimal compression to avoid using too much CPU.  It also saves the log in case you want it.  On Github Actions to see the video afterwards I upload it as an archive.  It is crucial to stop FFMPEG first as otherwise the video file will be corrupt:

      - name: Stop recording
        if: always()
        run: |
          killall -SIGINT ffmpeg
          sleep 5
      - name: Archive video
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: screen-recordings
          path: |
            **/*.mp4

This stops all FFMPEG instances on the system.  The sleep gives FFMPEG time to finish up.  The `if: always()` is essential because technically the task before (the Gradle build) will have failed, but we still (especially!) want the recording if a test has failed.


Running in parallel
---

There is ultimately only one keyboard and mouse input system per X server-screen.  If you run tests in parallel, they will all fight over the mouse and keyboard.  For this reason it's impossible to run *headed* tests in parallel but it is possible to run tests in parallel with `Xvfb`.  To do this, you will either need to run multiple Xvfb or multiple screens on the Xvfb server.

For example, to run multiple screens you can adjust the above to:

        Xvfb :42 -screen 0 1920x1200x24 -screen 1 1920x1200x24 &
        sleep 5
        DISPLAY=:42.0 icewm &
        DISPLAY=:42.1 icewm &

And so on.  In your build you can hardcode the display variable or dynamically work it out.  E.g. in Gradle you might run:

    def allSubprojects = subprojects
    subprojects { subproject ->
         test {
             maxParallelForks = 1
             int index = allSubprojects.collect {p -> p.name}.sort().indexOf(subproject.name)
             environment "DISPLAY", ":42." + index
         }
    }

You can also get Gradle or your build system to do the Xvfb launching if you prefer.  Feel free to send a pull request with a working example if you have one.