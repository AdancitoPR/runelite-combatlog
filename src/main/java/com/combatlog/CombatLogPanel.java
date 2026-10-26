package com.combatlog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.plaf.ScrollBarUI;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.laf.RuneLiteLAF;
import net.runelite.client.ui.laf.RuneLiteScrollBarUI;

class CombatLogPanel extends PluginPanel
{
	private static final Color ROW_ALT = new Color(46, 46, 46);
	private static final Color VALUE = new Color(230, 230, 230);
	private static final Color CHOICE_IDLE = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color CHOICE_HOVER = ColorScheme.DARKER_GRAY_HOVER_COLOR;
	private static final Color CHOICE_PRESSED = new Color(24, 24, 24);
	private static final Color CHOICE_SELECTED = new Color(68, 68, 68);
	private static final Color CHOICE_SELECTED_HOVER = new Color(86, 86, 86);
	private static final Color CHOICE_SELECTED_PRESSED = new Color(50, 50, 50);
	/** Padding around the empty state and the fight notes. */
	private static final int TEXT_INSET = 8;
	/**
	 * Room for wrapping copy inside the sidebar, after the plugin border and {@link #TEXT_INSET}.
	 * The scrollbar sits beside this width.
	 */
	private static final int SIDEBAR_TEXT_ROOM = PluginPanel.PANEL_WIDTH
		- (PluginPanel.BORDER_OFFSET + TEXT_INSET) * 2;
	/**
	 * Swing's HTML renderer maps a CSS pixel to 1.3 layout pixels. The wrap width is the
	 * sidebar room divided by that, or the line runs under the scrollbar.
	 */
	private static final int WRAPPED_TEXT_WIDTH = (SIDEBAR_TEXT_ROOM - 8) * 10 / 13;

	private final CombatLog combatLog;
	private final CombatLogConfig config;
	private final MonsterIcons monsterIcons;
	private final WeaponIcons weaponIcons;
	private final TabIcons tabIcons;
	private final StyleIcons styleIcons;
	private final PrayerIcons prayerIcons;
	private final GameHitsplats gameHitsplats;
	private ConfigManager configManager;
	private DamageView selectedView;
	private final Map<Long, Boolean> expanded = new HashMap<>();
	private final Map<String, Boolean> groupsOpen = new HashMap<>();
	/** Newest fights the last rebuild kept open. Manual toggles reset when this set changes. */
	private Set<Long> appliedOpenFights = new HashSet<>();
	/** Groups that held one of those fights. A group the player opened closes once it no longer does. */
	private Set<String> appliedOpenGroups = new HashSet<>();

	private final JPanel totalsBox = new JPanel();
	private final JLabel detailValue = new JLabel("\u2014");
	private final JPanel hitsPanel = new JPanel();
	private final JLabel emptyLabel = new JLabel();
	private final JPanel body = new JPanel();
	private final JButton clearButton = new JButton("Clear");
	private final JButton copyButton = new JButton("Copy");

	private final JButton outgoingButton = new JButton();
	private final JButton incomingButton = new JButton();
	private final JButton bothButton = new JButton();

	private boolean clearArmed;
	private Timer clearTimer;
	private Timer copyTimer;

	CombatLogPanel(CombatLog combatLog, CombatLogConfig config, MonsterIcons monsterIcons, WeaponIcons weaponIcons)
	{
		this(combatLog, config, monsterIcons, weaponIcons, null, null, null, null);
	}

