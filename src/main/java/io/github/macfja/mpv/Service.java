package io.github.macfja.mpv;

import com.alibaba.fastjson.JSONObject;
import io.github.macfja.mpv.communication.Communication;
import io.github.macfja.mpv.communication.handling.AbstractEventHandler;
import io.github.macfja.mpv.communication.handling.AbstractMessageHandler;
import io.github.macfja.mpv.communication.handling.MessageHandlerInterface;
import io.github.macfja.mpv.communication.handling.NamedEventHandler;
import io.github.macfja.mpv.communication.handling.PropertyObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The default/base implementation of MpvService.
 *
 * @author MacFJA
 */
public class Service implements MpvService {
    /**
     * The internal observer
     */
    private final SynchronousSend waitFor = new SynchronousSend();
    /**
     * Indicate if the class is ready to use
     */
    protected boolean isInitialized = false;
    /**
     * The path to path to the MPV communication socket
     */
    private String socketPath;
    /**
     * The path to the MPV binary
     */
    private String mpvPath;
    /**
     * The process that contains the MPV instance
     */
    private Process mpvProcess;
    /**
     * The instance that will communicate with MPV
     */
    protected Communication ioCommunication = new Communication();
    /**
     * The name of the event that we will wait
     *
     * @see Service#waitForEvent(String, int)
     * @see Service#waitForEvent(String)
     */
    private String waitedEvent;
    /**
     * The class logger
     */
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The constructor.
     * Path to MPV defaulted to "mpv"
     */
    public Service() {
        this("mpv");
    }

    /**
     * The class constructor.
     *
     * @param mpvPath Path to MPV binary
     */
    public Service(String mpvPath) {
        this.mpvPath = mpvPath;

        socketPath = System.getProperty("java.io.tmpdir") + this.getClass().getName();
        ioCommunication.setSocketPath(socketPath);
        ioCommunication.addMessageHandler(new AbstractEventHandler() {
            @Override
            public boolean canHandle(String eventName) {
                return eventName.equals(waitedEvent);
            }

            @Override
            public Runnable doHandle(JSONObject message) {
                final String eventName = message.getString("event");

                if (!eventName.equals(waitedEvent)) {
                    return null;
                }

                return new Runnable() {
                    @Override
                    public void run() {
                        logger.debug(" - The event was waited");
                        synchronized (waitedEvent) {
                            waitedEvent.notify();
                        }
                    }
                };
            }
        });
        ioCommunication.addMessageHandler(waitFor);
        initialize();
    }

    @Override
    public String sendCommand(String command, List<? extends Serializable> arguments) throws IOException {
        int requestId;
        waitFor.addRequest(requestId = ioCommunication.write(command, arguments));

        synchronized (waitFor) {
            int tries = 0;
            while (!waitFor.hasResult(requestId)) {
                try {
                    waitFor.wait(500);
                    tries++;

                    if (tries > 10) {// We wait for more than 5sec
                        logger.warn("Timeout for response of " + command + " / " + arguments.toString());
                        waitFor.notify();
                        break;
                    }
                } catch (InterruptedException e) {
                    logger.warn("Response waiting interrupted for " + command + " / " + arguments.toString(), e);
                    waitFor.notify();
                    break;
                }
            }
        }
        return waitFor.getResult(requestId);
    }


    @Override
    public void sendNonBlockingCommand(String command, List<? extends Serializable> arguments) throws IOException {
        ioCommunication.write(command, arguments);
    }

    @Override
    public void waitForEvent(String eventName) {
        waitForEvent(eventName, 1000);
    }

    @Override
    public void waitForEvent(String eventName, int timeout) {
        waitedEvent = eventName;
        synchronized (waitedEvent) {
            try {
                waitedEvent.wait(timeout);
            } catch (InterruptedException e) {
                logger.error("Error while waiting for an event", e);
            } finally {
                waitedEvent = null;
            }
        }
    }

    /**
     * Start all needed process
     */
    protected void initialize() {
        ProcessBuilder pb = new ProcessBuilder(Arrays.asList(mpvPath, "--idle=yes", "--force-window=no", "--input-ipc-server=" + socketPath));
        try {
            mpvProcess = pb.start();
            Thread.sleep(500);
            ioCommunication.open();
            isInitialized = true;
        } catch (IOException | InterruptedException e) {
            logger.error("Unable to start Mpv", e);
            isInitialized = false;
        }
    }

    @Override
    public String setProperty(String name, String value) throws IOException {
        return sendCommand("set_property", Arrays.asList(name, value));
    }

    @Override
    public String setProperty(String name, Boolean value) throws IOException {
        return sendCommand("set_property", Arrays.asList(name, value));
    }

