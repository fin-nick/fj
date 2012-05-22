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

package jimm.chat;

import DrawControls.icons.Icon;
import java.util.*;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import jimm.history.*;
import protocol.Protocol;
import jimm.modules.*;
import jimm.ui.menu.*;
import jimm.util.ResourceBundle;
import DrawControls.*;
import protocol.Contact;

public final class ChatHistory implements SelectListener {
    protected final Vector historyTable = new Vector();
    private MenuModel listOfChats = null;
    private Select listOfChatsView = null;
    public static final ChatHistory instance = new ChatHistory();
    
    final public static int DEL_TYPE_ALL_EXCEPT_CUR = 1;
    final public static int DEL_TYPE_ALL            = 2;
    
    // Sets the counter for the ChatHistory
    public int calcCounter(Contact contact) {
        return (contact == null) ? 0 : (historyTable.indexOf(contact) + 1);
    }
    public int getTotal() {
        return historyTable.size();
    }
    private Contact contactAt(int index) {
        return (Contact)historyTable.elementAt(index);
    }
    
    // Creates a new chat form
    public void registerChat(Contact item) {
        if (!historyTable.contains(item)) {
            historyTable.addElement(item);
        }
    }
    
    public void unregisterChat(Contact item) {
        if (null == item) return;
        historyTable.removeElement(item);
    }
    
    // Delete the chat history for uin
    public void chatHistoryDelete(Contact except) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            Contact key = contactAt(i);
            if (key == except) continue;
            key.deleteChat();
        }
    }
    
    
    public void restoreContactsWithChat(Protocol p) {
        int total = getTotal();
        for (int i = 0; i < total; ++i) {
            Contact contact = contactAt(i);
            if (p != contact.getProtocol()) {
                continue;
            }
            if (!p.inContactList(contact)) {
                if (contact.isSingleUserContact()) {
                    contact.setTempFlag(true);
                    contact.setGroup(null);
                } else {
                    if (null == contact.getGroup())  {
                        contact.setGroup(null);
                    }
                }
                p.addTempContact(contact);
            }
        }
    }
    
    public void updateChatList() {
        MenuModel chats = listOfChats;
        if (null == chats) {
            return;
        }
        updateChatList(chats);
        listOfChatsView.update();
    }
    private synchronized int updateChatList(MenuModel chats) {
        int chatFrom = chats.count();
        int chatCount = getTotal();
        for (int i = chatFrom; i < chatCount; ++i) {
            Contact contact = contactAt(i);
            chats.addRawItem(contact.getName(), null, i);
        }

        Icon[] icons = new Icon[7];
        int withChat = -1;
        int current  = 0;
        Contact currentContact = ContactList.getInstance().getCurrentContact();
        for (int i = 0; i < chatCount; ++i) {
            Contact contact = contactAt(i);
            contact.getLeftIcons(icons);
            chats.setRawItem(i, contact.getName(), icons[0]);
            if ((-1 == withChat) && (0 < contact.getUnreadMessageCount())) {
                withChat = i;
            }
            if (currentContact == contact) {
                current = i;
            }
        }
        return (0 <= withChat) ? withChat : current;
    }
    public void showChatList() {
        if (0 == getTotal()) {
            return;
        }
        MenuModel chats = new MenuModel();
        int current = updateChatList(chats);
        chats.setDefaultItemCode(current);
        chats.setActionListener(this);
        if (null == listOfChatsView) {
            listOfChatsView = new Select(chats);
        }
        listOfChatsView.setModel(chats);
        listOfChats = chats;
        listOfChatsView.show();
    }
    
    // shows next or previos chat
    public void showNextPrevChat(Contact item, boolean next) {
        int chatNum = historyTable.indexOf(item);
        if (-1 == chatNum) {
            return;
        }
        int nextChatNum = (chatNum + (next ? 1 : -1) + getTotal()) % getTotal();
        Contact nextContact = contactAt(nextChatNum);
        if (null != nextContact) {
            nextContact.activate();
        }
    }
    
    public void select(Select select, MenuModel model, int cmd) {
        listOfChats = null;
        if ((0 <= cmd) && (cmd < getTotal())) {
            Contact contact = contactAt(cmd);
            ContactList.activate();
            contact.activate();
        }
    }
}