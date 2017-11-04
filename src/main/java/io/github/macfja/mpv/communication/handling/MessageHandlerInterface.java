package io.github.macfja.mpv.communication.handling;

import com.alibaba.fastjson.JSONObject;

/**
 * Interface of a message handler that will work with MPV (JSON) message.
 *
 * @author MacFJA
 */
public interface MessageHandlerInterface {
    /**
     * Indicate if the {@code message} can by handled
     *
     * @param message The message to handle
     * @return {@code true} if the message can be handled
     */
    boolean canHandle(JSONObject message);

    /**
     * Handle the message.
     *
     * @param message The message to handle
     */
    void handle(JSONObject message);
}
