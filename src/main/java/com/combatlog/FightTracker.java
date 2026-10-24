package com.combatlog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import javax.annotation.Nullable;

/**
 * One open life per NPC index. Pausing damage does not start another log for
 * that same NPC. A death or despawn closes the life, and the next hit on that
 * index starts a new one. A quiet gap is only the fallback for fights that
 * are not tied to an NPC index.
 */
final class FightTracker
{
	private static final int WEAPON_WINDOW = 8;

	private final List<Fight> fights = new ArrayList<>();
	private final Map<String, Fight> open = new HashMap<>();
	private final Map<String, Fight> lastClosed = new HashMap<>();
	/** The open surface fight for each Doom delve. Shield and respawn share it. */
	private final Map<Integer, Fight> surfaceByLevel = new HashMap<>();
	private final Map<Long, Fight> byId = new HashMap<>();
	private final List<AttackSwing> queued = new ArrayList<>();
	private final TreeSet<Integer> activeTicks = new TreeSet<>();
	private final Map<String, HeadGear> playerHeads = new HashMap<>();
	private long nextFightId = 1;
	private long nextAttackId = 1;
	private int nextSequence;
	private int lastTick;

	@Nullable
	HitRecord accept(HitRecord hit)
	{
		Fight fight = locate(hit);
		if (fight == null)
		{
			return null;
		}
		if (hit.getTick() > lastTick)
		{
			lastTick = hit.getTick();
		}
		Long attackId = hit.getAttackId();
		boolean special = hit.isSpecial();
		if (attackId == null)
		{
			AttackSwing swing = selectSwing(fight, hit);
			if (swing != null)
			{
				attackId = swing.getAttackId();
				if (swing.isSpec() && hit.getSource() == DamageSource.WEAPON)
				{
					special = true;
				}
			}
			else
			{
				attackId = attackIdFor(fight, hit);
			}
		}
		HitRecord stored = hit.withMeta(hit.getTick(), fight.getId(), attackId, hit.getWeaponItemId(), special, hit.getSource(), hit.getOverkill(), nextSequence++);
		fight.addHit(stored);
		byId.put(fight.getId(), fight);
		return stored;
	}

	/**
	 * The NPC the player is already fighting, including the short window after
	 * it dies so the last incoming hit stays on that same log.
	 */
	/**
	 * The one player fight still open, so incoming damage can stay on it when
	 * the opponent is no longer the interaction target.
	 */
	@Nullable
	String soleOpenPlayer()
	{
		String name = null;
		for (Fight fight : open.values())
		{
			if (fight.isClosed() || fight.getKind() != TargetKind.PLAYER)
			{
				continue;
			}
			if (name != null)
			{
				return null;
			}
			name = fight.getName();
		}
		return name;
	}

	@Nullable
	Integer currentNpc(@Nullable Integer npcIndex, int tick)
	{
		if (npcIndex == null)
		{
			return null;
		}
		String key = npcKey(npcIndex);
		Fight current = open.get(key);
		if (current != null && !current.isClosed())
		{
			return npcIndex;
		}
		Fight closed = lastClosed.get(key);
		if (closed != null && closed.isKilled() && tick <= closed.getEndTick() + FightSummary.LATE_TICKS)
		{
			return npcIndex;
		}
		return null;
	}

	void closeLife(@Nullable Integer npcIndex, int tick)
	{
		if (npcIndex == null)
		{
			return;
		}
		if (tick > lastTick)
		{
			lastTick = tick;
		}
		String key = npcKey(npcIndex);
		Fight fight = open.get(key);
		if (fight == null || fight.isClosed())
		{
			return;
		}
		fight.closeKilled(tick);
		detach(fight, key);
	}

	/**
	 * Closes the life without a kill, for an NPC that left the scene while
	 * still alive. It does not count toward kills or best times.
	 */
	void closeQuiet(@Nullable Integer npcIndex, int tick)
	{
		if (npcIndex == null)
		{
			return;
		}
		if (tick > lastTick)
		{
			lastTick = tick;
		}
		String key = npcKey(npcIndex);
		Fight fight = open.get(key);
		if (fight == null || fight.isClosed())
		{
			return;
		}
		fight.closeQuiet(fight.lastTick());
		detach(fight, key);
	}

