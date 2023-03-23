package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "살인마 토끼", rank = Rank.L, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §c살의§f: 모든 공격력이 §c살의§f%만큼 증가합니다.",
		"§7패시브 §8- §5피의 향§f: 모든 플레이어의 현재 체력 비율을 볼 수 있습니다.",
		" 근접 공격 시 대상의 체력이 33% 이하라면 §c살의§f를 $[MURDER_GAIN]만큼 획득합니다.",
		" 만약 체력이 33%를 넘는다면 §c살의§f의 효과가 $[MULTIPLY]배가 되나 매번 §c살의§f를 $[MURDER_LOSE] 소모합니다.",
		"§7철괴 우클릭 §8- §4물어뜯기§f: 다음 근접 공격을 강화합니다. $[COOLDOWN]",
		" 강화된 공격은 §c살의§f 효과를 $[SKILL_MULTIPLY]배로 증폭시키고, 대상을 $[APPARENTDEATH_DURATION]초간 §7§n가사§f 상태로 만듭니다.",
		"§8[§7가사§8] §f체력이 33% 이하라면 체력이 고정되고, 공격 및 이동이 불가능해집니다."
		},
		summarize = {
		""
		})
public class KillerBunny extends AbilityBase {
	
	public KillerBunny(Participant participant) {
		super(participant);
	}
	
	
	
}
