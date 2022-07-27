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
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "집행관 R", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §3심판§f: $[RANGE]칸 내 적들에게 §3심판 표식§f을 $[DURATION]초간 부여합니다.",
		" §3심판 표식§f은 대상이 다른 생명체에게 준 피해량 $[PERCENTAGE]%를 누적시킵니다. $[COOLDOWN]",
		"§7검 들고 F §8- §b조율§f: §a고문 §7↔ §c집행§f 모드를 변경할 수 있습니다.",
		" 변경 후 다음 근접 공격은 심판 표식을 터뜨려 효과를 발동시킬 수 있습니다.",
		"§2[§a고문§2]§f", 
		"§8<§7패시브§8>§f 받는 모든 피해가 $[DECREASE]% 감소합니다.",
		"§8<§7표식 폭발§8>§f 대상은 표식 수치만큼 공격력을 채울 때까지 공격력이 §a0§f이 됩니다.",
		"§4[§c집행§4]§f", 
		"§8<§7패시브§8>§f 적에게 가하는 피해가 $[INCREASE]% 증가합니다.",
		"§8<§7표식 폭발§8>§f 표식 수치만큼 §c추가 피해§f를 입히고 처형 가능하다면 §4처형§f합니다."
		})
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
			
			if (getPlayer().equals(damager) && !e.getEntity().equals(damager)) {
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
