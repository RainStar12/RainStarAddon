package RainStarSynergy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(
		name = "���� ����", rank = Rank.L, species = Species.UNDEAD, explain = {
		"��7���� ���� ��8- ��c��ĥ����f: ���� ü���� 50%���� ���� �� ��c�߰� ���ء�8(��7Max ��1.8��8)��f��,",
		" ���� ü���� 50%���� ���� �� �⺻ ������� ���ҡ�8(��7Min ��0.8��8)��f�մϴ�.",
		" ���� �������� ���� �׿��� ��� ���� HP�� $[HEAL_AMOUNT]%�� ȸ���� �� �ֽ��ϴ�.",
		"��7�нú� ��8- ��c�� ������f: ��� ���� ü���� Ȯ���� �� �ֽ��ϴ�.",
		" �ٴڿ� ��ħ���� ����, ���� ü���� ���� ���� ��ġ�� �˷��ݴϴ�.",
		"��7ö�� Ÿ���� ��8- ��c���� �⿬��f: $[RANGE]ĭ �̳��� ���� �ٶ󺸰� ö���� ��Ŭ���ϸ�",
		" ����� �������� ������ ����, ���� ü�� $[EXECUTION_HEALTH]% ������ ���� ó���մϴ�. $[COOLDOWN]",
		" ó���� �� �ϳ��� �ִ� ü���� ����ϸ�, �ƹ��� ó������ ���� ���",
		" $[STUN_DURAITON]�ʰ� ���� ���¿� ������ ��Ÿ���� 2��� �����ϴ�."
		})

public class Bloodlust extends Synergy {
	
	public Bloodlust(Participant participant) {
		super(participant);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity (EntityDamageByEntityEvent e) {	
		if (e.getDamager() instanceof Projectile) {
		    Projectile arrow = (Projectile) e.getDamager();
		    if (getPlayer().equals(arrow.getShooter()) && e.getEntity() instanceof LivingEntity) {
		    	final LivingEntity target = (LivingEntity) e.getEntity();
		    	double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		    	double nowHealth = target.getHealth();
		    	double multiply = ((1 - (nowHealth / maxHealth)) + 0.8);
				e.setDamage(e.getDamage() * multiply);
			}
		}
		
		if (e.getDamager(). equals(getPlayer()) && e.getEntity() instanceof LivingEntity) {
			final LivingEntity target = (LivingEntity) e.getEntity();
	    	double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
	    	double nowHealth = target.getHealth();
	    	double multiply = ((1 - (nowHealth / maxHealth)) + 0.8);
	    	e.setDamage(e.getDamage() * multiply);
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
