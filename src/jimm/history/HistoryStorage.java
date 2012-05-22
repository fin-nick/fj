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
 * File: src/jimm/HistoryStorage.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Artyomov Denis, Igor Palkin
 *******************************************************************************/

// #sijapp cond.if modules_HISTORY is "true" #

package jimm.history;

import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import jimm.*;
import jimm.io.Storage;
import jimm.modules.*;
import jimm.ui.PopupWindow;
import jimm.util.ResourceBundle;
import java.io.*;
import javax.microedition.midlet.*;

import DrawControls.*;
import jimm.comm.*;
import protocol.Contact;
// #sijapp cond.if target="SIEMENS2" | target="MOTOROLA" | target="MIDP2"#
import javax.microedition.io.*;
// #sijapp cond.end#



// History storage implementation
public class HistoryStorage {
    //===================================//
    //                                   //
    //    Data storage implementation    //
    //                                   //
    //===================================//
    
    public static final int CLEAR_EACH_DAY   = 0;
    public static final int CLEAR_EACH_WEEK  = 1;
    public static final int CLEAR_EACH_MONTH = 2;
    
    private static final String PREFIX = "hist";
    
    private String storageName;
    private Storage historyStore;
    private int currRecordCount = -1;

    public HistoryStorage(Contact contact) {
        storageName = getRSName(contact);
    }
    
    public static HistoryStorage getHistory(Contact contact) {
        return new HistoryStorage(contact);
    }
    
    private boolean openHistory(boolean create) {
        if (null == historyStore) {
            try {
                historyStore = new Storage(storageName);
                historyStore.open(create);
            } catch (Exception e) {
                historyStore = null;
                return false;
            }
        }
        return true;
    }
    public void openHistory() {
        openHistory(false);
    }
    public void closeHistory() {
        if (null != historyStore) {
            historyStore.close();
        }
        historyStore = null;
        currRecordCount = -1;
    }
    
    synchronized void closeHistoryView() {
        closeHistory();
    }
    // Add message text to contact history
    public synchronized void addText(
            String text, // text to save
            byte type,   // type of message 0 - incoming, 1 - outgouing
            String from, // text sender
            long time    // date of message
            ) {

        boolean isOpened = openHistory(true);
        if (!isOpened) {
            return;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream das = new DataOutputStream(baos);
            das.writeByte(type);
            das.writeUTF(from);
            das.writeUTF(text);
            das.writeUTF(Util.getDateString(time, false));
            byte[] buffer = baos.toByteArray();            
            historyStore.addRecord(buffer);
        } catch (Exception e) {
        }
        closeHistory();
        currRecordCount = -1;
    }
    
    // Returns reference for record store
    RecordStore getRS() {
        return historyStore.getRS();
    }
    
    // Returns record store name for Contact
    private String getRSName(Contact contact) {
        return Storage.getStorageName(PREFIX + contact.getUniqueUin());
    }
    
    // Returns record count for Contact
    public int getHistorySize() {
        if (currRecordCount < 0) {
            openHistory(false);
            currRecordCount = 0;
            try {
                if (null != historyStore) {
                    currRecordCount = historyStore.getNumRecords();
                }
            } catch (Exception e) {
            }
        }
        return currRecordCount;
    }
    
    // Returns full data of stored message
    public CachedRecord getRecord(int recNo) {
        if (null == historyStore) {
            openHistory(false);
        }
        CachedRecord result = new CachedRecord();
        try {
            byte[] data = historyStore.getRecord(recNo + 1);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            result.type = dis.readByte();
            result.from = dis.readUTF();
            result.text = dis.readUTF();
            result.date = dis.readUTF();

        } catch (Exception e) {
            result.type = 0;
            result.from = "";
            result.text = "";
            result.date = "";
        }
        return result;
    }
    
    // Clears messages history for Contact
    public void removeHistory() {
        closeHistory();
        removeRMS(storageName);
    }

    private void removeRMS(String rms) {
        try {
            RecordStore.deleteRecordStore(rms);
        } catch (Exception e) {
        }
    }
    // Clears all records for all uins
    public void clearAll(Contact except) {
        closeHistory();
        String exceptRMS = (null == except) ? null : getRSName(except);
        try {
            String[] stores = RecordStore.listRecordStores();
            
            for (int i = 0; i < stores.length; i++) {
                String store = stores[i];
                if (!store.startsWith(PREFIX)) continue;
                if (store.equals(exceptRMS)) continue;
                removeRMS(store);
            }
        } catch (Exception e) {
        }
    }
}
// #sijapp cond.end#