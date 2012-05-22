/*
 * InputTextBox.java
 *
 * @author Vladimir Krukov
 */

package jimm.ui;

import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.ui.base.*;
import jimm.util.ResourceBundle;

/**
 * Extended TestBox.
 * Now realized:
 * 1) long text editor;
 * 2) smiles;
 * 3) templates;
 * 4) text buffer;
 * 5) transliteration (cyrilic);
 * 6) restoring previous windows.
 *
 * @author Vladimir Kryukov
 */
public final class InputTextBox extends DisplayableEx implements CommandListener, ActionListener {
    // #sijapp cond.if modules_SMILES is "true" #
    private Command insertEmotionCommand;
    // #sijapp cond.end#
    private Command insertTemplateCommand;
    private Command pasteCommand;
    private Command quoteCommand;
    private Command clearCommand;
//    private Command detransliterateCommand; //it is changed
//    private Command transliterateCommand; //it is changed
    private Command nextCommand;
    private Command prevCommand;

    private Command cancelCommand;
    private Command okCommand;
    
    private int caretPos = 0;
    private boolean cancelNotify = false;
    private static final int MAX_CHAR_PER_PAGE = 3000;
    private static final int MIN_CHAR_PER_PAGE = 1000;
    private int textLimit;
    private String caption;
    private final TextBox box;

    private Vector strings = new Vector();
    private int current = 0;
    private int inputType;
    private int inputKeySet;
    private boolean hasTranslitButton;

    private int getItemType() {
        // #sijapp cond.if target is "MIDP2" #
        if (jimm.Jimm.isPhone(jimm.Jimm.PHONE_NOKIA)) {
            return Command.SCREEN;
        }
        // #sijapp cond.end #
        return Command.ITEM;
    }
    private TextBox createTextBox() {
        TextBox box = null;
        try {
            box = new TextBox(caption, "", Math.min(MAX_CHAR_PER_PAGE, textLimit), inputType);
        } catch (Exception e) {
            box = new TextBox(caption, "", Math.min(MIN_CHAR_PER_PAGE, textLimit), inputType);
        }
        setCaption(caption);
        
        int commandType = getItemType();

        if (TextField.ANY == inputType) {
            // #sijapp cond.if modules_SMILES is "true" #
            insertEmotionCommand  = initCommand("insert_emotion", commandType, 1);
            // #sijapp cond.end#
            insertTemplateCommand = initCommand("templates", commandType, 2);
            pasteCommand          = initCommand("paste", commandType, 3);
            quoteCommand          = initCommand("quote", commandType, 4);
            clearCommand          = initCommand("clear", commandType, 5);
//            if (StringConvertor.hasConverter(StringConvertor.DETRANSLITERATE)) {         //it is changed
//                detransliterateCommand = initCommand("detransliterate", commandType, 8); //it is changed
//            }                                                                            //it is changed
//            if (StringConvertor.hasConverter(StringConvertor.TRANSLITERATE)) {           //it is changed
//                transliterateCommand   = initCommand("transliterate",   commandType, 9); //it is changed
//            }                                                                            //it is changed
        }
        
        if (textLimit > box.getMaxSize()) {
            nextCommand = initCommand("next", commandType, 10);
            prevCommand = initCommand("prev", commandType, 11);
        }

        return box;
    }
    private void setOkCommandCaption(String title) {
        int okType = Command.OK;
        int cancelType = FormEx.getBackType();
        int cancelIndex = 15;
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" #
        if (Options.getBoolean(Options.OPTION_SWAP_SEND_AND_BACK)) {
            okType = FormEx.getBackType();
            cancelType = getItemType();
        }
        // #sijapp cond.end #
        // #sijapp cond.if target is "MIDP2"#
        if (Jimm.isS60v5()) {
            cancelIndex = 7;
        }
        // #sijapp cond.end #
        okCommand = initCommand(title, okType, 6);
        cancelCommand = initCommand("cancel", cancelType, cancelIndex);
    }

