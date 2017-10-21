package io.github.macfja.mpv;

import com.alibaba.fastjson.JSONObject;

/**
 * Abstract PropertyObserver class.
 * Observer of a property changes.
 *
 * @author MacFJA
 */
abstract public class PropertyObserver implements Observer {
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
        this(propertyName, 1);
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
     * Indicate if the received data is valid
     *
     * @param eventData The received data
     * @return <code>true</code> if it's ok
     */
    protected boolean isValid(JSONObject eventData) {
        return eventData.getString("name").equals(propertyName)
                && eventData.get("data") != null
                && eventData.getIntValue("id") == id;
    }

    @Override
    public void trigger(String eventName, JSONObject json) {
        if (!isValid(json)) {
            return;
        }

        changed(
                json.getString("name"),
                json.get("data"),
                json.getInteger("id")
        );
    }

    /**
     * The method that will be call when the property changed
     *
     * @param propertyName The name of the property
     * @param value        The new property value
     * @param id           The id of the associated group
     */
    abstract public void changed(String propertyName, Object value, Integer id);
}
