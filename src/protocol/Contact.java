/*
 * Contact.java
 *
 * Created on 13 Май 2008 г., 15:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import DrawControls.*;
import DrawControls.tree.*;
import DrawControls.icons.*;
import java.io.InputStream;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.chat.*;
import jimm.chat.message.*;
import jimm.cl.ContactList;
import jimm.forms.ManageContactListForm;
import jimm.history.*;
import jimm.modules.*;
import jimm.search.*;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.comm.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author vladimir
 */
abstract public class Contact extends TreeNode implements SelectListener {
    protected static final ImageList authIcon = ImageList.createImageList("/auth.png");
    protected static final MenuModel contactMenu = new MenuModel();
    
    abstract public void setOfflineStatus();
    abstract public boolean isMeVisible();
    abstract public void showUserInfo();
    abstract public void showStatus();
    public final boolean isOnline() {
        return Status.offlineStatus != status;
    }


    protected String uin;
    private String name;
    private int groupId = Group.NOT_IN_GROUP;
    public final String getUin() {
        return uin;
    }
    public String getUniqueUin() {
        return uin;
    }
    
    public final String getName() {
        return name;
    }
    public void setName(String newName) {
        if (!StringConvertor.isEmpty(newName)) {
    	    name = newName;
        }
    }
    public final void setGroupId(int id) {
        groupId = id;
    }
    public final int getGroupId() {
        return groupId;
    }
    public final Group getGroup() {
        return getProtocol().getGroupById(groupId);
    }
    public final void setGroup(Group group) {
        setGroupId((null == group) ? Group.NOT_IN_GROUP : group.getId());
    }
    public String getDefaultGroupName() {
        return null;
    }
    
    protected abstract boolean sendSomeMessage(PlainMessage msg);
    public final void sendMessage(String msg, boolean addToChat) {
        msg = StringConvertor.trim(msg);
        if (StringConvertor.isEmpty(msg) || !getProtocol().isConnected()) {
            return;
        }
        
        boolean prevLock = capLocked;
        capLocked = true;
        PlainMessage plainMsg = new PlainMessage(getProtocol(), this,
                Util.createCurrentDate(false), msg);
        boolean sended = sendSomeMessage(plainMsg);
        if (addToChat && sended) {
            getChat().addMyMessage(plainMsg);
        }
        capLocked = prevLock;
    }

    public boolean isVisibleInContactList() {
        if (!Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE)) return true;
        return isOnline() || hasChat() || isTemp();
    }

    // #sijapp cond.if modules_HISTORY is "true" #
    public void showHistory() {
        HistoryStorage history;
        if (hasChat()) {
            history = getChat().getHistory();
        } else {
            history = HistoryStorage.getHistory(this);
        }
        new HistoryStorageList(history, this).show();
    }
    // #sijapp cond.end#

///////////////////////////////////////////////////////////////////////////
    protected Status status = Status.offlineStatus;
    public Status getStatus() {
        return status;
    }
    protected final void setStatus___(byte prevStatus, Status status) {
        this.status = status;
        byte currStatus = this.status.getStatusIndex();
        statusChainged(prevStatus, currStatus);
    }
    protected final void setStatus___(byte index, String text) {
        byte prevStatus = this.status.getStatusIndex();
        status = createStatus(status, index, text);
        statusChainged(prevStatus, index);
    }
    private void statusChainged(byte prev, byte curr) {
        if (prev == curr) {
            return;
        }
            
        // #sijapp cond.if modules_SOUND is "true" #
        int type = getProtocol().getProfile().protocolType;
        if (10 <= type) type = Profile.PROTOCOL_JABBER;
        boolean prevOffline = getProtocol().getStatusInfo().isOffline(prev, type); //it is changed
        boolean currOffline = getProtocol().getStatusInfo().isOffline(curr, type); //it is changed
        if (!currOffline && prevOffline) {                                         //it is changed
            Notify.playSoundNotification(Notify.NOTIFY_ONLINE);
        }
        if (currOffline && !prevOffline) {                                         //offline sound
            Notify.playSoundNotification(Notify.NOTIFY_OFFLINE);                   //offline sound
        }                                                                          //offline sound
        // #sijapp cond.end #
        if (isCurrent()) {
            showTopLine(getProtocol().getStatusInfo().getName(status));
        }
    }
    public String getStatusMessage() {
        return getProtocol().getStatusInfo().getName(status);
    }
    protected final Status createStatus(Status prev, byte statusIndex, String text) {
        if (Status.I_STATUS_OFFLINE == statusIndex) {
            return Status.offlineStatus;
        }
        Status result = ((null == prev) || (Status.offlineStatus == prev)) ? new Status() : prev;
        result.setStatusIndex(statusIndex);
        result.setText(text);
        return result;
    }