    @Override
    public String getProperty(String name) throws IOException {
        return sendCommand("get_property", Collections.singletonList(name));
    }

    @Override
    public <T> T getProperty(String name, Class<T> type) throws IOException {
        String result = getProperty(name);
        return JSONObject.parseObject(result).getObject("data", type);
    }

    @Override
    public void registerEvent(NamedEventHandler observer) {
        ioCommunication.addMessageHandler(observer);
    }

    @Override
    public void registerPropertyChange(PropertyObserver observer) throws IOException {
        if (!hasPropertyObserver(observer.getPropertyName(), observer.getId())) {
            sendCommand("observe_property", Arrays.asList(observer.getId(), observer.getPropertyName()));
        }
        ioCommunication.addMessageHandler(observer);
    }

    @Override
    public void unregisterPropertyChange(PropertyObserver observer) throws IOException {
        ioCommunication.removeMessageHandler(observer);
        if (!hasPropertyObserver(observer.getPropertyName(), observer.getId())) {
            sendNonBlockingCommand("unobserve_property", Collections.singletonList(observer.getId()));
        }
    }

    @Override
    public void unregisterPropertyChange(String propertyName) throws IOException {
        List<PropertyObserver> toRemove = new ArrayList<>();
        for (MessageHandlerInterface messageHandler : ioCommunication.getMessageHandlers()) {
            if (messageHandler instanceof PropertyObserver
                    && ((PropertyObserver) messageHandler).getPropertyName().equals(propertyName)) {
                toRemove.add((PropertyObserver) messageHandler);
            }
        }
        for (PropertyObserver item : toRemove) {
            ioCommunication.removeMessageHandler(item);
        }
    }

    /**
     * Check if a property have at least one observer
     *
     * @param propertyName The name of the property to check
     * @param groupId      The id of the group to be associated with
     * @return {@code true} if an observer exist
     */
    private boolean hasPropertyObserver(String propertyName, int groupId) {
        for (MessageHandlerInterface handler : ioCommunication.getMessageHandlers()) {
            if (
                    handler instanceof PropertyObserver
                            && ((PropertyObserver) handler).getPropertyName().equals(propertyName)
                            && ((PropertyObserver) handler).getId().equals(groupId)
                    ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void fireEvent(String eventName) {
        fireEvent(eventName, null);
    }

    @Override
    public void fireEvent(String eventName, JSONObject data) {
        JSONObject object = new JSONObject();
        object.put("event", eventName);
        if (data != null) {
            object.put("data", data);
        }
        fireEvent(object);
    }

    @Override
    public void fireEvent(JSONObject event) {
        ioCommunication.simulateMessage(event);
    }


    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    @Override
    public void close() throws IOException {
        ioCommunication.close();

        try {
            if (mpvProcess != null) {
                mpvProcess.exitValue();
            }
        } catch (Exception e) {
            mpvProcess.destroy();
        } finally {
            Files.deleteIfExists(Paths.get(socketPath));
        }
    }

    /**
     * Internal observer to get the response of a command
     */
    private class SynchronousSend extends AbstractMessageHandler {
        /**
         * List of all request/response waited and received (but not yet retrieved)
         */
        private Map<Integer, String> data = new HashMap<>();

        /**
         * Add a new waited response
         *
         * @param requestId The associated request id
         */
        void addRequest(int requestId) {
            data.put(requestId, null);
        }

        /**
         * Check if a request id is waited or not
         *
         * @param requestId The request id to check
         * @return {@code true} if a response with this request id is waited
         */
        boolean hasRequest(int requestId) {
            return data.containsKey(requestId);
        }

        /**
         * Get the command response
         *
         * @param requestId The id of the request to look for
         * @return The response. Return {@code null} if no response or the request id is not found
         */
        String getResult(int requestId) {
            if (!data.containsKey(requestId)) {
                return null;
            }
            return data.remove(requestId);
        }

        /**
         * Indicate if we receive the response
         *
         * @param requestId The id of the request to look for
         * @return {@code true} if we have the result
         */
        boolean hasResult(int requestId) {
            return data.containsKey(requestId) && data.get(requestId) != null;
        }

        @Override
        public boolean canHandle(JSONObject message) {
            return message.containsKey("request_id") && hasRequest(message.getIntValue("request_id"));
        }

        @Override
        synchronized public Runnable doHandle(final JSONObject message) {
            int requestId = message.getIntValue("request_id");
            if (hasRequest(requestId)) {
                data.put(requestId, message.toJSONString());
                notify();
            }
            return null;
        }
    }
}
