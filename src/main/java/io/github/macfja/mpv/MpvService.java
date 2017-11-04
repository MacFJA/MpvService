package io.github.macfja.mpv;

import com.alibaba.fastjson.JSONObject;
import io.github.macfja.mpv.communication.handling.NamedEventHandler;
import io.github.macfja.mpv.communication.handling.PropertyObserver;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * MpvService Interface.
 * Define the list of available methods.
 *
 * @author MacFJA
 */
public interface MpvService extends Closeable {
    /**
     * Send a command to Mpv
     *
     * @param command   The command to send
     * @param arguments The command arguments
     * @return The raw result of the command
     * @throws IOException if an I/O error occurs
     */
    String sendCommand(String command, List<? extends Serializable> arguments) throws IOException;

    /**
     * Send a command to Mpv (don't wait for the result)
     *
     * @param command   The command to send
     * @param arguments The command arguments
     * @throws IOException if an I/O error occurs
     */
    void sendNonBlockingCommand(String command, List<? extends Serializable> arguments) throws IOException;

    /**
     * Set the value of a Mpv string property
     *
     * @param name  The property name
     * @param value The value
     * @return The raw result
     * @throws IOException if an I/O error occurs
     */
    String setProperty(String name, String value) throws IOException;

    /**
     * Set the value of a Mpv boolean property
     *
     * @param name  The property name
     * @param value The value
     * @return The raw result
     * @throws IOException if an I/O error occurs
     */
    String setProperty(String name, Boolean value) throws IOException;

    /**
     * Get the value of a Mpv property
     *
     * @param name The property name
     * @return The raw result
     * @throws IOException if an I/O error occurs
     */
    String getProperty(String name) throws IOException;

    /**
     * Get the value of a Mpv property
     *
     * @param name The property name
     * @param type The classname of the value data type
     * @param <T>  The classname of the value data type
     * @return The property result
     * @throws IOException if an I/O error occurs
     */
    <T> T getProperty(String name, Class<T> type) throws IOException;

    /**
     * Register an event listener
     *
     * @param observer The observer to call when the event is triggered
     */
    void registerEvent(NamedEventHandler observer);

    /**
     * Register to a Mpv property changes
     *
     * @param observer The property observer that will be call
     * @throws IOException if an I/O error occurs
     */
    void registerPropertyChange(PropertyObserver observer) throws IOException;

    /**
     * Un-register to a Mpv property changes
     *
     * @param observer The property observer that will be call
     * @throws IOException if an I/O error occurs
     */
    void unregisterPropertyChange(PropertyObserver observer) throws IOException;

    /**
     * Un-register all property changes for a property
     *
     * @param propertyName The name of the property
     * @throws IOException if an I/O error occurs
     */
    void unregisterPropertyChange(String propertyName) throws IOException;

    /**
     * Fire a simple event
     *
     * @param eventName The event name
     */
    void fireEvent(String eventName);

    /**
     * Fire a complex event
     *
     * @param eventName The event name
     * @param data      The data associated with the event
     */
    void fireEvent(String eventName, JSONObject data);

    /**
     * Fire a complex event
     *
     * @param event The event Json Object
     */
    void fireEvent(JSONObject event);

    /**
     * Wait for an event to occurs.
     * This method will block the current thread until the event occurs.
     * A default timeout is defined to 1 second.
     *
     * @param eventName The name of the event to wait
     */
    void waitForEvent(String eventName);

    /**
     * Wait for an event to occurs.
     * This method will block the current thread until the event occurs.
     *
     * @param eventName The name of the event to wait
     * @param timeout   The amount of millisec to wait before considering that the event will not occur
     */
    void waitForEvent(String eventName, int timeout);
}
