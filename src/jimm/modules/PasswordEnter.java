/*******************************************************************************
 Module: Protection Jimm password
 Version: 1.0  Date: 21-10-2008
 Author(s): Gusev Vladimir (VAGus)
 *******************************************************************************/
// #sijapp cond.if modules_PASSWORD is "true" #
package jimm.modules;

import javax.microedition.lcdui.*;
import javax.microedition.rms.*;

import jimm.comm.StringConvertor;
import jimm.ui.PopupWindow;
import jimm.util.*;
import jimm.*;

public class PasswordEnter extends Form implements CommandListener {

    private static Object lastDisplayable;
    private static int modeDisplayable;
    private static Form form = new Form(ResourceBundle.getString("pass_title_enter"));
    private static TextField passTextField = new TextField(ResourceBundle.getString("pass_enter_string"), "", 25, TextField.PASSWORD);
    private static Command backCommand = new Command(ResourceBundle.getString("back"), Command.BACK, 1);
    private static Command exitCommand = new Command(ResourceBundle.getString("pass_exit"), Command.EXIT, 1);
    private static Command okCommand = new Command(ResourceBundle.getString("pass_ok"), Command.OK, 2);

    private static PasswordEnter instance = new PasswordEnter();

    public PasswordEnter() {
        super(null);
        form.setCommandListener(this);
    }
    public static PasswordEnter getInstance() {
        return instance;
    }

    private static String getRMSRecordValue(RecordStore rms, int index) {
        String ret="";
        try {
            byte[] data = rms.getRecord(index);
            if (data != null) {
                ret = StringConvertor.utf8beByteArrayToString(data, 0, data.length);
            }
        } catch (RecordStoreException e) {
        }
        return ret;
    }
    private static void buildForm() {
        form.deleteAll();
        if (modeDisplayable==0) {
            form.removeCommand(backCommand);
            form.addCommand(exitCommand);
        } else {
            form.removeCommand(exitCommand);
            form.addCommand(backCommand);
        }
        form.addCommand(okCommand);
        form.append(passTextField);
        passTextField.setString("");
    }
    public static String getPassword() {
        String ret = "";
        RecordStore rms = null;

        try {
            rms = RecordStore.openRecordStore("password", false);
            ret = getRMSRecordValue(rms, 1);
        } catch (RecordStoreException e1) {
        }
        finally {
            try {
                rms.closeRecordStore();
            } catch (Exception e) {
            }
        }
        return ret;
    }
    public static void show(int mode) {
        modeDisplayable = mode;
        buildForm();
        lastDisplayable = Jimm.getCurrentDisplay();
        Jimm.setDisplay(form);
    }
    public static boolean init() {
        boolean ret=false;
        RecordStore rms = null;

        try {
            rms = RecordStore.openRecordStore("password", false);
            String pass = getRMSRecordValue(rms, 1);
            if (pass!=null && pass.length()>0) {
                ret=true;
            }
        } catch (RecordStoreException e1) {
        }
        finally {
            try {
                rms.closeRecordStore();
            } catch (Exception e) {
            }
        }
        return ret;
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            Jimm.setDisplay(lastDisplayable);
        } else if (c == exitCommand) {
            if(modeDisplayable==0) {
                try {
                    Jimm.getJimm().destroyApp(true);
                } catch (Exception e) {
                }
            }
        } else if (c == okCommand) {
            if (passTextField.getString().length()==0) {
                new PopupWindow(ResourceBundle.getString("error"), ResourceBundle.getString("pass_err_enter")).show();
                return;
            }
            String password = getPassword();
            if (password.compareTo(passTextField.getString())==0) {
                if(modeDisplayable==0) {
                    Jimm.continueStartApp();
                } else {
                    SplashCanvas.continueJimm();
                }
            } else {
                new PopupWindow(ResourceBundle.getString("error"), ResourceBundle.getString("pass_err_pass")).show();
                return;
            }
        }
    }
}
// #sijapp cond.end#