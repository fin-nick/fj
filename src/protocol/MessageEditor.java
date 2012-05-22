/*
 * MessageEditor.java
 *
 * Created on 8 Июнь 2010 г., 20:27
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import jimm.Jimm;
import jimm.cl.ContactList;
import jimm.ui.InputTextBox;
import jimm.util.ResourceBundle;

/**
 *
 * @author Vladimir Krukov
 */
public class MessageEditor implements CommandListener {
    private final InputTextBox messageTextbox = new InputTextBox("message", 5000, "send");
    private Contact writeMessageToUIN = null;
    
    /** Creates a new instance of MessageEditor */
    public MessageEditor() {
        messageTextbox.setCancelNotify(true);
        messageTextbox.setCommandListener(this);
    }
    public void writeMessage(Contact to, String message) {
        to.typing(true);
        /* If user want reply with quotation */ 
        if (null != message) {
            messageTextbox.setString(message);

        /* Keep old text if press "cancel" while last edit */ 
        } else if (writeMessageToUIN != to) {
            messageTextbox.setString(null);
        }
        if (writeMessageToUIN != to) {
            writeMessageToUIN = to;
            /* Display textbox for entering messages */
            messageTextbox.setCaption(ResourceBundle.getString("message") + " " + to.getName());
        }

        messageTextbox.show();
    }

    public void commandAction(Command c, Displayable d) {
        if (messageTextbox.isCancelCommand(c) || messageTextbox.isOkCommand(c)) {
            writeMessageToUIN.typing(false);
        }
        if (messageTextbox.isOkCommand(c)) {
            writeMessageToUIN.sendMessage(messageTextbox.getString(), true);
            if (writeMessageToUIN.hasChat()) {
                writeMessageToUIN.activate();
            } else {
                ContactList.activate();
            }
            messageTextbox.setString(null);
            return;
        }
    }
    public void insert(String text) {
        
    }
    public InputTextBox getTextBox() {
        return messageTextbox;
    }
    public boolean isActive(Contact c) {
        return c == writeMessageToUIN;
    }
}
