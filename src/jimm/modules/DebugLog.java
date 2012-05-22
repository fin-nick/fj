/*******************************************************************************
Jimm - Mobile Messaging - J2ME ICQ clone
Copyright (C) 2003-05  Jimm Project

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
********************************************************************************
File: src/jimm/DebugLog.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Artyomov Denis
*******************************************************************************/

// #sijapp cond.if modules_DEBUGLOG is "true" #
package jimm.modules;

import DrawControls.*;
import javax.microedition.lcdui.*;

import DrawControls.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.MD5;
import jimm.comm.Util;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.util.*;

public class DebugLog extends TextListEx implements SelectListener {
    private static DebugLog instance = new DebugLog();
    
    private DebugLog() {
        super(ResourceBundle.getString("debug log"));

        MenuModel menu = new MenuModel();
        menu.addItem("copy_text",     MENU_COPY);
        menu.addItem("copy_all_text", MENU_COPY_ALL);
        menu.addItem("clear",         MENU_CLEAN);
        menu.addItem("Properties",    MENU_PROPERTIES);
        menu.addItem("back",          MENU_BACK);
        menu.setActionListener(this);
        setMenu(menu, MENU_BACK);
    }

    public static void activate() {
        instance.setCurrentItem(instance.getSize());
        instance.show();
    }

    private static final int MENU_COPY       = 0;
    private static final int MENU_COPY_ALL   = 1;
    private static final int MENU_CLEAN      = 2;
    private static final int MENU_PROPERTIES = 3;
    private static final int MENU_BACK       = 4;
    
    public void select(Select select, MenuModel model, int cmd) {
        switch (cmd) {
            case MENU_COPY:
            case MENU_COPY_ALL:
                copy(cmd == MENU_COPY_ALL);
                break;
            
            case MENU_CLEAN:
                synchronized (instance) {
                    clearLog();
                }
                break;
                
            case MENU_PROPERTIES:
                dumpProperties();
                break;

            case MENU_BACK:
                back();
                return;
        }
        select.back();
    }

    protected void clearLog() {
        recordsNumber = 0;
        clear();
    }
    private void removeOldRecords() {
        final int maxRecordCount = 200;
        while (maxRecordCount < recordsNumber) {
            recordsNumber--;
            removeFirstText();
        }
    }

    public static void memoryUsage(String str) {
        long size = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        size = (size + 512) / 1024;
        println(str + " = " + size + "kb.");
    }

    private static String _(String text) {
        if (null == text) {
            return "";
        }
        String text1 = ResourceBundle.getString(text);
        if (text1 != text) {
            return "[l] " + text1;
        }
        return text;
    }
    public static void systemPrintln(String text) {
        System.out.println(text);
    }
    private int recordsNumber = 0;
    public static void println(String text) {
    	synchronized (instance) {
            int counter = instance.uiBigTextIndex;
            instance.lock();
            String date = Util.getDateString(false);
    		instance.addBigText(date + ": ", THEME_MAGIC_EYE_NUMBER, Font.STYLE_PLAIN, counter);
    		instance.addBigText(_(text), THEME_TEXT, Font.STYLE_PLAIN, counter);
    		instance.doCRLF(counter);
            instance.uiBigTextIndex++;
            instance.recordsNumber++;
            instance.removeOldRecords();
            instance.unlock();
		}
		System.out.println(text);
    }

    public static void panic(String str) {
        try {
            // make stack trace...
            throw new Exception();
        } catch (Exception e) {
            panic(str, e);
        }
    }
    public static void assert0(String str, boolean result) {
        if (result) {
            try {
                // make stack trace...
                throw new Exception();
            } catch (Exception e) {
                println("assert: " + _(str));
                e.printStackTrace();
            }
        }
    }

    public static void panic(String str, Exception e) {
        System.err.println("panic: " + _(str));
        println("panic: " + _(str) + " "  + e.getMessage()
                + " (" + e.getClass().getName() + ")");
        e.printStackTrace();
    }
    
    private static long profilerTime;
    public static long profilerStart() {
        profilerTime = System.currentTimeMillis();
        return profilerTime;
    }
    public static long profilerStep(String str, long startTime) {
        long now = System.currentTimeMillis();
        println("profiler: " + _(str) + ": " + (now - startTime));
        return now;
    }
    public static void profilerStep(String str) {
        long now = System.currentTimeMillis();
        println("profiler: " + _(str) + ": " + (now - profilerTime));
        profilerTime = now;
    }
    
