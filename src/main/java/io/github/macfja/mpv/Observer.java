package io.github.macfja.mpv;

import com.alibaba.fastjson.JSONObject;

/**
 * Observer interface.
 * The interface that will respond to an Mpv event.
 *
 * @author MacFJA
 */
public interface Observer {
    /**
     * The method that will be call when the event is triggered
     *
     * @param eventName The name of the event
     * @param json      The event object
     */
    void trigger(String eventName, JSONObject json);
}
