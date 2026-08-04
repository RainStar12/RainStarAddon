package rainstar.abilitywar.game.murder.innocent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.boundary.CenteredBoundingBox;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.ItemLib;

@AbilityManifest(name = "시민: 군인", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 4개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§c정조준§8]§f 화살 대신 일직선으로 나아가고 지형지물을 관통하는",
		"특수 투사체를 발사합니다. 투사체는 매우 빠르나 히트박스가 작습니다.",
		"이 투사체는 5초 이내로 두 발 맞혀야 적중한 것으로 판정합니다.",
		"§8[§3경계§8]§f §c머더§f에게 공격받으면 막아내고 주변을 밀쳐냅니다. §8(§71회용§8)"
})		
public class Soldier extends AbstractInnocent {

	public Soldier(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a군인", "§f모든 §c머더§f의 습격을 막아내고 반격하세요!", 10, 80, 10);
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
	
	private final ActionbarChannel ac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private Map<Participant, Stack> stackMap = new HashMap<>();
	private boolean actived = false;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (getPlayer().equals(entity)) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}
	};

	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 4)) {
					if (!addArrow()) {
						murderMystery.addGold(getParticipant());
					} else {
						if (!hasBow()) {
							getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
						}
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(EntityShootBowEvent e) {
		if (getPlayer().equals(e.getEntity()) && e.getProjectile() instanceof Arrow) {
			e.setCancelled(true);
			ItemLib.removeItem(getPlayer().getInventory(), Material.ARROW, 1);
			new Bullet(getPlayer(), getPlayer().getEyeLocation(), getPlayer().getLocation().getDirection()).start();
			SoundLib.ENTITY_WITHER_BREAK_BLOCK.playSound(getPlayer().getLocation(), 4f, 2f);
		}
	}
	
	@SubscribeEvent(priority = 1)
	private void onMurder(MurderEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled()) {
			boolean block = false;
			if (skill.isRunning()) block = true;
			else if (!actived) {
				skill.start();
				block = true;
			}
			
			if (block) {
				e.setCancelled(true);
				SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
				ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
				getPlayer().sendMessage("§2[§8!§2] §a머더의 공격을 막아냈습니다!");
				for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 5, 5, predicate)) {
					player.setVelocity(VectorUtil.validateVector(player.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().multiply(3).setY(0)));
				}
			}
		}
	}
	
	@SubscribeEvent(priority = 1)
	private void onArrowKill(ArrowKillEvent e) {
		if (e.getShooter().getAbility() instanceof AbstractMurderer && e.getTarget().equals(getParticipant()) && !e.isCancelled()) {
			boolean block = false;
			if (skill.isRunning()) block = true;
			else if (!actived) {
				skill.start();
				block = true;
			}
			
			if (block) {
				e.setCancelled(true);
				SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
				ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
				getPlayer().sendMessage("§2[§8!§2] §a머더의 공격을 막아냈습니다!");
				for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 5, 5, predicate)) {
					player.setVelocity(VectorUtil.validateVector(player.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().multiply(3).setY(0)));
				}
			}
		}
	}
	
	private final AbilityTimer skill = new AbilityTimer(4) {

		@Override
		protected void onStart() {
			actived = true;
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			ac.update(null);
		}

		@Override
		protected void run(int count) {
			ac.update("§3저항§f: §3" + df.format(count / 20.0) + "§f초");
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
    
	public class Bullet extends AbilityTimer {

		private final LivingEntity shooter;
		private final CenteredBoundingBox boundingBox;
		private final Vector forward;
		private final Predicate<Entity> predicate;

		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity) {
			super(30);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.boundingBox = CenteredBoundingBox.of(startLocation, -.175, -.175, -.175, .175, .175, .175);
			this.forward = arrowVelocity.multiply(10);
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity.equals(getPlayer())) return false;
					return (entity instanceof Player) && getGame().isParticipating(entity.getUniqueId())
							&& !((MurderMystery) getGame()).isDead(entity.getUniqueId());
				}
			};
		}

		private Location lastLocation;

		@Override
		protected void run(int i) {
			final Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(.35);
				private final int amount = (int) (vectorBetween.length() / 0.35);
				private int cursor = 0;

				@Override
				public boolean hasNext() {
					return cursor < amount;
				}

				@Override
				public Location next() {
					if (cursor >= amount) throw new NoSuchElementException();
					cursor++;
					return lastLocation.clone().add(unit.clone().multiply(cursor));
				}
			}; iterator.hasNext(); ) {
				final Location location = iterator.next();
				boundingBox.setCenter(location);
				final double y = location.getY();
				if (y < 0 || y > 256 || !location.getChunk().isLoaded()) {
					stop(false);
					return;
				}
				for (Player player : LocationUtil.getConflictingEntities(Player.class, shooter.getWorld(), boundingBox, predicate)) {
					if (!shooter.equals(player)) {
						if (stackMap.containsKey(getGame().getParticipant(player))) {
							if (((MurderMystery) getGame()).fireArrowHitEvent(getPlayer(), player)) {
								stackMap.get(getGame().getParticipant(player)).stop(false);
							}
						} else new Stack(getGame().getParticipant(player)).start();
						stop(false);
						return;
					}
				}
				ParticleLib.TOWN_AURA.spawnParticle(location);
			}
			lastLocation = newLocation;
		}

	}
	
	public class Stack extends AbilityTimer {
		
		private final Participant participant;
		private final IHologram hologram;
		@SuppressWarnings("serial")
		private List<String> colors = new ArrayList<String>() {{
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§4");
			add("§4");
			add("§4");
			add("§4");
			add("§4");
			add("§4");
			add("§4");
			add("§5");
			add("§5");
			add("§5");
			add("§5");
			add("§5");
			add("§5");
			add("§4");
			add("§4");
			add("§4");
			add("§4");
			add("§4");
			add("§4");
			add("§4");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
			add("§c");
		}};
		
		private Stack(Participant participant) {
			super(TaskType.REVERSE, 100);
			setPeriod(TimeUnit.TICKS, 1);
			this.participant = participant;
			this.hologram = NMS.newHologram(participant.getPlayer().getWorld(), participant.getPlayer().getLocation().getX(),
					participant.getPlayer().getLocation().getY() + participant.getPlayer().getEyeHeight() + 0.6, participant.getPlayer().getLocation().getZ(), 
					"§c§l✗");
			hologram.display(getPlayer());
			stackMap.put(participant, this);
			SoundLib.ENTITY_PLAYER_HURT.playSound(participant.getPlayer().getLocation(), 1, 1);
			Healths.setHealth(participant.getPlayer(), Math.max(1, participant.getPlayer().getHealth() - 1));
		}
		
		@Override
		protected void run(int count) {
			hologram.teleport(participant.getPlayer().getWorld(), participant.getPlayer().getLocation().getX(), 
					participant.getPlayer().getLocation().getY() + participant.getPlayer().getEyeHeight() + 0.6, participant.getPlayer().getLocation().getZ(), 
					participant.getPlayer().getLocation().getYaw(), 0);
			hologram.setText(colors.get(count % 40) + "§l✗");
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			stackMap.remove(participant);
			hologram.unregister();
		}
		
	}
	
}
