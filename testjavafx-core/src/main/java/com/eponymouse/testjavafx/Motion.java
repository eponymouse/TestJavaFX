package com.eponymouse.testjavafx;

public enum Motion
{
    TELEPORT,
    STRAIGHT_LINE;

    public static Motion DEFAULT()
    {
        return STRAIGHT_LINE;
    }
}
