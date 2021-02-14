package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(
		name = "쿠로「개안」", rank = Rank.S, species = Species.OTHERS, explain = {
		"§7패시브 §8- §7이명 §8『어둠 군주』§f: 어둠에서 자라, 어둠의 힘을 사용하기에",
		" 현재 내가 있는 위치가 어두우면 어두울수록 나의 스킬이 강력해진다.",
		"§7근접 타격 후 F §8- §7비기 §3『시간 절단』§f: 시간의 틈새에 참격을 끼워넣는 기술로,",
		" 우선 검의 힘을 끌어모으기 위해 10초를 대기한다. 대기 이후에 바라보는 방향으로",
		" 거대한 참격을 날려 검의 힘을 모을 때부터 단 한 번이라도 과거에",
		" 그 위치에서 존재했던 자들의 시간을 뒤틀어 큰 피해를 입힐 수 있다. $[SWORD_COOLDOWN]",
		"§7패시브 §8- §c『마안 폭주』§f: 나는 저주받은 마안을 사용하기에,",
		" 회복을 받을 수 없는 몸이 되었지만 회복을 하려 할 때마다 대신",
		" 회복량의 10%만큼 추가 공격력을 최대 10까지 획득할 수 있다."
		})

public class KuroEye extends AbilityBase {

	public KuroEye(Participant participant) {
		super(participant);
	}
	
}
