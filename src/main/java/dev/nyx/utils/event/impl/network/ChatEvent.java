package dev.nyx.utils.event.impl.network;

import dev.nyx.utils.event.Event;

public class ChatEvent extends Event {
    private String content;

    public ChatEvent(String content) {
        this.content = content;
    }

    public String getMessage() {
        return content;
    }

    /** Lets listeners rewrite the outgoing message before it is sent. */
    public void setMessage(String content) {
        this.content = content;
    }
}
