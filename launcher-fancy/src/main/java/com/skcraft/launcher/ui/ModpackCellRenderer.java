/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.skcraft.launcher.ui;

import com.skcraft.launcher.Instance;
import lombok.extern.java.Log;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom cell renderer for the modpack list with 64x64 icons.
 * Uses manual painting for reliable rendering in JTable.
 */
@Log
public class ModpackCellRenderer extends JComponent implements TableCellRenderer {

    private static final int ICON_SIZE = 52;
    private static final int ROW_HEIGHT = 72;
    private static final int PADDING = 8;
    private static final int COLLAPSED_WIDTH_THRESHOLD = 92; // Below this width, hide text
    private static final Color BACKGROUND_SELECTED = new Color(60, 60, 60, 220);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_SECONDARY = new Color(180, 180, 180);

    private BufferedImage defaultIcon;
    private final Map<String, BufferedImage> iconCache = new HashMap<>();

    // Cached fonts - Minecraftia for title, Noto Sans for status
    private static Font titleFont;
    private static Font statusFont;
    private static boolean fontsInitialized = false;

    private static void initFonts() {
        if (!fontsInitialized) {
            // Use Minecraftia for modpack title
            titleFont = MinecraftFontManager.getMinecraftFont(Font.PLAIN, 14f);
            statusFont = UIFontManager.getUIFont(Font.PLAIN, 11f);
            fontsInitialized = true;
        }
    }

    // Current cell state
    private String title = "";
    private String status = "";
    private BufferedImage icon = null;
    private boolean selected = false;
    private boolean pressed = false;

    // Track which row is currently pressed (for pressed effect)
    private static int pressedRow = -1;

    public ModpackCellRenderer() {
        setOpaque(false);
        loadDefaultIcons();
    }

    private void loadDefaultIcons() {
        defaultIcon = loadResourceIcon("/com/skcraft/launcher/default_modpack_icon.png");
        if (defaultIcon == null) {
            defaultIcon = loadResourceIcon("/com/skcraft/launcher/instance_icon.png");
        }
        if (defaultIcon == null) {
            // Create a simple placeholder icon
            defaultIcon = createPlaceholderIcon();
        }
    }

