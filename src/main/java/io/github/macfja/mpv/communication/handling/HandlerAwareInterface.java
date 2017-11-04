package io.github.macfja.mpv.communication.handling;

import java.util.List;

/**
 * Interface to indicate that a class can work with message handler
 *
 * @author MacFJA
 */
public interface HandlerAwareInterface {
    /**
     * Set the handler to use when a message is received
     *
     * @param messageHandler The handler
     */
    void addMessageHandler(MessageHandlerInterface messageHandler);

    /**
     * Remove a handler
     *
     * @param messageHandler The handler to remove
     */
    void removeMessageHandler(MessageHandlerInterface messageHandler);

    /**
     * Get all message handlers
     *
     * @return List of message handlers
     */
    List<MessageHandlerInterface> getMessageHandlers();

    /**
     * Remove all message listener
     */
    void clearMessageHandlers();
}
