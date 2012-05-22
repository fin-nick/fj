/*
 * IcqClientForm.java
 *
 * Created on 10 Июнь 2007 г., 14:59
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_ICQ is "true" #
// #sijapp cond.if modules_CLIENTS is "true" #
package jimm.forms;

import DrawControls.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import protocol.icq.*;
import protocol.icq.action.*;
import jimm.ui.*;
import jimm.ui.menu.*;

/**
 *
 * @author vladimir
 */
public class IcqClientForm extends SomeStatusForm {
    public IcqClientForm(Icq icq) {
        super(icq);
    }

    protected void addStatuses(MenuModel menu) {
        byte[] clients = ClientDetector.instance.getClientsForMask();
        Client client = new Client();
        for (int i = 0; i < clients.length; ++i) {
            client.setClient(clients[i], null);
            menu.addItem(client.getName(), client.getIcon(), clients[i]);
        }
        menu.setDefaultItemCode(Options.getInt(Options.OPTIONS_CLIENT));
    }

    protected void statusSelected(int statusIndex) {
        ((Icq)protocol).setClient(statusIndex);
        back();
    }
}
// #sijapp cond.end #
// #sijapp cond.end #