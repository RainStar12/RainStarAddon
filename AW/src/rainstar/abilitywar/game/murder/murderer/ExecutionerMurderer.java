package rainstar.abilitywar.game.murder.murderer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractJob;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "머더: 처형인", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"활과 화살을 얻을 수 없고, 기존에 소지하던 것도 금으로 치환됩니다.",
		"§8[§4처형§8]§f 금 우클릭으로 금 4개를 소모해 다음 공격을 §4처형§f으로 만듭니다.",
		"§8[§c살상§8]§f 검 이동 속도가 20% 증가합니다. 검의 사거리가 4칸 증가합니다.",
		"금 좌클릭으로 금 30개를 소모해 검의 사거리를 1 증가시킬 수 있습니다.",
		"시민을 처치할 때마다 §e금§f 10개와 대상이 가진 §e금§f을 가져옵니다.",
		"금이 최대치를 초과할 경우, 자동으로 30금을 소모해 사거리를 증가시킵니다."
})
public class ExecutionerMurderer extends AbstractMurderer {
	
	public ExecutionerMurderer(Participant participant) {
		super(participant);
	}

	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			final PlayerInventory inventory = getPlayer().getInventory();
			final boolean hadSword = Items.isMurdererSword(inventory.getItem(1));
			final int arrowCount = getArrowCount();
			inventory.clear();
			final MurderMystery murderMystery = (MurderMystery) getGame();
			murderMystery.addGold(getParticipant(), arrowCount * 8);
			getPlayer().getInventory().setHeldItemSlot(0);
			murderMystery.updateGold(getParticipant());
			if (!hadSword) {
				getPlayer().sendMessage("§e15초 §f뒤에 §4살인자§c의 검§f을 얻습니다.");
				new AbilityTimer(1) {
					@Override
					protected void run(int count) {
						getPlayer().getInventory().setHeldItemSlot(0);
						inventory.setItem(1, Items.MURDERER_SWORD.getStack());
						getPlayer().sendMessage("§4살인자§c의 검§f을 들고 있을 때 더 빠르게 움직일 수 있습니다.");
						for (Player player : Bukkit.getOnlinePlayers()) {
							NMS.sendTitle(player, "§4머더§c가 검을 얻었습니다.", "", 10, 80, 10);
							new AbilityTimer(1) {
								@Override
								protected void onEnd() {
									NMS.clearTitle(player);
								}
							}.setInitialDelay(TimeUnit.SECONDS, 5).start();
						}
					}
				}.setInitialDelay(TimeUnit.SECONDS, 15).start();
			} else {
				getPlayer().getInventory().setHeldItemSlot(0);
				inventory.setItem(1, Items.MURDERER_SWORD.getStack());
			}
			
			NMS.sendTitle(getPlayer(), "§e직업§f: §4처형인", "§f모든 시민을 §4처형§f시키세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.stop(false);
			speedpassive.start();
			ac1.update("§c검 사거리§7: §b+" + range);
			ac2.update(execution ? "§4[§c처형§4]" : "§8[§7처형§8]");
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}
	
