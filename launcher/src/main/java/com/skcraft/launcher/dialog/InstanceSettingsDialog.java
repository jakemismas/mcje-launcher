package com.skcraft.launcher.dialog;

import com.skcraft.launcher.Instance;
import com.skcraft.launcher.InstanceSettings;
import com.skcraft.launcher.dialog.component.BetterComboBox;
import com.skcraft.launcher.launch.MemorySettings;
import com.skcraft.launcher.launch.runtime.JavaRuntime;
import com.skcraft.launcher.launch.runtime.JavaRuntimeFinder;
import com.skcraft.launcher.persistence.Persistence;
import com.skcraft.launcher.swing.FormPanel;
import com.skcraft.launcher.swing.LinedBoxPanel;
import com.skcraft.launcher.util.SharedLocale;
import lombok.extern.java.Log;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

@Log
public class InstanceSettingsDialog extends JDialog {
	private final InstanceSettings settings;
	private final Instance instance;

	private final LinedBoxPanel formsPanel = new LinedBoxPanel(false);

	// Icon settings
	private final FormPanel iconPanel = new FormPanel();
	private final JLabel iconPreview = new JLabel();
	private final JButton changeIconButton = new JButton(SharedLocale.tr("instance.options.changeIcon", "Change Icon"));
	private final JButton removeIconButton = new JButton(SharedLocale.tr("instance.options.removeIcon", "Remove Icon"));

	private final FormPanel memorySettingsPanel = new FormPanel();
	private final JCheckBox enableMemorySettings = new JCheckBox(SharedLocale.tr("instance.options.customMemory"));
	private final JSpinner minMemorySpinner = new JSpinner();
	private final JSpinner maxMemorySpinner = new JSpinner();

	private final JCheckBox enableCustomRuntime = new JCheckBox(SharedLocale.tr("instance.options.customJava"));
	private final FormPanel runtimePanel = new FormPanel();
	private final JComboBox<JavaRuntime> javaRuntimeBox = new BetterComboBox<>();
	private final JTextField javaArgsBox = new JTextField();

	private final LinedBoxPanel buttonsPanel = new LinedBoxPanel(true);
	private final JButton okButton = new JButton(SharedLocale.tr("button.save"));
	private final JButton cancelButton = new JButton(SharedLocale.tr("button.cancel"));

	private boolean saved = false;

	public InstanceSettingsDialog(Window owner, Instance instance) {
		super(owner);
		this.instance = instance;
		this.settings = instance.getSettings();

		setTitle(SharedLocale.tr("instance.options.title"));
		setModalityType(DEFAULT_MODALITY_TYPE);
		initComponents();
		setSize(new Dimension(400, 550));
		setLocationRelativeTo(owner);
	}

	private void initComponents() {
		// Icon panel
		iconPreview.setPreferredSize(new Dimension(64, 64));
		iconPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		iconPreview.setHorizontalAlignment(SwingConstants.CENTER);
		updateIconPreview();

		JPanel iconButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		iconButtonPanel.add(changeIconButton);
		iconButtonPanel.add(removeIconButton);

		iconPanel.addRow(new JLabel(SharedLocale.tr("instance.options.icon", "Modpack Icon:")), iconPreview);
		iconPanel.addRow(new JLabel(), iconButtonPanel);

		changeIconButton.addActionListener(e -> browseForIcon());
		removeIconButton.addActionListener(e -> removeIcon());

		memorySettingsPanel.addRow(enableMemorySettings);
		memorySettingsPanel.addRow(new JLabel(SharedLocale.tr("options.minMemory")), minMemorySpinner);
		memorySettingsPanel.addRow(new JLabel(SharedLocale.tr("options.maxMemory")), maxMemorySpinner);

		// TODO: Do we keep this list centrally somewhere? Or is actively refreshing good?
		JavaRuntime[] javaRuntimes = JavaRuntimeFinder.getAvailableRuntimes().toArray(new JavaRuntime[0]);
		javaRuntimeBox.setModel(new DefaultComboBoxModel<>(javaRuntimes));

		runtimePanel.addRow(enableCustomRuntime);
		runtimePanel.addRow(new JLabel(SharedLocale.tr("options.jvmRuntime")), javaRuntimeBox);
		runtimePanel.addRow(new JLabel(SharedLocale.tr("options.jvmArguments")), javaArgsBox);

		okButton.setMargin(new Insets(0, 10, 0, 10));
		buttonsPanel.addGlue();
		buttonsPanel.addElement(okButton);
		buttonsPanel.addElement(cancelButton);

		enableMemorySettings.addActionListener(e -> {
			if (enableMemorySettings.isSelected()) {
				settings.setMemorySettings(new MemorySettings());
			} else {
				settings.setMemorySettings(null);
			}

			updateComponents();
		});

		enableCustomRuntime.addActionListener(e -> {
			runtimePanel.setEnabled(enableCustomRuntime.isSelected());
		});

		okButton.addActionListener(e -> {
			save();
			dispose();
		});

		cancelButton.addActionListener(e -> dispose());

		formsPanel.addElement(iconPanel);
		formsPanel.addElement(memorySettingsPanel);
		formsPanel.addElement(runtimePanel);

		add(formsPanel, BorderLayout.NORTH);
		add(buttonsPanel, BorderLayout.SOUTH);

		updateComponents();
	}

