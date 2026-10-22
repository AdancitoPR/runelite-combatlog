package com.combatlog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.SplitComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class CombatLogOverlay extends OverlayPanel
{
	static final Color BLOCK_COLOR = new Color(150, 150, 150);
	static final Color PLAYER_COLOR = new Color(255, 176, 140);
	static final Color GROUP_LINE = new Color(55, 200, 90);
	private final CombatLog combatLog;
	private final CombatLogConfig config;
	private final MonsterIcons monsterIcons;
	private final WeaponIcons weaponIcons;
	private final StyleIcons styleIcons;
	private final PrayerIcons prayerIcons;
	private CombatSnapshot snapshot;
	private int snapshotVersion;
	private DamageView snapshotView;

	@Inject
	CombatLogOverlay(CombatLogPlugin plugin, CombatLog combatLog, CombatLogConfig config, MonsterIcons monsterIcons, WeaponIcons weaponIcons, StyleIcons styleIcons, PrayerIcons prayerIcons)
	{
		super(plugin);
		this.combatLog = combatLog;
		this.config = config;
		this.monsterIcons = monsterIcons;
		this.weaponIcons = weaponIcons;
		this.styleIcons = styleIcons;
		this.prayerIcons = prayerIcons;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_LOW);
		setResizable(false);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		CombatSnapshot snapshot = snapshot();
		if (snapshot.isEmpty())
		{
			return null;
		}

		Font font = FontManager.getRunescapeFont();
		if (font != null)
		{
			graphics.setFont(font);
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text(CombatLogPlugin.NAME)
			.build());
		if (snapshot.getView() != DamageView.INCOMING)
		{
			panelComponent.getChildren().add(amountLine("Damage done", snapshot.getTotalDamage(), DamageColors.OUTGOING));
			int thrall = CombatLogFormatter.thrallDamage(snapshot.getHits());
			if (thrall > 0)
			{
				panelComponent.getChildren().add(amountLine("Thrall damage", thrall, Color.WHITE));
			}
		}
		if (snapshot.getView() != DamageView.OUTGOING)
		{
			panelComponent.getChildren().add(amountLine("Taken", snapshot.getDamageTaken(), DamageColors.INCOMING));
		}

		int shown = 0;
		int limit = Math.max(1, config.overlayHits());
		boolean drewGroup = false;
		for (Fight fight : snapshot.getFights())
		{
			if (shown >= limit)
			{
				break;
			}
			if (drewGroup)
			{
				panelComponent.getChildren().add(new GroupSeparator());
			}
			String footer = FightSummary.footer(fight, snapshot.getNowTick(), snapshot.getView(), true);
			if (footer != null)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left(footer)
					.leftColor(BLOCK_COLOR)
					.build());
			}
			panelComponent.getChildren().add(fightTitle(fight));
			for (AttackRow attack : AttackRow.group(fight.visibleHits(snapshot.getView())))
			{
				if (shown >= limit)
				{
					break;
				}
				shown++;
				panelComponent.getChildren().add(hitRow(attack));
			}
			drewGroup = true;
		}

		panelComponent.setPreferredSize(new Dimension(overlayWidth(), 0));
		return super.render(graphics);
	}

	/**
	 * The overlay never reads time based fields, so a summary stays valid until
	 * the log changes or the view does.
	 */
	private CombatSnapshot snapshot()
	{
		DamageView view = damageView();
		int version = combatLog.version();
		if (snapshot == null || version != snapshotVersion || view != snapshotView)
		{
			snapshot = combatLog.summarize(System.currentTimeMillis(), 10, view);
			snapshotVersion = version;
			snapshotView = view;
		}
		return snapshot;
	}

	private int overlayWidth()
	{
		try
		{
			int width = config.overlayWidth();
			if (width < 120)
			{
				return 140;
			}
			return Math.min(width, 400);
		}
		catch (RuntimeException ex)
		{
			return 140;
		}
	}

	private DamageView damageView()
	{
		try
		{
			DamageView view = config.damageView();
			return view == null ? DamageView.OUTGOING : view;
		}
		catch (RuntimeException ex)
		{
			return DamageView.OUTGOING;
		}
	}

	private static LineComponent amountLine(String label, long amount, Color color)
	{
		return LineComponent.builder()
			.left(label)
			.right(CombatLogFormatter.formatDamage(amount))
			.rightColor(color)
			.build();
	}

	private LayoutableRenderableEntity fightTitle(Fight fight)
	{
		LineComponent line = LineComponent.builder()
			.left(fight.getName())
			.leftColor(fight.getKind() == TargetKind.PLAYER ? PLAYER_COLOR : Color.WHITE)
			.right(CombatLogFormatter.formatDamage(fight.damageDone()))
			.rightColor(DamageColors.OUTGOING)
			.build();
		BufferedImage monster = fight.getNpcId() == null ? null : monsterIcons.get(fight.getNpcId());
		if (monster == null)
		{
			return line;
		}
		return SplitComponent.builder()
			.first(new ImageComponent(monster))
			.second(line)
			.orientation(ComponentOrientation.HORIZONTAL)
			.gap(new Point(4, 0))
			.build();
	}

	private LayoutableRenderableEntity hitRow(AttackRow attack)
	{
		HitRecord hit = attack.first();
		boolean incoming = hit.getDirection() == DamageDirection.INCOMING;
		boolean special = attack.isSpecial();
		boolean splash = attack.isSplash();
		String kind = splash ? "Splash" : special ? "Spec" : incoming ? "Taken" : "Dealt";
		Color kindColor = splash ? DamageColors.SPLASH : special ? DamageColors.SPEC : incoming ? DamageColors.INCOMING : DamageColors.OUTGOING;
		return new IconLine(kind, kindColor, CombatLogFormatter.rowAmount(attack), DamageColors.forHit(hit), hitIcon(hit));
	}

	private BufferedImage hitIcon(HitRecord hit)
	{
		if (hit.getSource() == DamageSource.THRALL)
		{
			return monsterIcons.get(Thralls.iconId(hit));
		}
		if (hit.getDirection() == DamageDirection.INCOMING && hit.getCombatStyle() != null)
		{
			return styleIcons.get(hit.getCombatStyle());
		}
		return beside(weaponIcon(hit), prayerIcon(hit));
	}

	private BufferedImage prayerIcon(HitRecord hit)
	{
		if (hit.getDirection() != DamageDirection.OUTGOING || hit.getSource() != DamageSource.WEAPON || prayerIcons == null)
		{
			return null;
		}
		return prayerIcons.get(hit.getPrayerIcon());
	}

	private static BufferedImage beside(BufferedImage weapon, BufferedImage prayer)
	{
		if (weapon == null)
		{
			return prayer;
		}
		if (prayer == null)
		{
			return weapon;
		}
		return Hitsplats.beside(weapon, prayer);
	}

	private BufferedImage weaponIcon(HitRecord hit)
	{
		Integer itemId = hit.getWeaponItemId();
		if (itemId == null)
		{
			return null;
		}
		return weaponIcons.get(itemId);
	}

	/**
	 * One hit, drawn inside the overlay width. The icon sits just left of the
	 * number so neither one is painted past the panel edge.
	 */
	private static final class IconLine implements LayoutableRenderableEntity
	{
		private static final int ICON_GAP = 3;

		private final String left;
		private final Color leftColor;
		private final String right;
		private final Color rightColor;
		private final BufferedImage icon;
		private Point preferredLocation = new Point();
		private Dimension preferredSize = new Dimension();
		private final Rectangle bounds = new Rectangle();

		private IconLine(String left, Color leftColor, String right, Color rightColor, BufferedImage icon)
		{
			this.left = left;
			this.leftColor = leftColor;
			this.right = right;
			this.rightColor = rightColor;
			this.icon = icon;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			FontMetrics metrics = graphics.getFontMetrics();
			int textHeight = metrics.getHeight();
			int iconWidth = icon == null ? 0 : icon.getWidth();
			int iconHeight = icon == null ? 0 : icon.getHeight();
			int leftWidth = metrics.stringWidth(left);
			int rightWidth = metrics.stringWidth(right);
			int gap = icon == null ? 0 : ICON_GAP;
			int rowHeight = Math.max(textHeight, iconHeight);
			int width = preferredSize.width > 0
				? preferredSize.width
				: leftWidth + 6 + iconWidth + gap + rightWidth;

			int x = preferredLocation.x;
			int baseline = preferredLocation.y + (rowHeight - textHeight) / 2 + metrics.getAscent();
			graphics.setColor(leftColor);
			graphics.drawString(left, x, baseline);
			int rightX = x + width - rightWidth;
			graphics.setColor(rightColor);
			graphics.drawString(right, rightX, baseline);
			if (icon != null)
			{
				int iconX = Math.max(x + leftWidth + 4, rightX - gap - iconWidth);
				int iconY = preferredLocation.y + Math.max(0, (rowHeight - iconHeight) / 2);
				graphics.drawImage(icon, iconX, iconY, null);
			}

			Dimension size = new Dimension(width, rowHeight);
			bounds.setLocation(preferredLocation);
			bounds.setSize(size);
			return size;
		}

		@Override
		public void setPreferredLocation(Point location)
		{
			preferredLocation = location;
		}

		@Override
		public void setPreferredSize(Dimension size)
		{
			preferredSize = size;
		}

		@Override
		public Rectangle getBounds()
		{
			return bounds;
		}
	}

	private static final class GroupSeparator implements LayoutableRenderableEntity
	{
		private static final int LINE = 2;
		private static final int PAD = 3;

		private Point preferredLocation = new Point();
		private Dimension preferredSize = new Dimension();
		private final Rectangle bounds = new Rectangle();

		@Override
		public Dimension render(Graphics2D graphics)
		{
			int width = Math.max(1, preferredSize.width);
			int y = preferredLocation.y + PAD;
			graphics.setColor(GROUP_LINE);
			graphics.fillRect(preferredLocation.x, y, width, LINE);
			Dimension size = new Dimension(width, LINE + PAD * 2);
			bounds.setLocation(preferredLocation);
			bounds.setSize(size);
			return size;
		}

		@Override
		public void setPreferredLocation(Point location)
		{
			preferredLocation = location;
		}

		@Override
		public void setPreferredSize(Dimension size)
		{
			preferredSize = size;
		}

		@Override
		public Rectangle getBounds()
		{
			return bounds;
		}
	}
}
