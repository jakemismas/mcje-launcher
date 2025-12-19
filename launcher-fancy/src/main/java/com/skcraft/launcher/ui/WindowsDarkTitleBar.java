/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.ui;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import lombok.extern.java.Log;

import javax.swing.*;
import java.awt.*;

/**
 * Utility to enable dark title bar on Windows 10/11.
 */
@Log
public class WindowsDarkTitleBar {

    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 = 19;

    private static boolean isWindows;
    private static Dwmapi dwmapi;

    static {
        isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (isWindows) {
            try {
                dwmapi = Native.load("dwmapi", Dwmapi.class);
            } catch (Throwable e) {
                log.warning("Failed to load dwmapi: " + e.getMessage());
                dwmapi = null;
            }
        }
    }

    /**
     * Enable dark title bar for the given window.
     * Only works on Windows 10 version 1809 and later.
     *
     * @param window the window to modify
     */
    public static void enable(Window window) {
        if (!isWindows || dwmapi == null || window == null) {
            return;
        }

        try {
            WinDef.HWND hwnd = getHWND(window);
            if (hwnd != null) {
                // Try the newer attribute first (Windows 10 20H1+)
                int[] value = new int[]{1};
                int result = dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, value, 4);

                // If that fails, try the older attribute (Windows 10 1809-1909)
                if (result != 0) {
                    dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1, value, 4);
                }

                log.info("Dark title bar enabled for window");
            }
        } catch (Throwable e) {
            log.warning("Failed to enable dark title bar: " + e.getMessage());
        }
    }

    private static WinDef.HWND getHWND(Window window) {
        try {
            // Make window displayable to get native handle
            if (!window.isDisplayable()) {
                window.addNotify();
            }

            // Get the native window handle
            long hwndLong = com.sun.jna.Native.getWindowID(window);
            return new WinDef.HWND(com.sun.jna.Pointer.createConstant(hwndLong));
        } catch (Throwable e) {
            log.warning("Failed to get HWND: " + e.getMessage());
            return null;
        }
    }

    /**
     * JNA interface for dwmapi.dll
     */
    public interface Dwmapi extends StdCallLibrary {
        int DwmSetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, int[] pvAttribute, int cbAttribute);
    }
}
