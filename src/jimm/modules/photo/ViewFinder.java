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
 * File: src/jimm/modules/photo/ViewFinder.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Andreas Rossbacher, Dmitry Tunin, Vladimir Kryukov
 *******************************************************************************/

// #sijapp cond.if modules_FILES="true"#
// #sijapp cond.if target isnot "MOTOROLA" #
package jimm.modules.photo;

import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.media.*;
// #sijapp cond.if target isnot "MOTOROLA"#
import javax.microedition.media.control.VideoControl;
import jimm.*;
import jimm.comm.Util;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.util.ResourceBundle;
// #sijapp cond.end#

/** ************************************************************************* */
/** ************************************************************************* */
// Class for viewfinder
public class ViewFinder extends Canvas implements CommandListener, Runnable {
    
    private Object prev;
    private PhotoListener listener;
    
    public void show() {
        prev = Jimm.getCurrentDisplay();
        Jimm.setDisplay(this);
        start();
    }
    public void setPhotoListener(PhotoListener l) {
        listener = l;
    }
    private void back() {
        dismiss();
        Jimm.setDisplay(prev);
    }
    
    // Variables
    private Player player = null;
    private VideoControl videoControl = null;
    private byte[] data;
    
    // Commands
    private Command backCommand = new Command(ResourceBundle.getString("back"), FormEx.getBackType(), 2);
    private Command selectCommand = new Command(ResourceBundle.getString("ok"), Command.OK, 1);
    
    public ViewFinder() {
        addCommand(backCommand);
        addCommand(selectCommand);
        setCommandListener(this);
    }
    
    // paint method, inherid form Canvas
    public void paint(Graphics g) {
        g.setColor(0xffffffff);
        int width = getWidth();
        int height = getHeight();
        g.fillRect(0, 0, width, height);

        g.setColor(0x00000000);
        if (STATE_PREEVIEW == state) {
            if (null != thumbnailImage) {
                g.drawImage(thumbnailImage, width / 2, height / 2, Graphics.VCENTER | Graphics.HCENTER);
            } else {
                g.drawString("...", width / 2 - 5, height / 2, Graphics.TOP | Graphics.LEFT);
            }
        }
        String caption = (STATE_CAPTURE == state) ? "viewfinder" : "send_img";
        g.drawString(ResourceBundle.getString(caption), 1, 1, Graphics.TOP | Graphics.LEFT);
    }
    
    private void createPlayer(String url) throws IOException, MediaException {
        player = Manager.createPlayer(url);
        player.realize();
        videoControl = (VideoControl) player.getControl("VideoControl");
    }
    
