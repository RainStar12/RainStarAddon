package RainStarAbility;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;

@AbilityManifest(name = "바보", rank = Rank.A, species = Species.HUMAN, explain = {
		"피해를 받으면, 해당 피해를 $[DELAY]초 후에는 §b잊어버립니다§8(§7회복합니다§8)§f.",
		"철괴 우클릭 시 $[DURATION]초간 더 멍청해져 §b잊어버리는 속도§f가 $[MULTIPLY]배로 빨라지지만",
		"§c쿨타임§f 동안은 §e똑똑해져§f 잊어버리질 않습니다. $[COOLDOWN]"
		})

public class Fool extends AbilityBase implements ActiveHandler {

	public Fool(Participant participant) {
		super(participant);
	}
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), 50);
	private final int delay = (int) (DELAY.getValue() * 20);
	private final int multiply = MULTIPLY.getValue();
	private final DecimalFormat df = new DecimalFormat("0.00");
	private final DecimalFormat df2 = new DecimalFormat("0.0");
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(Fool.class,
			"cooldown", 40, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> DURATION = abilitySettings.new SettingObject<Integer>(Fool.class,
			"duration", 10, "# 지속 시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DELAY = abilitySettings.new SettingObject<Double>(Fool.class,
			"delay", 20.0, "# 피해를 잊는 데 걸리는 시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> MULTIPLY = abilitySettings.new SettingObject<Integer>(Fool.class,
			"multiply", 2, "# 스킬 사용시 피해를 잊는 속도") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	@SubscribeEvent(priority = 9999)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && !cool.isRunning()) {
			new ForgetTimer(delay, e.getFinalDamage()).start();
		}
	}
	
	@SubscribeEvent(priority = 9999)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = 9999)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
	
	private final Duration skill = new Duration(DURATION.getValue() * 20, cool) {

		@Override
		protected void onDurationProcess(int count) {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
			if (!cool.isCooldown() && !skill.isDuration()) return skill.start();
		}
		return false;
	}
	
	private class ForgetTimer extends AbilityTimer {
		
		private final double damage;
		private final ActionbarChannel channel;
		
		public ForgetTimer(Integer duration, Double damage) {
			super(duration);
			setPeriod(TimeUnit.TICKS, 1);
			this.channel = newActionbarChannel();
			this.damage = damage;
		}
		
		@Override
		public void run(int count) {
			if (skill.isRunning()) this.setCount(this.getCount() - multiply);
			channel.update("§e" + df2.format(count / (double) 20) + "§f초: §a+" + (count < 20 ? "§a§k" : "") + df.format(damage));
		}
		
		@Override
		public void onEnd() {
			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), damage, RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + damage);
			}
			channel.unregister();
		}
		
		@Override
		public void onSilentEnd() {
			channel.unregister();
		}
		
	}
	
}
