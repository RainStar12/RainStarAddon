package RainStarAbility;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Fear;
import daybreak.abilitywar.utils.base.Formatter;

@AbilityManifest(name = "섀도우", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7패시브 §8- §9가장 오래된 감정§f: §b공포 상태§f의 적에게 주는 피해가 $[FEAR_DAMAGE_INCREASE]% 증가합니다.",
		"§7검 우클릭 §8- §5흑마술§f: $[DARKARTS_DURATION]초간 내게 피해를 주는 대상은 $[DARKARTS_FEAR]초간 §b공포 상태§f가 됩니다.",
		" §5흑마술§f이 끝날 때, §b공포§f를 준 대상 수 × §d$[HEAL_AMOUNT]HP§f를 §d회복§f합니다. $[DARKARTS_COOLDOWN]",
		"§7철괴 우클릭 §8- §c그림자 베기§f: $[SHADOW_DURATION]초간 §3그림자 상태§f가 되어, 적을 근접 공격하면",
		" 대상의 방향으로 짧게 돌진 이후 $[RANGE]칸 내의 적들을 $[SHADOW_FEAR]초간 공포에 빠뜨립니다.",
		" 이때 적을 처치하면 §3그림자 상태§f를 $[SHADOW_ADD_DURATION]초 증가시키고, §3그림자 상태§f가 끝날 때",
		" 천천히 줄어드는 §c공격력 $[SHADOW_DAMAGE_INCREASE]% × 처치 수§f를 획득합니다. $[SHADOW_COOLDOWN]",
		"§b[§7아이디어 제공자§b] §dspace_kdd"
		},
		summarize = {
		""
		})
public class Shadow extends AbilityBase {

	public Shadow(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> FEAR_DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Integer>(Shadow.class, "fear-damage-increase", 20,
			"# 공포 대상에게 피해량 증가 수치") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	public static final SettingObject<Double> DARKARTS_DURATION = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "darkarts-duration", 5.0,
			"# 흑마술 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> DARKARTS_FEAR = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "darkarts-fear", 3.0,
			"# 흑마술 공포 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> HEAL_AMOUNT = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "heal-amount", 4.0,
			"# 흑마술 종료 시 회복량") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DARKARTS_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Shadow.class, "darkarts-cooldown", 70,
			"# 흑마술 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
		
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Double> SHADOW_DURATION = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "shadow-duration", 10.0,
			"# 그림자 베기 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "range", 2.0,
			"# 그림자 베기 범위") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> SHADOW_FEAR = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "shadow-fear", 1.0,
			"# 흑마술 공포 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> SHADOW_ADD_DURATION = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "shadow-duration", 5.0,
			"# 그림자 베기 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> SHADOW_DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Integer>(Shadow.class, "shadow-damage-increase", 30,
			"# 그림자 베기 적 처치 공격력 증가 수치") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	public static final SettingObject<Integer> SHADOW_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Shadow.class, "shadow-cooldown", 110,
			"# 그림자 베기 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
		
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	//configs
	private final double feardamageincrease = 1 + (FEAR_DAMAGE_INCREASE.getValue() * 0.01);
	private final int shadowdamageincrease = SHADOW_DAMAGE_INCREASE.getValue();	
	private final int darkartsduration = (int) (DARKARTS_DURATION.getValue() * 20);
	private final int darkartsfear = (int) (DARKARTS_FEAR.getValue() * 20);
	private final int shadowduration = (int) (SHADOW_DURATION.getValue() * 20);
	private final int shadowaddduration = (int) (SHADOW_ADD_DURATION.getValue() * 20);
	private final int shadowfear = (int) (SHADOW_FEAR.getValue() * 20);	
	private final double healamount = HEAL_AMOUNT.getValue();
	private final double range = RANGE.getValue();
	private final Cooldown darkartscool = new Cooldown(DARKARTS_COOLDOWN.getValue());
	private final Cooldown shadowcool = new Cooldown(SHADOW_COOLDOWN.getValue());
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && e.getEntity() instanceof Player && getGame().isParticipating((Player) e.getEntity())) {
			Participant p = getGame().getParticipant((Player) e.getEntity());
			if (p.hasEffect(Fear.registration)) {
				
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
