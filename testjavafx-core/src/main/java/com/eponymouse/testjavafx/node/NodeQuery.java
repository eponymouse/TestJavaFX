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
package com.eponymouse.testjavafx.node;

import javafx.application.Platform;
import javafx.scene.Node;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface NodeQuery
{
    /**
     * Finds the first instance of a Node that matches this query, unsafely
     * casts it to type T and returns it.  
     * @return The first Node found or null if no nodes are found.
     * @param <T>
     */
    public <T extends Node> T query();

    public <T extends Node> List<T> queryAll();
    
    public NodeQuery lookup(String query);
    
    public NodeQuery lookup(Predicate<Node> nodePredicate);
    
    public default <T extends Node> Optional<T> tryQuery()
    {
        return Optional.ofNullable(query());
    }

    /**
     * Like query but if no such node is found, it is retried every 100ms
     * for 5 seconds.  Either the first found node in that period is returned,
     * or null if there was still no such node after 5 seconds.
     * 
     * This method is useful if you want to locate a node that may appear in
     * response to a GUI event that you just triggered, and you want to avoid
     * an arbitrary sleep to wait for the loading.
     * 
     * Note that the method is safe to call on the FX thread, but in that case
     * it will not retry, it will instead act like calling query()
     * 
     * @return
     * @param <T>
     */
    public default  <T extends Node> T queryWithRetry()
    {
        T t = query();
        if (!Platform.isFxApplicationThread())
        {
            for (int retries = 50; t == null && retries >= 0; retries--)
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                }
                t = query();
            }
        }
        return t;
    }
}
