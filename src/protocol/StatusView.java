/*
 * StatusView.java
 *
 * Created on 12 Август 2010 г., 21:26
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import DrawControls.icons.Icon;
import jimm.comm.Util;
import jimm.ui.*;
import jimm.ui.menu.*;
import protocol.icq.*;
import jimm.util.ResourceBundle; //add
import javax.microedition.lcdui.Font; //add

/**
 *
 * @author Vladimir Kryukov
 */
public class StatusView extends TextListEx implements SelectListener {
    public static final int INFO_MENU_COPY           = 1;
    public static final int INFO_MENU_COPY_ALL       = 2;
    public static final int INFO_MENU_BACK           = 3;
    public static final int INFO_MENU_GOTO_URL       = 4;
    public static final int ST_GET_STATUS_MSG        = 5;
    public static final int ST_GET_XSTATUS_MSG       = 6;
    private Contact contact;
    
    
    /** Creates a new instance of StatusView */
    public StatusView() {
        super(null);
    }
    public void select(Select select, MenuModel model, int cmd) {
        switch (cmd) {
            case INFO_MENU_COPY:
            case INFO_MENU_COPY_ALL:
                copy(INFO_MENU_COPY_ALL == cmd);
                restore();
                break;
            case INFO_MENU_BACK:
                back();
                clear();
                break;
            case INFO_MENU_GOTO_URL:
                TextListEx tx = new TextListEx("").gotoURL(getCurrText(0, false));
                if (null != tx) {
                    tx.show();
                }
                break;
            // #sijapp cond.if protocols_ICQ is "true" #
            case ST_GET_STATUS_MSG:
                ((IcqContact)contact).requestStatusMessage();
                restore();
                break;
            // #sijapp cond.if modules_XSTATUSES is "true" #
            case ST_GET_XSTATUS_MSG:
                protocol.icq.plugin.XtrazMessagePlugin.request((IcqContact)contact);
                restore();
                break;
            // #sijapp cond.end #
            // #sijapp cond.end #
        }
    }

    // #sijapp cond.if modules_CLIENTS is "true" #
    public void addClient(Icon icon, String name) {
        addBigText(ResourceBundle.getString("dc_info") + "\n", THEME_TEXT, Font.STYLE_BOLD, -1); //add
        addPlain(icon, name);
    }
    // #sijapp cond.end #
    public void addStatus(Protocol protocol, Status status) {
        addBigText(ResourceBundle.getString("status") + "\n", THEME_TEXT, Font.STYLE_BOLD, -1); //add
        StatusInfo info = protocol.getStatusInfo();
        addPlain(info.getIcon(status), info.getName(status)); //it is changed
    }
    public void addStatus(Icon icon, String name) {
        addPlain(icon, name); //it is changed
    }
    public void addStatusText(String text) {
        add(null, null, text);
    }
    protected MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        menu.addItem("copy_text",     INFO_MENU_COPY);
        menu.addItem("copy_all_text", INFO_MENU_COPY_ALL);
        if ((1 < getCurrTextIndex()) && Util.hasURL(getCurrText(0, false))) {
            menu.addItem("goto_url", INFO_MENU_GOTO_URL);
        }
        ///contact
        // #sijapp cond.if protocols_ICQ is "true" #
        if ((contact instanceof IcqContact) && contact.isOnline()) {
            IcqContact c = (IcqContact)contact;
            menu.addItem("reqstatmsg", ST_GET_STATUS_MSG);
            // #sijapp cond.if modules_XSTATUSES is "true" #
            if ((XStatus.noneXStatus != c.getXStatus())
                    && c.getClient().hasCapability(Client.CAPF_AIM_SERVERRELAY)) {
                menu.addItem("reqxstatmsg", ST_GET_XSTATUS_MSG);
            }
            // #sijapp cond.end #
        }
        // #sijapp cond.end #
        menu.addItem("back", INFO_MENU_BACK);
        menu.setActionListener(this);
        return menu;
    }
    public void init(Contact c) {
        contact = c;
        setMenuCodes(INFO_MENU_BACK, INFO_MENU_COPY);
        lock();
        clear();
        setCaption(contact.getName());
        addBigText(ResourceBundle.getString("main_info") + "\n", THEME_TEXT, Font.STYLE_BOLD, -1); //add
        add(contact.getProtocol().getUinName(), contact.getUin());
    }
    public void showIt() {
        unlock();
        super.show();
    }
}
