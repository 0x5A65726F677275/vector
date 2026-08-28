package com.artofvector.ui;

import javax.swing.JComponent;

/**
 * A top-level workbench tab. Modules stay independent of each other and of window chrome.
 */
public interface WorkbenchModule {

    String tabTitle();

    JComponent component();

    default javax.swing.Icon tabIcon() {
        return null;
    }

    default void onActivated() {
        // Optional hook when the tab becomes visible.
    }
}
