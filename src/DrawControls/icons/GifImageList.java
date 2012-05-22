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
 File: src/DrawControls/GifImageList.java
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
import jimm.ui.base.*;
// #sijapp cond.if modules_GIFSMILES is "true" #

/**
 *
 * @author vladimir
 */
public class GifImageList extends ImageList implements Runnable {
    
    private GifIcon[] icons;
    private Thread thread;

    //! Return image by index
    public Icon iconAt(int index) { //!< Index of requested image in the list
        if (index < size() && index >= 0) {
            return icons[index];
        }
        return null;
    }
    public int size() {
        return (null == icons) ? 0 : icons.length;
    }

    /** Creates a new instance of GifImageList */
    public GifImageList() {
    }
    private String getAnimationFile(String resName, int i) {
        return resName + "/" + (i + 1) + ".gif";
    }

    public void load(String resName, int w, int h) {
        Vector tmpIcons = new Vector();
        try {
            for (int i = 0; ; i++) {
                GifDecoder gd = new GifDecoder();
                if (GifDecoder.STATUS_OK != gd.read(getAnimationFile(resName, i))) {
                    break;
                }
                tmpIcons.addElement(new GifIcon(gd));
                width = Math.max(width, gd.getImage().getWidth());
                height = Math.max(height, gd.getImage().getHeight());
            }
        } catch (Exception e) {
        }
        icons = new GifIcon[tmpIcons.size()];
        tmpIcons.copyInto(icons);
        if (size() > 0) {
            thread = new Thread(this);
            thread.start();
        }
    }

    private long time;
    private static final int TIME = 100;
    public void run() {
        time = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(TIME);
            } catch (Exception e) {
            }
            long newTime = System.currentTimeMillis();
            Object screen = jimm.Jimm.getCurrentDisplay();
            boolean animationWorked = true;
            boolean screenIsCanvasEx = (screen instanceof CanvasEx);
            if (animationWorked) {
                boolean update = false;
                for (int i = 0; i < size(); i++) {
                    update |= icons[i].nextFrame(newTime - time);
                }
                if (update) {
                    if (screenIsCanvasEx) {
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
