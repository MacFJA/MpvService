package io.github.macfja.mpv;

import com.alibaba.fastjson.JSONObject;
import io.github.macfja.mpv.service.Communication;
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
 * Created by dev on 29/03/2017.
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
     * The list of observer
     */
    private Map<String, List<Observer>> observersList = new HashMap<>();
    /**
     * The process that contains the MPV instance
     */
    private Process mpvProcess;
    /**
     * The instance that will communicate with MPV
     */
    protected Communication ioCommunication = new Communication();
    /**
     * The handler for events
     */
    private Communication.EventHandler eventHandler;
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
        ioCommunication.setEventHandler(eventHandler = new Communication.EventHandler() {
            @Override
            public void handle(String eventName, JSONObject eventJson) {
                logger.debug("Start handling: " + eventName);

                if (eventName.equals(waitedEvent)) {
                    logger.debug(" - The event was waited");
                    synchronized (waitedEvent) {
                        waitedEvent.notify();
                    }
                }

                if (!observersList.containsKey(eventName)) {
                    logger.debug(" - No observer for this event");
                    return;
                }
                List<Observer> observers = observersList.get(eventName);
                logger.debug(" - Found " + observers.size() + " observer(s)");

                for (Observer observer : observers) {
                    observer.trigger(eventName, eventJson);
                }
            }
        });
        ioCommunication.setResponseHandler(new Communication.ResponseHandler() {
            @Override
            public boolean canHandle(int requestId) {
                return waitFor.getRequestId() == requestId;
            }

            @Override
            public void handle(JSONObject response) {
                logger.debug("Handling command response with data: " + response.toString());
                waitFor.trigger("", response);
            }
        });
        initialize();
    }

    @Override
    public String sendCommand(String command, List<? extends Serializable> arguments) throws IOException {
        waitFor.setRequestId(ioCommunication.write(command, arguments));

        synchronized (waitFor) {
            int tries = 0;
            while (!waitFor.hasResult()) {
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
        String result = waitFor.getResult();
        waitFor.setRequestId(-1);
        return result;
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
    public void registerEvent(String eventName, Observer observer) {
        List<Observer> observers;
        if (observersList.containsKey(eventName)) {
            observers = observersList.get(eventName);
        } else {
            observers = new ArrayList<>();
        }
        observers.add(observer);
        observersList.put(eventName, observers);
    }

    @Override
    public void registerPropertyChange(PropertyObserver observer) throws IOException {
        registerEvent("property-change", observer);
        sendNonBlockingCommand("observe_property", Arrays.asList(observer.getId(), observer.getPropertyName()));
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
        eventHandler.handle(event.getString("event"), event);
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
            mpvProcess.exitValue();
        } catch (Exception e) {
            mpvProcess.destroy();
            Files.deleteIfExists(Paths.get(socketPath));
        }
    }

    /**
     * Internal observer to get the response of a command
     */
    private class SynchronousSend implements Observer {
        /**
         * The request id.
         * The response must have the same id.
         */
        private int requestId = -1;
        /**
         * The response
         */
        private String result;

        /**
         * Get the request id
         *
         * @return The id
         */
        int getRequestId() {
            return requestId;
        }

        /**
         * Set the request id that we will wait
         *
         * @param requestId The id
         */
        void setRequestId(int requestId) {
            this.requestId = requestId;
            this.result = null;
        }

        /**
         * Get the command response
         *
         * @return The response
         */
        String getResult() {
            return result;
        }

        /**
         * Indicate if we receive the response
         *
         * @return <code>true</code> if we have the result
         */
        boolean hasResult() {
            return result != null;
        }

        @Override
        public synchronized void trigger(String eventName, JSONObject json) {
            result = json.toJSONString();
            notify();
        }
    }
}

