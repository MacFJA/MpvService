package io.github.macfja.mpv.communication;

import com.alibaba.fastjson.JSONObject;
import io.github.macfja.mpv.communication.handling.MessageHandlerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The implementation of communication interface.
 * It use the unix "{@code nc}" command communicate with MPV IPC.
 *
 * @author MacFJA
 */
public class Communication implements CommunicationInterface {
    /**
     * The process used to write and read data.
     */
    private Process ioSocket;
    /**
     * The writer
     */
    private BufferedWriter ioWriter;
    /**
     * Indicate if we should send a quite command to MPV when the {@code close} method is call.
     *
     * @see Communication#close()
     */
    private boolean exitOnClose = true;
    /**
     * The path to the socket that MPV listen
     */
    private String socketPath;
    /**
     * The class logger
     */
    protected Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * The listening part
     */
    private MessagesListener messagesListener;

    @Override
    public void setExitOnClose(boolean exitOnClose) {
        this.exitOnClose = exitOnClose;
    }

    @Override
    public void setSocketPath(String socketPath) {
        this.socketPath = socketPath;
    }

    @Override
    public void addMessageHandler(MessageHandlerInterface messageHandler) {
        messagesListener.addMessageHandler(messageHandler);
    }

    /**
     * Constructor and initializer.
     */
    public Communication() {
        messagesListener = new MessagesListener(logger);
    }

    @Override
    public void removeMessageHandler(MessageHandlerInterface messageHandler) {
        messagesListener.removeMessageHandler(messageHandler);
    }

    @Override
    public List<MessageHandlerInterface> getMessageHandlers() {
        return messagesListener.getMessageHandlers();
    }

    @Override
    public void clearMessageHandlers() {
        messagesListener.clearMessageHandlers();
    }

    /**
     * Check if every component is ready ti be used.
     * Start them if necessary.
     *
     * @throws IOException If an error occurs when opening the communication
     */
    private void ensureIoReady() throws IOException {
        if (ioWriter == null || !messagesListener.isRunning() || ioSocket == null) {
            open();
        }
    }

    @Override
    public int write(String command, List<? extends Serializable> arguments) throws IOException {
        ensureIoReady();

        ArrayList<Object> parameters = new ArrayList<>();
        parameters.add(command);
        parameters.addAll(arguments == null ? Collections.EMPTY_LIST : arguments);
        JSONObject json = new JSONObject();
        json.put("command", parameters);
        int requestId = ((int) Math.ceil(Math.random() * 1000));
        json.put("request_id", requestId);
        logger.debug("Send: " + json.toJSONString());

        ioWriter.write(json.toJSONString());
        ioWriter.newLine();
        ioWriter.flush();

        return requestId;
    }

    @Override
    public void simulateMessage(JSONObject message) {
        messagesListener.handleLine(message);
    }

    @Override
    public void open() throws IOException {
        logger.info("Starting processes");
        try {
            if (ioSocket == null || ioSocket.exitValue() != -1) {
                logger.info("Start MPV communication");
                ProcessBuilder builder = new ProcessBuilder(Arrays.asList("nc", "-U", socketPath));
                ioSocket = builder.start();
                Thread.sleep(500);
            }
        } catch (IOException e) {
            logger.error("Unable to start communication", e);
            ioSocket = null;

            throw e;
        } catch (InterruptedException e) {
            logger.warn("Sleeping interrupted", e);
        }

        if (ioWriter == null) {
            logger.info("Start MPV writer");
            ioWriter = new BufferedWriter(new OutputStreamWriter(ioSocket.getOutputStream()));
        }

        if (!messagesListener.isRunning()) {
            logger.info("Start MPV reader");
            messagesListener.start(ioSocket.getInputStream());
        }
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
            ioWriter.close();
            ioSocket.destroy();
        } finally {
            ioSocket = null;
            ioWriter = null;
        }
    }
}
