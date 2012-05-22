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
 File: src/DrawControls/AniImageList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Vladimir Kryukov
 *******************************************************************************/
/*
 * GifImageList.java
 *
 * Created on 4 Апрель 2008 г., 18:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package DrawControls.icons;

import java.io.InputStream;
import java.util.Vector;
import javax.microedition.lcdui.Form;
import jimm.chat.*;
import jimm.ui.base.*;
// #sijapp cond.if modules_ANISMILES is "true" #

/**
 *
 * @author vladimir
 */
public class AniImageList extends ImageList implements Runnable {
    
    private AniIcon[] icons;
    private Thread thread;

    //! Return image by index
    public Icon iconAt(int index) { //!< Index of requested image in the list
        if (index < size() && index >= 0) {
            return icons[index];
        }
        return null;
    }
    public int size() {
        return icons != null ? icons.length : 0;
    }

    public AniImageList() {
    }
    private String getAnimationFile(String resName, int i) {
        return resName + "/" + (i + 1) + ".png";
    }

    public void load(String resName, int w, int h) {
        Vector tmpIcons = new Vector();
        try {
            InputStream is = getClass().getResourceAsStream(resName + "/animate.bin");
            int smileCount = is.read() + 1;

            icons = new AniIcon[smileCount];
            ImageList imgs = new ImageList();
            for (int smileNum = 0; smileNum < smileCount; smileNum++) {
                int imageCount = is.read();
                int frameCount = is.read();
                imgs.load(getAnimationFile(resName, smileNum), imageCount);
                boolean loaded = (0 < imgs.size());
                AniIcon icon = loaded ? new AniIcon(imgs.iconAt(0), frameCount) : null;
                for (int frameNum = 0; frameNum < frameCount; frameNum++) {
                    int iconIndex = is.read();
                    int delay = is.read() * WAIT_TIME;
                    if (loaded) {
                        icon.addFrame(frameNum, imgs.iconAt(iconIndex), delay);
                    }
                }
                icons[smileNum] = icon;
                if (loaded) {
                    width = Math.max(width, icon.getWidth());
                    height = Math.max(height, icon.getHeight());
                }
            }
        } catch (Exception e) {
        }
        if (size() > 0) {
            thread = new Thread(this);
            thread.start();
        }
    }

    private static final int WAIT_TIME = 100;
    public void run() {
        long time = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(WAIT_TIME);
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                jimm.modules.DebugLog.panic("ani thread", e);
                // #sijapp cond.end#
            }
            long newTime = System.currentTimeMillis();
            boolean animationWorked = true;
            if (animationWorked) {
                boolean update = false;
                for (int i = 0; i < size(); i++) {
                    if (icons[i] != null) {
                        update |= icons[i].nextFrame(newTime - time);
                    }
                }
                if (update) {
                    Object screen = jimm.Jimm.getCurrentDisplay();
                    if (screen instanceof CanvasEx) {
                        ((CanvasEx)screen).invalidate();

//                    } else if (screen instanceof Form) {
//                        ((Form)screen).set(0, ((Form)screen).get(0));
                    }
                }
            }
            time = newTime;
        }
    }
}
// #sijapp cond.end #
