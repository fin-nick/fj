/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *getName
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/jimm/FileTransfer.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Andreas Rossbacher, Dmitry Tunin
 *******************************************************************************/

// #sijapp cond.if modules_FILES="true"#
package jimm;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.media.*;
// #sijapp cond.if target isnot "MOTOROLA"#
import javax.microedition.media.control.VideoControl;
// #sijapp cond.end#

import jimm.comm.*;
import jimm.modules.*;
import jimm.cl.*;
import jimm.modules.fs.*;
import jimm.modules.photo.*;
import jimm.modules.traffic.*;
import jimm.ui.*;
import jimm.util.ResourceBundle;
import protocol.Contact;

public final class FileTransfer implements CommandListener, FileBrowserListener,
        // #sijapp cond.if target isnot "MOTOROLA" #
        PhotoListener,
        // #sijapp cond.end #
        Runnable {
    
    // Form for entering the name and description
    private FormEx name_Desc;
    
    // File data
    private InputStream fis;
    private int fsize;
    
    // File path and description TextField
    private static final int fileNameField = 1000;
    private static final int descriptionField = 1001;
    
    private Contact cItem;
    
    // Constructor
    public FileTransfer(Contact _cItem) {
        cItem = _cItem;
    }
        
    // Return the cItem belonging to this FileTransfer
    public Contact getReceiver() {
        return cItem;
    }
    
    // Set the file data
    public void setData(InputStream is, int size) {
        fis = is;
        fsize = size;
    }
    public InputStream getFileIS() {
        return fis;
    }
    public int getFileSize() {
        return fsize;
    }
    
    // Start the file transfer procedure depening on the ft type
    public void startFileTransfer() {
        FileBrowser fsBrowser = new FileBrowser(false);
        fsBrowser.setListener(this);
        fsBrowser.activate();
    }
    // #sijapp cond.if target isnot "MOTOROLA" #
    private ViewFinder vf;
    public void startPhotoTransfer() {
        String supports = System.getProperty("video.snapshot.encodings");
        if (StringConvertor.isEmpty(supports)) {
            cItem.getProtocol().processException(new JimmException(185, 0, true));
        } else {
            vf = new ViewFinder();
            vf.setPhotoListener(this);
            vf.show();
        }
    }
    // #sijapp cond.end #
    
    private FileSystem file;
    public void onFileSelect(String filename) {
        file = FileSystem.getInstance();
        try {
            file.openFile(filename);
            // FIXME resource leak
            InputStream fis = file.openInputStream();
            int size = (int)file.fileSize();
            // Set the file data in file transfer
            setData(fis, size);
            // Create filename and ask for name and description
            askForNameDesc(file.getName(), "");
        } catch (Exception e) {
            file.close();
            cItem.getProtocol().processException(new JimmException(191, 3, true));
        }
        
    }
    
    //
    public void onDirectorySelect(String s0) {}
    
    // Init the ft
    private void initFT(String filename, String description) {
        try {
            // Windows fix
            filename = filename.replace(':', '.');
            filename = filename.replace('/', '_');
            filename = filename.replace('\\', '_');
            filename = filename.replace('%', '_');
            cItem.getProtocol().sendFile(this, filename, description);
        } catch (Exception e) {
        }
    }
    
    /* Helpers for options UI: */
    private static final int  transferMode = 1002;
    private void askForNameDesc(String filename, String description) {
        name_Desc = new FormEx("name_desc", "ok", "back", this);
        name_Desc.addTextField(fileNameField, "filename", filename, 255, TextField.ANY);
        name_Desc.addTextField(descriptionField, "description", description, 255, TextField.ANY);
        String items = "jimm.net.ru|jimm.org";// + "|direct";
        // #sijapp cond.if protocols_JABBER is "true" #
        if (cItem instanceof protocol.jabber.JabberContact) {
            if (cItem.isSingleUserContact() && cItem.isOnline()) {
                items += "|ibb";
            }
        }
        // #sijapp cond.end #
        name_Desc.addSelector(transferMode, null, items, 0);
        name_Desc.addString(ResourceBundle.getString("size") + ": ", String.valueOf(fsize/1024)+" kb");
        // #sijapp cond.if modules_TRAFFIC is "true" #
        name_Desc.addString(ResourceBundle.getString("cost") + ": ",
                Traffic.costToString(((fsize / Options.getInt(Options.OPTION_COST_PACKET_LENGTH)) + 1)
                * Options.getInt(Options.OPTION_COST_OF_1M)));
        // #sijapp cond.end #
        name_Desc.endForm();
        name_Desc.show();
    }
    
    // Command listener
    public void commandAction(Command c, Displayable d) {
        if (name_Desc.saveCommand == c) {
            if (name_Desc.getSelectorValue(transferMode) == 2) {
                initFT(name_Desc.getTextFieldValue(fileNameField),
                        name_Desc.getTextFieldValue(descriptionField));
            } else {
                Progress.getProgress().init("ft_transfer");
                new Thread(this).start();
            }
        } else if (name_Desc.backCommand == c) {
            destroy();
            ContactList.activate();
        }
    }
    
    public void destroy() {
        if (null != file) {
            file.close();
        }
        try {
            if (null != fis) {
                fis.close();
            }
        } catch (Exception e) {
        }
        fis = null;
        name_Desc.clearForm();
        System.gc();
        // #sijapp cond.if target isnot "MOTOROLA" #
        if (null != vf) {
            vf.dismiss();
        }
        // #sijapp cond.end #
    }
    
    /** ************************************************************************* */
    public void run() {
        try {
            sendFileThroughWebThread();
        } catch(JimmException e) {
            JimmException.handleException(e);
        }
        destroy();
    }
    private void sendFileThroughWebThread() throws JimmException {
        InputStream is;
        OutputStream os;
        HttpConnection sc;
        
        String host = null;
        switch (name_Desc.getSelectorValue(transferMode)) {
            case 0: host = "files.jimm.net.ru:81"; break;
            case 1: host = "filetransfer.jimm.org"; break;
        }
        final String url = "http://" + host + "/__receive_file.php";
        String filename = name_Desc.getTextFieldValue(fileNameField);
        String description = name_Desc.getTextFieldValue(descriptionField);
        try {
            sc = (HttpConnection) Connector.open(url, Connector.READ_WRITE);
            sc.setRequestMethod(HttpConnection.POST);
            
            String boundary = "a9f843c9b8a736e53c40f598d434d283e4d9ff72";
            
            sc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            os = sc.openOutputStream();
            
            // Send post header
            StringBuffer buffer2 = new StringBuffer();
            buffer2.append("--").append(boundary).append("\r\n");
            buffer2.append("Content-Disposition: form-data; name=\"filedesc\"\r\n");
            buffer2.append("\r\n");
            buffer2.append(description);
            buffer2.append("\r\n");
            buffer2.append("--").append(boundary).append("\r\n");
            buffer2.append("Content-Disposition: form-data; name=\"jimmfile\"; filename=\"");
            buffer2.append(filename).append("\"\r\n");
            buffer2.append("Content-Type: application/octet-stream\r\n");
            buffer2.append("Content-Transfer-Encoding: binary\r\n");
            buffer2.append("\r\n");
            os.write(StringConvertor.stringToByteArrayUtf8(buffer2.toString()));
            
            // Send file data and show progress
            byte[] buffer = new byte[1024*2];
            int counter = fsize;
            while (counter > 0) {
                int read = fis.read(buffer);
                os.write(buffer, 0, read);
                counter -= read;
                if (fsize != 0) {
                    int percent = (100 - 2) * (fsize - counter) / fsize;
                    Progress.getProgress().setProgress(percent);
                }
            }
            
            // Send end of header
            String end = "\r\n--" + boundary + "--\r\n";
            os.write(StringConvertor.stringToByteArrayUtf8(end));

            // Read response
            is = sc.openInputStream();
            
            int respCode = sc.getResponseCode();
            if (HttpConnection.HTTP_OK != respCode) {
                throw new JimmException(194, respCode);
            }
            
            StringBuffer response = new StringBuffer();
            for (;;) {
                int read = is.read();
                if (read == -1) break;
                response.append((char)(read & 0xFF));
            }
            
            String respString = response.toString();
            
            int dataPos = respString.indexOf("http://");
            if (-1 == dataPos) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                DebugLog.println("server say '" + respString + "'");
                // #sijapp cond.end#
                throw new JimmException(194, 1);
            }
            
            respString = Util.replace(respString, "\r", "");
            respString = Util.replace(respString, "\n", "");
            
            // Send info about file
            StringBuffer messText = new StringBuffer();
            messText.append("File: ").append(filename).append("\n");
            messText.append("Size: ").append(fsize / 1024).append("KB\n");
            messText.append("Link: ").append(respString);
            
            cItem.sendMessage(messText.toString(), true);
            Progress.getProgress().setProgress(100);
            try {
                // Close all http connection headers
                os.close();
                is.close();
                sc.close();
            } catch (IOException e) {
            }
            
        } catch (IOException e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("send file", e);
            // #sijapp cond.end#
            throw new JimmException(194, 0);
        }
    }
    // #sijapp cond.if target isnot "MOTOROLA" #
    public void processPhoto(byte[] data) {
        try {
            setData(new ByteArrayInputStream(data), data.length);
            String filename = "jimm_cam_"
                    + Util.getDateString(false)
                    + "_" + Util.getNextCounter() + ".jpeg";
            askForNameDesc(filename.replace(' ', '_'), "");
        } catch (Exception e) {
            JimmException.handleException(new JimmException(191, 4));
        }
    }
    // #sijapp cond.end #
}
// #sijapp cond.end#