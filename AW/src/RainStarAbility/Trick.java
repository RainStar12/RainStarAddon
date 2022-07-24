package RainStarAbility;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.ClickType;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@AbilityManifest(name = "트릭", rank = Rank.A, species = Species.HUMAN, explain = {
		"철괴 우클릭으로 생명체들에게 가한 피해를 한 번 더 입힙니다. $[COOLDOWN]",
		"피해량은 스킬로 피해입힌 적 수에 비례해 더 증가합니다."
		},
		summarize = {
		"철괴 우클릭으로 생명체들에게 가한 피해를 한 번 더 입힙니다. $[COOLDOWN]",
		"피해량은 스킬로 피해입힌 적 수에 비례해 더 증가합니다."
		})
public class Trick extends AbilityBase implements ActiveHandler {
	
	public Trick(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Trick.class, "cooldown", 10,
            "# 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    private Map<LivingEntity, Double> damageMap = new HashMap<>();
    
    public AbilityTimer skill = new AbilityTimer(10) {
	}.setPeriod(TimeUnit.TICKS, 1).register();
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
			if (!cooldown.isCooldown() && !skill.isRunning()) {
				skill.start();
				for (LivingEntity livingEntity : damageMap.keySet()) {
					livingEntity.damage(damageMap.get(livingEntity), getPlayer());
				}
			}
		}
		return false;
	}
	
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && e.getEntity() instanceof LivingEntity && !skill.isRunning()) {
			damageMap.put((LivingEntity) e.getEntity(), e.getDamage());
		}
    }
    
    

}
