package RainStarSynergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;

@AbilityManifest(
		name = "���� ����", rank = Rank.L, species = Species.UNDEAD, explain = {
		"��7���� ���� ��8- ��c��ĥ����f: ���� ü���� 50%���� ���� �� ��c�߰� ���ء�8(��7Max ��1.75��8)��f��,",
		" ���� ü���� 50%���� ���� �� �⺻ ������� ���ҡ�8(��7Min ��0.75��8)��f�մϴ�.",
		" ���� �������� ���� �׿��� ��� ���� HP�� $[HEAL_AMOUNT]%�� ȸ���� �� �ֽ��ϴ�.",
		"��7�нú� ��8- ��c�� ������f: ��� ���� ü���� Ȯ���� �� �ֽ��ϴ�.",
		" �ٴڿ� ��ħ���� ����, ���� ü���� ���� ���� ��ġ�� �˷��ݴϴ�.",
		"��7ö�� Ÿ���� ��8- ��c���� �⿬��f: $[RANGE]ĭ �̳��� ���� �ٶ󺸰� ö���� ��Ŭ���ϸ�",
		" ����� �������� ������ ����, ���� ü�� $[EXECUTION_HEALTH]% ������ ���� ó���մϴ�. $[COOLDOWN]",
		" ó���� �� �ϳ��� �ִ� ü���� ����ϸ�, �ƹ��� ó������ ���� ���",
		" $[STUN_DURAITON]�ʰ� ���� ���¿� ������ ��Ÿ���� 2��� �����ϴ�."
		})

public class Bloodlust extends Synergy {
	
	public Bloodlust(Participant participant) {
		super(participant);
	}
	
}
