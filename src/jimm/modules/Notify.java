/*
 * Notify.java
 *
 * Created on 22 ������ 2007 �., 17:17
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.modules;

// #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
import java.util.*;
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import java.io.InputStream;
// #sijapp cond.end#
import jimm.*;
import jimm.cl.*;
import jimm.comm.Util;
import jimm.ui.PopupWindow;
import jimm.util.*;
import javax.microedition.lcdui.*;

/**
 *
 * @author vladimir
 */
// #sijapp cond.if modules_SOUND is "true" #
public class Notify
    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#    
         implements PlayerListener
    // #sijapp cond.end #
             {
    
    /* Notify notification typs */
    public static final int NOTIFY_MESSAGE = 0;
    public static final int NOTIFY_ONLINE  = 1;
    public static final int NOTIFY_TYPING  = 2;
    public static final int NOTIFY_MULTIMESSAGE = 3;
    public static final int NOTIFY_ALARM = 4;
    public static final int NOTIFY_OTHER = 5;   //other sound
    public static final int NOTIFY_OFFLINE = 6; //offline sound
    public static final int NOTIFY_EYE = 7;     //eye sound
    
    private long nextPlayTime = 0;
    private int playingType = 0;

    
    /**
     * Creates a new instance of Notify
     */
    private Notify() {
    }

    private static Notify _this = new Notify();
    public static Notify getSound() {
        return _this;
    }

    // #sijapp cond.if target is "SIEMENS2"#
    private String file = null;
    private int volume = 0;
    // #sijapp cond.end#
    
    private String getMimeType(String ext) {
        if ("mp3".equals(ext)) return "audio/mpeg";
        if ("mid".equals(ext) || "midi".equals(ext)) return "audio/midi";
        if ("amr".equals(ext)) return "audio/amr";
        if ("mmf".equals(ext)) return "audio/mmf";
        if ("imy".equals(ext)) return "audio/iMelody";
        return "audio/X-wav"; // wav
    }

    // #sijapp cond.if modules_ANDROID is "true" #
    private class StopVibro extends TimerTask {
        public void run() {
            Notify.this.vibrate(0);
        }
    }
    // #sijapp cond.end #
    private void vibrate(int duration) {

        Jimm.getJimm().getDisplay().vibrate(duration);
        // #sijapp cond.if modules_ANDROID is "true" #
        if (0 < duration) {
            new Timer().schedule(new StopVibro(), duration);
        }
        // #sijapp cond.end #
    }

    // Play a sound notification
    private int getNotificationMode(int notType) {
        switch (notType) {
            case NOTIFY_MESSAGE:
                return Options.getInt(Options.OPTION_MESS_NOTIF_MODE);
                
            case NOTIFY_ONLINE:
                return Options.getInt(Options.OPTION_ONLINE_NOTIF_MODE);

            case NOTIFY_OFFLINE:                                          //offline sound
                return Options.getInt(Options.OPTION_OFFLINE_NOTIF_MODE); //offline sound
                
            case NOTIFY_TYPING:
                return Options.getInt(Options.OPTION_TYPING_MODES);       //typing

            case NOTIFY_MULTIMESSAGE:
                return 0;

            case NOTIFY_OTHER:                                            //other sound
                return Options.getInt(Options.OPTION_OTHER_NOTIF_MODE);   //other sound
        }
        return 0;
    }
    private boolean isCompulsory(int notType) {
        switch (notType) {
            case NOTIFY_MESSAGE:
            case NOTIFY_MULTIMESSAGE:
            case NOTIFY_ALARM:
                return true;
        }
        return false;
    }
    private void playNotification(int notType) {
        final long now = System.currentTimeMillis();
        if (!isCompulsory(playingType) && isCompulsory(notType)) {
            nextPlayTime = 0;
        }
        if (NOTIFY_ALARM == notType) {
            if (!Options.getBoolean(Options.OPTION_ALARM)) return;
            if (now < nextPlayTime) return;
            nextPlayTime = now + 0; //it is changed
            playingType = notType;
            vibrate(1500);
            if (Options.getBoolean(Options.OPTION_SILENT_MODE)) return;
            playNotify(notType, 100);
            return;
        }

        // #sijapp cond.if modules_MAGIC_EYE is "true" #                                                 //eye sound
           if (NOTIFY_EYE == notType) {                                                                  //eye sound
               if (Options.getBoolean(Options.OPTION_SILENT_MODE)) return;                               //eye sound
               if (Options.getBoolean(Options.OPTION_EYE_NOTIF)) {                                       //eye sound
               if (!_this.play("eye.wav", 60)) if(!_this.play("eye.amr", 60)) _this.play("eye.mp3", 60); //eye sound
               }                                                                                         //eye sound
           }                                                                                             //eye sound
        // #sijapp cond.end #                                                                            //eye sound

        int vibraKind = Options.getInt(Options.OPTION_VIBRATOR);
        if (vibraKind == 2) {
            vibraKind = Jimm.isLocked() ? 1 : 0;
        }
        if ((vibraKind > 0) 
                && ((NOTIFY_MESSAGE == notType) || (NOTIFY_MULTIMESSAGE == notType))) {
            vibrate(Util.strToIntDef(Options.getString(Options.OPTION_VIBRATOR_TIME), 150)); //vibra time
        }
        
        if (Options.getBoolean(Options.OPTION_SILENT_MODE)) return;
        if (now < nextPlayTime) return;
        nextPlayTime = now + 2000;
        playingType = notType;

        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        switch (getNotificationMode(notType)) {
            case 1:
                try {
                    switch(notType) {
                        case NOTIFY_MESSAGE:
                            Manager.playTone(ToneControl.C4, 750, Options.getInt(Options.OPTION_MESS_NOTIF_VOL));
                            break;
                        case NOTIFY_ONLINE:
                        case NOTIFY_OFFLINE: //offline sound
                        case NOTIFY_TYPING:
                        case NOTIFY_OTHER: //other sound
                            Manager.playTone(ToneControl.C4 + 7, 750, Options.getInt(Options.OPTION_ONLINE_NOTIF_VOL));
                    }
                    
                } catch (Exception e) {
                }
                break;
                
            case 2:
                int notifyType = NOTIFY_MESSAGE;
                int volume = 0;
                switch (notType) {
                    case NOTIFY_MESSAGE:
                        volume = Options.getInt(Options.OPTION_MESS_NOTIF_VOL);
                        break;

                    case NOTIFY_ONLINE:
                        volume = Options.getInt(Options.OPTION_ONLINE_NOTIF_VOL);
                        break;

                    case NOTIFY_OFFLINE:                                           //offline sound
                        volume = Options.getInt(Options.OPTION_OFFLINE_NOTIF_VOL); //offline sound
                        break;                                                     //offline sound

                    case NOTIFY_TYPING:
                        volume = Options.getInt(Options.OPTION_TYPING_VOL);        //typing
                        break;

                    case NOTIFY_OTHER:                                             //other sound
                        volume = Options.getInt(Options.OPTION_OTHER_NOTIF_VOL);   //other sound
                        break;                                                     //other sound
                }
                playNotify(notType, volume);
                break;
        }
    }
    public static synchronized void playSoundNotification(int notType) {
        getSound().playNotification(notType);
    }

    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#    
    // Reaction to player events. (Thanks to Alexander Barannik for idea!)
    private Player player;
    public void playerUpdate(final Player player, final String event, Object eventData) {
        if (event.equals(PlayerListener.END_OF_MEDIA)) {
            closePlayer();
            // #sijapp cond.if target is "SIEMENS2"#
            if (null != file) {
                play(file, volume);
                file = null;
            }
            // #sijapp cond.end#
        }
    }

    private boolean testSoundFile(String source) {
        Notify notify = new Notify();
        notify.createPlayer(source);
        boolean ok = (null != notify.player);
        notify.closePlayer();
        return ok;
    }

    /* Creates player for file 'source' */
    private void createPlayer(String source) {
        closePlayer();
        try {
            /* What is file extention? */
            String ext = "wav";
            int point = source.lastIndexOf('.');
            if (-1 != point) {
                ext = source.substring(point + 1).toLowerCase();
            }
            
            InputStream is = getClass().getResourceAsStream(source);
            if (null != is) {
                player = Manager.createPlayer(is, getMimeType(ext));
                player.addPlayerListener(this);
            }
        } catch (Exception e) {
            closePlayer();
        }
    }
    private void closePlayer() {
        if (null != player) {
            try {
                player.stop();
            } catch (Exception e) {
            }
            try {
                player.close();
            } catch (Exception e) {
            }
            player = null;
        }
    }
    
    // sets volume for player
    private void setVolume(int value) {
        try {
            VolumeControl c = (VolumeControl) player.getControl("VolumeControl");
            if ((null != c) && (0 < value)) {
                c.setLevel(value);
            }
        } catch (Exception e) {
        }
    }
    
    private boolean play(String file, int volume) {
        createPlayer(file);
        if (null == file) {
            return false;
        }
        try {
            player.realize();
            setVolume(volume);
            player.prefetch();
            player.start();
        } catch (Exception e) {
            closePlayer();
            return false;
        }
        return true;
    }
    private void playNotify(int notifyType, int volume) {
        String file = files[notifyType];
        // #sijapp cond.if target is "SIEMENS2" #
        if (Options.getBoolean(Options.OPTION_VOLUME_BUGFIX)) {
            this.file = file;
            this.volume = volume;
            if (!play("silence.wav", 100)) {
                this.file = null;
                play(file, volume);
            }
        } else {
            play(file, volume);
        }
        // #sijapp cond.else #
        play(file, volume);
        // #sijapp cond.end #
    }
    private String selectSoundType(String name) {
        /* Test other extensions */
        String[] exts = Util.explode("mp3|wav|mid|midi|mmf|amr|imy|", '|');
        for (int i = 0; i < exts.length; ++i) {
            String testFile = name + exts[i];
            if (testSoundFile(testFile)) {
                return testFile;
            }
        }
        return null;
    }
    private String[] files = {null, null, null, null, null, null, null}; //it is changed
    public void initSounds() {
        files[NOTIFY_ONLINE]  = selectSoundType("/online.");
		files[NOTIFY_OFFLINE]  = selectSoundType("/offline."); //offline sound
        files[NOTIFY_MESSAGE] = selectSoundType("/message.");
        files[NOTIFY_TYPING]  = selectSoundType("/typing.");
        files[NOTIFY_ALARM]   = selectSoundType("/alarm.");
        files[NOTIFY_OTHER]   = selectSoundType("/other."); //other sound
    }
    public boolean hasSound(int notifyType) {
        return null != files[notifyType];
    }
    
    static public void changeSoundMode(boolean showPopup) {
        boolean newValue = !Options.getBoolean(Options.OPTION_SILENT_MODE);
        if (showPopup) {
            PopupWindow.showShadowPopup("Jimm",
                    ResourceBundle.getString(newValue ? "#sound_is_off" : "#sound_is_on"));
        }
        getSound().vibrate(newValue ? 0 : 100);
        getSound().closePlayer();
        Options.setBoolean(Options.OPTION_SILENT_MODE, newValue);
        Options.safeSave();
    }
}
// #sijapp cond.end #