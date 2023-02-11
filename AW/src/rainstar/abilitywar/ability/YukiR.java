package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Frost;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;

@AbilityManifest(name = "유키 R", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §b만년설§f: §b§n빙결§f 외 §n상태이상§f을 받을 때 지속시간 절반의 §b§n빙결§f로 교체합니다.",
		"§7지팡이 우클릭 §8- §3아이스 볼트§f: 우클릭을 유지하는 동안 영창합니다. $[COOLDOWN]",
		" 우클릭을 해제할 때 §b냉기탄§f을 발사해 가까이의 적에게 발사됩니다.",
		" §b냉기탄§f의 상태이상 시간, 피해량, 유지 시간은 영창 시간에 비례합니다.",
		" §b냉기탄§f에 맞은 적은 §9§n냉기§f 상태이상을 받고, 이미 §9§n냉기§f라면 §b§n빙결§f시킵니다.",
		" 영창을 $[MAX_CASTING]초 이상 유지하면 영창이 취소되고 자신이 $[SELF_FROST]초 §b§n빙결§f됩니다.",
		"§7지팡이 좌클릭 §8- §9블리자드§f: 자신을 포함한 $[RANGE]칸 내 모든 생명체의 §b§n빙결§f을 깨뜨리는 한기를",
		" 퍼뜨립니다. 자신은 §b§n빙결§f 시간의 $[MULTIPLY]배만큼 §b신속 $[SPEED_AMPLIFIER]§f를 획득하고,",
		" 다른 생명체는 방어력이 2 감소하는 §b눈꽃 표식§f을 최대 3까지 얻습니다.",
		"§1[§9냉기§1]§f 이동 속도, 공격 속도, 회복력이 감소합니다.",
		" 지속시간과 쿨타임 타이머가 천천히 흐릅니다."
		},
		summarize = {
		""
		})
public class YukiR extends AbilityBase {
	
	public YukiR(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Double> MAX_CASTING = 
			abilitySettings.new SettingObject<Double>(YukiR.class, "max-casting", 7.5,
            "# 최대 영창시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Double> SELF_FROST = 
			abilitySettings.new SettingObject<Double>(YukiR.class, "self-frost", 3.0,
            "# 영창 캔슬 시 자체 빙결", "# 단위: 초") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(YukiR.class, "range", 8.0,
            "# 블리자드 범위", "# 단위: 칸") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };

	public static final SettingObject<Double> MULTIPLY = 
			abilitySettings.new SettingObject<Double>(YukiR.class, "multiply", 3.0,
            "# 신속 지속시간", "# 단위: 배") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> SPEED_AMPLIFIER = 
			abilitySettings.new SettingObject<Integer>(YukiR.class, "speed-amplifier", 1,
            "# 신속 포션 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
		@Override
		public String toString() {
			return "" + (1 + getValue());
        }
    };
    
	
    
    
    
    
	@SubscribeEvent(onlyRelevant = true)
	public void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		if (e.getEffectType().
				) {
			
		}
	}
    
}
