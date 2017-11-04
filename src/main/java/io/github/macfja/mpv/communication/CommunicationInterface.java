package io.github.macfja.mpv.communication;

import com.alibaba.fastjson.JSONObject;
import io.github.macfja.mpv.communication.handling.HandlerAwareInterface;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * The interface used by the MPV service implementation to communicate with MPV.
 *
 * @author MacFJA
 */
public interface CommunicationInterface extends HandlerAwareInterface, Closeable {
    /**
     * Change the behavior when the {@code close()} method is call.
     * If {@code true} then a {@code quit} command will be sent to MPV
     *
     * @param exitOnClose The flag
     * @see CommunicationInterface#close()
     */
    void setExitOnClose(boolean exitOnClose);

    /**
     * Set the path to the socket that MPV listen
     *
     * @param socketPath The path
     */
    void setSocketPath(String socketPath);

    /**
     * Send a command to MPV
     *
     * @param command   The command name
     * @param arguments The list of arguments of the command
     * @return The request id
     * @throws IOException If an error when send the command
     */
    int write(String command, List<? extends Serializable> arguments) throws IOException;

    /**
     * Simulated the receive of a message
     *
     * @param message The message to receive
     */
    void simulateMessage(JSONObject message);

    /**
     * Open the communication with MPV
     *
     * @throws IOException If an error occurs when opening the communication
     */
    void open() throws IOException;
}
