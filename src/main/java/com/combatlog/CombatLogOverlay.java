package com.combatlog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
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
	static final Color HIT_COLOR = new Color(120, 186, 255);
	static final Color MAX_HIT_COLOR = new Color(255, 196, 64);
	static final Color BLOCK_COLOR = new Color(150, 150, 150);
	static final Color PLAYER_COLOR = new Color(255, 176, 140);
	static final Color GROUP_LINE = new Color(55, 200, 90);

	private final CombatLog combatLog;
	private final CombatLogConfig config;
	private final MonsterIcons monsterIcons;
	private final WeaponIcons weaponIcons;

	@Inject
	CombatLogOverlay(CombatLogPlugin plugin, CombatLog combatLog, CombatLogConfig config, MonsterIcons monsterIcons, WeaponIcons weaponIcons)
	{
		super(plugin);
		this.combatLog = combatLog;
		this.config = config;
		this.monsterIcons = monsterIcons;
		this.weaponIcons = weaponIcons;
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

		CombatSnapshot snapshot = combatLog.summarize(System.currentTimeMillis(), config.recentSeconds());
		if (snapshot.isEmpty())
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Combat log")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(CombatLogFormatter.formatDamage(snapshot.getTotalDamage()))
			.leftColor(HIT_COLOR)
			.right(CombatLogFormatter.formatRate(snapshot.getSessionDps()) + " dps")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Recent " + config.recentSeconds() + "s")
			.leftColor(BLOCK_COLOR)
			.right(CombatLogFormatter.formatRate(snapshot.getRecentDps()))
			.build());

		int shown = 0;
		int limit = Math.max(1, config.overlayHits());
		boolean drewGroup = false;
		for (List<HitRecord> group : FightGroups.group(snapshot.getHits()))
		{
			if (shown >= limit)
			{
				break;
			}
			if (drewGroup)
			{
				panelComponent.getChildren().add(new GroupSeparator());
			}
			for (HitRecord hit : group)
			{
				if (shown >= limit)
				{
					break;
				}
				shown++;
				panelComponent.getChildren().add(hitRow(hit));
			}
			drewGroup = true;
		}

		return super.render(graphics);
	}

	private LayoutableRenderableEntity hitRow(HitRecord hit)
	{
		String amount = CombatLogFormatter.amountLabel(hit);
		LineComponent line = LineComponent.builder()
			.left(hit.getTargetName())
			.leftColor(hit.getTargetKind() == TargetKind.PLAYER ? PLAYER_COLOR : Color.WHITE)
			.right(amount)
			.rightColor(amountColor(hit))
			.build();

		LayoutableRenderableEntity row = line;
		BufferedImage weapon = weaponIcon(hit);
		if (weapon != null)
		{
			row = new DamageLine(line, weapon, amount);
		}

		BufferedImage monster = hit.getNpcId() == null ? null : monsterIcons.get(hit.getNpcId());
		if (monster == null)
		{
			return row;
		}
		return SplitComponent.builder()
			.first(new ImageComponent(monster))
			.second(row)
			.orientation(ComponentOrientation.HORIZONTAL)
			.gap(new Point(4, 0))
			.build();
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

	static Color amountColor(HitRecord hit)
	{
		if (hit.isBlock())
		{
			return BLOCK_COLOR;
		}
		if (hit.getTint() != null)
		{
			switch (hit.getTint())
			{
				case "Cyan":
					return new Color(80, 220, 220);
				case "Yellow":
					return new Color(240, 220, 70);
				case "Orange":
					return new Color(255, 160, 60);
				case "White":
					return Color.WHITE;
				case "Poise":
					return new Color(196, 120, 255);
				default:
					break;
			}
		}
		if (hit.getKind() == HitKind.MAX_HIT)
		{
			return MAX_HIT_COLOR;
		}
		return HIT_COLOR;
	}

	/**
	 * Draws the damage text on the right and the weapon sprite just to its left.
	 * The damage number stays visible when the sprite has not loaded yet, because
	 * this wrapper is only used once an icon is cached.
	 */
	private static final class DamageLine implements LayoutableRenderableEntity
	{
		private static final int GAP = 4;

		private final LineComponent line;
		private final BufferedImage icon;
		private final String amount;
		private Point preferredLocation = new Point();
		private Dimension preferredSize = new Dimension();
		private final Rectangle bounds = new Rectangle();

		private DamageLine(LineComponent line, BufferedImage icon, String amount)
		{
			this.line = line;
			this.icon = icon;
			this.amount = amount;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			int width = Math.max(0, preferredSize.width);
			int amountWidth = graphics.getFontMetrics().stringWidth(amount);
			int iconX = preferredLocation.x + width - amountWidth - GAP - icon.getWidth();
			if (iconX < preferredLocation.x)
			{
				iconX = preferredLocation.x;
			}

			line.setPreferredLocation(preferredLocation);
			line.setPreferredSize(new Dimension(width, preferredSize.height));
			Dimension lineSize = line.render(graphics);

			int rowHeight = Math.max(lineSize.height, icon.getHeight());
			int iconY = preferredLocation.y + Math.max(0, (rowHeight - icon.getHeight()) / 2);
			ImageComponent image = new ImageComponent(icon);
			image.setPreferredLocation(new Point(iconX, iconY));
			image.render(graphics);

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
