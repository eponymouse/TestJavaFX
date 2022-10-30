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
package org.testjavafx;

/**
 * Different types of mouse motion that can be used to move the mouse around.
 */
public enum Motion
{
    /**
     * Instantly moves the mouse to the destination, without moving to any
     * intermediate locations.  Fast and can be executed with less thread waiting,
     * but unrealistic because it does not trigger any mouse entered/exited
     * events or anything that activates on hover.
     */
    TELEPORT,
    /**
     * Moves in a straight (potentially diagonal) line between the current position
     * and the destination.  A realistic option but takes longer because it
     * moves through intermediate locations at a realistic speed.
     */
    STRAIGHT_LINE,
    /**
     * Move horizontally to the correct X location, then vertically to the correct
     * Y location.
     */
    HORIZONTAL_FIRST,
    /**
     * Move vertically to the correct Y location, then horizontally to the correct
     * X location.  Particularly useful for activating menus and submenus, as it
     * ensures the mouse doesn't leave the menu on its way between the parent
     * and the child item.
     */
    VERTICAL_FIRST;

    /**
     * The default motion.  Currently {@link #STRAIGHT_LINE}.
     *
     * @return The default mouse motion.
     */
    @SuppressWarnings("checkstyle:MethodName")
    public static Motion DEFAULT()
    {
        return STRAIGHT_LINE;
    }
}
