package rainstar.abilitywar.ability;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
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
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "정밀 조준", rank = Rank.A, species = Species.HUMAN, explain = {
		"$[RELOAD]초마다 다음 $[AMMO_SIZE]발의 화살을 정밀하게 조준합니다.",
		"화살로부터 $[RANGE]칸 내 가장 가까운 플레이어에게 §5유도§f됩니다.",
		"§8[§7HIDDEN§8] §c아직 한 발 남았다§f: 최후의 순간에 최고의 한 발을."
		},
		summarize = {
		"일정 주기로 §d유도 화살§f을 §a충전§f합니다. §d유도 화살§f은 적 $[RANGE]칸에서 §5유도§f됩니다."
		})

@Tips(tip = {
        "유도 화살로 자신의 에임을 보정해주는 역할을 합니다.",
        "내가 에임이 좋지 않다고 생각된다면 이 능력을 추천드립니다!"
}, strong = {
        @Description(subject = "높은 명중률", explain = {
        		"적의 3칸 이내라면 유도해 화살을 명중시키기 때문에,",
        		"안정적인 명중률이 보장됩니다."
        })
}, weak = {
        @Description(subject = "지형지물", explain = {
        		"화살은 유도만 할 뿐, 관통의 효과를 지니지 못해",
        		"지형지물로 화살이 가로막힐 수 있음을 주의하세요."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.ZERO, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.TWO), difficulty = Difficulty.VERY_EASY)

public class PrecisionAiming extends AbilityBase {
	
	public PrecisionAiming(Participant participant) {
		super(participant);
	}
	
	private int range = RANGE.getValue();
	private final int ammo = AMMO_SIZE.getValue();
	private final int reload = (int) (Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(50) * RELOAD.getValue() : RELOAD.getValue());
	private final ActionbarChannel ac = newActionbarChannel();
	private Player target = null;
	private int stack = 0;
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	public static final SettingObject<Integer> RELOAD 
	= abilitySettings.new SettingObject<Integer>(PrecisionAiming.class, "reload-time", 40, "# 강화 대기 시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> RANGE 
	= abilitySettings.new SettingObject<Integer>(PrecisionAiming.class, "range", 3, "# 유도 범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> AMMO_SIZE 
	= abilitySettings.new SettingObject<Integer>(PrecisionAiming.class, "ammo-size", 7, "# 강화할 화살의 수") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			stack = ammo;
			ac.update(Strings.repeat("§a▐", stack).concat(Strings.repeat("§7▐", ammo - stack)));
			passive.start();
		}
	}
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		if (reload == 0) {
    			if (!ammocharging.isRunning()) ammocharging.start();
    		} else {
        		if (count % reload == 0 && !ammocharging.isRunning()) {
        			ammocharging.start();
        		}	
    		}
    	}
    	
    }.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.SECONDS, 1).register();
    
    private final AbilityTimer ammocharging = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		if (stack < ammo) {
    			stack++;
    			ac.update(Strings.repeat("§a▐", stack).concat(Strings.repeat("§7▐", ammo - stack)));
    			SoundLib.BLOCK_IRON_DOOR_CLOSE.playSound(getPlayer(), 1, 1.7f);
    		}
    		if (stack == ammo) {
    			stop(false);
    		}
    	}
    	
    }.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 2).register();
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof ArmorStand) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
			if (stack > 0) {
				stack--;
    			ac.update(Strings.repeat("§a▐", stack).concat(Strings.repeat("§7▐", ammo - stack)));
				new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length()).start();	
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) {
			target = (Player) e.getDamager();
		}
		if (e.getDamager().hasMetadata("Firework")) {
			e.setCancelled(true);
		}
	}
	
	class Homing extends AbilityTimer implements Listener {
		
		private Arrow arrow;
		private double lengths;
		
		private Homing(Arrow arrow, Double length) {
			super(TaskType.REVERSE, 50);
			setPeriod(TimeUnit.TICKS, 1);
			this.arrow = arrow;
			this.lengths = length;
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int arg0) {
			if (arrow != null) {
				arrow.setGlowing(true);
				Player p = LocationUtil.getNearestEntity(Player.class, arrow.getLocation(), predicate);
				if (p != null) {
					if (arrow.getLocation().distanceSquared(p.getEyeLocation()) <= (range * range)) {
						arrow.setGravity(false);
						arrow.setVelocity(VectorUtil.validateVector((p.getPlayer().getEyeLocation().toVector()
									.subtract(arrow.getLocation().toVector())).normalize().multiply(lengths)));	
					}
				}
			}
		}
		
		@EventHandler
		public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			if (e.getDamager().equals(arrow) && getPlayer().getHealth() <= 4 && e.getEntity() instanceof Player) {
				if (stack == 0) {
					if (target != null) {
						if (e.getEntity().equals(target) && target.getHealth() - e.getFinalDamage() <= 0) {
							Healths.setHealth(getPlayer(), getPlayer().getHealth() * 8);
			    			getPlayer().sendMessage("§8[§7HIDDEN§8] §f나를 죽이려 한 대상에게 마지막 화살로 되갚음해주었습니다.");
			    			getPlayer().sendMessage("§8[§7HIDDEN§8] §c아직 한 발 남았다§f를 달성하였습니다.");
			    			ammocharging.start();
			    			range += 10;
			    			SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer());
							final Firework firework = getPlayer().getWorld().spawn(target.getEyeLocation().add(0, 2, 0), Firework.class);
							final FireworkMeta meta = firework.getFireworkMeta();
							meta.addEffect(
									FireworkEffect.builder()
											.withColor(Color.PURPLE)
											.with(Type.CREEPER)
											.build()
							);
							meta.setPower(0);
							firework.setFireworkMeta(meta);
							firework.setMetadata("Firework", NULL_VALUE);
							new BukkitRunnable() {
								@Override
								public void run() {
									firework.detonate();
								}
							}.runTaskLater(AbilityWar.getPlugin(), 1L);
						}
					}	
				}
			}
		}
		
		@EventHandler
		public void onProjectileHit(ProjectileHitEvent e) {
			if (e.getEntity().equals(arrow)) {
				if (e.getHitBlock() != null) stop(false);
				else {
					this.pause();
					arrow.setGravity(true);
					arrow.setGlowing(false);
					new BukkitRunnable() {
						@Override
						public void run() {
							stop(false);
						}
					}.runTaskLater(AbilityWar.getPlugin(), 1L);
				}
			}
		}
		
		@Override
		protected void onEnd() {
			HandlerList.unregisterAll(this);
			arrow.setGravity(true);
			arrow.setGlowing(false);
		}
		
		@Override
		protected void onSilentEnd() {
			onEnd();
		}
	}
	
}