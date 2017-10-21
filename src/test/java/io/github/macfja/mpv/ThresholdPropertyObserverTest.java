package io.github.macfja.mpv;

import com.alibaba.fastjson.JSONObject;
import io.github.macfja.mpv.service.Utils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class ThresholdPropertyObserverTest {
    static MpvService mpvService;

    @BeforeClass
    static public void init() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");
        mpvService = (new Service("/Users/dev/Applications/mpv.app/Contents/MacOS/mpv"));
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
    public void testCustomEvent()
    {
        JSONObject event = Utils.buildPropertyChangeEvent("x-fake-prop", "ok", 1);

        JSONObject event2 =  Utils.buildPropertyChangeEvent("x-fake-prop", "ok", 2);

        final StringBuilder called = new StringBuilder();
        try {
            mpvService.registerPropertyChange(new ThresholdPropertyObserver(1.5f, "x-fake-prop") {
                @Override
                public void changed(String propertyName, Object value, Integer id) {
                    Assert.assertEquals("x-fake-prop", propertyName);
                    Assert.assertEquals("ok", value);
                    called.append(1);
                }
            });

            mpvService.registerPropertyChange(new ThresholdPropertyObserver(1.5f, "x-fake-prop", 2) {
                @Override
                public void changed(String propertyName, Object value, Integer id) {
                    Assert.assertEquals("x-fake-prop", propertyName);
                    Assert.assertEquals("ok", value);
                    Assert.assertEquals(new Integer(2), id);
                    called.append(1);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mpvService.fireEvent(event2);
            mpvService.fireEvent(event);
            mpvService.fireEvent(event);
            mpvService.fireEvent(event);
            Thread.sleep(500);
            mpvService.fireEvent(event);
            mpvService.fireEvent(event2);
            mpvService.fireEvent(event);
            mpvService.fireEvent(event);
            Thread.sleep(500);
            mpvService.fireEvent(event);
            mpvService.fireEvent(event2);
            mpvService.fireEvent(event);
            mpvService.fireEvent(event);
            Thread.sleep(500);
            mpvService.fireEvent(event);
            mpvService.fireEvent(event);
            mpvService.fireEvent(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(3, called.length());
    }
}
