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
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "����º�", rank = Rank.A, species = Species.HUMAN, explain = {
		"�ٸ� ����ü�� �� ��b�����f���θ� �������� �� �ֽ��ϴ�.",
		"�ٸ� ����ü�� �����̳� �Ĺ��� Ÿ���� �� ���ط��� ��c75% ���ҡ�f�ϰ�,",
		"������ Ÿ���� �� ���ط��� ��c15% ������f�մϴ�."
		})

public class HeadtoHead extends AbilityBase {

	public HeadtoHead(Participant participant) {
		super(participant);
	}
	
	private boolean isntFront(Entity owner, Entity target) {
	    Vector eye = owner.getLocation().getDirection().setY(0).normalize();
	    Vector toEntity = target.getLocation().getDirection().setY(0).normalize();
	    double dot = toEntity.dot(eye);
	    return dot >= -0.785D;
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && e.getDamager() != null) {
			if (isntFront(getPlayer(), e.getDamager())) {
				e.setCancelled(true);
				SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation());
			}
		}
		if (getPlayer().equals(e.getDamager()) && e.getEntity() instanceof LivingEntity) {
			if (isntFront(e.getEntity(), getPlayer())) {
				e.setDamage(e.getDamage() * 0.25);
			} else {
				e.setDamage(e.getDamage() * 1.15);
				ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0.25, 1, 0.25, 25, 1, MaterialX.REDSTONE_BLOCK);
			}
		}
		if (e.getDamager() instanceof Projectile && e.getEntity() instanceof LivingEntity) {
			Projectile projectile = (Projectile) e.getDamager();
			if (getPlayer().equals(projectile.getShooter())) {
				if (isntFront(e.getEntity(), e.getDamager())) {
					e.setDamage(e.getDamage() * 0.25);
				} else {
					e.setDamage(e.getDamage() * 1.15);
					ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0.25, 1, 0.25, 25, 1, MaterialX.REDSTONE_BLOCK);
				}
			}
		}
	}
	
}