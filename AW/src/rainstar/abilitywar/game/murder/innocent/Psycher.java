package rainstar.abilitywar.game.murder.innocent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "시민: 초능력자", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§3염력§8]§f 만일 금을 27개 이상 소지 중이라면 소모해서,",
		"자신의 화살을 §5염력§f으로 조작해 화살로부터 가장 가까운",
		"사람의 5칸 내에서 §5유도§f되게 만듭니다.",
		"§8[§5지정§8]§f 금을 들고 F키를 눌러 현재 자신의 §b위치§f를 지정할 수 있습니다.",
		"§8[§5이동§8]§f 금 좌클릭으로 금 4개를 소모해 지정 §b위치§f로 §5공간 이동§f합니다. §8(§74회용§8)"
})
public class Psycher extends AbstractInnocent {
	
	public Psycher(Participant participant) {
		super(participant);
	}
	
	private Location location = null;
	private static final RGB PURPLE = RGB.of(113, 43, 204), NIGHT = RGB.of(10, 18, 140);
	private int stack = 4;
	
	private final List<RGB> gradation = Gradient.createGradient(100, PURPLE, NIGHT);
	
	private Map<Projectile, ArrowParticle> arrowParticles = new HashMap<>();
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}
	};
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a초능력자", "§f능력의 힘으로 §c머더§f를 따돌리고, 격퇴하세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void run(int count) {
				}

				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
		}
	}
	
    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
    		getPlayer().sendMessage("§4[§c!§4] §b현재 위치§f를 §5공간 이동§f 좌표로 지정하였습니다.");
    		location = getPlayer().getLocation();
    	}
    }
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 8)) {
					if (!addArrow()) {
						murderMystery.addGold(getParticipant());
					} else {
						if (!hasBow()) {
							getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
						}
					}
				}
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				MurderMystery murderMystery = (MurderMystery) getGame();
				if (location != null) {
					if (stack > 0) {
						if (murderMystery.consumeGold(getParticipant(), 4)) {
							getPlayer().sendMessage("§4[§c!§4] §5순간 이동§f을 사용하였습니다.");	
							SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(getPlayer().getLocation());
							ParticleLib.DRAGON_BREATH.spawnParticle(getPlayer().getLocation(), 0.2, 0.2, 0.2, 7, 0);
							getPlayer().teleport(location);
							SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(getPlayer().getLocation());
							for (int i = 0; i < 14; i++) {
								ParticleLib.REDSTONE.spawnParticle(getPlayer().getLocation().clone().add(Math.random() * 0.6, Math.random() * 0.6, Math.random() * 0.6), PURPLE);
							}
							stack--;
						}	
					} else getPlayer().sendMessage("§4[§c!§4] §c이미 횟수를 전부 사용했습니다.");	
				} else getPlayer().sendMessage("§4[§c!§4] §c지정된 좌표가 없습니다.");	
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
			final MurderMystery murderMystery = (MurderMystery) getGame();
			if (murderMystery.consumeGold(getParticipant(), 27)) {
				new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length()).start();
				new ArrowParticle(e.getEntity(), e.getEntity().getVelocity().length()).start();
				getPlayer().sendMessage("§4[§c!§4] §3염력§f을 사용하였습니다.");
			}
		}
	}
	
    @SubscribeEvent
    public void onProjectileHit(ProjectileHitEvent e) {
    	if (arrowParticles.containsKey(e.getEntity())) {
    		arrowParticles.get(e.getEntity()).stop(false);
    	}
    }
	
	private class Homing extends AbilityTimer implements Listener {
		
		private final Arrow arrow;
		private final double lengths;
		
		private Homing(Arrow arrow, double length) {
			super(TaskType.REVERSE, 300);
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
					if (arrow.getLocation().distanceSquared(p.getEyeLocation()) <= 25) {
						arrow.setGravity(false);
						arrow.setVelocity(VectorUtil.validateVector((p.getPlayer().getEyeLocation().toVector()
									.subtract(arrow.getLocation().toVector())).normalize().multiply(lengths)));	
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
	
    public class ArrowParticle extends AbilityTimer {
    	
		private Location lastloc;
		private Vector forward;
		private final double length;
		private final Projectile projectile;
    	
    	public ArrowParticle(Projectile projectile, double length) {
			super(TaskType.INFINITE, -1);
    		setPeriod(TimeUnit.TICKS, 1);
    		this.projectile = projectile;
    		this.length = length;
			this.lastloc = projectile.getLocation();
			arrowParticles.put(projectile, this);
    	}
    	
    	@Override
    	public void run(int i) {
    		this.forward = projectile.getLocation().clone().subtract(lastloc.clone()).toVector().normalize().multiply(length);
    		Location newLocation = lastloc.clone().add(forward);
    		for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastloc.toVector()),
						unit = vectorBetween.clone().normalize().multiply(.1);
				private final int amount = (int) (vectorBetween.length() / 0.1);
				private int cursor = 0;

				@Override
				public boolean hasNext() {
					return cursor < amount;
				}

				@Override
				public Location next() {
					if (cursor >= amount)
						throw new NoSuchElementException();
					cursor++;
					return lastloc.clone().add(unit.clone().multiply(cursor));
				}
			};iterator.hasNext();) {
				final Location location = iterator.next();
				ParticleLib.REDSTONE.spawnParticle(location, random.pick(gradation));
				lastloc = newLocation;
			}
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		arrowParticles.remove(projectile);
    	}
    	
    }

}
