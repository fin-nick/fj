/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/ContactList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis
 *******************************************************************************/

package jimm.cl;

import DrawControls.icons.Icon;
import DrawControls.tree.*;
import DrawControls.*;
import jimm.*;
import jimm.chat.*;
import jimm.chat.message.Message;
import jimm.chat.message.PlainMessage;
import jimm.comm.*;
import jimm.forms.*;
import jimm.forms.ManageContactListForm;
import jimm.modules.*;
import jimm.modules.traffic.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.util.*;

import java.util.*;
import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import DrawControls.icons.*; //add for a menu icons
import protocol.*;
import protocol.icq.*;
import protocol.mrim.*;
import protocol.jabber.*;

public final class ContactList implements CommandListener, SelectListener, ContactListListener {


    private final TextListEx aboutTextList = new TextListEx(null);
    private final MenuModel mainMenu = new MenuModel();
    private MessageEditor editor;
    private Select mainMenuView;
    private VirtualContactList contactList;

    private ContactList() {
        contactList = new VirtualContactList();
        contactList.setCLListener(this);
        mainMenu.setActionListener(this);
        mainMenuView = new Select(mainMenu);
    }
    public void initMessageEditor() {
        editor = new MessageEditor();
    }

    public byte getProtocolType(Profile account) {
        for (int i = 0; i < Profile.protocolTypes.length; ++i) {
            if (account.protocolType == Profile.protocolTypes[i]) {
                return account.protocolType;
            }
        }
        return Profile.protocolTypes[0];
    }
    private boolean isValidProtocol(Protocol p, byte type) {
        switch (type) {
            // #sijapp cond.if protocols_ICQ is "true" #
            case Profile.PROTOCOL_ICQ:
                if (p instanceof Icq) {
                    return true;
                }
                break;
                // #sijapp cond.end #
                // #sijapp cond.if protocols_MRIM is "true" #
            case Profile.PROTOCOL_MRIM:
                if (p instanceof Mrim) {
                    return true;
                }
                break;
                // #sijapp cond.end #
                // #sijapp cond.if protocols_JABBER is "true" #
            case Profile.PROTOCOL_JABBER:
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_LJ:
            case Profile.PROTOCOL_VK:
            case Profile.PROTOCOL_QIP:
            case Profile.PROTOCOL_YANDEX:
            case Profile.PROTOCOL_GTALK:
            case Profile.PROTOCOL_OVI:
                if (p instanceof Jabber) {
                    return true;
                }
                break;
                // #sijapp cond.end #
                // #sijapp cond.if protocols_MSN is "true" #
            case Profile.PROTOCOL_MSN:
                if (p instanceof protocol.msn.Msn) {
                    return true;
                }
                break;
                // #sijapp cond.end #
        }
        return false;
    }
    // #sijapp cond.if modules_MULTI is "true" #
    public int addProtocols(Vector accounts) {
        int added = 0;
        int count = contactList.getModel().getProtocolCount();
        Protocol[] protocols = new Protocol[count];
        for (int i = 0; i < count; ++i) {
            protocols[i] = contactList.getModel().getProtocol(i);
        }
        contactList.getModel().removeAllProtocols();
        for (int i = 0; i < accounts.size(); ++i) {
            Profile profile = (Profile)accounts.elementAt(i);
            if (!profile.isActive) {
                continue;
            }
            for (int j = 0; j < count; ++j) {
                Protocol protocol = protocols[j];
                if ((null != protocol) && (protocol.getProfile() == profile)
                        && isValidProtocol(protocol, profile.protocolType)) {
                    contactList.getModel().addProtocol(protocol);
                    contactList.invalidateTree();
                    contactList.update(protocol);
                    added++;
                    profile = null;
                    protocols[j] = null;
                    break;
                }
            }
            if (null != profile) {
                addProtocol(profile);
                added++;
            }
        }
        for (int j = 0; j < count; ++j) {
            Protocol protocol = protocols[j];
            if (null != protocol) {
                protocol.disconnect();
                protocol.safeSave();
                protocol.dismiss();
            }
        }
        return added;
    }
    // #sijapp cond.end #
    public void addProtocol(Profile account) {
        Protocol protocol = null;
        byte type = getProtocolType(account);
        switch (type) {
            // #sijapp cond.if protocols_ICQ is "true" #
            case Profile.PROTOCOL_ICQ:
                protocol = new Icq();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MRIM is "true" #
            case Profile.PROTOCOL_MRIM:
                protocol = new Mrim();
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_JABBER is "true" #
            case Profile.PROTOCOL_JABBER:
                // #sijapp cond.if modules_MULTI is "true" #
            case Profile.PROTOCOL_GTALK:
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_LJ:
            case Profile.PROTOCOL_YANDEX:
            case Profile.PROTOCOL_VK:
            case Profile.PROTOCOL_QIP:
            case Profile.PROTOCOL_OVI:
                // #sijapp cond.end #
                protocol = new Jabber(type);
                break;
            // #sijapp cond.end #
            // #sijapp cond.if protocols_MSN is "true" #
            case Profile.PROTOCOL_MSN:
                protocol = new protocol.msn.Msn();
                break;
            // #sijapp cond.end #
        }
        if (null == protocol) {
            return;
        }
        protocol.setAccount(account);
        contactList.getModel().addProtocol(protocol);

        protocol.safeLoad();

        // #sijapp cond.if modules_SERVERLISTS is "true" #
        protocol.setPrivateStatus((byte)Options.getInt(Options.OPTION_PRIVATE_STATUS));
        // #sijapp cond.end #

        protocol.init();
        contactList.invalidateTree();
        contactList.update(protocol);
    }
    public Protocol getProtocol(Profile profile) {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (null == p) {
                continue;
            }
            if (p.getProfile() == profile) {
                return p;
            }
        }
        return null;
    }

    private static final ContactList instance = new ContactList();
    public static ContactList getInstance() {
        return instance;
    }

    
    public static void activate() {
        instance.contactList.buildTree(instance.contactList.getCurrentProtocol());
        instance.updateUnreadedMessageCount();
        instance.contactList.show();
    }
    public void updateAbout() {
        aboutTextList.initAbout();
    }
    
    public MessageEditor getMessageEditor() {
        return editor;
    }

    /* *********************************************************** */
    final static public int SORT_BY_STATUS = 0;
    final static public int SORT_BY_ONLINE = 1;
    final static public int SORT_BY_NAME   = 2;
    
    /* *********************************************************** */
    

    //==================================//
    //                                  //
    //    WORKING WITH CONTACTS TREE    //
    //                                  //  
    //==================================//
    public Protocol getCurrentProtocol() {
        return contactList.getCurrentProtocol();
    }
    public boolean isConnected() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if ((null != p) && p.isConnected()) {
                return true;
            }
        }
        return false;
    }
    public void disconnect() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if ((null != p) && p.isConnected()) {
                p.disconnect();
            }
        }
    }
    public void safeSave() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (null != p) {
                p.safeSave();
            }
        }
    }

    public void collapseAll() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (null == p) continue;
            Vector groups = p.getGroupItems();
            for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
                ((TreeBranch)groups.elementAt(groupIndex)).setExpandFlag(false);
            }
        }
        contactList.setCurrentItem(0);
        update();
    }
    public void update(Protocol protocol) {
        contactList.invalidateTree();
        contactList.update(protocol);
    }
    public void update() {
        for (int i = contactList.getModel().getProtocolCount() - 1; 0 <= i; --i) {
            Protocol protocol = contactList.getModel().getProtocol(i);
            if (null != protocol) {
                contactList.invalidateTree();
                contactList.update(protocol);
            }
        }
    }
    public VirtualContactList getManager() {
        return contactList;
    }

    private final boolean hasContact(Contact contact) {
        return contact.getProtocol().inContactList(contact);
    }

    // Adds the given message to the message queue of the contact item
    // identified by the given UIN
    public void addMessage(Contact contact, Message message) {
        Protocol p = contact.getProtocol();
        if (!p.isConnected()) {
            return;
        }

        boolean isSingleUserContact = contact.isSingleUserContact();
        boolean isMultiUserNotify = false;
        // #sijapp cond.if protocols_JABBER is "true" #
        if (!isSingleUserContact && !message.isOffline()) {
            String msg = message.getText();
            String myName = contact.getMyName();
            // regexp: "^nick. "
            isSingleUserContact = msg.startsWith(myName)
                    && msg.startsWith(" ", myName.length() + 1);
            isMultiUserNotify = ChatTextList.isHighlight(msg, myName);
        }
        // #sijapp cond.end #

        boolean isPaused = false;
        // #sijapp cond.if target is "MIDP2" #
        isPaused = Jimm.isPaused() && isCollapsible();
		if (isPaused && isSingleUserContact && Options.getBoolean(Options.OPTION_BRING_UP)) {
            Jimm.setMinimized(false);
            isPaused = false;
        }
		// #sijapp cond.end #

        // #sijapp cond.if modules_SOUND is "true" #
        if (message.isOffline()) {
            // Offline messages don't play sound
            
        } else if (isSingleUserContact) {
            if (contact.isSingleUserContact()
                    && contact.isAuth()
                    && message.isWakeUp()
                    && !contact.isTemp()) {
                Notify.playSoundNotification(Notify.NOTIFY_ALARM);

            } else {
                Notify.playSoundNotification(Notify.NOTIFY_MESSAGE);
            }
                
        // #sijapp cond.if protocols_JABBER is "true" #
        } else if (isMultiUserNotify) {
            Notify.playSoundNotification(Notify.NOTIFY_MULTIMESSAGE);
        } else {                                               //other sound
            Notify.playSoundNotification(Notify.NOTIFY_OTHER); //other sound
        // #sijapp cond.end #
        }
        // #sijapp cond.end#

        // #sijapp cond.if modules_LIGHT is "true" #
        if (isSingleUserContact || isMultiUserNotify) {
            CustomLight.setLightMode(CustomLight.ACTION_MESSAGE);
        }
        // #sijapp cond.end#

        // Notify splash canvas
        if (Jimm.isLocked()) {
            SplashCanvas.messageAvailable();
        }
        if (hasContact(contact)) {
            // Update contact list
            contactList.updateContactWithNewMessage(contact, isSingleUserContact);
        }
        if (!isPaused && isSingleUserContact
                && (message instanceof PlainMessage) && !message.isOffline()) {
            String text = message.getProcessedText();
            if (text.startsWith(PlainMessage.CMD_ME)) {
                text = text.substring(4);
                if (0 == text.length()) {
                    return;
                }
                text = message.getName() + " " + text;
            }
            String from = contact.getName();
            // #sijapp cond.if protocols_JABBER is "true" #
            if (!contact.isSingleUserContact()) {
                from = message.getName() + "@" + from;
            }
            // #sijapp cond.end #
            contact.showPopupWindow(from, text);
        }
        if (null == VirtualList.getMessageIcon()) {
            updateUnreadedMessageCount();
        }
    }
    public final void markMessages(Contact c) {
        if (null != VirtualList.getMessageIcon()) {
            updateUnreadedMessageCount();
        }
    }
    private void updateUnreadedMessageCount() {
        Icon icon = null;
        if (0 < getUnreadMessCount()) {
            icon = Message.msgIcons.iconAt(Message.ICON_MSG_NONE);
        }
        if (icon != VirtualList.getMessageIcon()) {
            VirtualList.setMessageIcon(icon);
            jimm.ui.base.NativeCanvas.getInstance().repaint();
        }
    }
 
    

    // #sijapp cond.if modules_SOUND is "true" #
    public final void beginTyping(Contact item, boolean type) {
        // #sijapp cond.if modules_SOUND is "true" #
        if (type && item.getProtocol().isConnected()) {
            Notify.playSoundNotification(Notify.NOTIFY_TYPING);
        }
        // #sijapp cond.end #
        contactList.invalidate();
    }
    // #sijapp cond.end#
    public void updateTitle() {
        contactList.updateTitle();
    }
        
    private Contact currentContact;
    public final Contact getCurrentContact() {
        return currentContact;
    }
    public final void setCurrentContact(Contact contact) {
        currentContact = contact;
    }

	/* Static constants for menu actios */
	private static final int MENU_CONNECT    = 1;
	private static final int MENU_DISCONNECT = 2;
    private static final int MENU_DISCO      = 3;
	private static final int MENU_OPTIONS    = 4;
	private static final int MENU_TRAFFIC    = 5;
	private static final int MENU_KEYLOCK    = 6;
	private static final int MENU_STATUS     = 7;
	private static final int MENU_XSTATUS    = 8;
	private static final int MENU_PRIVATE_STATUS = 9;
	private static final int MENU_GROUPS     = 10;
    private static final int MENU_SEND_SMS   = 11;
	private static final int MENU_ABOUT      = 12;
	private static final int MENU_MINIMIZE   = 13;
	private static final int MENU_SOUND      = 14;
	private static final int MENU_MYSELF     = 15;
	private static final int MENU_CLIENT     = 16;
    private static final int MENU_DEBUGLOG   = 17;
    private static final int MENU_MAGIC_EYE  = 18;
    private static final int MENU_MICROBLOG  = 19;
    private static final int MENU_TEST       = 20;
	private static final int MENU_EXIT       = 21;

    /////////////////////////////////////////////////////////////////

    // #sijapp cond.if target="SIEMENS2"#
    private MenuModel siemensCommandSelect;
    private Config siemensCommands = new Config().loadLocale("/siemens-commands.txt");
    private MenuModel getSiemensMenu() {
        if ((null == siemensCommandSelect) && (0 < siemensCommands.getKeys().length)) {
            siemensCommandSelect = new MenuModel();
            for (int i = 0; i < siemensCommands.getKeys().length; ++i) {
                if (!siemensCommands.getValues()[i].startsWith("native:")) {
                    continue;
                }
                siemensCommandSelect.addRawItem(siemensCommands.getKeys()[i], null, i);
                
            }
            siemensCommandSelect.setActionListener(this);
        }
        return siemensCommandSelect;
    }
    private void execSiemensCommand(int cmdNum) {
        try {
            Jimm.platformRequestUrl(siemensCommands.getValues()[cmdNum]);
        } catch (Exception e) {
        }
    }
    // #sijapp cond.end#
    /** ************************************************************************* */
    public boolean isCollapsible() {
        // #sijapp cond.if target is "MIDP2" #
        return Jimm.isPhone(Jimm.PHONE_SE) || Jimm.isPhone(Jimm.PHONE_NOKIA_S60);
        // #sijapp cond.else #
        return false;
        // #sijapp cond.end #
    }
    private final boolean isSmsSupported() {
        // #sijapp cond.if protocols_MRIM is "true" #
        Protocol p = getCurrentProtocol();
        if ((p instanceof Mrim) && p.isConnected()) {
            return true;
        }
        // #sijapp cond.end #

        // #sijapp cond.if target is "SIEMENS1" #
        return true;
        // #sijapp cond.elseif target is "MIDP2"| target is "SIEMENS2" #
        // #sijapp cond.if modules_FILES="true"#
        return !isCollapsible();
        // #sijapp cond.else #
        return false;
        // #sijapp cond.end #
        // #sijapp cond.else #
        return false;
        // #sijapp cond.end #
    }

    
    /* Builds the main menu (visual list) */
    public void activateMainMenu() {
        updateMenu();
        mainMenu.setDefaultItemCode(MENU_STATUS);
        mainMenuView.show();
    }

    public void protocolMenu(MenuModel mainMenu, Protocol protocol) {
        if (protocol.isConnecting()) {
            mainMenu.addItem("disconnect",  MENU_DISCONNECT);
            return;
        }
        if (protocol.isConnected()) {
            mainMenu.addItem("disconnect", menuIcons.iconAt(6),  MENU_DISCONNECT); //menu icons
            
        } else {
            mainMenu.addItem("connect", menuIcons.iconAt(5),  MENU_CONNECT); //menu icons
        }
        mainMenu.addItem("set_status",  MENU_STATUS);
        // #sijapp cond.if modules_XSTATUSES is "true" #
        mainMenu.addItem("set_xstatus", MENU_XSTATUS);
        // #sijapp cond.end #
        // #sijapp cond.if protocols_ICQ is "true" #
        if (protocol instanceof Icq) {
            // #sijapp cond.if modules_SERVERLISTS is "true" #
            mainMenu.addItem("private_status", MENU_PRIVATE_STATUS);
            // #sijapp cond.end #
            // #sijapp cond.if modules_CLIENTS is "true" #
            if (1 < ClientDetector.instance.getClientsForMask().length) {
                mainMenu.addItem("client", MENU_CLIENT);
            }
            // #sijapp cond.end #
        }
        // #sijapp cond.end #
        if (protocol.isConnected()) {
            boolean hasVCard = true;
            // #sijapp cond.if protocols_JABBER is "true" #
            if (protocol instanceof Jabber) {
                hasVCard = ((Jabber)protocol).hasVCardEditor();
                if (((Jabber)protocol).hasS2S()) {
                    mainMenu.addItem("service_discovery", menuIcons.iconAt(13), MENU_DISCO); //menu icons
                }
            }
            // #sijapp cond.end #
            mainMenu.addItem("manage_contact_list", menuIcons.iconAt(10), MENU_GROUPS); //menu icons
            if (hasVCard) {
                mainMenu.addItem("myself", menuIcons.iconAt(12), MENU_MYSELF); //menu icons
            }
            // #sijapp cond.if protocols_MRIM is "true" #
            if (protocol instanceof Mrim) { 
                mainMenu.addItem("microblog", MENU_MICROBLOG);
            }
            // #sijapp cond.end #
        }
        if (isSmsSupported()) {
            mainMenu.addItem("send_sms", menuIcons.iconAt(2), MENU_SEND_SMS); //menu icons
        }
        
        mainMenu.setItem(MENU_STATUS, "set_status", protocol.getStatusInfo().getIcon(protocol.getStatus()));
        // #sijapp cond.if protocols_MRIM is "true" #
        if (protocol instanceof Mrim) {
            final Mrim mrim = (Mrim)protocol;
            // #sijapp cond.if modules_XSTATUSES is "true" #
            mainMenu.setItem(MENU_XSTATUS, "set_xstatus", mrim.getXStatus().getIcon());
            // #sijapp cond.end #
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_ICQ is "true" #
        if (protocol instanceof Icq) {
            final Icq icq = (Icq)protocol;
            // #sijapp cond.if modules_XSTATUSES is "true" #
            mainMenu.setItem(MENU_XSTATUS, "set_xstatus", icq.getXStatus().getIcon());
            // #sijapp cond.end #
            // #sijapp cond.if modules_CLIENTS is "true" #
            mainMenu.setItem(MENU_CLIENT, "client", icq.getClient().getIcon());
            // #sijapp cond.end #
            // #sijapp cond.if modules_SERVERLISTS is "true" #
            mainMenu.setItem(MENU_PRIVATE_STATUS, "private_status", PrivateStatusForm.getIcon(protocol));
            // #sijapp cond.end #
        }
        // #sijapp cond.end #
        // #sijapp cond.if protocols_JABBER is "true" #
        if (protocol instanceof Jabber) {
            final Jabber jabber = (Jabber)protocol;
            // #sijapp cond.if modules_XSTATUSES is "true" #
            mainMenu.setItem(MENU_XSTATUS, "set_xstatus", jabber.getXStatus().getIcon());
            // #sijapp cond.end #
        }
        // #sijapp cond.end #

    }

    private static final ImageList menuIcons = ImageList.createImageList("/menuicons.png"); //add for a menu icons

    protected void updateMenu() {
        final Protocol protocol = getCurrentProtocol();

        int currentCommand = mainMenuView.getSelectedItemCode();
        mainMenu.clean();
        // #sijapp cond.if modules_MULTI is "true" #
        mainMenu.addItem("keylock_enable", menuIcons.iconAt(0),  MENU_KEYLOCK); //menu icons
        // #sijapp cond.else#
        if (protocol.isConnected()) {
            mainMenu.addItem("keylock_enable", menuIcons.iconAt(0),  MENU_KEYLOCK); //menu icons
        }
        // #sijapp cond.end#
        protocolMenu(mainMenu, protocol);
        mainMenu.addItem("options_lng", menuIcons.iconAt(1), MENU_OPTIONS); //menu icons
        
        // #sijapp cond.if modules_SOUND is "true" #
        mainMenu.addItem("#sound_on", MENU_SOUND);
        // #sijapp cond.end#
        
        // #sijapp cond.if modules_MAGIC_EYE is "true" #
        mainMenu.addItem("magic eye", menuIcons.iconAt(11), MENU_MAGIC_EYE); //menu icons
        // #sijapp cond.end#
        // #sijapp cond.if modules_TRAFFIC is "true" #
        mainMenu.addItem("traffic_lng", menuIcons.iconAt(7), MENU_TRAFFIC); //menu icons
        // #sijapp cond.end#
        mainMenu.addItem("about", menuIcons.iconAt(14), MENU_ABOUT); //menu icons
        // #sijapp cond.if target is "MIDP2" #
        if (isCollapsible()) {
            mainMenu.addItem("minimize", menuIcons.iconAt(8), MENU_MINIMIZE); //menu icons
        }
        // #sijapp cond.elseif target is "SIEMENS2" #
        if (null == getSiemensMenu()) {
            mainMenu.addItem("minimize", menuIcons.iconAt(8), MENU_MINIMIZE); //menu icons
        } else {
            mainMenu.addEllipsisItem("minimize", null, MENU_MINIMIZE);
        }
        // #sijapp cond.end#
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        mainMenu.addItem("debug log",menuIcons.iconAt(15), MENU_DEBUGLOG); //menu icons & debug log down
        mainMenu.addItem("test",menuIcons.iconAt(15), MENU_TEST); //menu icons
        // #sijapp cond.end#
        mainMenu.addItem("exit", menuIcons.iconAt(9), MENU_EXIT); //menu icons

        // #sijapp cond.if modules_SOUND is "true" #
        if (Options.getBoolean(Options.OPTION_SILENT_MODE)) {            //menu icons
        mainMenu.setItem(MENU_SOUND, "#sound_on", menuIcons.iconAt(3));  //menu icons
        } else {                                                         //menu icons
        mainMenu.setItem(MENU_SOUND, "#sound_off", menuIcons.iconAt(4)); //menu icons
        }                                                                //menu icons
        // #sijapp cond.end#
        mainMenu.setDefaultItemCode(currentCommand);
        mainMenuView.setModel(mainMenu);
        mainMenuView.invalidate();
    }

    /* Activates the main menu */
    public static void updateMainMenu() {
        instance.updateMenu();
    }

    public int getUnreadMessCount() {
        int count = 0;
        for (int i = 0; i < contactList.getModel().getProtocolCount(); ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (null != p) {
                count += p.getUnreadMessCount();
            }
        }
        return count;
    }
    private void doExit(boolean anyway) {
        if (!anyway && (0 < getUnreadMessCount())) {
            JimmUI.attentionBox(ResourceBundle.getString("have_unread_mess"), this);
        } else {
            /* Exit app */
            Jimm.getJimm().quit();
        }
    }
    
    /* Command listener */
    public void commandAction(Command c, Displayable d) {
        if (JimmUI.isYesCommand(c)) {
            doExit(true);

        } else if ((null != passwordTextBox) && passwordTextBox.isOkCommand(c)) {
            final Protocol protocol = getCurrentProtocol();
            protocol.setPassword(passwordTextBox.getString());
            passwordTextBox.back();
            if (!StringConvertor.isEmpty(protocol.getPassword())) { 
                protocol.connect();
            }
        }
    }
    private InputTextBox passwordTextBox;
    private void execCommand(int cmd) {
        final Protocol proto = getCurrentProtocol();
        switch (cmd) {
            case MENU_CONNECT:
                if (proto.isEmpty()) {
                    new OptionsForm().showAccountEditor(proto);

                } else if (StringConvertor.isEmpty(proto.getPassword())) {
                    passwordTextBox = new InputTextBox("password", 32, TextField.PASSWORD);
                    passwordTextBox.setCommandListener(this);
                    passwordTextBox.show();

                } else {
                    contactList.restore();
                    proto.connect();
                }
                break;

            case MENU_DISCONNECT:
                /* Disconnect */
                proto.disconnect();
                Thread.yield();
                /* Show the main menu */
                ContactList.activate();
                break;

            case MENU_KEYLOCK:
                /* Enable keylock */
                Jimm.lockJimm();
                break;

            case MENU_STATUS:
                // #sijapp cond.if protocols_MRIM is "true" #
                if (proto instanceof Mrim) {
                    new SomeStatusForm(proto).show();
                }
                // #sijapp cond.end #
                // #sijapp cond.if protocols_ICQ is "true" #
                if (proto instanceof Icq) {
                    new SomeStatusForm(proto).show();
                }
                // #sijapp cond.end #
                // #sijapp cond.if protocols_JABBER is "true" #
                if (proto instanceof Jabber) {
                    new SomeStatusForm(proto).show();
                }
                // #sijapp cond.end #
                break;

                // #sijapp cond.if modules_XSTATUSES is "true" #
            case MENU_XSTATUS:
                // #sijapp cond.if protocols_ICQ is "true" #
                if (proto instanceof Icq) {
                    new IcqXStatusForm((Icq)proto).show();
                }
                // #sijapp cond.end #
                // #sijapp cond.if protocols_MRIM is "true" #
                if (proto instanceof Mrim) {
                    new MrimXStatusForm((Mrim)proto).show();
                }
                // #sijapp cond.end #
                // #sijapp cond.if protocols_JABBER is "true" #
                if (proto instanceof Jabber) {
                    new JabberXStatusForm((Jabber)proto).show();
                }
                // #sijapp cond.end #
                break;
                // #sijapp cond.end #

                // #sijapp cond.if protocols_ICQ is "true" #
                // #sijapp cond.if modules_CLIENTS is "true" #
            case MENU_CLIENT:
                new IcqClientForm((Icq)proto).show();
                break;
                // #sijapp cond.end #

                // #sijapp cond.if modules_SERVERLISTS is "true" #
            case MENU_PRIVATE_STATUS:
                new PrivateStatusForm(proto).show();
                break;
                // #sijapp cond.end #
                // #sijapp cond.end #

                // #sijapp cond.if protocols_JABBER is "true" #
            case MENU_DISCO:
                ((Jabber)proto).getServiceDiscovery().showIt();
                break;
                // #sijapp cond.end #
                
                // #sijapp cond.if protocols_MRIM is "true" #
            case MENU_MICROBLOG:
                ((Mrim)proto).getMicroBlog().show();
                break;
                // #sijapp cond.end #
                
            case MENU_OPTIONS:
                /* Options */
                OptionsForm.activate();
                break;

                // #sijapp cond.if modules_TRAFFIC is "true" #
            case MENU_TRAFFIC:
                /* Traffic */
                new TrafficScreen().show();
                break;
                // #sijapp cond.end #

            case MENU_ABOUT:
                updateAbout();
                aboutTextList.show();
                break;

            case MENU_GROUPS:
                new ManageContactListForm(proto).show();
                break;

                // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
            case MENU_MINIMIZE:
                /* Minimize Jimm (if supported) */
                // #sijapp cond.if target="SIEMENS2"#
                MenuModel sieMenu = getSiemensMenu();
                if (null != sieMenu) {
                    new Select(sieMenu).show();
                    break;
                }
                // #sijapp cond.end#
                contactList.restore();
                Jimm.setMinimized(true);
                break;
                // #sijapp cond.end#

            // #sijapp cond.if modules_DEBUGLOG is "true" #
            case MENU_DEBUGLOG:
                DebugLog.activate();
                break;
            // #sijapp cond.end#

            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            case MENU_MAGIC_EYE:
                MagicEye.activate();
                break;
            // #sijapp cond.end#
                
            // #sijapp cond.if modules_SOUND is "true" #
            case MENU_SOUND:
                Notify.changeSoundMode(false);
                updateMenu();
                break;
            // #sijapp cond.end#
                
            case MENU_MYSELF:
                proto.createTempContact(proto.getUserId(), proto.getNick()).showUserInfo();
                break;
             
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            case MENU_TEST:
                for (int i = 0; i < 10; ++i) {
                    long m = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024);
                    PopupWindow.showShadowPopup("memory "+ m + "kb ",
                            "text" + i + "\nhj:)khgkgfnh\n\n\ngtfn\ngtfn\ngtfn\ngter44fn\n"
                            + "tssfn\ngttwfn\ngtftrn\ngtfn\ngtf54354n\ngt435fn\ng43554tfn\nmenjkhk\n"
                            + ":):D:( :) :) q*WRITE*");
                }
                break;
            // #sijapp cond.end#

            case MENU_EXIT:
                doExit(false);
                break;
        }
    }
    public void select(Select select, MenuModel model, int cmd) {
        // #sijapp cond.if target="SIEMENS2"#
        if (null == select) {
        } else if (siemensCommandSelect == model) {
            execSiemensCommand(cmd);
            return;
        }
        // #sijapp cond.end#
        execCommand(cmd);
    }
}
