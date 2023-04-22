package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "순간 가속", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §b액셀§f: §3가속§f을 획득하고, $[ACCEL_DURATION]초간 §3가속§f × $[SPEED_UP]%만큼 §b이동 속도§f가",
		" §a증가§f합니다. $[MAX_STACK]번 사용 시 §3가속§f을 초기화하고 §c쿨타임§f을 가집니다. $[COOLDOWN]",
		"§7근접 공격 §8- §eE=mc²§f: 내 현재 §b이동 속도§f에 비례하여 피해량이 강력해집니다."
		},
		summarize = {
		""
		})
public class Accel extends AbilityBase implements ActiveHandler {
	
	public Accel(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> ACCEL_DURATION = 
			abilitySettings.new SettingObject<Double>(Accel.class, "accel-duration", 5.0,
					"# 가속 지속시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> SPEED_UP = 
			abilitySettings.new SettingObject<Integer>(Accel.class, "speed-up", 10, 
					"# 가속 당 이동 속도 증가치", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> MAX_STACK = 
			abilitySettings.new SettingObject<Integer>(Accel.class, "max-stack", 7, 
					"# 최대 중첩가능 횟수") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Accel.class, "cooldown", 60, 
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

	private final int duration = (int) (ACCEL_DURATION.getValue() * 20);
	private final double speedup = SPEED_UP.getValue() * 0.01;
	private final int maxstack = MAX_STACK.getValue();
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
	private int accel = 0;
	private ActionbarChannel ac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0");
	private final DecimalFormat df2 = new DecimalFormat("0.0");
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(getPlayer())) {
			double speed = getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue();
			e.setDamage(e.getDamage() * (1 + ((speed - 0.15) * 2)));
			new Holograms(e.getEntity().getLocation(), (1 + ((speed - 0.15) * 2))).start();
		}
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown()) {
			if (skill.isRunning()) getPlayer().sendMessage("§3[§b!§3] §f가속 효과가 진행중입니다.");
			else {
				accel++;
				return skill.start();
			}
		}
		return false;
	}
	
	public AbilityTimer skill = new AbilityTimer(duration) {
		
		private AttributeModifier modifier;
		
		@Override
		public void onStart() {
			SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(getPlayer(), 1, 1.0f + (accel * 0.1f));
			SoundLib.ENTITY_FIREWORK_ROCKET_LARGE_BLAST.playSound(getPlayer(), 1, 1.0f + (accel * 0.1f));
			modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", (accel * speedup), Operation.ADD_SCALAR);
			try {
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
			} catch (IllegalArgumentException ignored) {
			}
		}
		
		@Override
		public void run(int count) {
			ac.update("§3가속§7: §b" + df.format(accel * speedup * 100) + "§f% §8/ §e남은 시간§f: §a" + df2.format(count / 20) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			if (accel >= maxstack) {
				accel = 0;
				cooldown.start();
			}
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private class Holograms extends AbilityTimer {
		
		private final IHologram hologram;
		private Random random = new Random();
		private final DecimalFormat multiplyDF = new DecimalFormat("0.00");
		private double multiply;
		
		private Holograms(Location hitLoc, double multiply) {
			super(TaskType.REVERSE, 30);
			setPeriod(TimeUnit.TICKS, 1);
			this.hologram = NMS.newHologram(getPlayer().getWorld(), 
					hitLoc.getX() + (((random.nextDouble() * 2) - 1) * 0.5),
					hitLoc.getY() + 1.25 + (((random.nextDouble() * 2) - 1) * 0.25), 
					hitLoc.getZ() + (((random.nextDouble() * 2) - 1) * 0.5), 
					"§a§l× §3§l" + multiplyDF.format(multiply));
			this.multiply = multiply;
			for (Player player : getPlayer().getWorld().getPlayers()) {
				hologram.display(player);
			}
		}
		
		@Override
		protected void run(int count) {
			hologram.setText("§a§l× §3§l" + multiplyDF.format(multiply));
			if (count < 20) hologram.teleport(hologram.getLocation().add(0, 0.02, 0));	
			else {
				if (count % 2 == 0) {
					for (Player player : getPlayer().getWorld().getPlayers()) {
						hologram.hide(player);
					}
				} else {
					for (Player player : getPlayer().getWorld().getPlayers()) {
						hologram.display(player);
					}
				}
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
		}
		
	}
	
}
