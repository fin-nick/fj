/*
 * XtrazForm.java
 *
 * Created on 15 ���� 2007 �., 22:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_ICQ is "true" #
// #sijapp cond.if modules_XSTATUSES is "true" #
package jimm.forms;

import DrawControls.*;
import java.io.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.io.Storage;
import protocol.icq.*;
import protocol.icq.action.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.util.*;

import java.util.*;

import javax.microedition.lcdui.*;

/**
 *
 * @author vladimir
 */
public class IcqXStatusForm extends SomeStatusForm implements CommandListener {
    private FormEx form;

    /** Creates a new instance of XtrazForm */
    public IcqXStatusForm(Icq protocolIcq) {
        super(protocolIcq);
        form = new FormEx("set_xstatus", "save", "back", this);
    }

    private void showXtrazForm() {
        int id = statusIndex;
        form.clearForm();
        form.addTextField(Options.OPTION_XTRAZ_TITLE, "xtraz_title", xst_titles[id], 32, TextField.ANY);
        form.addTextField(Options.OPTION_XTRAZ_DESC, "xtraz_desc", xst_descs[id], 100, TextField.ANY);
        form.endForm();
        form.show();
    }
    
    public void commandAction(Command command, Displayable displayable) {
        if (form.saveCommand == command) {
            setXStatus(statusIndex,
                    form.getTextFieldValue(Options.OPTION_XTRAZ_TITLE),
                    form.getTextFieldValue(Options.OPTION_XTRAZ_DESC));
            back();
            
        } else if (form.backCommand == command) {
            back();
        }
    }

    /**************************************************************************/
    
    private static String[] xst_titles = new String[XStatus.getXStatusCount()];
    private static String[] xst_descs  = new String[XStatus.getXStatusCount()];
    static {
        try {
            Storage storage = new Storage("icq-xstatus");
            storage.open(false);
            storage.loadXStatuses(xst_titles, xst_descs);
            storage.close();
        } catch (Exception e) {
        }
    }
    private void setXStatus(int id, String title, String desc) {
        if (0 <= id) {
            xst_titles[id] = StringConvertor.notNull(title);
            xst_descs[id]  = StringConvertor.notNull(desc);
            try {
                Storage storage = new Storage("icq-xstatus");
                storage.open(true);
                storage.saveXStatuses(xst_titles, xst_descs);
                storage.close();
            } catch (Exception e) {
            }
        }
        ((Icq)protocol).setXStatus(id, title, desc);
    }

    protected void addStatuses(MenuModel menu) {
        XStatus xstatus = new XStatus();
		for (byte i = -1; i < XStatus.getXStatusCount(); ++i) {
            xstatus.setRawXstatus(i);
            menu.addItem(xstatus.getXStatusAsString(), xstatus.getIcon(), i);
        }
        menu.setDefaultItemCode(protocol.getProfile().xstatusIndex);
    }

    protected void statusSelected(int statusIndex) {
        if (statusIndex < 0) {
            setXStatus(statusIndex, null, null);
            back();
            
        } else {
            showXtrazForm();
        }
    }
}
// #sijapp cond.end #
// #sijapp cond.end #
