package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "유키", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7지팡이 우클릭 §8- §b프로스트바이트§f: 주변의 한기를 최대 10까지 끌어모읍니다.",
		" 다시 우클릭하면 가까운 적에게 유도되는 냉기 투사체를 발사해, 적중한 적을 냉기시킵니다.",
		" 모은 한기에 따라서 투사체의 지속시간과 냉기 지속시간이 증가합니다.",
		" 10초 이상 한기를 끌어모으면, 스킬이 취소되고 본인은 빙결 상태이상에 걸립니다.", 
		"§7지팡이 좌클릭 §8- §b앱솔루트 제로§f: 주변의 모든 빙결 상태이상을 가진 플레이어를",
		" 얼음을 깨트려 마법 대미지를 입힙니다. 또한 대상에게 방어력이 2 감소하는",
		" 눈꽃 표식을 20초간 부여시킵니다. $[BREAK_COOLDOWN]",
		" 눈꽃 표식은 최대 5번까지 중첩됩니다.",
		"§7상태이상 §8- §9냉기§f: 이동 속도, 공격 속도가 감속하고 회복력이 줄어듭니다.",
		" 또한 지속시간과 쿨타임 타이머가 천천히 흐르게 됩니다."
		},
		summarize = {
		""
		})
public class YukiR extends AbilityBase {
	
	public YukiR(Participant participant) {
		super(participant);
	}

}
