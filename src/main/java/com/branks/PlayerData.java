package com.branks;

public class PlayerData {
    public volatile String name;
    /** Id of the manually assigned rank (null = use the default rank). */
    public volatile String rank;
    /** Id of the tag the player selected (null = none). */
    public volatile String tag;

    public PlayerData(String name) {
        this.name = name;
    }
}
