package com.combatlog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.util.HashMap;
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
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class CombatLogPanel extends PluginPanel
{
	private static final int TARGET_ROWS = 6;
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
	private ConfigManager configManager;
	private DamageView selectedView;
	private final Map<Long, Boolean> expanded = new HashMap<>();

	private final JLabel totalLabel = new JLabel("Total damage");
	private final JLabel damageValue = new JLabel("\u2014");
	private final JLabel detailValue = new JLabel("\u2014");
	private final JPanel targetsPanel = new JPanel();
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

		totalLabel.setFont(FontManager.getRunescapeSmallFont());
		totalLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		damageValue.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		damageValue.setForeground(VALUE);
		detailValue.setFont(FontManager.getRunescapeSmallFont());
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

	@Inject
	CombatLogPanel(CombatLog combatLog, CombatLogConfig config, MonsterIcons monsterIcons, WeaponIcons weaponIcons, ConfigManager configManager)
	{
		this(combatLog, config, monsterIcons, weaponIcons);
		this.configManager = configManager;
	}

	void setDamageView(DamageView view)
	{
		applyView(view == null ? DamageView.OUTGOING : view, false);
	}

	private JPanel buildNorth(JPanel stats)
	{
		styleChoice(outgoingButton, DamageView.OUTGOING, Hitsplats.mark(false), "Outgoing");
		styleChoice(incomingButton, DamageView.INCOMING, Hitsplats.mark(true), "Incoming");
		styleChoice(bothButton, DamageView.BOTH, Hitsplats.both(), "Both");
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

	private void styleChoice(JButton button, DamageView view, BufferedImage icon, String tooltip)
	{
		button.setFocusable(false);
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBorderPainted(true);
		button.setRolloverEnabled(false);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setText("");
		button.setIcon(new ImageIcon(icon));
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
		damageValue.setText(text);
	}

	private void showSplat(boolean incoming, String text)
	{
		damageValue.setText("");
		damageValue.setIcon(new ImageIcon(Hitsplats.amount(incoming, text)));
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
				showSplat(true, CombatLogFormatter.formatDamage(snapshot.getDamageTaken()));
				detailValue.setText(snapshot.getIncomingHits() + (snapshot.getIncomingHits() == 1 ? " hit" : " hits"));
			}
		}
		else if (snapshot.getDamageHits() == 0 && snapshot.getBlocks() == 0 && snapshot.getDamageTaken() == 0)
		{
			totalLabel.setText("Total damage");
			showPlainDamage("\u2014");
			detailValue.setText("Waiting for a hit");
		}
		else if (snapshot.getDamageHits() == 0)
		{
			totalLabel.setText("Total damage");
			showSplat(false, "0");
			detailValue.setText(snapshot.getBlocks() == 1 ? "1 block" : snapshot.getBlocks() + " blocks");
		}
		else
		{
			totalLabel.setText("Total damage");
			String dealt = CombatLogFormatter.formatDamage(snapshot.getTotalDamage());
			if (snapshot.getView() == DamageView.BOTH && snapshot.getDamageTaken() > 0)
			{
				damageValue.setText("");
				damageValue.setIcon(new ImageIcon(Hitsplats.beside(
					Hitsplats.amount(false, dealt),
					Hitsplats.amount(true, CombatLogFormatter.formatDamage(snapshot.getDamageTaken())))));
			}
			else
			{
				showSplat(false, dealt);
			}
			detailValue.setText(snapshot.getDamageHits() + (snapshot.getDamageHits() == 1 ? " hit" : " hits")
				+ "  \u00b7  max " + snapshot.getMaxHit()
				+ "  \u00b7  avg " + CombatLogFormatter.formatAverage(snapshot.getAverageHit()));
		}

		rebuildTargets(snapshot);
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

			JLabel damageLabel = new JLabel("", SwingConstants.RIGHT);
			damageLabel.setIcon(new ImageIcon(Hitsplats.amount(false, CombatLogFormatter.formatDamage(target.getDamage()))));
			holdWidth(damageLabel);
			row.setToolTipText(target.getHits() + (target.getHits() == 1 ? " hit" : " hits")
				+ ", max " + target.getMaxHit()
				+ (target.getBlocks() > 0 ? ", " + target.getBlocks() + (target.getBlocks() == 1 ? " block" : " blocks") : ""));

			row.add(nameLabel, BorderLayout.CENTER);
			row.add(damageLabel, BorderLayout.EAST);
			JPanel block = new JPanel();
			block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
			block.setOpaque(false);
			block.setAlignmentX(LEFT_ALIGNMENT);
			row.setAlignmentX(LEFT_ALIGNMENT);
			block.add(row);
			if (target.getPace() != null)
			{
				JLabel pace = new JLabel("<html><body style='width:190px'>" + target.getPace() + "</body></html>");
				pace.setFont(FontManager.getRunescapeSmallFont());
				pace.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				pace.setAlignmentX(LEFT_ALIGNMENT);
				block.add(pace);
			}
			fitRow(block);
			targetsPanel.add(block);
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
			hitsPanel.add(fightHeader(fight, snapshot, open));
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
				footerLabel.setFont(FontManager.getRunescapeSmallFont());
				footerLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				footerLabel.setAlignmentX(LEFT_ALIGNMENT);
				hitsPanel.add(footerLabel);
			}
			String pace = FightSummary.pace(snapshot.getFights(), fight.getName(), fight.getKind());
			if (pace != null && snapshot.getView() != DamageView.INCOMING)
			{
				JLabel paceLabel = new JLabel("<html><body style='width:190px'>" + pace + "</body></html>");
				paceLabel.setFont(FontManager.getRunescapeSmallFont());
				paceLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				paceLabel.setAlignmentX(LEFT_ALIGNMENT);
				hitsPanel.add(paceLabel);
			}
			hitsPanel.add(groupSeparator());
			fightIndex++;
		}
	}

	private JPanel fightHeader(Fight fight, CombatSnapshot snapshot, boolean open)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(4, 4, 4, 6));
		row.setAlignmentX(LEFT_ALIGNMENT);

		JLabel toggle = new JLabel(open ? "-" : "+");
		toggle.setFont(FontManager.getRunescapeSmallFont());
		toggle.setForeground(ColorScheme.BRAND_ORANGE);
		Dimension toggleSize = new Dimension(12, toggle.getPreferredSize().height);
		toggle.setPreferredSize(toggleSize);
		toggle.setMinimumSize(toggleSize);

		HitRecord sample = fight.getHits().isEmpty() ? null : fight.getHits().get(0);
		JLabel icon = sample == null ? new JLabel() : monsterIcon(sample);
		JLabel name = new JLabel(fight.getName());
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(fight.getKind() == TargetKind.PLAYER ? CombatLogOverlay.PLAYER_COLOR : Color.WHITE);

		boolean incoming = snapshot.getView() == DamageView.INCOMING;
		int shownDamage = incoming ? fight.damageTaken() : fight.weaponDamage();
		String extra = !incoming && fight.extraDamage() > 0 ? "+" + fight.extraDamage() : "";
		JLabel amount = new JLabel(extra, SwingConstants.RIGHT);
		amount.setFont(FontManager.getRunescapeSmallFont());
		amount.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		amount.setIcon(new ImageIcon(Hitsplats.amount(incoming, CombatLogFormatter.formatDamage(shownDamage))));
		amount.setHorizontalTextPosition(SwingConstants.LEADING);
		amount.setIconTextGap(extra.isEmpty() ? 0 : 4);
		holdWidth(amount);

		JPanel nameAndIcon = new JPanel(new BorderLayout(4, 0));
		nameAndIcon.setOpaque(false);
		nameAndIcon.add(toggle, BorderLayout.WEST);
		nameAndIcon.add(icon, BorderLayout.CENTER);
		JPanel named = new JPanel(new BorderLayout(4, 0));
		named.setOpaque(false);
		named.add(nameAndIcon, BorderLayout.WEST);
		named.add(name, BorderLayout.CENTER);

		row.add(named, BorderLayout.CENTER);
		row.add(amount, BorderLayout.EAST);
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
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(index % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : ROW_ALT);
		row.setBorder(new EmptyBorder(3, 6, 3, 6));
		row.setAlignmentX(LEFT_ALIGNMENT);

		JLabel icon = monsterIcon(hit);
		String label = hit.getTargetName();
		JLabel name = new JLabel(label);
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(hit.getTargetKind() == TargetKind.PLAYER ? CombatLogOverlay.PLAYER_COLOR : Color.WHITE);
		name.setMinimumSize(new Dimension(0, name.getPreferredSize().height));

		String[] parts = Hitsplats.parts(attack);
		JLabel amount = new JLabel(parts[1], SwingConstants.RIGHT);
		amount.setFont(FontManager.getRunescapeSmallFont());
		amount.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		amount.setIcon(new ImageIcon(Hitsplats.amount(hit.getDirection() == DamageDirection.INCOMING, parts[0])));
		amount.setHorizontalTextPosition(SwingConstants.LEADING);
		amount.setIconTextGap(parts[1].isEmpty() ? 0 : 4);
		holdWidth(amount);

		JPanel nameAndIcon = new JPanel(new BorderLayout(6, 0));
		nameAndIcon.setOpaque(false);
		nameAndIcon.add(icon, BorderLayout.WEST);
		nameAndIcon.add(name, BorderLayout.CENTER);

		JPanel damage = new JPanel(new BorderLayout(4, 0));
		damage.setOpaque(false);
		damage.add(weaponSlot(hit), BorderLayout.WEST);
		damage.add(amount, BorderLayout.EAST);

		row.add(nameAndIcon, BorderLayout.CENTER);
		row.add(damage, BorderLayout.EAST);
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
