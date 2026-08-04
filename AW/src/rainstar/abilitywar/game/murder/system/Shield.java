package rainstar.abilitywar.game.murder.system;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.GameEndEvent;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.murderer.ZombieMurderer;

public class Shield {
	
	private static Map<Participant, EachShield> shieldmap = new HashMap<>();
	
	public static void addShield(Participant participant, int amount) {
		if (!shieldmap.containsKey(participant)) new EachShield(participant);
		shieldmap.get(participant).addShield(amount);
	}
	
	public static int getShield(Participant participant) {
		if (!shieldmap.containsKey(participant)) return 0;
		else return shieldmap.get(participant).getShield();
	}
	
	public static void setShield(Participant participant, int amount) {
		if (!shieldmap.containsKey(participant)) new EachShield(participant);
		shieldmap.get(participant).setShield(amount);
	}
	
	public static class EachShield implements Listener {
	
		public EachShield(Participant participant) {
			this.participant = participant;
			ac = participant.actionbar().newChannel();
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			shieldmap.put(participant, this);
		}
		
		private final Participant participant;
		private ActionbarChannel ac;
		private int shield = 0;
		
		public void addShield(int amount) {
			shield += amount;
			ac.update("§b실드§7: §3" + shield);
		}
		
		public int getShield() {
			return shield;
		}
		
		public void setShield(int amount) {
			shield = amount;
			ac.update("§b실드§7: §3" + shield);
		}
		
		@EventHandler(priority = EventPriority.HIGH)
		private void onMurder(MurderEvent e) {
			if (!e.isCancelled() && e.getTarget().equals(participant) && shield > 0) {
				e.setCancelled(true);
				SoundLib.ITEM_SHIELD_BLOCK.playSound(participant.getPlayer().getLocation(), 1, 1);
				ParticleLib.CRIT_MAGIC.spawnParticle(participant.getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
				shield = Math.max(0, shield - 1);
				new Invincible(participant, 4).start();
				ac.update("§b실드§7: §3" + shield);
				if (ZombieMurderer.class.equals(participant.getAbility().getClass())) {
					AbstractMurderer murder = (AbstractMurderer) participant.getAbility();
					if (!murder.hasBow()) {
						participant.getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
					}
					murder.addArrow();
				}
			}
		}
		
		@EventHandler(priority = EventPriority.HIGH)
		private void onArrowKilled(ArrowKillEvent e) {
			if (!e.isCancelled() && e.getTarget().equals(participant) && shield > 0) {
				e.setCancelled(true);
				SoundLib.ITEM_SHIELD_BLOCK.playSound(participant.getPlayer().getLocation(), 1, 1);
				ParticleLib.CRIT_MAGIC.spawnParticle(participant.getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
				shield = Math.max(0, shield - 1);
				new Invincible(participant, 4).start();
				ac.update("§b실드§7: §3" + shield);
				if (ZombieMurderer.class.equals(participant.getAbility().getClass())) {
					AbstractMurderer murder = (AbstractMurderer) participant.getAbility();
					if (!murder.hasBow()) {
						participant.getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
					}
					murder.addArrow();
				}
			}
		}
		
		@EventHandler()
		private void onGameEnd(GameEndEvent e) {
			ac.unregister();
			HandlerList.unregisterAll(this);
		}
		
		public class Invincible extends SimpleTimer implements Listener {
			
			private final Participant participant;
			
			private Invincible(Participant participant, int duration) {
				super(TaskType.REVERSE, duration);
				setPeriod(TimeUnit.TICKS, 1);
				this.participant = participant;
			}
			
			@Override
			public void onStart() {
				Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			}
			
			@Override
			public void onEnd() {
				onSilentEnd();
			}
			
			@Override
			public void onSilentEnd() {
				HandlerList.unregisterAll(this);
			}
			
			@EventHandler(priority = EventPriority.NORMAL)
			private void onMurder(MurderEvent e) {
				if (e.getTarget().equals(participant)) e.setCancelled(true);
			}
			
			@EventHandler(priority = EventPriority.NORMAL)
			private void onArrowKilled(ArrowKillEvent e) {
				if (e.getTarget().equals(participant)) e.setCancelled(true);
			}
			
		}
		
	}
	
}
