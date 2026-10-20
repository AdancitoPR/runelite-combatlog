package com.combatlog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

	private final CombatLog combatLog;
	private final CombatLogConfig config;
	private final MonsterIcons monsterIcons;
	private final WeaponIcons weaponIcons;
	private final TabIcons tabIcons;
	private final StyleIcons styleIcons;
	private ConfigManager configManager;
	private DamageView selectedView;
	private final Map<Long, Boolean> expanded = new HashMap<>();

	private final JLabel totalLabel = new JLabel("Total damage");
	private final JLabel damageValue = new JLabel("\u2014");
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
		this(combatLog, config, monsterIcons, weaponIcons, null, null);
	}

	CombatLogPanel(CombatLog combatLog, CombatLogConfig config, MonsterIcons monsterIcons, WeaponIcons weaponIcons, TabIcons tabIcons, StyleIcons styleIcons)
	{
		super(true);
		this.combatLog = combatLog;
		this.config = config;
		this.monsterIcons = monsterIcons;
		this.weaponIcons = weaponIcons;
		this.tabIcons = tabIcons;
		this.styleIcons = styleIcons;
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

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		applyScrollChrome();

		totalLabel.setFont(uiFont());
		totalLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		damageValue.setFont(boldFont(20f));
		damageValue.setForeground(VALUE);
		detailValue.setFont(uiFont());
		detailValue.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel totalRow = new JPanel(new BorderLayout(8, 0));
		totalRow.setOpaque(false);
		totalRow.setAlignmentX(LEFT_ALIGNMENT);
		totalRow.add(totalLabel, BorderLayout.WEST);
		totalRow.add(damageValue, BorderLayout.CENTER);

		JPanel stats = new JPanel();
		stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));
		stats.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		stats.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(8, 8, 8, 8)));
		stats.add(totalRow);
		stats.add(Box.createVerticalStrut(2));
		stats.add(detailValue);
		add(buildNorth(stats), BorderLayout.NORTH);

		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.setBorder(new EmptyBorder(8, 8, 8, 8));

		emptyLabel.setFont(uiFont());
		emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		emptyLabel.setAlignmentX(LEFT_ALIGNMENT);
		emptyLabel.setOpaque(true);
		emptyLabel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		emptyLabel.setVerticalAlignment(SwingConstants.TOP);
		emptyLabel.setText("<html><body style='width:190px'>No hits yet. Attack something and the damage you deal shows up here in red."
			+ "<br><br>Damage you take is logged too, including poison, venom, burn, and bleed. Other players' hits stay off this log.</body></html>");
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

	@Inject
	CombatLogPanel(CombatLog combatLog, CombatLogConfig config, MonsterIcons monsterIcons, WeaponIcons weaponIcons, TabIcons tabIcons, StyleIcons styleIcons, ConfigManager configManager)
	{
		this(combatLog, config, monsterIcons, weaponIcons, tabIcons, styleIcons);
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

	private void showPlainDamage(String text)
	{
		damageValue.setIcon(null);
		damageValue.setForeground(VALUE);
		damageValue.setText(text);
	}

	private void showNumber(Color color, String text)
	{
		damageValue.setIcon(null);
		damageValue.setForeground(color);
		damageValue.setText(text);
	}

	private void showDealtTaken(CombatSnapshot snapshot)
	{
		int thrall = CombatLogFormatter.thrallDamage(snapshot.getHits());
		showColoredNumbers(
			CombatLogFormatter.formatDamage(snapshot.getTotalDamage()),
			thrall > 0 ? CombatLogFormatter.formatDamage(thrall) : null,
			CombatLogFormatter.formatDamage(snapshot.getDamageTaken()));
	}

	private void showColoredNumbers(String dealt, String thrall, String taken)
	{
		StringBuilder html = new StringBuilder("<html><span style='color:rgb(255,64,64)'>").append(dealt).append("</span>");
		if (thrall != null)
		{
			html.append("&nbsp;<span style='color:rgb(255,255,255)'>").append(thrall).append("</span>");
		}
		if (taken != null)
		{
			html.append("&nbsp;&nbsp;<span style='color:rgb(90,150,255)'>").append(taken).append("</span>");
		}
		html.append("</html>");
		damageValue.setIcon(null);
		damageValue.setText(html.toString());
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
		disarmClear();
		CombatSnapshot snapshot = combatLog.summarize(System.currentTimeMillis(), 10, damageView());
		JScrollPane scrollPane = getScrollPane();
		int previousHeight = getPreferredSize().height;
		int scrollValue = scrollPane.getVerticalScrollBar().getValue();
		boolean pinnedToTop = scrollValue <= 0;

		if (snapshot.getView() == DamageView.INCOMING)
		{
			totalLabel.setText("Damage taken");
			if (snapshot.getIncomingHits() == 0)
			{
				showPlainDamage("\u2014");
				detailValue.setText("Waiting for a hit");
			}
			else
			{
				showNumber(DamageColors.INCOMING, CombatLogFormatter.formatDamage(snapshot.getDamageTaken()));
				detailValue.setText(snapshot.getIncomingHits() + (snapshot.getIncomingHits() == 1 ? " hit" : " hits"));
			}
		}
		else if (snapshot.getView() == DamageView.BOTH)
		{
			totalLabel.setText("Dealt / taken");
			if (snapshot.getDamageHits() == 0 && snapshot.getIncomingHits() == 0 && snapshot.getBlocks() == 0
				&& CombatLogFormatter.thrallDamage(snapshot.getHits()) == 0)
			{
				showPlainDamage("\u2014");
				detailValue.setText("Waiting for a hit");
			}
			else
			{
				showDealtTaken(snapshot);
				detailValue.setText(snapshot.getDamageHits() + " dealt  \u00b7  " + snapshot.getIncomingHits() + " taken");
			}
		}
		else if (snapshot.getDamageHits() == 0 && snapshot.getBlocks() == 0 && snapshot.getDamageTaken() == 0
			&& CombatLogFormatter.thrallDamage(snapshot.getHits()) == 0)
		{
			totalLabel.setText("Total damage");
			showPlainDamage("\u2014");
			detailValue.setText("Waiting for a hit");
		}
		else if (snapshot.getDamageHits() == 0 && CombatLogFormatter.thrallDamage(snapshot.getHits()) == 0)
		{
			totalLabel.setText("Total damage");
			showNumber(DamageColors.OUTGOING, "0");
			detailValue.setText(snapshot.getBlocks() == 1 ? "1 block" : snapshot.getBlocks() + " blocks");
		}
		else
		{
			totalLabel.setText("Total damage");
			int thrall = CombatLogFormatter.thrallDamage(snapshot.getHits());
			if (thrall > 0)
			{
				showColoredNumbers(CombatLogFormatter.formatDamage(snapshot.getTotalDamage()), CombatLogFormatter.formatDamage(thrall), null);
			}
			else
			{
				showNumber(DamageColors.OUTGOING, CombatLogFormatter.formatDamage(snapshot.getTotalDamage()));
			}
			detailValue.setText(snapshot.getDamageHits() + (snapshot.getDamageHits() == 1 ? " hit" : " hits")
				+ "  \u00b7  max " + snapshot.getMaxHit()
				+ "  \u00b7  avg " + CombatLogFormatter.formatAverage(snapshot.getAverageHit()));
		}

		rebuildFights(snapshot);
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

		hitsPanel.add(sectionLabel("Fights"));
		hitsPanel.add(Box.createVerticalStrut(4));

		ZoneId zone = ZoneId.systemDefault();
		int index = 0;
		int fightIndex = 0;
		for (Fight fight : snapshot.getFights())
		{
			boolean open = expanded.containsKey(fight.getId()) ? expanded.get(fight.getId()) : fightIndex == 0;
			hitsPanel.add(fightHeader(fight, open));
			if (open)
			{
				for (AttackRow attack : AttackRow.group(fight.visibleHits(snapshot.getView())))
				{
					hitsPanel.add(attackRow(attack, index, zone));
					index++;
				}
			}
			String footer = FightSummary.footer(fight, snapshot.getNowTick(), snapshot.getView());
			if (footer != null)
			{
				JLabel footerLabel = new JLabel("<html><body style='width:190px'>" + footer + "</body></html>");
				footerLabel.setFont(uiFont());
				footerLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				footerLabel.setAlignmentX(LEFT_ALIGNMENT);
				hitsPanel.add(footerLabel);
			}
			String pace = FightSummary.pace(snapshot.getFights(), fight.getName(), fight.getKind());
			if (pace != null && snapshot.getView() != DamageView.INCOMING)
			{
				JLabel paceLabel = new JLabel("<html><body style='width:190px'>" + pace + "</body></html>");
				paceLabel.setFont(uiFont());
				paceLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				paceLabel.setAlignmentX(LEFT_ALIGNMENT);
				hitsPanel.add(paceLabel);
			}
			hitsPanel.add(groupSeparator());
			fightIndex++;
		}
	}

	private JPanel fightHeader(Fight fight, boolean open)
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
		JLabel icon = sample == null ? new JLabel() : monsterIcon(sample);
		JLabel name = new JLabel(fight.getName());
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

		JLabel total = new JLabel(CombatLogFormatter.formatDamage(fight.damageDone()), SwingConstants.RIGHT);
		total.setFont(uiFont());
		total.setForeground(DamageColors.OUTGOING);
		holdWidth(total);

		row.add(named, BorderLayout.CENTER);
		row.add(total, BorderLayout.EAST);
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				expanded.put(fight.getId(), !open);
				refresh();
			}
		});
		fitRow(row);
		return row;
	}

	private JPanel attackRow(AttackRow attack, int index, ZoneId zone)
	{
		HitRecord hit = attack.first();
		boolean incoming = hit.getDirection() == DamageDirection.INCOMING;
		boolean special = attack.isSpecial();
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(index % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : ROW_ALT);
		row.setBorder(new EmptyBorder(3, 6, 3, 6));
		row.setAlignmentX(LEFT_ALIGNMENT);

		JLabel kind = new JLabel(special ? "Spec" : incoming ? "Taken" : "Dealt");
		kind.setFont(uiFont());
		kind.setForeground(special ? DamageColors.SPEC : incoming ? DamageColors.INCOMING : DamageColors.OUTGOING);

		boolean thrall = hit.getSource() == DamageSource.THRALL;
		JPanel lead = new JPanel(new BorderLayout(6, 0));
		lead.setOpaque(false);
		lead.add(kind, BorderLayout.WEST);
		if (!thrall && hit.getWeaponItemId() != null)
		{
			lead.add(weaponSlot(hit), BorderLayout.EAST);
		}
		else if (incoming && hit.getCombatStyle() != null)
		{
			lead.add(styleSlot(hit), BorderLayout.EAST);
		}

		JLabel amount = new JLabel(CombatLogFormatter.rowAmount(attack), SwingConstants.RIGHT);
		amount.setFont(uiFont());
		amount.setForeground(DamageColors.forHit(hit));
		holdWidth(amount);

		JPanel figures = new JPanel(new BorderLayout(4, 0));
		figures.setOpaque(false);
		if (thrall)
		{
			figures.add(thrallSlot(hit), BorderLayout.WEST);
		}
		figures.add(amount, BorderLayout.CENTER);

		row.add(lead, BorderLayout.WEST);
		row.add(figures, BorderLayout.EAST);
		row.setToolTipText(CombatLogFormatter.formatTime(hit.getTimeMillis(), zone) + "  " + hit.describe());
		fitRow(row);
		return row;
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

	private JLabel thrallSlot(HitRecord hit)
	{
		JLabel icon = new JLabel();
		icon.setOpaque(false);
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		Dimension size = new Dimension(MonsterIcons.SIZE, MonsterIcons.SIZE);
		icon.setPreferredSize(size);
		icon.setMinimumSize(size);
		icon.setMaximumSize(size);
		BufferedImage image = monsterIcons.get(Thralls.iconId(hit));
		if (image != null)
		{
			icon.setIcon(new ImageIcon(image));
		}
		icon.setToolTipText("Thrall");
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
