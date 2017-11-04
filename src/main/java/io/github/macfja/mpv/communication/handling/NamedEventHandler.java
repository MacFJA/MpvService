package io.github.macfja.mpv.communication.handling;

/**
 * An abstract/base implementation of a message handler for specific event name.
 *
 * @author MacFJA
 */
abstract public class NamedEventHandler extends AbstractEventHandler {
    /**
     * The name of the event to handle
     */
    private String eventName;

    /**
     * Constructor.
     *
     * @param eventName The name of the event to handle
     */
    public NamedEventHandler(String eventName) {
        this.eventName = eventName;
    }

    @Override
    public boolean canHandle(String eventName) {
        return eventName.equals(this.eventName);
    }
}
