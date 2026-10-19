package com.combatlog;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.HitsplatID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.game.NPCManager;
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

	@Inject
	private ItemManager itemManager;

	@Inject
	private NPCManager npcManager;

	private NavigationButton navButton;
	private final AtomicBoolean refreshQueued = new AtomicBoolean();
	private final SpecMarker specMarker = new SpecMarker();
	private final Map<Integer, int[]> lastHealth = new HashMap<>();
	private int lastSpecPercent = -1;
	private int lastVengeance = -1;
	private int vengeanceClearedTick = -1;
	private int thrallTick = -1;
	private int lastHitTick = -100;

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
	public void onGameTick(GameTick event)
	{
		int tick = client.getTickCount();
		Player player = client.getLocalPlayer();
		boolean interacting = player != null && player.getInteracting() != null;
		if (interacting || tick - lastHitTick <= 8)
		{
			combatLog.noteCombatTick(tick);
		}
		for (NPC npc : client.getNpcs())
		{
			int scale = npc.getHealthScale();
			int ratio = npc.getHealthRatio();
			if (scale > 0 && ratio == 0)
			{
				combatLog.closeLife(npc.getIndex(), tick);
			}
			else if (scale > 0 && ratio > 0)
			{
				lastHealth.put(npc.getIndex(), new int[] {ratio, scale});
			}
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (actor == null)
		{
			return;
		}
		int tick = client.getTickCount();
		if (actor == client.getLocalPlayer())
		{
			int animation = actor.getAnimation();
			if (animation <= 0 || animation == 829 || animation == 424)
			{
				return;
			}
			Actor target = actor.getInteracting();
			if (!(target instanceof NPC) && !(target instanceof Player))
			{
				return;
			}
			Integer index = target instanceof NPC ? ((NPC) target).getIndex() : null;
			Integer weapon = equippedWeaponId();
			combatLog.noteAttack(tick, index, target.getName(), weapon, attackSpeed(weapon));
			return;
		}
		if (actor instanceof NPC && actor.getName() != null && actor.getName().toLowerCase().contains("thrall"))
		{
			thrallTick = tick;
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int tick = client.getTickCount();
		if (event.getVarpId() == VarPlayer.SPECIAL_ATTACK_PERCENT)
		{
			int current = event.getValue();
			if (lastSpecPercent >= 0 && current < lastSpecPercent)
			{
				Player player = client.getLocalPlayer();
				Actor target = player == null ? null : player.getInteracting();
				Integer index = target instanceof NPC ? ((NPC) target).getIndex() : null;
				Integer weapon = equippedWeaponId();
				specMarker.arm(index, weapon == null ? 0 : weapon);
			}
			lastSpecPercent = current;
		}
		if (event.getVarbitId() == Varbits.VENGEANCE_ACTIVE)
		{
			int current = event.getValue();
			if (lastVengeance > 0 && current == 0)
			{
				vengeanceClearedTick = tick;
			}
			lastVengeance = current;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		combatLog.closeLife(event.getNpc().getIndex(), client.getTickCount());
		lastHealth.remove(event.getNpc().getIndex());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		combatLog.noteForm(npc.getIndex(), npcId(npc));
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		Actor actor = event.getActor();
		if (actor == null || event.getHitsplat() == null)
		{
			return;
		}

		int tick = client.getTickCount();
		boolean self = actor == client.getLocalPlayer();
		if (self)
		{
			recordIncoming(event, tick);
			return;
		}

		TargetKind targetKind = null;
		Integer npcId = null;
		Integer npcIndex = null;
		if (actor instanceof NPC)
		{
			NPC npc = (NPC) actor;
			targetKind = TargetKind.NPC;
			npcId = npcId(npc);
			npcIndex = npc.getIndex();
		}
		else if (actor instanceof Player)
		{
			targetKind = TargetKind.PLAYER;
		}

		int amount = event.getHitsplat().getAmount();
		int type = event.getHitsplat().getHitsplatType();
		boolean burn = type == HitsplatID.BURN;
		Integer weapon = combatLog.weaponFor(npcIndex, tick, equippedWeaponId());
		HitRecord record = burn
			? HitCapture.captureBurn(type, amount, actor.getName(), targetKind, npcId, npcIndex, weapon, System.currentTimeMillis(), tick)
			: HitCapture.capture(type, amount, actor.getName(), targetKind, npcId, npcIndex, weapon, false, config.includeBlocks(), config.recordPlayerHits(), System.currentTimeMillis());
		if (record == null)
		{
			return;
		}

		boolean ownAttack = combatLog.hasRecentAttack(npcIndex, tick);
		DamageSource source = DamageSources.classify(burn, ownAttack, thrallTick == tick, amount, 3, vengeanceClearedTick == tick && !ownAttack);
		boolean special = specMarker.claim(npcIndex, DamageDirection.OUTGOING, source);
		int overkill = overkill(actor, npcIndex, npcId, amount);
		record = record.withMeta(tick, null, null, weapon, special, source, overkill, 0);
		combatLog.add(record);
		lastHitTick = tick;
		if (actor instanceof NPC && ((NPC) actor).getHealthScale() > 0 && ((NPC) actor).getHealthRatio() == 0)
		{
			combatLog.closeLife(npcIndex, tick);
		}
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
		else if ("damageView".equals(event.getKey()))
		{
			panel.setDamageView(config.damageView());
			queueRefresh();
		}
	}

	private void recordIncoming(HitsplatApplied event, int tick)
	{
		NPC attacker = soleAttacker();
		String name = attacker == null || attacker.getName() == null ? "Taken" : attacker.getName();
		Integer npcId = attacker == null ? null : npcId(attacker);
		Integer npcIndex = attacker == null ? null : attacker.getIndex();
		HitRecord record = HitCapture.captureIncoming(
			event.getHitsplat().getHitsplatType(),
			event.getHitsplat().getAmount(),
			name,
			TargetKind.NPC,
			npcId,
			npcIndex,
			System.currentTimeMillis(),
			tick);
		if (record == null)
		{
			return;
		}
		combatLog.add(record);
		lastHitTick = tick;
		queueRefresh();
	}

	private NPC soleAttacker()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		NPC found = null;
		for (NPC npc : client.getNpcs())
		{
			if (npc.getInteracting() != player)
			{
				continue;
			}
			if (found != null)
			{
				return null;
			}
			found = npc;
		}
		return found;
	}

	private int overkill(Actor actor, Integer npcIndex, Integer npcId, int amount)
	{
		if (!(actor instanceof NPC) || npcIndex == null || npcId == null)
		{
			return 0;
		}
		NPC npc = (NPC) actor;
		boolean killed = npc.getHealthScale() > 0 && npc.getHealthRatio() == 0;
		int[] previous = lastHealth.get(npcIndex);
		if (!killed || previous == null)
		{
			if (npc.getHealthScale() > 0 && npc.getHealthRatio() > 0)
			{
				lastHealth.put(npcIndex, new int[] {npc.getHealthRatio(), npc.getHealthScale()});
			}
			return 0;
		}
		return Overkill.amount(amount, previous[0], previous[1], npcManager.getHealth(npcId), true);
	}

	private int attackSpeed(Integer weaponId)
	{
		if (weaponId == null || itemManager == null)
		{
			return 4;
		}
		ItemStats stats = itemManager.getItemStats(weaponId);
		if (stats == null || stats.getEquipment() == null || stats.getEquipment().getAspeed() <= 0)
		{
			return 4;
		}
		return stats.getEquipment().getAspeed();
	}

	private Integer equippedWeaponId()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null)
		{
			return null;
		}
		Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (weapon == null || weapon.getId() <= 0)
		{
			return null;
		}
		return weapon.getId();
	}

	private static Integer npcId(NPC npc)
	{
		NPCComposition composition = npc.getTransformedComposition();
		if (composition == null)
		{
			composition = npc.getComposition();
		}
		if (composition != null)
		{
			return composition.getId();
		}
		return npc.getId();
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