///////////////////////////////////////////////////////////////////////////
    private static boolean capLocked = false;
    protected final void showTopLine(String text) {
        if (capLocked) {
            return;
        }
        Object vis = null;
        InputTextBox editor = ContactList.getInstance().getMessageEditor().getTextBox();
        if (editor.isShown()) {
            vis = editor;
        } else if (hasChat()) {
            vis = getChat();
        }
        UIUpdater.startFlashCaption(vis, text);
    }

    ///////////////////////////////////////////////////////////////////////////
    public void writeMessage(String initText) {
        ContactList.getInstance().getMessageEditor().writeMessage(this, initText);
    }

    /* Activates the contact item menu */
    public void activate() {
        ContactList.getInstance().setCurrentContact(this);
        
        ChatTextList chat = getChat();
        if (hasChat()) {
            chat.activate();
            return;
        }
        deleteChat();
        writeMessage(null);
    }
///////////////////////////////////////////////////////////////////////////
    // #sijapp cond.if modules_SERVERLISTS is "true" #
    private static final ImageList serverListsIcons = ImageList.createImageList("/serverlists.png");
    /**
     * 0 - ignore list
     * 1 - invisible list
     * 2 - visible list
     */
    private final Icon getServerListStatusIcon(int index) {
        return serverListsIcons.iconAt(index);
    }
    protected final Icon getServerListIcon() {
        if (inIgnoreList()) {
            return getServerListStatusIcon(0);
        }
        if (inInvisibleList()) {
            return getServerListStatusIcon(1);
        }
        if (inVisibleList()) {
            return getServerListStatusIcon(2);
        }
        return null;
    }
    // #sijapp cond.end #
///////////////////////////////////////////////////////////////////////////
    protected final boolean isCurrent() {
        return this == getProtocol().getContactList().getCurrentContact();
    }
    /* Shows popup window with text of received message */
    public final void showPopupWindow(String from, String text) {
        if (Jimm.isLocked()) return;
        Object win = Jimm.getCurrentDisplay();
        if (!(win instanceof CanvasEx)
                && !Options.getBoolean(Options.OPTION_POPUP_OVER_SYSTEM)) {
            return;
        }
        boolean haveToShow = false;
        boolean chatVisible = hasChat() && (getChat() == win);
        boolean uinEquals = isCurrent();
        
        switch (Options.getInt(Options.OPTION_POPUP_WIN2)) {
            case 0: return;
            case 1:
                haveToShow = chatVisible ? false : uinEquals;
                break;
            case 2:
                haveToShow = chatVisible ? !uinEquals : true;
                break;
        }
        
        if (!haveToShow) return;
        
        // #sijapp cond.if target is "MIDP2"#
        InputTextBox editor = ContactList.getInstance().getMessageEditor().getTextBox();
        boolean save = Jimm.isPhone(Jimm.PHONE_SE) && editor.isShown();
        if (save) {
            editor.saveCurrentPage();
        }
        // #sijapp cond.end#
        PopupWindow.showShadowPopup(from, text);
        
        // #sijapp cond.if target is "MIDP2"#
        if (save) {
            editor.setCurrentScreen();
        } 
        // #sijapp cond.end#
    }
