package io.github.macfja.mpv;

import io.github.macfja.mpv.wrapper.Shorthand;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test case for the issue about nested call.
 *
 * @link https://github.com/MacFJA/MpvService/issues/2
 */
public class ConcurrencyTest {
    static MpvService mpvService;

    @BeforeClass
    static public void init() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");

        mpvService = new Service();
    }

    @AfterClass
    static public void finish() {
        try {
            mpvService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mpvService = null;
    }

    @Test
    public void nestedCall() throws IOException {
        final AtomicBoolean observerCalled = new AtomicBoolean(false);
        final AtomicBoolean responseIsNull = new AtomicBoolean(true);
        mpvService.registerPropertyChange(new PropertyObserver("volume") {
            @Override
            public void changed(String propertyName, Object value, Integer id) {
                observerCalled.set(true);
                try {
                    String response = mpvService.getProperty("mpv-version");

                    // Before the correction, response is equals to `null`
                    responseIsNull.set(response == null);
                } catch (IOException e) {
                    Assert.fail();
                    e.printStackTrace();
                }
            }
        });
        mpvService.setProperty("volume", "0");
        // The sendCommand timeout is defined to 10 * 500ms
        mpvService.waitForEvent("x-fake", 8000);
        Assert.assertTrue("The event was never call", observerCalled.get());
        Assert.assertFalse("The response of the nested call is null", responseIsNull.get());
    }
}