	CombatLogPanel(CombatLog combatLog, CombatLogConfig config, MonsterIcons monsterIcons, WeaponIcons weaponIcons, TabIcons tabIcons, StyleIcons styleIcons, PrayerIcons prayerIcons, GameHitsplats gameHitsplats)
	{
		super(true);
		this.combatLog = combatLog;
		this.config = config;
		this.monsterIcons = monsterIcons;
		this.weaponIcons = weaponIcons;
		this.tabIcons = tabIcons;
		this.styleIcons = styleIcons;
		this.prayerIcons = prayerIcons;
		this.gameHitsplats = gameHitsplats;
		monsterIcons.onLoaded(this::refresh);
		weaponIcons.onLoaded(this::refresh);
		if (tabIcons != null)
		{
			tabIcons.onLoaded(this::refresh);
		}
		if (styleIcons != null)
		{
			styleIcons.onLoaded(this::refresh);
		}
		if (prayerIcons != null)
		{
			prayerIcons.onLoaded(this::refresh);
		}
		if (gameHitsplats != null)
		{
			gameHitsplats.onLoaded(this::refresh);
		}

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		applyScrollChrome();

		totalsBox.setLayout(new BoxLayout(totalsBox, BoxLayout.Y_AXIS));
		totalsBox.setOpaque(false);
		totalsBox.setAlignmentX(LEFT_ALIGNMENT);
		detailValue.setFont(uiFont());
		detailValue.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		detailValue.setAlignmentX(LEFT_ALIGNMENT);

		JPanel stats = new JPanel();
		stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));
		stats.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		stats.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(8, 8, 8, 8)));
		stats.add(totalsBox);
		stats.add(Box.createVerticalStrut(2));
		stats.add(detailValue);
		add(buildNorth(stats), BorderLayout.NORTH);

		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.setBorder(new EmptyBorder(TEXT_INSET, TEXT_INSET, TEXT_INSET, TEXT_INSET));

		emptyLabel.setFont(uiFont());
		emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		emptyLabel.setAlignmentX(LEFT_ALIGNMENT);
		emptyLabel.setOpaque(true);
		emptyLabel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		emptyLabel.setVerticalAlignment(SwingConstants.TOP);
		emptyLabel.setText("<html><body style='width:" + WRAPPED_TEXT_WIDTH + "px'>No hits yet. Attack something and the damage you deal shows up here in red."
			+ "<br><br>Damage you take is logged too, including poison, venom, burn, and bleed. Other players' hits stay off this log.</body></html>");
		emptyLabel.setMaximumSize(emptyLabel.getPreferredSize());
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

	@Inject
	CombatLogPanel(CombatLog combatLog, CombatLogConfig config, MonsterIcons monsterIcons, WeaponIcons weaponIcons, TabIcons tabIcons, StyleIcons styleIcons, PrayerIcons prayerIcons, GameHitsplats gameHitsplats, ConfigManager configManager)
	{
		this(combatLog, config, monsterIcons, weaponIcons, tabIcons, styleIcons, prayerIcons, gameHitsplats);
		this.configManager = configManager;
	}

	void setDamageView(DamageView view)
	{
		applyView(view == null ? DamageView.OUTGOING : view, false);
	}

	private JPanel buildNorth(JPanel stats)
	{
		styleChoice(outgoingButton, DamageView.OUTGOING, "Outgoing");
		styleChoice(incomingButton, DamageView.INCOMING, "Incoming");
		styleChoice(bothButton, DamageView.BOTH, "Both");
		applyTabIcons();
		applyView(configuredView(), false);

		JPanel choices = new JPanel(new GridLayout(1, 3, 4, 0));
		choices.setBackground(ColorScheme.DARK_GRAY_COLOR);
		choices.setBorder(new EmptyBorder(0, 8, 8, 8));
		choices.add(outgoingButton);
		choices.add(incomingButton);
		choices.add(bothButton);

		JPanel north = new JPanel(new BorderLayout());
		north.setBackground(ColorScheme.DARK_GRAY_COLOR);
		north.add(stats, BorderLayout.NORTH);
		north.add(choices, BorderLayout.SOUTH);
		return north;
	}

	private void applyTabIcons()
	{
		setTabIcon(outgoingButton, tabIcons == null ? null : tabIcons.sword());
		setTabIcon(incomingButton, tabIcons == null ? null : tabIcons.shield());
		setTabIcon(bothButton, tabIcons == null ? null : tabIcons.both());
	}

	private static void setTabIcon(JButton button, BufferedImage image)
	{
		if (image != null)
		{
			button.setIcon(new ImageIcon(image));
		}
	}

	private void styleChoice(JButton button, DamageView view, String tooltip)
	{
		button.setFocusable(false);
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBorderPainted(true);
		button.setRolloverEnabled(false);
		button.setFont(uiFont());
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setText("");
		button.setToolTipText(tooltip);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent event)
			{
				button.putClientProperty("hover", Boolean.TRUE);
				paintChoice(button, selectedView == view);
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				button.putClientProperty("hover", Boolean.FALSE);
				button.putClientProperty("pressed", Boolean.FALSE);
				paintChoice(button, selectedView == view);
			}

			@Override
			public void mousePressed(MouseEvent event)
			{
				button.putClientProperty("pressed", Boolean.TRUE);
				paintChoice(button, selectedView == view);
			}

			@Override
			public void mouseReleased(MouseEvent event)
			{
				button.putClientProperty("pressed", Boolean.FALSE);
				paintChoice(button, selectedView == view);
			}
		});
		button.addActionListener(event -> applyView(view, true));
	}

	private void paintChoice(JButton button, boolean selected)
	{
		boolean hover = Boolean.TRUE.equals(button.getClientProperty("hover"));
		boolean pressed = Boolean.TRUE.equals(button.getClientProperty("pressed"));
		Color fill;
		if (selected && pressed)
		{
			fill = CHOICE_SELECTED_PRESSED;
		}
		else if (selected && hover)
		{
			fill = CHOICE_SELECTED_HOVER;
		}
		else if (selected)
		{
			fill = CHOICE_SELECTED;
		}
		else if (pressed)
		{
			fill = CHOICE_PRESSED;
		}
		else if (hover)
		{
			fill = CHOICE_HOVER;
		}
		else
		{
			fill = CHOICE_IDLE;
		}
		button.setBackground(fill);
		button.setForeground(selected || hover ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 2, 0, selected ? ColorScheme.BRAND_ORANGE : fill),
			new EmptyBorder(5, 4, 3, 4)));
	}

	private void showWaiting()
	{
		totalsBox.removeAll();
		JLabel dash = new JLabel("\u2014");
		dash.setFont(boldFont(20f));
		dash.setForeground(VALUE);
		dash.setAlignmentX(LEFT_ALIGNMENT);
		totalsBox.add(dash);
	}

	private void showFightTotals(CombatSnapshot snapshot)
	{
		totalsBox.removeAll();
		boolean outgoing = snapshot.getView() != DamageView.INCOMING;
		boolean incoming = snapshot.getView() != DamageView.OUTGOING;
		if (outgoing)
		{
			addTotal("Damage done", CombatLogFormatter.formatDamage(snapshot.getTotalDamage()), DamageColors.OUTGOING);
			int thrall = CombatLogFormatter.thrallDamage(snapshot.getHits());
			if (thrall > 0)
			{
				addTotal("Thrall damage", CombatLogFormatter.formatDamage(thrall), Color.WHITE);
			}
		}
		if (incoming)
		{
			addTotal("Taken", CombatLogFormatter.formatDamage(snapshot.getDamageTaken()), DamageColors.INCOMING);
		}
	}

	private void addTotal(String label, String value, Color color)
	{
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		JLabel name = new JLabel(label);
		name.setFont(uiFont());
		name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JLabel amount = new JLabel(value, SwingConstants.RIGHT);
		amount.setFont(boldFont(18f));
		amount.setForeground(color);
		row.add(name, BorderLayout.WEST);
		row.add(amount, BorderLayout.EAST);
		totalsBox.add(row);
	}

	private void paintChoices()
	{
		paintChoice(outgoingButton, selectedView == DamageView.OUTGOING);
		paintChoice(incomingButton, selectedView == DamageView.INCOMING);
		paintChoice(bothButton, selectedView == DamageView.BOTH);
	}

	private DamageView configuredView()
	{
		if (selectedView != null)
		{
			return selectedView;
		}
		try
		{
			DamageView configured = config.damageView();
			return configured == null ? DamageView.OUTGOING : configured;
		}
		catch (RuntimeException ex)
		{
			return DamageView.OUTGOING;
		}
	}

	private DamageView damageView()
	{
		return configuredView();
	}

	private void applyView(DamageView view, boolean persist)
	{
		selectedView = view;
		paintChoices();
		if (persist && configManager != null)
		{
			configManager.setConfiguration(CombatLogConfig.GROUP, "damageView", view);
		}
		if (persist)
		{
			refresh();
		}
	}

	void refresh()
	{
		applyTabIcons();
		CombatSnapshot snapshot = combatLog.summarize(System.currentTimeMillis(), 10, damageView());
		JScrollPane scrollPane = getScrollPane();
		int previousHeight = getPreferredSize().height;
		int scrollValue = scrollPane.getVerticalScrollBar().getValue();
		boolean pinnedToTop = scrollValue <= 0;

		if (snapshot.getView() == DamageView.INCOMING)
		{
			int splashes = CombatLogFormatter.countSplashes(snapshot.getHits(), DamageDirection.INCOMING);
			if (snapshot.getIncomingHits() == 0 && splashes == 0)
			{
				showWaiting();
				detailValue.setText("Waiting for a hit");
			}
			else if (snapshot.getIncomingHits() == 0)
			{
				showFightTotals(snapshot);
				detailValue.setText(splashes == 1 ? "1 splash" : splashes + " splashes");
			}
			else
			{
				showFightTotals(snapshot);
				detailValue.setText(snapshot.getIncomingHits() + (snapshot.getIncomingHits() == 1 ? " hit" : " hits"));
			}
		}
		else if (snapshot.getView() == DamageView.BOTH)
		{
			if (snapshot.getDamageHits() == 0 && snapshot.getIncomingHits() == 0 && snapshot.getBlocks() == 0
				&& CombatLogFormatter.thrallDamage(snapshot.getHits()) == 0
				&& CombatLogFormatter.countSplashes(snapshot.getHits(), null) == 0)
			{
				showWaiting();
				detailValue.setText("Waiting for a hit");
			}
			else
			{
				showFightTotals(snapshot);
				detailValue.setText(snapshot.getDamageHits() + " dealt  \u00b7  " + snapshot.getIncomingHits() + " taken");
			}
		}
		else if (snapshot.getDamageHits() == 0 && snapshot.getBlocks() == 0 && snapshot.getDamageTaken() == 0
			&& CombatLogFormatter.thrallDamage(snapshot.getHits()) == 0)
		{
			showWaiting();
			detailValue.setText("Waiting for a hit");
		}
		else if (snapshot.getDamageHits() == 0 && CombatLogFormatter.thrallDamage(snapshot.getHits()) == 0)
		{
			showFightTotals(snapshot);
			detailValue.setText(missDetail(snapshot));
		}
		else
		{
			showFightTotals(snapshot);
			detailValue.setText(snapshot.getDamageHits() + (snapshot.getDamageHits() == 1 ? " hit" : " hits")
				+ "  \u00b7  max " + snapshot.getMaxHit()
				+ "  \u00b7  avg " + CombatLogFormatter.formatAverage(snapshot.getAverageHit()));
		}

		emptyLabel.setVisible(snapshot.isEmpty());
		boolean hasRows = !snapshot.isEmpty();
		copyButton.setEnabled(hasRows);
		clearButton.setEnabled(hasRows);
		rebuildFights(snapshot);

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
		useRuneLiteScrollBar(scrollPane.getVerticalScrollBar());
		useRuneLiteScrollBar(scrollPane.getHorizontalScrollBar());

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

	private static void useRuneLiteScrollBar(JScrollBar bar)
	{
		if (bar == null || !(UIManager.getLookAndFeel() instanceof RuneLiteLAF))
		{
			return;
		}
		bar.setUI((ScrollBarUI) RuneLiteScrollBarUI.createUI(bar));
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

	private void rebuildFights(CombatSnapshot snapshot)
	{
		hitsPanel.removeAll();
		if (snapshot.getFights().isEmpty())
		{
			return;
		}

		List<Fight> ordered = new ArrayList<>(snapshot.getFights());
		ordered.sort(CombatLogPanel::newerFight);

		Map<String, List<Fight>> groups = new LinkedHashMap<>();
		for (Fight fight : ordered)
		{
			if (!fight.isClosed() && !DoomDelve.named(fight.getName()) && !DoomDelve.isDoom(fight.getNpcId()))
			{
				continue;
			}
			String key = groupKey(fight);
			List<Fight> group = groups.get(key);
			if (group == null)
			{
				group = new ArrayList<>();
				groups.put(key, group);
			}
			group.add(fight);
		}

		hitsPanel.add(sectionLabel("Fights"));
		hitsPanel.add(Box.createVerticalStrut(4));

		int index = 0;
		Set<Long> autoOpen = autoOpenFights(ordered);
		reconcileExpanded(autoOpen);
		Set<String> autoGroups = new HashSet<>();
		for (List<Fight> group : groups.values())
		{
			for (Fight fight : group)
			{
				if (autoOpen.contains(fight.getId()))
				{
					autoGroups.add(groupKey(fight));
					break;
				}
			}
		}
		reconcileGroups(autoGroups);
		Set<String> shown = new HashSet<>();
		for (Fight fight : ordered)
		{
			String key = groupKey(fight);
			List<Fight> grouped = groups.get(key);
			if (!fight.isClosed() && (grouped == null || !grouped.contains(fight)))
			{
				boolean open = expanded.containsKey(fight.getId())
					? Boolean.TRUE.equals(expanded.get(fight.getId()))
					: autoOpen.contains(fight.getId());
				index = addFight(snapshot, fight, open, index, 0);
				hitsPanel.add(groupSeparator());
				continue;
			}
			if (!shown.add(key))
			{
				continue;
			}
			List<Fight> group = groups.get(key);
			boolean holdsOpenFight = autoGroups.contains(key);
			boolean open = groupsOpen.containsKey(key)
				? Boolean.TRUE.equals(groupsOpen.get(key))
				: holdsOpenFight;
			hitsPanel.add(groupHeader(group, snapshot, open));
			if (open)
			{
				for (Fight fightInGroup : group)
				{
					boolean fightOpen = expanded.containsKey(fightInGroup.getId())
						? Boolean.TRUE.equals(expanded.get(fightInGroup.getId()))
						: autoOpen.contains(fightInGroup.getId());
					index = addFight(snapshot, fightInGroup, fightOpen, index, 12);
				}
			}
			hitsPanel.add(groupSeparator());
		}
	}

	private Set<Long> autoOpenFights(List<Fight> fights)
	{
		List<Fight> newest = new ArrayList<>(fights);
		newest.sort(CombatLogPanel::newerFight);
		Set<Long> open = new HashSet<>();
		int limit = Math.min(openFightCount(), newest.size());
		for (int i = 0; i < limit; i++)
		{
			open.add(newest.get(i).getId());
		}
		return open;
	}

	private int openFightCount()
	{
		int count = config.openFights();
		if (count < 0)
		{
			return 0;
		}
		return Math.min(10, count);
	}

	private static int newerFight(Fight left, Fight right)
	{
		int ticks = Integer.compare(right.lastTick(), left.lastTick());
		if (ticks != 0)
		{
			return ticks;
		}
		int ended = Integer.compare(right.getEndTick(), left.getEndTick());
		if (ended != 0)
		{
			return ended;
		}
		return Long.compare(right.getId(), left.getId());
	}

	/**
	 * The open-fights setting chooses which logs are expanded. A manual open or
	 * close sticks while that same set of fights is the newest, then gives way
	 * once a newer fight takes a slot.
	 */
	private void reconcileExpanded(Set<Long> autoOpen)
	{
		if (autoOpen.equals(appliedOpenFights))
		{
			return;
		}
		expanded.entrySet().removeIf(entry ->
		{
			boolean pinnedOpen = Boolean.TRUE.equals(entry.getValue());
			if (pinnedOpen)
			{
				return !autoOpen.contains(entry.getKey());
			}
			return autoOpen.contains(entry.getKey());
		});
		appliedOpenFights = new HashSet<>(autoOpen);
	}

	private void reconcileGroups(Set<String> autoGroups)
	{
		if (autoGroups.equals(appliedOpenGroups))
		{
			return;
		}
		groupsOpen.entrySet().removeIf(entry ->
		{
			boolean pinnedOpen = Boolean.TRUE.equals(entry.getValue());
			if (pinnedOpen)
			{
				return !autoGroups.contains(entry.getKey());
			}
			return autoGroups.contains(entry.getKey());
		});
		appliedOpenGroups = new HashSet<>(autoGroups);
	}

	private int addFight(CombatSnapshot snapshot, Fight fight, boolean open, int index, int leftPad)
	{
		JPanel block = new JPanel();
		block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
		block.setOpaque(false);
		block.setAlignmentX(LEFT_ALIGNMENT);
		if (leftPad > 0)
		{
			block.setBorder(new EmptyBorder(0, leftPad, 0, 0));
		}
		block.add(fightHeader(fight, open, snapshot.getView()));
		String delve = DoomDelve.detail(fight.getDelve());
		if (delve != null)
		{
			JLabel delveLabel = new JLabel(delve);
			delveLabel.setFont(uiFont());
			delveLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			delveLabel.setAlignmentX(LEFT_ALIGNMENT);
			block.add(delveLabel);
		}
		if (open)
		{
			for (AttackRow attack : AttackRow.group(fight.visibleHits(snapshot.getView())))
			{
				block.add(attackRow(attack, index));
				index++;
			}
		}
		String footer = FightSummary.footer(fight, snapshot.getNowTick(), snapshot.getView());
		if (footer != null)
		{
			block.add(noteLabel(footer));
		}
		block.add(Box.createVerticalStrut(2));
		installDeleteMenu(block, fight);
		hitsPanel.add(block);
		fitRow(block);
		return index;
	}

	private JPanel groupHeader(List<Fight> group, CombatSnapshot snapshot, boolean open)
	{
		JPanel block = new JPanel();
		block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
		block.setOpaque(false);
		block.setAlignmentX(LEFT_ALIGNMENT);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(4, 4, 4, 6));
		row.setAlignmentX(LEFT_ALIGNMENT);

		JLabel toggle = new JLabel(open ? "-" : "+");
		toggle.setFont(uiFont());
		toggle.setForeground(ColorScheme.BRAND_ORANGE);
		Dimension toggleSize = new Dimension(Math.max(14, toggle.getPreferredSize().width), toggle.getPreferredSize().height);
		toggle.setPreferredSize(toggleSize);
		toggle.setMinimumSize(toggleSize);

		Fight first = group.get(0);
		HitRecord sample = first.getHits().isEmpty() ? null : first.getHits().get(0);
		JLabel icon = targetIcon(first, sample);
		JLabel name = new JLabel(DoomDelve.displayName(first.getName(), first.getNpcId()));
		name.setFont(uiFont());
		name.setForeground(first.getKind() == TargetKind.PLAYER ? CombatLogOverlay.PLAYER_COLOR : Color.WHITE);

		JPanel nameAndIcon = new JPanel(new BorderLayout(4, 0));
		nameAndIcon.setOpaque(false);
		nameAndIcon.add(toggle, BorderLayout.WEST);
		nameAndIcon.add(icon, BorderLayout.CENTER);
		JPanel named = new JPanel(new BorderLayout(4, 0));
		named.setOpaque(false);
		named.add(nameAndIcon, BorderLayout.WEST);
		named.add(name, BorderLayout.CENTER);

		Integer maxHp = null;
		boolean sameHp = true;
		boolean delveHp = false;
		for (Fight fight : group)
		{
			if (fight.getDelve() != null)
			{
				delveHp = true;
			}
			if (fight.getMaxHp() == null)
			{
				continue;
			}
			if (maxHp == null)
			{
				maxHp = fight.getMaxHp();
			}
			else if (!maxHp.equals(fight.getMaxHp()))
			{
				sameHp = false;
				break;
			}
		}
		if (!sameHp || delveHp)
		{
			maxHp = null;
		}
		row.add(named, BorderLayout.CENTER);
		if (maxHp != null)
		{
			JLabel amount = new JLabel("HP: " + CombatLogFormatter.formatDamage(maxHp), SwingConstants.RIGHT);
			amount.setFont(uiFont());
			amount.setForeground(Color.WHITE);
			holdWidth(amount);
			row.add(amount, BorderLayout.EAST);
		}
		String key = groupKey(first);
		bindClick(row, () ->
		{
			groupsOpen.put(key, !open);
			refresh();
		});
		fitRow(row);
		block.add(row);

		String summary = FightSummary.groupLine(group);
		if (summary != null && snapshot.getView() != DamageView.INCOMING)
		{
			block.add(noteLabel(summary));
		}
		fitRow(block);
		return block;
	}

	private static JLabel noteLabel(String text)
	{
		JLabel label = new JLabel("<html><body style='width:" + WRAPPED_TEXT_WIDTH + "px'>" + text + "</body></html>");
		label.setFont(uiFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(LEFT_ALIGNMENT);
		Dimension size = label.getPreferredSize();
		label.setMaximumSize(new Dimension(size.width, size.height));
		return label;
	}

	private static String groupKey(Fight fight)
	{
		return fight.getKind() + "\0" + DoomDelve.displayName(fight.getName(), fight.getNpcId());
	}

	private void installDeleteMenu(Component component, Fight fight)
	{
		component.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				showDeleteMenu(event, fight);
			}

			@Override
			public void mouseReleased(MouseEvent event)
			{
				showDeleteMenu(event, fight);
			}
		});
		if (component instanceof java.awt.Container)
		{
			java.awt.Container container = (java.awt.Container) component;
			for (int i = 0; i < container.getComponentCount(); i++)
			{
				installDeleteMenu(container.getComponent(i), fight);
			}
		}
	}

	private void showDeleteMenu(MouseEvent event, Fight fight)
	{
		if (!event.isPopupTrigger())
		{
			return;
		}
		JPopupMenu menu = new JPopupMenu();
		JMenuItem delete = new JMenuItem("Delete");
		delete.addActionListener(action ->
		{
			expanded.remove(fight.getId());
			combatLog.removeFight(fight.getId());
			refresh();
		});
		menu.add(delete);
		menu.show(event.getComponent(), event.getX(), event.getY());
	}

	private JPanel fightHeader(Fight fight, boolean open, DamageView view)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(4, 4, 4, 6));
		row.setAlignmentX(LEFT_ALIGNMENT);

		JLabel toggle = new JLabel(open ? "-" : "+");
		toggle.setFont(uiFont());
		toggle.setForeground(ColorScheme.BRAND_ORANGE);
		Dimension toggleSize = new Dimension(Math.max(14, toggle.getPreferredSize().width), toggle.getPreferredSize().height);
		toggle.setPreferredSize(toggleSize);
		toggle.setMinimumSize(toggleSize);

		HitRecord sample = fight.getHits().isEmpty() ? null : fight.getHits().get(0);
		JLabel icon = targetIcon(fight, sample);
		JLabel name = new JLabel(DoomDelve.displayName(fight.getName(), fight.getNpcId()));
		name.setFont(uiFont());
		name.setForeground(fight.getKind() == TargetKind.PLAYER ? CombatLogOverlay.PLAYER_COLOR : Color.WHITE);

		JPanel nameAndIcon = new JPanel(new BorderLayout(4, 0));
		nameAndIcon.setOpaque(false);
		nameAndIcon.add(toggle, BorderLayout.WEST);
		nameAndIcon.add(icon, BorderLayout.CENTER);
		JPanel named = new JPanel(new BorderLayout(4, 0));
		named.setOpaque(false);
		named.add(nameAndIcon, BorderLayout.WEST);
		named.add(name, BorderLayout.CENTER);

		boolean taken = fight.showsTaken(view);
		JLabel total = new JLabel(CombatLogFormatter.formatDamage(fight.headerAmount(view)), SwingConstants.RIGHT);
		total.setFont(uiFont());
		total.setForeground(taken ? DamageColors.INCOMING : DamageColors.OUTGOING);
		holdWidth(total);

		row.add(named, BorderLayout.CENTER);
		row.add(total, BorderLayout.EAST);
		bindClick(row, () ->
		{
			expanded.put(fight.getId(), !open);
			refresh();
		});
		fitRow(row);
		return row;
	}

	/**
	 * Clicks land on the plus, the name, or the icon, not on the row behind them.
	 */
	private static void bindClick(Component component, Runnable action)
	{
		component.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (!SwingUtilities.isLeftMouseButton(event) || event.getClickCount() != 1)
				{
					return;
				}
				action.run();
			}
		});
		if (component instanceof java.awt.Container)
		{
			java.awt.Container container = (java.awt.Container) component;
			for (int i = 0; i < container.getComponentCount(); i++)
			{
				bindClick(container.getComponent(i), action);
			}
		}
	}

	private static String missDetail(CombatSnapshot snapshot)
	{
		int splashes = CombatLogFormatter.countSplashes(snapshot.getHits(), DamageDirection.OUTGOING);
		int blocks = Math.max(0, snapshot.getBlocks() - splashes);
		if (splashes > 0 && blocks == 0)
		{
			return splashes == 1 ? "1 splash" : splashes + " splashes";
		}
		if (splashes > 0)
		{
			String blockText = blocks == 1 ? "1 block" : blocks + " blocks";
			String splashText = splashes == 1 ? "1 splash" : splashes + " splashes";
			return blockText + "  \u00b7  " + splashText;
		}
		return snapshot.getBlocks() == 1 ? "1 block" : snapshot.getBlocks() + " blocks";
	}

	private JPanel attackRow(AttackRow attack, int index)
	{
		HitRecord hit = attack.first();
		boolean incoming = hit.getDirection() == DamageDirection.INCOMING;
		boolean punish = hit.getSource() == DamageSource.PUNISH;
		boolean special = attack.isSpecial() && !punish;
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(index % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : ROW_ALT);
		row.setBorder(new EmptyBorder(3, 6, 3, 6));
		row.setAlignmentX(LEFT_ALIGNMENT);

		boolean splash = attack.isSplash();
		boolean status = hit.getSource().isStatus();
		JLabel kind = new JLabel(CombatLogFormatter.rowKind(attack));
		kind.setFont(uiFont());
		kind.setForeground(splash ? DamageColors.SPLASH : special && !status ? DamageColors.SPEC : incoming ? DamageColors.INCOMING : DamageColors.OUTGOING);

		boolean thrall = hit.getSource() == DamageSource.THRALL;
		JPanel lead = new JPanel(new BorderLayout(6, 0));
		lead.setOpaque(false);
		lead.add(kind, BorderLayout.WEST);
		if (!status && !punish && (thrall || hit.getWeaponItemId() != null || hit.getPrayerIcon() != null || hit.getAttackStyleIcon() != null))
		{
			lead.add(dealtIcons(hit), BorderLayout.EAST);
		}
		else if (incoming && !status && hit.getCombatStyle() != null)
		{
			lead.add(styleSlot(hit), BorderLayout.EAST);
		}

		JPanel figures = new JPanel(new BorderLayout(4, 0));
		figures.setOpaque(false);
		figures.add(amountFigure(attack, hit), BorderLayout.CENTER);

		row.add(lead, BorderLayout.WEST);
		row.add(figures, BorderLayout.EAST);
		fitRow(row);
		return row;
	}

	private BufferedImage hitSplat(AttackRow attack)
	{
		if (config.damageAmount() != DamageAmount.HITSPLAT || gameHitsplats == null)
		{
			return null;
		}
		return gameHitsplats.image(attack);
	}

	private JComponent amountFigure(AttackRow attack, HitRecord hit)
	{
		BufferedImage splat = hitSplat(attack);
		if (splat == null || CombatLogFormatter.specHits(attack) != null || hit.getSource() == DamageSource.PUNISH)
		{
			JLabel amount = new JLabel(CombatLogFormatter.rowAmount(attack), SwingConstants.RIGHT);
			amount.setFont(uiFont());
			amount.setForeground(attack.isSpecial() && hit.getSource() != DamageSource.PUNISH ? DamageColors.SPEC : DamageColors.forHit(hit));
			holdWidth(amount);
			tipWhenClipped(amount, CombatLogFormatter.rowAmount(attack));
			return amount;
		}
		JLabel icon = new JLabel(new ImageIcon(splat), SwingConstants.RIGHT);
		icon.setToolTipText(CombatLogFormatter.rowAmount(attack));
		if (!maxHit(attack))
		{
			return icon;
		}
		JLabel note = new JLabel("(Max hit)");
		note.setFont(uiFont());
		note.setForeground(attack.isSpecial() ? DamageColors.SPEC : DamageColors.forHit(hit));
		JPanel both = new JPanel(new BorderLayout(4, 0));
		both.setOpaque(false);
		both.add(icon, BorderLayout.WEST);
		both.add(note, BorderLayout.EAST);
		return both;
	}

	private static boolean maxHit(AttackRow attack)
	{
		for (HitRecord recorded : attack.getHits())
		{
			if (recorded.getKind() == HitKind.MAX_HIT)
			{
				return true;
			}
		}
		return false;
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
		CombatSnapshot snapshot = combatLog.summarize(System.currentTimeMillis(), 10, damageView());
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

	/**
	 * The sidebar scroll pane. Tests use this instead of reflecting into {@link net.runelite.client.ui.PluginPanel}.
	 */
	JScrollPane wrappedScrollPane()
	{
		return getScrollPane();
	}

	private JLabel targetIcon(Fight fight, @Nullable HitRecord sample)
	{
		if (fight.getKind() == TargetKind.PLAYER)
		{
			JLabel icon = iconLabel(MonsterIcons.SIZE);
			Integer head = fight.getHeadItemId();
			BufferedImage image = PlayerIcon.image(fight, monsterIcons, weaponIcons);
			if (image != null)
			{
				icon.setIcon(new ImageIcon(image));
			}
			if (head != null && head > 0 && fight.getHeadItemName() != null)
			{
				icon.setToolTipText(fight.getHeadItemName());
			}
			return icon;
		}
		Integer id = fight.getNpcId();
		if (id == null && sample != null)
		{
			id = sample.getNpcId();
		}
		BufferedImage image = id == null || monsterIcons == null ? null : monsterIcons.get(id);
		if (image == null && id == null && monsterIcons != null && CombatDummy.named(fight.getName()))
		{
			image = monsterIcons.get(CombatDummy.ICON_ID);
		}
		JLabel icon = iconLabel(MonsterIcons.SIZE);
		if (image != null)
		{
			icon.setIcon(new ImageIcon(image));
		}
		return icon;
	}

	private static JLabel iconLabel(int size)
	{
		JLabel icon = new JLabel();
		icon.setOpaque(false);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		Dimension box = new Dimension(size, size);
		icon.setPreferredSize(box);
		icon.setMinimumSize(box);
		icon.setMaximumSize(box);
		return icon;
	}

	private JLabel thrallSlot(HitRecord hit)
	{
		JLabel icon = new JLabel();
		icon.setOpaque(false);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		Dimension size = new Dimension(WeaponIcons.SIZE, WeaponIcons.SIZE);
		icon.setPreferredSize(size);
		icon.setMinimumSize(size);
		icon.setMaximumSize(size);
		BufferedImage image = Thralls.image(hit);
		if (image != null)
		{
			icon.setIcon(new ImageIcon(image));
		}
		int id = Thralls.iconId(hit);
		icon.setToolTipText(Thralls.maxHit(id) > 0 ? Thralls.name(id) : Thralls.name(Thralls.FALLBACK_NPC_ID));
		return icon;
	}

	private JLabel styleSlot(HitRecord hit)
	{
		JLabel icon = new JLabel();
		icon.setOpaque(false);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		Dimension size = new Dimension(StyleIcons.SIZE, StyleIcons.SIZE);
		icon.setPreferredSize(size);
		icon.setMinimumSize(size);
		icon.setMaximumSize(size);
		if (styleIcons != null && hit.getCombatStyle() != null)
		{
			BufferedImage image = styleIcons.get(hit.getCombatStyle());
			if (image != null)
			{
				icon.setIcon(new ImageIcon(image));
			}
			icon.setToolTipText(styleName(hit.getCombatStyle()));
		}
		return icon;
	}

	private static String styleName(CombatStyle style)
	{
		switch (style)
		{
			case RANGED:
				return "Ranged";
			case MAGIC:
				return "Magic";
			default:
				return "Melee";
		}
	}

	private JPanel dealtIcons(HitRecord hit)
	{
		JPanel icons = new JPanel();
		icons.setLayout(new BoxLayout(icons, BoxLayout.X_AXIS));
		icons.setOpaque(false);
		if (hit.getSource() == DamageSource.THRALL)
		{
			icons.add(thrallSlot(hit));
		}
		else if (hit.getWeaponItemId() != null)
		{
			icons.add(weaponSlot(hit));
		}
		else if (hit.getAttackStyleIcon() != null)
		{
			icons.add(attackStyleSlot(hit));
		}
		if (hit.getSource() != DamageSource.THRALL && hit.getPrayerIcon() != null)
		{
			if (icons.getComponentCount() > 0)
			{
				icons.add(Box.createHorizontalStrut(3));
			}
			icons.add(prayerSlot(hit));
		}
		return icons;
	}

	private JLabel prayerSlot(HitRecord hit)
	{
		JLabel icon = new JLabel();
		icon.setOpaque(false);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		Dimension size = new Dimension(PrayerIcons.SIZE, PrayerIcons.SIZE);
		icon.setPreferredSize(size);
		icon.setMinimumSize(size);
		icon.setMaximumSize(size);
		if (prayerIcons != null)
		{
			BufferedImage image = prayerIcons.get(hit.getPrayerIcon());
			if (image != null)
			{
				icon.setIcon(new ImageIcon(image));
			}
		}
		String name = hit.getPrayerIcon() == null ? null : OffensivePrayers.name(hit.getPrayerIcon());
		if (name != null)
		{
			icon.setToolTipText(name);
		}
		return icon;
	}

	private JLabel attackStyleSlot(HitRecord hit)
	{
		JLabel icon = new JLabel();
		icon.setOpaque(false);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		Dimension size = new Dimension(WeaponIcons.SIZE, WeaponIcons.SIZE);
		icon.setPreferredSize(size);
		icon.setMinimumSize(size);
		icon.setMaximumSize(size);
		Integer spriteId = hit.getAttackStyleIcon();
		if (prayerIcons != null && spriteId != null)
		{
			BufferedImage image = prayerIcons.get(spriteId);
			if (image != null)
			{
				icon.setIcon(new ImageIcon(image));
			}
		}
		String name = UnarmedStyle.name(spriteId);
		if (name != null)
		{
			icon.setToolTipText(name);
		}
		return icon;
	}

	private JLabel weaponSlot(HitRecord hit)
	{
		JLabel icon = new JLabel();
		icon.setOpaque(false);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		Dimension size = new Dimension(WeaponIcons.SIZE, WeaponIcons.SIZE);
		icon.setPreferredSize(size);
		icon.setMinimumSize(size);
		icon.setMaximumSize(size);
		Integer itemId = hit.getWeaponItemId();
		if (itemId != null)
		{
			BufferedImage image = weaponIcons.get(itemId);
			if (image != null)
			{
				icon.setIcon(new ImageIcon(image));
			}
		}
		return icon;
	}

	private static Font uiFont()
	{
		Font font = FontManager.getRunescapeFont();
		if (font == null)
		{
			font = FontManager.getRunescapeSmallFont();
		}
		return font == null ? new Font(Font.DIALOG, Font.PLAIN, 16) : font;
	}

	private static Font boldFont(float size)
	{
		Font font = FontManager.getRunescapeBoldFont();
		if (font == null)
		{
			font = uiFont();
		}
		return font.deriveFont(Font.BOLD, size);
	}

	private static JLabel sectionLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(uiFont());
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

	/**
	 * A long spec total is clipped by the sidebar. Hovering the number shows the full text.
	 */
	private static void tipWhenClipped(JLabel label, String full)
	{
		label.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent event)
			{
				refreshAmountTip(label, full);
			}
		});
		label.addHierarchyListener(event ->
		{
			if (label.isShowing())
			{
				SwingUtilities.invokeLater(() -> refreshAmountTip(label, full));
			}
		});
	}

	private static void refreshAmountTip(JLabel label, String full)
	{
		label.setToolTipText(amountClipped(label) ? full : null);
	}

	private static boolean amountClipped(JLabel label)
	{
		int needed = label.getPreferredSize().width;
		if (label.getWidth() > 0 && label.getWidth() < needed)
		{
			return true;
		}
		Container viewport = label.getParent();
		while (viewport != null && !(viewport instanceof JViewport))
		{
			viewport = viewport.getParent();
		}
		if (viewport == null || viewport.getWidth() <= 0 || !label.isShowing())
		{
			return false;
		}
		int x = SwingUtilities.convertPoint(label, 0, 0, viewport).x;
		return x < 0 || x + needed > viewport.getWidth();
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
		button.setFont(uiFont());
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
