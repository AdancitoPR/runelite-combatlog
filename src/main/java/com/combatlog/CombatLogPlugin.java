package com.combatlog;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Combat Log",
	description = "Records the hits you land on enemies",
	tags = {"combat", "damage", "dps", "hitsplat", "log"},
	enabledByDefault = true
)
public class CombatLogPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private CombatLogConfig config;

	@Inject
	private CombatLog combatLog;

	@Inject
	private CombatLogPanel panel;

	@Inject
	private CombatLogOverlay overlay;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	private NavigationButton navButton;
	private final AtomicBoolean refreshQueued = new AtomicBoolean();

	@Override
	protected void startUp()
	{
		combatLog.setMaxEntries(config.maxEntries());

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "combat_log.png");
		navButton = NavigationButton.builder()
			.tooltip("Combat Log")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		if (config.showOverlay())
		{
			overlayManager.add(overlay);
		}
		queueRefresh();
		log.debug("Combat Log started");
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(overlay);
		log.debug("Combat Log stopped");
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor actor = event.getActor();
		if (actor == null || event.getHitsplat() == null)
		{
			return;
		}

		boolean self = actor == client.getLocalPlayer();
		TargetKind targetKind = null;
		if (actor instanceof NPC)
		{
			targetKind = TargetKind.NPC;
		}
		else if (actor instanceof Player)
		{
			targetKind = TargetKind.PLAYER;
		}

		HitRecord record = HitCapture.capture(
			event.getHitsplat().getHitsplatType(),
			event.getHitsplat().getAmount(),
			actor.getName(),
			targetKind,
			self,
			config.includeBlocks(),
			config.recordPlayerHits(),
			System.currentTimeMillis());
		if (record == null)
		{
			return;
		}

		combatLog.add(record);
		log.debug("{}", record.describe());
		queueRefresh();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN && config.clearOnLogout())
		{
			combatLog.clear();
			queueRefresh();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CombatLogConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("maxEntries".equals(event.getKey()))
		{
			combatLog.setMaxEntries(config.maxEntries());
			queueRefresh();
		}
		else if ("showOverlay".equals(event.getKey()))
		{
			if (config.showOverlay())
			{
				overlayManager.add(overlay);
			}
			else
			{
				overlayManager.remove(overlay);
			}
		}
	}

	private void queueRefresh()
	{
		if (refreshQueued.compareAndSet(false, true))
		{
			SwingUtilities.invokeLater(this::refreshOnEdt);
		}
	}

	private void refreshOnEdt()
	{
		refreshQueued.set(false);
		panel.refresh();
		if (refreshQueued.get())
		{
			queueRefresh();
		}
	}

	@Provides
	CombatLogConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CombatLogConfig.class);
	}
}
