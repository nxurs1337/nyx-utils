package dev.nyx.utils.event.impl.entity.player;

import dev.nyx.utils.event.Event;

public class TickEvent extends Event {
    public static class Post extends TickEvent { }

    public static class Pre extends TickEvent { }
}
