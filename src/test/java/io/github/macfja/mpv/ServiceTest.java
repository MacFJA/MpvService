package io.github.macfja.mpv;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.github.macfja.mpv.service.Utils;
import org.hamcrest.CoreMatchers;
import org.junit.*;

import java.io.IOException;

public class ServiceTest {
    static MpvService mpvService;

    @BeforeClass
    static public void init() {
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
    public void testGetProperty()
    {
        try {
            String result = mpvService.getProperty("mpv-version");
            Assert.assertThat(result, CoreMatchers.containsString("mpv"));
            Assert.assertTrue(Utils.isResultSuccess(result));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPropertyChange()
    {
        final StringBuilder called = new StringBuilder();
        try {
            mpvService.registerPropertyChange(new PropertyObserver("metadata") {
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
                Utils.buildPropertyChangeEvent("metadata", "hello", 1)
        );
        Assert.assertEquals(1, called.length());
    }

    @Test
    public void testCustomEvent()
    {
        final StringBuilder called = new StringBuilder();
        mpvService.registerEvent("x-custom", new Observer() {
            @Override
            public void trigger(String eventName, JSONObject json) {
                Assert.assertEquals("x-custom", eventName);
                called.append(1);
            }
        });
        mpvService.fireEvent("x-custom");
        mpvService.fireEvent("x-custom");
        mpvService.fireEvent("x-custom");
        Assert.assertEquals(3, called.length());
    }

    @Test
    public void testNonExistingEvent()
    {
        final StringBuilder called = new StringBuilder();
        mpvService.registerEvent("x-custom", new Observer() {
            @Override
            public void trigger(String eventName, JSONObject json) {
                Assert.assertEquals("x-custom", eventName);
                called.append(1);
            }
        });
        mpvService.fireEvent("x-custom2");
        mpvService.fireEvent("x-custom3");
        Assert.assertEquals(0, called.length());
    }
}
