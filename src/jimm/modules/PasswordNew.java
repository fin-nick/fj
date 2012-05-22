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
import jimm.cl.ContactList;
import jimm.util.*;
import jimm.*;

public class PasswordNew extends Form implements CommandListener {

//    private static Object lastDisplayable;
    private static Form form = new Form(ResourceBundle.getString("pass_title_new"));
    private static TextField passTextField0 = new TextField(ResourceBundle.getString("pass_old"), "", 10, TextField.PASSWORD);
    private static TextField passTextField = new TextField(ResourceBundle.getString("pass_new"), "", 10, TextField.PASSWORD);
    private static TextField passTextField2 = new TextField(ResourceBundle.getString("pass_retry"), "", 10, TextField.PASSWORD);
    private static Command backCommand = new Command(ResourceBundle.getString("back"), Command.BACK, 1);
    private static Command saveCommand = new Command(ResourceBundle.getString("save"), Command.OK, 1);

    private static PasswordNew instance = new PasswordNew();

    public PasswordNew() {
        super(null);
        form.setCommandListener(this);
    }
    public static PasswordNew getInstance() {
        return instance;
    }

    private static void addValueToRMSRecord(RecordStore rms, String value) {
        try {
            byte[] buffer;
            buffer = StringConvertor.stringToByteArray(value, true);
            rms.addRecord(buffer, 0, buffer.length);
        } catch (RecordStoreException e) {
        }
    }
    private static void buildForm() {
        form.deleteAll();
        form.addCommand(backCommand);
        form.addCommand(saveCommand);
        if (PasswordEnter.getInstance().getPassword().length()!=0) {
            form.append(passTextField0);
        }
        form.append(passTextField);
        form.append(passTextField2);
        passTextField0.setString("");
        passTextField.setString("");
        passTextField2.setString("");
    }

    public static void show() {
        buildForm();
//        lastDisplayable = Jimm.getCurrentDisplay();
        Jimm.setDisplay(form);
    }
    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            back();
        } else if (c == saveCommand) {
            switch(save()) {
                case -1:
                    new PopupWindow(ResourceBundle.getString("error"), ResourceBundle.getString("pass_err_old")).show();
                    break;
                case -2:
                    new PopupWindow(ResourceBundle.getString("error"), ResourceBundle.getString("pass_err_new")).show();
                    break;
                case 0:
                    back();
            }
        }
    }
    private static void back() {
        ContactList.activate();
//        Jimm.setDisplay(lastDisplayable);
//        lastDisplayable = null;
    }
    private static int save() {
        RecordStore rms = null;
        String old_password = PasswordEnter.getInstance().getPassword();
        if (old_password.length()!=0 &&
            old_password.compareTo(passTextField0.getString())!=0) {
            return -1;
        }
        if (passTextField.getString().compareTo(passTextField2.getString())!=0) {
            return -2;
        }
        try {
            String[] stores = RecordStore.listRecordStores();
            for(int i=0;i<stores.length;i++) {
                if (stores[i].compareTo("password")==0) {
                    RecordStore.deleteRecordStore(stores[i]);
                    break;
                }
            }
            rms = RecordStore.openRecordStore("password", true);
            addValueToRMSRecord(rms, passTextField.getString());
        } catch (RecordStoreException e1) {
        }
        finally {
            try {
                rms.closeRecordStore();
            } catch (Exception e) {
            }
        }
        return 0;
    }
}
// #sijapp cond.end#