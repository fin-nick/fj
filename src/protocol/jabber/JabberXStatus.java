/*
 * JabberXStatus.java
 *
 * Created on 26 Апрель 2009 г., 19:50
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_JABBER is "true" #
// #sijapp cond.if modules_XSTATUSES is "true" #
package protocol.jabber;

import DrawControls.icons.*;
import jimm.comm.Config;
import jimm.comm.StringConvertor;
import jimm.util.ResourceBundle;

/**
 *
 * @author Vladimir Krukov
 */
public class JabberXStatus {
    private static final ImageList xstatusIcons = ImageList.createImageList("/jabber-xstatus.png");
    private static final String[] xstatusCaps;
    private static final String[] xstatusNames;
    static {
        Config cfg = new Config().loadLocale("/jabber-xstatus.txt");
        xstatusCaps = cfg.getKeys();
        xstatusNames = cfg.getValues();
    }
    public static final JabberXStatus noneXStatus = new JabberXStatus();
    private static final byte XSTATUS_NONE = -1;

    private byte xstatusIndex = XSTATUS_NONE;
    private String text;

    public static final byte TYPE_X = 0;
    public static final byte TYPE_MOOD = 1;
    public static final byte TYPE_ACTIVITY = 2;
    public static final byte TYPE_TUNE = 2;
    private byte type;
    /** Creates a new instance of Jabberxstatus */
    public JabberXStatus() {
    }
    public static final int getXStatusCount() {
        return xstatusCaps.length;
    }
    private byte getType(String type) {
        if (type.startsWith("q" + "ip")) {
            return TYPE_X;
        }
        if (type.startsWith("m" + "ood")) {
            return TYPE_MOOD;
        }
        if (type.startsWith("a" + "ctivity")) {
            return TYPE_ACTIVITY;
        }
        if (type.startsWith("t" + "une")) {
            return TYPE_TUNE;
        }
        return -1;
    }
    final boolean isType(String path) {
        return (-1 == type) || (type == getType(path));
    }

    public static final String XSTATUS_TEXT_NONE = "qip:none";
    public static final String XSTATUS_START = "qip:";
    private static byte findXStatusIndex(String id) {
        if (XSTATUS_TEXT_NONE.equals(id)) {
    	    return XSTATUS_NONE;
        }
        for (byte capsIndex = 0; capsIndex < xstatusCaps.length; capsIndex++) {
    	    int index = xstatusCaps[capsIndex].indexOf(id);
            if (-1 != index) {
                String xstr = xstatusCaps[capsIndex];
                final int endPos = index + id.length();
                if ((endPos < xstr.length()) && (',' != xstr.charAt(endPos))) {
                    continue;
                }
                return capsIndex;
            }
        }
        return XSTATUS_NONE;
    }
    public void setStatusIndex(int index) {
        if ((-1 <= index) && (index < xstatusCaps.length)) {
            xstatusIndex = (byte)index;
        }
    }
    public void setText(String text) {
        if (-1 < xstatusIndex) {
            this.text = text;
        }
    }
    public static JabberXStatus createXStatus(JabberXStatus prev, String id, String text) {
        if ((null == prev) || StringConvertor.isEmpty(id)) {
            return noneXStatus;
        }
        byte xstatusIndex = findXStatusIndex(id);
        if (XSTATUS_NONE == xstatusIndex) {
            return prev.isType(id) ? noneXStatus : prev;
        }
        JabberXStatus result = ((null == prev) || (noneXStatus == prev))
                ? new JabberXStatus() : prev;
        // TODO: remove this condition
        result.type = result.getType(id);
        result.xstatusIndex = xstatusIndex;
        result.text = text;
        return result;
    }
    
    public Icon getIcon() {
        return xstatusIcons.iconAt(xstatusIndex);
    }
    public String getName() {
        if (XSTATUS_NONE == xstatusIndex) {
            return ResourceBundle.getString("xstatus_none");
        }
        return xstatusNames[xstatusIndex];
    }
    public String getText() {
        return text;
    }
    
    private final String substr(String str, int pos, String defval) {
        if (pos < 0) {
            return defval;
        }
        int strEnd = str.indexOf(',', pos);
        if (-1 == strEnd) {
    	    str = str.substring(pos);
        } else {
            str = str.substring(pos, strEnd);
        }
        return "-".equals(str) ? defval : str;
    }

    public String getCode() {
        if (0 == xstatusCaps.length) {
            return null;
        }
        boolean isXStatus = xstatusCaps[0].startsWith(XSTATUS_START);
        if (XSTATUS_NONE == xstatusIndex) {
            return isXStatus ? XSTATUS_TEXT_NONE : "";
        }
        return substr(xstatusCaps[xstatusIndex], 0,
                isXStatus ? XSTATUS_TEXT_NONE : "");
    }
    
    public String getIcqXStatus() {
        if (0 == xstatusCaps.length) {
            return null;
        }
        final String ICQ_XSTATUS_PREFIX = "pyicq:";
        final String ICQ_XSTATUS_NONE = "None";
        if (-1 == xstatusCaps[0].indexOf(ICQ_XSTATUS_PREFIX)) {
            return null;
        }
        if (XSTATUS_NONE == xstatusIndex) {
            return ICQ_XSTATUS_NONE;
        }
        int index = xstatusCaps[xstatusIndex].indexOf(ICQ_XSTATUS_PREFIX);
        if (-1 != index) {
            index += ICQ_XSTATUS_PREFIX.length();
        }
        return substr(xstatusCaps[xstatusIndex], index, ICQ_XSTATUS_NONE);
    }

    public final boolean isPep() {
        return TYPE_X != type;
    }
}
// #sijapp cond.end #
// #sijapp cond.end #