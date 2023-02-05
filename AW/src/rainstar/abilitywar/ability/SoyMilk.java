package rainstar.abilitywar.ability;

import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import kotlin.ranges.RangesKt;

@AbilityManifest(name = "두유", rank = Rank.A, species = Species.OTHERS, explain = {
        "§7패시브 §8- §e꿀꺽꿀꺽§f: 모든 §d회복 효과§f를 받을 수 없습니다. 기본적으로 체력을",
        " 빠르게 획득해, §a총 2초에 체력 1을 획득§f합니다. §8(§7틱당 0.025§8)",
        "§7철괴 우클릭 §8- §6두유 러버§f: 체력을 $[HEALTH_GAIN] 획득하고, $[DURATION]초간 §e꿀꺽꿀꺽§f이 봉인됩니다.",
        " 지속시간동안 근접 공격 쿨타임이 사라지며 근접 공격력이 §c$[DAMAGE_DECREASE]%§f 감소합니다.",
        " 이떼 근접 공격을 피격당한 적은 다음 기본 무적 시간이 초기화됩니다. $[COOLDOWN]",
		"§a[§e능력 제공자§a] §bduyu_999"
        },
        summarize = {
        ""
        })
public class SoyMilk extends AbilityBase {
	
	public SoyMilk(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> HEALTH_GAIN = 
			abilitySettings.new SettingObject<Double>(SoyMilk.class, "health-gain", 8.0,
            "# 체력 획득량") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> DURATION = 
			abilitySettings.new SettingObject<Integer>(SoyMilk.class, "duration", 30,
            "# 지속시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> DAMAGE_DECREASE = 
			abilitySettings.new SettingObject<Integer>(SoyMilk.class, "damage-decrease", 80,
            "# 공격력 감소율", "# 단위: %") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(SoyMilk.class, "cooldown", 90,
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
    
    
    
    
    
    private AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
    	public void run(int count) {
    		Healths.setHealth(getPlayer(), getPlayer().getHealth() + 0.025);
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    @SubscribeEvent(onlyRelevant = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
    	if (passive.isRunning()) e.setCancelled(true);
    }
    

}
