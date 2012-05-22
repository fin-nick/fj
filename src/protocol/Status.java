/*
 * Status.java
 *
 * Created on 29 Май 2008 г., 17:52
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import DrawControls.icons.*;

/**
 *
 * @author Vladimir Krukov
 */
public final class Status {
    public static final byte I_STATUS_OFFLINE = 0;
    public static final byte I_STATUS_ONLINE  = 1;
    public static final byte I_STATUS_AWAY    = 2;
    public static final byte I_STATUS_CHAT    = 3;

    // Jabber statuses
    public static final byte I_STATUS_XA = 4;
    public static final byte I_STATUS_DND = 5;
    // Mrim statuses
    public static final byte I_STATUS_UNDETERMINATED = 4;
    public static final byte I_STATUS_INVISIBLE      = 5;
    // Icq statuses
    public static final byte I_STATUS_NA         = 9;
    public static final byte I_STATUS_OCCUPIED   = 10;
    public static final byte I_STATUS_DND_       = 11;
    public static final byte I_STATUS_INVISIBLE_ = 12;
    public static final byte I_STATUS_INVIS_ALL  = 13;

    public static final byte I_STATUS_EVIL       = 6;
    public static final byte I_STATUS_DEPRESSION = 7;

    public static final byte I_STATUS_HOME       = 4;
    public static final byte I_STATUS_WORK       = 5;
    public static final byte I_STATUS_LUNCH      = 8;

    
    public final void setStatusIndex(byte statusIndex) {
        this.statusIndex = statusIndex;
    }
    private boolean is(final byte s) {
        return s == statusIndex;
    }

    public static final Status offlineStatus = new Status();
    private byte statusIndex = 0;
    private String text;
    public byte getStatusIndex() {
        return statusIndex;
    }

    public final String getText() {
        return text;
    }
    public void setText(String txt) {
        text = txt;
    }
}
