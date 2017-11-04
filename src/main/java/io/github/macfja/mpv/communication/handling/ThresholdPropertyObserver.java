package io.github.macfja.mpv.communication.handling;

import com.alibaba.fastjson.JSONObject;

import java.util.Date;

/**
 * Abstract ThresholdPropertyObserver class.
 * Observer of a property changes, only if the change don't occurs outside a given frequency.
 *
 * @author MacFJA
 */
abstract public class ThresholdPropertyObserver extends PropertyObserver {
    /**
     * The interval of seconds between two allowed change notification
     */
    private final Float seconds;
    /**
     * Last valid notification time
     */
    private Long lastExecutionTime;

    /**
     * Constructor
     *
     * @param seconds      The amount of seconds between two valid change notification
     * @param propertyName The name of the property to observer
     * @param id           The id of the group to be associated with
     */
    public ThresholdPropertyObserver(Float seconds, String propertyName, Integer id) {
        super(propertyName, id);
        this.seconds = seconds;
    }

    /**
     * Create a new property observer (group defaulted to 1)
     *
     * @param seconds      The amount of seconds between two valid change notification
     * @param propertyName The name of the property to observe
     */
    public ThresholdPropertyObserver(Float seconds, String propertyName) {
        super(propertyName);
        this.seconds = seconds;
    }

    @Override
    public boolean canHandle(JSONObject message) {
        boolean eventMatching = super.canHandle(message);

        return eventMatching && inSchedule();
    }

    /**
     * Indicate if the minimum time between two notification is respected.
     *
     * @return <p>{@code true} if the previous valid notification occurs at least {@code seconds} ago,
     *         or if the observer was never call</p>
     */
    private boolean inSchedule() {
        return lastExecutionTime == null
                || (new Date()).getTime() - lastExecutionTime > seconds * 1000;
    }

    @Override
    public void handle(JSONObject message) {
        if (inSchedule()) {
            lastExecutionTime = (new Date()).getTime();
            super.handle(message);
        }
    }
}