    private BufferedImage createPlaceholderIcon() {
        BufferedImage img = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(60, 60, 60));
        g.fillRect(0, 0, ICON_SIZE, ICON_SIZE);
        g.setColor(new Color(100, 100, 100));
        g.drawRect(0, 0, ICON_SIZE - 1, ICON_SIZE - 1);
        g.dispose();
        return img;
    }

    private BufferedImage loadResourceIcon(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                return ImageIO.read(is);
            }
        } catch (Exception e) {
            log.warning("Failed to load icon: " + path);
        }
        return null;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        this.selected = isSelected;
        this.pressed = (row == pressedRow);

        // Get instance from model
        Instance instance = null;
        if (table.getModel() instanceof com.skcraft.launcher.swing.InstanceTableModel) {
            instance = ((com.skcraft.launcher.swing.InstanceTableModel) table.getModel()).getInstance(row);
        }

        if (instance != null) {
            this.title = instance.getTitle();

            // Set status and icon based on instance state (always show modpack icon or default)
            if (!instance.isLocal()) {
                this.status = "Click to install";
                this.icon = getInstanceIcon(instance);
            } else if (instance.isUpdatePending()) {
                this.status = "Update available";
                this.icon = getInstanceIcon(instance);
            } else {
                this.status = "Ready to play";
                this.icon = getInstanceIcon(instance);
            }
        } else {
            this.title = "Unknown";
            this.status = "";
            this.icon = defaultIcon;
        }

        return this;
    }

    private BufferedImage getInstanceIcon(Instance instance) {
        String instanceName = instance.getName();
        if (instanceName == null) {
            return defaultIcon;
        }

        String cacheKey = instanceName;
        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }

        // First, check for bundled icon in resources (distributed with launcher)
        String resourcePath = "/com/skcraft/launcher/icons/" + instanceName + ".png";
        BufferedImage bundledIcon = loadResourceIcon(resourcePath);
        if (bundledIcon != null) {
            BufferedImage scaled = scaleImage(bundledIcon, ICON_SIZE, ICON_SIZE);
            iconCache.put(cacheKey, scaled);
            log.info("Loaded bundled icon for: " + instance.getTitle() + " from " + resourcePath);
            return scaled;
        }

        // Second, check for icon.png in instance directory (user-added)
        File dir = instance.getDir();
        if (dir != null) {
            File iconFile = new File(dir, "icon.png");
            if (iconFile.exists()) {
                try {
                    BufferedImage iconImg = ImageIO.read(iconFile);
                    if (iconImg != null) {
                        BufferedImage scaled = scaleImage(iconImg, ICON_SIZE, ICON_SIZE);
                        iconCache.put(cacheKey, scaled);
                        log.info("Loaded custom icon for: " + instance.getTitle() + " from " + iconFile.getAbsolutePath());
                        return scaled;
                    }
                } catch (Exception e) {
                    log.warning("Failed to load custom icon for " + instance.getTitle() + ": " + e.getMessage());
                }
            }
        }

        return defaultIcon;
    }

    private BufferedImage scaleImage(BufferedImage original, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        return scaled;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Initialize fonts on first paint (ensures classloader is ready)
        initFonts();

        Graphics2D g2d = (Graphics2D) g.create();
        // Disable antialiasing for blocky Minecraft-style borders
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        boolean isCollapsed = width < COLLAPSED_WIDTH_THRESHOLD;

        // Draw icon - center it when collapsed, left-align when expanded
        int iconX = isCollapsed ? (width - ICON_SIZE) / 2 : PADDING;
        int iconY = (height - ICON_SIZE) / 2;

        // Draw Minecraft-style selection border when selected (dark gray/black)
        if (selected) {
            int borderSize = 2;
            int innerGap = 3; // Gap between border and background fill

            // Colors change when pressed (inverted highlights like launch button)
            Color bgColor = pressed ? new Color(40, 40, 40, 220) : BACKGROUND_SELECTED;
            Color highlightColor = pressed ? new Color(20, 20, 20, 180) : new Color(120, 120, 120, 180);
            Color shadowColor = pressed ? new Color(100, 100, 100, 180) : new Color(20, 20, 20, 180);

            if (isCollapsed) {
                // Square border around icon only when collapsed (consistent padding on all sides)
                int outerPadding = 8; // Padding around icon to border
                int boxSize = ICON_SIZE + outerPadding * 2;
                // Center the box in the cell with equal margins
                int boxX = (width - boxSize) / 2;
                int boxY = (height - boxSize) / 2;

                // Fill background (with gap from border)
                int bgInset = borderSize + innerGap;
                g2d.setColor(bgColor);
                g2d.fillRect(boxX + bgInset, boxY + bgInset, boxSize - bgInset * 2, boxSize - bgInset * 2);

                // Dark outer edge (dark gray/black)
                g2d.setColor(new Color(30, 30, 30));
                g2d.fillRect(boxX, boxY, boxSize, borderSize); // Top
                g2d.fillRect(boxX, boxY + boxSize - borderSize, boxSize, borderSize); // Bottom
                g2d.fillRect(boxX, boxY, borderSize, boxSize); // Left
                g2d.fillRect(boxX + boxSize - borderSize, boxY, borderSize, boxSize); // Right

                // Inner highlight (top-left - swaps with shadow when pressed)
                g2d.setColor(highlightColor);
                g2d.fillRect(boxX + borderSize, boxY + borderSize, boxSize - borderSize * 2, 1);
                g2d.fillRect(boxX + borderSize, boxY + borderSize, 1, boxSize - borderSize * 2);

                // Inner shadow (bottom-right - swaps with highlight when pressed)
                g2d.setColor(shadowColor);
                g2d.fillRect(boxX + borderSize, boxY + boxSize - borderSize - 1, boxSize - borderSize * 2, 1);
                g2d.fillRect(boxX + boxSize - borderSize - 1, boxY + borderSize, 1, boxSize - borderSize * 2);
            } else {
                // Rectangle border around entire row when expanded (more padding)
                int boxX = 2;
                int boxY = 1;
                int boxWidth = width - 4;
                int boxHeight = height - 2;

                // Fill background (with gap from border)
                int bgInset = borderSize + innerGap;
                g2d.setColor(bgColor);
                g2d.fillRect(boxX + bgInset, boxY + bgInset, boxWidth - bgInset * 2, boxHeight - bgInset * 2);

                // Dark outer edge (dark gray/black)
                g2d.setColor(new Color(30, 30, 30));
                g2d.fillRect(boxX, boxY, boxWidth, borderSize); // Top
                g2d.fillRect(boxX, boxY + boxHeight - borderSize, boxWidth, borderSize); // Bottom
                g2d.fillRect(boxX, boxY, borderSize, boxHeight); // Left
                g2d.fillRect(boxX + boxWidth - borderSize, boxY, borderSize, boxHeight); // Right

                // Inner highlight (top-left - swaps with shadow when pressed)
                g2d.setColor(highlightColor);
                g2d.fillRect(boxX + borderSize, boxY + borderSize, boxWidth - borderSize * 2, 1);
                g2d.fillRect(boxX + borderSize, boxY + borderSize, 1, boxHeight - borderSize * 2);

                // Inner shadow (bottom-right - swaps with highlight when pressed)
                g2d.setColor(shadowColor);
                g2d.fillRect(boxX + borderSize, boxY + boxHeight - borderSize - 1, boxWidth - borderSize * 2, 1);
                g2d.fillRect(boxX + boxWidth - borderSize - 1, boxY + borderSize, 1, boxHeight - borderSize * 2);
            }
        }

        // Draw icon
        if (icon != null) {
            BufferedImage scaledIcon = icon;
            if (icon.getWidth() != ICON_SIZE || icon.getHeight() != ICON_SIZE) {
                scaledIcon = scaleImage(icon, ICON_SIZE, ICON_SIZE);
            }
            g2d.drawImage(scaledIcon, iconX, iconY, null);
        }

        // Only draw text when expanded
        if (!isCollapsed) {
            int textX = PADDING + ICON_SIZE + PADDING;
            int textAreaWidth = width - textX - PADDING;

            // Title - wrap to max 2 lines
            g2d.setFont(titleFont);
            g2d.setColor(TEXT_PRIMARY);
            FontMetrics titleFm = g2d.getFontMetrics();
            int lineHeight = titleFm.getHeight();

            // Calculate wrapped lines (max 2)
            String[] titleLines = wrapText(title, textAreaWidth, titleFm, 2);

            // Status font metrics for positioning
            g2d.setFont(statusFont);
            FontMetrics statusFm = g2d.getFontMetrics();

            // Calculate vertical positioning - minimal spacing (just newline)
            int titleHeight = titleLines.length * lineHeight;
            int totalTextHeight = titleHeight + statusFm.getAscent(); // Status directly after title
            int startY = (height - totalTextHeight) / 2 + titleFm.getAscent();

            // Draw title lines
            g2d.setFont(titleFont);
            g2d.setColor(TEXT_PRIMARY);
            for (int i = 0; i < titleLines.length; i++) {
                g2d.drawString(titleLines[i], textX, startY + (i * lineHeight));
            }

            // Status (no spacing - directly touching title)
            g2d.setFont(statusFont);
            g2d.setColor(TEXT_SECONDARY);
            // Position status right at the descent of the last title line (no gap)
            int statusY = startY + ((titleLines.length - 1) * lineHeight) + titleFm.getDescent() + statusFm.getAscent();
            String displayStatus = truncateText(status, textAreaWidth, statusFm);
            g2d.drawString(displayStatus, textX, statusY);
        }

        g2d.dispose();
    }

    private String truncateText(String text, int maxWidth, FontMetrics fm) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        int availableWidth = maxWidth - ellipsisWidth;
        if (availableWidth <= 0) {
            return ellipsis;
        }
        StringBuilder truncated = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (fm.stringWidth(truncated.toString() + c) > availableWidth) {
                break;
            }
            truncated.append(c);
        }
        return truncated.toString() + ellipsis;
    }

    /**
     * Wrap text to multiple lines (max specified), truncating last line with ellipsis if needed.
     */
    private String[] wrapText(String text, int maxWidth, FontMetrics fm, int maxLines) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }
        if (fm.stringWidth(text) <= maxWidth) {
            return new String[]{text};
        }

        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;

            if (fm.stringWidth(testLine) <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                // Word doesn't fit on current line
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // Single word too long - force it on a line (will be truncated later)
                    currentLine.append(word);
                }

                // Check if we've hit max lines
                if (lines.size() >= maxLines) {
                    break;
                }
            }
        }

        // Add remaining text
        if (currentLine.length() > 0 && lines.size() < maxLines) {
            lines.add(currentLine.toString());
        }

        // If we have more content but hit max lines, truncate last line with ellipsis
        if (lines.size() == maxLines) {
            String lastLine = lines.get(maxLines - 1);
            // Check if there's more content that didn't fit
            String allText = String.join(" ", lines);
            if (!allText.equals(text)) {
                lines.set(maxLines - 1, truncateText(lastLine + "...", maxWidth, fm).replace("......", "..."));
            } else if (fm.stringWidth(lastLine) > maxWidth) {
                lines.set(maxLines - 1, truncateText(lastLine, maxWidth, fm));
            }
        }

        // Truncate any line that's still too long
        for (int i = 0; i < lines.size(); i++) {
            if (fm.stringWidth(lines.get(i)) > maxWidth) {
                lines.set(i, truncateText(lines.get(i), maxWidth, fm));
            }
        }

        return lines.toArray(new String[0]);
    }

    public void clearCache() {
        iconCache.clear();
    }

    public static int getRowHeight() {
        return ROW_HEIGHT;
    }

    /**
     * Set the currently pressed row for pressed effect rendering.
     * @param row the row being pressed, or -1 for none
     */
    public static void setPressedRow(int row) {
        pressedRow = row;
    }

    /**
     * Get the currently pressed row.
     * @return the pressed row, or -1 if none
     */
    public static int getPressedRow() {
        return pressedRow;
    }
}
