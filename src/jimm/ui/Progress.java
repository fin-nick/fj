/*
 * Progress.java
 *
 * Created on 11 Июль 2010 г., 14:53
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui;

import jimm.SplashCanvas;
import jimm.cl.ContactList;
import jimm.util.ResourceBundle;
import protocol.Protocol;
import protocol.icq.action.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class Progress {
    private static Progress instance = new Progress();
    private Protocol proto;
    private boolean canceled;
    
    private Progress() {
    }
    public static Progress getProgress() {
        return instance;
    }
    private void init_(String captionLngStr) {
        canceled = false;
        SplashCanvas.setProgressBar(this);
        SplashCanvas.setMessage(ResourceBundle.getString(captionLngStr));
        setProgress(0);
    }
    public void init(String captionLngStr) {
        proto = null;
        init_(captionLngStr);
    }
    public void init(Protocol prototol) {
        proto = prototol;
        init_("connecting");
    }

    public void setProgress(int percent) {
        SplashCanvas.setProgress(percent);
        if (0 == percent) {
            canceled = false;
            SplashCanvas.showSplash();
        }
        if (100 == percent) {
            SplashCanvas.setProgressBar(null);
            ContactList.activate();
        }
    }
    public boolean isCanceled() {
        return canceled;
    }
    public void closeAnyAction() {
        try {
            canceled = true;
            Protocol p = proto;
            if (null != p) {
                p.disconnect();
                proto = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ContactList.activate();
    }
}
