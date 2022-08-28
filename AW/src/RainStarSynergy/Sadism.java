package RainStarSynergy;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;

@AbilityManifest(
		name = "가학증", rank = Rank.L, species = Species.HUMAN, explain = {
		"적에게 피해를 줄 때마다 §c공격력§f이 §e$[DAMAGE_INCREASE]§f% 상승합니다. 피격 시 §b초기화§f됩니다."
		})
public class Sadism extends Synergy {
	
	public Sadism(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> DAMAGE_INCREASE = 
			synergySettings.new SettingObject<Integer>(Sadism.class, "damage-increase", 33,
            "# 타격당 공격력 증가 수치", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
    private double increase = DAMAGE_INCREASE.getValue() * 0.01;
	private int stack = 0;
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager)) {
			e.setDamage(e.getDamage() * (1 + (stack * increase)));
			stack++;
		}
		
		if (e.getEntity().equals(getPlayer()) && damager != null) {
			stack = 0;
		}
	}

}
