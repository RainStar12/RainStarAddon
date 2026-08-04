package rainstar.abilitywar.game.murder.system.detectivebuffer;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractDetective;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.system.config.statictype.GhostJobs;
import daybreak.abilitywar.utils.base.color.RGB;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

@AbilityManifest(name = "탐정 <사이코메트리>", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"활이 기본적으로 지급되며, 7초마다 최대 §54§f개까지 화살이 보급됩니다.",
		"§5누군가의 공격으로 죽은 플레이어의 위치§f에 흔적이 남아 볼 수 있으며,",
		"흔적이 생성될 때마다 15초간 §5나침반§f이 가장 최근 흔적의 위치를 가리키고,",
		"5초간 무적 상태가 됩니다.",
		"흔적 위에 §5사망자명§f과 사망 이후 얼마나 §5시간§f이 지났는지 나타납니다.",
		"또한 흔적 위에 대상의 사망 당시 20칸 이내에 있던 사람의 §5명단§f이 나타납니다.",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다."
})
public class DetectivePsychometry extends AbstractDetective {

	private final ActionbarChannel actionbarChannel = newActionbarChannel();

	public DetectivePsychometry(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			PlayerInventory inventory = getPlayer().getInventory();
			inventory.clear();
			getPlayer().getInventory().setHeldItemSlot(0);
			inventory.setItem(1, Items.DETECTIVE_BOW.getStack());
			if (getArrowCount() < 4) {
				charge.start();
			}
			((MurderMystery) getGame()).updateGold(getParticipant());
			NMS.sendTitle(getPlayer(), "§e역할§f: §3탐정", "§c머더§f로부터 §a시민§f들을 보호하세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			passive.start();
		}
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId()));
		}
	};
	
	private Location lastBlood;
	private ActionbarChannel ac = newActionbarChannel();
	private ActionbarChannel ac2 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private GhostJobs ghostjobs = getGame().getModule(GhostJobs.class);
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (detector.isRunning()) {
				if (lastBlood != null) getPlayer().setCompassTarget(lastBlood);	
			} else {
				if (LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate) != null) {
					getPlayer().setCompassTarget(LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate).getLocation());	
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer skill = new AbilityTimer(TaskType.REVERSE, 100) {
		
		@Override
		public void onStart() {
			SoundLib.ITEM_ARMOR_EQUIP_CHAIN.playSound(getPlayer().getLocation(), 1f, 0.8f);
		}
		
		@Override
		public void run(int count) {
			ac2.update("§3무적§7: §b" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac2.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer detector = new AbilityTimer(300) {
		
		@Override
		public void run(int count) {
			ac.update("§c시체 추적§7: §e" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			lastBlood = null;
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();

	private final AbilityTimer charge = new AbilityTimer(14) {
		private final ProgressBar progressBar = new ProgressBar(14, 14);

		@Override
		protected void onStart() {
			progressBar.setStep(0);
		}

		@Override
		protected void run(int count) {
			if (getArrowCount() >= 4) {
				stop(true);
				return;
			}
			progressBar.step();
			actionbarChannel.update("화살 보급: " + progressBar.toString());
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
			addArrow();
			if (getArrowCount() < 4) {
				start();
			}
		}

		@Override
		protected void onSilentEnd() {
			actionbarChannel.update(null);
		}
	}.setPeriod(TimeUnit.TICKS, 10);

	@SubscribeEvent(onlyRelevant = true)
	private void onShoot(EntityShootBowEvent e) {
		charge.start();
	}

	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 8)) {
					if (!addArrow()) {
						murderMystery.addGold(getParticipant());
					}
				}
			}
		}
	}
	
	@SubscribeEvent(priority = 1)
	private void onMurderShield(MurderEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled() && skill.isRunning()) {
			e.setCancelled(true);
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
			ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
		}
	}
	
	@SubscribeEvent(priority = 1)
	private void onArrowShield(ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled() && skill.isRunning()) {
			e.setCancelled(true);
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
			ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
		}
	}

	@SubscribeEvent(priority = Priority.HIGHEST, ignoreCancelled = true)
	private void onMurder(final MurderEvent e) {
		List<Player> players = new ArrayList<>();
		for (Player player : LocationUtil.getNearbyEntities(Player.class, e.getTarget().getPlayer().getLocation(), 20, 20, predicate)) {
			players.add(player);
		}
		Collections.shuffle(players);
		final StringJoiner joiner = new StringJoiner(" §f/ ");
		for (Player player : players) {
			joiner.add("§c" + player.getName());
		}
		new Blood(e.getTarget().getPlayer().getLocation().clone().add(0, 1, 0), e.getTarget().getPlayer().getName(), joiner.toString(), e.getTarget()).start();
		lastBlood = e.getTarget().getPlayer().getLocation().clone().add(0, 1, 0);
		if (detector.isRunning()) detector.stop(false);
		detector.start();
		skill.start();
		skill.setCount(100);
	}

	@SubscribeEvent(priority = Priority.HIGHEST, ignoreCancelled = true)
	private void onArrowKill(final ArrowKillEvent e) {
		List<Player> players = new ArrayList<>();
		for (Player player : LocationUtil.getNearbyEntities(Player.class, e.getTarget().getPlayer().getLocation(), 20, 20, predicate)) {
			players.add(player);
		}
		Collections.shuffle(players);
		final StringJoiner joiner = new StringJoiner(" §f/ ");
		for (Player player : players) {
			joiner.add("§c" + player.getName());
		}
		new Blood(e.getTarget().getPlayer().getLocation().add(0, 1, 0), e.getTarget().getPlayer().getName(), joiner.toString(), e.getTarget()).start();
		lastBlood = e.getTarget().getPlayer().getLocation().clone().add(0, 1, 0);
		if (detector.isRunning()) detector.stop(false);
		detector.start();
		skill.start();
		skill.setCount(100);
	}

	public boolean hasBow() {
		ItemStack stack = getPlayer().getInventory().getItem(2);
		return stack != null && stack.getType() == Material.BOW;
	}

	public boolean addArrow() {
		ItemStack stack = getPlayer().getInventory().getItem(3);
		if (stack != null && stack.getType() == Material.ARROW) {
			if (stack.getAmount() < 64) {
				stack.setAmount(stack.getAmount() + 1);
				getPlayer().getInventory().setItem(3, stack);
				getPlayer().sendMessage("§8+ §f1 화살");
				return true;
			} else return false;
		} else {
			getPlayer().getInventory().setItem(3, new ItemStack(Material.ARROW));
			getPlayer().sendMessage("§8+ §f1 화살");
			return true;
		}
	}

	private static final RGB RED = RGB.of(254, 54, 54), DARK_RED = RGB.of(128, 8, 8), BLACK = RGB.of(1, 1, 1);

	private class Blood extends AbilityTimer {

		private final Location location;
		private final String name;
		private final String namelist;
		private final String job;
		private final IHologram hologram_list;
		private final IHologram hologram_name;
		private final IHologram hologram_job;
		private final IHologram hologram_time;
		private final DecimalFormat df = new DecimalFormat("0.0");

		private Blood(final Location location, String name, String namelist, Participant participant) {
			super();
			setPeriod(TimeUnit.TICKS, 1);
			this.location = location;
			this.name = name;
			this.namelist = namelist;
			this.job = ghostjobs.getGhostJob(participant).getManifest().name();
			this.hologram_list =  NMS.newHologram(location.getWorld(), location.getX(),
					location.getY() + 0.6, location.getZ(), namelist);
			this.hologram_name = NMS.newHologram(location.getWorld(), location.getX(),
					location.getY() + 0.6, location.getZ(), name);
			this.hologram_job = NMS.newHologram(location.getWorld(), location.getX(),
					location.getY() + 0.6, location.getZ(), job);
			this.hologram_time = NMS.newHologram(location.getWorld(), location.getX(),
					location.getY() + 0.6, location.getZ(), "0초");
			hologram_list.display(getPlayer());
			hologram_name.display(getPlayer());	
			hologram_time.display(getPlayer());
		}

		@Override
		protected void run(int count) {
			hologram_list.display(getPlayer());
			hologram_name.display(getPlayer());	
			hologram_time.display(getPlayer());
			hologram_job.display(getPlayer());
			hologram_list.teleport(location.clone().add(0, 2.2, 0));
			hologram_name.teleport(location.clone().add(0, 1.8, 0));
			hologram_job.teleport(location.clone().add(0, 1.4, 0));
			hologram_time.teleport(location.clone().add(0, 1, 0));
			hologram_list.setText(namelist);
			hologram_name.setText("§7" + name);
			hologram_time.setText("§b" + df.format(count / 20.0) + "초");
			ParticleLib.REDSTONE.spawnParticle(getPlayer(), location, count <= 600 ? RED : (count <= 1200 ? DARK_RED : BLACK));
		}
		
		@Override
		protected void onEnd() {
			hologram_name.unregister();
			hologram_time.unregister();
			hologram_list.unregister();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram_name.unregister();
			hologram_time.unregister();
			hologram_list.unregister();
		}

	}

}