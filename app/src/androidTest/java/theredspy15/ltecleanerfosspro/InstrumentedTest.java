/*
 * Copyright 2020 Hunter J Drum
 */

package theredspy15.ltecleanerfosspro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class InstrumentedTest {
    private FileScanner fs;

    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File path = new File(Environment.getExternalStorageDirectory().toString() + "/");
        Resources res = appContext.getResources();
        fs = new FileScanner(path,appContext);
        fs.setAutoWhite(false);
        fs.setResources(res);
        fs.setDelete(true);
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("theredspy15.ltecleanerfoss", appContext.getPackageName());
    }

    @Test
    public void checkLogFiles() {
        File logFile = createFile("testfile.loG");
        File clogFile = createFile("clogs.pnG");
        fs.setUpFilters(true,
                false, false);
        fs.startScan();

        assertTrue(clogFile.exists());
        assertFalse(logFile.exists());
    }

    @Test
    public void checkTempFiles() {
        File tmpFile = createFile("testfile.tMp");
        fs.setUpFilters(true,
                false, false);
        fs.startScan();

        assertFalse(tmpFile.exists());
    }

    @Test
    public void checkThumbFiles() {
        File thumbFile = createFile("thumbs.Db");
        fs.setUpFilters(false,
                true, false);
        fs.startScan();

        assertFalse(thumbFile.exists());
    }

    @Test
    public void checkAPKFiles() {
        File thumbFile = createFile("chrome.aPk");
        fs.setUpFilters(true,
                true, true);
        fs.startScan();

        assertFalse(thumbFile.exists());
    }

    @Test
    public void checkEmptyDir() {
        File emptyDir = createDir("testdir");
        fs.setUpFilters(true,
                false, false);
        fs.setEmptyDir(true);
        fs.startScan();

        assertFalse(emptyDir.exists());
    }

    private File createFile(String name) {
        File file = new File(Environment.getExternalStorageDirectory().toString()
                + "/" + name);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assertTrue(file.exists());
        return file;
    }

    private File createDir(String name) {
        File file = new File(Environment.getExternalStorageDirectory(), name);

        assertTrue(file.mkdir());
        assertTrue(file.exists());
        return file;
    }
}
