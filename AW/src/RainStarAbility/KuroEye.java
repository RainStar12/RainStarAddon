package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(
		name = "쿠로「개안」", rank = Rank.S, species = Species.OTHERS, explain = {
		"마안의 힘을 통제하지 못하는 §8어둠§f 속성의 검사다.",
		"§7패시브 §8- §7이명 §8『§7어둠 군주§8』§f: 어둠에서 자라, 어둠의 힘을 사용하기에",
		" 현재 내가 있는 위치가 어두우면 어두울수록 나의 스킬이 강력해진다.",
		"§7검 우클릭 §8- §7발현 §2『§a역행의 검날§2』§f: 시간의 흐름을 거꾸로 타고난 역행의 검날은",
		" 3초간 검날의 시간을 거슬러가며 타격했던 적에게 동일한 피해를 다시 입히는",
		" 성질이 있다. 능력 사용 후 검이 있어야 할 현실의 시간까지 되돌아오는 6초간",
		" 나는 검을 이용해 피해를 입힐 수 없다. $[REVERSE_COOLDOWN]",
		"§7검 들고 F §8- §7비기 §3『§b시간 절단§3』§f: 시간의 틈새에 참격을 끼워넣는 기술로,",
		" 우선 검의 힘을 끌어모으기 위해 10초를 대기한다. 대기 이후에 바라보는 방향으로",
		" 거대한 참격을 날려 검의 힘을 모을 때부터 단 한 번이라도 과거에",
		" 그 위치에서 존재했던 자들의 시간을 뒤틀어 큰 피해를 입힐 수 있다. $[TIMECUTTER_COOLDOWN]",
		"§7패시브 §8- §4『§c마안 폭주§4』§f: 나는 저주받은 마안을 사용하기에,",
		" 회복을 받을 수 없는 몸이 되었지만 회복을 하려 할 때마다 대신",
		" 회복량의 10%만큼 추가 공격력을 최대 10까지 획득할 수 있다."
		})

public class KuroEye extends AbilityBase {

	public KuroEye(Participant participant) {
		super(participant);
	}
	
}
