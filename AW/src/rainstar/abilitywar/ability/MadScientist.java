package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.Poison;

@AbilityManifest(name = "매드 사이언티스트", rank = Rank.L, species = Species.HUMAN, explain = {
		"§7수치 §8- §a생명력§f: 최대 체력의 생명력 수치% 이상으로 회복할 수 없습니다.",
		" 또한 생명력 수치%가 곧 수술의 성공률이 됩니다.",
		"§7철괴 좌클릭 §8- §5도핑§f: 생명력을 $[HEALTHY_DECREASE]% 감소시",
		" $[DURATION]초간 §c공격력§f과 §b이동 속도§f가 §8(§7100 - 생명력§8)§f%만큼 증가합니다.",
		" §d도핑 중§f 재사용 시 효과를 즉시 종료하고 §8",
		"§7철괴 우클릭 §8- §3수술§f: 10칸 이내의 대상을 바라보고 수술을 진행합니다. $[COOLDOWN]",
		" §2[§a성공§2] §f대상을 $[HEAL_AMOUNT]만큼 §d회복§f시키고, 자신은 1.5배 더 회복합니다.",
		" §4[§c실패§4] §f회복 효과가 피해 효과로 바뀌는 §2§n중독§f을 $[POISON]초 부여합니다."
		},
		summarize = {
		"§a생명력§f 수치 이상으로 회복하지 못하고, (생명력)%로 수술이 성공합니다.",
		"§7철괴 좌클릭으로§f §a생명력§f을 감소시키고 잠시간 공격력, 이동 속도가 §a생명력§f에",
		"반비례하여 증가합니다.",
		"§710칸 내 대상을 바라보고 철괴 우클릭으로§f 수술하여 성공 시 상호 회복, 실패 시 출혈시킵니다."
		})
public class MadScientist extends AbilityBase implements ActiveHandler {
	
	public MadScientist(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(MadScientist.class, "cooldown", 80,
            "# 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
	
	public static final SettingObject<Integer> HEALTHY_DECREASE = 
			abilitySettings.new SettingObject<Integer>(MadScientist.class, "healthy-decrease", 10,
            "# 도핑 스킬 생명력 수치 감소율", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> DURATION = 
			abilitySettings.new SettingObject<Double>(MadScientist.class, "duration", 15.0,
            "# 도핑 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> HEALTH_GAIN_AMOUNT = 
			abilitySettings.new SettingObject<Double>(MadScientist.class, "health-gain-amount", 6.0,
            "# 도핑으로 획득하는 체력", "# 단위: 반 칸") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> HEAL_AMOUNT = 
			abilitySettings.new SettingObject<Double>(MadScientist.class, "heal-amount", 6.0,
            "# 수술 성공 시 회복량", "# 단위: 반 칸") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> POISON = 
			abilitySettings.new SettingObject<Double>(MadScientist.class, "poison", 15.0,
            "# 수술 실패 시 중독 지속시간", "# 단위: 초") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer()))
				return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
								.getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
    private ActionbarChannel ac = newActionbarChannel();
    private ActionbarChannel ac2 = newActionbarChannel();
    private final double gainhealth = HEALTH_GAIN_AMOUNT.getValue();
    private final double decrease = HEALTHY_DECREASE.getValue() * 0.01;
	private double healthy = 1.0;
	private final DecimalFormat df = new DecimalFormat("0");
	private final int duration = (int) (DURATION.getValue() * 20);
	private final Random random = new Random();
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final double healamount = HEAL_AMOUNT.getValue();
	private final int poisonduration = (int) (POISON.getValue() * 20);
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update("§d♥§f: §a" + df.format(healthy * 100) + "§2%");
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		if (getPlayer().getHealth() + e.getAmount() >= maxHP * healthy) {
			double amount = (maxHP * healthy) - getPlayer().getHealth();
			e.setAmount(amount);
		}
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT)) {
			if (clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown()) {
				Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 10, predicate);
				if (player != null) {
					if (random.nextDouble() <= healthy) {
						final EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, healamount, RegainReason.CUSTOM);
						Bukkit.getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							Healths.setHealth(player, player.getHealth() + healamount);
							ParticleLib.HEART.spawnParticle(player.getLocation(), 0.5, 1, 0.5, 10, 1);
							SoundLib.ENTITY_PLAYER_LEVELUP.playSound(player.getLocation(), 1, 1);
						}
						
						final EntityRegainHealthEvent event2 = new EntityRegainHealthEvent(getPlayer(), healamount * 1.5, RegainReason.CUSTOM);
						Bukkit.getPluginManager().callEvent(event2);
						if (!event2.isCancelled()) {
							double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
							Healths.setHealth(player, Math.min((maxHP * healthy), player.getHealth() + (healamount * 1.5)));
							ParticleLib.HEART.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 1);
							SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer().getLocation(), 1, 1);
						}
						getPlayer().sendMessage("§2[§a!§2] §a수술에 성공하였습니다.");
					} else {
						ParticleLib.ITEM_CRACK.spawnParticle(player.getLocation(), 0, 0, 0, 10, 0.35, MaterialX.REDSTONE_BLOCK);
						Poison.apply(getGame().getParticipant(player), TimeUnit.TICKS, poisonduration);
						getPlayer().sendMessage("§4[§c!§4] §c수술에 실패하였습니다.");
					}
					return cooldown.start();
				} else return false;
			} else if (clicktype.equals(ClickType.LEFT_CLICK)) {
				if (healthy <= 0.01) getPlayer().sendMessage("§5[§c!§5] §f생명력이 다했습니다.");
				else {
					SoundLib.BLOCK_BREWING_STAND_BREW.playSound(getPlayer(), 1, 0.5f);
					healthy = Math.max(0.01, healthy - decrease);
					if (healthy <= 0.011) healthy = 0.01;
					ac.update("§d♥§f: §a" + df.format(healthy * 100) + "§2%");
					Healths.setHealth(getPlayer(), getPlayer().getHealth() + gainhealth);
					if (!doping.isRunning()) {
						if (cooldown.isRunning()) cooldown.setCount(0);
						return doping.start();
					} else {
						int count = doping.getCount();
						doping.stop(false);
						doping.start();
						doping.setCount(count + duration);
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private AbilityTimer doping = new AbilityTimer(duration) {
		
		private AttributeModifier modifier;
		
		@Override
		public void onStart() {
			modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", 1 - healthy, Operation.ADD_SCALAR);
			try {
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
			} catch (IllegalArgumentException ignored) {
			}
		}
		
		@Override
		public void run(int count) {
			ac2.update("§5도핑§f: " + df.format(count / 20.0) + "초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac2.update(null);
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (doping.isRunning()) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (getPlayer().equals(damager)) {
				e.setDamage(e.getDamage() * (1 + (1 - healthy)));
			}	
		}
	}

}