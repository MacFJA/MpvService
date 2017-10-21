package io.github.macfja.mpv.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * Utility class.
 */
final public class Utils {
    /**
     * Test if the result of a command is a success or not
     *
     * @param rawResult The textual raw result to check
     * @return <code>true</code> if the result is a success
     */
    public static boolean isResultSuccess(String rawResult) {
        return isResultSuccess(JSON.parseObject(rawResult));
    }

    /**
     * Test if the result of a command is a success or not
     *
     * @param result The JSONObject to check
     * @return <code>true</code> if the result is a success
     */
    public static boolean isResultSuccess(JSONObject result) {
        return result.containsKey("error") && result.getString("error").equals("success");
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