	private void updateComponents() {
		if (settings.getMemorySettings() != null) {
			memorySettingsPanel.setEnabled(true);
			enableMemorySettings.setSelected(true);

			minMemorySpinner.setValue(settings.getMemorySettings().getMinMemory());
			maxMemorySpinner.setValue(settings.getMemorySettings().getMaxMemory());
		} else {
			memorySettingsPanel.setEnabled(false);
			enableMemorySettings.setSelected(false);
		}

		if (settings.getRuntime() != null) {
			runtimePanel.setEnabled(true);
			enableCustomRuntime.setSelected(true);
		} else {
			runtimePanel.setEnabled(false);
			enableCustomRuntime.setSelected(false);
		}

		javaRuntimeBox.setSelectedItem(settings.getRuntime());
		javaArgsBox.setText(settings.getCustomJvmArgs());
	}

	private void save() {
		if (enableMemorySettings.isSelected()) {
			MemorySettings memorySettings = settings.getMemorySettings();

			memorySettings.setMinMemory((int) minMemorySpinner.getValue());
			memorySettings.setMaxMemory((int) maxMemorySpinner.getValue());
		} else {
			settings.setMemorySettings(null);
		}

		if (enableCustomRuntime.isSelected()) {
			settings.setRuntime((JavaRuntime) javaRuntimeBox.getSelectedItem());
			settings.setCustomJvmArgs(javaArgsBox.getText());
		} else {
			settings.setRuntime(null);
			settings.setCustomJvmArgs(null);
		}

		saved = true;
	}

	private void updateIconPreview() {
		File iconFile = instance.getIconPath();
		if (iconFile.exists()) {
			try {
				BufferedImage img = ImageIO.read(iconFile);
				if (img != null) {
					Image scaled = img.getScaledInstance(64, 64, Image.SCALE_SMOOTH);
					iconPreview.setIcon(new ImageIcon(scaled));
					iconPreview.setText(null);
					removeIconButton.setEnabled(true);
					return;
				}
			} catch (IOException e) {
				log.log(Level.WARNING, "Failed to load icon preview", e);
			}
		}
		iconPreview.setIcon(null);
		iconPreview.setText("No icon");
		removeIconButton.setEnabled(false);
	}

	private void browseForIcon() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(SharedLocale.tr("instance.options.selectIcon", "Select Icon Image"));
		chooser.setFileFilter(new FileNameExtensionFilter("Images (PNG, JPG)", "png", "jpg", "jpeg"));

		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File source = chooser.getSelectedFile();
			try {
				BufferedImage img = ImageIO.read(source);
				if (img == null) {
					JOptionPane.showMessageDialog(this, "Could not read image file.",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Scale to 64x64
				BufferedImage scaled = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = scaled.createGraphics();
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2d.drawImage(img, 0, 0, 64, 64, null);
				g2d.dispose();

				// Save to instance directory
				File dest = instance.getIconPath();
				ImageIO.write(scaled, "PNG", dest);

				updateIconPreview();
			} catch (IOException e) {
				log.log(Level.WARNING, "Failed to save icon", e);
				JOptionPane.showMessageDialog(this, "Failed to save icon: " + e.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void removeIcon() {
		File iconFile = instance.getIconPath();
		if (iconFile.exists()) {
			if (iconFile.delete()) {
				updateIconPreview();
			} else {
				JOptionPane.showMessageDialog(this, "Failed to delete icon file.",
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public static boolean open(Window parent, Instance instance) {
		InstanceSettingsDialog dialog = new InstanceSettingsDialog(parent, instance);
		dialog.setVisible(true);

		if (dialog.saved) {
			Persistence.commitAndForget(instance);
		}

		return dialog.saved;
	}
}
