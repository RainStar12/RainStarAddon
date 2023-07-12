package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import rainstar.abilitywar.effect.Charm;

@AbilityManifest(name = "구미호(둔갑)", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §c둔갑§f: 회복 효과를 받을 수 없습니다.",
		" 체력을 전부 소모할 때 전체 체력의 40%만 남고 구미호로 돌아갑니다.",
		"§7공격 §8- §c사랑의 매질§f: 다른 플레이어를 공격할 때 $[STACK_COOL]초마다 표식을 쌓고,",
		" 표식이 $[STACK_MAX]개가 될 때 대상을 $[CHARM_DURATION]초간 §d§n유혹§f합니다. §d§n유혹§f 도중엔 표식을 쌓지 못합니다.",
		"§7철괴 우클릭 §8- §c집착§f: 마지막으로 §d§n유혹§f한 대상에게 돌진합니다.",
		" 이후 $[DEADLINE]초 내로 가하는 다음 근접 공격이 §d§n유혹§f을 제거하고",
		" §d§n유혹§f의 남은 시간당 $[DAMAGE_INCREASE]%의 추가 피해를 가합니다. $[COOLDOWN]",
		"§c[§d유혹§c]§f 대상이 강제로 나를 바라보게 되고, 내게 주는 피해량이 $[CHARM_DECREASE]% 감소합니다.",
		" 대상을 공격할 때마다 준 최종 대미지의 $[CHARM_HEAL]%만큼 체력을 얻습니다."
		},
		summarize = {
		"§d유혹 상태§f인 적을 타격해 회복하는 것 외엔 회복 효과를 받을 수 없습니다.",
		"체력을 전부 소모하면 전체 체력의 40%만 남고 구미호로 되돌아갑니다.",
		"다른 플레이어를 공격할 때마다 표식을 쌓아 $[STACK_MAX]개가 될 때 대상을 §d유혹§f합니다.",
		"§d유혹§f된 대상은 나를 강제로 바라보며 내게 주는 피해량이 감소합니다.",
		"§7철괴 좌클릭§f으로 끈을 던져 대상의 위치까지 이동해 공격합니다. $[COOLDOWN]"
		})

public class NineTailFoxC extends AbilityBase implements ActiveHandler {
	