	/**
	 * Closes every open NPC life without a kill, for a logout or world hop
	 * where the NPCs leave the scene without despawn events.
	 */
	void closeAllQuiet(int tick)
	{
		if (tick > lastTick)
		{
			lastTick = tick;
		}
		for (Iterator<Map.Entry<String, Fight>> iterator = open.entrySet().iterator(); iterator.hasNext();)
		{
			Map.Entry<String, Fight> entry = iterator.next();
			Fight fight = entry.getValue();
			if (fight.getKind() != TargetKind.NPC)
			{
				continue;
			}
			if (!fight.isClosed())
			{
				fight.closeQuiet(fight.lastTick());
				lastClosed.put(entry.getKey(), fight);
				unmapSurface(fight);
			}
			iterator.remove();
		}
		surfaceByLevel.clear();
	}

	void noteForm(@Nullable Integer npcIndex, int npcId)
	{
		if (npcIndex == null)
		{
			return;
		}
		Fight fight = open.get(npcKey(npcIndex));
		if (fight == null)
		{
			return;
		}
		// Burrow is its own log. Retagging the surface fight would split the delve.
		if (DoomDelve.surface(fight.getNpcId()) && DoomDelve.burrowed(npcId))
		{
			return;
		}
		fight.setNpcId(npcId);
		if (DoomDelve.surface(npcId) && fight.getDelve() != null && !fight.isClosed())
		{
			surfaceByLevel.put(fight.getDelve(), fight);
		}
	}

	void noteMaxHp(@Nullable Integer npcIndex, int maxHp)
	{
		if (npcIndex == null || maxHp <= 0)
		{
			return;
		}
		Fight fight = open.get(npcKey(npcIndex));
		if (fight != null)
		{
			fight.setMaxHp(maxHp);
		}
	}

	void noteDelve(@Nullable Integer npcIndex, int level)
	{
		if (npcIndex == null || level <= 0)
		{
			return;
		}
		Fight fight = open.get(npcKey(npcIndex));
		if (fight == null || fight.isClosed())
		{
			return;
		}
		boolean first = fight.getDelve() == null;
		if (first && DoomDelve.surface(fight.getNpcId()))
		{
			finishLowerSurfaces(level, fight);
		}
		fight.setDelve(level);
		if (first && DoomDelve.surface(fight.getNpcId()) && fight.getDelve() != null)
		{
			surfaceByLevel.put(fight.getDelve(), fight);
		}
	}

	/**
	 * A later spawn of the same delve, including the shielded form, keeps
	 * using the fight already open for that level.
	 */
	void joinDoomSurface(@Nullable Integer npcIndex, int level)
	{
		if (npcIndex == null || level <= 0)
		{
			return;
		}
		Fight fight = surfaceByLevel.get(level);
		if (fight == null || fight.isClosed())
		{
			return;
		}
		open.put(npcKey(npcIndex), fight);
		flushQueuedFor(fight, npcIndex);
	}

	/** The surface boss was gone and is back. The gap is not lost ticks. */
	void resumeDoomSurface(int tick)
	{
		for (Fight fight : surfaceByLevel.values())
		{
			if (!fight.isClosed())
			{
				fight.notePhaseResume(tick);
			}
		}
	}

