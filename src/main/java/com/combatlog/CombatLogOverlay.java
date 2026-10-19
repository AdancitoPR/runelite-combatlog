package com.combatlog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
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

		CombatSnapshot snapshot = combatLog.summarize(System.currentTimeMillis(), 10, damageView());
		if (snapshot.isEmpty())
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Combat log")
			.build());
		if (snapshot.getView() == DamageView.INCOMING)
		{
			panelComponent.getChildren().add(splatLine("Damage taken",
				Hitsplats.amount(true, CombatLogFormatter.formatDamage(snapshot.getDamageTaken()))));
		}
		else if (snapshot.getView() == DamageView.BOTH && snapshot.getDamageTaken() > 0)
		{
			panelComponent.getChildren().add(splatLine("Total damage", Hitsplats.beside(
				Hitsplats.amount(false, CombatLogFormatter.formatDamage(snapshot.getTotalDamage())),
				Hitsplats.amount(true, CombatLogFormatter.formatDamage(snapshot.getDamageTaken())))));
		}
		else
		{
			panelComponent.getChildren().add(splatLine("Total damage",
				Hitsplats.amount(false, CombatLogFormatter.formatDamage(snapshot.getTotalDamage()))));
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
			String footer = FightSummary.footer(fight, snapshot.getNowTick(), snapshot.getView());
			if (footer != null)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left(footer)
					.leftColor(BLOCK_COLOR)
					.build());
			}
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

		return super.render(graphics);
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

	private LayoutableRenderableEntity splatLine(String label, BufferedImage splat)
	{
		LineComponent line = LineComponent.builder()
			.left(label)
			.right("")
			.build();
		return new DamageLine(line, splat, "");
	}

	private LayoutableRenderableEntity hitRow(AttackRow attack)
	{
		HitRecord hit = attack.first();
		String[] parts = Hitsplats.parts(attack);
		String name = hit.getTargetName();
		LineComponent line = LineComponent.builder()
			.left(name)
			.leftColor(hit.getTargetKind() == TargetKind.PLAYER ? PLAYER_COLOR : Color.WHITE)
			.right(parts[1])
			.rightColor(BLOCK_COLOR)
			.build();

		BufferedImage splat = Hitsplats.amount(hit.getDirection() == DamageDirection.INCOMING, parts[0]);
		BufferedImage weapon = weaponIcon(hit);
		BufferedImage mark = weapon == null ? splat : Hitsplats.beside(weapon, splat);
		LayoutableRenderableEntity row = new DamageLine(line, mark, parts[1]);

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

	/**
	 * Draws the row text and a sprite just to the left of the right-hand label.
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
