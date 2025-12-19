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
 * Manages loading and caching of Minecraft-style fonts.
 * - Minecraftia: Used for modpack names and settings text
 * - Minecrafter: Used for the LAUNCH button (decorative)
 */
@Log
public class MinecraftFontManager {

    private static Font minecraftiaFont;  // For modpack names, settings
    private static Font minecrafterFont;  // For LAUNCH button
    private static boolean initialized = false;

    /**
     * Get the Minecraftia font (for modpack names, settings text).
     *
     * @param size the font size
     * @return the Minecraftia font, or a fallback if not available
     */
    public static Font getMinecraftFont(float size) {
        if (!initialized) {
            loadFonts();
        }
        if (minecraftiaFont != null) {
            return minecraftiaFont.deriveFont(size);
        }
        return new Font("SansSerif", Font.BOLD, (int) size);
    }

    /**
     * Get the Minecraftia font with the specified style and size.
     *
     * @param style the font style (Font.PLAIN, Font.BOLD, etc.)
     * @param size the font size
     * @return the Minecraftia font, or a fallback if not available
     */
    public static Font getMinecraftFont(int style, float size) {
        if (!initialized) {
            loadFonts();
        }
        if (minecraftiaFont != null) {
            return minecraftiaFont.deriveFont(style, size);
        }
        return new Font("SansSerif", style, (int) size);
    }

    /**
     * Get the Minecrafter font (decorative, for LAUNCH button).
     *
     * @param style the font style
     * @param size the font size
     * @return the Minecrafter font, or a fallback if not available
     */
    public static Font getLaunchButtonFont(int style, float size) {
        if (!initialized) {
            loadFonts();
        }
        if (minecrafterFont != null) {
            return minecrafterFont.deriveFont(style, size);
        }
        return new Font("SansSerif", Font.BOLD, (int) size);
    }

    private static synchronized void loadFonts() {
        if (initialized) {
            return;
        }

        // Load Minecraftia (for modpack names, settings)
        try (InputStream is = MinecraftFontManager.class.getResourceAsStream(
                "/com/skcraft/launcher/fonts/Minecraftia-Regular.ttf")) {
            if (is != null) {
                minecraftiaFont = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(minecraftiaFont);
                log.info("Minecraftia font loaded successfully");
            } else {
                log.warning("Minecraftia font file not found, using fallback");
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load Minecraftia font, using fallback", e);
        }

        // Load Minecrafter (for LAUNCH button) - note: this font is in root, not fonts folder
        try (InputStream is = MinecraftFontManager.class.getResourceAsStream(
                "/com/skcraft/launcher/Minecrafter-reg.ttf")) {
            if (is != null) {
                minecrafterFont = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(minecrafterFont);
                log.info("Minecrafter font loaded successfully");
            } else {
                log.warning("Minecrafter font file not found, using fallback");
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to load Minecrafter font, using fallback", e);
        }

        initialized = true;
    }

    /**
     * Check if fonts were loaded successfully.
     *
     * @return true if at least one font is available
     */
    public static boolean isFontAvailable() {
        if (!initialized) {
            loadFonts();
        }
        return minecraftiaFont != null || minecrafterFont != null;
    }

    /**
     * Pre-load fonts to avoid delay on first render.
     * Call this early in initialization.
     */
    public static void preload() {
        if (!initialized) {
            loadFonts();
        }
    }
}
