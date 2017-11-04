package io.github.macfja.mpv.communication.handling;

import com.alibaba.fastjson.JSONObject;

/**
 * An abstract/base implementation of a message handler for events
 *
 * @author MacFJA
 */
abstract public class AbstractEventHandler extends AbstractMessageHandler {
    @Override
    public boolean canHandle(JSONObject message) {
        if (!message.containsKey("event")) {
            return false;
        }
        return canHandle(message.getString("event"));
    }

    /**
     * Indicate if the message can by handled by testing its event name
     *
     * @param eventName The name of the event
     * @return {@code true} if the message can be handled
     */
    abstract public boolean canHandle(String eventName);
}
