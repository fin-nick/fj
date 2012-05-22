/*
 * JabberXStatusForm.java
 *
 * Created on 01.07.2010
 *
 */
// #sijapp cond.if protocols_JABBER is "true" #
// #sijapp cond.if modules_XSTATUSES is "true" #
package jimm.forms;


import DrawControls.icons.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.io.Storage;
import protocol.jabber.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.util.ResourceBundle;
import protocol.Protocol;


public class JabberXStatusForm extends SomeStatusForm implements CommandListener {
    private FormEx form;


    /** Creates a new instance of StatusForm */
    public JabberXStatusForm(Jabber protocol) {
        super(protocol);
        form = new FormEx("set_xstatus", "save", "back", this);
    }

    private void showXtrazForm() {
        int id = statusIndex;
        form.clearForm();
        form.addTextField(Options.OPTION_XTRAZ_TITLE, "status_message", xst_titles[id], 1024, TextField.ANY);
        form.endForm();
        form.show();
    }

    public void commandAction(Command command, Displayable displayable) {
        if (form.saveCommand == command) {
            setXStatus(statusIndex, form.getTextFieldValue(Options.OPTION_XTRAZ_TITLE));
            back();


        } else if (form.backCommand == command) {
            back();
        }
    }

    private static String[] xst_titles = new String[JabberXStatus.getXStatusCount()];
    private static String[] xst_descs  = new String[JabberXStatus.getXStatusCount()];
    static {
        try {
            Storage storage = new Storage("jabber-xstatus");
            storage.open(false);
            storage.loadXStatuses(xst_titles, xst_descs);
            storage.close();
        } catch (Exception e) {
        }
    }
    private void setXStatus(int id, String title) {
        if (0 <= id) {
            xst_titles[id] = StringConvertor.notNull(title);
            try {
                Storage storage = new Storage("jabber-xstatus");
                storage.open(true);
                storage.saveXStatuses(xst_titles, xst_descs);
                storage.close();
            } catch (Exception e) {
            }
        }
//        protocol.setXStatus(statusIndex, title);
        ((Jabber)protocol).setXStatus(statusIndex, title);
    }
    protected void addStatuses(MenuModel menu) {
        JabberXStatus status = new JabberXStatus();
                for (byte i = -1; i < JabberXStatus.getXStatusCount(); ++i) {
            status.setStatusIndex(i);
            menu.addItem(status.getName(), status.getIcon(), i);
        }
        menu.setDefaultItemCode(protocol.getProfile().xstatusIndex);
    }

    protected void statusSelected(int statusIndex) {
        if (statusIndex < 0) {
            setXStatus(statusIndex, null);
            back();


        } else {
            showXtrazForm();
        }
    }
}
// #sijapp cond.end #
// #sijapp cond.end #