package com.combatlog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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

	@Inject
	CombatLogOverlay(CombatLogPlugin plugin, CombatLog combatLog, CombatLogConfig config, MonsterIcons monsterIcons, WeaponIcons weaponIcons, StyleIcons styleIcons)
	{
		super(plugin);
		this.combatLog = combatLog;
		this.config = config;
		this.monsterIcons = monsterIcons;
		this.weaponIcons = weaponIcons;
		this.styleIcons = styleIcons;
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

		Font font = FontManager.getRunescapeFont();
		if (font != null)
		{
			graphics.setFont(font);
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Combat log")
			.build());
		if (snapshot.getView() != DamageView.INCOMING)
		{
			panelComponent.getChildren().add(amountLine("Dealt", snapshot.getTotalDamage(), DamageColors.OUTGOING));
			int thrall = CombatLogFormatter.thrallDamage(snapshot.getHits());
			if (thrall > 0)
			{
				panelComponent.getChildren().add(amountLine("Thrall", thrall, Color.WHITE));
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
			String footer = FightSummary.footer(fight, snapshot.getNowTick(), snapshot.getView());
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
		LineComponent kind = LineComponent.builder()
			.left(splash ? "Splash" : special ? "Spec" : incoming ? "Taken" : "Dealt")
			.leftColor(splash ? DamageColors.SPLASH : special ? DamageColors.SPEC : incoming ? DamageColors.INCOMING : DamageColors.OUTGOING)
			.build();
		LineComponent amount = LineComponent.builder()
			.right(CombatLogFormatter.rowAmount(attack))
			.rightColor(DamageColors.forHit(hit))
			.build();
		BufferedImage weapon = hitIcon(hit);
		LayoutableRenderableEntity figures = weapon == null ? amount : SplitComponent.builder()
			.first(new ImageComponent(weapon))
			.second(amount)
			.orientation(ComponentOrientation.HORIZONTAL)
			.gap(new Point(4, 0))
			.build();
		return SplitComponent.builder()
			.first(kind)
			.second(figures)
			.orientation(ComponentOrientation.HORIZONTAL)
			.gap(new Point(6, 0))
			.build();
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
		return weaponIcon(hit);
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
