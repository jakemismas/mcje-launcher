/*
 * SKCraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher;

import com.google.common.base.Supplier;
import com.skcraft.launcher.swing.SwingHelper;
import com.skcraft.launcher.ui.UIFontManager;
import lombok.extern.java.Log;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.logging.Level;

@Log
public class FancyLauncher {

    public static void main(final String[] args) {
        Launcher.setupLogger();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setContextClassLoader(FancyLauncher.class.getClassLoader());
                    UIManager.getLookAndFeelDefaults().put("ClassLoader", FancyLauncher.class.getClassLoader());
                    UIManager.getDefaults().put("SplitPane.border", BorderFactory.createEmptyBorder());
                    // Use native decorations (title bar, close/minimize buttons)
                    JFrame.setDefaultLookAndFeelDecorated(false);
                    JDialog.setDefaultLookAndFeelDecorated(false);
                    System.setProperty("sun.awt.noerasebackground", "true");
                    System.setProperty("substancelaf.windowRoundedCorners", "false");
                    // Enable better font rendering
                    System.setProperty("awt.useSystemAAFontSettings", "on");
                    System.setProperty("swing.aatext", "true");

                    // Mac-specific settings for better integration
                    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                        System.setProperty("apple.laf.useScreenMenuBar", "true");
                        System.setProperty("apple.awt.application.name", "MCJE Launcher");
                        System.setProperty("apple.awt.application.appearance", "system");
                    }

                    if (!SwingHelper.setLookAndFeel("com.skcraft.launcher.skin.LauncherLookAndFeel")) {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    }

                    // Set default font for all Swing components to Noto Sans (13pt for better readability)
                    setDefaultFont(UIFontManager.getUIFont(Font.PLAIN, 13f));

                    Launcher launcher = Launcher.createFromArguments(args);
                    launcher.setMainWindowSupplier(new CustomWindowSupplier(launcher));
                    launcher.showLauncherWindow();
                } catch (Throwable t) {
                    log.log(Level.WARNING, "Load failure", t);
                    SwingHelper.showErrorDialog(null, "Uh oh! The updater couldn't be opened because a " +
                            "problem was encountered.", "Launcher error", t);
                }
            }
        });
    }

    /**
     * Sets the default font for all Swing UI components.
     */
    private static void setDefaultFont(Font font) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, new javax.swing.plaf.FontUIResource(font));
            }
        }
    }

    private static class CustomWindowSupplier implements Supplier<Window> {

        private final Launcher launcher;

        private CustomWindowSupplier(Launcher launcher) {
            this.launcher = launcher;
        }

        @Override
        public Window get() {
            return new FancyLauncherFrame(launcher);
        }
    }

}
