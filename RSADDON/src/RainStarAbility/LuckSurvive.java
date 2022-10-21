package RainStarAbility;

import java.text.DecimalFormat;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "이걸 사네", rank = Rank.A, species = Species.HUMAN, explain = {
		"피해받고 나서 체력이 §a5%§f 이하일 경우 $[DURATION]초간 무적 및 공격력이 $[INCREASE]% 증가합니다.",
		"$[COOLDOWN] §3/§f 체력이 20% 이하일 때 피해량을 $[DECREASE]% 줄여 받습니다.",
		"§b[§7아이디어 제공자§b] §5railohd"
		},
		summarize = {
		"피해받은 이후 체력이 매우 적으면 일정 시간 §c무적 및 공격력이 증가§f합니다.",
		"체력이 20% 이하일 때 피해를 줄여 받습니다."
		})
public class LuckSurvive extends AbilityBase {

	public LuckSurvive(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> DURATION = 
			abilitySettings.new SettingObject<Double>(LuckSurvive.class, "inv-duration", 3.0,
            "# 무적 및 공격력 증가 지속시간") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	
	public static final SettingObject<Integer> INCREASE = 
			abilitySettings.new SettingObject<Integer>(LuckSurvive.class, "increase", 25,
            "# 피해 증가량", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> DECREASE = 
			abilitySettings.new SettingObject<Integer>(LuckSurvive.class, "decrease", 30,
            "# 피해 감소량", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(LuckSurvive.class, "cooldown", 40,
            "# 쿨타임") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }

    };
    
	private final DecimalFormat df = new DecimalFormat("0.0");
    private ActionbarChannel ac = newActionbarChannel();
    private final int duration = (int) (DURATION.getValue() * 20);
    private final double increase = (1 + (INCREASE.getValue() * 0.01));
    private final double decrease = (1 - (DECREASE.getValue() * 0.01));
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
    private final AbilityTimer inv = new AbilityTimer(TaskType.REVERSE, duration) {
    	
    	@Override
    	public void run(int count) {
    		ac.update("§3무적§f: " + df.format((count / (double) 20)));
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
    		
	@SubscribeEvent(priority = 998)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			if (getPlayer().getHealth() <= maxHP * 0.2) {
				e.setDamage(e.getDamage() * decrease);
			}
			
			if (getPlayer().getHealth() - e.getFinalDamage() <= maxHP * 0.05 && !cooldown.isRunning()) {
				ParticleLib.TOTEM.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 100, 1);
				SoundLib.ITEM_TOTEM_USE.playSound(getPlayer().getLocation(), 1, 1.25f);
				inv.start();
				cooldown.start();
			}
            
            if (inv.isRunning()) {
                ParticleLib.TOTEM.spawnParticle(getPlayer().getLocation(), 0.25, 1, 0.25, 10, 0);
                e.setCancelled(true);
            }
		}
	}
    
    @SubscribeEvent
    public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
    	onEntityDamage(e);
    }
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	onEntityDamage(e);
    	
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && inv.isRunning()) {
			e.setDamage(e.getDamage() * increase);
		}
    }
	
}
