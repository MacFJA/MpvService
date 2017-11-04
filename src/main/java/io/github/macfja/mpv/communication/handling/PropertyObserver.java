package io.github.macfja.mpv.communication.handling;

import com.alibaba.fastjson.JSONObject;

/**
 * Abstract PropertyObserver class.
 * Observer of a property changes.
 *
 * @author MacFJA
 */
public abstract class PropertyObserver extends AbstractMessageHandler implements MessageHandlerInterface {
    /**
     * The property observer change group.
     */
    private final Integer id;
    /**
     * The name of the property that is observed
     */
    private final String propertyName;

    /**
     * Create a new property observer for a property name and a group
     *
     * @param propertyName The name of the property to observe
     * @param id           The id of the group to be associated with
     */
    public PropertyObserver(String propertyName, Integer id) {
        this.propertyName = propertyName;
        this.id = id;
    }

    /**
     * Create a new property observer (group defaulted to 1)
     *
     * @param propertyName The name of the property to observe
     */
    public PropertyObserver(String propertyName) {
        this(propertyName, propertyName.hashCode());
    }

    /**
     * Get the property change group id.
     * All property change associated in the group can be manage at the same time (mainly, un-observe at the same time)
     *
     * @return the group id
     */
    public Integer getId() {
        return id;
    }

    /**
     * Get the name of the observed property
     *
     * @return the property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * The method that will be call when the property changed
     *
     * @param propertyName The name of the property
     * @param value        The new property value
     * @param id           The id of the associated group
     */
    abstract public void changed(String propertyName, Object value, Integer id);

    @Override
    public boolean canHandle(JSONObject message) {
        return message.containsKey("event")
                && message.getString("event").equals("property-change")
                && message.getString("name").equals(propertyName)
                && message.get("data") != null
                && message.getIntValue("id") == id;

    }

    @Override
    public Runnable doHandle(final JSONObject message) {
        return new Runnable() {
            @Override
            public void run() {
                changed(
                        message.getString("name"),
                        message.get("data"),
                        message.getInteger("id")
                );
            }
        };
    }

    /**
     * Build a compatible property event
     *
     * @param propertyName The name of the property
     * @param newValue     The new value of the property
     * @param id           The group id of the event
     * @return The event json
     */
    public static JSONObject buildPropertyChangeEvent(String propertyName, Object newValue, int id) {
        return (new JSONObject())
                .fluentPut("event", "property-change")
                .fluentPut("name", propertyName)
                .fluentPut("data", newValue)
                .fluentPut("id", id);
    }
}
