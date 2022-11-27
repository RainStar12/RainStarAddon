package RainStarAbility;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "사냥꾼", rank = Rank.L, species = Species.HUMAN, explain = {
        "§7패시브 §8- §c먹이사슬§f: 사냥감에게 주는 피해량이 대상이 잃은 체력에 비례해 증가합니다.",
        "§7철괴 좌클릭 §8- §3덫§f: 자신의 위치에 덫을 설치합니다. 흐릿하게 보이는 덫 주변으로",
        " 다가온 플레이어는 덫에 걸려 덫으로부터",
        ""
        
        },
        summarize = {
        ""
        })
public class Hunter extends AbilityBase {
	
	public Hunter(Participant participant) {
		super(participant);
	}

	
	
}
