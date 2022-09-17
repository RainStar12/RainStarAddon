package RainStarGame.SelectMix;

import java.util.StringJoiner;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.random.Random;

@AbilityManifest(name = "null", rank = Rank.C, species = Species.SPECIAL, explain = {
		"$(EXPLAIN)"
		},
		summarize = {
		"NullPointerException"
		})
public class Null extends AbilityBase {
	
	public Null(Participant participant) {
		super(participant);
	}
	
	private final Random random = new Random();
	
	@SuppressWarnings("unused")
	private final Object EXPLAIN = new Object() {
		@Override
		public String toString() {
			final StringJoiner joiner = new StringJoiner("\n");
			switch(random.nextInt(6)) {
			case 0:
				joiner.add("능력을 고르지 않으셨군요? 그런 당신에게 아무 능력도 드리지 않습니다!");
				break;
			case 1:
				joiner.add("때때로 당신은 옳은 선택지는 하나도 없고 잘못된 선택지에서만 선택을 해야 할 수도 있다.");
				joiner.add("이런 상황에서는 최대한 덜 잘못된 선택지를 택하라. §o- Colleen Hoover");
				break;
			case 2:
				joiner.add("도망쳐서 도착한 곳에 낙원은 없다. §o-베르세르크");
				break;
			case 3:
				joiner.add("참 §a우유부단§f하시네요.");
				joiner.add("§a우유부단§f도 이거보단 좋을 걸?");
				break;
			case 4:
				joiner.add("엄청난 이스터에그!");
				joiner.add("네, 그게 끝입니다.");
				break;
			case 5:
				joiner.add("네가 안 고른 능력이다. 악으로 깡으로 버텨라.");
				break;
			}
			return joiner.toString();
		}
	};

}
