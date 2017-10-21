package io.github.macfja.mpv;

import com.alibaba.fastjson.JSONObject;
import io.github.macfja.mpv.wrapper.Shorthand;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Map;

public class MpvShortHandTest {
    static Shorthand mpvService;

    @BeforeClass
    static public void init() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");

        mpvService = new Shorthand((new Service("/Users/dev/Applications/mpv.app/Contents/MacOS/mpv")));
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
    public void testMultiple() throws IOException {
        final StringBuilder sb = new StringBuilder();
        mpvService.registerEvent("start-file", new Observer() {
            @Override
            public void trigger(String eventName, JSONObject json) {
                sb.append(1);
            }
        });
        mpvService.setProperty("volume", "13");
        Assert.assertEquals(13f, mpvService.getProperty("volume", Float.class), 0.f);
        URL url = this.getClass().getResource("../../../../Discovery_Hit.mp3");
        mpvService.addMedia(url.getPath(), false);
        mpvService.play();
        try {
            mpvService.waitForEvent("playback-restart");
            float pos1 = mpvService.getProperty("time-pos", Float.class);
            Thread.sleep(1000);
            float pos2 = mpvService.getProperty("time-pos", Float.class);
            Thread.sleep(2000);
            float pos3 = mpvService.getProperty("time-pos", Float.class);

            Assert.assertTrue("Backing time! (" + pos1 + ", " + pos2 + ")", pos1 < pos2);
            Assert.assertTrue("In a time machine!", pos2 < pos3);

            Assert.assertTrue(pos1 + 1 <= Math.round(pos2));
            Assert.assertTrue(pos1 + 1.5 > pos2);

            Assert.assertTrue(pos2 + 2 <= Math.round(pos3));
            Assert.assertTrue(pos2 + 2.5 > pos3);

            mpvService.seek(1, Shorthand.Seek.Absolute);
            Assert.assertEquals(new Integer(1), mpvService.getProperty("time-pos", Integer.class));

            Thread.sleep(1000);
            float pos4 = mpvService.getProperty("time-pos", Float.class);
            mpvService.pause();
            Thread.sleep(1000);
            float pos5 = mpvService.getProperty("time-pos", Float.class);
            Assert.assertEquals(pos4, pos5, 0.1f);
            mpvService.seek(-1, Shorthand.Seek.Relative);
            Assert.assertEquals((float) (pos5 - 1), (float) mpvService.getProperty("time-pos", Float.class), 0.05f);
            Map<Shorthand.TimeKey, BigDecimal> time = mpvService.getTimes();
            Assert.assertEquals(pos5 - 1, time.get(Shorthand.TimeKey.Elapsing).floatValue(), 0.5f);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(1, sb.length());
    }

    protected void silentLoadFile() {
        try {
            mpvService.setProperty("volume", "0");
            URL url = this.getClass().getResource("../../../../Discovery_Hit.mp3");
            mpvService.addMedia(url.getPath(), false);
            mpvService.waitForEvent("playback-restart");
            mpvService.pause();
            mpvService.seek(0, Shorthand.Seek.Absolute);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSeeks() {
        silentLoadFile();
        //double total = 14.367344 + 0.966531;
        double total = 15.333875;
        try {
            mpvService.seek(1, Shorthand.Seek.Absolute);
            Assert.assertEquals(1, mpvService.getProperty("time-pos", Float.class), 0.1f);
        } catch (IOException e) {
            Assert.fail();
        }

        try {
            mpvService.seek(1, Shorthand.Seek.Relative);
            Assert.assertEquals(2, mpvService.getProperty("time-pos", Float.class), 0.1f);
        } catch (IOException e) {
            Assert.fail();
        }

        try {
            mpvService.seek(50, Shorthand.Seek.AbsolutePercent);
            Assert.assertEquals(total / 2, mpvService.getProperty("time-pos", Float.class), 0.1f);
        } catch (IOException e) {
            Assert.fail();
        }

        try {
            mpvService.seek(25, Shorthand.Seek.RelativePercent);
            Assert.assertEquals(3 * total / 4, mpvService.getProperty("time-pos", Float.class), 0.1f);
        } catch (IOException e) {
            Assert.fail();
        }
    }
}
