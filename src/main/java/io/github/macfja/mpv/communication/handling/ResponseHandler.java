package io.github.macfja.mpv.communication.handling;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * An abstract/base implementation of a message handler to handle result of a command
 *
 * @author MacFJA
 */
public abstract class ResponseHandler extends AbstractMessageHandler {
    @Override
    public boolean canHandle(JSONObject message) {
        if (!message.containsKey("request_id")) {
            return false;
        }
        return canHandle(message.getInteger("request_id"));
    }

    /**
     * Indicate if the provided requestId is handled by this handler
     *
     * @param requestId The is of the request associated with the response
     * @return {@code true} if the handler can work with this request id
     */
    abstract public boolean canHandle(Integer requestId);

    /**
     * Test if the result of a command is a success or not
     *
     * @param rawResult The textual raw result to check
     * @return {@code true} if the result is a success
     */
    public static boolean isResultSuccess(String rawResult) {
        return isResultSuccess(JSON.parseObject(rawResult));
    }

    /**
     * Test if the result of a command is a success or not
     *
     * @param result The JSONObject to check
     * @return {@code true} if the result is a success
     */
    public static boolean isResultSuccess(JSONObject result) {
        return result.containsKey("error") && result.getString("error").equals("success");
    }
}
