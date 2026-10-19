package com.combatlog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.ZoneId;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class CombatLogOverlay extends OverlayPanel
{
	static final Color HIT_COLOR = new Color(120, 186, 255);
	static final Color MAX_HIT_COLOR = new Color(255, 196, 64);
	static final Color BLOCK_COLOR = new Color(150, 150, 150);
	static final Color PLAYER_COLOR = new Color(255, 176, 140);

	private final CombatLog combatLog;
	private final CombatLogConfig config;

	@Inject
	CombatLogOverlay(CombatLogPlugin plugin, CombatLog combatLog, CombatLogConfig config)
	{
		super(plugin);
		this.combatLog = combatLog;
		this.config = config;
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

		ZoneId zone = ZoneId.systemDefault();
		int shown = 0;
		int limit = Math.max(1, config.overlayHits());
		for (HitRecord hit : snapshot.getHits())
		{
			if (shown >= limit)
			{
				break;
			}
			shown++;
			panelComponent.getChildren().add(LineComponent.builder()
				.left(CombatLogFormatter.formatTime(hit.getTimeMillis(), zone) + "  " + hit.getTargetName())
				.leftColor(hit.getTargetKind() == TargetKind.PLAYER ? PLAYER_COLOR : Color.WHITE)
				.right(CombatLogFormatter.amountLabel(hit))
				.rightColor(amountColor(hit))
				.build());
		}

		return super.render(graphics);
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
}
