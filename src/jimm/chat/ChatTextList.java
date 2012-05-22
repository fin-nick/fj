/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/jimm/ChatHistory.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Andreas Rossbacher, Artyomov Denis, Dmitry Tunin, Vladimir Kryukov
 *******************************************************************************/

/*
 * ChatTextList.java
 *
 * Created on 19 Апрель 2007 г., 15:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.chat;

import DrawControls.icons.Icon;
import DrawControls.text.*;
import java.util.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.chat.message.*;
import jimm.cl.*;
import jimm.comm.*;
import jimm.history.*;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import protocol.icq.*;
import jimm.modules.*;
import jimm.util.ResourceBundle;
import DrawControls.*;
import protocol.Contact;
import protocol.jabber.*;

public final class ChatTextList extends TextList implements SelectListener {
    
    private Contact contact;
    private boolean writable = true;
    private Vector messData = new Vector();
    private int messTotalCounter = 0;
    // #sijapp cond.if modules_HISTORY is "true" #
    private HistoryStorage history;
    // #sijapp cond.end#
    private static InputTextLine line = new InputTextLine();
    private boolean classic = false;
    private static Icon[] statusIcons = new Icon[7];
    
    public final void setWritable(boolean wr) {
        writable = wr;
    }
    
    public ChatTextList(Contact item) {
        super(null);
        contact = item;
        
        setFontSet(GraphicsEx.chatFontSet);
        // #sijapp cond.if modules_HISTORY is "true" #
        fillFormHistory();
        // #sijapp cond.end #
        setMenuCodes(USER_MENU_SHOW_CL, -1);
    }
    
    public void updateStatusIcons() {
        for (int i = 0; i < statusIcons.length; ++i) {
            statusIcons[i] = null;
        }
        contact.getLeftIcons(statusIcons);
        setCapImages(statusIcons);
    }    

    // #sijapp cond.if modules_STYLUS is "true"#    
    public void captionTapped() {
        ChatHistory.instance.showChatList();
    }
    // #sijapp cond.end#
    
    private byte getInOutColor(boolean incoming) {
        return incoming ? THEME_CHAT_INMSG : THEME_CHAT_OUTMSG;
    }

    public static final String ADDRESS = ", ";
    private final void writeMessage(String initText) {
        if (writable) {
            if (classic) {
                line.setString(initText);
                line.setVisible(true);
                invalidate();
                restore();
                return;
            }
            contact.writeMessage(initText);
        }
    }
    public final void writeMessageTo(String nick) {
        if (null != nick) {
            if ('/' == nick.charAt(0)) {
                nick = ' ' + nick;
            }
            nick += ADDRESS;

        } else {
            nick = "";
        }
        writeMessage(nick);
    }
    // #sijapp cond.if protocols_JABBER is "true" #
    private String getJuickNum(String text) {
        if (StringConvertor.isEmpty(text)) {
            return null;
        }
        String lastLine = text.substring(text.lastIndexOf('\n') + 1);
        if ('#' != lastLine.charAt(0)) {
            return null;
        }
        int numEnd = lastLine.indexOf(' ');
        if (-1 == numEnd) {
            numEnd = lastLine.indexOf('/');
        }
        if (-1 == numEnd) {
            return null;
        }
        return lastLine.substring(0, numEnd) + " ";
    }
    // #sijapp cond.end #
    protected final void itemSelected() {
        if (contact.isSingleUserContact()) {
            // #sijapp cond.if protocols_JABBER is "true" #
            if (contact instanceof JabberContact) {
                if (contact.getUin().equals("juick@juick.com")) {
                    writeMessage(getJuickNum(getCurrentMessage()));
                    return;
                }
            }
            // #sijapp cond.end #
            writeMessage(null);
            return;
        }
        MessData md = getCurrentMsgData();
        String nick = (null == md) ? null : md.getNick();
        writeMessageTo(contact.getMyName().equals(nick) ? null : nick);
    }
    public final void deleteChats() {
        MenuModel select = new MenuModel();
        select.addItem("currect_contact",         MENU_DEL_CURRENT_CHAT);
        select.addItem("all_contact_except_this", MENU_DEL_ALL_CHATS_EXCEPT_CUR);
        select.addItem("all_contacts",            MENU_DEL_ALL_CHATS);
        select.setActionListener(this);
        new Select(select).show();
    }
    protected final void doKeyReaction(int keyCode, int actionCode, int type) {
        if (classic && line.doKeyReaction(this, keyCode, actionCode, type)) {
            return;
        }
        if (CanvasEx.KEY_PRESSED == type) {
            resetUnreadMessages();
            switch(keyCode) {
                case NativeCanvas.CALL_KEY:
                    actionCode = 0;
                    break;
                    
                case NativeCanvas.CLEAR_KEY:
                    deleteChats();
                    return;
            }
            switch (actionCode) {
                case NativeCanvas.NAVIKEY_FIRE:
                    if ('5' == keyCode) {
                        itemSelected();
                        return;
                    }
                    writeMessage(null);
                    return;
                    
                case NativeCanvas.NAVIKEY_LEFT:
                case NativeCanvas.NAVIKEY_RIGHT:
                    ChatHistory.instance.showNextPrevChat(contact, NativeCanvas.NAVIKEY_RIGHT == actionCode);
                    return;
            }
        }
        if (!JimmUI.execHotKey(contact, keyCode, type)) {
            super.doKeyReaction(keyCode, actionCode, type);
        }
    }
    
    
    
    private static final int MENU_REPLY = 999;

    private static final int MENU_ADD_TO_HISTORY = 998;
    private static final int MENU_COPY_TEXT = 1024;
    private static final int MENU_GOTO_URL = 1033;
    private static final int MENU_DEL_CHAT = 1027;
    private static final int MENU_DEL_CURRENT_CHAT = 1028;
    private static final int MENU_DEL_ALL_CHATS_EXCEPT_CUR = 1029;
    private static final int MENU_DEL_ALL_CHATS    = 1030;
    static final int USER_MENU_SHOW_CL      = 1031;
    public MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        if (isMessageAvailable(MESSAGE_AUTH_REQUEST)) {
            menu.addItem("grant", Contact.USER_MENU_GRANT_AUTH);
            menu.addItem("deny",  Contact.USER_MENU_DENY_AUTH);
        }
        
        if (contact.isSingleUserContact()) {
            menu.addItem("reply",     Contact.USER_MENU_MESSAGE);
        } else {
            if (writable) {
                menu.addItem("message",   Contact.USER_MENU_MESSAGE);
                menu.addItem("reply",     MENU_REPLY);
            }
            menu.addItem("list_of_users", Contact.USER_MENU_USERS_LIST);
        }

        menu.addItem("copy_text", MENU_COPY_TEXT);
        if (!JimmUI.clipBoardIsEmpty() && writable) {
            menu.addItem("paste", Contact.USER_MENU_PASTE);
            menu.addItem("quote", Contact.USER_MENU_QUOTE);
        }

        // #sijapp cond.if modules_HISTORY is "true" #
        if (!Options.getBoolean(Options.OPTION_HISTORY) && contact.hasHistory()) {
            menu.addItem("add_to_history", MENU_ADD_TO_HISTORY);
        }
        // #sijapp cond.end#
        // #sijapp cond.if protocols_ICQ is "true" #
        if (!contact.isAuth()) {
            menu.addEllipsisItem("requauth", null, IcqContact.USER_MENU_REQU_AUTH);
        }
        // #sijapp cond.end #
        // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
        MessData md = getCurrentMsgData();
        if ((null != md) && md.isURL()) {
            menu.addItem("goto_url", MENU_GOTO_URL);
        }
        // #sijapp cond.end#
        
        menu.addItem("user_menu",   Contact.USER_MENU_SHOW);
        menu.addItem("delete_chat", MENU_DEL_CHAT);
        menu.addItem("close",       USER_MENU_SHOW_CL);
        menu.setActionListener(this);
        return menu;
    }
    
    public void select(Select select, MenuModel model, int cmd) {
        if (!writable && ((MENU_REPLY == cmd) || (Contact.USER_MENU_MESSAGE == cmd))) {
            return;
        }
        switch (cmd) {
            case MENU_REPLY:
                itemSelected();
                break;

            case MENU_COPY_TEXT:
                copyText();
                select.back();
                break;

            // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
            case MENU_GOTO_URL:
                TextListEx urls = new TextListEx(null).gotoURL(getCurrentMessage());
                if (null != urls) {
                    urls.show(this);
                }
                break;
            // #sijapp cond.end#

            // #sijapp cond.if modules_HISTORY is "true" #
            case MENU_ADD_TO_HISTORY:
                addTextToHistory();
                select.back();
                break;
            // #sijapp cond.end#

            case MENU_DEL_CHAT:
                deleteChats();
                break;
            
            case MENU_DEL_CURRENT_CHAT:
                contact.deleteChat();
                ContactList.activate();
                break;

            case MENU_DEL_ALL_CHATS_EXCEPT_CUR:
                ChatHistory.instance.chatHistoryDelete(contact);
                ContactList.activate();
                break;
                
            case MENU_DEL_ALL_CHATS:
                ChatHistory.instance.chatHistoryDelete(null);
                ContactList.activate();
                break;
            
            case USER_MENU_SHOW_CL:
                contact.getProtocol().ui_setActiveContact(contact);
                ContactList.activate();
                break;

            default:
                contact.select(select, null, cmd);
        }
    }
    
    
    public void beginTyping(boolean type) {
        invalidate();
    }

    // #sijapp cond.if protocols_JABBER is "true" #
    public static boolean isHighlight(String text, String nick) {
        for (int index = text.indexOf(nick); -1 != index; index = text.indexOf(nick, index + 1)) {
            if (0 < index) {
                char before = text.charAt(index - 1);
                if ((' ' != before) && ('\n' != before) && ('\t' != before)) {
                    continue;
                }
            }
            if (index + nick.length() + 2 < text.length()) {
                // Calculate space char...
                // ' a': min(' ', 'a') is ' '
                // 'a ': min('a', ' ') is ' '
                char after = (char)Math.min(text.charAt(index + nick.length()),
                        text.charAt(index + nick.length() + 1));
                if ((' ' != after) && ('\n' != after) && ('\t' != after)) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }
    // #sijapp cond.end#

    private synchronized void addTextToForm(Message message) {
        String from = message.getName();
        long time = message.getNewDate();
        boolean incoming = message.isIncoming();
        boolean offline = message.isOffline();
        
        String messageText = message.getProcessedText();
        if (StringConvertor.isEmpty(messageText)) {
            return;
        }
        boolean isMe = messageText.startsWith(PlainMessage.CMD_ME);
        if (isMe) {
            messageText = messageText.substring(4);
            if (0 == messageText.length()) {
                return;
            }
        }

        lock();
        int lastSize = getSize();
        int texOffset = 0;
        
        
        int messageId = messTotalCounter++;
        
        Icon icon = message.getIcon();
        if (null != icon) {
            ListItem img = formatedText.addImage(icon, messageId);
            message.setVisibleIcon(img);
        }
        if (isMe) {
            byte style = getInOutColor(incoming);
            addBigText("*", style, Font.STYLE_PLAIN, messageId);
            addBigText(from, style, Font.STYLE_PLAIN, messageId);
            addBigText(" ", style, Font.STYLE_PLAIN, messageId);
            addTextWithEmotions(messageText, style, Font.STYLE_PLAIN, messageId);
            doCRLF(messageId);
            
        } else {
            addBigText(from + " (" + Util.getDateString(time, !offline) + "): ",
                    getInOutColor(incoming), Font.STYLE_BOLD, messageId);
            doCRLF(messageId);
//          addBigText("[" + Util.getDateString(time, !offline) + "] " + from + "> ", //alternative kind chat
//                  getInOutColor(incoming), Font.STYLE_BOLD, messageId);             //alternative kind chat
//          doCRLF(messageId);                                                        //alternative kind chat
            texOffset = getSize() - lastSize;
            
            // addEllipsisItem message
            byte color = THEME_TEXT;
            byte fontStyle = Font.STYLE_PLAIN;                             //font highlight
            // #sijapp cond.if protocols_JABBER is "true" #
            if (incoming && !contact.isSingleUserContact()
                    && isHighlight(messageText, contact.getMyName())) {
                color = CanvasEx.THEME_CHAT_HIGHLIGHT_MSG;
                fontStyle = Font.STYLE_BOLD;                               //font highlight
            }
            // #sijapp cond.end#
            addTextWithEmotions(messageText, color, fontStyle, messageId); //font highlight
            doCRLF(messageId);
            
        }
        
        boolean contains_url = false;
        // #sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
        contains_url = Util.hasURL(messageText);
        // #sijapp cond.end#
        messData.addElement(new MessData(incoming, time, texOffset, from, contains_url));
        
        int selectedMessageId = getCurrTextIndex();
        if (!incoming) {
            setCurrentItem(getSize() - 1);

        } else {
            int unread = getUnreadMessageCount();
            if (messTotalCounter - unread - 2 <= selectedMessageId) {
                setCurrentItem(getSize() - 1);
                if (isVisibleChat()) {
                    setCurrentItem(lastSize);
                } else {
                    setCurrTextIndex(messTotalCounter - unread - 1);
                }
            }
        }
        removeOldMessages();
        unlock();
    }
    
    public void activate() {
        // #sijapp cond.if (target="MIDP2"|target="SIEMENS2")#
        classic = Options.getBoolean(Options.OPTION_CLASSIC_CHAT);
        // #sijapp cond.end#
        line.setString("");
        int h = line.getRealHeight();
        line.setSize(NativeCanvas.getScreenHeight() - h, getWidth(), h);
        show();
    }
    // #sijapp cond.if (target="MIDP2"|target="SIEMENS2")#
    protected int getHeight() {
        return classic ? NativeCanvas.getScreenHeight() - line.getHeight() : NativeCanvas.getScreenHeight();
    }

    protected void paint(GraphicsEx g) {
        super.paint(g);
        if (classic) {
            if (line.isVisible()) {
                line.paint(g.getGraphics());
                NativeCanvas.setCommands("menu", "backspace");
            } else {
                NativeCanvas.setCommands("menu", "close");
            }
        }
    }
    // #sijapp cond.end#
    
    protected void restoring() {
        NativeCanvas.setCommands("menu", "close");
        resetUnreadMessages();
        updateStatusIcons();
        updateCaption();
        if (!classic) {
            line.setVisible(false);
        }
    }
    
    // #sijapp cond.if modules_HISTORY is "true" #
    final static private int MAX_HIST_LAST_MESS = 5;
    private final void fillFormHistory() {
        if (!contact.hasHistory()) return;
        if (Options.getBoolean(Options.OPTION_SHOW_LAST_MESS)) {
            if (0 != getSize()) return;
            HistoryStorage history = getHistory();
            history.openHistory();
            int recCount = history.getHistorySize();
            if (0 == recCount) return;
            
            int loadOffset = Math.max(recCount - MAX_HIST_LAST_MESS, 0);
            for (int i = loadOffset; i < recCount; ++i) {
                CachedRecord rec = history.getRecord(i);
                if (null == rec) continue;
                addBigText("[" + rec.from + " " + rec.date + "]",
                        getInOutColor(rec.type == 0),
                        Font.STYLE_PLAIN, -1);
                doCRLF(-1);
                
                addTextWithEmotions(rec.text, CanvasEx.THEME_CHAT_FROM_HISTORY, Font.STYLE_PLAIN, -1);
                doCRLF(-1);
            }
            history.closeHistory();
        }
    }
    public HistoryStorage getHistory() {
        if ((null == history) && contact.hasHistory()) {
            history = HistoryStorage.getHistory(contact);
        }
        return history;
    }
    private void addToHistory(String msg, boolean incoming, String nick, long time) {
        if (!contact.hasHistory()) return;
        getHistory().addText(msg, (byte)(incoming ? 0 : 1), nick, time);
    }

    private void addTextToHistory() {
        if (!contact.hasHistory()) return;
        MessData md = getCurrentMsgData();
        if (null == md) return;
        String text = getCurrText(md.getOffset(), false);
        if (null == text) return;
        addToHistory(text, md.isIncoming(), md.getNick(), md.getTime());
    }
    // #sijapp cond.end#
    
    private void updateCaption() {
        int counter = ChatHistory.instance.calcCounter(contact);
        int total   = ChatHistory.instance.getTotal();
        // Calculate the title for the chatdisplay.
        String title = "[" + counter + "/" + total + "] " + contact.getName();
        setCaption(title);
    }
    private int getStartTextIndex() {
        int startIndex = 0;
        for (int i = 0; i < formatedText.getSize(); ++i) {
            startIndex = formatedText.getTextIndex(i);
            if (-1 < startIndex) return startIndex;
        }
        return 0;
    }
    public MessData getCurrentMsgData() {
        try {
            int messageId = getCurrTextIndex();
            if (messageId < 0) return null;
            int messIndex = messageId - getStartTextIndex();
            return (MessData)messData.elementAt(messIndex);
        } catch (Exception e) {
            return null;
        }
    }
    private String getCurrentMessage() {
        MessData md = getCurrentMsgData();
        return (null == md) ? "" : getCurrText(md.getOffset(), false);
    }
    public void clear() {
        super.clear();
        messData.removeAllElements();
        messTotalCounter = 0;
    }
    private void removeOldMessages() {
        final int maxHistorySize = (Util.strToIntDef(Options.getString(Options.OPTION_MAX_HISTORY_SIZE), 100)); //quantity of messages in a chat
        if (maxHistorySize < messData.size()) {
            if (-1 == formatedText.getTextIndex(0)) {
                // remove history
                removeFirstText();
            }
            while (maxHistorySize < messData.size()) {
                messData.removeElementAt(0);
                removeFirstText();
            }
        }
    }
    
    private void copyText() {
        MessData md = getCurrentMsgData();
        if (null == md) return;
        long yesterday = Util.createCurrentDate(false) - 23 * 60 * 60;
        String msg = getCurrText(md.getOffset(), false);
        String time = Util.getDateString(md.getTime(), yesterday < md.getTime());
        JimmUI.setClipBoardText(md.isIncoming(), time, md.getNick(), msg);
    }
    
    public boolean empty() {
        return (0 == messData.size()) && (0 == formatedText.getSize());
    }
    
    public long getLastMessageTime() {
        if (0 == messData.size()) return 0;
        MessData md = (MessData)messData.lastElement();
        return md.getTime();
    }
    
    // Adds a message to the message display
    public void addMessage(Message message) {
        final long time = message.getNewDate();
        int type = -1;
        if (message instanceof PlainMessage) {
            type = MESSAGE_PLAIN;
            
            addTextToForm(message);
            // #sijapp cond.if modules_HISTORY is "true" #
            if (Options.getBoolean(Options.OPTION_HISTORY)) {
                final String nick = message.getName();
                addToHistory(message.getText(), true, nick, time);
            }
            // #sijapp cond.end#
            
        } else if (message instanceof SystemNotice) {
            SystemNotice notice = (SystemNotice) message;
            type = MESSAGE_SYS_NOTICE;
            if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
                type = MESSAGE_AUTH_REQUEST;
            }

            // #sijapp cond.if modules_PRESENCE is "true" #                     //presence
            if (SystemNotice.SYS_NOTICE_PRESENCE == notice.getSysnoteType()) {  //presence
                type = MESSAGE_SYS_PRESENCE;                                    //presence
            }                                                                   //presence
            // #sijapp cond.end#                                                //presence
            
            // #sijapp cond.if modules_MAGIC_EYE is "true" #
            if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {   //add
            MagicEye.addAction(contact.getProtocol(), contact.getUin(), message.getText());
            }                                                                   //add
            // #sijapp cond.end #
            addTextToForm(message);
        }
        if (!isVisibleChat()) {
            increaseMessageCount(type);
        }
    }
    private boolean isVisibleChat() {
        return (this == Jimm.getCurrentDisplay()) && !Jimm.isPaused();
    }
    
    /* Increases the mesage count */
    private void increaseMessageCount(int type) {
        switch (type) {
            case MESSAGE_PLAIN:
                if (plainMessageCounter < Short.MAX_VALUE) {
                    plainMessageCounter++;
                }
                return;
            case MESSAGE_SYS_NOTICE:
                if (sysNoticeCounter < Byte.MAX_VALUE) {
                    sysNoticeCounter++;
                }
                return;
            case MESSAGE_AUTH_REQUEST:
                if (authRequestCounter < Byte.MAX_VALUE) {
                    authRequestCounter++;
                }
                return;
        }
    }
    /* Message types */
    public static final int MESSAGE_PLAIN        = 1;
    public static final int MESSAGE_SYS_NOTICE   = 2;
    public static final int MESSAGE_AUTH_REQUEST = 3;
    // #sijapp cond.if modules_PRESENCE is "true" #   //presence
    public static final int MESSAGE_SYS_PRESENCE = 4; //presence
    // #sijapp cond.end#                              //presence
    private short plainMessageCounter = 0;
    private byte sysNoticeCounter = 0;
    private byte authRequestCounter = 0;
    
    public void resetAuthRequests() {
        boolean notEmpty = (0 < authRequestCounter);
        authRequestCounter = 0;
        if (notEmpty) {
            contact.getProtocol().getContactList().markMessages(contact);
        }
    }
    private void resetUnreadMessages() {
        boolean notEmpty = (0 < plainMessageCounter) || (0 < sysNoticeCounter);
        plainMessageCounter = 0;
        sysNoticeCounter = 0;
        if (notEmpty) {
            contact.getProtocol().getContactList().markMessages(contact);
        }
    }
    public int getUnreadMessageCount() {
        return plainMessageCounter + sysNoticeCounter + authRequestCounter;
    }
    /* Returns true if the next available message is a message of given type
       Returns false if no message at all is available, or if the next available
       message is of another type */
    public boolean isMessageAvailable(int type) {
        switch (type) {
            case MESSAGE_PLAIN:        return plainMessageCounter > 0;
            case MESSAGE_SYS_NOTICE:   return sysNoticeCounter > 0;
            case MESSAGE_AUTH_REQUEST: return authRequestCounter > 0;
        }
        return false;
    }
    
    
    public void addMyMessage(PlainMessage message) {
        resetUnreadMessages();
        addTextToForm(message);
        // #sijapp cond.if modules_HISTORY is "true" #
        if (Options.getBoolean(Options.OPTION_HISTORY)) {
            addToHistory(message.getText(), false, message.getName(), message.getNewDate());
        }
        // #sijapp cond.end#
    }
    
    public Contact getContact() {
        return contact;
    }
}
