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
	private final Map<Long, Fight> byId = new HashMap<>();
	private final List<AttackSwing> queued = new ArrayList<>();
	private final TreeSet<Integer> activeTicks = new TreeSet<>();
	private long nextFightId = 1;
	private long nextAttackId = 1;
	private int nextSequence;
	private int lastTick;

	HitRecord accept(HitRecord hit)
	{
		if (hit.getTick() > lastTick)
		{
			lastTick = hit.getTick();
		}
		Fight fight = locate(hit);
		Long attackId = hit.getAttackId() != null ? hit.getAttackId() : attackIdFor(fight, hit);
		HitRecord stored = hit.withMeta(hit.getTick(), fight.getId(), attackId, hit.getWeaponItemId(), hit.isSpecial(), hit.getSource(), hit.getOverkill(), nextSequence++);
		fight.addHit(stored);
		byId.put(fight.getId(), fight);
		return stored;
	}

	/**
	 * The NPC the player is already fighting, including the short window after
	 * it dies so the last incoming hit stays on that same log.
	 */
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
		open.remove(key);
		lastClosed.put(key, fight);
	}

	void noteForm(@Nullable Integer npcIndex, int npcId)
	{
		if (npcIndex == null)
		{
			return;
		}
		Fight fight = open.get(npcKey(npcIndex));
		if (fight != null)
		{
			fight.setNpcId(npcId);
		}
	}

	void noteAttack(AttackSwing swing)
	{
		if (swing.getAttackId() == 0)
		{
			swing = new AttackSwing(swing.getTick(), swing.getNpcIndex(), swing.getName(), swing.getWeaponItemId(), swing.getSpeed(), nextAttackId++);
		}
		if (swing.getTick() > lastTick)
		{
			lastTick = swing.getTick();
		}
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
			return swing.getWeaponItemId();
		}
		return equipped;
	}

	boolean hasRecentAttack(@Nullable Integer npcIndex, int tick)
	{
		return latestSwing(npcIndex, tick) != null;
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
		int count = 0;
		for (int tick : activeTicks)
		{
			if (oldestTick <= 0 || tick >= oldestTick)
			{
				count++;
			}
		}
		return count;
	}

	List<Fight> fights()
	{
		List<Fight> copy = new ArrayList<>();
		for (Fight fight : fights)
		{
			if (!fight.getHits().isEmpty())
			{
				copy.add(fight);
			}
		}
		copy.sort(Comparator.comparingLong((Fight fight) -> fight.lastTick()).reversed());
		return copy;
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
		if (open.get(key) == fight)
		{
			open.remove(key);
		}
		if (lastClosed.get(key) == fight)
		{
			lastClosed.remove(key);
		}
	}

	void clear()
	{
		fights.clear();
		open.clear();
		lastClosed.clear();
		byId.clear();
		queued.clear();
		activeTicks.clear();
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

	private Long attackIdFor(Fight fight, HitRecord hit)
	{
		if (hit.getDirection() == DamageDirection.OUTGOING && !hit.isBlock())
		{
			for (int i = fight.getAttacks().size() - 1; i >= 0; i--)
			{
				AttackSwing swing = fight.getAttacks().get(i);
				int gap = hit.getTick() - swing.getTick();
				if (gap >= 0 && gap <= WEAPON_WINDOW)
				{
					return swing.getAttackId();
				}
			}
			for (int i = fight.getHits().size() - 1; i >= 0; i--)
			{
				HitRecord previous = fight.getHits().get(i);
				if (previous.isBlock() || previous.getDirection() != DamageDirection.OUTGOING || previous.getSource() != hit.getSource())
				{
					continue;
				}
				if (previous.getTick() > 0 && previous.getTick() == hit.getTick() && Objects.equals(previous.getWeaponItemId(), hit.getWeaponItemId()) && previous.getAttackId() != null)
				{
					return previous.getAttackId();
				}
				break;
			}
		}
		return nextAttackId++;
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
}
