package io.github.macfja.mpv.service;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Communication class.
 * This class do the actual sending and receiving of data to and from MPV instance.
 *
 * @author MacFJA
 */
public class Communication implements Closeable {
    /**
     * The process used to write and read data.
     */
    private Process ioSocket;
    /**
     * The writer
     */
    private BufferedWriter ioWriter;
    /**
     * Indicate if we should send a quite command to MPV when the <code>close</code> method is call.
     */
    private boolean exitOnClose = true;
    /**
     * Indicate if the writer and listener are ready to use
     */
    private boolean initialized = false;
    /**
     * The path to the socket that MPV listen
     */
    private String socketPath;
    /**
     * The handler for command response
     */
    private ResponseHandler responseHandler;
    /**
     * The handler for event send by MPV
     */
    private EventHandler eventHandler;
    /**
     * The class logger
     */
    protected Logger logger = LoggerFactory.getLogger(getClass());

    private ScheduledExecutorService queueProcessor = new ScheduledThreadPoolExecutor(1);

    /**
     * Change the behavior when the <code>close</code> method is call.
     * If <code>true</code> then a <code>quit</code> command will be sent to MPV
     *
     * @param exitOnClose The flag
     */
    public void setExitOnClose(boolean exitOnClose) {
        this.exitOnClose = exitOnClose;
    }

    /**
     * Set the path to the socket that MPV listen
     *
     * @param socketPath The path
     */
    public void setSocketPath(String socketPath) {
        this.socketPath = socketPath;
    }

    /**
     * Set the handler to use when a response of a command is received
     *
     * @param responseHandler The handler
     */
    public void setResponseHandler(ResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    /**
     * Set the handler to use when an event is received
     *
     * @param eventHandler The handler
     */
    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public void close() throws IOException {
        try {
            if (ioSocket != null) {
                ioSocket.exitValue();
            }
        } catch (Exception e) {
            if (exitOnClose) {
                write("exit", null);
            }
            initialized = false;
            ioWriter.close();
            ioSocket.destroy();
        }
    }

    /**
     * Send a command to MPV
     *
     * @param command   The command name
     * @param arguments The list of arguments of the command
     * @return The request id
     * @throws IOException If an error when send the command
     */
    public int write(String command, List<? extends Serializable> arguments) throws IOException {
        if (!initialized) {
            start();
        }
        ArrayList<Object> parameters = new ArrayList<>();
        parameters.add(command);
        parameters.addAll(arguments == null ? Collections.EMPTY_LIST : arguments);
        JSONObject json = new JSONObject();
        json.put("command", parameters);
        int requestId = ((int) Math.ceil(Math.random() * 1000));
        json.put("request_id", requestId);
        logger.debug("Send command: " + json.toJSONString());

        ioWriter.write(json.toJSONString());
        ioWriter.newLine();
        ioWriter.flush();

        return requestId;
    }

    /**
     * Listen all message that MPV send to the socket
     */
    private void listen() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner sc = new Scanner(ioSocket.getInputStream());

                while (sc.hasNextLine()) {
                    final String line = sc.nextLine();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            logger.debug("Receiving data: " + line);
                            if (line == null || !line.startsWith("{")) {
                                logger.debug(" - No a valid JSON");
                                return;
                            }
                            JSONObject object = JSONObject.parseObject(line);
                            if (object.containsKey("request_id") && responseHandler.canHandle(object.getIntValue("request_id"))) {
                                logger.debug(" - Contains a valid request_id");
                                responseHandler.handle(object);
                                return;
                            }

                            if (!object.containsKey("event")) {
                                logger.debug(" - Not an event");
                                return;
                            }
                            String eventName = object.getString("event");
                            logger.debug(" - It's the event: " + eventName);
                            eventHandler.handle(eventName, object);
                        }
                    }).start();
                }

                logger.info("The listener ended.");
                try {
                    ioWriter.close();
                } catch (IOException e) {
                    // no-op
                }
                initialized = false;
            }
        }).start();
    }

    /**
     * Start all process (connect to the IO socket and create a writer)
     */
    private void start() {
        try {
            logger.info("Starting processes");
            if (ioSocket == null || ioSocket.exitValue() != -1) {
                ProcessBuilder builder = new ProcessBuilder(Arrays.asList("nc", "-U", socketPath));
                try {
                    ioSocket = builder.start();
                    Thread.sleep(500);
                    ioWriter = new BufferedWriter(new OutputStreamWriter(ioSocket.getOutputStream()));
                    listen();
                    initialized = true;
                } catch (IOException | InterruptedException e) {
                    logger.error("Unable to start communication", e);
                    initialized = false;
                }
            }
        } catch (Exception e) {
            logger.error("Unable to start communication", e);
            initialized = false;
        }
    }

    /**
     * The handler for command response.
     *
     * @author MacFJA
     */
    public interface ResponseHandler {
        /**
         * Indicate if the provided request id can be handled.
         * If it's not the case, the response will be discard
         *
         * @param requestId The request id
         * @return <code>true</code> if the response with this request id is wanted
         */
        boolean canHandle(int requestId);

        /**
         * Handle the response
         *
         * @param response The response
         */
        void handle(JSONObject response);
    }

    /**
     * The handler for event.
     *
     * @author MacFJA
     */
    public interface EventHandler {
        /**
         * Handle the event
         *
         * @param eventName The name of the event
         * @param eventJson The event json
         */
        void handle(String eventName, JSONObject eventJson);
    }
}
