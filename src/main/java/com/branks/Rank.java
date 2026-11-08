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
