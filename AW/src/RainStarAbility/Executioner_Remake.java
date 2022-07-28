package RainStarAbility;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "집행관 R", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §3심판§f: $[RANGE]칸 내 적들에게 §3심판 표식§f을 $[DURATION]초간 부여합니다.",
		" §3심판 표식§f은 대상이 다른 생명체에게 준 최종 피해량을 누적시킵니다. $[COOLDOWN]",
		"§7검 들고 F §8- §b조율§f: §a고문 §7↔ §c집행§f 모드를 변경할 수 있습니다.",
		" 변경 후 다음 근접 공격은 심판 표식을 터뜨려 효과를 발동시킬 수 있습니다.",
		"§2[§a고문§2]§f", 
		"§8<§7패시브§8>§f 받는 모든 피해가 §a$[DECREASE]%§f 감소합니다.",
		"§8<§7표식 폭발§8>§f 대상은 표식 수치만큼 공격력을 채울 때까지 공격력이 §2$[ATTACK_DECREASE]% 감소§f합니다.",
		"§4[§c집행§4]§f", 
		"§8<§7패시브§8>§f 적에게 가하는 피해가 §c$[INCREASE]%§f 증가합니다.",
		"§8<§7표식 폭발§8>§f 표식 수치만큼 §c추가 피해§f를 입히고 체력이 $[EXECUTE_HEALTH]% 이하면 §4처형§f합니다."
		})
public class Executioner_Remake extends AbilityBase {
	
	public Executioner_Remake(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(Executioner_Remake.class, "range", 7.0,
            "# 철괴 우클릭 사거리", "# 단위: 칸") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Double> DURATION = 
			abilitySettings.new SettingObject<Double>(Executioner_Remake.class, "duration", 15.0,
            "# 심판 표식 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Executioner_Remake.class, "cooldown", 30,
            "# 철괴 우클릭 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
	public static final SettingObject<Integer> DECREASE = 
			abilitySettings.new SettingObject<Integer>(Executioner_Remake.class, "damage-decrease", 15,
            "# [고문] 받는 피해 감소량", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> ATTACK_DECREASE = 
			abilitySettings.new SettingObject<Integer>(Executioner_Remake.class, "enemy-attack-decrease", 20,
            "# [고문] 적 공격력 감소량", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> INCREASE = 
			abilitySettings.new SettingObject<Integer>(Executioner_Remake.class, "damage-increase", 15,
            "# [집행] 공격력 증가량", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> EXECUTE_HEALTH = 
			abilitySettings.new SettingObject<Integer>(Executioner_Remake.class, "execute-health", 10,
            "# [집행] 처형 조건 체력", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
    private final double range = RANGE.getValue();
    private final int duration = (int) (DURATION.getValue() * 20);
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._25);
    private final double dmgdecrease = 1 - (DECREASE.getValue() * 0.01);
    private final double attackdecrease = 1 - (ATTACK_DECREASE.getValue() * 0.01);
    private final double attackincrease = 1 + (INCREASE.getValue() * 0.01);
    private final double executeable = EXECUTE_HEALTH.getValue() * 0.01;
    
	private Map<Player, AttackStack> damageMap = new HashMap<>();
	private Set<Player> executed = new HashSet<>();
	
	private boolean execute = false;
	private boolean changed = false;
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (damageMap.containsKey(e.getEntity()) && e.getDamager().equals(getPlayer()) && changed) {
			Player player = (Player) e.getEntity();
			changed = false;
			if (!execute) {
				new DamageStack(player, damageMap.get(e.getEntity()).damage).start();
			} else {
				e.setDamage(e.getDamage() + damageMap.get(e.getEntity()).damage);
				double maxHP = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				if (player.getHealth() <= maxHP * executeable) {
					player.setHealth(0);
					executed.add(player);
				}
			}
		}
	}
	
	private class DamageStack extends AbilityTimer implements Listener {
		
		private final double requireDMG;
		private final Player player;
		private double damage = 0;
		private ActionbarChannel actionbar;
		
		private DamageStack(Player player, double requireDMG) {
			super();
			this.player = player;
			this.requireDMG = requireDMG;
			this.actionbar = getGame().getParticipant(player).actionbar().newChannel();
			actionbar.update("§a필요 대미지§f: §e" + (requireDMG - damage));
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
		}
		
		@EventHandler(priority = EventPriority.MONITOR)
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (player.equals(damager) && !e.getEntity().equals(player)) {
				damage += e.getFinalDamage();
				actionbar.update("§a필요 대미지§f: §e" + (requireDMG - damage));
				e.setDamage(e.getDamage() * attackdecrease);
				if (damage >= requireDMG) this.stop(false);
			}
		}
		
	}
	
	private class AttackStack extends AbilityTimer implements Listener {
		
		private final Player damager;
		private final IHologram hologram;
		private boolean hid = false;
		private double damage = 0;
		private final DecimalFormat df = new DecimalFormat("0.00");
		
		private AttackStack(Player damager, int duration) {
			super(TaskType.REVERSE, duration);
			setPeriod(TimeUnit.TICKS, 1);
			this.hologram = NMS.newHologram(damager.getWorld(), damager.getLocation().getX(), damager.getLocation().getY() + damager.getEyeHeight() + 0.6, damager.getLocation().getZ(), "");
			this.damager = damager;
			Executioner_Remake.this.damageMap.put(damager, this);
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int count) {
			if (NMS.isInvisible(damager)) {
				if (!hid) {
					this.hid = true;
					hologram.hide(getPlayer());
				}
			} else {
				if (hid) {
					this.hid = false;
					hologram.display(getPlayer());
				}
				hologram.teleport(damager.getWorld(), damager.getLocation().getX(), damager.getLocation().getY() + damager.getEyeHeight() + 0.6, damager.getLocation().getZ(), damager.getLocation().getYaw(), 0);
			}
		}
		
		@EventHandler(priority = EventPriority.MONITOR)
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (getPlayer().equals(damager) && !e.getEntity().equals(damager)) {
				damage += e.getFinalDamage();
				hologram.setText("§4" + df.format(damage));
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
			hologram.unregister();
			Executioner_Remake.this.damageMap.remove(damager);
		}
		
	}

}
