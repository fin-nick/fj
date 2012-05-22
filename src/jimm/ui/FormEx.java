/*
 * FormEx.java
 *
 * Created on 24 Май 2008 г., 21:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui;

import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.Jimm;
import jimm.comm.Util;
import jimm.modules.*;
import jimm.ui.base.*;
import jimm.util.*;
import jimm.comm.*;

/**
 *
 * @author Vladimir Krukov
 */
public class FormEx extends DisplayableEx {
    
    public static final int getBackType() {
        // #sijapp cond.if target is "MIDP2" #
        return Jimm.isPhone(Jimm.PHONE_SE_SYMBIAN) ? Command.CANCEL : Command.BACK;
        // #sijapp cond.else#
        return Command.BACK;
        // #sijapp cond.end#
    }
    
    private Form form;
    // Initialize commands
    public final Command backCommand;
    public final Command saveCommand;
    /** Creates a new instance of FormEx */
    public FormEx(String title, String ok, String back, CommandListener l) {
        form = new Form(ResourceBundle.getString(title));
        backCommand = new Command(ResourceBundle.getString(back), FormEx.getBackType(), 2);
        saveCommand = new Command(ResourceBundle.getString(ok), Command.OK, 1);
        form.addCommand(saveCommand);
        form.addCommand(backCommand);
        form.setCommandListener(l);
    }
    public void setItemStateListener(ItemStateListener l) {
        form.setItemStateListener(l);
    }
    public int getItemId(Item item) {
        for (int num = 0; num < controlIds.size(); ++num) {
            if (controls.elementAt(num) == item) {
                return ((Integer)controlIds.elementAt(num)).intValue();
            }
        }
        return -1;
    }
    
    public void addTextField(int controlId, String label, String text, int size, int type) {
        text = StringConvertor.notNull(text);
        text = (text.length() > size) ? text.substring(0, size) : text;
        append(controlId, new TextField(ResourceBundle.getString(label), text, size, type));
    }
    public void setTextFieldLabel(int controlId, String label) {
        try {
            ((TextField)get(controlId)).setLabel(ResourceBundle.getString(label));
        } catch (Exception e) {
        }
    }
    public void addLatinTextField(int controlId, String label, String text, int size, int type) {
        text = StringConvertor.notNull(text);
        text = (text.length() > size) ? text.substring(0, size) : text;
        TextField tf = new TextField(ResourceBundle.getString(label), text, size, type);
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        try {
            tf.setInitialInputMode("UCB_BASIC_LATIN");
        } catch (Exception e) {
        }
        // #sijapp cond.end#
        append(controlId, tf);
    }
    public void addVolumeControl(int controlId, String label, int current) {
        append(controlId, new Gauge(ResourceBundle.getString(label), true, 10, current / 10));
    }
    public void addCheckBox(int controlId, String label, boolean selected) {
        addChoiceGroup(controlId, null, ChoiceGroup.MULTIPLE);
        addChoiceItem(controlId, label, selected);
    }
    public void addChoiceGroup(int controlId, String label, int type) {
        ChoiceGroup choiceGroup =  new ChoiceGroup(ResourceBundle.getString(label), type);
        // #sijapp cond.if target is "MIDP2"#
        if (Jimm.isPhone(Jimm.PHONE_NOKIA_S60)) {
            choiceGroup.setFitPolicy(ChoiceGroup.TEXT_WRAP_ON);
        }
        // #sijapp cond.end#
        append(controlId, choiceGroup);
    }
    public void addChoiceItem(int controlId, String label, boolean selected) {
        ChoiceGroup choiceGroup = (ChoiceGroup)get(controlId);
        choiceGroup.append(ResourceBundle.getString(label), null);
        if (selected) {
            choiceGroup.setSelectedIndex(choiceGroup.size() - 1, true);
        }
    }
    public void addSelector(int controlId, String label, String items, int index) {
        int choiceType = Choice.EXCLUSIVE;
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        choiceType = Choice.POPUP;
        // #sijapp cond.end#
        addChoiceGroup(controlId, label, choiceType);
        String[] strings = Util.explode(items, '|');
        if (0 == strings.length) {
            return;
        }
        for (int i = 0; i < strings.length; ++i) {
            addChoiceItem(controlId, strings[i], index == i);
        }
    }
    public void addString(String label, String text) {
        if (!StringConvertor.isEmpty(text)) {
            text = " " + text;
        }
        form.append(new StringItem(ResourceBundle.getString(label), text));
    }
    public void addString(String text) {
        form.append(text);
    }
    
    private Vector controlIds = new Vector();
    private Vector controls   = new Vector();
    private void append(int controlId, Item item) {
        form.append(item);
        controlIds.addElement(new Integer(controlId));
        controls.addElement(item);
    }
    private Item get(int controlId) {
        for (int num = 0; num < controlIds.size(); ++num) {
            if (((Integer)controlIds.elementAt(num)).intValue() == controlId) {
                return (Item)controls.elementAt(num);
            }
        }
        return null;
    }
    public boolean hasControl(int controlId) {
        return null != get(controlId);
    }
    
    public int getVolumeValue(int controlId) {
        try {
            return ((Gauge)get(controlId)).getValue() * 10;
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getGaugeValue", e);
            // #sijapp cond.end#
        }
        return 0;
    }
    public String getTextFieldValue(int controlId) {
        try {
            return ((TextField)get(controlId)).getString();
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getTextFieldValue", e);
            // #sijapp cond.end#
        }
        return null;
    }
    public int getSelectorValue(int controlId) {
        try {
            return ((ChoiceGroup)get(controlId)).getSelectedIndex();
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getSelectorValue", e);
            // #sijapp cond.end#
        }
        return 0;
    }
    public String getSelectorString(int controlId) {
        try {
            return ((ChoiceGroup)get(controlId)).getString(getSelectorValue(controlId));
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getSelectorString", e);
            // #sijapp cond.end#
        }
        return null;
    }
    
    public boolean getChoiceItemValue(int controlId, int itemNum) {
        try {
            return ((ChoiceGroup)get(controlId)).isSelected(itemNum);
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getChoiceItemValue", e);
            // #sijapp cond.end#
        }
        return false;
    }
    
    public boolean getCheckBoxValue(int controlId) {
        try {
            return ((ChoiceGroup)get(controlId)).isSelected(0);
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("getChoiceItemValue", e);
            // #sijapp cond.end#
        }
        return false;
    }
    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
    public void addImage(Image image) {
        form.append(image);
    }
    // #sijapp cond.end#
    public void restore() {
        Jimm.setDisplay(this);
    }
    public final void setDisplayableToDisplay() {
        Jimm.setDisplayable(form);
    }
    
    public void showing() {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_SYSTEM);
        // #sijapp cond.end#
    }
    
    public void closed() {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_SYSTEM_OFF);
        // #sijapp cond.end #
    }
    
    public void clearForm() {
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        form.deleteAll();
        // #sijapp cond.else#
        while (0 < form.size()) {
            form.delete(0);
        }
        // #sijapp cond.end#
        controlIds.removeAllElements();
        controls.removeAllElements();
        setItemStateListener(null);
    }
    
    public void destroy() {
        clearForm();
        form.setCommandListener(null);
    }

    public void endForm() {
        // #sijapp cond.if target is "MIDP2"#
        if (Jimm.isPhone(Jimm.PHONE_JBED)) {
            addString(" ");
        }
        // #sijapp cond.end#
    }
}
