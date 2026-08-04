package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
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
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.manager.effect.Fear;
import daybreak.abilitywar.utils.base.ProgressBar;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "머더: 사신", rank = Rank.SPECIAL, species = Species.GOD, explain = {
		"모든 시민을 죽이세요!",
		"활과 화살을 반납하고 금으로 돌려받습니다.",
		"§8[§4괴담§8]§f 전직 후 2분 이내에 사신이 있다는 것이 공개됩니다.",
		"§8[§b환영§8]§f 주변 11칸 이내에 §a시민팀§f이 없다면 §7투명 효과§f를 받습니다.",
		"§7투명 효과§f를 받는 중에는 §b신속 2§f를 받지만 해제될 때 소리가 납니다.",
		"또한 §7투명 효과§f 중엔 §e금§f을 주울 때 소리가 나며 작은 파티클이 튑니다.",
		"§8[§9악몽§8]§f 금 좌클릭 시 금 20개를 소모해 다음 §8[§b환영§8]§f 해제 시 범위 내",
		"모든 대상을 1초간 §9§n공포§f에 빠뜨립니다.",
		"§8[§c수확§8]§f 누군가가 죽으면 그 자리에 §b영혼§f이 남아 웅크려서 수확 가능합니다.",
		"수확 중에는 은신이 풀리며 수확 시 §8[§b환영§8]§f 범위가 §8(§711/총 시민팀 수§8)§f만큼 줄어듭니다.",
		"수확 시마다 §9§n공포§f 지속시간이 0.4초 증가합니다."
})
public class GrimReaperMurderer extends AbstractMurderer {
	
	public GrimReaperMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
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
			
			NMS.sendTitle(getPlayer(), "§e직업§f: §b사신", "§f모든 §a시민§f을 저승으로 보내세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.start();
			passive.start();
			ac2.update("§8[§7악몽§8]");
			for (Participant participant : getGame().getParticipants()) {
				if (participant.hasAbility() && !(participant.getAbility() instanceof AbstractMurderer)) {
					innocents++;
				}
			}
			
			new AbilityTimer(1) {
				@Override
				protected void run(int count) {
					Bukkit.broadcastMessage("§7§o도시에 오싹한 기운이 감돕니다...");
				}
			}.setInitialDelay(TimeUnit.SECONDS, random.nextInt(121)).start();
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}
	