    private void initVideo() throws JimmException {
        thumbnailImage = null;
        data = null;
        try {
            // Create the player
            // #sijapp cond.if target is "MIDP2" #
            try {
                if (Jimm.isPhone(Jimm.PHONE_NOKIA_S40)) {
                    createPlayer("capture://image");
                }
            } catch (Exception e) {
            }
            // #sijapp cond.end #
            if (null == videoControl) {
                createPlayer("capture://video");
            }
            if (null == videoControl) {
                throw new JimmException(180, 0);
            }
            videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, this);
            
            int canvasWidth  = getWidth();
            int canvasHeight = getHeight();
            try {
                videoControl.setDisplayLocation(2, 2);
                videoControl.setDisplaySize(canvasWidth - 4, canvasHeight - 4);
            } catch (MediaException me) {
                try {
                    videoControl.setDisplayFullScreen(true);
                } catch (MediaException me2) {
                }
            }
            int displayWidth  = videoControl.getDisplayWidth();
            int displayHeight = videoControl.getDisplayHeight();
            int x = (canvasWidth - displayWidth) / 2;
            int y = (canvasHeight - displayHeight) / 2;
            
            videoControl.setDisplayLocation(x, y);
            
            videoControl.setVisible(true);
            player.start();
        } catch (IOException ioe) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("initVideo (ioe)", ioe);
            // #sijapp cond.end#
            throw new JimmException(181, 0);
        }  catch (MediaException me) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("initVideo (me)", me);
            // #sijapp cond.end#
            throw new JimmException(181, 1);
        }  catch (SecurityException se) {
            throw new JimmException(181, 2);
        }  catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("initVideo (e)", e);
            // #sijapp cond.end#
            throw new JimmException(181, 3);
        }
    }
    // start the viewfinder
    private void start() {
        stop();
        try {
            initVideo();
            state = STATE_CAPTURE;
        }  catch (JimmException e) {
            stop();
            JimmException.handleException(e);
        }
    }
    
    // stop the viewfinder
    private void stop() {
        if (null != videoControl) {
            try {
                videoControl.setVisible(false);
                // Remove video control at SE phones placing it beyond screen border
                // #sijapp cond.if target is "MIDP2" #
                if (Jimm.isPhone(Jimm.PHONE_SE)) {
                    videoControl.setDisplayLocation(1000, 1000);
                }
                // #sijapp cond.end #
            } catch (Exception e) {
            }
        }
        videoControl = null;
        
        if (null != player) {
            try {
                if (Player.STARTED == player.getState()) {
                    player.stop();
                }
                player.close();
            } catch (Exception e) {
            }
        }
        player = null;
        System.gc();
    }
    
    private static int takePhotoMethod = 0;
    private byte[] getSnapshot(String type) {
        try {
            return videoControl.getSnapshot(type);
        } catch (SecurityException e) {
            return null;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getSnapshot(" + type + ")", e);
            // #sijapp cond.end#
            takePhotoMethod++;
            return null;
        }
    }
    
    private Image thumbnailImage = null;
    // take a snapshot form the viewfinder
    public void takeSnapshot() {
        if (null != player) {
            data = null;
            //"&width=" + this.res[0][this.res_marker] + "&height=" + this.res[1][this.res_marker]
            String type = Jimm.getAppProperty("Jimm-Snapshot", null);
            if (null != type) data = getSnapshot(type);
            switch (takePhotoMethod) {
                case 0: if (null == data) data = getSnapshot("encoding=jpeg&width=320&height=240");
                case 1: if (null == data) data = getSnapshot("encoding=jpeg&width=480&height=640");
                case 2: if (null == data) data = getSnapshot("encoding=jpeg&width=640&height=480");
                case 3: if (null == data) data = getSnapshot("encoding=jpeg");
                case 4: if (null == data) data = getSnapshot("JPEG");
            }
            if (null == data) data = getSnapshot(null);
            
            state = STATE_PREEVIEW;
            stop();
            repaint();
            if (null == data) {
                JimmException.handleException(new JimmException(183, 0));

            } else {
                Image img = Image.createImage(data, 0, data.length);
                thumbnailImage = Util.createThumbnail(img, getWidth(), getHeight());
                repaint();
            }
        }
    }
    public void dismiss() {
        stop();
        data = null;
        thumbnailImage = null;
        listener = null;
    }
    
    public void run() {
        takeSnapshot();
    }
    
    private static final byte STATE_CAPTURE = 0;
    private static final byte STATE_PREEVIEW = 1;
    private static final byte STATE_ERROR = 2;
    private byte state = STATE_CAPTURE;
    protected void keyPressed(int key) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_KEY_PRESS);
        // #sijapp cond.end#
        int keyCode = NativeCanvas.getJimmKey(key);
        int action = NativeCanvas.getJimmAction(keyCode, key);
        doKeyPressed(keyCode, action);
    }
    
    // Key pressed
    public void doKeyPressed(int keyCode, int actionCode) {
        switch (actionCode) {
            case NativeCanvas.RIGHT_SOFT:
                if (STATE_CAPTURE == state) {
                    stop();
                    back();
                    
                } else {
                    state = STATE_CAPTURE;
                    start();
                }
                break;
                
            case NativeCanvas.NAVIKEY_FIRE:
                if (STATE_CAPTURE == state) {
                    state = STATE_PREEVIEW;
                    new Thread(this).start();
                    
                } else if (null != thumbnailImage) {
                    stop();
                    listener.processPhoto(data);
                    dismiss();
                }
                break;
        }
    }
    
    public void commandAction(Command command, Displayable displayable) {
        if (backCommand == command) {
            doKeyPressed(NativeCanvas.RIGHT_SOFT, NativeCanvas.RIGHT_SOFT);
            
        } else if (selectCommand == command) {
            doKeyPressed(KEY_NUM5, NativeCanvas.NAVIKEY_FIRE);
        }
        
    }
    
}
// #sijapp cond.end #
// #sijapp cond.end#