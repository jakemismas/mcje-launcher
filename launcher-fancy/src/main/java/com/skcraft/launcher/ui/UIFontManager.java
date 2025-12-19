/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.ui;

import lombok.extern.java.Log;

import java.awt.*;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Manages loading and caching of the UI font (Noto Sans).
 * Place NotoSans-Regular.ttf in resources/com/skcraft/launcher/
 */
@Log
public class UIFontManager {

    private static Font uiFont;
    private static Font uiFontBold;
    private static boolean initialized = false;

    /**
     * Get the UI font at the specified size.
     *
     * @param size the font size
     * @return the UI font, or a fallback if not available
     */
    public static Font getUIFont(float size) {
        if (!initialized) {
            loadFont();
        }
        if (uiFont != null) {
            return uiFont.deriveFont(size);
        }
        return new Font(Font.DIALOG, Font.PLAIN, (int) size);
    }

    /**
     * Get the UI font with the specified style and size.
     *
     * @param style the font style (Font.PLAIN, Font.BOLD, etc.)
     * @param size the font size
     * @return the UI font, or a fallback if not available
     */
    public static Font getUIFont(int style, float size) {
        if (!initialized) {
            loadFont();
        }

        Font baseFont = (style == Font.BOLD && uiFontBold != null) ? uiFontBold : uiFont;
        if (baseFont != null) {
            return baseFont.deriveFont(style, size);
        }
        return new Font(Font.DIALOG, style, (int) size);
    }

    private static synchronized void loadFont() {
        if (initialized) {
            return;
        }

        // Try to load Noto Sans Regular
        uiFont = loadFontFile("/com/skcraft/launcher/fonts/NotoSans-Regular.ttf");

        // Try to load Noto Sans Bold
        uiFontBold = loadFontFile("/com/skcraft/launcher/fonts/NotoSans-Bold.ttf");

        // If bold not found, derive from regular
        if (uiFontBold == null && uiFont != null) {
            uiFontBold = uiFont.deriveFont(Font.BOLD);
        }

        if (uiFont != null) {
            log.info("UI font (Noto Sans) loaded successfully");
        } else {
            log.warning("UI font not found, using system fallback. " +
                    "Place NotoSans-Regular.ttf in resources/com/skcraft/launcher/");
        }

        initialized = true;
    }

    private static Font loadFontFile(String path) {
        try (InputStream is = UIFontManager.class.getResourceAsStream(path)) {
            if (is != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);
                log.info("Loaded font: " + path + " -> " + font.getFontName());
                return font;
            } else {
                log.warning("Font resource not found: " + path);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load font: " + path, e);
        }
        return null;
    }

    /**
     * Check if the UI font was loaded successfully.
     *
     * @return true if the font is available
     */
    public static boolean isFontAvailable() {
        if (!initialized) {
            loadFont();
        }
        return uiFont != null;
    }

    /**
     * Pre-load the font to avoid delay on first render.
     */
    public static void preload() {
        if (!initialized) {
            loadFont();
        }
    }
}
