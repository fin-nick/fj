/*
 * StatusForm.java
 *
 * Created on 10 Июнь 2007 г., 13:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_MRIM is "true" #
// #sijapp cond.if modules_XSTATUSES is "true" #
package jimm.forms;

import DrawControls.icons.*;
import java.io.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.io.Storage;
import protocol.mrim.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.util.ResourceBundle;
import protocol.Protocol;

/**
 *
 * @author vladimir
*/
public class MrimXStatusForm extends SomeStatusForm implements CommandListener {
    private MrimXStatus status = new MrimXStatus();
    private FormEx form;

    /** Creates a new instance of StatusForm */
    public MrimXStatusForm(Mrim protocol) {
        super(protocol);        
        form = new FormEx("set_xstatus", "save", "back", this);
    }
    
    private void showXtrazForm() {
        int id = statusIndex;
        form.clearForm();
        MrimXStatus status = ((Mrim)protocol).getXStatus();
        form.addTextField(Options.OPTION_XTRAZ_TITLE, "xtraz_title",
                xst_titles[id], 32, TextField.ANY);
        form.addTextField(Options.OPTION_XTRAZ_DESC, "xtraz_desc", 
                xst_descs[id], 64, TextField.ANY);
        form.endForm();
        form.show();
    }

    public void commandAction(Command c, Displayable d) {
        if (form.saveCommand == c) {
            setStatus(statusIndex,
                    form.getTextFieldValue(Options.OPTION_XTRAZ_TITLE),
                    form.getTextFieldValue(Options.OPTION_XTRAZ_DESC));
        }
        back();
    }
    private void setStatus(int statusIndex, String title, String desc) {
        protocol.getProfile().xstatusIndex = (byte)statusIndex;
        protocol.getProfile().xstatusTitle = title;
        protocol.getProfile().xstatusDescription = desc;
        Options.safeSave();
        if (-1 != statusIndex) {
            xst_titles[statusIndex] = StringConvertor.notNull(title);
            xst_descs[statusIndex]  = StringConvertor.notNull(desc);
            try {
                Storage storage = new Storage("mrim-xstatus");
                storage.open(true);
                storage.saveXStatuses(xst_titles, xst_descs);
                storage.close();
            } catch (Exception e) {
            }
        }
        setXStatus((Mrim)protocol);
        ContactList.updateMainMenu();
    }
    public static void setXStatus(Mrim protocol) {
        int statusIndex = protocol.getProfile().xstatusIndex;
        String title    = "";
        String desc     = "";
        if (-1 < statusIndex) {
            title = protocol.getProfile().xstatusTitle;
            desc = protocol.getProfile().xstatusDescription;
        }
        protocol.setXStatus(statusIndex, title, desc);
    }

    private static String[] xst_titles = new String[MrimXStatus.getXStatusCount()];
    private static String[] xst_descs  = new String[MrimXStatus.getXStatusCount()];
    static {
        try {
            Storage storage = new Storage("mrim-xstatus");
            storage.open(false);
            storage.loadXStatuses(xst_titles, xst_descs);
            storage.close();
        } catch (Exception e) {
        }
    }

    protected void addStatuses(MenuModel menu) {
        for (int i = -1; i < MrimXStatus.getXStatusCount(); ++i) {
            status.setStatusIndex(i);
            menu.addItem(status.getName(), status.getIcon(), i);
        }
        menu.setDefaultItemCode(protocol.getProfile().xstatusIndex);
    }

    protected void statusSelected(int statusIndex) {
        if (-1 == statusIndex) {
    	    setStatus(statusIndex, "", "");
    	    back();
            return;
        }
        showXtrazForm();
    }
}
// #sijapp cond.end #
// #sijapp cond.end #
