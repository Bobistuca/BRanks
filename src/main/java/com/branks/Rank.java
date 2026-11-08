package com.branks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Rank {
    public final String id;
    public volatile String name;
    public volatile String prefix = "";
    public volatile String suffix = "";
    public volatile String color = "";
    public volatile String chatColor = "";
    /** Hierarchy level, from MIN_LEVEL (lowest) to MAX_LEVEL (highest). */
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 2556;
    public volatile int level = MIN_LEVEL;
    public volatile boolean isDefault;
    public final List<String> permissions = new CopyOnWriteArrayList<String>();
    public final List<String> inherits = new CopyOnWriteArrayList<String>();

    public Rank(String id) {
        this.id = id;
        this.name = id;
    }

    /** Display name with the rank color applied. */
    public String coloredName() {
        return Colors.translate(Colors.code(color) + name);
    }
}
