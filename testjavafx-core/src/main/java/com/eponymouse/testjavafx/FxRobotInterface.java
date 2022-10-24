/*
 * TestJavaFX: Testing for JavaFX applications
 * Copyright (c) Neil Brown, 2022.
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
package com.eponymouse.testjavafx;

import com.eponymouse.testjavafx.node.NodeQuery;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.stage.Window;

import java.util.List;

public interface FxRobotInterface extends FXRobotInterfaceKeyboard<FxRobotInterface>, FxRobotInterfaceMouse<FxRobotInterface>
{
    public List<Window> listWindows();

    public NodeQuery lookup(String query);

    public Point2D point(Node node);

    public default Point2D point(String query)
    {
        return point(lookup(query).queryWithRetry());
    }
}