    private InputTextBox(String title, int len, int type, String okCaption) {
        setCaption(ResourceBundle.getString(title));
        cancelNotify = false;
        inputType = type;
        textLimit = len;
        inputKeySet = 0;
        box = createTextBox();
        setOkCommandCaption(okCaption);
        addDefaultCommands();
        box.setCommandListener(this);
        // #sijapp cond.if target is "MIDP2"#
        if (Jimm.isPhone(Jimm.PHONE_SE)) {
            System.gc();
        }
        // #sijapp cond.end#
    }
    public InputTextBox(String title, int len, int type) {
        this(title, len, TextField.ANY, "ok");
    }
    public InputTextBox(String title, int len, String okCaption) {
        this(title, len, TextField.ANY, okCaption);
    }
    public InputTextBox(String title, int len) {
        this(title, len, TextField.ANY);
    }
    
    public void setCancelNotify(boolean notify) {
        cancelNotify = notify;
    }

    public final boolean isOkCommand(Command cmd) {
        return okCommand == cmd;
    }
    

    private void switchText(int cur) {
        saveCurrentPage();
        current = Math.max(Math.min(cur, strings.size()), 0);
        setCurrentScreen();
    }
    
    private Command initCommand(String title, int type, int pos) {
        return new Command(ResourceBundle.getString(title), type, pos);
    }
    private void addCommand(Command cmd) {
        if (null != cmd) {
            box.addCommand(cmd);
        }
    }
    
    private void addDefaultCommands() {
        addCommand(okCommand);

        // #sijapp cond.if modules_SMILES is "true" #
        if (Emotions.isSupported()) {
            addCommand(insertEmotionCommand);
        }
        // #sijapp cond.end#
        addCommand(insertTemplateCommand);
        addCommand(pasteCommand);
        addCommand(quoteCommand);
        addCommand(clearCommand);
        hasTranslitButton = (3 == Options.getInt(Options.OPTION_MSGSEND_MODE));
//        if (hasTranslitButton) {                //it is changed
//            addCommand(detransliterateCommand); //it is changed
//            addCommand(transliterateCommand);   //it is changed
//        }                                       //it is changed
        addCommand(cancelCommand);

        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        updateConstraints();
        updateInitialInputMode();
        // #sijapp cond.end#
    }
    private void updateCommands() {
        boolean hasTranslitButtonNow = (3 == Options.getInt(Options.OPTION_MSGSEND_MODE));
        if (hasTranslitButton != hasTranslitButtonNow) {
            hasTranslitButton = hasTranslitButtonNow;
//            if (hasTranslitButton) {                       //it is changed
//                addCommand(detransliterateCommand);        //it is changed
//                addCommand(transliterateCommand);          //it is changed
//            } else {                                       //it is changed
//                box.removeCommand(detransliterateCommand); //it is changed
//                box.removeCommand(transliterateCommand);   //it is changed
//            }                                              //it is changed
        }
        
//        if (1 < strings.size()) {           //it is changed
            addCommand(nextCommand);
            addCommand(prevCommand);
//        } else {                            //it is changed
//            box.removeCommand(nextCommand); //it is changed
//            box.removeCommand(prevCommand); //it is changed
//        }                                   //it is changed
        
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        updateConstraints();
        updateInitialInputMode();
        // #sijapp cond.end#
        // #sijapp cond.if target is "MIDP2"#
        if (Jimm.isPhone(Jimm.PHONE_SE)) {
            System.gc();
        }
        // #sijapp cond.end#
    }

    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
    private void updateInitialInputMode() {
        int mode = Options.getInt(Options.OPTION_INPUT_MODE);
        if (inputKeySet == mode) {
            return;
        }
        inputKeySet = mode;
        try {
            String[] modes = {null, "UCB_BASIC_LATIN", "UCB_CYRILLIC"};
            box.setInitialInputMode(modes[mode]);
        } catch (Exception e) {
        }
    }

    private void updateConstraints() {
        int mode = inputType;
        if (Options.getBoolean(Options.OPTION_TF_FLAGS)) {
            mode |= TextField.INITIAL_CAPS_SENTENCE;
        }
        try {
            if (mode != box.getConstraints()) {
                box.setConstraints(mode);
            }
        } catch (Exception e) {
        }
    }
    // #sijapp cond.end#
    // #sijapp cond.if target is "MIDP2"#
    private void closeT9() {
        try {
            box.setConstraints(TextField.NON_PREDICTIVE);
        } catch (Exception e) {
        }
    }
    private boolean heedT9Fix() {
        return Jimm.isPhone(Jimm.PHONE_SE) && Jimm.isSetAppProperty("SE-T9-Fix");
    }
    // #sijapp cond.end#

