package rainstar.abilitywar.ability.silent;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.event.ParticipantNewEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import rainstar.abilitywar.effect.Mute;
import rainstar.abilitywar.effect.SightLock;
import rainstar.abilitywar.system.event.MuteRemoveEvent;

@AbilityManifest(name = "사일런트", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7패시브 §c- §8암습§f: §3§n침묵§f에 걸린 대상에게서 §7은신§f합니다.",
		" 나를 바라보지 않는 대상에게 주는 피해량이 §c$[NOT_LOOK_DAMAGE_INCREASE]%§f 증가합니다.",
		"§7근접 공격 §c- §3정적§f: 대상에게 §e표식§f을 부여합니다. §e표식§f은 대상의 시야를 일시적으로",
		" §5§n고정§f시키고, $[MAX_STACK]번째 §e표식§f이 쌓이면 초기화되고 대상을 $[PASSIVE_MUTE]초간 §3§n침묵§f시킵니다.",
		" 대상이 나를 공격한다면 §e표식§f은 초기화됩니다. 초기화 시, $[UNIT_COOLDOWN]",
		"§7철괴 우클릭 §c- §9고요§f: 주변 $[RANGE]칸 내 모든 적을 $[ACTIVE_MUTE]초간 §3§n침묵§f시킵니다. $[COOLDOWN]",
		" $[SPEED_DURATION]초간 게임 내 §3§n침묵§f 상태자 수 × §b$[SPEED_PER]%§f만큼 §b이동 속도§f가 증가합니다. §8(§7최대 $[MAX_SPEED]%§8)",
		"§9[§3침묵§9] §a액티브§f, §6타게팅§f 스킬을 사용할 수 없습니다."
		},
		summarize = {
		"§3§n침묵§f 소지자로부터 §7은신§f하고, 나를 보지 않는 적에게 §c추가 피해§f를 가합니다.",
		"근접 공격 시마다 적의 시야를 한순간 §5§n고정§f시킵니다.",
		"위 효과를 대상에게 피격받지 않고 4회 발동 시 대상을 잠시 §3§n침묵§f시킵니다.",
		"§7철괴 우클릭 시§f 주변 적을 전부 §3§n침묵§f시키고 §3§n침묵§f 소지자만큼 §b이속이 증가§f합니다",
		"§9[§3침묵§9] §a액티브§f, §6타게팅§f 스킬을 사용할 수 없습니다."
		})

public abstract class AbstractSilent extends AbilityBase implements ActiveHandler {

