package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "사냥꾼", rank = Rank.L, species = Species.HUMAN, explain = {
        "§7패시브 §8- §c먹이사슬§f: §c사냥감§f에게 주는 피해량이 대상이 잃은 체력에 비례해 증가합니다.",
        "§7철괴 우클릭 §8- §3덫§f: 자신의 위치에 덫을 설치합니다. §3덫§f을 밟은 플레이어는",
        " §c사냥감§f이 되고 §3덫§f으로부터 일정 범위 내로 벗어나지 못합니다.",
        " §3덫§f은 밟기 전까진 $[TRAP_NOTCATCHED_DURATION]초간 지속되고, 밟히고 나면 $[TRAP_CATCHED_DURATION]초간 지속됩니다.",
        " §3덫§f은 최대 $[MAX_COUNT]까지만 설치할 수 있습니다. $[COOLDOWN]",
        "§7철괴 좌클릭 §8- §c사냥터§f: $[RANGE]칸 내의 모든 생명체를 §c사냥감§f으로 취급합니다.",
        " 다시 좌클릭 시 4발의 탄환을 발사할 수 있으며, 공격 및 이동이 불가능합니다.",
        " 발사한 탄환은 내 최종 피해량의 $[DAMAGE]%를 입히고, 적중 시마다 $[DAMAGE_INCREASE]% 증가합니다.",
        " 모든 탄환을 적중시키면 체력을 $[HEALTH]만큼 회복합니다."
        },
        summarize = {
        ""
        })
public class Hunter extends AbilityBase {
	
	public Hunter(Participant participant) {
		super(participant);
	}

	
	
}
