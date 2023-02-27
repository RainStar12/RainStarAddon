package rainstar.abilitywar.ability;

import org.bukkit.Material;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@AbilityManifest(name = "훌륭한 대화수단", rank = Rank.S, species = Species.OTHERS, explain = {
		"§6벽돌§f을 들고 있는 동안 게이지바가 나타나, 효과의 세기가 조절됩니다.",
		"§6벽돌§f로 타격 시 대상의 최대 체력 비례 고정 피해§8(§7최대 $[MAX_TRUEDMG]%§8)§f를 입히고,",
		"§b§n공포§8(§7최대 $[MAX_FEAR]초§8)§f에 빠뜨립니다. 위 효과는 §4치명타§f가 적용됩니다. $[COOLDOWN]",
		"§a[§e능력 제공자§a] §cDdun_kim"
		},
		summarize = {
		""
		})
public class GreatCommunicator extends AbilityBase {

    public GreatCommunicator(Participant participant) {
        super(participant);
    }
    
	public static final SettingObject<Integer> MAX_TRUEDMG = 
			abilitySettings.new SettingObject<Integer>(GreatCommunicator.class, "max-true-damage", 15,
			"# 최대 고정 피해량 배율", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> MAX_FEAR = 
			abilitySettings.new SettingObject<Double>(GreatCommunicator.class, "max-fear", 2.5,
			"# 최대 공포 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(GreatCommunicator.class, "cooldown", 40,
			"# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	private final double maxtruedmg = MAX_TRUEDMG.getValue() * 0.01;
	private final int maxfear = (int) (MAX_FEAR.getValue() * 20);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());

    public AbilityTimer handchecker = new AbilityTimer() {

        @Override
        public void run(int count) {
            if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.IRON_INGOT)) {
            	
            }
        }

    }.setPeriod(TimeUnit.TICKS, 1).register();

}