	public AbstractSilent(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> NOT_LOOK_DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Integer>(AbstractSilent.class, "not-look-damage-increase-", 15,
            "# 나를 바라보지 않는 대상에게 공격력 증가", "# 단위: %") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> MAX_STACK = 
			abilitySettings.new SettingObject<Integer>(AbstractSilent.class, "max-stack-", 6,
            "# 최대 표식 개수") {

        @Override
        public boolean condition(Integer value) {
            return value >= 1;
        }

    };
    
	public static final SettingObject<Double> PASSIVE_MUTE = 
			abilitySettings.new SettingObject<Double>(AbstractSilent.class, "passive-mute-", 3.3,
            "# 표식 폭발 시 침묵 부여 시간", "# 단위: 초") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> UNIT_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(AbstractSilent.class, "unit-cooldown-", 10,
            "# 유닛별 쿨타임", "# 단위: 초") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
		@Override
		public String toString() {
			return "§c유닛별 쿨타임 §7: §f" + getValue() + "초";
        }

    };
    
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(AbstractSilent.class, "range-", 8.0,
            "# 액티브 침묵 범위", "# 단위: 칸") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Double> ACTIVE_MUTE = 
			abilitySettings.new SettingObject<Double>(AbstractSilent.class, "active-mute-", 7.0,
            "# 액티브 침묵 시간", "# 단위: 초") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(AbstractSilent.class, "cooldown", 45,
            "# 액티브 쿨타임", "# 단위: 초") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
        }

    };
    
	public static final SettingObject<Double> SPEED_DURATION = 
			abilitySettings.new SettingObject<Double>(AbstractSilent.class, "speed-duration", 15.0,
            "# 액티브 이동 속도 증가 시간", "# 단위: 초") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> SPEED_PER = 
			abilitySettings.new SettingObject<Integer>(AbstractSilent.class, "speed-per-", 25,
            "# 침묵 소지자 수 비례 이속 증가치", "# 단위: %") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> MAX_SPEED = 
			abilitySettings.new SettingObject<Integer>(AbstractSilent.class, "max-speed-", 200,
            "# 최대 이동 속도 증가치", "# 단위: %") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	private final double notlookdamageincrease = NOT_LOOK_DAMAGE_INCREASE.getValue() * 0.01;
	private final int maxstack = MAX_STACK.getValue();
	private final int passivemute = (int) (PASSIVE_MUTE.getValue() * 20);
	private final int activemute = (int) (ACTIVE_MUTE.getValue() * 20);
	private final double range = RANGE.getValue();
	private final int speedduration = (int) (SPEED_DURATION.getValue() * 20);
	private final double speedper = SPEED_PER.getValue() * 0.01;
	private final double maxspeed = MAX_SPEED.getValue() * 0.01;
	private final int unitCooldown = (int) ((UNIT_COOLDOWN.getValue() * 1000) * Wreck.calculateDecreasedAmount(25));
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), 35);
    
	private final Map<UUID, Long> unitcooldowns = new HashMap<>();
	private Set<Participant> muted = new HashSet<>();
	private final Map<Participant, Stack> stackMap = new HashMap<>();
	private final ActionbarChannel ac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	
	protected abstract void hide0(Player player);
	protected abstract void show0(Player player);
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			for (Participant p : muted) {
				hide0(p.getPlayer());
			}
		}
		if (update == Update.RESTRICTION_SET) {
			for (Participant p : muted) {
				show0(p.getPlayer());
			}
		}
		if (update == Update.ABILITY_DESTROY) {
			for (Participant p : muted) {
				show0(p.getPlayer());
			}
			muted.clear();
		}
	}
	
	@SubscribeEvent
	public void onMuteRemove(MuteRemoveEvent e) {
		if (muted.contains(e.getParticipant())) {
			muted.remove(e.getParticipant());
			show0(e.getParticipant().getPlayer());
			new BukkitRunnable() {
				@Override
				public void run() {
					for (Participant participant : muted) {
						hide0(participant.getPlayer());
					}
				}
			}.runTaskLater(AbilityWar.getPlugin(), 3L);
		}
	}
	
	@SubscribeEvent
	public void onParticipantEffectApply(ParticipantNewEffectApplyEvent e) {
		if (e.getEffect().getRegistration().equals(Mute.registration)) {
			muted.add(e.getParticipant());
			hide0(e.getParticipant().getPlayer());
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager)) {
			if (!getPlayer().equals(LocationUtil.getEntityLookingAt(Player.class, (LivingEntity) e.getEntity(), 30, predicate))) {
				e.setDamage(e.getDamage() * notlookdamageincrease);
			}
			
			if (getPlayer().equals(e.getDamager()) && e.getEntity() instanceof Player && !e.isCancelled()) {
				Player player = (Player) e.getEntity();
				if (getGame().isParticipating(player)) {
					Participant participant = getGame().getParticipant(player);
					if (stackMap.containsKey(participant)) {
						if (stackMap.get(participant).addStack()) {
							Mute.apply(participant, TimeUnit.TICKS, passivemute);
						} else SightLock.apply(participant, TimeUnit.TICKS, 3);
					} else if (System.currentTimeMillis() - unitcooldowns.getOrDefault(player.getUniqueId(), 0L) >= unitCooldown) new Stack(participant).start();
					
				}
			}
		}
		
		if (stackMap.containsKey(getGame().getParticipant(damager)) && e.getEntity().equals(getPlayer())) {
			stackMap.get(getGame().getParticipant(damager)).stop(false);
		}
	}
	
    private final AbilityTimer speedup = new AbilityTimer(TaskType.REVERSE, speedduration) {
    	
    	private AttributeModifier incmovespeed;
    	
    	@Override
    	public void onStart() {
    		incmovespeed = new AttributeModifier(UUID.randomUUID(), "incmovespeed", Math.min(maxspeed, speedper * muted.size()), Operation.ADD_SCALAR);
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(incmovespeed);
    	}
    	
    	@Override
    	public void run(int count) {
    		ac.update("§b이속 증가§7: §f" + df.format(count / 20.0));
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		ac.update(null);
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(incmovespeed);
    		cooldown.start();
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown() && !speedup.isRunning()) {
			for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate)) {
				Mute.apply(getGame().getParticipant(player), TimeUnit.TICKS, activemute);
			}
			
			return speedup.start();
		}
		return false;
	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		
		private Stack(Participant participant) {
			super(20);
			setPeriod(TimeUnit.TICKS, 4);
			this.player = participant.getPlayer();
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("§3/", stack).concat(Strings.repeat("§7/", maxstack - stack)));
			hologram.display(getPlayer());
			stackMap.put(getGame().getParticipant(player), this);
			addStack();
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
		}

		private boolean addStack() {
			setCount(20);
			stack++;
			hologram.setText(Strings.repeat("§3/", stack).concat(Strings.repeat("§7/", maxstack - stack)));
			if (stack >= maxstack) {
				stop(false);
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			final long current = System.currentTimeMillis();
			unitcooldowns.put(player.getUniqueId(), current);
			hologram.unregister();
			stackMap.remove(getGame().getParticipant(player));
		}
		
	}
	
}
