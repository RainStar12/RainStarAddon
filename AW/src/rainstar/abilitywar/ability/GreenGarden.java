package rainstar.abilitywar.ability;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.google.common.collect.ImmutableMap;

@AbilityManifest(name = "그린 가든", rank = Rank.S, species = Species.OTHERS, explain = {
		"형형색색의 §d꽃§f의 §6씨앗§f을 최대 $[MAX_SEED]개까지 소지합니다.", 
		"§6씨앗§f을 전부 사용하면 $[RECHARGE]초 후 보급됩니다.", 
		"§7철괴 우클릭§f으로 제자리에 §6씨앗§f을 심어 색에 맞는 §d꽃§f을 $[BLOOMING_WAIT]초 후 피워냅니다.", 
		"§d꽃§f의 종류에 따라 $[FLOWER_EFFECT_DURATION]초간 $[RANGE]칸 내 생명체에게 효과를 부여합니다.", 
		"§a긍정 효과§f라면 아군에게, §c부정 효과§f라면 적에게 적용됩니다.", 
		"§4양귀비 §c§n중독§7 | §a민들레 §a회복속도 증가§7 | §5파꽃 §c받는 피해량 증가", 
		"§b난초 §a신속§7 | §f선애기별꽃 §a저항 및 넉백 제거§7 | §d라일락 §c§n몽환", 
		"§6튤립 §a공격력 증가§7 | §c장미 §a피해량 반사§7 | §e해바라기 §c§n유혹"
		})
public class GreenGarden extends AbilityBase {
	
	public GreenGarden(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> MAX_SEED = 
			abilitySettings.new SettingObject<Integer>(GreenGarden.class, "max-seed", 3,
            "# 소지 가능한 최대 씨앗 개수") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> RECHARGE = 
			abilitySettings.new SettingObject<Integer>(GreenGarden.class, "recharge-time", 45,
            "# 씨앗 보급까지 걸리는 시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Double> BLOOMING_WAIT = 
			abilitySettings.new SettingObject<Double>(GreenGarden.class, "blooming-wait", 3.0,
            "# 개화까지 걸리는 시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Double> FLOWER_EFFECT_DURATION =
			abilitySettings.new SettingObject<Double>(GreenGarden.class, "flower-effect-duration", 6.3,
            "# 효과 지속시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
    private final int maxseed = MAX_SEED.getValue();
    private final int recharge = RECHARGE.getValue();
    private final int bloomingwait = (int) (BLOOMING_WAIT.getValue() * 20);
    
    
    enum Seed {
    	POPPY("§4⧫", false, Material.POPPY) {
    		protected void onPlanted(Player owner) {
    			new AbilityTimer(TaskType.REVERSE, (int) (FLOWER_EFFECT_DURATION.getValue() * 20)) {
    				
    				@Override
    				public void run(int count) {
    					
    				}
    				
				}.setPeriod(TimeUnit.TICKS, 1).start();
    		}
    	},
    	DANDELION("§a⧫", true, Material.DANDELION) {
    		protected void onPlanted(Player owner) {
    			new AbilityTimer(TaskType.REVERSE, (int) (FLOWER_EFFECT_DURATION.getValue() * 20)) {
    				
    				@Override
    				public void run(int count) {
    					
    				}
    				
				}.setPeriod(TimeUnit.TICKS, 1).start();
    		}
    	},
    	ALLIUM("§5⧫", false, Material.ALLIUM) {
    		protected void onPlanted(Player owner) {
    			new AbilityTimer(TaskType.REVERSE, (int) (FLOWER_EFFECT_DURATION.getValue() * 20)) {
    				
    				@Override
    				public void run(int count) {
    					
    				}
    				
				}.setPeriod(TimeUnit.TICKS, 1).start();
    		}
    	},
    	ORCHID("§b⧫", true, Material.BLUE_ORCHID) {
    		protected void onPlanted(Player owner) {
    			new AbilityTimer(TaskType.REVERSE, (int) (FLOWER_EFFECT_DURATION.getValue() * 20)) {
    				
    				@Override
    				public void run(int count) {
    					
    				}
    				
				}.setPeriod(TimeUnit.TICKS, 1).start();
    		}
    	},
    	AZURE_BLUET("§f⧫", true, Material.AZURE_BLUET) {
    		protected void onPlanted(Player owner) {
    			new AbilityTimer(TaskType.REVERSE, (int) (FLOWER_EFFECT_DURATION.getValue() * 20)) {
    				
    				@Override
    				public void run(int count) {
    					
    				}
    				
				}.setPeriod(TimeUnit.TICKS, 1).start();
    		}
    	},
    	LILAC("§d⧫", false, Material.LILAC) {
    		protected void onPlanted(Player owner) {
    			new AbilityTimer(TaskType.REVERSE, (int) (FLOWER_EFFECT_DURATION.getValue() * 20)) {
    				
    				@Override
    				public void run(int count) {
    					
    				}
    				
				}.setPeriod(TimeUnit.TICKS, 1).start();
    		}
    	},
    	TULIP("§6⧫", true, Material.ORANGE_TULIP) {
    		protected void onPlanted(Player owner) {
    			new AbilityTimer(TaskType.REVERSE, (int) (FLOWER_EFFECT_DURATION.getValue() * 20)) {
    				
    				@Override
    				public void run(int count) {
    					
    				}
    				
				}.setPeriod(TimeUnit.TICKS, 1).start();
    		}
    	},
    	ROSE("§c⧫", true, Material.ROSE_BUSH) {
    		protected void onPlanted(Player owner) {
    			new AbilityTimer(TaskType.REVERSE, (int) (FLOWER_EFFECT_DURATION.getValue() * 20)) {
    				
    				@Override
    				public void run(int count) {
    					
    				}
    				
				}.setPeriod(TimeUnit.TICKS, 1).start();
    		}
    	},
    	SUNFLOWER("§e⧫", false, Material.SUNFLOWER);
    	
    	private final String seedcolor;
    	private final boolean positive;
    	private final Material flower;
    	
    	Seed(String seedcolor, boolean positive, Material flower) {
    		this.seedcolor = seedcolor;
    		this.positive = positive;
    		this.flower = flower;
    	}
    	
    	protected abstract void onPlanted(Player owner);
    	
    }
    
	
}
