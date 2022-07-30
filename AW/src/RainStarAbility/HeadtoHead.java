package RainStarAbility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "정면승부", rank = Rank.A, species = Species.HUMAN, explain = {
		"다른 생명체가 내 측면이나 후방을 공격하면 피해량이 §c50% 감소§f합니다.",
		"다른 생명체의 측면이나 후방을 타격할 때 피해량이 §c35% 감소§f하고,",
		"정면을 타격할 때 피해량이 §c15% 증가§f합니다."
		},
		summarize = {
		})

public class HeadtoHead extends AbilityBase {

	public HeadtoHead(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> GET_DECREASE = 
			abilitySettings.new SettingObject<Integer>(HeadtoHead.class, "get-damage-decrease", 50, 
			"# 받는 피해 감소량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> ATTACK_DECREASE = 
			abilitySettings.new SettingObject<Integer>(HeadtoHead.class, "attack-damage-decrease", 35, 
			"# 받는 피해 감소량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> ATTACK_INCREASE = 
			abilitySettings.new SettingObject<Integer>(HeadtoHead.class, "attack-damage-increase", 15, 
			"# 주는 피해 증가량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private boolean isntFront(Entity owner, Entity target) {
	    Vector eye = owner.getLocation().getDirection().setY(0).normalize();
	    Vector toEntity = target.getLocation().getDirection().setY(0).normalize();
	    double dot = toEntity.dot(eye);
	    return dot >= -0.725D;
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (!getPlayer().equals(projectile.getShooter())) {
				if (e.getEntity().equals(getPlayer())) {
					if (isntFront(getPlayer(), (Entity) projectile.getShooter())) {
						e.setDamage(e.getDamage() * 0.5);
						SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation());
					}
				}
			} else {
				if (isntFront(e.getEntity(), getPlayer())) {
					e.setDamage(e.getDamage() * 0.65);
				} else {
					e.setDamage(e.getDamage() * 1.15);
					ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0.25, 1, 0.25, 25, 1, MaterialX.REDSTONE_BLOCK);
				}
			}
		} else {
			if (e.getEntity().equals(getPlayer()) && e.getDamager() != null) {
				if (isntFront(getPlayer(), e.getDamager())) {
					e.setDamage(e.getDamage() * 0.5);
					SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation());
				}
			}
			if (getPlayer().equals(e.getDamager()) && e.getEntity() instanceof LivingEntity) {
				if (isntFront(e.getEntity(), getPlayer())) {
					e.setDamage(e.getDamage() * 0.65);
				} else {
					e.setDamage(e.getDamage() * 1.15);
					ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0.25, 1, 0.25, 25, 1, MaterialX.REDSTONE_BLOCK);
				}
			}
		}
	}
	
}