package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "추적자", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7나침반 타격 §8- §c추적 장치§f: 대상에게 추적 장치를 부착합니다.",
		" 나침반을 통해 대상의 방향을 파악할 수 있습니다.",
		""
		},
		summarize = {
		"$[MAX_SEED]개의 §d꽃§f 씨앗을 지니고 시작하며 전부 사용하면 $[RECHARGE]초 후 보급됩니다.",
		"§7철괴 좌클릭§f으로 제자리에 §6씨앗§f을 심어 색에 맞는 §d꽃§f을 $[BLOOMING_WAIT]초 후 피워냅니다.",
		"§d꽃§f 종류에 따라 존재하는 효과를 주변 대상에게 부여합니다. §8(§a긍정 §7- §a아군§7, §c부정 §7- §c적군§7§8)"
		})
public class Chaser extends AbilityBase {
	
	public Chaser(Participant participant) {
		super(participant);
	}

}
