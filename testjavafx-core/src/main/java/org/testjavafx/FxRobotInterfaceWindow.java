/*
 * TestJavaFX: Testing for JavaFX applications
 * Copyright (c) TestFX contributors 2013-2019 and Neil Brown, 2022.
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

import com.google.common.collect.ImmutableList;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public interface FxRobotInterfaceWindow<T extends FxRobotInterfaceWindow<T>>
{
    public default List<Window> listTargetWindows()
    {
        return fetchWindowsByProximityTo(focusedWindow());
    }
    
    private List<Window> fetchWindowsByProximityTo(Window targetWindow)
    {
        List<Window> windows = listWindows();
        return orderWindowsByProximityTo(targetWindow, windows);
    }

    private List<Window> orderWindowsByProximityTo(Window targetWindow, List<Window> windows)
    {
        List<Window> copy = new ArrayList<>(windows);
        copy.sort(Comparator.comparingInt(w -> calculateWindowProximityTo(targetWindow, w)));
        return Collections.unmodifiableList(copy);
    }

    private int calculateWindowProximityTo(Window targetWindow, Window window)
    {
        if (window == targetWindow)
        {
            return 0;
        }
        if (isOwnerOf(window, targetWindow))
        {
            return 1;
        }
        return 2;
    }

    private boolean isOwnerOf(Window window, Window targetWindow)
    {
        Window ownerWindow = retrieveOwnerOf(window);
        if (ownerWindow == targetWindow)
        {
            return true;
        }
        return ownerWindow != null && isOwnerOf(ownerWindow, targetWindow);
    }

    private Window retrieveOwnerOf(Window window)
    {
        if (window instanceof Stage)
        {
            return ((Stage) window).getOwner();
        }
        if (window instanceof PopupWindow)
        {
            return ((PopupWindow) window).getOwnerWindow();
        }
        return null;
    }

    /**
     * Gets a list of all currently showing JavaFX windows.
     * 
     * This method is safe to call on the FX thread.  If
     * called on another thread it waits to fetch the list
     * from the FX thread, and will block if the FX thread
     * is busy.
     */
    public default List<Window> listWindows()
    {
        return FxThreadUtils.syncFx(() -> ImmutableList.copyOf(Window.getWindows()));
    }

    public Window focusedWindow();
}
