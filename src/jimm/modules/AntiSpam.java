/*
 * AntiSpam.java
 *
 * Created on 24 Апрель 2007 г., 13:57
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.modules;

import java.util.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.chat.message.Message;
import jimm.chat.message.PlainMessage;
import jimm.chat.message.SystemNotice;
import jimm.cl.*;
import jimm.comm.*;
import jimm.ui.*;
import jimm.util.ResourceBundle;
import protocol.*;

// #sijapp cond.if modules_ANTISPAM is "true" #
/**
 *
 * @author vladimir
 */
public class AntiSpam {
    private static AntiSpam antiSpam = new AntiSpam();

    private Vector validUins = new Vector();
    private Vector uncheckedUins = new Vector();
    private AntiSpam() {
    }
    
    private void sendHelloMessage(Contact contact) {
        validUins.addElement(contact.getUin());
        uncheckedUins.removeElement(contact.getUin());
        if (contact.isMeVisible()) {
            contact.sendMessage(Options.getString(Options.OPTION_ANTISPAM_HELLO), false);
        }
    }

    private void sendQuestion(Contact contact) {
        if (uncheckedUins.contains(contact.getUin())) {
            uncheckedUins.removeElement(contact.getUin());
            return;
        }
        String message = Options.getString(Options.OPTION_ANTISPAM_MSG);
        if (contact.isMeVisible() && !StringConvertor.isEmpty(message)) {
            contact.sendMessage("antispam!\n" + message, false); //it is changed
            uncheckedUins.addElement(contact.getUin());
        }
    }

    private boolean isChecked(String uin) {
        if (validUins.contains(uin)) {
            validUins.removeElement(uin);
            return true;
        }
        return false;
    }
    private void denyAuth(Protocol protocol, Message message) {
        if (message instanceof SystemNotice) {
    	    SystemNotice notice = (SystemNotice)message;
    	    if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
                protocol.autoDenyAuth(message.getSndrUin());
    	    }
        }
    }
    private boolean containsKeywords(String msg) {
        String opt = Options.getString(Options.OPTION_ANTISPAM_KEYWORDS);
        if (0 == opt.length()) return false;
        if (5000 < msg.length()) {
            return true;
        }
        String[] keywords = Util.explode(StringConvertor.toLowerCase(opt), ' ');
        msg = StringConvertor.toLowerCase(msg);
        for (int i = 0; i < keywords.length; i++) {
            if (-1 != msg.indexOf(keywords[i])) {
                return true;
            }
        }
        return false;
    }
    public boolean isSpamMessage(Protocol protocol, Message message) {
        if (!Options.getBoolean(Options.OPTION_ANTISPAM_ENABLE)) {
            return false;
        }
        String uin = message.getSndrUin();
        if (isChecked(uin)) {
            return false;
        }
        denyAuth(protocol, message);
        if (!(message instanceof PlainMessage)) {
            return true;
        }

        String msg = message.getText();
        // #sijapp cond.if modules_MAGIC_EYE is "true" #
        if (msg.length() < 256) {
    	    MagicEye.addAction(protocol, uin, "antispam", msg);
        // #sijapp cond.if modules_SOUND is "true" #         //eye sound
            Notify.playSoundNotification(Notify.NOTIFY_EYE); //eye sound
        // #sijapp cond.end #                                //eye sound
        }
        // #sijapp cond.end #
        if (message.isOffline()) {
            return Options.getBoolean(Options.OPTION_ANTISPAM_OFFLINE);
        }
        Contact contact = protocol.createTempContact(uin);

        String[] msgs = Util.explode(Options.getString(Options.OPTION_ANTISPAM_ANSWER), '\n');
        for (int i = 0; i < msgs.length; i++) {
            if (StringConvertor.stringEquals(msg, msgs[i])) {
                sendHelloMessage(contact);
                return true;
            }
        }
        sendQuestion(contact);
        return true;
    }
    
    public static boolean isSpam(Protocol protocol, Message message) {
        if (antiSpam.containsKeywords(message.getText())) {
            antiSpam.denyAuth(protocol, message);
            return true;
        }
        return antiSpam.isSpamMessage(protocol, message);
    }
}
// #sijapp cond.end #