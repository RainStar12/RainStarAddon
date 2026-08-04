package rainstar.abilitywar.ability;

import java.text.DecimalFormat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;


@AbilityManifest(name = "제로", rank = Rank.C, species = Species.HUMAN, explain = {
		"피해를 받으면 첫 번째 자릿수 이외에 전부 §b§l0§f으로 변경합니다.",
		"§7ex) 19.543 §e→§7 1§b0§f.§b000§f §8/§7 205 §e→§7 2§b00§f §8/§7 0.111 §e→§7 0.1§b00§f",
		"효과로 감소시킨 피해 총량이 받을 피해보다 높다면 1회 방어해줍니다."
		},
		summarize = {
		"받는 피해량이 첫 번째 자릿수를 제외하고 전부 §b§l0§f이 됩니다.",
		"효과로 감소시킨 피해 총량이 받을 피해보다 높다면 1회 방어해줍니다."
		})
public class Zero extends AbilityBase {
	
	public Zero(Participant participant) {
		super(participant);
	}
	
	private double damagestack = 0;
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.00");
	
	@Override
	public void onUpdate(Update update) {
		if (update.equals(Update.RESTRICTION_CLEAR)) {
			ac.update("§b" + df.format(damagestack));
		}
	}
	
	@SubscribeEvent(onlyRelevant = true, priority = 998)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getDamage() > damagestack) {
				double damage = e.getDamage();
				int length = (int) (Math.log10(damage) + 1);
				damage = damage / Math.pow(10, length - 1);
				damage = Math.floor(damage);
				damage = damage * Math.pow(10, length - 1);
				new Holograms(getPlayer().getLocation(), e.getDamage(), damage).start();
				damagestack += (e.getDamage() - damage);
				e.setDamage(damage);
			} else {
				new Holograms(getPlayer().getLocation(), e.getDamage(), 0).start();
				e.setCancelled(true);
				damagestack = 0;
				getPlayer().setNoDamageTicks(10);
				SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
			}
			ac.update("§b" + df.format(damagestack));
		}
	}
	
	@SubscribeEvent()
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent()
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
	
	private class Holograms extends AbilityTimer {
		
		private final IHologram hologram;
		private Random random = new Random();
		private final DecimalFormat damageDF = new DecimalFormat("0.00");
		private double damages;
		private double damage;
		private double controldamage;
		
		private Holograms(Location hitLoc, double damage, double controldamage) {
			super(TaskType.REVERSE, 30);
			setPeriod(TimeUnit.TICKS, 1);
			this.hologram = NMS.newHologram(getPlayer().getWorld(), 
					hitLoc.getX() + (((random.nextDouble() * 2) - 1) * 0.5),
					hitLoc.getY() + 1.25 + (((random.nextDouble() * 2) - 1) * 0.25), 
					hitLoc.getZ() + (((random.nextDouble() * 2) - 1) * 0.5), 
					"§b§l" + damageDF.format(damage));
			this.damage = damage;
			this.controldamage = controldamage;
			for (Player player : getPlayer().getWorld().getPlayers()) {
				hologram.display(player);
			}
		}
		
		@Override
		protected void run(int count) {
			if (count > 20) {
				if (damage > controldamage) {
					damages = damage - (((damage - controldamage) * (31 - count)) / 10);	
				} else if (damage < controldamage) {
					damages = damage + (((controldamage - damage) * (31 - count)) / 10);
				} else if (damage == controldamage) {
					damages = damage;
				}
				hologram.setText("§7§l" + damageDF.format(damages));
			} else hologram.setText("§b§l" + damageDF.format(damages));
			hologram.teleport(hologram.getLocation().add(0, 0.03, 0));
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
		}
		
	}

}
