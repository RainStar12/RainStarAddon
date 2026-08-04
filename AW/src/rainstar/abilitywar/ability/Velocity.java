package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "벨로시티", rank = Rank.B, species = Species.HUMAN, explain = {
		"모든 벡터 영향을 2배로 받습니다. 낙하 피해를 받지 않습니다.",
		"허공에서 웅크리면 바라보는 방향으로 돌진합니다. $[COOLDOWN]"
		})
public class Velocity extends AbilityBase {

	public Velocity(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			rsAbilitySettings.new SettingObject<Integer>(Velocity.class, "cooldown", 30, "# 돌진 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	@SubscribeEvent
	private void onPlayerVelocity(final PlayerVelocityEvent e) {
		if (e.getPlayer().equals(getPlayer()) && !e.isCancelled() && !subcool.isRunning()) {
			e.setVelocity(e.getPlayer().getVelocity().multiply(2));
			subcool.start();
		}
	}
    
	private boolean dash = true;
	private Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
    private final AbilityTimer subcool = new AbilityTimer(1) {
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SuppressWarnings("deprecation")
	@SubscribeEvent
	private void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			if (!e.getPlayer().isOnGround() && !dash && !getPlayer().isFlying()) {
				if (e.getPlayer().isSneaking() && !cooldown.isCooldown()) {
					getPlayer().setVelocity(getPlayer().getLocation().getDirection().setY(0).normalize().multiply(1.3));
					ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.5, 0.1, 0.5, 100, 0);
					SoundLib.BLOCK_SNOW_FALL.playSound(getPlayer().getLocation(), 1, 0.7f);
					dash = true;
					cooldown.start();
					SoundLib.ENTITY_IRON_GOLEM_DEATH.playSound(getPlayer().getLocation(), 1, 2);
					ParticleLib.SWEEP_ATTACK.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 1, 10);
				}	
			}
			if (e.getPlayer().isOnGround() && dash) {
				dash = false;
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.FALL)) {
				e.setCancelled(true);
				e.getEntity().sendMessage("§a낙하 대미지를 받지 않습니다.");
				SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound((Player) e.getEntity());
			}
		}
	}
	
}
