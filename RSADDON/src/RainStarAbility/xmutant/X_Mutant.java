package RainStarAbility.xmutant;

import java.util.List;
import java.util.stream.Collectors;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.random.Random;

@AbilityManifest(name = "변이체 X", rank = Rank.L, species = Species.OTHERS, explain = {
		"게임 시작 시, 이 능력은 기존 능력의 §2변종§f 능력 중 하나로 변경됩니다.",
		"§2변종§f 능력은 기존 능력이 §cSPECIAL§f 등급으로 강화된 능력입니다."
		},
		summarize = {
		"게임 시작 시, 이 능력은 기존 능력의 §2변종§f 능력 10가지 중 하나로 변경됩니다.",
		"§2변종§f 능력은 기존 능력이 §cSPECIAL§f 등급으로 강화된 능력입니다."
		})
public class X_Mutant extends AbilityBase {
	
	public X_Mutant(Participant participant) {
		super(participant);
	}

	private final Random random = new Random();
	
	public AbilityRegistration getRandomXAbility() {
		final List<AbilityRegistration> registrations = AbilityList.values().stream().filter(
				ability -> ability.getManifest().name().startsWith("X-")
		).collect(Collectors.toList());
		return registrations.isEmpty() ? null : random.pick(registrations);
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			try {
				getParticipant().setAbility(getRandomXAbility());
				getPlayer().sendMessage(Formatter.formatAbilityInfo(getParticipant().getAbility()).toArray(new String[0]));
			} catch (UnsupportedOperationException | ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
	}
	
}