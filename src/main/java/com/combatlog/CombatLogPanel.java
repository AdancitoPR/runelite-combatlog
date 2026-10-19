package com.combatlog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
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

	private final JLabel damageValue = new JLabel("\u2014");
	private final JLabel rateValue = new JLabel("\u2014");
	private final JLabel detailValue = new JLabel("\u2014");
	private final JPanel targetsPanel = new JPanel();
	private final JPanel hitsPanel = new JPanel();
	private final JLabel emptyLabel = new JLabel();
	private final ScrollBody body = new ScrollBody();
	private final JScrollPane scroll;
	private final JButton clearButton = new JButton("Clear");
	private final JButton copyButton = new JButton("Copy");

	private boolean clearArmed;
	private Timer clearTimer;
	private Timer copyTimer;

	@Inject
	CombatLogPanel(CombatLog combatLog, CombatLogConfig config)
	{
		super(false);
		this.combatLog = combatLog;
		this.config = config;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

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
		targetsPanel.setOpaque(false);
		targetsPanel.setAlignmentX(LEFT_ALIGNMENT);
		body.add(targetsPanel);

		emptyLabel.setFont(FontManager.getRunescapeSmallFont());
		emptyLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		emptyLabel.setAlignmentX(LEFT_ALIGNMENT);
		emptyLabel.setText("<html><body style='width:190px'>No hits yet. Attack something and the blue hitsplats you land will show up here.<br><br>Poison, venom, burn, and other players' hits stay off this log. The game does not mark those splats as yours.</body></html>");
		body.add(emptyLabel);

		hitsPanel.setLayout(new BoxLayout(hitsPanel, BoxLayout.Y_AXIS));
		hitsPanel.setOpaque(false);
		hitsPanel.setAlignmentX(LEFT_ALIGNMENT);
		body.add(hitsPanel);

		scroll = new JScrollPane(body);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(scroll, BorderLayout.CENTER);

		add(buildButtons(), BorderLayout.SOUTH);
		refresh();
	}

	void refresh()
	{
		disarmClear();
		CombatSnapshot snapshot = combatLog.summarize(System.currentTimeMillis(), config.recentSeconds());
		int previousHeight = body.getPreferredSize().height;
		int scrollValue = scroll.getVerticalScrollBar().getValue();
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
			scroll.getVerticalScrollBar().setValue(0);
		}
		else
		{
			int growth = body.getPreferredSize().height - previousHeight;
			scroll.getVerticalScrollBar().setValue(scrollValue + Math.max(0, growth));
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
		for (HitRecord hit : snapshot.getHits())
		{
			JPanel row = new JPanel(new BorderLayout(6, 0));
			row.setBackground(index % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : ROW_ALT);
			row.setBorder(new EmptyBorder(3, 6, 3, 6));
			row.setAlignmentX(LEFT_ALIGNMENT);

			JLabel time = new JLabel(CombatLogFormatter.formatTime(hit.getTimeMillis(), zone));
			time.setFont(FontManager.getRunescapeSmallFont());
			time.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			holdWidth(time);

			JLabel name = new JLabel(hit.getTargetName());
			name.setFont(FontManager.getRunescapeSmallFont());
			name.setForeground(hit.getTargetKind() == TargetKind.PLAYER ? CombatLogOverlay.PLAYER_COLOR : Color.WHITE);
			name.setMinimumSize(new Dimension(0, name.getPreferredSize().height));

			JLabel amount = new JLabel(CombatLogFormatter.amountLabel(hit), SwingConstants.RIGHT);
			amount.setFont(FontManager.getRunescapeSmallFont());
			amount.setForeground(CombatLogOverlay.amountColor(hit));
			holdWidth(amount);

			JPanel nameAndTime = new JPanel(new BorderLayout(6, 0));
			nameAndTime.setOpaque(false);
			nameAndTime.add(time, BorderLayout.WEST);
			nameAndTime.add(name, BorderLayout.CENTER);

			row.add(nameAndTime, BorderLayout.CENTER);
			row.add(amount, BorderLayout.EAST);
			row.setToolTipText(hit.describe());
			fitRow(row);
			hitsPanel.add(row);
			index++;
		}
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

	private static final class ScrollBody extends JPanel implements Scrollable
	{
		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return Math.max(visibleRect.height - 16, 16);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
