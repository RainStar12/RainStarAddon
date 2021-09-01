package RainStarSynergy.chance;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;

@AbilityManifest(name = "기회", rank = Rank.L, species = Species.GOD, explain = {
		"게임 시작 시 시너지 능력의 모든 플레이어는 세 가지 선택지 중 하나를 택할 수 있는",
		"GUI를 오픈합니다. 이 선택지에는 §a기존 능력§f, §b새로운 무작위의 능력§f,",
		"§7능력명과 능력 설명이 보이지 않는 무작위 능력§f이 존재하며",
		"플레이어는 §a30§f초 내에 능력을 선택하여 나올 수 있습니다.",
		"기회에게는, 기존 능력 선택지가 없고 대신 무작위 2개와 블라인드 1개가 더 생깁니다."
		})

public class Chance extends Synergy {
	
	public Chance(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (!getGame().hasModule(AbilityChanceGUI.class)) {
				getGame().addModule(new AbilityChanceGUI(getGame()));
			}
		}
	}
	
}