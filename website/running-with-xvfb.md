---
title: "TestJavaFX"
---

Running with Xvfb
===

Xvfb is a headless X server that's available on Linux.  It is a useful way to run JavaFX tests on Linux, especially in CI systems like Github Actions.

You can use xvfb-run but it's sometimes awkward to wrap that around your gradle run.  For this reason I find it easier to start Xvfb before the tests.  As an example of a Github Actions task, you can run:

    - name: Initialise
      run: | 
        sudo apt-get update
        sudo apt-get install xvfb sawfish
        Xvfb :42 -screen 0 1920x1200x24 &
        sleep 5
        sawfish --display=:42.0 &
    - name: Build with Gradle
      uses: gradle/gradle-build-action
      env:
        DISPLAY: ":42.0"

The choice of display is arbitrary but needs to match across all three places.  I have found that parts of JavaFX testing (e.g. menu screen positions) can be buggy without a window manager, hence the use of `sawfish` as a simple window manager.  (I also tried `icewm` but found that it could crash at certain points while running JavaFX.)  The sleep is needed to let `Xvfb` startup properly before launching `sawfish`.

An example full configuration is available (including the screen recording described next) in TestJavaFX itself: <a href="https://github.com/eponymouse/TestJavaFX/blob/main/.github/workflows/build-and-test.yaml">build-and-test.yaml</a>. 

Recording video of the tests
---

If you run your tests on a remote system (e.g. with Github Actions) then it can be frustrating to debug test failures.  Sometimes the failure will not reproduce on your machine or it may take a long time to run.  It is useful in this case to see what happened exactly during the test.  Xvfb is good for this as it allows us to record the screen with ffmpeg.

To do this, add the following lines to the initialise task shown above:
    
    sudo apt-get install ffmpeg
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

An extra argument to FFMPEG I found useful to line up the video with times in my log files with debug output from the test is:

    -vf "drawtext=text='%{localtime\:%T}':x=100:y=50:fontcolor=white:fontsize=30"

This draws the local time on the video so you can easily find the part of the video that matches the timestamp of a particular log message.


Running in parallel
---

There is ultimately only one keyboard and mouse input system per X server-screen.  If you run tests in parallel, they will all fight over the mouse and keyboard.  For this reason it's impossible to run *headed* tests in parallel but it is possible to run tests in parallel with Monocle or `Xvfb`.  To do this with Xvfb, you will need to run multiple Xvfb instances.

You can hardcode the number of displays, but it's more scalable to get your Gradle build to start and stop new Xvfb, sawfish and ffmpeg instances as needed.  To do this (when the `-Pxvfb=true` flag is passed to Gradle), I added this to my Gradle build in the top-level file:

    def allSubprojects = subprojects
    subprojects { subproject ->
      // .. other config ..
      test {
        // .. other config ..
        maxParallelForks = 1
      
        int index = allSubprojects.collect {p -> p.name}.sort().indexOf(subproject.name)
        int display = 42 + index

        if (index >= 0 && "true".equals(project.findProperty("xvfb"))) {
          environment "DISPLAY", ":" + display + ".0"
        }
      
        doFirst {
          if (index >= 0 && "true".equals(project.findProperty("xvfb"))) {
            new ProcessBuilder("bash", "manage-xvfb-screens.sh", "start", Integer.toString(display), subproject.name).start().waitFor()
          }
        }

      }

      // Can't use test.doLast because it doesn't run if the test fails:  
      task stopXvfb {
        doLast {
          int index = allSubprojects.collect {p -> p.name}.sort().indexOf(subproject.name)
          int display = 42 + index
          if (index >= 0 && "true".equals(project.findProperty("xvfb"))) {
            new ProcessBuilder("bash", "manage-xvfb-screens.sh", "stop", Integer.toString(display), subproject.name).start().waitFor()
          }
        }
      }
      test.finalizedBy(stopXvfb)
    }

Then the file `manage-xvfb-screens.sh` is as follows:

    case $1 in
      start)
        Xvfb :"$2" -screen 0 1280x1024x24 &
        echo $! > processes-"$2"-xvfb.pid
        sleep 5
        sawfish --display=:"$2".0 &
        echo $! > processes-"$2"-sawfish.pid
        sleep 5
        ffmpeg -nostdin -y -video_size 1280x1024 -framerate 8 -f x11grab -i :"$2".0 -codec:v libx264rgb -preset ultrafast -vf "drawtext=text='%{localtime\:%T}':x=100:y=50:fontcolor=white:fontsize=30" recording-"$3".mp4 > "$3".out.ffmpeg.log 2> "$3".err.ffmpeg.log &
        echo $! > processes-"$2"-ffmpeg.pid
        ;;
      stop)
        # Tell FFMPEG to stop recording:
        kill -SIGINT $(cat processes-"$2"-ffmpeg.pid)
        # Give it time to finish:
        sleep 10
        kill $(cat processes-"$2"-sawfish.pid)
        sleep 5
        kill $(cat processes-"$2"-xvfb.pid)
      ;;
    esac

With this the videos will be named `recording-foo.mp4`, where `foo` is the name of a particular submodule.
