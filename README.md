# Detailed Combat Log

Plugin adds a Sidebar panel with a log of the damage done in the fight, divided by NPC and adjustable by incoming, outgoing or combination of both. Plugin specified types of damage, weapon icons, offensive prayers used, type of incoming damage, and summarizes basic fight data. Includes an optional overlay with a summary of the last hits. 

Plugin icon identified by the Arkan blade.

## Examples

A few Cerberus kills, collapsed. The header is the session total. Each fight has its time, accuracy, and any ticks lost. Best time for that monster is repeated under the fight.

![Collapsed Cerberus fights, with session damage and best time](screenshots/fight-list.png)


![Cerberus fight with weapon, prayer, and thrall icons](screenshots/cerberus-hits.png)


![Undead cow fight with dealt and taken rows](screenshots/undead-cow.png)

## What it keeps

Your hitsplats, max hits. Max hits are marked `(Max hit)` next to the number.
Thrall damage (indicated in white text)

Damage you take is logged too, including poison, venom, burn, and bleed. 
Other players' hits on your target are not.

Blocks are kept by default and do not add to the damage total. A magic splash is logged as a 0 when the attack was magic. Accuracy is your weapon hits divided by weapon hits plus blocks and splashes. Thralls and other effects stay out of that percentage.

Multi-hit weapons and specs are one row per swing, with the splats added together. 
A spec is labeled Spec.

The three icons under the totals switch the list between outgoing damage, incoming damage, and both. The overlay summarizes the same view. 

Log deleted on logout. A world hop keeps it.

## Settings

| Setting | Default | |
| --- | --- | --- |
| Record blocks | On | Keep 0-damage blocks. Totals ignore them either way. |
| Record player hits | On | Also log hits you land on other players. |
| Log length | 300 | Drop the oldest rows after this. |
| Clear on logout | Off | World hops keep the log. |
| Damage view | Outgoing | Outgoing, incoming, or both. The sidebar icons change this too. |
| Show overlay | On | Recent hits on the game view. |
| Overlay rows | 6 | How many hits the overlay lists. |
| Overlay width | 140px | From 120 to 400. |