///////////////////////////////////////////////////////////////////////////
    private byte booleanValues;
    public static final byte CONTACT_NO_AUTH       = 1 << 1; /* Boolean */
    private static final byte CONTACT_IS_TEMP      = 1 << 3; /* Boolean */
    //public static final byte B_AUTOANSWER          = 1 << 2; /* Boolean */
    public static final byte SL_VISIBLE            = 1 << 4; /* Boolean */
    public static final byte SL_INVISIBLE          = 1 << 5; /* Boolean */
    public static final byte SL_IGNORE             = 1 << 6; /* Boolean */
    public final void setBooleanValue(byte key, boolean value) {
        setBooleanValues((byte)((getBooleanValues() & (~key)) | (value ? key : 0x00)));
    }
    public final boolean is(byte key) {
        return (booleanValues & key) != 0;
    }
    public final boolean isTemp() {
        return (booleanValues & CONTACT_IS_TEMP) != 0;
    }
    public final boolean isAuth() {
        return (booleanValues & CONTACT_NO_AUTH) == 0;
    }
    public final void setBooleanValues(byte vals) {
        booleanValues = vals;
    }
    public final byte getBooleanValues() {
        return booleanValues;
    }
    public final void setTempFlag(boolean isTemp) {
        setBooleanValue(Contact.CONTACT_IS_TEMP, isTemp);
    }

    // #sijapp cond.if modules_SERVERLISTS is "true" #
    protected boolean inVisibleList() {
        return (booleanValues & SL_VISIBLE) != 0;
    }
    protected boolean inInvisibleList() {
        return (booleanValues & SL_INVISIBLE) != 0;
    }
    protected boolean inIgnoreList() {
        return (booleanValues & SL_IGNORE) != 0;
    }
    private void showListOperation() {
        MenuModel sl = new MenuModel();
        String visibleList = inVisibleList()
                ? "rem_visible_list" : "add_visible_list";
        String invisibleList = inInvisibleList()
                ? "rem_invisible_list": "add_invisible_list";
        String ignoreList = inIgnoreList()
                ? "rem_ignore_list": "add_ignore_list";
        
        sl.addItem(visibleList,   USER_MENU_PS_VISIBLE);
        sl.addItem(invisibleList, USER_MENU_PS_INVISIBLE);
        sl.addItem(ignoreList,    USER_MENU_PS_IGNORE);
        sl.setActionListener(this);
        new Select(sl).show();
    }
    // #sijapp cond.end #

    public String getMyName() {
        return protocol.getNick();
    }

///////////////////////////////////////////////////////////////////////////

    /* Returns total count of all unread messages (messages, sys notices, urls, auths) */
    public final int getUnreadMessageCount() {
        if (hasChat()) {
            return getChat().getUnreadMessageCount();
        }
        return 0;
    }
    public boolean isSingleUserContact() {
        return true;
    }
    public boolean hasHistory() {
        return !isTemp();
    }

    protected final void resetAuthRequests() {
        getChat().resetAuthRequests();
    }

    /* Returns image index for contact */
    // #sijapp cond.if target isnot "DEFAULT"#
    protected boolean typing = false;
    public final void beginTyping(boolean type) {
        typing = type;
    }
    public final boolean isTyping() {
        return typing;
    }
    // #sijapp cond.end#
    public final void setStatusImage() {
        // #sijapp cond.if modules_LIGHT is "true" #
        if (isSingleUserContact()) {
            CustomLight.setLightMode(CustomLight.ACTION_ONLINE);
        }
        // #sijapp cond.end #
        if (Jimm.isLocked()) {
            if (isSingleUserContact()) {
                SplashCanvas.setNotifyMessage(getProtocol().getStatusInfo().getIcon(status), getName());
            }
            return;
        }
        
        if (hasChat() && (getChat() == Jimm.getCurrentDisplay())) {
            getChat().updateStatusIcons();
        }
    }
    protected final Icon getMessageIcon() {
        int index = -1;
        if (hasChat()) {
            ChatTextList chat = getChat();
            if (chat.isMessageAvailable(ChatTextList.MESSAGE_PLAIN)) {
                index = Message.ICON_MSG_NONE;
            } else if (chat.isMessageAvailable(ChatTextList.MESSAGE_AUTH_REQUEST)) {
                index = Message.ICON_SYSREQ;
            } else if (chat.isMessageAvailable(ChatTextList.MESSAGE_SYS_NOTICE)) {
                index = Message.ICON_SYS_OK;
            // #sijapp cond.if modules_PRESENCE is "true" #                          //presence
            } else if (chat.isMessageAvailable(ChatTextList.MESSAGE_SYS_PRESENCE)) { //presence
            // #sijapp cond.end#                                                     //presence
            }
        }
        // #sijapp cond.if target isnot "DEFAULT"#
        if (typing) {
            index = Message.ICON_TYPE;
        }
        // #sijapp cond.end#

        return Message.msgIcons.iconAt(index);
    }
