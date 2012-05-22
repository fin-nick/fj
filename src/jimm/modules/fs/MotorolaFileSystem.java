// #sijapp cond.if modules_FILES="true" & target = "MOTOROLA"#
package jimm.modules.fs;

/**
 *
 * @author vladimir
 */
import javax.microedition.io.file.*;
import javax.microedition.io.Connector;

import java.util.*;
import java.io.*;
import jimm.JimmException;

public class MotorolaFileSystem extends FileSystem {
	private com.motorola.io.FileConnection fileConnection;

	public Vector getDirectoryContents(String currDir,
            boolean onlyDirs) throws JimmException {
        Vector filelist = new Vector();
		try {
			if (currDir.equals(ROOT_DIRECTORY)) {
				String[] roots = com.motorola.io.FileSystemRegistry.listRoots();
				for (int i = 0; i < roots.length; i++) {
					filelist.addElement(new FileNode(currDir, roots[i].substring(1)));
                }

			} else {
				com.motorola.io.FileConnection fileconn = (com.motorola.io.FileConnection) Connector
						.open("file://" + currDir);
				String[] list = fileconn.list();
				filelist.addElement(new FileNode(currDir, FileSystem.PARENT_DIRECTORY));
				for (int i = 0; i < list.length; i++) {
					if (onlyDirs & !list[i].endsWith("/")) continue;
                    final String curfile = list[i].substring(currDir.length());
					filelist.addElement(new FileNode(currDir, curfile));
				}
				fileconn.close();
			}
		} catch (Exception e) {
			throw new JimmException(191, 0, true);
		}
        return filelist;
	}

	public long totalSize() throws Exception {
		return fileConnection.totalSize();
	}

    private String filename;
	public void openFile(String file) throws JimmException {
        try {
            fileConnection = (com.motorola.io.FileConnection) Connector.open("file://" + file);
            filename = file;
        } catch (Exception e) {
            fileConnection = null;
            filename = null;
		}
        if (null == fileConnection) {
            throw new JimmException(191, 1, true);
        }
	}

	public OutputStream openOutputStream() throws Exception {
		if (fileConnection.exists()) {
			fileConnection.delete();
		}
    	fileConnection.create();
		return fileConnection.openOutputStream();
	}

	public InputStream openInputStream() throws Exception {
		return fileConnection.openInputStream();
	}

	public void close() {
		try {
			if (null != fileConnection) {
                fileConnection.close();
            }
		} catch (Exception e) {
		}
	}

	public long fileSize() throws Exception {
        return (null == fileConnection) ? -1 : fileConnection.fileSize();
	}
	public String getName() {
        return filename;
	}
}
// #sijapp cond.end#
