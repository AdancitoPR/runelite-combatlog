package com.combatlog;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.HitsplatID;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.StructComposition;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ProjectileMoved;
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
	private int thrallMax;
	private Integer thrallNpcId;
	private int playerAttackTick = -1;
	private int playerAttackSplats = 1;
	private Integer playerAttackIndex;
	private final List<PendingHit> pending = new ArrayList<>();
	private final IncomingStyles incomingStyles = new IncomingStyles();
	private int lastHitTick = -100;
	private Integer opponentIndex;
	private String opponentName;
	private Integer opponentNpcId;

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
		flushPending(tick);
		refreshThrall();
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
			if (target instanceof NPC && !isThrall((NPC) target))
			{
				rememberOpponent((NPC) target);
			}
			Integer weapon = equippedWeaponId();
			combatLog.noteAttack(tick, index, target.getName(), weapon, attackSpeed(weapon));
			playerAttackTick = tick;
			playerAttackIndex = index;
			int reserved = specMarker.reserved(index);
			playerAttackSplats = Math.max(reserved, attackSplats(weapon, target));
			return;
		}
		if (actor instanceof NPC && isThrall((NPC) actor))
		{
			rememberThrall((NPC) actor);
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
			if (!isThrall(npc))
			{
				rememberOpponent(npc);
			}
		}
		else if (actor instanceof Player)
		{
			targetKind = TargetKind.PLAYER;
		}

		int amount = event.getHitsplat().getAmount();
		int type = event.getHitsplat().getHitsplatType();
		boolean burn = type == HitsplatID.BURN;
		boolean splash = !burn && MagicSplash.outgoing(type, playerCasting(), incomingStyles.playerSpellLanding(npcIndex, client.getGameCycle()));
		int captureType = splash && type == HitsplatID.BLOCK_OTHER ? HitsplatID.BLOCK_ME : type;
		Integer weapon = combatLog.weaponFor(npcIndex, tick, equippedWeaponId());
		HitRecord record = burn
			? HitCapture.captureBurn(type, amount, actor.getName(), targetKind, npcId, npcIndex, weapon, System.currentTimeMillis(), tick)
			: HitCapture.capture(captureType, amount, actor.getName(), targetKind, npcId, npcIndex, weapon, false, config.includeBlocks() || splash, config.recordPlayerHits(), System.currentTimeMillis());
		if (record != null && splash)
		{
			record = record.asSplash();
		}
		if (record == null)
		{
			return;
		}

		boolean recentAttack = combatLog.hasRecentAttack(npcIndex, tick);
		int overkill = overkill(actor, npcIndex, npcId, amount);
		boolean killed = actor instanceof NPC && ((NPC) actor).getHealthScale() > 0 && ((NPC) actor).getHealthRatio() == 0;
		record = record.withMeta(tick, null, null, weapon, false, burn ? DamageSource.BURN : DamageSource.WEAPON, overkill, 0);
		pending.add(new PendingHit(tick, record, npcIndex, burn, vengeanceClearedTick == tick && !recentAttack, weapon, killed));
		lastHitTick = tick;
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
		NPC interacting = interactingNpc();
		NPC aggro = soleAggro();
		Integer index = IncomingTarget.index(
			interacting == null ? null : interacting.getIndex(),
			incomingStyles.attackerIndex(client.getGameCycle()),
			combatLog.currentNpc(opponentIndex, tick),
			aggro == null ? null : aggro.getIndex());
		NPC attacker = npcByIndex(index);
		String name;
		Integer npcId;
		Integer npcIndex;
		if (attacker != null)
		{
			name = attacker.getName() == null || attacker.getName().isEmpty() ? "Unknown" : attacker.getName();
			npcId = npcId(attacker);
			npcIndex = attacker.getIndex();
		}
		else if (index != null && index.equals(opponentIndex))
		{
			name = opponentName == null || opponentName.isEmpty() ? "Unknown" : opponentName;
			npcId = opponentNpcId;
			npcIndex = index;
		}
		else if (index != null)
		{
			name = "Unknown";
			npcId = null;
			npcIndex = index;
		}
		else
		{
			name = "Taken";
			npcId = null;
			npcIndex = null;
		}
		int type = event.getHitsplat().getHitsplatType();
		CombatStyle style = npcIndex == null
			? incomingStyles.landingStyle(client.getGameCycle(), name)
			: incomingStyles.styleFor(npcIndex, client.getGameCycle(), name);
		boolean splash = MagicSplash.incoming(type, style);
		HitRecord record = HitCapture.captureIncoming(
			type,
			event.getHitsplat().getAmount(),
			name,
			TargetKind.NPC,
			npcId,
			npcIndex,
			System.currentTimeMillis(),
			tick,
			config.includeBlocks() || splash);
		if (record == null)
		{
			return;
		}
		if (splash)
		{
			record = record.asSplash().withStyle(CombatStyle.MAGIC);
		}
		else if (record.getSource() == DamageSource.WEAPON)
		{
			record = record.withStyle(style);
		}
		combatLog.add(record);
		lastHitTick = tick;
		queueRefresh();
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile projectile = event.getProjectile();
		Player player = client.getLocalPlayer();
		if (projectile == null || player == null)
		{
			return;
		}
		Actor source = projectile.getSourceActor();
		if (source instanceof Player)
		{
			if (source == player && spellProjectile())
			{
				Actor target = projectile.getTargetActor();
				Integer index = target instanceof NPC ? ((NPC) target).getIndex() : null;
				if (index == null)
				{
					Actor interacting = player.getInteracting();
					index = interacting instanceof NPC ? ((NPC) interacting).getIndex() : null;
				}
				incomingStyles.notePlayerSpell(index, projectile.getEndCycle());
			}
			return;
		}
		NPC npc = source instanceof NPC ? (NPC) source : null;
		if (npc != null && isThrall(npc))
		{
			return;
		}
		boolean aimed = IncomingStyles.aimedAt(
			projectile.getTargetActor(),
			projectile.getInteracting(),
			player,
			projectile.getTargetPoint(),
			player.getWorldLocation());
		if (!aimed && !nearPlayer(projectile.getTarget(), player.getLocalLocation()))
		{
			return;
		}
		Integer index = npc == null ? null : npc.getIndex();
		int projectileId = projectile.getId();
		incomingStyles.noteProjectile(index, projectile.getEndCycle(), projectileId, IncomingStyles.isMagicProjectile(projectileId));
	}

	private static boolean nearPlayer(@Nullable LocalPoint target, @Nullable LocalPoint player)
	{
		if (target == null || player == null)
		{
			return false;
		}
		int dx = Math.abs(target.getX() - player.getX());
		int dy = Math.abs(target.getY() - player.getY());
		return Math.max(dx, dy) <= 256;
	}

	private NPC interactingNpc()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		Actor target = player.getInteracting();
		if (!(target instanceof NPC) || isThrall((NPC) target))
		{
			return null;
		}
		return (NPC) target;
	}

	private NPC soleAggro()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		NPC found = null;
		for (NPC npc : client.getNpcs())
		{
			if (npc.getInteracting() != player || isThrall(npc))
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

	@Nullable
	private NPC npcByIndex(@Nullable Integer index)
	{
		if (index == null)
		{
			return null;
		}
		for (NPC npc : client.getNpcs())
		{
			if (npc.getIndex() == index)
			{
				return npc;
			}
		}
		return null;
	}

	private void rememberOpponent(NPC npc)
	{
		opponentIndex = npc.getIndex();
		String name = npc.getName();
		if (name != null && !name.isEmpty())
		{
			opponentName = name;
		}
		Integer id = npcId(npc);
		if (id != null)
		{
			opponentNpcId = id;
		}
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

	private boolean playerCasting()
	{
		String[] styles = weaponStyles();
		if (styles == null)
		{
			return false;
		}
		return SpellStyle.isCasting(SpellStyle.selected(styles, attackStyle(), defensiveCasting()));
	}

	private boolean spellProjectile()
	{
		String[] styles = weaponStyles();
		if (styles == null)
		{
			return false;
		}
		return SpellStyle.spellProjectile(SpellStyle.selected(styles, attackStyle(), defensiveCasting()), styles);
	}

	private int attackStyle()
	{
		return client.getVarpValue(VarPlayer.ATTACK_STYLE);
	}

	private int defensiveCasting()
	{
		return client.getVarbitValue(Varbits.DEFENSIVE_CASTING_MODE);
	}

	@Nullable
	private String[] weaponStyles()
	{
		try
		{
			int weaponType = client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
			EnumComposition weapons = client.getEnum(EnumID.WEAPON_STYLES);
			if (weapons == null)
			{
				return SpellStyle.fallback(weaponType);
			}
			int styleEnumId = weapons.getIntValue(weaponType);
			if (styleEnumId == -1)
			{
				return SpellStyle.fallback(weaponType);
			}
			EnumComposition styles = client.getEnum(styleEnumId);
			int[] structIds = styles == null ? null : styles.getIntVals();
			if (structIds == null)
			{
				return SpellStyle.fallback(weaponType);
			}
			String[] names = new String[structIds.length];
			for (int i = 0; i < structIds.length; i++)
			{
				StructComposition struct = client.getStructComposition(structIds[i]);
				names[i] = struct == null ? null : struct.getStringValue(SpellStyle.STYLE_NAME_PARAM);
			}
			return names;
		}
		catch (RuntimeException ex)
		{
			return null;
		}
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

	private void refreshThrall()
	{
		for (NPC npc : client.getNpcs())
		{
			if (!isThrall(npc))
			{
				continue;
			}
			rememberThrall(npc);
			return;
		}
		thrallNpcId = null;
		thrallMax = 0;
	}

	private void rememberThrall(NPC npc)
	{
		thrallNpcId = npcId(npc);
		int known = Thralls.maxHit(thrallNpcId);
		thrallMax = known > 0 ? known : 3;
	}

	private static boolean isThrall(NPC npc)
	{
		if (npc.getName() != null && npc.getName().toLowerCase().contains("thrall"))
		{
			return true;
		}
		Integer id = npcId(npc);
		return id != null && Thralls.maxHit(id) > 0;
	}

	private void flushPending(int currentTick)
	{
		if (pending.isEmpty())
		{
			return;
		}
		List<PendingHit> ready = new ArrayList<>();
		Iterator<PendingHit> iterator = pending.iterator();
		while (iterator.hasNext())
		{
			PendingHit hit = iterator.next();
			if (hit.tick < currentTick)
			{
				ready.add(hit);
				iterator.remove();
			}
		}
		int index = 0;
		while (index < ready.size())
		{
			int tick = ready.get(index).tick;
			int end = index + 1;
			while (end < ready.size() && ready.get(end).tick == tick)
			{
				end++;
			}
			classify(ready.subList(index, end));
			index = end;
		}
		if (!ready.isEmpty())
		{
			queueRefresh();
		}
	}

	private void classify(List<PendingHit> hits)
	{
		int tick = hits.get(0).tick;
		boolean swung = thrallMax > 0 && thrallNpcId != null;
		List<Integer> eligible = new ArrayList<>();
		for (int i = 0; i < hits.size(); i++)
		{
			PendingHit hit = hits.get(i);
			if (hit.burn)
			{
				continue;
			}
			eligible.add(i);
		}
		int slots = 0;
		if (playerAttackTick == tick)
		{
			slots = Math.max(1, playerAttackSplats);
		}
		int[] amounts = new int[eligible.size()];
		boolean[] blocks = new boolean[eligible.size()];
		for (int i = 0; i < eligible.size(); i++)
		{
			PendingHit hit = hits.get(eligible.get(i));
			amounts[i] = hit.record.getAmount();
			blocks[i] = hit.record.isBlock() || hit.record.isSplash();
		}
		int picked = Thralls.pick(amounts, blocks, slots, thrallMax, swung && thrallMax > 0);
		int thrallAt = picked < 0 ? -1 : eligible.get(picked);
		for (int i = 0; i < hits.size(); i++)
		{
			PendingHit hit = hits.get(i);
			DamageSource source = hit.burn
				? DamageSource.BURN
				: i == thrallAt
					? DamageSource.THRALL
					: hit.vengeance
						? DamageSource.VENGEANCE
						: DamageSource.WEAPON;
			boolean special = specMarker.claim(hit.npcIndex, DamageDirection.OUTGOING, source);
			HitRecord record = hit.record.withMeta(hit.tick, null, null, source == DamageSource.THRALL ? null : hit.weapon, special, source, hit.record.getOverkill(), 0);
			if (source == DamageSource.THRALL)
			{
				record = record.asSource(DamageSource.THRALL, null, thrallNpcId);
			}
			combatLog.add(record);
			if (hit.killed)
			{
				combatLog.closeLife(hit.npcIndex, hit.tick);
			}
			log.debug("{} [{}]", record.describe(), record.getSource());
		}
	}

	private static int attackSplats(Integer weaponId, Actor target)
	{
		if (weaponId == null)
		{
			return 1;
		}
		switch (weaponId)
		{
			case 22325:
			case 25736:
			case 25739:
				if (target instanceof NPC)
				{
					NPCComposition composition = ((NPC) target).getTransformedComposition();
					if (composition != null && composition.getSize() > 1)
					{
						return 3;
					}
				}
				return 1;
			case 28997:
				return 2;
			default:
				return 1;
		}
	}

	private static final class PendingHit
	{
		private final int tick;
		private final HitRecord record;
		private final Integer npcIndex;
		private final boolean burn;
		private final boolean vengeance;
		private final Integer weapon;
		private final boolean killed;

		private PendingHit(int tick, HitRecord record, Integer npcIndex, boolean burn, boolean vengeance, Integer weapon, boolean killed)
		{
			this.tick = tick;
			this.record = record;
			this.npcIndex = npcIndex;
			this.burn = burn;
			this.vengeance = vengeance;
			this.weapon = weapon;
			this.killed = killed;
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
