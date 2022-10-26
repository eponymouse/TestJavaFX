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
import com.eponymouse.testjavafx.FxThreadUtils;
import com.eponymouse.testjavafx.Motion;
import com.eponymouse.testjavafx.junit4.ApplicationTest;
import com.google.common.collect.ImmutableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.ArrayList;

public class MoveTest extends ApplicationTest
{
    private final ArrayList<Point2D> path = new ArrayList<>();
    private final ArrayList<Point2D> dragPath = new ArrayList<>();
    private Point2D lastPress = null;
    private Point2D lastRelease = null;
    
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        GridPane gridPane = new GridPane();
        for (int i = 0; i < 10; i++)
        {
            for (int j = 0; j < 10; j++)
            {
                gridPane.add(makeSquare(i, j), i, j);
            }
        }
        primaryStage.setScene(new Scene(gridPane));
        // Get mouse out of the way before showing window:
        moveTo(Point2D.ZERO, Motion.TELEPORT);
        primaryStage.show();
    }

    private Node makeSquare(int i, int j)
    {
        Rectangle r = new Rectangle(50, 50);
        r.getStyleClass().addAll("x" + i, "y" + j);
        Point2D us = new Point2D(i, j);
        r.setOnDragDetected(e -> {r.startFullDrag();});
        r.setOnMouseEntered(e -> path.add(us));
        r.setOnMouseDragEntered(e -> dragPath.add(us));
        r.setOnMousePressed(e -> {this.lastPress = us;});
        r.setOnMouseDragReleased(e -> {this.lastRelease = us;});
        return r;
    }
    
    @Test
    public void testHorizontal()
    {
        moveTo(".x3.y7", Motion.TELEPORT);
        MatcherAssert.assertThat(getPath(), Matchers.equalTo(ImmutableList.of(new Point2D(3, 7))));
        moveTo(".x4.y7", Motion.STRAIGHT_LINE);
        MatcherAssert.assertThat(getPath(), Matchers.equalTo(ImmutableList.of(new Point2D(3, 7), new Point2D(4, 7))));
        moveTo(".x6.y7", Motion.STRAIGHT_LINE);
        MatcherAssert.assertThat(getPath(), Matchers.equalTo(ImmutableList.of(
                new Point2D(3, 7),
                new Point2D(4, 7),
                new Point2D(5, 7),
                new Point2D(6, 7))));
    }

    @Test
    public void testDiagonal()
    {
        moveTo(".x3.y7", Motion.TELEPORT);
        MatcherAssert.assertThat(getPath(), Matchers.equalTo(ImmutableList.of(new Point2D(3, 7))));
        moveTo(".x5.y2", Motion.STRAIGHT_LINE);
        MatcherAssert.assertThat(getPath(), Matchers.equalTo(ImmutableList.of(
                new Point2D(3, 7),
                new Point2D(3, 6),
                new Point2D(4, 6),
                new Point2D(4, 5),
                new Point2D(4, 4),
                new Point2D(5, 3),
                new Point2D(5, 2))));
        MatcherAssert.assertThat(getDragPath(), Matchers.empty());
    }

    @Test
    public void testDiagonalDrag()
    {
        moveTo(".x3.y7", Motion.TELEPORT);
        MatcherAssert.assertThat(getPath(), Matchers.equalTo(ImmutableList.of(new Point2D(3, 7))));
        MatcherAssert.assertThat(getDragPath(), Matchers.empty());
        drag();
        dropTo(point(".x5.y2"));
        MatcherAssert.assertThat(FxThreadUtils.syncFx(() -> lastPress), Matchers.equalTo(new Point2D(3, 7)));
        MatcherAssert.assertThat(FxThreadUtils.syncFx(() -> lastRelease), Matchers.equalTo(new Point2D(5, 2)));
        MatcherAssert.assertThat(getDragPath(), Matchers.equalTo(ImmutableList.of(
            //new Point2D(3, 7), Source node won't feature
            new Point2D(3, 6),
            new Point2D(4, 6),
            new Point2D(4, 5),
            new Point2D(4, 4),
            new Point2D(5, 3),
            new Point2D(5, 2))));
    }

    @Test
    public void testHorizFirst()
    {
        moveTo(".x2.y7", Motion.TELEPORT);
        MatcherAssert.assertThat(getPath(), Matchers.equalTo(ImmutableList.of(new Point2D(2, 7))));
        moveTo(".x5.y4", Motion.HORIZONTAL_FIRST);
        MatcherAssert.assertThat(getPath(), Matchers.equalTo(ImmutableList.of(
                new Point2D(2, 7),
                new Point2D(3, 7),
                new Point2D(4, 7),
                new Point2D(5, 7),
                new Point2D(5, 6),
                new Point2D(5, 5),
                new Point2D(5, 4))));
    }

    @Test
    public void testVertFirst()
    {
        moveTo(".x2.y7", Motion.TELEPORT);
        MatcherAssert.assertThat(getPath(), Matchers.equalTo(ImmutableList.of(new Point2D(2, 7))));
        moveTo(".x5.y4", Motion.VERTICAL_FIRST);
        MatcherAssert.assertThat(getPath(), Matchers.equalTo(ImmutableList.of(
                new Point2D(2, 7),
                new Point2D(2, 6),
                new Point2D(2, 5),
                new Point2D(2, 4),
                new Point2D(3, 4),
                new Point2D(4, 4),
                new Point2D(5, 4))));
    }
    
    private ImmutableList<Point2D> getPath()
    {
        return FxThreadUtils.syncFx(() -> ImmutableList.copyOf(path));
    }

    private ImmutableList<Point2D> getDragPath()
    {
        return FxThreadUtils.syncFx(() -> ImmutableList.copyOf(dragPath));
    }
}
