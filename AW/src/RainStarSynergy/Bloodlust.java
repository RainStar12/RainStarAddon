package RainStarSynergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;

@AbilityManifest(
		name = "피의 갈망", rank = Rank.L, species = Species.UNDEAD, explain = {
		"§7근접 공격 §8- §c피칠갑§f: 적의 체력이 50%보다 적을 때 §c추가 피해§8(§7Max ×1.75§8)§f를,",
		" 적의 체력이 50%보다 많을 때 기본 대미지가 감소§8(§7Min ×0.75§8)§f합니다.",
		" 근접 공격으로 적을 죽였을 경우 잃은 HP의 $[HEAL_AMOUNT]%를 회복할 수 있습니다.",
		"§7패시브 §8- §c피 냄새§f: 모든 적의 체력을 확인할 수 있습니다.",
		" 바닥에 나침반이 생겨, 가장 체력이 적은 적의 위치를 알려줍니다.",
		"§7철괴 타게팅 §8- §c피의 향연§f: $[RANGE]칸 이내의 적을 바라보고 철괴를 우클릭하면",
		" 대상의 방향으로 빠르게 돌진, 이후 체력 $[EXECUTION_HEALTH]% 이하의 적을 처형합니다. $[COOLDOWN]",
		" 처형된 적 하나당 최대 체력이 상승하며, 아무도 처형하지 못할 경우",
		" $[STUN_DURAITON]초간 기절 상태에 빠지고 쿨타임을 2배로 가집니다."
		})

public class Bloodlust extends Synergy {
	
	public Bloodlust(Participant participant) {
		super(participant);
	}
	
}