///////////////////////////////////////////////////////////////////////////
    /* Returns color for contact name */
    public final byte getTextTheme() {
        if (isTemp()) {
            return CanvasEx.THEME_CONTACT_TEMP;
        }
        if (hasChat()) {
            return CanvasEx.THEME_CONTACT_WITH_CHAT;
        }
        if (isOnline()) {
            return CanvasEx.THEME_CONTACT_ONLINE;
        }
        return CanvasEx.THEME_CONTACT_OFFLINE;
    }
    public final String getText() {
        return getName();
    }

    // Node weight declaration.
    // -3       - normal group
    // -2       - non editable group
    // -1       - non removable group
    //  9       - chat group (online)
    // 10       - contact with message
    // 20 - 49  - normal-contact (status)
    // 50       - chat group (offline)
    // 51       - offline-contact
    // 60       - temp-contact
    public final int getNodeWeight() {
        // #sijapp cond.if protocols_JABBER is "true" #
        if (!isSingleUserContact()) {
            return isOnline() ? 9 : 50;
        }
        // #sijapp cond.end #
        if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_CHAT) //contacts to a chat up
                && hasChat()) {                                  //contacts to a chat up
            return 0;                                            //contacts to a chat up
        }                                                        //contacts to a chat up
        if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)
                && getUnreadMessageCount() > 0) {
            return 10;
        }
        int sortType = Options.getInt(Options.OPTION_CL_SORT_BY);
        if (ContactList.SORT_BY_NAME == sortType) {
            return 20;
        }
        if (isOnline()) {
            switch (sortType) {
                case ContactList.SORT_BY_STATUS:
                    // 29 = 49 - 20 last normal status
                    return 20 + getProtocol().getStatusInfo().getWidth(status);
                case ContactList.SORT_BY_ONLINE:
                    return 20;
            }
        }
        
        if (isTemp()) {
            return 60;
        }
        return 51;
    }
///////////////////////////////////////////////////////////////////////////
    private volatile ChatTextList chat = null;
    public final void deleteChat() {
        ChatHistory.instance.unregisterChat(this);
        if (null != chat) {
            chat.clear();
            chat = null;
            getProtocol().ui_updateContact(this);
        }
        chat = null;
    }
    public final ChatTextList createChat() {
        if (null == chat) {
            chat = new ChatTextList(this);
            if (!getProtocol().inContactList(this)) {
                setTempFlag(true);
                getProtocol().addLocalContact(this);
            }
            ChatHistory.instance.registerChat(this);
        }
        return chat;
    }
    public final ChatTextList getChat() {
        if (null == chat) {
            chat = new ChatTextList(this);
            if (!getProtocol().inContactList(this)) {
                setTempFlag(true);
                getProtocol().addLocalContact(this);
            }
        }
        return chat;
    }
    
    public final boolean hasChat() {
        if ((null != chat) && (!chat.empty() || !isSingleUserContact())) {
            ChatHistory.instance.registerChat(this);
            return true;
        }
        return false;
    }
///////////////////////////////////////////////////////////////////////////
    protected Protocol protocol;
    public final Protocol getProtocol() {
        return protocol;
    }