	protected final AbilityTimer speedpassive = new AbilityTimer() {
		@Override
		protected void run(int count) {
			if (Items.isMurdererSword(getPlayer().getInventory().getItemInMainHand())) {
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.18);
			}
		}
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private int range = 4;
	private ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel();
	private boolean execution = false;
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (!execution) {
					final MurderMystery murderMystery = (MurderMystery) getGame();
					if (murderMystery.consumeGold(getParticipant(), 4)) {
						getPlayer().sendMessage("§4[§c!§4] §c처형 준비.");
						execution = true;
						ac2.update(execution ? "§4[§c처형§4]" : "§8[§7처형§8]");
						SoundLib.ENTITY_ZOMBIE_VILLAGER_CONVERTED.playSound(getPlayer(), 1, 2f);
					}	
				} else getPlayer().sendMessage("§4[§c!§4] §c이미 처형이 준비되어 있습니다.");
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 30)) {
					getPlayer().sendMessage("§4[§c!§4] §b검 사거리가 1 증가하였습니다.");
					range++;
					ac1.update("§c검 사거리§7: §b+" + range);
					SoundLib.BLOCK_ANVIL_USE.playSound(getPlayer(), 1, 1.33f);
				}
			}
		} else if (Items.isMurdererSword(e.getItem())) {
			if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				final ItemStack mainHand = getPlayer().getInventory().getItemInMainHand();
				if (Items.isMurdererSword(mainHand)) {
					new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1.5, 0), getPlayer().getLocation().getDirection()).start();
				}
			}
		}
	}
	
	@SubscribeEvent(priority = 0)
	private void onMurder(MurderEvent e) {
		if (e.getMurderer().getParticipant().equals(getParticipant())) {
			if (execution) {
				e.setCancelled(true);
				SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(e.getTarget().getPlayer().getLocation(), 1, 0.5f);
				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(e.getTarget().getPlayer().getLocation(), 1, 0.9f);
				ParticleLib.CRIT.spawnParticle(e.getTarget().getPlayer().getLocation().add(0, 1, 0), .3f, .3f, .3f, 100, 1);
				ParticleLib.ITEM_CRACK.spawnParticle(e.getTarget().getPlayer().getEyeLocation(), .3f, .3f, .3f, 100, 0.5, MaterialX.REDSTONE);
				MurderMystery murderMystery = (MurderMystery) getGame();
				AbstractJob job = (AbstractJob) e.getTarget().getAbility();
				int arrowcount = job.getArrowCount();
				int addcount = murderMystery.getGold(e.getTarget()) + 10 + (arrowcount * 4);
				while ((murderMystery.getGold(getParticipant()) + addcount) > 64) {
					addcount -= 30;
					getPlayer().sendMessage("§4[§c!§4] §b검 사거리가 1 증가하였습니다.");
					range++;
					ac1.update("§c검 사거리§7: §b+" + range);
					SoundLib.BLOCK_ANVIL_USE.playSound(getPlayer(), 1, 1.33f);
				}
				murderMystery.addGold(getParticipant(), addcount);
				e.getTarget().getPlayer().setHealth(0);
				execution = false;
				ac2.update(execution ? "§4[§c처형§4]" : "§8[§7처형§8]");
			}
		}
	}
	
	@SubscribeEvent(priority = 999)
	private void onMurdered(MurderEvent e) {
		if (e.getMurderer().getParticipant().equals(getParticipant()) && !e.isCancelled()) {
			MurderMystery murderMystery = (MurderMystery) getGame();
			AbstractJob job = (AbstractJob) e.getTarget().getAbility();
			int arrowcount = job.getArrowCount();
			int addcount = murderMystery.getGold(e.getTarget()) + 10 + (arrowcount * 4);
			while ((murderMystery.getGold(getParticipant()) + addcount) > 64) {
				addcount -= 30;
				getPlayer().sendMessage("§4[§c!§4] §b검 사거리가 1 증가하였습니다.");
				range++;
				ac1.update("§c검 사거리§7: §b+" + range);
				SoundLib.BLOCK_ANVIL_USE.playSound(getPlayer(), 1, 1.33f);
			}
			murderMystery.addGold(getParticipant(), addcount);
		}
	}

	public class Bullet extends AbilityTimer {

		private final Player shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final Predicate<Entity> predicate;
		private final Set<Player> attacked = new HashSet<>();
		private final Location startLocation;
		private Location lastLocation;

		private Bullet(Player shooter, Location startLocation, Vector arrowVelocity) {
			super(1000);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-0.6, -0.6, -0.6, 0.6, 0.6, 0.6);
			this.forward = arrowVelocity.multiply(10);
			this.lastLocation = startLocation;
			this.startLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity.equals(getPlayer())) return false;
					return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
							&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
							&& !(getGame().getParticipant(entity.getUniqueId()).getAbility() instanceof AbstractMurderer));
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
		}

		@Override
		protected void run(int i) {
			final Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
				private final int amount = (int) (vectorBetween.length() / 0.1);
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
				entity.setLocation(location);
				if (!isRunning()) {
					return;
				}
				final Block block = location.getBlock();
				final Material type = block.getType();
				if (type.isSolid()) {
					stop(true);
					return;
				}
				if (startLocation.distanceSquared(location) > (9 + (range * range))) {
					stop(true);
					return;
				}
				for (Player player : LocationUtil.getConflictingEntities(Player.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(player) && !attacked.contains(player)) {
						attacked.add(player);
						MurderEvent event = new MurderEvent(ExecutionerMurderer.this, getGame().getParticipant(player));
						Bukkit.getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							player.setHealth(0);	
						}
					}
				}
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			entity.remove();
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
		}

		public class ArrowEntity extends CustomEntity {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			protected void onRemove() {
				Bullet.this.stop(false);
			}

		}

	}

}
