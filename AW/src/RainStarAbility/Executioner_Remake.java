package RainStarAbility;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

public class Executioner_Remake extends AbilityBase {
	
	public Executioner_Remake(Participant participant) {
		super(participant);
	}
	
	private Map<Player, AttackStack> damageMap = new HashMap<>();
	
	private class AttackStack extends AbilityTimer implements Listener {
		
		private final Player damager;
		private final IHologram hologram;
		private boolean hid = false;
		private double damage = 0;
		private final DecimalFormat df = new DecimalFormat("0.00");
		
		private AttackStack(Player damager) {
			super();
			setPeriod(TimeUnit.TICKS, 2);
			this.hologram = NMS.newHologram(damager.getWorld(), damager.getLocation().getX(), damager.getLocation().getY() + damager.getEyeHeight() + 0.6, damager.getLocation().getZ(), "");
			this.damager = damager;
			Executioner_Remake.this.damageMap.put(damager, this);
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int count) {
			if (NMS.isInvisible(damager)) {
				if (!hid) {
					this.hid = true;
					hologram.hide(getPlayer());
				}
			} else {
				if (hid) {
					this.hid = false;
					hologram.display(getPlayer());
				}
				hologram.teleport(damager.getWorld(), damager.getLocation().getX(), damager.getLocation().getY() + damager.getEyeHeight() + 0.6, damager.getLocation().getZ(), damager.getLocation().getYaw(), 0);
			}
		}
		
		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (getPlayer().equals(damager)) {
				damage += e.getFinalDamage();
				hologram.setText("§4" + df.format(damage));
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
			hologram.unregister();
			Executioner_Remake.this.damageMap.remove(damager);
		}
		
	}

}
