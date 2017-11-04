package io.github.macfja.mpv;

import io.github.macfja.mpv.communication.handling.PropertyObserver;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss");

        mpvService = new Service();
    }

    @AfterClass
    static public void finish() {
        try {
            Thread.sleep(1000);
            mpvService.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        mpvService = null;
    }

    @Test
    public void nestedCall() throws IOException, InterruptedException {
        final AtomicBoolean observerCalled = new AtomicBoolean(false);
        final AtomicBoolean responseIsNull = new AtomicBoolean(true);
        mpvService.registerPropertyChange(new io.github.macfja.mpv.communication.handling.PropertyObserver("volume") {
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

    @Test
    public void rapidEvent() throws IOException {
        final List<Integer> values = new ArrayList<>();
        final List<Integer> expected = new ArrayList<>();
        PropertyObserver observer;
        mpvService.registerPropertyChange(observer = new PropertyObserver("volume") {
            @Override
            public void changed(String propertyName, Object value, Integer id) {
                values.add(((BigDecimal) value).intValue());
            }
        });
        for (int iteration = 0; iteration < 10; iteration++) {
            mpvService.setProperty("volume", String.valueOf(iteration));
            expected.add(iteration);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(10, values.size());
        Assert.assertTrue(expected.equals(values));
        mpvService.unregisterPropertyChange(observer);
    }
}
