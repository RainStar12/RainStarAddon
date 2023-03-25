package rainstar.abilitywar.ability;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "다단히트", rank = Rank.A, species = Species.HUMAN, explain = {
		"근접 공격 피해를 입힐 때 $[DAMAGE_CONFIG]%의 피해로 $[COUNT_CONFIG]번 공격합니다.",
		"다단히트 효과 발동 중에는 효과를 중복 발동할 수 없습니다.",
		"또한 생명체에게 피해를 입을 때마다 $[INV_CONFIG]초간 무적 상태가 됩니다."
		},
		summarize = {
		"근접 공격 시 공격력이 감소한 채로 $[COUNT_CONFIG]번 때립니다.",
		"피해를 입을 때마다 순간 무적 상태가 됩니다."
		})

@Tips(tip = {
        "대미지가 일시적으로 낮아지지만, 총 3번을 공격하기에 최종적으로",
        "1.5배의 피해를 입힐 수 있는 능력입니다. 또한 반칸 이하의 피해를",
        "전부 무시할 수 있어 다단히트 능력에게 강합니다."
}, strong = {
        @Description(subject = "방어력이 낮은 대상", explain = {
        		"낮은 대미지로 여러 번 타격하기 때문에,",
        		"방어력이 적은 대상에게 대미지를 제대로 꽂을 수 있습니다."
        }),
        @Description(subject = "다단히트를 가하는 대상", explain = {
        		"아레스 등의 고속으로 연속공격을 하는 피해를",
        		"0.5초의 무적으로 생존력을 높일 수 있습니다."
        })
}, weak = {
        @Description(subject = "방어력이 높은 대상", explain = {
        		"낮은 대미지로 여러 번 타격하는 성질 때문에",
        		"방어력이 높은 대상에게는 대미지가 오히려 덜",
        		"나올 가능성이 있습니다."
        }),
        @Description(subject = "대미지 감경", explain = {
        		"적이 어떠한 수단이라도 받는 피해를 감경할 수 있다면",
        		"피해 감경을 여러번 적용하는 꼴이기에",
        		"대미지가 더 적게 들어갑니다.",
        		"예시로 베르투스 같은 적을 만난다면,",
        		"대미지 75% 감소도 세번 적용되는 식이죠."
        })
}, stats = @Stats(offense = Level.FOUR, survival = Level.FOUR, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.ZERO), difficulty = Difficulty.VERY_EASY)

public class MultiHit extends AbilityBase {

	public MultiHit(Participant participant) {
		super(participant);
	}
	
	private LivingEntity target;
	private double dmg = 0;
	
	private final int multiply = DAMAGE_CONFIG.getValue();
	private final int invincibility = (int) (INV_CONFIG.getValue() * 20);
	
	public static final SettingObject<Double> INV_CONFIG = abilitySettings.new SettingObject<Double>(MultiHit.class,
			"invulnerable-time", 0.4, "# 무적 시간", "# 단위는 초입니다.") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COUNT_CONFIG = abilitySettings.new SettingObject<Integer>(MultiHit.class,
			"damage-count", 3, "# 타격 횟수") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DAMAGE_CONFIG = abilitySettings.new SettingObject<Integer>(MultiHit.class,
			"damage", 50, "# 대미지", "# 단위: % (50 = 50%)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final AbilityTimer attacking = new AbilityTimer(Math.max(COUNT_CONFIG.getValue() - 1, 1)) {

		@Override
		protected void run(int arg0) {
			target.setNoDamageTicks(0);
			target.damage(dmg, getPlayer());
		}
		
	}.setPeriod(TimeUnit.TICKS, 3).register();
	
	private final AbilityTimer inv = new AbilityTimer(invincibility) {

		@Override
		protected void run(int arg0) {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && !e.isCancelled()) {
			if (!inv.isRunning()) {
				inv.start();
			} else {
				SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1, 1.2f);
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent(priority = 5)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().equals(getPlayer()) && !attacking.isRunning() && !e.isCancelled()) {
			dmg = e.getDamage() * (multiply / (double) 100);
			e.setDamage(dmg);
			target = (LivingEntity) e.getEntity();
			if (COUNT_CONFIG.getValue() >= 2) attacking.start();
		}	
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
}