package RainStarAbility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
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
		"§7패시브 §8- §c블러드러스트§f: 상대의 체력의 높을수록 더 약하게,",
		" 낮을수록 더 강하게 공격합니다. 대상의 체력이 절반 이상일 때",
		" §a녹색§f 파티클을, 아니라면 §c적색§f 파티클을 띄웁니다.",
		"§7킬 §8- §c카니발리즘§f: 대상을 죽였을 경우, 현재 남은 체력만큼 체력을 회복합니다."
		})

public class Butcher extends AbilityBase {
	
	public Butcher(Participant participant) {
		super(participant);
	}
	
	private static final RGB MajorityHealth = RGB.of(134, 229, 127);
	private static final RGB MinorityHealth = RGB.of(183, 0, 0);
	
	@SubscribeEvent
	public void onEntityDamageByEntity (EntityDamageByEntityEvent e) {	
		if (NMS.isArrow(e.getDamager())) {
		    Arrow arrow = (Arrow) e.getDamager();
		    if (getPlayer().equals(arrow.getShooter()) && e.getEntity() instanceof LivingEntity) {
		    	final LivingEntity target = (LivingEntity) e.getEntity();
				e.setDamage(Math.min(12 ,Math.max(e.getDamage() + (2 / Math.max(0.2, target.getHealth() / target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())) - 4, 0.75)));				
				if (target.getHealth() >= target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2) {
					
					ParticleLib.REDSTONE.spawnParticle(target.getLocation().add(0, 2.2, 0), MajorityHealth);
				} else {
					ParticleLib.REDSTONE.spawnParticle(target.getLocation().add(0, 2.2, 0), MinorityHealth);
		    }
		}}
		
		if (e.getDamager(). equals(getPlayer()) && e.getEntity() instanceof LivingEntity) {
			final LivingEntity targets = (LivingEntity) e.getEntity();
			e.setDamage(Math.min(12 ,Math.max(e.getDamage() + (2 / Math.max(0.2, targets.getHealth() / targets.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())) - 4, 0.75)));
			
			if (targets.getHealth() >= targets.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2) {
				ParticleLib.REDSTONE.spawnParticle(targets.getLocation().add(0, 2.2, 0), MajorityHealth);
			} else {
				ParticleLib.REDSTONE.spawnParticle(targets.getLocation().add(0, 2.2, 0), MinorityHealth);
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
						Healths.setHealth(getPlayer(), getPlayer().getHealth() * 2);
					}
					SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(getPlayer().getLocation(), 1, 0.5f);	
				}
				
			}.setPeriod(TimeUnit.TICKS, 2).start();
		}
	}
};