/*
 * PrivateStatusForm.java
 *
 * Created on 10 Июнь 2007 г., 14:24
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if modules_SERVERLISTS is "true" #
package jimm.forms;

import DrawControls.*;
import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import protocol.icq.*;
import protocol.icq.action.*;
import protocol.mrim.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import protocol.Protocol;

/**
 *
 * @author vladimir
 */
public class PrivateStatusForm extends SomeStatusForm {
    final private static ImageList privateStatusIcons = ImageList.createImageList("/privatestatuses.png");
    
    public static final int PSTATUS_ALL = 0;
    public static final int PSTATUS_VISIBLE_ONLY = 1;
    public static final int PSTATUS_NOT_INVISIBLE = 2;
    public static final int PSTATUS_CL_ONLY = 3;
    public static final int PSTATUS_NONE = 4;

    /** Creates a new instance of PrivateStatusForm */
    public PrivateStatusForm(Protocol protocol) {
        super(protocol);
    }
        
    public static Icon getIcon(Protocol protocol) {
        return privateStatusIcons.iconAt(protocol.getPrivateStatus());
    }

    protected void addStatuses(MenuModel menu) {
        // #sijapp cond.if protocols_ICQ is "true" #
        if (protocol instanceof Icq) {
            menu.addItem("ps_all",               privateStatusIcons.iconAt(0), PSTATUS_ALL);
            menu.addItem("ps_visible_list",      privateStatusIcons.iconAt(1), PSTATUS_VISIBLE_ONLY);
            menu.addItem("ps_exclude_invisible", privateStatusIcons.iconAt(2), PSTATUS_NOT_INVISIBLE);
            menu.addItem("ps_contact_list",      privateStatusIcons.iconAt(3), PSTATUS_CL_ONLY);
            menu.addItem("ps_none",              privateStatusIcons.iconAt(4), PSTATUS_NONE);
        }
        // #sijapp cond.end # 
        // #sijapp cond.if protocols_MRIM is "true" #
        if (protocol instanceof Mrim) {
            menu.addItem("ps_visible_list",      privateStatusIcons.iconAt(1), PSTATUS_VISIBLE_ONLY);
            menu.addItem("ps_exclude_invisible", privateStatusIcons.iconAt(2), PSTATUS_NOT_INVISIBLE);
        }
        // #sijapp cond.end # 
        menu.setDefaultItemCode(protocol.getPrivateStatus());
    }

    protected void statusSelected(int statusIndex) {
        protocol.setPrivateStatus((byte)statusIndex);
        Options.setInt(Options.OPTION_PRIVATE_STATUS, statusIndex);
        Options.safeSave();
        back();
    }
}
// #sijapp cond.end #
