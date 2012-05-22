// #sijapp cond.if modules_FILES="true" & target="SIEMENS2"#

package jimm.modules.fs;

import com.siemens.mp.io.file.*;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.*;
import jimm.JimmException;

import java.util.*;
import java.io.*;

class SiemensFileSystem extends FileSystem {
	private FileConnection fileConnection;

	public Vector getDirectoryContents(String currDir, boolean onlyDirs)
            throws JimmException {

        Vector filelist = new Vector();
		try {
			if (currDir.equals(ROOT_DIRECTORY)) {
				Enumeration roots = FileSystemRegistry.listRoots();
				while (roots.hasMoreElements()) {
					filelist.addElement(new FileNode(currDir, (String)roots.nextElement()));
                }

			} else {
				FileConnection fileconn = (FileConnection) Connector.open("file://" + currDir);

				Enumeration list = fileconn.list();
				filelist.addElement(new FileNode(currDir, FileSystem.PARENT_DIRECTORY));
				while (list.hasMoreElements()) {
					String filename = (String)list.nextElement();
					if (onlyDirs && !filename.endsWith("/")) {
                        continue;
                    }
					filelist.addElement(new FileNode(currDir, filename));
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

	public void openFile(String file) throws JimmException {
		try {
			fileConnection = (FileConnection) Connector.open("file://" + file);
		} catch (Exception e) {
            fileConnection = null;
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
		return (fileConnection != null) ? fileConnection.fileSize() : -1;
	}
	
	public String getName() {
        return (fileConnection != null) ? fileConnection.getName() : null;
	}
}

// #sijapp cond.end#