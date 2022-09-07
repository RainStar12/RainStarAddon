package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(
		name = "모로스", rank = Rank.S, species = Species.GOD, explain = {
		"피할 수 없는 운명의 신, 모로스.",
		"§7게임 시작 §8- §9운명론§f: 자신의 모든 수치화된 효과는 오차범위가 존재합니다.",
		" 오차범위는 게임 시작 시 정해지며, 바뀔 수 없습니다.",
		"§7적 타격 §8- §c필멸§f: $[MORTAL_DURATION]$[MORTAL_DURATION_ERROR]초 이내에 공격했던 대상이 사망 위기에 처했을 때,",
		" 대상은 §c§n그 어떠한 방법으로도§f 죽음을 피할 수 없습니다.",
		"§7패시브 §8- §3운명 개찬§f: $[RANGE]$[RANGE_ERROR]칸 내의 플레이어가 액티브 / 타게팅 스킬 발동 전",
		" $[DURATION]초간 타게팅 불가 상태가 됩니다. $[PASSIVE_COOLDOWN]$[PASSIVE_COOLDOWN_ERROR]",
		"§7철괴 우클릭 §8- §b변수 제거§f: §c필멸§f을 전부 §c제압§f으로 바꿉니다. $[ACTIVE_COOLDOWN]$[ACTIVE_COOLDOWN_ERROR]"
		},
		summarize = {
		""
		})

public class MorosR extends AbilityBase {

	public MorosR(Participant participant) {
		super(participant);
	}
	
	
	
}
