package io.github.macfja.mpv.wrapper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.github.macfja.mpv.MpvService;
import io.github.macfja.mpv.communication.handling.NamedEventHandler;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper class to add common actions
 *
 * @author MacFJA
 */
public class Shorthand implements MpvService {
    /**
     * The wrapped service
     */
    protected MpvService service;

    /**
     * Constructor
     *
     * @param service The service we extends
     */
    public Shorthand(MpvService service) {
        this.service = service;
    }

    /**
     * Change the position of the playback
     *
     * @param amount   The amount (seconds, or percentage)
     * @param seekType The type of seek (absolute, relative, percentage, etc.)
     * @throws IOException If an error occurs when sending the command
     */
    public void seek(Integer amount, Seek seekType) throws IOException {
        service.sendCommand("seek", Arrays.asList(amount, seekType.type));
    }

    /**
     * Change the position of the playback relatively from current position
     *
     * @param second The amount of seconds
     * @throws IOException If an error occurs when sending the command
     */
    public void seek(Integer second) throws IOException {
        seek(second, Seek.Relative);
    }

    /**
     * Start the playback
     *
     * @throws IOException If an error occurs when sending the command
     */
    public void play() throws IOException {
        service.setProperty("pause", false);
    }

    /**
     * Stop (pause) the playback
     *
     * @throws IOException If an error occurs when sending the command
     */
    public void pause() throws IOException {
        service.setProperty("pause", true);
    }

    /**
     * Start the playback if is was paused, or stop it in the other case
     *
     * @throws IOException If an error occurs when sending the command
     */
    public void playPause() throws IOException {
        service.sendCommand("cycle", Collections.singletonList("pause"));
    }

    /**
     * Add a new media
     *
     * @param path          The path where the media is
     * @param addToPlaylist <p>If {@code false}, the media will replace the current media,
     *                      otherwise it will be added to the end of the playlist</p>
     * @throws IOException If an error occurs when sending the command
     */
    public void addMedia(String path, Boolean addToPlaylist) throws IOException {
        service.sendCommand("loadfile", Arrays.asList(path.trim(), addToPlaylist ? "append-play" : "replace"));
    }

    /**
     * Get the time of the current playback.
     *
     * @return A map that contains the elapsing and remaining time
     * @throws IOException If an error occurs when sending the command
     */
    public Map<TimeKey, BigDecimal> getTimes() throws IOException {
        Map<TimeKey, BigDecimal> result = new HashMap<>();
        JSONObject object;
        object = JSON.parseObject(service.getProperty("time-remaining"));
        BigDecimal remaining = object.getBigDecimal("data");
        object = JSON.parseObject(service.getProperty("time-pos"));
        BigDecimal elapsing = object.getBigDecimal("data");

        result.put(TimeKey.Remaining, remaining);
        result.put(TimeKey.Elapsing, elapsing);

        return result;
    }

    /**
     * Play the next media
     *
     * @throws IOException If an error occurs when sending the command
     */
    public void next() throws IOException {
        service.sendCommand("playlist-next", new ArrayList<Serializable>());
    }

    /**
     * Play the previous media
     *
     * @throws IOException If an error occurs when sending the command
     */
    public void previous() throws IOException {
        service.sendCommand("playlist-prev", new ArrayList<Serializable>());
    }

    @Override
    public String sendCommand(String command, List<? extends Serializable> arguments) throws IOException {
        return service.sendCommand(command, arguments);
    }

    @Override
    public void sendNonBlockingCommand(String command, List<? extends Serializable> arguments) throws IOException {
        service.sendCommand(command, arguments);
    }

    @Override
    public String setProperty(String name, String value) throws IOException {
        return service.setProperty(name, value);
    }

    @Override
    public String setProperty(String name, Boolean value) throws IOException {
        return service.setProperty(name, value);
    }

    @Override
    public String getProperty(String name) throws IOException {
        return service.getProperty(name);
    }

    @Override
    public <T> T getProperty(String name, Class<T> type) throws IOException {
        String result = getProperty(name);
        return JSONObject.parseObject(result).getObject("data", type);
    }

    @Override
    public void registerEvent(NamedEventHandler observer) {
        service.registerEvent(observer);
    }

    @Override
    public void registerPropertyChange(io.github.macfja.mpv.communication.handling.PropertyObserver observer) throws IOException {
        service.registerPropertyChange(observer);
    }

    @Override
    public void unregisterPropertyChange(io.github.macfja.mpv.communication.handling.PropertyObserver observer) throws IOException {
        service.unregisterPropertyChange(observer);
    }

    @Override
    public void unregisterPropertyChange(String propertyName) throws IOException {
        service.unregisterPropertyChange(propertyName);
    }

    @Override
    public void fireEvent(String eventName) {
        service.fireEvent(eventName);
    }

    @Override
    public void fireEvent(String eventName, JSONObject data) {
        service.fireEvent(eventName, data);
    }

    @Override
    public void fireEvent(JSONObject event) {
        service.fireEvent(event);
    }

    @Override
    synchronized public void waitForEvent(String eventName) {
        service.waitForEvent(eventName);
    }

    @Override
    synchronized public void waitForEvent(String eventName, int timeout) {
        service.waitForEvent(eventName, timeout);
    }

    @Override
    public void close() throws IOException {
        service.close();
    }

    /**
     * The list of key used in the {@code seek} method result
     *
     * @see Shorthand#seek(Integer)
     * @see Shorthand#seek(Integer, Seek)
     */
    public enum TimeKey {
        Remaining,
        Elapsing
    }

    /**
     * List of available seek type
     *
     * @see Shorthand#seek(Integer, Seek)
     */
    public enum Seek {
        Relative("relative"),
        Absolute("absolute"),
        AbsolutePercent("absolute-percent"),
        RelativePercent("relative-percent"),
        Exact("exact"),
        Keyframes("keyframes");

        /**
         * The value to used in the command
         */
        private String type;

        Seek(String type) {
            this.type = type;
        }
    }
}
