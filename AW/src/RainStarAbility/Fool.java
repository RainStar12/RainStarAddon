package RainStarAbility;

import java.text.DecimalFormat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;

@AbilityManifest(name = "바보", rank = Rank.A, species = Species.HUMAN, explain = {
		"피해를 받으면, 해당 피해의 $[PERCENTAGE]%를 $[DELAY]초 후에는 §b잊어버립니다§8(§7회복합니다§8)§f.",
		"철괴 우클릭 시 $[DURATION]초간 더 멍청해져 §b잊어버리는 속도§f가 $[MULTIPLY]배로 빨라지지만",
		"§c쿨타임§f 동안은 §e똑똑해져§f 잊어버리질 않습니다. $[COOLDOWN]",
		"§b잊어버릴 때마다§f $[CHANCE]%의 확률로 시야가 흔들립니다."
		},
		summarize = {
		"피해를 받고 일정 시간이 지나면 피해량을 §d회복§f하고 시야가 흔들릴 수 있습니다.",
		"§7철괴 우클릭으로§f 잊어버리는 속도를 §b가속§f시킵니다. $[COOLDOWN]",
		"§c쿨타임§f 간에는 패시브 스킬도 같이 비활성화됩니다."
		})

public class Fool extends AbilityBase implements ActiveHandler {

	public Fool(Participant participant) {
		super(participant);
	}
	
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
	
	public static final SettingObject<Integer> PERCENTAGE = abilitySettings.new SettingObject<Integer>(Fool.class,
			"percentage", 75, "# 잊어버릴 피해량의 비율", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
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
	
	public static final SettingObject<Integer> CHANCE = abilitySettings.new SettingObject<Integer>(Fool.class,
			"chance", 20, "# 시야가 흔들릴 확률") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), 50);
	private final int delay = (int) (DELAY.getValue() * 20);
	private final int multiply = MULTIPLY.getValue();
	private final double percentage = (PERCENTAGE.getValue() * 0.01);
	private final DecimalFormat df = new DecimalFormat("0.00");
	private final DecimalFormat df2 = new DecimalFormat("0.0");
	private final double chance = CHANCE.getValue() * 0.01;
	private Random random = new Random();
	
	@SubscribeEvent(priority = 9999)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && !cool.isRunning() && !e.isCancelled()) {
			new ForgetTimer(delay, e.getFinalDamage() * percentage).start();
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
				if (Math.random() <= chance) {
					final Location location = getPlayer().getLocation();
					float yaw = location.getYaw() + random.nextInt(180) - 90;
					if (yaw > 180 || yaw < -180) {
						float mod = yaw % 180;
						if (mod < 0) {
							yaw = 180 + mod;
						} else if (mod > 0) {
							yaw = -180 + mod;
						}
					}
					NMS.rotateHead(getPlayer(), getPlayer(), yaw, location.getPitch() + random.nextInt(90) - 45);
				}
			}
			channel.unregister();
		}
		
		@Override
		public void onSilentEnd() {
			channel.unregister();
		}
		
	}
	
}