	private int innocents = 1, fearstack = 0;
	private double range = 11;
	private Set<Location> souls = new HashSet<>();
	private static final RGB cyan = RGB.of(167, 254, 233), skyblue = RGB.of(160, 227, 245);
	private final ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel();
	private Circle circle = Circle.of(11, 110);
	private DecimalFormat df = new DecimalFormat("0.0");
	private boolean wasInvisible = false, fearmode = false;

	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& !(getGame().getParticipant(entity.getUniqueId()).getAbility() instanceof AbstractMurderer))
					&& !((Player) entity).getGameMode().equals(GameMode.SPECTATOR);
		}
	};
	
	private final Predicate<Entity> fearPredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& !(getGame().getParticipant(entity.getUniqueId()).getAbility() instanceof AbstractMurderer))
					&& !((Player) entity).getGameMode().equals(GameMode.SPECTATOR)
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue();
		}
	};
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (!fearmode) {
					final MurderMystery murderMystery = (MurderMystery) getGame();
					if (murderMystery.consumeGold(getParticipant(), 20)) {
						getPlayer().sendMessage("§4[§c!§4] §9악몽§f이 준비됩니다.");
						fearmode = true;
						ac2.update("§5[§9악몽§5]");
					}
				} else getPlayer().sendMessage("§4[§c!§4] §c이미 악몽을 준비 중입니다.");
			}
		}
	}
	
	@SubscribeEvent(priority = 999)
	private void onMurdered(MurderEvent e) {
		if (!e.isCancelled()) souls.add(e.getTarget().getPlayer().getLocation().clone().add(0, 1, 0));
	}
	
	@SubscribeEvent(priority = 999)
	private void onArrowKilled(ArrowKillEvent e) {
		if (!e.isCancelled()) {
			if (e.getShooter().getAbility() instanceof AbstractMurderer) {
				souls.add(e.getTarget().getPlayer().getLocation().clone().add(0, 1, 0));	
			} else if (e.getTarget().getAbility() instanceof AbstractMurderer) {
				souls.add(e.getTarget().getPlayer().getLocation().clone().add(0, 1, 0));
			} else {
				souls.add(e.getTarget().getPlayer().getLocation().clone().add(0, 1, 0));
				souls.add(e.getShooter().getPlayer().getLocation().clone().add(0, 1, 0));
			}
		}
	}
	
	@SubscribeEvent
	private void onPickUp(EntityPickupItemEvent e) {
		if (e.getEntity().equals(getPlayer()) && wasInvisible) {
			MurderMystery murderMystery = (MurderMystery) getGame();
			if (murderMystery.getGold(getParticipant()) < 64 && e.getItem().getItemStack().isSimilar(Items.GOLD.getStack())) {
				SoundLib.BLOCK_NOTE_BLOCK_CHIME.playSound(getPlayer().getLocation(), 0.8f, 2);
				ParticleLib.TOTEM.spawnParticle(getPlayer().getEyeLocation(), 0, 0, 0, 1, 0.5);
			}
		}	
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (circle.size() > range) circle = Circle.of(range, (int) (range * 10));
			if (count % 2 == 0) {
				for (final Location locs : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), locs, skyblue);
				}
			}
			
			if (soulearning.isRunning()) {
				NMS.setInvisible(getPlayer(), false);
			} else {
				if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate).size() <= 0) NMS.setInvisible(getPlayer(), true);
				else NMS.setInvisible(getPlayer(), false);
			}
			
			if (NMS.isInvisible(getPlayer())) {
				PotionEffects.SPEED.addPotionEffect(getPlayer(), 20, 1, false, false, true);
				ac1.update("§7은신 중");
				wasInvisible = true;
			} else {
				getPlayer().removePotionEffect(PotionEffectType.SPEED);
				ac1.update(null);
				if (fearmode) {
					if (wasInvisible) {
						new BukkitRunnable() {
							
							@Override
							public void run() {
								for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, fearPredicate)) {
									SoundLib.ENTITY_WITHER_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
									Fear.apply(getGame().getParticipant(player), TimeUnit.TICKS, 20 + (fearstack * 8), getPlayer());
								}
								ac2.update("§8[§7악몽§8]");
								fearmode = false;
							}
						}.runTaskLater(AbilityWar.getPlugin(), 2L);
					} else {
						for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, fearPredicate)) {
							SoundLib.ENTITY_WITHER_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
							Fear.apply(getGame().getParticipant(player), TimeUnit.TICKS, 20 + (fearstack * 8), getPlayer());
						}
						ac2.update("§8[§7악몽§8]");
						fearmode = false;	
					}
				}
				if (wasInvisible) {
					SoundLib.AMBIENT_CAVE.playSound(getPlayer().getLocation(), 1f, 2f);	
					wasInvisible = false;
				}
			}
			
			for (Location soul : souls) {
				if (getPlayer().getLocation().distanceSquared(soul) <= 7 && getPlayer().isSneaking()) {
					soulearning.start();
				}
				ParticleLib.REDSTONE.spawnParticle(getPlayer(), soul, cyan);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer soulearning = new AbilityTimer(100) {
		
		private double counts;
		private ProgressBar progressBar;
		
		@Override
		protected void onStart() {
			this.counts = 0;
			this.progressBar = new ProgressBar(100, 20);
		}
		
		@Override
		public void run(int count) {
			this.counts += 1;
			boolean isEarning = false;
			for (Location soul : souls) {
				if (getPlayer().isSneaking() && getPlayer().getLocation().distanceSquared(soul) <= 7) {
					isEarning = true;
				}
			}
			if (isEarning) {
				progressBar.step();

				NMS.sendTitle(getPlayer(),
						"§b영혼 흡수 중...",
						progressBar.toString() + " §f" + df.format(this.counts) + "%",
						0, 5, 0
				);
			} else {
				this.stop(true);
			}
		}
		
		@Override
		public void onEnd() {
			Set<Location> earnSoul = new HashSet<>();
			
			for (Location soul : souls) {
				if (getPlayer().isSneaking() && getPlayer().getLocation().distanceSquared(soul) <= 7) {
					earnSoul.add(soul);
					getPlayer().sendMessage("§4[§c!§4] §b영혼을 흡수하였습니다.");
					range = Math.max(0, range - (11.0 / innocents));
					SoundLib.ITEM_BOTTLE_FILL_DRAGONBREATH.playSound(getPlayer(), 1, 1.75f);
					fearstack++;
				}
			}
			
			NMS.clearTitle(getPlayer());
			souls.removeAll(earnSoul);
		}
		
		@Override
		public void onSilentEnd() {
			NMS.clearTitle(getPlayer());
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
}
