// #sijapp cond.if modules_FILES="true"#
package jimm.modules.fs;

import jimm.util.ResourceBundle;
import java.util.*;
import java.io.*;
import jimm.*;


public abstract class FileSystem {
	public static final String ROOT_DIRECTORY = "/";
	public static final String PARENT_DIRECTORY = "../";

    // #sijapp cond.if target is "MOTOROLA" | target is "MIDP2" #
    static private final boolean supports_JSR75 = supportJSR75();
    static private boolean supportJSR75() {
        try {
            return Class.forName("javax.microedition.io.file.FileConnection") != null;
        } catch (ClassNotFoundException e) {
        }
        return false;
    }
   // #sijapp cond.end#
    public static boolean isSupported() {
		// #sijapp cond.if target="MOTOROLA"#
        return true;
		// #sijapp cond.elseif target="SIEMENS2"#
        return true;
		// #sijapp cond.else#
    	return supports_JSR75;
		// #sijapp cond.end#
    }
	public static FileSystem getInstance() {
		// #sijapp cond.if target="MOTOROLA"#
        if (supports_JSR75) {
            return new JSR75FileSystem();
        }
        return new MotorolaFileSystem();
		// #sijapp cond.elseif target="SIEMENS2"#
        return new SiemensFileSystem();
		// #sijapp cond.else#
    	return new JSR75FileSystem();
		// #sijapp cond.end#
	}

	public abstract Vector getDirectoryContents(String dir, boolean onlyDirs) throws JimmException;

	public abstract void openFile(String file) throws JimmException;
	public abstract OutputStream openOutputStream() throws Exception;
	public abstract InputStream openInputStream() throws Exception;
	public abstract void close();
	public abstract long fileSize() throws Exception;
	public abstract long totalSize() throws Exception;
	
	public abstract String getName();
}
// #sijapp cond.end#