package RainStarAbility;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerVelocityEvent;

import RainStarEffect.TimeSlowdown;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.google.common.base.Predicate;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;

@AbilityManifest(name = "크로노스", rank = Rank.S, species = Species.GOD, explain = {
		"시간의 신 크로노스.",
		"§7패시브 §8- §3시간 지배§f: §e밤낮§f에 따라 시간 버프를 얻습니다.",
		" 낮 동안엔 시간을 가속시켜 재생력이 빨라지고 쿨타임이 빨리 줄어듭니다.",
		" 밤 동안엔 시간을 감속시켜 주변 6칸 내의 발사체 및 이동 효과를 느리게 만듭니다.",
		"§7철괴 좌클릭 §8- §3시간 반전§f: 시간을 반전시켜 밤이라면 낮으로,",
		" 낮이라면 밤으로 만듭니다. 이 능력은 3번만 쓸 수 있습니다.",
		"§7철괴 우클릭 §8- §3시간 조작§f: 주변 6칸 내 플레이어의 시간을 조작해",
		" 액티브 능력을 즉시 발동시키게 하며 쿨타임을 15초 증가시키고,",
		" 10초간 시간 둔화 상태로 만듭니다. $[COOLDOWN]",
		"§7상태이상 §8- §3시간 둔화§f: 쿨타임이 매 초마다 1초씩 늘어납니다."
		},
		summarize = {
		"§e밤낮§f에 따른 고속 회복 / 주변 발사체 감속의 각각의 버프를 얻습니다.",
		"§7철괴 좌클릭§f으로 §e낮§f과 §8밤§f을 뒤바꿀 수 있습니다.",
		"§7철괴 우클릭§f으로 주변 플레이어의 액티브 능력을 강제 사용시키고 10초간",
		"쿨타임이 흐르지 않게 합니다. $[COOLDOWN]"
		})

@Tips(tip = {
        "패시브인 고속 회복으로 생존력을 높이고, 주변 플레이어의 액티브 능력",
        "사용을 늦추는 것으로 변수를 낮춰 안정적인 플레이가 가능합니다.",
        "다만 시간 조작의 쿨타임 증가는 대상이 쿨타임이 아니거나, 패시브 능력일",
        "경우에는 장점이 퇴색되기 때문에 가능하다면 대상이 능력을 사용하고 나서",
        "사용하는 편이 좋습니다."
}, strong = {
        @Description(subject = "짧은 능력 쿨타임의 대상", explain = {
                "짧은 쿨타임의 능력은 대부분 능력을 난사해서 축적 대미지를",
                "입혀 우위를 취하나, 시간 둔화 및 쿨타임 추가로 20초의",
                "쿨타임 손실을 입히면 대상을 제압하기 좋죠!"
        })
}, weak = {
        @Description(subject = "패시브 능력의 대상", explain = {
                "패시브 능력에게는 시간 조작이 아무런 영향을 미치지 못해,",
                "시간 지배 패시브로만 싸워야 합니다."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.FOUR, crowdControl = Level.FOUR, mobility = Level.ZERO, utility = Level.TWO), difficulty = Difficulty.EASY)

public class Chronos extends AbilityBase implements ActiveHandler {
	
	public Chronos(Participant participant) {
		super(participant);
	}

	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	private int number = 3;
	private static final RGB color = RGB.of(25, 147, 168);
	private long worldtime = 0;
	private static final Circle circle = Circle.of(6, 70);
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "cooldown", 120,
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
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
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
    
	private static boolean isNight(long worldtime) {
		return worldtime > 12300 && worldtime < 23850;
	}
	
	private void updateTime(World world) {
		if (worldtime > 12300 && worldtime < 23850) {
			world.setTime(1000);
		} else {
			world.setTime(13000);
		}
	}
	
	private final Set<Projectile> myprojectiles = new HashSet<Projectile>();
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		final Location playerLoc = getPlayer().getLocation();
			worldtime = getPlayer().getWorld().getTime();
    		
    		if (!getPlayer().isDead()) {
    			if (isNight(getPlayer().getWorld().getTime())) {
    				for (Projectile projectile : LocationUtil.getNearbyEntities(Projectile.class, playerLoc, 6, 6, null)) {
    					if (!projectile.isOnGround() && !myprojectiles.contains(projectile)) {
    						projectile.setVelocity(projectile.getVelocity().multiply(0.65));
    					}
    				}
    				cooldel.stop(false);
    			} else {
    				final double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    				if (getPlayer().getHealth() < maxHP) {
    					final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), 0.025, RegainReason.CUSTOM);
    					Bukkit.getPluginManager().callEvent(event);
    					if (!event.isCancelled()) {
    						getPlayer().setHealth(Math.min(getPlayer().getHealth() + 0.025, maxHP));
    					}
    				}
    				cooldel.start();
    			}
			}
    		if (count % 2 == 0) {
				for (Location loc : circle.toLocations(playerLoc).floor(playerLoc.getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
				}
    		}
    	}
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private final AbilityTimer cooldel = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		cool.setCount(Math.max(cool.getCount() - 1, 0));
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 40).register();
    
    @SubscribeEvent
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
    	if (getPlayer().equals(e.getEntity().getShooter())) {
        	myprojectiles.add(e.getEntity());
    	}
    }
    
    @SubscribeEvent
    public void onPlayerVelocity(PlayerVelocityEvent e) {
    	if (!cooldel.isRunning()) {
    		if (LocationUtil.isInCircle(getPlayer().getLocation(), e.getPlayer().getLocation(), 6)) {
    			e.setVelocity(VectorUtil.validateVector(e.getPlayer().getVelocity().multiply(0.8)));
			}
    	}
    }
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.RIGHT_CLICK && !cool.isCooldown()) {
			List<Player> players = LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 6, 6,
					predicate);
			for (Player p : players) {
				Participant participant = getGame().getParticipant(p);
				if (participant.hasAbility() && participant.getAbility() instanceof ActiveHandler) {
					ActiveHandler active = (ActiveHandler) participant.getAbility();
					active.ActiveSkill(Material.IRON_INGOT, ClickType.RIGHT_CLICK);
					active.ActiveSkill(Material.IRON_INGOT, ClickType.LEFT_CLICK);
					active.ActiveSkill(Material.GOLD_INGOT, ClickType.RIGHT_CLICK);
					active.ActiveSkill(Material.GOLD_INGOT, ClickType.LEFT_CLICK);	
				}
				if (participant.hasAbility() && !participant.getAbility().isRestricted()) {
					AbilityBase ab = participant.getAbility();
					for (GameTimer t : ab.getTimers()) {
						if (t instanceof Cooldown.CooldownTimer) {
							t.setCount(t.getCount() + 15);
							TimeSlowdown.apply(participant, TimeUnit.SECONDS, 10);
						}
					}
				}
			}
			cool.start();
			return true;
		}
		if (material == Material.IRON_INGOT && clicktype == ClickType.LEFT_CLICK) {
			if (number != 0) {
				updateTime(getPlayer().getWorld());
				number--;
				getPlayer().sendMessage("시간을 §3반전§f시켰습니다. 남은 횟수 : §e" + number + "§f회");
				return true;
			} else {
				getPlayer().sendMessage("더이상 시간을  §3반전§f시킬 수 없습니다.");
			}
		}
		return false;
	}
}