	boolean hasOpenDoomSurface()
	{
		for (Fight fight : surfaceByLevel.values())
		{
			if (!fight.isClosed())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Closes the surface fight once the delve is over. A kill is the boss
	 * dying. Leaving or wiping closes it without a kill.
	 */
	void finishDoomSurface(int tick, boolean killed)
	{
		if (tick > lastTick)
		{
			lastTick = tick;
		}
		List<Fight> finishing = new ArrayList<>();
		for (Fight fight : surfaceByLevel.values())
		{
			if (!fight.isClosed())
			{
				finishing.add(fight);
			}
		}
		for (Fight fight : finishing)
		{
			int end = fight.lastTick();
			if (killed)
			{
				fight.closeKilled(end);
			}
			else
			{
				fight.closeQuiet(end);
			}
			String key = fight.getNpcIndex() == null ? null : npcKey(fight.getNpcIndex());
			detach(fight, key);
		}
	}

	private void finishLowerSurfaces(int level, Fight current)
	{
		List<Fight> earlier = new ArrayList<>();
		for (Map.Entry<Integer, Fight> entry : surfaceByLevel.entrySet())
		{
			if (entry.getKey() < level && entry.getValue() != current && !entry.getValue().isClosed())
			{
				earlier.add(entry.getValue());
			}
		}
		for (Fight previous : earlier)
		{
			previous.closeKilled(previous.lastTick());
			String key = previous.getNpcIndex() == null ? null : npcKey(previous.getNpcIndex());
			detach(previous, key);
		}
	}

	private void detach(Fight fight, @Nullable String primaryKey)
	{
		open.entrySet().removeIf(entry -> entry.getValue() == fight);
		if (primaryKey != null)
		{
			lastClosed.put(primaryKey, fight);
		}
		unmapSurface(fight);
	}

	private void unmapSurface(Fight fight)
	{
		if (fight.getDelve() != null && surfaceByLevel.get(fight.getDelve()) == fight)
		{
			surfaceByLevel.remove(fight.getDelve());
			return;
		}
		surfaceByLevel.values().removeIf(mapped -> mapped == fight);
	}

	private void flushQueuedFor(Fight fight, int npcIndex)
	{
		Iterator<AttackSwing> iterator = queued.iterator();
		while (iterator.hasNext())
		{
			AttackSwing swing = iterator.next();
			if (!Objects.equals(swing.getNpcIndex(), npcIndex))
			{
				continue;
			}
			if (lastTick - swing.getTick() <= WEAPON_WINDOW)
			{
				fight.addAttack(swing);
			}
			iterator.remove();
		}
	}

	void noteAttack(AttackSwing swing)
	{
		if (swing.getAttackId() == 0)
		{
			swing = new AttackSwing(swing.getTick(), swing.getNpcIndex(), swing.getName(), swing.getWeaponItemId(), swing.getSpeed(), nextAttackId++);
		}
		if (duplicateSwing(swing))
		{
			return;
		}
		if (swing.getTick() > lastTick)
		{
			lastTick = swing.getTick();
		}
		int oldest = swing.getTick() - WEAPON_WINDOW;
		queued.removeIf(old -> old.getTick() < oldest);
		Fight fight = swing.getNpcIndex() != null ? open.get(npcKey(swing.getNpcIndex())) : open.get(playerKey(swing.getName()));
		if (fight != null && !fight.isClosed())
		{
			fight.addAttack(swing);
		}
		else
		{
			queued.add(swing);
		}
	}

	@Nullable
	Integer weaponFor(@Nullable Integer npcIndex, int tick, @Nullable Integer equipped)
	{
		AttackSwing swing = latestSwing(npcIndex, tick);
		if (swing != null && swing.getWeaponItemId() != null)
		{
			int gap = tick - swing.getTick();
			// A hit before the next attack can land still belongs to that swing,
			// even if the player has already equipped something else.
			if (equipped == null || equipped.equals(swing.getWeaponItemId()) || gap < swing.getSpeed())
			{
				return swing.getWeaponItemId();
			}
			return equipped;
		}
		return equipped;
	}

	/**
	 * The attack animation can be seen before the equipment packet. A weapon
	 * change on that same tick is the weapon that swung.
	 */
	void retargetWeapon(int tick, @Nullable Integer npcIndex, int weaponItemId, int speed)
	{
		for (Fight fight : fights)
		{
			fight.replaceWeapon(tick, npcIndex, weaponItemId, speed);
		}
		for (int i = 0; i < queued.size(); i++)
		{
			AttackSwing swing = queued.get(i);
			if (swing.getTick() != tick)
			{
				continue;
			}
			if (npcIndex != null && swing.getNpcIndex() != null && !npcIndex.equals(swing.getNpcIndex()))
			{
				continue;
			}
			queued.set(i, swing.withWeapon(weaponItemId, speed));
		}
	}

	boolean hasRecentAttack(@Nullable Integer npcIndex, int tick)
	{
		return latestSwing(npcIndex, tick) != null;
	}

	boolean fightingPlayer(@Nullable String name, int tick)
	{
		if (name == null || name.isEmpty())
		{
			return false;
		}
		Fight current = open.get(playerKey(name));
		if (current != null && !current.isClosed())
		{
			return true;
		}
		for (AttackSwing swing : queued)
		{
			if (swing.getNpcIndex() != null || !name.equals(swing.getName()))
			{
				continue;
			}
			int gap = tick - swing.getTick();
			if (gap >= 0 && gap <= WEAPON_WINDOW)
			{
				return true;
			}
		}
		return false;
	}

	void notePlayerHead(@Nullable String name, int itemId, @Nullable String itemName)
	{
		if (name == null || name.isEmpty() || itemId < 0)
		{
			return;
		}
		String shown = itemId > 0 ? itemName : null;
		playerHeads.put(name, new HeadGear(itemId, shown));
		Fight fight = open.get(playerKey(name));
		if (fight != null)
		{
			fight.setHead(itemId, shown);
		}
	}

	void noteCombatTick(int tick)
	{
		activeTicks.add(tick);
		if (tick > lastTick)
		{
			lastTick = tick;
		}
	}

	int getLastTick()
	{
		return lastTick;
	}

	int activeTickCount(int oldestTick)
	{
		return oldestTick <= 0 ? activeTicks.size() : activeTicks.tailSet(oldestTick).size();
	}

	void pruneTicksBefore(int tick)
	{
		activeTicks.headSet(tick).clear();
	}

	int queuedSize()
	{
		return queued.size();
	}

	List<Fight> fights()
	{
		List<Fight> copy = new ArrayList<>();
		for (Fight fight : fights)
		{
			if (!fight.getHits().isEmpty())
			{
				copy.add(fight.copy());
			}
		}
		copy.sort(Comparator.comparingLong((Fight fight) -> fight.lastTick()).reversed());
		return copy;
	}

	void remove(long fightId)
	{
		Fight fight = byId.remove(fightId);
		if (fight == null)
		{
			return;
		}
		fights.remove(fight);
		String key = key(fight);
		open.entrySet().removeIf(entry -> entry.getValue() == fight);
		if (lastClosed.get(key) == fight)
		{
			lastClosed.remove(key);
		}
		unmapSurface(fight);
	}

	void forget(HitRecord hit)
	{
		if (hit.getFightId() == null)
		{
			return;
		}
		Fight fight = byId.get(hit.getFightId());
		if (fight == null)
		{
			return;
		}
		fight.removeHit(hit);
		if (!fight.getHits().isEmpty())
		{
			return;
		}
		fights.remove(fight);
		byId.remove(fight.getId());
		String key = key(fight);
		open.entrySet().removeIf(entry -> entry.getValue() == fight);
		if (lastClosed.get(key) == fight)
		{
			lastClosed.remove(key);
		}
		unmapSurface(fight);
	}

	void clear()
	{
		fights.clear();
		open.clear();
		lastClosed.clear();
		surfaceByLevel.clear();
		byId.clear();
		queued.clear();
		activeTicks.clear();
		playerHeads.clear();
		lastTick = 0;
	}

	private Fight locate(HitRecord hit)
	{
		if (hit.getNpcIndex() == null && hit.getTargetKind() == TargetKind.NPC)
		{
			Fight only = soleOpenNpcFight();
			if (only != null)
			{
				return only;
			}
			// Incoming damage with no attacker used to open a fight named "Taken"
			// that never closed, because nothing can despawn. Leave it off the log.
			if (hit.getDirection() == DamageDirection.INCOMING)
			{
				return null;
			}
		}

		String key = key(hit);
		Fight current = open.get(key);
		if (current != null && !current.isClosed())
		{
			int last = current.lastTick();
			boolean sameNpc = hit.getNpcIndex() != null && hit.getTargetKind() == TargetKind.NPC;
			if (sameNpc || hit.getTick() <= 0 || hit.getTick() - last <= FightSummary.QUIET_TICKS)
			{
				return current;
			}
			current.closeQuiet(last);
			open.remove(key);
			lastClosed.put(key, current);
		}

		Fight closed = lastClosed.get(key);
		if (closed != null && closed.isKilled() && hit.getTick() <= closed.getEndTick() + FightSummary.LATE_TICKS)
		{
			return closed;
		}

		Fight created = new Fight(nextFightId++, hit.getTargetName(), hit.getTargetKind(), hit.getNpcId(), hit.getNpcIndex(), hit.getTick());
		if (hit.getTargetKind() == TargetKind.PLAYER)
		{
			HeadGear head = playerHeads.get(hit.getTargetName());
			if (head != null)
			{
				created.setHead(head.itemId, head.itemName);
			}
		}
		flushQueued(created, hit.getTick());
		fights.add(created);
		open.put(key, created);
		byId.put(created.getId(), created);
		return created;
	}

	private void flushQueued(Fight fight, int tick)
	{
		Iterator<AttackSwing> iterator = queued.iterator();
		while (iterator.hasNext())
		{
			AttackSwing swing = iterator.next();
			if (!sameTarget(fight, swing))
			{
				continue;
			}
			if (swing.getTick() <= tick && tick - swing.getTick() <= WEAPON_WINDOW)
			{
				fight.addAttack(swing);
				iterator.remove();
			}
			else if (tick - swing.getTick() > WEAPON_WINDOW)
			{
				iterator.remove();
			}
		}
	}

	void markSpecSwing(int tick, @Nullable Integer npcIndex)
	{
		for (Fight fight : fights)
		{
			fight.markSpec(tick, npcIndex);
		}
		for (int i = 0; i < queued.size(); i++)
		{
			AttackSwing swing = queued.get(i);
			if (swing.getTick() == tick && (npcIndex == null ? swing.getNpcIndex() == null : npcIndex.equals(swing.getNpcIndex())))
			{
				queued.set(i, swing.withSpec());
			}
		}
	}

	private boolean duplicateSwing(AttackSwing swing)
	{
		Fight fight = swing.getNpcIndex() != null ? open.get(npcKey(swing.getNpcIndex())) : open.get(playerKey(swing.getName()));
		if (fight != null)
		{
			for (AttackSwing existing : fight.getAttacks())
			{
				if (sameSwing(existing, swing))
				{
					return true;
				}
			}
		}
		for (AttackSwing existing : queued)
		{
			if (sameSwing(existing, swing))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean sameSwing(AttackSwing existing, AttackSwing swing)
	{
		if (existing.getTick() != swing.getTick())
		{
			return false;
		}
		if (swing.getNpcIndex() != null || existing.getNpcIndex() != null)
		{
			return Objects.equals(existing.getNpcIndex(), swing.getNpcIndex());
		}
		return existing.getName().equals(swing.getName());
	}

	/**
	 * The newest swing this hit can belong to. A blowpipe dart is still in the
	 * air when the next shot starts, so a hitsplat on that tick stays with the
	 * earlier swing instead of the one that just began.
	 */
	@Nullable
	private AttackSwing selectSwing(Fight fight, HitRecord hit)
	{
		if (hit.getDirection() != DamageDirection.OUTGOING || hit.isBlock())
		{
			return null;
		}
		AttackSwing best = null;
		for (AttackSwing swing : fight.getAttacks())
		{
			if (!openSwing(fight, hit, swing))
			{
				continue;
			}
			if (swing.getTick() == hit.getTick() && earlierSwingWaiting(fight, hit, swing))
			{
				continue;
			}
			if (best == null || swing.getTick() >= best.getTick())
			{
				best = swing;
			}
		}
		return best;
	}

	private boolean earlierSwingWaiting(Fight fight, HitRecord hit, AttackSwing swing)
	{
		for (AttackSwing older : fight.getAttacks())
		{
			if (older.getTick() >= swing.getTick())
			{
				break;
			}
			if (openSwing(fight, hit, older))
			{
				return true;
			}
		}
		return false;
	}

	private boolean openSwing(Fight fight, HitRecord hit, AttackSwing swing)
	{
		int gap = hit.getTick() - swing.getTick();
		if (gap < 0 || gap > swing.getSpeed() + 2)
		{
			return false;
		}
		Integer swingWeapon = swing.getWeaponItemId();
		Integer hitWeapon = hit.getWeaponItemId();
		if (hitWeapon != null && swingWeapon != null && !hitWeapon.equals(swingWeapon))
		{
			return false;
		}
		return claimed(fight, swing.getAttackId()) < splatsFor(swing);
	}

	private Long attackIdFor(Fight fight, HitRecord hit)
	{
		if (hit.getDirection() == DamageDirection.OUTGOING && !hit.isBlock())
		{
			for (int i = fight.getHits().size() - 1; i >= 0; i--)
			{
				HitRecord previous = fight.getHits().get(i);
				if (previous.isBlock() || previous.getDirection() != DamageDirection.OUTGOING || previous.getSource() != hit.getSource())
				{
					continue;
				}
				if (previous.getTick() > 0 && previous.getTick() == hit.getTick() && Objects.equals(previous.getWeaponItemId(), hit.getWeaponItemId()) && previous.getAttackId() != null
					&& claimed(fight, previous.getAttackId()) < splatsFor(previous.getWeaponItemId()))
				{
					return previous.getAttackId();
				}
				break;
			}
		}
		return nextAttackId++;
	}

	private static int claimed(Fight fight, long attackId)
	{
		int count = 0;
		for (HitRecord previous : fight.getHits())
		{
			if (!previous.isBlock() && previous.getAttackId() != null && previous.getAttackId() == attackId)
			{
				count++;
			}
		}
		return count;
	}

	private static int splatsFor(AttackSwing swing)
	{
		if (swing.isSpec())
		{
			int weaponId = swing.getWeaponItemId() == null ? 0 : swing.getWeaponItemId();
			return Math.max(1, SpecWeapons.splats(weaponId));
		}
		return normalSplats(swing.getWeaponItemId());
	}

	private static int splatsFor(@Nullable Integer weaponId)
	{
		return Math.max(normalSplats(weaponId), weaponId == null ? 1 : SpecWeapons.splats(weaponId));
	}

	private static int normalSplats(@Nullable Integer weaponId)
	{
		if (weaponId == null || weaponId <= 0)
		{
			return 1;
		}
		// A scythe swing on a large monster lands three hitsplats. A normal
		// blowpipe swing lands one, so later hits must not join that row.
		if (weaponId == 22325 || weaponId == 25736 || weaponId == 25739)
		{
			return 3;
		}
		if (weaponId == 28997)
		{
			return 2;
		}
		return 1;
	}

	@Nullable
	private AttackSwing latestSwing(@Nullable Integer npcIndex, int tick)
	{
		AttackSwing found = null;
		for (Fight fight : fights)
		{
			if (!Objects.equals(fight.getNpcIndex(), npcIndex))
			{
				continue;
			}
			for (AttackSwing swing : fight.getAttacks())
			{
				int gap = tick - swing.getTick();
				if (gap >= 0 && gap <= WEAPON_WINDOW && (found == null || swing.getTick() > found.getTick()))
				{
					found = swing;
				}
			}
		}
		for (AttackSwing swing : queued)
		{
			if (!Objects.equals(swing.getNpcIndex(), npcIndex))
			{
				continue;
			}
			int gap = tick - swing.getTick();
			if (gap >= 0 && gap <= WEAPON_WINDOW && (found == null || swing.getTick() > found.getTick()))
			{
				found = swing;
			}
		}
		return found;
	}

	@Nullable
	private Fight soleOpenNpcFight()
	{
		Fight found = null;
		for (Fight fight : open.values())
		{
			if (fight.isClosed() || fight.getKind() != TargetKind.NPC || fight.getNpcIndex() == null)
			{
				continue;
			}
			if (found != null)
			{
				return null;
			}
			found = fight;
		}
		return found;
	}

	private static boolean sameTarget(Fight fight, AttackSwing swing)
	{
		if (fight.getKind() == TargetKind.PLAYER)
		{
			return fight.getName().equals(swing.getName());
		}
		return Objects.equals(fight.getNpcIndex(), swing.getNpcIndex());
	}

	private static String key(HitRecord hit)
	{
		if (hit.getTargetKind() == TargetKind.PLAYER)
		{
			return playerKey(hit.getTargetName());
		}
		if (hit.getNpcIndex() != null)
		{
			return npcKey(hit.getNpcIndex());
		}
		return "npc-name:" + hit.getTargetName();
	}

	private static String key(Fight fight)
	{
		if (fight.getKind() == TargetKind.PLAYER)
		{
			return playerKey(fight.getName());
		}
		if (fight.getNpcIndex() != null)
		{
			return npcKey(fight.getNpcIndex());
		}
		return "npc-name:" + fight.getName();
	}

	private static String npcKey(int npcIndex)
	{
		return "npc:" + npcIndex;
	}

	private static String playerKey(String name)
	{
		return "player:" + name;
	}

	private static final class HeadGear
	{
		private final int itemId;
		@Nullable
		private final String itemName;

		private HeadGear(int itemId, @Nullable String itemName)
		{
			this.itemId = itemId;
			this.itemName = itemName;
		}
	}
}
