package com.artofvector.workspace;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cross-module file-open bus. The file tree lives in the main window; the editor listens here.
 */
public final class Workspace {

    public interface FileOpenListener {
        void onFileOpen(Path path);
    }

    public interface FolderListener {
        void onFolderChanged(Path folder);
    }

    private final List<FileOpenListener> fileListeners = new CopyOnWriteArrayList<>();
    private final List<FolderListener> folderListeners = new CopyOnWriteArrayList<>();
    private Path rootFolder;

    public void addFileOpenListener(FileOpenListener listener) {
        fileListeners.add(listener);
    }

    public void addFolderListener(FolderListener listener) {
        folderListeners.add(listener);
    }

    public void openFile(Path path) {
        for (FileOpenListener listener : fileListeners) {
            listener.onFileOpen(path);
        }
    }

    public void setRootFolder(Path folder) {
        this.rootFolder = folder;
        for (FolderListener listener : folderListeners) {
            listener.onFolderChanged(folder);
        }
    }

    public Path rootFolder() {
        return rootFolder;
    }
}
