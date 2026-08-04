package rainstar.abilitywar.game.murder.innocent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import rainstar.abilitywar.game.murder.murderer.ScammerMurderer;
import rainstar.abilitywar.game.murder.system.config.statictype.CheckMurdererSword;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Predicate;

@AbilityManifest(name = "시민: 해커", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c셋업§8]§f 머더가 검을 얻고 15초가 지나기 전까지 §8[§2해킹§8]§f하지 못합니다.",
		"§8[§2해킹§8]§f 15칸 내 다른 대상을 바라보고 금을 좌클릭하면 금을 3개 소모하여,",
		"3초 후 대상의 직업명 중 무작위 §b한 글자§f를 알아냅니다.",
		"머더는 §8[§2해킹§8]§f 여부를 알 수 있습니다.",
		"§8[§5크랙§8]§f §8[§2해킹§8]§f 성공 시, 5초간 그 대상에게 자신의 화살이 2칸 유도됩니다.",
		"이 상태에서 화살이 적중한다면 그 대상을 1초간 §e§n기절§f시킵니다."
})
public class Hacker extends AbstractInnocent {

	public Hacker(Participant participant) {
		super(participant);
	}
	
	private int particlecount = 1;
	private boolean reverse = false;
	private Player target = null;
	private final Circle circle = Circle.of(1, 25);
	private static final RGB cyan = RGB.of(0, 124, 138), green = RGB.of(0, 81, 7);
	private List<RGB> gradation = Gradient.createGradient(30, cyan, green);
	private CheckMurdererSword checkmurderersword = getGame().getModule(CheckMurdererSword.class);

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
			NMS.sendTitle(getPlayer(), "§e역할§f: §a해커", "§f모든 §c머더§f를 밝혀내세요!", 10, 80, 10);
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
				if (checkmurderersword.isCheckable()) {
					MurderMystery murderMystery = (MurderMystery) getGame();
					Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 15, predicate);
					if (!skill.isRunning() && !particleShow.isRunning()) {
						if (player != null) {
							if (murderMystery.consumeGold(getParticipant(), 3)) {
								target = player;
								skill.start();
								getPlayer().sendMessage("§4[§c!§4] §b" + player.getName() + "§f님에게 §2해킹§f을 사용하였습니다.");	
							}
						} else getPlayer().sendMessage("§4[§c!§4] §c바라보는 방향에 사람이 없습니다.");	
					} else getPlayer().sendMessage("§4[§c!§4] §c현재 해킹이 지속 중입니다.");	
				} else getPlayer().sendMessage("§4[§c!§4] §c지금은 해킹을 사용할 수 없습니다.");	
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
			if (particleShow.isRunning()) {
				new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length()).start();
			}
		}
	}
	
	private final AbilityTimer particleShow = new AbilityTimer(100) {

		private double y;
		private boolean add;

		@Override
		public void onStart() {
			y = 0.0;
			add = true;
		}

		@Override
		public void run(int count) {
			if (target != null) {
				if (add && y >= 2.0) {
					add = false;
				} else if (!add && y <= 0) {
					add = true;
				}

				if (add) {
					y += 0.1;
				} else {
					y -= 0.1;
				}
				
				if (particlecount == gradation.size()-1 || particlecount == 0) reverse = !reverse;
				particlecount = Math.min(gradation.size()-1, particlecount + (reverse ? -1 : +1));
				
				for (Location location : circle.toLocations(target.getLocation().add(0, y, 0)))
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), location, gradation.get(particlecount));
				for (Location location : circle.toLocations(target.getLocation().add(0, 2.0 - y, 0)))
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), location, gradation.get(particlecount));	
			}
		}

		@Override
		public void onEnd() {
			target = null;
			reverse = false;
			particlecount = 1;
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer skill = new AbilityTimer(60) {

		private double count;
		private ProgressBar progressBar;
		private ActionbarChannel targetActionbar;
		private DecimalFormat df = new DecimalFormat("0");

		@Override
		protected void onStart() {
			this.count = 0;
			this.progressBar = new ProgressBar(60, 20);
			this.targetActionbar = getGame().getParticipant(target).actionbar().newChannel();
		}

		@Override
		protected void onEnd() {
			if (target != null) {
				NMS.clearTitle(getPlayer());
				String jobname = getGame().getParticipant(target).getAbility().getDisplayName();
				if (getGame().getParticipant(target).getAbility().getClass().equals(ScammerMurderer.class)) jobname = "시민: 광대";
				getPlayer().sendMessage("§4[§c!§4] §e" + target.getName() + "§f님의 직업 명 중 한 글자는 §3[§b" + jobname.charAt(random.nextInt(jobname.length())) + "§3] §f입니다.");
				particleShow.start();
				if (getGame().getParticipant(target).getAbility().getClass().equals(ScammerMurderer.class)) target.sendMessage("§4[§c!§4] §3위장§f을 성공하였습니다.");
			}
			this.progressBar = null;
			targetActionbar.unregister();
			this.targetActionbar = null;
		}

		@Override
		protected void onSilentEnd() {
			this.progressBar = null;
			targetActionbar.unregister();
			this.targetActionbar = null;
		}

		@Override
		protected void run(int count) {
			this.count += (100 / 60.0);
			if (target != null) {
				progressBar.step();

				NMS.sendTitle(getPlayer(),
						"§3" + target.getName() + " §f해킹중...",
						progressBar.toString() + " §f" + df.format(this.count) + "%",
						0, 5, 0
				);
				if (getGame().getParticipant(target).getAbility() instanceof AbstractMurderer) targetActionbar.update(progressBar.toString("A", ChatColor.DARK_PURPLE + ChatColor.MAGIC.toString(), ChatColor.WHITE + ChatColor.MAGIC.toString()));
			}
		}
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
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
				Player p = LocationUtil.getNearestEntity(Player.class, arrow.getLocation(), predicate);
				if (p != null && p == target) {
					if (arrow.getLocation().distanceSquared(p.getEyeLocation()) <= 4) {
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
					if (e.getHitEntity() != null && e.getHitEntity() instanceof Player) {
						Player p = (Player) e.getHitEntity();
						if (getGame().isParticipating(p)) Stun.apply(getGame().getParticipant(p), TimeUnit.TICKS, 20);
					}
					this.pause();
					arrow.setGravity(true);
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
			arrow.setGlowing(false);
		}
		
		@Override
		protected void onSilentEnd() {
			onEnd();
		}
	}
	
}