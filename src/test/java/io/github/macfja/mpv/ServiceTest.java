package io.github.macfja.mpv;

import com.alibaba.fastjson.JSONObject;
import io.github.macfja.mpv.communication.handling.ResponseHandler;
import io.github.macfja.mpv.communication.handling.NamedEventHandler;
import io.github.macfja.mpv.communication.handling.PropertyObserver;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class ServiceTest {
    static MpvService mpvService;

    @BeforeClass
    static public void init() {
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
    public void testGetProperty()
    {
        try {
            String result = mpvService.getProperty("mpv-version");
            Assert.assertThat(result, CoreMatchers.containsString("mpv"));
            Assert.assertTrue(ResponseHandler.isResultSuccess(result));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPropertyChange() throws InterruptedException {
        final StringBuilder called = new StringBuilder();
        try {
            mpvService.registerPropertyChange(new PropertyObserver("metadata", 1) {
                @Override
                public void changed(String propertyName, Object value, Integer id) {
                    Assert.assertEquals("metadata", propertyName);
                    Assert.assertEquals("hello", value);
                    called.append(1);
                }
            });
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        mpvService.fireEvent(
                PropertyObserver.buildPropertyChangeEvent("metadata", "hello", 1)
        );
        mpvService.waitForEvent("metadata", 100);
        Assert.assertEquals(1, called.length());
    }

    @Test
    public void testCustomEvent() throws InterruptedException {
        final StringBuilder called = new StringBuilder();
        mpvService.registerEvent(new NamedEventHandler("x-custom") {
            @Override
            public Runnable doHandle(JSONObject message) {
                Assert.assertEquals("x-custom", message.getString("event"));
                called.append(1);
                return null;
            }
        });
        mpvService.fireEvent("x-custom");
        mpvService.fireEvent("x-custom");
        mpvService.fireEvent("x-custom");
        Thread.sleep(100);
        Assert.assertEquals(3, called.length());
    }

    @Test
    public void testNonExistingEvent() throws InterruptedException {
        Thread.sleep(1000);
        final StringBuilder called = new StringBuilder();
        mpvService.registerEvent(new NamedEventHandler("x-custom") {
            @Override
            public Runnable doHandle(JSONObject message) {
                Assert.assertEquals("x-custom", message.getString("event"));
                called.append(1);
                return null;
            }
        });
        mpvService.fireEvent("x-custom2");
        mpvService.fireEvent("x-custom3");
        Thread.sleep(500);
        Assert.assertEquals(0, called.length());
    }
}
