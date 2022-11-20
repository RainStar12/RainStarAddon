package RainStarAbility;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Zombie;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@AbilityManifest(name = "X-Infected", rank = Rank.S, species = Species.UNDEAD, explain = {
		"철괴 우클릭 시 $[DURATION]초간 §2좀비§f가 되어 컨트롤이 불가능해집니다.",
		"§2좀비§f는 내 최종 공격력의 §c$[DAMAGE]%§f의 피해를 주고, 받는 피해가 $[DECREASE]% 감소합니다.",
		"§2좀비§f와 자신의 체력은 공유되고, §2좀비화§f 중에는 §d회복 효과§f가 §c피해 효과§f가 됩니다."
		},
		summarize = {
		""
		})
public class XInfected extends AbilityBase {
	
	public XInfected(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> DURATION = 
			abilitySettings.new SettingObject<Double>(XInfected.class, "zombie-duration", 13.0,
            "# 좀비화 지속 시간") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> DAMAGE = 
			abilitySettings.new SettingObject<Integer>(XInfected.class, "damage-percentage", 115,
            "# 좀비가 주는 피해량 비율") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> DECREASE = 
			abilitySettings.new SettingObject<Integer>(XInfected.class, "damage-decrease", 20,
            "# 좀비화 간 받는 피해량 감소율") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
    private final int duration = (int) (DURATION.getValue() * 20);
    private final double damage = DAMAGE.getValue() * 0.01;
    private final double decrease = 1 - (DECREASE.getValue() * 0.01);
    
    private AbilityTimer infected = new AbilityTimer(duration) {
    	
    	Zombie zombie;
    	
    	@Override
    	public void onStart() {
    		zombie = getPlayer().getWorld().spawn(getPlayer().getLocation(), Zombie.class);
			zombie.setAdult();
			zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.45);
			
			zombie.getEquipment().setItemInMainHand(null);
			zombie.getEquipment().setArmorContents(getPlayer().getInventory().getArmorContents());
			zombie.getEquipment().setItemInMainHandDropChance(0);
			zombie.getEquipment().setBootsDropChance(0);
			zombie.getEquipment().setChestplateDropChance(0);
			zombie.getEquipment().setHelmetDropChance(0);
			zombie.getEquipment().setLeggingsDropChance(0);
			zombie.setCustomName("§2" + getPlayer().getName());
			zombie.setCustomNameVisible(true);
			zombie.setConversionTime(Integer.MAX_VALUE);
    	}
    	
    	@Override
    	public void run(int count) {
    		
    	}
    	
    	@Override
    	public void onEnd() {
    		
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();

}
