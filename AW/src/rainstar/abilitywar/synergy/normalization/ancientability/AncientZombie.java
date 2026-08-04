package rainstar.abilitywar.synergy.normalization.ancientability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "고대의 좀비", rank = Rank.SPECIAL, species = Species.UNDEAD, explain = {
        "받는 대미지가 $[DECREASE]% 감소합니다. 근육 경련으로 인해",
        "공격할 떄 에임이 튑니다."
		})
public class AncientZombie extends AbilityBase {
	
	public AncientZombie(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> DECREASE = rsAbilitySettings.new SettingObject<Integer>(AncientZombie.class, 
			"damage-decrease", 50, "# 받는 피해량 감소율", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> CHANCE = rsAbilitySettings.new SettingObject<Integer>(AncientZombie.class, 
			"chance", 30, "# 얼굴이 돌아갈 확률", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final double decrease = 1 - (DECREASE.getValue() * 0.01);
	private final int chance = CHANCE.getValue();
	private final Random random = new Random();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter().equals(getPlayer())) {
				damager = getPlayer();
			}
		}
		
		if ((getPlayer().equals(damager) || getPlayer().equals(e.getDamager())) && random.nextInt(100) < chance) {
			float yaw = random.nextInt(180) - 89;
			float pitch = random.nextInt(360) - 179;
			for (Player player : Bukkit.getOnlinePlayers()) {
				NMS.rotateHead(player, getPlayer(), yaw, pitch);
			}
		}
		
		if (getPlayer().equals(e.getEntity())) {
			e.setDamage(e.getDamage() * decrease);
			SoundLib.ENTITY_ZOMBIE_HURT.playSound(getPlayer().getLocation(), 1, 1);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		if (getPlayer().equals(e.getEntity())) {
			e.setDamage(e.getDamage() * decrease);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (getPlayer().equals(e.getEntity())) {
			e.setDamage(e.getDamage() * decrease);
		}
	}

}
