package rainstar.abilitywar.ability;

import java.util.Collection;
import java.util.HashSet;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.manager.effect.Fear;
import daybreak.abilitywar.game.manager.effect.Hemophilia;
import daybreak.abilitywar.game.manager.effect.Oppress;
import daybreak.abilitywar.game.manager.effect.Rooted;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableMap;
import rainstar.abilitywar.effect.Charm;
import rainstar.abilitywar.effect.Poison;

@AbilityManifest(name = "서큐버스", rank = Rank.S, species = Species.UNDEAD, explain = {
        "§7철괴 우클릭 §8- §d달콤하게§f: $[RANGE]칸 내의 모든 적을 $[CHARM_DURATION]초간 §d§n유혹§f합니다.",
        " 이 스킬은 $[COUNT]번 사용 후 §c쿨타임§f을 가집니다. $[COOLDOWN]",
        " 자신 외 게임 참가자가 체력을 회복할 때마다 §c쿨타임§f이 회복량 × $[COOLDECREASE_MULTIPLY]초 줄어듭니다.",
        "§7철괴 좌클릭 §8- §c아찔하게§f: $[RANGE]칸 내의 자신 외 모든 적의 §3§n상태이상§f을",
        " §4§n혈사병§f을 제외하고 전부 §c§n출혈 효과§f로 변경합니다. 이 스킬은 §c쿨타임§f이 없습니다."
        },
        summarize = {
        "§7철괴 우클릭으로§f 주변 적들을 §d§n유혹§f합니다. $[COUNT]번 쓰면 쿨타임이 생깁니다.",
        "§7철괴 좌클릭으로§f 주변 적들의 §3상태이상§f을 전부 §c§n출혈§f 효과로 바꿉니다."
        })
public class Succubus extends AbilityBase implements ActiveHandler {

	public Succubus(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(Succubus.class, "skill-range", 7.5,
            "# 모든 스킬 범위", "# 단위: 칸") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> COUNT = 
			abilitySettings.new SettingObject<Integer>(Succubus.class, "count", 3,
            "# 철괴 우클릭 연속 사용 가능 횟수") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
	
	public static final SettingObject<Double> CHARM_DURATION = 
			abilitySettings.new SettingObject<Double>(Succubus.class, "charm-duration", 6.9,
            "# 유혹 지속 시간") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> CHARM_DECREASE = 
			abilitySettings.new SettingObject<Integer>(Succubus.class, "charm-decrease-", 15,
            "# 유혹 도중 대미지 감소율 (단위: %)") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
	
	public static final SettingObject<Integer> CHARM_HEAL = 
			abilitySettings.new SettingObject<Integer>(Succubus.class, "charm-heal-", 25,
            "# 유혹된 대상 타격시 회복률 (단위: %)") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Double> COOLDECREASE_MULTIPLY = 
			abilitySettings.new SettingObject<Double>(Succubus.class, "cooldown-decrease-multiply", 3.3,
            "# 회복량 비례 쿨타임 감소율") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Succubus.class, "cooldown-", 444,
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
	
	private static final ImmutableMap<EffectRegistration<?>, Double> multiplyEffects = ImmutableMap.<EffectRegistration<?>, Double>builder()
			.put(Stun.registration, 4.0)
			.put(Fear.registration, 4.0)
			.put(Charm.registration, 3.5)
			.put(Poison.registration, 1.2)
			.put(Rooted.registration, 2.5)
			.put(Oppress.registration, 3.0)
			.build();
    
	private ActionbarChannel ac = newActionbarChannel();
    private final double range = RANGE.getValue();
    private final int count = COUNT.getValue();
    private final int duration = (int) (CHARM_DURATION.getValue() * 20);
    private final int decrease = CHARM_DECREASE.getValue();
    private final int heal = CHARM_HEAL.getValue();
    private final double multiply = COOLDECREASE_MULTIPLY.getValue();
    private int stack = 1;
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    private final Circle circle = Circle.of(range, (int) Math.min(range * 12.5, 200));
	private static final RGB color = RGB.of(251, 43, 136);
	
	private final Predicate<Entity> healpredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
					return false;
				}
			} else return false;
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
    		ac.update("§d유혹 가능 §f: §e" + count);
    		passive.start();
    	}
    }
    
    private AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
    	public void run(int count) {
    		if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.IRON_INGOT)) {
    			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
				}
    		}
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (healpredicate.test(e.getEntity()) && cooldown.isCooldown()) {
			cooldown.setCount((int) (cooldown.getCount() - e.getAmount() * multiply));
		}
	}
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT)) {
	    	if (clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown()) {
	    		if (LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), range, predicate).size() > 0) {
		    		for (Player player : LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), range, predicate)) {
		    			Participant p = getGame().getParticipant(player);
		    			Charm.apply(p, TimeUnit.TICKS, duration, getPlayer(), heal, decrease);
		    			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
							ParticleLib.HEART.spawnParticle(getPlayer(), loc, 0, 0, 0, 1, 0);
						}
		    		}
		    		if (stack < count) {
		    			ac.update("§d유혹 가능 §f: §e" + (count - stack));
		    			stack++;
		    		} else {
		    			stack = 1;
		    			cooldown.start();
		    			ac.update("§d유혹 가능 §f: §e" + count);
		    		}
		    		return true;	
	    		} else {
	    			getPlayer().sendMessage("§c[§d!§c] §d유혹§f할 수 있는 플레이어가 없습니다.");
	    			return false;
	    		}
	    	} else if (clicktype.equals(ClickType.LEFT_CLICK)) {
	    		for (Player player : LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), range, predicate)) {
	    			Participant p = getGame().getParticipant(player);
	    			if (p.getEffects().size() > 0) {
	    				Collection<Effect> effectlist = new HashSet<>(p.getEffects());
		    			for (Effect effects : effectlist) {
		    				if (!effects.getRegistration().equals(Hemophilia.registration) && !effects.getRegistration().equals(Bleed.registration)) {
			    				int duration = (int) (effects.getCount() * effects.getPeriod());
			    				if (multiplyEffects.containsKey(effects.getRegistration())) duration = (int) (duration * multiplyEffects.get(effects.getRegistration()));
			    				Bleed.apply(p, TimeUnit.TICKS, duration);
		    				}
		    			}	
		    			p.removeEffects(new Predicate<Effect>() {
		                    @Override
		                    public boolean test(Effect effect) {
		                    	return !(effect.getRegistration().equals(Bleed.registration) || effect.getRegistration().equals(Hemophilia.registration));
		                    }

							@Override
							public boolean apply(@Nullable Effect arg0) {
								return false;
							}
		                });
		    			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
		    				ParticleLib.DAMAGE_INDICATOR.spawnParticle(getPlayer(), loc, 0, 0, 0, 1, 0);
						}
	    			}
	    		}
	    	}
	    	
	    }
		return false;
	}
	
}