    public static void startTests() {
        println("TimeZone info");
        java.util.TimeZone tz = java.util.TimeZone.getDefault();
        println("TimeZone offset: " + tz.getRawOffset());
        println("Daylight: " + tz.useDaylightTime());
        println("ID: " + tz.getID());
        println("Jimm-Snapshot: " + Jimm.getAppProperty("Jimm-Snapshot", null));
        
        MD5 md5 = new MD5();
        md5.init();
        md5.updateASCII("\u0422\u0435\u0441\u0442");
        md5.finish();
        assert0("md5 (ru): failed", !md5.getDigestHex().equals("16497fa0c8e13ce8fab874d959db91b9"));
        
        md5 = new MD5();
        md5.init();
        md5.updateASCII("Test");
        md5.finish();
        assert0("md5 (en): failed", !md5.getDigestHex().equals("0cbc6611f5540bd0809a388dc95a615b"));
        
        assert0("bs64decode (0): failed", !MD5.decodeBase64(" eg==").equals("z"));
        assert0("bs64decode (1): failed", !MD5.decodeBase64("eg==").equals("z"));
        assert0("bs64decode (2): failed", !MD5.decodeBase64("eno=").equals("zz"));
        assert0("bs64decode (3): failed", !MD5.decodeBase64("enp6").equals("zzz"));
        assert0("bs64decode (4): failed", !MD5.decodeBase64(" eg==\n").equals("z"));
        assert0("bs64decode (5): failed", !MD5.decodeBase64("eg==\n").equals("z"));
        assert0("bs64decode (6): failed", !MD5.decodeBase64("eno=\n").equals("zz"));
        assert0("bs64decode (7): failed", !MD5.decodeBase64("enp6\n").equals("zzz"));
        assert0("bs64 (1): failed", !MD5.toBase64(new byte[]{'z'}).equals("eg=="));
        assert0("bs64 (2): failed", !MD5.toBase64(new byte[]{'z', 'z'}).equals("eno="));
        assert0("bs64 (3): failed", !MD5.toBase64(new byte[]{'z', 'z', 'z'}).equals("enp6"));
        //protocol.icq.ClientDetector.g();
    }
    public static void dumpProperties() {
        println("RamFree: "   + System.getProperty("com.nokia.memoryramfree"));
        println("Network: "   + System.getProperty("com.nokia.mid.networkid"));
        //println("Avaliable: " + System.getProperty("com.nokia.mid.networkavailability"));
        //println("Status: "    + System.getProperty("com.nokia.mid.networkstatus"));
        println("Signal: "    + System.getProperty("com.nokia.mid.networksignal"));
        println("Indicator: " + System.getProperty("com.nokia.canvas.net.indicator.location"));
        //println("SellId: "    + System.getProperty("com.nokia.mid.cellid"));
        println("Point: "     + System.getProperty("com.nokia.network.access"));
        
        println("Battery: " + batteryLevel());
        println("Params: "     + System.getProperty("com.nokia.mid.cmdline"));
        //println("Dir: "     + System.getProperty("fileconn.dir.private"));
        //println("Camera: "  + System.getProperty("camera.orientations"));

        //println("Soft1 "  + System.getProperty("com.nokia.softkey1.label.location"));
        //println("Soft2 "  + System.getProperty("com.nokia.softkey2.label.location"));
        //println("Soft3 "  + System.getProperty("com.nokia.softkey3.label.location"));
    }
    private static String batteryLevel() {
        String level = System.getProperty("com.nokia.mid.batterylevel");
        if (null == level) {
            level = System.getProperty("batterylevel");
        }
        return level;
    }


    public static void dump(String comment, byte[] data) {
        StringBuffer sb = new StringBuffer();
        sb.append("dump: " + comment + ":\n");
        for (int i = 0; i < data.length; ++i) {
            String hex = Integer.toHexString(((int)data[i]) & 0xFF);
            if (1 == hex.length()) sb.append(0);
            sb.append(hex);
            sb.append(" ");
            if (i % 16 == 15) sb.append("\n");
        }
        println(sb.toString());
    }
}
// #sijapp cond.end#