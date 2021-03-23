package RainStarSynergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;

@AbilityManifest(
		name = "마왕", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7패시브 §8- §8어둠의 지도자§f: 자신의 위치가 어두우면 어두울수록",
		" 스킬 피해량이 증가하고, 밝기 레벨이 5보다도 낮으면 힘과 신속 버프를 얻습니다.",
		" 실명을 가지고 있는 적을 공격할 때 대상에게 §c추가 피해§f를 입힙니다.",
		"§7철괴 우클릭 §8- §c암전§f: 어둠의 공간을 만들어내 공간 내의 모든 플레이어에게",
		" 실명 효과를 $[BLIND_DURATION]초, 공포 효과를 $[FEAR_DURATION]초간 부여합니다. $[COOLDOWN]",
		" 이후 공간이 사라지기 전까지 공간 내에서 자유로이 날 수 있습니다.",
		"§7검 들고 F §8- §b시공간 절단§f: 스킬을 사용한 시점으로부터 모든 플레이어의",
		" 이동 흔적이 남고, 다시 능력을 사용 혹은 자동 중단할 때 범위 내의",
		" 모든 시간의 흔적의 원 주인에게 피해를 입히며 차원의 저편으로 보내버립니다.",
		"§7상태이상 §8- §0공포§f: 공포를 건 대상을 쳐다보면 이동 속도가 급감합니다.",
		" 매 3초마다 공포를 건 대상의 반대 방향을 쳐다보게 됩니다."
		})

public class DemonLord extends Synergy {

	public DemonLord(Participant participant) {
		super(participant);
	}
	
	
	
	
	
}
