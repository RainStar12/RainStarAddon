package RainStarAbility;

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
		"근접 공격 피해를 입힐 때 40%의 피해로 3번 공격합니다.",
		"또한 피해를 입을 때마다 $[InvConfig]틱간 무적 상태가 됩니다."
		})

@Tips(tip = {
        "대미지가 일시적으로 낮아지지만, 총 3번을 공격하기에 최종적으로",
        "1.2배의 피해를 입힐 수 있는 능력입니다. 또한 반칸 이하의 피해를",
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
        })
}, stats = @Stats(offense = Level.FOUR, survival = Level.FOUR, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.ZERO), difficulty = Difficulty.VERY_EASY)

public class MultiHit extends AbilityBase {

	public MultiHit(Participant participant) {
		super(participant);
	}
	
	private LivingEntity target;
	private double dmg = 0;
	
	private final int invincibility = InvConfig.getValue();
	
	public static final SettingObject<Integer> InvConfig = abilitySettings.new SettingObject<Integer>(MultiHit.class,
			"Inv Time", 10, "# 무적 시간", "# 단위는 틱입니다. 20틱 = 1초") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final AbilityTimer attacking = new AbilityTimer(2) {

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
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().equals(getPlayer()) && !attacking.isRunning()) {
			dmg = (2 * (e.getDamage() / 5));
			e.setDamage(dmg);
			target = (LivingEntity) e.getEntity();
			attacking.start();
		}	
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
}