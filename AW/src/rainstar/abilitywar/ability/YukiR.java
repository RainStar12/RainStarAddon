package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "유키", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §b만년설§f: §n상태이상§f을 받을 때 지속시간 절반의 §b§n빙결§f로 교체합니다.",
		"§7지팡이 우클릭 §8- §3아이스 볼트§f: 우클릭을 유지하는 동안 영창하여 이동 속도가 감소합니다.",
		" 우클릭을 해제할 때 §b냉기탄§f을 발사해 가까이의 적에게 발사됩니다.",
		" §b냉기탄§f의 상태이상 시간, 피해량, 유지 시간은 영창 시간에 비례합니다.",
		" §b냉기탄§f에 맞은 적은 §9§n냉기§f 상태이상을 받고, 이미 §9§n냉기§f라면 §b§n빙결§f시킵니다.",
		" 영창을 $[MAX_CASTING]초 이상 유지하면 영창이 취소되고 자신이 $[SELF_FROST]초 §b§n빙결§f됩니다.",
		"§7지팡이 좌클릭 §8- §9블리자드§f: 자신을 포함한 $[RANGE]칸 내 모든 생명체의 §b§n빙결§f을 깨뜨리는 한기를",
		" 퍼뜨립니다. 자신은 §b§n빙결§f 시간의 $[MULTIPLY]배만큼 §b신속 $[SPEED_AMPLIFIER]§f를 획득하고,",
		" 다른 생명체는 방어력이 2 감소하는 §b눈꽃 표식§f을 최대 $[MAX_SNOWFLAKE]까지 얻습니다.",
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

}
