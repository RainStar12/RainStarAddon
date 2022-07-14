package RainStarAbility;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "귤즙", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7게이지 §8- §e과즙§f: $[PERIOD]초마다 한 칸씩 최대 10칸을 보유 가능합니다.", 
		"§7화살 발사 §8- §6귤 화살§f: 적중 위치에 과즙 장판이 터집니다. $[ARROW_CONSUME]",
		" 만일 생명체를 적중시킨다면 $[GAUGE_RETURN]칸만큼 §e과즙 게이지§f를 돌려받습니다.",
		" 장판 위의 적은 §3끌리거나 밀리는§8(§7벡터§8)§f 효과가 대폭 감소합니다.",
		" 장판 위 적에게 주는 §b원거리 피해§f가 $[PROJECTILE_DAMAGE_INCREASE]% 증가합니다.",
		"§7철괴 우클릭 §8- §c과즙 폭발§f: 모든 과즙 게이지를 전부 소모하여 제자리에 터뜨려",
		" $[RANGE]칸 내의 적을 강하게 밀쳐내고 §7실명§f시킵니다. 이때 잠시 빨라집니다.",
		" 모든 효과§8(§7넉백, 실명, 신속§8)§f의 세기는 게이지에 비례합니다."
		})

public class TangerineJuice extends AbilityBase {
	
	public TangerineJuice(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> PERIOD = 
			abilitySettings.new SettingObject<Double>(TangerineJuice.class, "period", 5.0,
			"# 과즙이 차오르는 주기", "# WRECK 효과 50%까지 적용") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> ARROW_CONSUME = 
			abilitySettings.new SettingObject<Integer>(TangerineJuice.class, "juice-consume:arrow", 3,
			"# 화살에 소모할 과즙 게이지") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§c과즙 소모 §7: §b" + getValue();
        }

	};
	
	public static final SettingObject<Integer> GAUGE_RETURN = 
			abilitySettings.new SettingObject<Integer>(TangerineJuice.class, "gauge-return", 1,
			"# 생명체 적중 시 돌려받는 과즙량") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> PROJECTILE_DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Integer>(TangerineJuice.class, "projectile-damage-increase", 25,
			"# 장판 위 적에게 주는 추가 대미지") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(TangerineJuice.class, "range", 3.0,
			"# 과즙 폭발 사거리") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final int consume = ARROW_CONSUME.getValue();
	private ActionbarChannel ac = newActionbarChannel();
	private int juicegauge = 0;
	
	private final int period = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(60) * (PERIOD.getValue() * 20) : (PERIOD.getValue() * 20));
	
	private Set<Projectile> arrows = new HashSet<>();
	
	private AbilityTimer juice = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (period == 0) juicegauge = 10;
			else if (count % period == 0) juicegauge = Math.min(10, juicegauge + 1);
			ac.update(Strings.repeat("§7/", 10 - juicegauge) + Strings.repeat("§6/", juicegauge));
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
    @SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
    	if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
    		if (juicegauge >= consume) {
    			juicegauge -= consume;
    			arrows.add(e.getEntity());
    		}
    	}
    }
	

}
