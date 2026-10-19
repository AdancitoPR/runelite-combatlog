package com.combatlog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.util.List;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class CombatLogPanel extends PluginPanel
{
	private static final int TARGET_ROWS = 6;
	private static final Color ROW_ALT = new Color(46, 46, 46);
	private static final Color VALUE = new Color(230, 230, 230);

	private final CombatLog combatLog;
	private final CombatLogConfig config;
	private final MonsterIcons monsterIcons;
	private final WeaponIcons weaponIcons;

	private final JLabel damageValue = new JLabel("\u2014");
	private final JLabel rateValue = new JLabel("\u2014");
	private final JLabel detailValue = new JLabel("\u2014");
	private final JPanel targetsPanel = new JPanel();
	private final JPanel hitsPanel = new JPanel();
	private final JLabel emptyLabel = new JLabel();
	private final JPanel body = new JPanel();
	private final JButton clearButton = new JButton("Clear");
	private final JButton copyButton = new JButton("Copy");

	private boolean clearArmed;
	private Timer clearTimer;
	private Timer copyTimer;

	@Inject
	CombatLogPanel(CombatLog combatLog, CombatLogConfig config, MonsterIcons monsterIcons, WeaponIcons weaponIcons)
	{
		super(true);
		this.combatLog = combatLog;
		this.config = config;
		this.monsterIcons = monsterIcons;
		this.weaponIcons = weaponIcons;
		monsterIcons.onLoaded(this::refresh);
		weaponIcons.onLoaded(this::refresh);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		applyScrollChrome();

		damageValue.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		damageValue.setForeground(VALUE);
		rateValue.setFont(FontManager.getRunescapeSmallFont());
		rateValue.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		detailValue.setFont(FontManager.getRunescapeSmallFont());
		detailValue.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel stats = new JPanel();
		stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));
		stats.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		stats.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(8, 8, 8, 8)));
		stats.add(damageValue);
		stats.add(Box.createVerticalStrut(2));
		stats.add(rateValue);
		stats.add(Box.createVerticalStrut(2));
		stats.add(detailValue);
		add(stats, BorderLayout.NORTH);

		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.setBorder(new EmptyBorder(8, 8, 8, 8));

		targetsPanel.setLayout(new BoxLayout(targetsPanel, BoxLayout.Y_AXIS));
		targetsPanel.setOpaque(true);
		targetsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		targetsPanel.setAlignmentX(LEFT_ALIGNMENT);
		body.add(targetsPanel);

		emptyLabel.setFont(FontManager.getRunescapeSmallFont());
		emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		emptyLabel.setAlignmentX(LEFT_ALIGNMENT);
		emptyLabel.setOpaque(true);
		emptyLabel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		emptyLabel.setVerticalAlignment(SwingConstants.TOP);
		emptyLabel.setText("<html><body style='width:190px'>No hits yet. Attack something and the blue hitsplats you land will show up here."
			+ "<br><br>Poison, venom, burn, and other players' hits stay off this log. The game does not mark those splats as yours.</body></html>");
		int emptyHeight = emptyLabel.getPreferredSize().height;
		emptyLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, emptyHeight));
		body.add(emptyLabel);

		hitsPanel.setLayout(new BoxLayout(hitsPanel, BoxLayout.Y_AXIS));
		hitsPanel.setOpaque(true);
		hitsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		hitsPanel.setAlignmentX(LEFT_ALIGNMENT);
		body.add(hitsPanel);
		add(body, BorderLayout.CENTER);

		add(buildButtons(), BorderLayout.SOUTH);
		refresh();
	}

	void refresh()
	{
		disarmClear();
		CombatSnapshot snapshot = combatLog.summarize(System.currentTimeMillis(), config.recentSeconds());
		JScrollPane scrollPane = getScrollPane();
		int previousHeight = getPreferredSize().height;
		int scrollValue = scrollPane.getVerticalScrollBar().getValue();
		boolean pinnedToTop = scrollValue <= 0;

		if (snapshot.getDamageHits() == 0 && snapshot.getBlocks() == 0)
		{
			damageValue.setText("\u2014");
			rateValue.setText("Waiting for a hit");
			detailValue.setText(" ");
		}
		else if (snapshot.getDamageHits() == 0)
		{
			damageValue.setText("0");
			rateValue.setText("\u2014 dps  \u00b7  \u2014 recent");
			detailValue.setText(snapshot.getBlocks() == 1 ? "1 block" : snapshot.getBlocks() + " blocks");
		}
		else
		{
			damageValue.setText(CombatLogFormatter.formatDamage(snapshot.getTotalDamage()));
			rateValue.setText(CombatLogFormatter.formatRate(snapshot.getSessionDps()) + " dps  \u00b7  "
				+ CombatLogFormatter.formatRate(snapshot.getRecentDps()) + " recent");
			detailValue.setText(snapshot.getDamageHits() + (snapshot.getDamageHits() == 1 ? " hit" : " hits")
				+ "  \u00b7  max " + snapshot.getMaxHit()
				+ "  \u00b7  avg " + CombatLogFormatter.formatAverage(snapshot.getAverageHit()));
		}

		rebuildTargets(snapshot);
		rebuildHits(snapshot);
		emptyLabel.setVisible(snapshot.isEmpty());
		boolean hasRows = !snapshot.isEmpty();
		copyButton.setEnabled(hasRows);
		clearButton.setEnabled(hasRows);

		revalidate();
		if (pinnedToTop)
		{
			scrollPane.getVerticalScrollBar().setValue(0);
		}
		else
		{
			int growth = getPreferredSize().height - previousHeight;
			scrollPane.getVerticalScrollBar().setValue(scrollValue + Math.max(0, growth));
		}
		repaint();
		applyScrollChrome();
		SwingUtilities.invokeLater(this::applyScrollChrome);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		super.paintComponent(g);
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		applyScrollChrome();
	}

	private void applyScrollChrome()
	{
		JScrollPane scrollPane = getScrollPane();
		if (scrollPane == null)
		{
			return;
		}

		scrollPane.setBorder(null);
		scrollPane.setOpaque(true);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		JViewport viewport = scrollPane.getViewport();
		viewport.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		viewport.setOpaque(true);
		viewport.setBackground(ColorScheme.DARK_GRAY_COLOR);

		Component view = viewport.getView();
		if (view instanceof JComponent && view != this)
		{
			JComponent scrollView = (JComponent) view;
			scrollView.setOpaque(true);
			scrollView.setBackground(ColorScheme.DARK_GRAY_COLOR);
		}

		JPanel wrapped = getWrappedPanel();
		if (wrapped != null && wrapped != this)
		{
			wrapped.setOpaque(true);
			wrapped.setBackground(ColorScheme.DARK_GRAY_COLOR);
		}
		repaint();
	}

	private JPanel buildButtons()
	{
		JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
		buttons.setBorder(new EmptyBorder(6, 8, 8, 8));
		styleButton(clearButton);
		styleButton(copyButton);
		clearButton.addActionListener(event -> onClear());
		copyButton.addActionListener(event -> onCopy());
		buttons.add(clearButton);
		buttons.add(copyButton);
		return buttons;
	}

	private void rebuildTargets(CombatSnapshot snapshot)
	{
		targetsPanel.removeAll();
		if (snapshot.getTargets().isEmpty())
		{
			targetsPanel.setVisible(false);
			return;
		}

		targetsPanel.setVisible(true);
		targetsPanel.add(sectionLabel("Targets"));
		targetsPanel.add(Box.createVerticalStrut(4));

		int shown = 0;
		for (TargetTotal target : snapshot.getTargets())
		{
			if (shown >= TARGET_ROWS)
			{
				break;
			}
			shown++;
			JPanel row = new JPanel(new BorderLayout(8, 0));
			row.setOpaque(false);
			row.setAlignmentX(LEFT_ALIGNMENT);

			String name = target.getName();
			if (target.getKind() == TargetKind.PLAYER)
			{
				name = name + " (player)";
			}
			JLabel nameLabel = new JLabel(name);
			nameLabel.setFont(FontManager.getRunescapeSmallFont());
			nameLabel.setForeground(target.getKind() == TargetKind.PLAYER ? CombatLogOverlay.PLAYER_COLOR : Color.WHITE);
			nameLabel.setMinimumSize(new Dimension(0, nameLabel.getPreferredSize().height));

			JLabel damageLabel = new JLabel(CombatLogFormatter.formatDamage(target.getDamage()), SwingConstants.RIGHT);
			damageLabel.setFont(FontManager.getRunescapeSmallFont());
			damageLabel.setForeground(CombatLogOverlay.HIT_COLOR);
			holdWidth(damageLabel);

			row.add(nameLabel, BorderLayout.CENTER);
			row.add(damageLabel, BorderLayout.EAST);
			row.setToolTipText(target.getHits() + (target.getHits() == 1 ? " hit" : " hits")
				+ ", max " + target.getMaxHit()
				+ (target.getBlocks() > 0 ? ", " + target.getBlocks() + (target.getBlocks() == 1 ? " block" : " blocks") : ""));
			fitRow(row);
			targetsPanel.add(row);
		}

		int hidden = snapshot.getTargets().size() - shown;
		if (hidden > 0)
		{
			JLabel more = new JLabel(hidden + " more");
			more.setFont(FontManager.getRunescapeSmallFont());
			more.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			more.setAlignmentX(LEFT_ALIGNMENT);
			targetsPanel.add(Box.createVerticalStrut(2));
			targetsPanel.add(more);
		}
		targetsPanel.add(Box.createVerticalStrut(10));
	}

	private void rebuildHits(CombatSnapshot snapshot)
	{
		hitsPanel.removeAll();
		if (snapshot.isEmpty())
		{
			return;
		}

		hitsPanel.add(sectionLabel("Hits"));
		hitsPanel.add(Box.createVerticalStrut(4));

		ZoneId zone = ZoneId.systemDefault();
		int index = 0;
		boolean firstGroup = true;
		for (List<HitRecord> group : FightGroups.group(snapshot.getHits()))
		{
			if (!firstGroup)
			{
				hitsPanel.add(groupSeparator());
			}
			firstGroup = false;
			for (HitRecord hit : group)
			{
				JPanel row = new JPanel(new BorderLayout(6, 0));
				row.setBackground(index % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : ROW_ALT);
				row.setBorder(new EmptyBorder(3, 6, 3, 6));
				row.setAlignmentX(LEFT_ALIGNMENT);

				JLabel icon = monsterIcon(hit);

				JLabel name = new JLabel(hit.getTargetName());
				name.setFont(FontManager.getRunescapeSmallFont());
				name.setForeground(hit.getTargetKind() == TargetKind.PLAYER ? CombatLogOverlay.PLAYER_COLOR : Color.WHITE);
				name.setMinimumSize(new Dimension(0, name.getPreferredSize().height));

				JLabel amount = new JLabel(CombatLogFormatter.amountLabel(hit), SwingConstants.RIGHT);
				amount.setFont(FontManager.getRunescapeSmallFont());
				amount.setForeground(CombatLogOverlay.amountColor(hit));
				holdWidth(amount);

				JPanel nameAndIcon = new JPanel(new BorderLayout(6, 0));
				nameAndIcon.setOpaque(false);
				nameAndIcon.add(icon, BorderLayout.WEST);
				nameAndIcon.add(name, BorderLayout.CENTER);

				JPanel damage = new JPanel(new BorderLayout(4, 0));
				damage.setOpaque(false);
				JLabel weapon = weaponIcon(hit);
				if (weapon != null)
				{
					damage.add(weapon, BorderLayout.WEST);
				}
				damage.add(amount, BorderLayout.EAST);

				row.add(nameAndIcon, BorderLayout.CENTER);
				row.add(damage, BorderLayout.EAST);
				row.setToolTipText(CombatLogFormatter.formatTime(hit.getTimeMillis(), zone) + "  " + hit.describe());
				fitRow(row);
				hitsPanel.add(row);
				index++;
			}
		}
	}

	private static JPanel groupSeparator()
	{
		JPanel line = new JPanel();
		line.setOpaque(false);
		line.setAlignmentX(LEFT_ALIGNMENT);
		line.setBorder(BorderFactory.createCompoundBorder(
			new EmptyBorder(3, 0, 3, 0),
			BorderFactory.createMatteBorder(2, 0, 0, 0, CombatLogOverlay.GROUP_LINE)));
		fitRow(line);
		return line;
	}

	private void onClear()
	{
		if (!clearArmed)
		{
			clearArmed = true;
			clearButton.setText("Confirm");
			clearButton.setBackground(new Color(122, 42, 42));
			if (clearTimer != null)
			{
				clearTimer.stop();
			}
			clearTimer = new Timer(2000, event -> disarmClear());
			clearTimer.setRepeats(false);
			clearTimer.start();
			return;
		}

		disarmClear();
		combatLog.clear();
		refresh();
	}

	private void disarmClear()
	{
		clearArmed = false;
		clearButton.setText("Clear");
		clearButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		if (clearTimer != null)
		{
			clearTimer.stop();
			clearTimer = null;
		}
	}

	private void onCopy()
	{
		CombatSnapshot snapshot = combatLog.summarize(System.currentTimeMillis(), config.recentSeconds());
		if (snapshot.isEmpty())
		{
			return;
		}
		String text = CombatLogFormatter.toClipboardText(snapshot, ZoneId.systemDefault());
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
		copyButton.setText("Copied");
		if (copyTimer != null)
		{
			copyTimer.stop();
		}
		copyTimer = new Timer(1200, event -> copyButton.setText("Copy"));
		copyTimer.setRepeats(false);
		copyTimer.start();
	}

	private JLabel monsterIcon(HitRecord hit)
	{
		JLabel icon = new JLabel();
		icon.setOpaque(false);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		Dimension size = new Dimension(MonsterIcons.SIZE, MonsterIcons.SIZE);
		icon.setPreferredSize(size);
		icon.setMinimumSize(size);
		icon.setMaximumSize(size);
		if (hit.getNpcId() != null)
		{
			BufferedImage image = monsterIcons.get(hit.getNpcId());
			if (image != null)
			{
				icon.setIcon(new ImageIcon(image));
			}
		}
		return icon;
	}

	private JLabel weaponIcon(HitRecord hit)
	{
		Integer itemId = hit.getWeaponItemId();
		if (itemId == null)
		{
			return null;
		}
		BufferedImage image = weaponIcons.get(itemId);
		if (image == null)
		{
			return null;
		}
		JLabel icon = new JLabel(new ImageIcon(image));
		icon.setOpaque(false);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		return icon;
	}

	private static JLabel sectionLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static void holdWidth(JLabel label)
	{
		Dimension size = label.getPreferredSize();
		label.setMinimumSize(size);
		label.setPreferredSize(size);
	}

	private static void fitRow(JPanel row)
	{
		int height = row.getPreferredSize().height;
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
	}

	private static void styleButton(JButton button)
	{
		button.setFocusable(false);
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(Color.WHITE);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setBorder(new EmptyBorder(6, 8, 6, 8));
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent event)
			{
				if (button.isEnabled() && !"Confirm".equals(button.getText()))
				{
					button.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
				}
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				if (!"Confirm".equals(button.getText()))
				{
					button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				}
			}
		});
	}

}