///////////////////////////////////////////////////////////////////////////
    public static final int USER_MENU_MESSAGE          = 1001;
    public static final int USER_MENU_PASTE            = 1002;
    public static final int USER_MENU_QUOTE            = 1003;
    public static final int USER_MENU_REQU_AUTH        = 1004;
    public static final int USER_MENU_FILE_TRANS       = 1005;
    public static final int USER_MENU_CAM_TRANS        = 1006;
    public static final int USER_MENU_USER_REMOVE      = 1007;
    public static final int USER_MENU_RENAME           = 1009;
    //public static final int USER_MENU_LOCAL_INFO       = 1011;
    public static final int USER_MENU_USER_INFO        = 1012;
    public static final int USER_MENU_MOVE             = 1015;
    public static final int USER_MENU_STATUSES         = 1016;
    public static final int USER_MENU_LIST_OPERATION   = 1017;
    public static final int USER_MENU_HISTORY          = 1025;
    public static final int USER_MENU_SHOW             = 1032;
    public static final int USER_MENU_ADD_USER         = 1018;
    public static final int USER_MENU_GRANT_FUTURE_AUTH = 1019;

    public static final int USER_MENU_GRANT_AUTH       = 1021;
    public static final int USER_MENU_DENY_AUTH        = 1022;


    protected static final int USER_MENU_PS_VISIBLE       = 1034;
    protected static final int USER_MENU_PS_INVISIBLE     = 1035;
    protected static final int USER_MENU_PS_IGNORE        = 1036;

    public static final int USER_MENU_USERS_LIST = 1037;
    public static final int USER_MANAGE_CONTACT = 1038;

    public void doAction(int cmd) {
        switch (cmd) {
            case USER_MENU_MESSAGE: /* Send plain message */
                writeMessage(null);
                break;
                
            case USER_MENU_QUOTE: /* Send plain message with quotation */
            case USER_MENU_PASTE: /* Send plain message without quotation */
                writeMessage(JimmUI.getClipBoardText(USER_MENU_QUOTE == cmd));
                break;
                
            case USER_MENU_ADD_USER:
                Search search = new Search(getProtocol());
                search.setSearchParam(Search.UIN, uin);
                search.show(Search.TYPE_NOFORM);
                break;

            case USER_MENU_STATUSES: /* Show user statuses */
                showStatus();
                break;

            case USER_MENU_USER_REMOVE:
                // #sijapp cond.if modules_HISTORY is "true" #
                HistoryStorage.getHistory(this).removeHistory();
                // #sijapp cond.end#
                getProtocol().removeContact(this);
                ContactList.activate();
                break;
                
            case USER_MENU_RENAME:
                /* Rename the contact local and on the server
                   Reset and display textbox for entering name */
                new ManageContactListForm(getProtocol(), this).showContactRename();
                break;
                                
            // #sijapp cond.if modules_HISTORY is "true" #
            case USER_MENU_HISTORY: /* Stored history */
                showHistory();
                break;
            // #sijapp cond.end #
                
            case USER_MENU_SHOW:
                MenuModel menu = getContextMenu();
                if (null != menu) {
                    new Select(menu).show();
                }
                break;

            case USER_MENU_MOVE:
                new ManageContactListForm(getProtocol(), this).showContactMove();
                break;
            
            // #sijapp cond.if modules_FILES is "true"#
            case USER_MENU_FILE_TRANS:
                // Send a filetransfer with a file given by path
                new FileTransfer(this).startFileTransfer();
                break;
                
                // #sijapp cond.if target isnot "MOTOROLA" #
            case USER_MENU_CAM_TRANS:
                // Send a filetransfer with a camera image
                new FileTransfer(this).startPhotoTransfer();
                break;
            // #sijapp cond.end#
            // #sijapp cond.end#

            case USER_MENU_USER_INFO:
                showUserInfo();
                break;

            case USER_MENU_USERS_LIST:
                break;

            case USER_MANAGE_CONTACT:
                MenuModel manageContact = new MenuModel();
                manageContact.clean();
                initManageContactMenu(manageContact);
                manageContact.setActionListener(this);
                if (0 < manageContact.count()) {
                    new Select(manageContact).show();
                }
                break;
            // #sijapp cond.if modules_SERVERLISTS is "true" #
            case USER_MENU_LIST_OPERATION:
                showListOperation();
                break;
            // #sijapp cond.end #
                
        }
    }

    public void select(Select select, MenuModel model, int cmd) {
        doAction(cmd);
    }
    // #sijapp cond.if target isnot "DEFAULT"#
    protected void typing(boolean isTyping) {}
    // #sijapp cond.end#

///////////////////////////////////////////////////////////////////////////
    protected final void addChatItems(MenuModel menu) {
        menu.addEllipsisItem("send_message", null, USER_MENU_MESSAGE);
        if (!isAuth()) {
            menu.addEllipsisItem("requauth", null, USER_MENU_REQU_AUTH);
        }
        if (!JimmUI.clipBoardIsEmpty()) {
            menu.addEllipsisItem("paste", null, USER_MENU_PASTE);
            menu.addEllipsisItem("quote", null, USER_MENU_QUOTE);
        }
    }
    protected final void addFileTransferItems(MenuModel menu) {
        // #sijapp cond.if modules_FILES is "true"#
        if (isOnline()) {
            if (jimm.modules.fs.FileSystem.isSupported()) {
                menu.addEllipsisItem("ft_name", null, USER_MENU_FILE_TRANS);
            }
            // #sijapp cond.if target isnot "MOTOROLA"#
            menu.addEllipsisItem("ft_cam", null, USER_MENU_CAM_TRANS);
            // #sijapp cond.end#
        }
        // #sijapp cond.end#
    }
    protected abstract void initManageContactMenu(MenuModel menu);

///////////////////////////////////////////////////////////////////////////
    
    public final void setOptimalName(UserInfo info) {
        if (getName().equals(getUin())) {
            String nick = info.getOptimalName();
            if (nick.length() != 0) {
                getProtocol().renameContact(this, nick);
            }
        }
    }

///////////////////////////////////////////////////////////////////////////
    protected static final StatusView statusView = new StatusView();
    
///////////////////////////////////////////////////////////////////////////
    public void dismiss() {
        chat = null;
        deleteChat();
        protocol = null;
    }
}