	public NineTailFoxC(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "cooldown", 90,
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
    
	public static final SettingObject<Double> STACK_COOL = 
			abilitySettings.new SettingObject<Double>(NineTailFoxC.class, "stack-cooldown", 1.0,
            "# 스택을 쌓을 때 내부 쿨타임") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> STACK_MAX = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "stack-max-", 5,
            "# 최대 스택 개수") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> CHARM_DURATION = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "charm-duration-", 5,
            "# 유혹 지속 시간") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> CHARM_DECREASE = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "charm-decrease", 35,
            "# 유혹 도중 대미지 감소율 (단위: %)") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
	
	public static final SettingObject<Integer> CHARM_HEAL = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "charm-heal-", 40,
            "# 유혹된 대상 타격시 회복률 (단위: %)") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Double> DEADLINE = 
			abilitySettings.new SettingObject<Double>(NineTailFoxC.class, "deadline", 3.0,
            "# 근접 공격 강화 유효시간") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Integer>(NineTailFoxC.class, "damage-increase", 20,
            "# 철괴 우클릭 공격력 증가 배율", "# 단위: %") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
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
			} else return false;
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	private final double stackcool = STACK_COOL.getValue();
	private final int maxstack = STACK_MAX.getValue();
	private final int charmduration = CHARM_DURATION.getValue();
	private final int charmdecrease = CHARM_DECREASE.getValue();
	private final int charmheal = CHARM_HEAL.getValue();
	private final int deadline = (int) (DEADLINE.getValue() * 20);
	private final double dmginc = DAMAGE_INCREASE.getValue() / 2000.0;
	private Participant lastcharmed;
	private DecimalFormat df = new DecimalFormat("0.0");
	
	private final Map<Player, Stack> stackMap = new HashMap<>();
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !cool.isCooldown() && !skill.isRunning()) {
	    	return skill.start();
	    }
		return false;
	}
    
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.FALL)) {
				getPlayer().sendMessage("§a낙하 대미지를 받지 않습니다.");
				SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
				e.setCancelled(true);
			}
	    	if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && !e.isCancelled()) {
	    		e.setCancelled(true);
				double nowhp = ((4 * (getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) / 10));
				getPlayer().setHealth(nowhp);
			   	SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(getPlayer(), 1, 0.7f);
			   	getPlayer().sendMessage("§5[§d!§5] §c둔갑이 풀렸습니다. 구미호로 되돌아갑니다. §7/aw check 혹은 /aw sum");
		    	AbilityBase ab = getParticipant().getAbility();
		    	if (ab.getClass().equals(Mix.class)) {
		    		final Mix mix = (Mix) ab;
					final AbilityBase first = mix.getFirst(), second = mix.getSecond();
					if (this.equals(first)) {
						try {
							mix.setAbility(NineTailFox.class, second.getClass());
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					} else if (this.equals(second)) {
						try {
							mix.setAbility(first.getClass(), NineTailFox.class);
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					}
		    	} else {
			    	try {
						getParticipant().setAbility(NineTailFox.class);
					} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
						e1.printStackTrace();
					}	
		    	}
	    	}
		}
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && !e.isCancelled() && predicate.test(e.getEntity())) {
			Participant target = getGame().getParticipant((Player) e.getEntity());
			if (!target.hasEffect(Charm.registration)) {
				if (stackMap.containsKey(e.getEntity())) {
					if (stackMap.get(e.getEntity()).getCount() < (120 - (stackcool * 20))) {
						if (stackMap.get(e.getEntity()).addStack()) {
							Charm.apply(target, TimeUnit.SECONDS, charmduration, getPlayer(), charmheal, charmdecrease);
							lastcharmed = target;
						}	
					}
				} else new Stack((Player) e.getEntity()).start();
			} else if (skill.isRunning()) {
				int durations = 0;
				for (Charm charm : target.getEffects(Charm.registration)) {
					if (charm.getApplier().equals(getPlayer())) {
						durations += charm.getDuration();
					}
				}
				e.setDamage(e.getDamage() * (1 + (durations * dmginc)));
				for (Charm charm : target.getEffects(Charm.registration)) {
					if (charm.getApplier().equals(getPlayer())) {
						charm.stop(false);
					}
				}
				skill.stop(false);
				SoundLib.ENTITY_PLAYER_ATTACK_CRIT.playSound(target.getPlayer().getLocation(), 1, 1.3f);
				ParticleLib.DAMAGE_INDICATOR.spawnParticle(target.getPlayer().getLocation(), 0.5, 1, 0.5, 10, 1);
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onEntityRegainHealth(EntityRegainHealthEvent e) {
		e.setCancelled(true);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerJoin(PlayerJoinEvent e) {
		for (Stack stack : stackMap.values()) {
			stack.hologram.display(getPlayer());
		}
	}
	
	private final AbilityTimer skill = new AbilityTimer(deadline) {

		@Override
		protected void onStart() {
			getPlayer().setVelocity(lastcharmed.getPlayer().getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().multiply(1.4).setY(0));
		}
		
		@Override
		protected void run(int count) {
			
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			cool.start();
		}

	}.setPeriod(TimeUnit.TICKS, 1);
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		
		private Stack(Player player) {
			super(100);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("§d♥", stack).concat(Strings.repeat("§d♡", maxstack - stack)));
			hologram.display(getPlayer());
			stackMap.put(player, this);
			addStack();
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
		}

		private boolean addStack() {
			setCount(100);
			stack++;
			hologram.setText(Strings.repeat("§d♥", stack).concat(Strings.repeat("§d♡", maxstack - stack)));
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
			hologram.unregister();
			stackMap.remove(player);
		}
		
	}

}