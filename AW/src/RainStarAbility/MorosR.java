package RainStarAbility;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import RainStarAbility.Moros.Mortal;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;

@AbilityManifest(
		name = "모로스", rank = Rank.S, species = Species.GOD, explain = {
		"피할 수 없는 운명의 신, 모로스.",
		"§7게임 시작 §8- §9운명론§f: 자신의 모든 수치화된 효과는 §a오차범위§f가 존재합니다.",
		" §a오차범위§f의 최종 오차값은 게임 시작 시 정해지며, 바뀔 수 없습니다.",
		"§7적 타격 §8- §c필멸§f: $[MORTAL_DURATION]$[MORTAL_DURATION_SPREAD]초 이내에 공격했던 대상이 사망 위기에 처했을 때,",
		" 대상은 §c§n그 어떠한 방법으로도§f 죽음을 피할 수 없습니다.",
		"§7패시브 §8- §3운명 개찬§f: $[RANGE]$[RANGE_SPREAD]칸 내의 §a액티브§8 / §6타게팅§f 스킬을 미리 감지하고 직전에",
		" $[DURATION]$[DURATION_SPREAD]초간 §b타게팅 불가 상태§f가 됩니다. $[PASSIVE_COOLDOWN]$[PASSIVE_COOLDOWN_SPREAD]",
		"§7철괴 우클릭 §8- §b변수 제거§f: §c필멸§f을 전부 §c제압§f으로 바꿉니다. $[ACTIVE_COOLDOWN]$[ACTIVE_COOLDOWN_SPREAD]",
		" §c제압§f된 대상에게는 §c필멸§f 부여 대신 $[DAMAGE_INCREASE]$[DAMAGE_INCREASE_SPREAD]%의 추가 피해를 입힙니다."
		},
		summarize = {
		""
		})

public class MorosR extends AbilityBase {

	public MorosR(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> MORTAL_DURATION = abilitySettings.new SettingObject<Integer>(MorosR.class,
			"mortal-duration", 5, "# 필멸 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> MORTAL_DURATION_SPREAD = abilitySettings.new SettingObject<Integer>(MorosR.class,
			"mortal-duration-spread", 3, "# 필멸 지속시간 오차범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§7±" + getValue() + "§f";
        }
	};
	
	public static final SettingObject<Integer> RANGE = abilitySettings.new SettingObject<Integer>(MorosR.class,
			"range", 10, "# 운명 개찬 범위", "# 단위: 칸") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> RANGE_SPREAD = abilitySettings.new SettingObject<Integer>(MorosR.class,
			"range-spread", 5, "# 운명 개찬 범위 오차범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§7±" + getValue() + "§f";
        }
	};
	
	public static final SettingObject<Integer> DURATION = abilitySettings.new SettingObject<Integer>(MorosR.class,
			"targetable-false-duration", 10, "# 타게팅 불가 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DURATION_SPREAD = abilitySettings.new SettingObject<Integer>(MorosR.class,
			"targetable-false-duration-spread", 7, "# 타게팅 불가 지속시간 오차범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§7±" + getValue() + "§f";
        }
	};
	
	public static final SettingObject<Integer> PASSIVE_COOLDOWN = abilitySettings.new SettingObject<Integer>(MorosR.class,
			"passive-cooldown", 60, "# 운명 개찬 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> PASSIVE_COOLDOWN_SPREAD = abilitySettings.new SettingObject<Integer>(MorosR.class,
			"passive-cooldown-spread", 20, "# 운명 개찬 쿨타임의 오차범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return "§7±" + getValue() + "§f";
        }
	};
	
	public static final SettingObject<Integer> ACTIVE_COOLDOWN = abilitySettings.new SettingObject<Integer>(MorosR.class,
			"active-cooldown", 80, "# 변수 제거 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> ACTIVE_COOLDOWN_SPREAD = abilitySettings.new SettingObject<Integer>(MorosR.class,
			"active-cooldown-spread", 40, "# 변수 제거 쿨타임의 오차범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return "§7±" + getValue() + "§f";
        }
	};
	
	private Map<Player, Mortal> mortals = new HashMap<>();
	private boolean first = false;
	private final Random random = new Random();
	private int mortalDuration = MORTAL_DURATION.getValue();
	private final int mortalDurationSpread = MORTAL_DURATION_SPREAD.getValue();
	private int range = RANGE.getValue();
	private final int rangeSpread = RANGE_SPREAD.getValue();
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR && !first) {
			first = true;
			mortalDuration = (int) (mortalDuration + ((random.nextInt(mortalDurationSpread * 100 + 1) * (random.nextBoolean() ? -0.01 : 0.01))));
			range = (int) (range + ((random.nextInt(rangeSpread * 100 + 1) * (random.nextBoolean() ? -0.01 : 0.01))));
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player) {
			if (e.getDamager().equals(getPlayer()) && !e.isCancelled() && !e.getEntity().equals(getPlayer())) {
				if (!mortals.containsKey(e.getEntity())) {
					mortals.put((Player) e.getEntity(), new Mortal((Player) e.getEntity()));
					mortals.get(e.getEntity()).start();
				} else mortals.get(e.getEntity()).addDamage();
			}
			if (NMS.isArrow(e.getDamager())) {
				Arrow arrow = (Arrow) e.getDamager();
				if (getPlayer().equals(arrow.getShooter()) && !e.isCancelled() && !e.getEntity().equals(getPlayer()) && e.getEntity() != null) {
					if (!mortals.containsKey(e.getEntity())) {
						mortals.put((Player) e.getEntity(), new Mortal((Player) e.getEntity()));
						mortals.get(e.getEntity()).start();
					} else mortals.get(e.getEntity()).addDamage();
				}
			}
		}
	}
	
    public class Mortal extends AbilityTimer implements Listener {
    	
    	private Player player;
		private ActionbarChannel actionbarChannel = newActionbarChannel();
    	
    	private Mortal(Player player) {
			super(TaskType.REVERSE, 5);
			setPeriod(TimeUnit.SECONDS, 1);
			this.player = player;
    	}
    	
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			actionbarChannel = getGame().getParticipant(player).actionbar().newChannel();
		}
		
		@Override
		protected void run(int count) {
			actionbarChannel.update("§4필멸§f: " + count + "초");
		}
		
		private void addDamage() {
			if (isRunning()) {
				setCount(5);
				actionbarChannel.update("§4필멸§f: " + getCount() + "초");
			}
		}
		
		@EventHandler(priority = EventPriority.MONITOR)
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			if (e.getEntity().equals(player)) {
				Player player = (Player) e.getEntity();
				if (player.getHealth() - e.getFinalDamage() <= 0) {
					e.setCancelled(true);
					player.setHealth(0);
				}
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
			mortals.remove(player);
			if (actionbarChannel != null) {
				actionbarChannel.unregister();	
			}
		}
    	
    }
	
}
