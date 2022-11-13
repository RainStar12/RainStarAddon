package RainStarAbility;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.ClickType;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.ability.event.AbilityActiveSkillEvent;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;

@AbilityManifest(name = "선견지명", rank = Rank.S, species = Species.HUMAN, explain = {
		"검 들고 F키로 바라보고 있는 대상을 $[FORESIGHT_DURATION]초간 지정합니다. $[COOLDOWN]",
		"대상이 §a액티브 §3/ §6타게팅 §f스킬을 사용할 때 해당 스킬을 캔슬시키고,",
		"그 스킬을 $[SKILL_DURATION]초간 자신이 대신 사용합니다. §8(§7패시브도 획득 가능§8)"
		},
		summarize = {
		""
		})
public class Foresight extends AbilityBase implements ActiveHandler, TargetHandler {
	
	public Foresight(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Foresight.class, "cooldown", 77,
			"# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
		
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Double> FORESIGHT_DURATION = 
			abilitySettings.new SettingObject<Double>(Foresight.class, "darkarts-duration", 5.0,
			"# 지정 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> SKILL_DURATION = 
			abilitySettings.new SettingObject<Double>(Foresight.class, "darkarts-duration", 10.0,
			"# 복제한 스킬 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final int foresightdur = (int) (FORESIGHT_DURATION.getValue() * 20);
	private final int skilldur = (int) (SKILL_DURATION.getValue() * 20);
	
	private Player target = null;
	
	@SubscribeEvent
	public void onSwap(PlayerSwapHandItemsEvent e) {
		
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		return false;
	}
	
	@Override
	public void TargetSkill(Material material, LivingEntity entity) {
		
	}
	
	@SubscribeEvent
	public void onPreActive(AbilityPreActiveSkillEvent e) {
		
	}
	
	

}
