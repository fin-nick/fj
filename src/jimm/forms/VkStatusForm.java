/*
 * StatusForm.java
 *
 * Created on 10 Июнь 2007 г., 13:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_VK is "true" #
package jimm.forms;

import DrawControls.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import protocol.vk.*;
import jimm.ui.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author vladimir
 */
public class VkStatusForm implements CommandListener {
    private InputTextBox message = new InputTextBox("status_message", 512);
    
    private VK vk;
    public VkStatusForm(VK protocol) {
        vk = protocol;
        message.setCommandListener(this);
    }
    public void show() {
        message.show();
    }
    
    private void setStatus(int statusIndex, String statusMsg) {
        vk.setOnlineStatus(statusIndex, statusMsg);
        ContactList.updateMainMenu();
    }
    public void commandAction(Command c, Displayable d) {
        if (message.isOkCommand(c)) {
            vk.setStatus(message.getString());
            message.back();
        }
    }
}
// #sijapp cond.end #
