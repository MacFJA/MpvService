package io.github.macfja.mpv.communication.handling;

import com.alibaba.fastjson.JSONObject;

/**
 * An abstract class for message handler that allow parallel work.
 * The handling of a message is launch in a new thread
 *
 * @author MacFJA
 */
public abstract class AbstractMessageHandler implements MessageHandlerInterface {
    @Override
    public void handle(JSONObject message) {
        Runnable worker = doHandle(message);

        if (worker == null) {
            return;
        }
        new Thread(worker).start();
    }

    /**
     * The actual message handling
     *
     * @param message The message to process
     * @return A runnable code (can be {@code null})
     */
    abstract public Runnable doHandle(JSONObject message);
}