    // #sijapp cond.if modules_LIGHT is "true" #
    public void closed() {
        CustomLight.setLightMode(CustomLight.ACTION_SYSTEM_OFF);
        // #sijapp cond.if target is "MIDP2"#
        if (heedT9Fix()) {
            closeT9();
        }
        // #sijapp cond.end#
    }
    public void showing() {
    }
    // #sijapp cond.end#
    public void restore() {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_SYSTEM);
        // #sijapp cond.end#
        // #sijapp cond.if target is "MIDP2"#
        if (heedT9Fix()) {
            updateConstraints();
        }
        // #sijapp cond.end#
        Jimm.setDisplay(this);
    }
    
    public final void setDisplayableToDisplay() {
        updateCommands();
        Jimm.setDisplayable(box);
    }

    private CommandListener listener;
    public void setCommandListener(CommandListener cl) {
        listener = cl;
    }

    public void commandAction(Command c, Displayable d) {
        try {
            if (cancelCommand == c) {
                if (cancelNotify) {
                    listener.commandAction(c, null);
                }
                back();
                
            } else if (clearCommand == c) {
                setString(null);
                
            } else if (nextCommand == c) {
                switchText(current + 1);
                
            } else if (prevCommand == c) {
                switchText(current - 1);
                
            } else if ((pasteCommand == c) || (quoteCommand == c)) {
                boolean quote = (quoteCommand == c);
                int pos = getCaretPosition();
                String clip = JimmUI.getClipBoardText(quote);
                if (quote && (2 < pos)) {
                    String text = box.getString();
                    if (('\n' == text.charAt(pos - 2)) && ('\n' == text.charAt(pos - 1))) {
                        pos--;
                        clip = clip.substring(0, clip.length() - 1);
                    }
                }
                insert(clip, pos);
                
//            } else if (detransliterateCommand == c) {                           //it is changed
//                setTextToBox(StringConvertor.detransliterate(box.getString())); //it is changed
                
//            } else if (transliterateCommand == c) {                             //it is changed
//                setTextToBox(StringConvertor.transliterate(box.getString()));   //it is changed
                
            } else if (insertTemplateCommand == c) {
                caretPos = getCaretPosition();
                Templates.getInstance().selectTemplate(this);
                
            } else if (Templates.getInstance().isMyOkCommand(c)) {
                String s = Templates.getInstance().getSelectedTemplate();
                if (null != s) {
                    insert(s, caretPos);
                }
                
                // #sijapp cond.if modules_SMILES is "true" #
            } else if (insertEmotionCommand == c) {
                caretPos = getCaretPosition();
                Emotions.selectEmotion(this);
                // #sijapp cond.end #
                
            } else {
                listener.commandAction(c, null);
            }
        } catch (Exception e) {
            // #sijapp cond.if modules_DEBUGLOG is "true" #
            DebugLog.panic("Text box", e);
            // #sijapp cond.end #
            if (isOkCommand(c)) {
                back();
            }
        }
  }
    private int getCaretPosition() {
        // #sijapp cond.if target is "MOTOROLA"#
        return box.getString().length();
        // #sijapp cond.else#
        return box.getCaretPosition();
        // #sijapp cond.end#
    }

    public void action(CanvasEx canvas, int cmd) {
        // #sijapp cond.if modules_SMILES is "true" # 
        if (canvas instanceof Selector) {
            // #sijapp cond.if target is "MOTOROLA"#
            caretPos = box.getString().length();
            // #sijapp cond.end#
            String space = getSpace();
            insert(space + ((Selector)canvas).getSelectedCode() + space, caretPos);
        }
        // #sijapp cond.end#
    }
    public final String getSpace() {
        return (0 == Options.getInt(Options.OPTION_MSGSEND_MODE)) ? " " : "  ";
    }

    public boolean isCancelCommand(Command cmd) {
        return cancelCommand == cmd;
    }
    public String getRawString() {
        saveCurrentPage();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < strings.size(); ++i) {
            String str = (String)strings.elementAt(i);
            if (null != str) {
                buf.append(str);
            }
        }
        return StringConvertor.removeCr(buf.toString());
    }
    public String getString() {
        String messText = getRawString();
        switch (Options.getInt(Options.OPTION_MSGSEND_MODE)) {
            case 1: return StringConvertor.detransliterate(messText);
            case 2: return StringConvertor.transliterate(messText);
        }
        return messText;
    }
    
    private void insert(String str, int pos) {
        try {
            box.insert(str, pos);
            return;
        } catch (Exception e) {
        }
        
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        try {
        // #sijapp cond.end #
        switchText(current);
        if (current < strings.size()) {
            strings.removeElementAt(current);
            String curStr = box.getString();
            if (!StringConvertor.isEmpty(curStr)) {
                pos = Math.min(pos, curStr.length());
                str = curStr.substring(0, pos) + str + curStr.substring(pos);
            }
        }
        int maxSize = box.getMaxSize();
        int size = str.length();
        for (int i = 0; size > 0; i += maxSize, size -= maxSize, ++current) {
            strings.insertElementAt(str.substring(i, i + Math.min(size, maxSize)), current);
        }
        current--;
        setCurrentScreen();
        // #sijapp cond.if modules_DEBUGLOG is "true" #
        } catch (Exception e) {
            DebugLog.println("TextBox error: " + e.getMessage());
        }
        // #sijapp cond.end #
    }

    private String getCurrentPageString() {
        String text = StringConvertor.notNull(box.getString());
        return StringConvertor.removeCr(text);
    }
    public final void saveCurrentPage() {
        String text = getCurrentPageString();
        if (strings.size() <= current) {
            if (0 < text.length()) {
                strings.addElement(text);
            }
        } else {
            strings.setElementAt(text, current);
        }
    }
    public void setTicker(String text) {
        if (Options.getBoolean(Options.OPTION_TICKER_SYSTEM)) { //ticker
            String boxText = box.getString();
            box.setTicker((null == text) ? null : new Ticker(text));
            if ((0 != boxText.length()) && (0 == box.getString().length())) {
                box.setString(boxText);
            }
        }
    }
    public final void setCaption(String title) {
        caption = (null == title) ? "" : title;
        if (Options.getBoolean(Options.OPTION_UNTITLED_INPUT)) {
            title = null;
        } else if ((strings.size() > 1) && (textLimit > box.getMaxSize())) {
            title = "[" + (current + 1) + "/" + (strings.size() + 1) + "] " + caption;
        } else {
            title = caption;
        }
        if (null != box) {
            String boxText = box.getString();
            box.setTitle(title);
            if (!boxText.equals(box.getString()) && (0 == box.getString().length())) {
                box.setString(boxText);
            }
        }
    }
    private String getCaption() {
        return caption;
    }

    private void setTextToBox(String text) {
        if (null == text) {
            box.setString("");
            return;
        }
        String normalizedText = text;
        // #sijapp cond.if target is "MIDP2"#
        try {
            if (Jimm.isPhone(Jimm.PHONE_NOKIA)) {
                box.setString("");
                box.insert(text, getCaretPosition());
                return;
            }
        } catch (Exception e) {
        }
        // #sijapp cond.end #
        try {
            box.setString(normalizedText);
            return;
        } catch (Exception e) {
        }
        try {
            box.setString(normalizedText.substring(0, textLimit));
            return;
        } catch (Exception e) {
        }
        box.setString("");
    }
    public final void setCurrentScreen() {
        setTextToBox((String) ((strings.size() > current)
                ? strings.elementAt(current) : null));
        setCaption(caption);
        updateCommands();
    }

    public void setString(String initText) {
        current = 0;
        strings.removeAllElements();
        if (null == initText) {
            setTextToBox(null);
            setCaption(caption);
            return;
        }
        int maxSize = box.getMaxSize();
        if (initText.length() > textLimit) {
            initText = initText.substring(0, textLimit);
        }
        int size = initText.length();
        for (int i = 0; size > 0; i += maxSize, size -= maxSize) {
            strings.addElement(initText.substring(i, i + Math.min(size, maxSize)));
        }
        setCurrentScreen();
    }
    public boolean isShown() {
        return box.isShown();
    }
}
