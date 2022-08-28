package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "서큐버스", rank = Rank.S, species = Species.UNDEAD, explain = {
        "§7철괴 우클릭 §8- §d달콤하게§f: $[RANGE]칸 내의 모든 적을 $[DURATION]초간 §d유혹§f합니다.",
        " 이 스킬은 $[COUNT]번 사용 후 쿨타임을 가집니다. $[COOLDOWN]",
        "§7철괴 좌클릭 §8- §c아찔하게§f: $[RANGE]칸 내의 자신 외 모든 적의 §3상태이상§f을",
        " 전부 §c출혈 효과§f로 변경합니다. 이 스킬은 §c쿨타임§f이 없습니다."
        },
        summarize = {
        "§7철괴 우클릭으로§f 주변 적들을 §d유혹§f합니다. $[COUNT]번 쓰면 쿨타임이 생깁니다.",
        "§7철괴 좌클릭으로§f 주변 적들의 §3상태이상§f을 전부 §c출혈§f 효과로 바꿉니다."
        })
public class Succubus extends AbilityBase {

	public Succubus(Participant participant) {
		super(participant);
	}
	
}
