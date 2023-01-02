package rainstar.abilitywar.ability;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.PlayerDeathEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "도살자", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c블러드러스트§f: 대상에 체력에 따라 많을수록 §a약하게§8(§7최소 0.6배§8)§f,",
		" 적을수록 §4강하게§8(§7최대 1.6배§8)§f 공격합니다.",
		"§7킬 §8- §c카니발리즘§f: 대상을 죽였을 경우, 현재 남은 체력만큼 체력을 회복합니다.",
		" 이때 최대 체력을 넘는 회복량은 §e흡수 체력§f으로 얻습니다."
		},
		summarize = {
		"적 체력에 반비례하여 대상에게 피해를 §c더§f 혹은 §a덜§f 입힙니다.",
		"적을 죽일 때마다 현재 체력만큼 체력을 §d회복§f합니다.",
		"최대 체력을 넘는 회복량은 §e흡수 체력§f이 됩니다."
		})

public class Butcher extends AbilityBase {
	
	public Butcher(Participant participant) {
		super(participant);
	}
	
	private static final RGB MajorityHealth = RGB.of(134, 229, 127);
	private static final RGB MinorityHealth = RGB.of(183, 0, 0);
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {	
		if (e.getDamager() instanceof Projectile) {
		    Projectile arrow = (Projectile) e.getDamager();
		    if (getPlayer().equals(arrow.getShooter()) && e.getEntity() instanceof LivingEntity) {
		    	final LivingEntity target = (LivingEntity) e.getEntity();
		    	double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		    	double nowHealth = target.getHealth();
		    	double multiply = ((1 - (nowHealth / maxHealth)) + 0.6);
				e.setDamage(e.getDamage() * multiply);
				if (target.getHealth() >= target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2) {			
					ParticleLib.REDSTONE.spawnParticle(target.getLocation().add(0, 2.2, 0), MajorityHealth);
				} else {
					ParticleLib.REDSTONE.spawnParticle(target.getLocation().add(0, 2.2, 0), MinorityHealth);
				}
			}
		}
		
		if (e.getDamager(). equals(getPlayer()) && e.getEntity() instanceof LivingEntity) {
			final LivingEntity target = (LivingEntity) e.getEntity();
	    	double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
	    	double nowHealth = target.getHealth();
	    	double multiply = ((1 - (nowHealth / maxHealth)) + 0.6);
	    	e.setDamage(e.getDamage() * multiply);
			if (target.getHealth() >= target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2) {
				ParticleLib.REDSTONE.spawnParticle(target.getLocation().add(0, 2.2, 0), MajorityHealth);
			} else {
				ParticleLib.REDSTONE.spawnParticle(target.getLocation().add(0, 2.2, 0), MinorityHealth);
			}
		}

	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (getPlayer().equals(e.getEntity().getKiller())) { 			
			new AbilityTimer(10) {				
				private final Location startLocation = e.getEntity().getLocation().clone();
				
				@Override
				protected void run(int count) {
					ParticleLib.DAMAGE_INDICATOR.spawnParticle(startLocation.clone().add(Line.vectorAt(startLocation, getPlayer().getLocation(), 10, 10 - count)), 0, 0, 0, 1, 0);
					ParticleLib.HEART.spawnParticle(startLocation.clone().add(Line.vectorAt(startLocation, getPlayer().getLocation(), 10, 10 - count)), 0, 0, 0, 1, 0);					
				}
				
				@Override
				protected void onEnd() {
					final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), getPlayer().getHealth(), RegainReason.CUSTOM);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						if (getPlayer().getHealth() * 2 > maxHealth) {
							double yellowHeart = (getPlayer().getHealth() * 2) - maxHealth;
							Healths.setHealth(getPlayer(), maxHealth);
							NMS.setAbsorptionHearts(getPlayer(), NMS.getAbsorptionHearts(getPlayer()) + (float) yellowHeart);
						} else Healths.setHealth(getPlayer(), getPlayer().getHealth() * 2);
					}
					SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(getPlayer().getLocation(), 1, 0.5f);	
				}
				
			}.setPeriod(TimeUnit.TICKS, 2).start();
		}
	}
}