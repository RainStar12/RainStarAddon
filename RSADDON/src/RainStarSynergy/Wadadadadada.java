package RainStarSynergy;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "와다다다다다", rank = Rank.A, species = Species.HUMAN, explain = {
		"근접 공격 피해를 입힐 때 $[DAMAGE_CONFIG]%의 피해로 $[COUNT_CONFIG]번 공격합니다.",
		"또한 피해를 입을 때마다 $[INV_CONFIG]초간 무적 상태가 됩니다."
		})

public class Wadadadadada extends Synergy {

	public Wadadadadada(Participant participant) {
		super(participant);
	}
	
	private LivingEntity target;
	private double dmg = 0;
	
	private final int multiply = DAMAGE_CONFIG.getValue();
	private final int invincibility = (int) (INV_CONFIG.getValue() * 20);
	
	public static final SettingObject<Double> INV_CONFIG = abilitySettings.new SettingObject<Double>(Wadadadadada.class,
			"invulnerable-time", 0.5, "# 무적 시간", "# 단위는 초입니다.") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COUNT_CONFIG = abilitySettings.new SettingObject<Integer>(Wadadadadada.class,
			"damage-count", 6, "# 타격 횟수") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DAMAGE_CONFIG = abilitySettings.new SettingObject<Integer>(Wadadadadada.class,
			"damage", 30, "# 대미지", "# 단위: % (50 = 50%)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final AbilityTimer attacking = new AbilityTimer(COUNT_CONFIG.getValue()) {

		@Override
		protected void run(int arg0) {
			target.setNoDamageTicks(0);
			target.damage(dmg, getPlayer());
		}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
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
			dmg = e.getDamage() * (multiply / (double) 100);
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