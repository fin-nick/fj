/*
 * AutoAbsence.java
 *
 * Created on 4 Июль 2010 г., 22:42
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if modules_ABSENCE is "true" #
package jimm.modules;

import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Displayable;
import jimm.Jimm;
import jimm.Options;
import jimm.cl.ContactList;
import jimm.ui.base.CanvasEx;
import jimm.ui.base.NativeCanvas;
import jimm.ui.base.UIUpdater;
import protocol.Protocol;
import protocol.Status;

/**
 *
 * @author Vladimir Kryukov
 */
public final class AutoAbsence {
    public static final AutoAbsence instance = new AutoAbsence();
    /**
     * Creates a new instance of AutoAbsence
     */
    public AutoAbsence() {
        absence = false;
        userActivity();
    }

    private byte[] statuses;
    private String[] messages;
    private long activityTime;
    private boolean absence;

    private void doAway() {
        if (absence) {
            return;
        }
        int count = ContactList.getInstance().getManager().getModel().getProtocolCount();
        statuses = new byte[count];
        messages = new String[count];
        for (int i = 0; i < count; ++i) {
            Protocol p = ContactList.getInstance().getManager().getModel().getProtocol(i);
            if (isSupported(p)) {
                statuses[i] = p.getProfile().statusIndex;
                messages[i] = p.getProfile().statusMessage;
                p.setOnlineStatus(Status.I_STATUS_AWAY, messages[i]);
            }
        }
        absence = true;
    }
    private boolean isSupported(Protocol p) {
        if ((null == p) || !p.isConnected()) {
            return false;
        }
        // #sijapp cond.if protocols_VK is "true" #
        if (p instanceof protocol.vk.VK) {
            return false;
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_MSN is "true" #
        if (p instanceof protocol.msn.Msn) {
            return false;
        }
        // #sijapp cond.end #
        return true;
    }
    private void doRestore() {
        if (!absence) {
            return;
        }
        absence = false;
        int count = ContactList.getInstance().getManager().getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = ContactList.getInstance().getManager().getModel().getProtocol(i);
            if (isSupported(p)) {
                p.setOnlineStatus(statuses[i], messages[i]);
            }
        }
    }

    private boolean isBlockOn() {
        return Options.getBoolean(Options.OPTION_AA_BLOCK);
    }
    private int getTimeInterval() {
        return Options.getInt(Options.OPTION_AA_TIME);
    }
    public final void run() {
        try {
            if (absence) {
                return;
            }
            long time = getTimeInterval();
            if (0 == time) {
                return;
            }
            long now = System.currentTimeMillis();
            if (time * 60 * 1000 < now - activityTime) {                
                doAway();
            if (Options.getBoolean(Options.OPTION_AA_LOCK)) { //autoblock
                Jimm.lockJimm();                              //autoblock
                }                                             //autoblock
            }
        } catch (Exception e) {
        }
    }
    public final void away() {
        if (isBlockOn()) {
            doAway();
        }
    }
    public final void online() {
        if (isBlockOn()) {
            doRestore();
        }
    }

    public final void userActivity() {
        try {
            long time = getTimeInterval();
            if (0 == time) {
                return;
            }
            activityTime = System.currentTimeMillis();
            if (Jimm.isLocked()) {
                return;
            }
            if (absence) {
                doRestore();
            }
        } catch (Exception e) {
        }
    }
}
// #sijapp cond.end#