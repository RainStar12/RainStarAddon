package rainstar.abilitywar.ability;

import org.bukkit.Material;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@AbilityManifest(name = "훌륭한 대화수단", rank = Rank.S, species = Species.OTHERS, explain = {
		"§6벽돌§f을 들고 있는 동안 게이지바가 나타나, 효과의 세기가 조절됩니다.",
		"§6벽돌§f로 타격 시 대상의 체력을 감소§8(§7최대 $[MAX_HEALTHDOWN]%§8)§f시키고, §b§n공포§8(§7최대 $[MAX_FEAR]초§8)§f에 빠뜨립니다.",
		"위 효과는 §4치명타§f가 적용됩니다. $[COOLDOWN]",
		"§a[§e능력 제공자§a] §cDdun_kim"
		},
		summarize = {
		""
		})
public class GreatCommunicator extends AbilityBase {

    public GreatCommunicator(Participant participant) {
        super(participant);
    }
    
    

    public AbilityTimer handchecker = new AbilityTimer() {

        @Override
        public void run(int count) {
            if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.IRON_INGOT)) {

            }
        }

    }.setPeriod(TimeUnit.TICKS, 1).register();

}