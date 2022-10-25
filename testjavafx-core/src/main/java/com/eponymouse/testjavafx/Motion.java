package com.eponymouse.testjavafx;

public enum Motion
{
    TELEPORT,
    STRAIGHT_LINE,
    HORIZONTAL_FIRST,
    VERTICAL_FIRST;

    public static Motion DEFAULT()
    {
        return STRAIGHT_LINE;
    }
}
