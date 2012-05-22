/*
 * ProtocolBranch.java
 *
 * Created on 24 Март 2010 г., 15:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if modules_MULTI is "true" #
package DrawControls.tree;

import DrawControls.icons.Icon;
import javax.microedition.lcdui.Font;
import jimm.Options;
import jimm.ui.base.CanvasEx;
import jimm.ui.menu.MenuModel;
import protocol.*;
/**
 *
 * @author Vladimir Kryukov
 */
public class ProtocolBranch extends TreeBranch {
    private Protocol protocol;
    public ProtocolBranch(Protocol p) {
        protocol = p;
        setExpandFlag(false);
    }
    public boolean isProtocol(Protocol p) {
        return protocol == p;
    }
    public Protocol getProtocol() {
        return protocol;
    }
    
    public String getText() {
        return protocol.getUserId();
    }
    
    public int getNodeWeight() {
        return 0;
    }
    public final void getLeftIcons(Icon[] leftIcons) {
        leftIcons[0] = protocol.getCurrentStatusIcon();
        // #sijapp cond.if modules_XSTATUSES is "true" #
        Icon x = null;
        // #sijapp cond.if protocols_ICQ is "true" #
        if (protocol  instanceof protocol.icq.Icq) {
            x = ((protocol.icq.Icq)protocol).getXStatus().getIcon();
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_MRIM is "true" #
        if (protocol  instanceof protocol.mrim.Mrim) {
            x = ((protocol.mrim.Mrim)protocol).getXStatus().getIcon();
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_JABBER is "true" #
        if (protocol  instanceof protocol.jabber.Jabber) {
            x = ((protocol.jabber.Jabber)protocol).getXStatus().getIcon();
        }
        // #sijapp cond.end #
        if (!Options.getBoolean(Options.OPTION_REPLACE_STATUS_ICON) || x == null) {
            leftIcons[1] = x;
        } else {
            leftIcons[0] = x;
            leftIcons[1] = null;
        }
        // #sijapp cond.end #
    }
    public final void getRightIcons(Icon[] rightIcons) {
        if (!isExpanded() && (0 < protocol.getUnreadMessCount())) {
            rightIcons[0] = jimm.chat.message.Message.msgIcons.iconAt(jimm.chat.message.Message.ICON_MSG_NONE);
        }
    }
    public MenuModel getContextMenu() {
        MenuModel menu = new MenuModel();
        jimm.cl.ContactList cl = jimm.cl.ContactList.getInstance();
        cl.protocolMenu(menu, protocol);
        menu.setActionListener(cl);
        return menu;
    }
}
// #sijapp cond.end #