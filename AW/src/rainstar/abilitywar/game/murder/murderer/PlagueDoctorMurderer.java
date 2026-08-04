package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.PlayerInventory;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.system.config.variabletype.NoKill;

@AbilityManifest(name = "머더: 역병의사", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 감염시키세요! 살인자의 칼을 반납하고 화살 한 발을 얻고 시작합니다.",
		"금 우클릭으로 금 13개를 소모해 활과 화살을 얻을 수 있습니다.",	
		"§8[§a독화살§8]§f 화살이 적중한 지역에 즉시 §2역병 지대§f를 생성합니다.",
		"§8[§2역병§8]§f 금괴를 좌클릭하면 금괴를 8개 소모해 주변 9칸 내",
		"모든 대상에게 §2역병§f을 부여합니다. §2역병§f은 부여된 후 30~60초가 지나면",
		"발병하기 시작하고, 30초간 구속 및 화살 감속 후 §4처형§f됩니다.",
		"§8[§4즉살§8]§f 금 F키로 모든 §2역병§f 보유자들을 즉시 §4처형§f시킵니다§8(§71회용§8)§f.",
		"직업 배정 후 2분 30초 전까진 사용할 수 없습니다.",
		"§8[§3전염§8]§f §2역병§f을 가진 시민은 다른 플레이어가 6.5칸 내로 접촉하면",
		"대상도 §2역병§f이 부여됩니다.",
		"§8[§c역병 지대§8]§f §2역병§f으로 사망한 시체 5.5칸 내에 접근 시 감염됩니다.",
		"§8[§a해독§8]§f 자신이 사망한다면 모든 §2역병§f이 해제됩니다."
})
public class PlagueDoctorMurderer extends AbstractMurderer {
	
	public PlagueDoctorMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			final MurderMystery murderMystery = (MurderMystery) getGame();
			final PlayerInventory inventory = getPlayer().getInventory();
			final int arrowCount = getArrowCount();
			inventory.clear();
			murderMystery.updateGold(getParticipant());
			murderMystery.addGold(getParticipant(), arrowCount * 8);
			addArrow();
			if (!hasBow()) {
				getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
			}
			NMS.sendTitle(getPlayer(), "§e직업§f: §2역병의사", "§f모든 §a시민§f과 §5탐정§f을 감염시키세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			
			passive.start();
			cantskill.start();
			cantskill.setCount(3000);
			murderMystery.addGold(getParticipant(), 5);
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
			if (!plagues.isEmpty()) {
				List<Participant> infected = new ArrayList<>();
				for (Participant participant : plagues.keySet()) {
					infected.add(participant);
				}	
				for (Participant infecteds : infected) {
					plagues.get(infecteds).stop(true);
				}
			}
		}
	}
	
	private Map<Participant, Plague> plagues = new HashMap<>();
	private Circle circle = Circle.of(9, 90);
	private Circle circle2 = Circle.of(5.5, 32);
	private static final RGB green = RGB.of(28, 141, 1);
	private boolean onetime = false;
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId()))
					&& !(getGame().getParticipant(entity.getUniqueId()).getAbility() instanceof AbstractMurderer)
					&& !plagues.containsKey(getGame().getParticipant(entity.getUniqueId()));
		}
	};
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			for (final Location locs : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(getPlayer(), locs, green);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	public AbilityTimer cantskill = new AbilityTimer(100) {
		
		@Override
		public void run(int count) {
			ac.update("§2즉살 불가§7: §c" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onProjectileHit(ProjectileHitEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter())) {
			Location hitLoc = e.getEntity().getLocation();
			if (e.getHitBlock() != null) hitLoc = e.getHitBlock().getLocation();
			else if (e.getHitEntity() != null) hitLoc = e.getHitEntity().getLocation();
			if (getGame().hasModule(NoKill.class)) {
				if (getGame().getModule(NoKill.class).isKillTime()) {
					new PlagueArea(hitLoc, 100).start();
				}
			}
		}
	}
	
    @SubscribeEvent(onlyRelevant = true, priority = 0)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (Items.isGold(e.getOffHandItem()) && e.getPlayer().equals(getPlayer())) {
    		if (!onetime) {
    			if (plagues.keySet().size() > 0) {
    				if (!cantskill.isRunning()) {
                		getPlayer().sendMessage("§4[§c!§4] §c역병을 가진 모든 대상을 처형합니다...");
                		List<Participant> participants = new ArrayList<>();
                		for (Participant participant : plagues.keySet()) {
                			participants.add(participant);
                		}
                		for (Participant participant : participants) {
                			plagues.get(participant).stop(false);
                		}
                		participants.clear();
                		onetime = true;		
    				} else getPlayer().sendMessage("§4[§c!§4] §c아직 즉살을 사용할 수 없습니다.");
    			} else getPlayer().sendMessage("§4[§c!§4] §c역병을 가진 대상이 없습니다.");
    		} else getPlayer().sendMessage("§4[§c!§4] §c이미 즉살을 사용하였습니다.");
    	}
    }
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (murderMystery.consumeGold(getParticipant(), 12)) {
					if (!addArrow()) {
						murderMystery.addGold(getParticipant());
					} else {
						if (!hasBow()) {
							getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
						}
					}
				}
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 9, 9, predicate).size() > 0) {
					if (getGame().hasModule(NoKill.class)) {
						if (getGame().getModule(NoKill.class).isKillTime()) {
							if (murderMystery.consumeGold(getParticipant(), 8)) {
								for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 9, 9, predicate)) {
									new Plague(getGame().getParticipant(player), random.nextInt(401) + 1200).start();
									SoundLib.BLOCK_BREWING_STAND_BREW.playSound(getPlayer(), 1, 0.5f);
								}
								getPlayer().sendMessage("§4[§c!§4] §2역병§f을 사용하였습니다.");
							}
						} else getPlayer().sendMessage("§4[§c!§4] §c아직 공격 가능 시간이 아닙니다.");
					} else {
						if (murderMystery.consumeGold(getParticipant(), 8)) {
							for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 9, 9, predicate)) {
								new Plague(getGame().getParticipant(player), random.nextInt(401) + 1200).start();
								SoundLib.BLOCK_BREWING_STAND_BREW.playSound(getPlayer(), 1, 0.5f);
							}
							getPlayer().sendMessage("§4[§c!§4] §2역병§f을 사용하였습니다.");
						}
					}
				} else getPlayer().sendMessage("§4[§c!§4] §c범위 내에 대상이 없습니다.");
			}
		}
	}
	
	public class Plague extends AbilityTimer implements Listener {
		
		private final Participant participant;
		private final ActionbarChannel ac;
		private final DecimalFormat df = new DecimalFormat("0.0");
		
		private Plague(Participant participant, int duration) {
			super(TaskType.REVERSE, duration);
			setPeriod(TimeUnit.TICKS, 1);
			this.participant = participant;
			this.ac = participant.actionbar().newChannel();
			plagues.put(participant, this);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int count) {
			if (((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId())) {
				stop(true);
			}
			for (Player player : LocationUtil.getNearbyEntities(Player.class, participant.getPlayer().getLocation(), 6.5, 6.5, predicate)) {
				new Plague(getGame().getParticipant(player), random.nextInt(600) + 1200).start();
			}
			if (count <= 600) {
				ac.update("§2역병§7: §c" + df.format(count / (double) 20) + "§f초");
				PotionEffects.SLOW.addPotionEffect(participant.getPlayer(), 100, 0, false, false, true);
				if (count % 2 == 0) ParticleLib.ITEM_CRACK.spawnParticle(participant.getPlayer().getLocation(), 0.3, 1, 0.3, 25, 0, MaterialX.SLIME_BLOCK);
			}
			for (Participant murderers : getGame().getParticipants()) {
				if (murderers.getAbility() instanceof AbstractMurderer) ParticleLib.REDSTONE.spawnParticle(murderers.getPlayer(), participant.getPlayer().getLocation().clone().add(0, 2.2, 0), green);
			}
		}
		
		@EventHandler()
		public void onProjectileLaunch(ProjectileLaunchEvent e) {
			if (participant.getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
				e.getEntity().setVelocity(e.getEntity().getVelocity().multiply(0.66));
			}
		}
		
		@Override
		protected void onEnd() {
			SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(participant.getPlayer().getLocation(), 1, 0.5f);	
			new PlagueArea(participant.getPlayer().getLocation(), 0).start();
			MurderEvent event = new MurderEvent(PlagueDoctorMurderer.this, participant);
			event.setCancelled(true);
			Bukkit.getPluginManager().callEvent(event);
			participant.getPlayer().setHealth(0);	
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			plagues.remove(participant);
			ac.unregister();
			HandlerList.unregisterAll(this);
		}
		
	}
	
	public class PlagueArea extends AbilityTimer {
		
		private final Location location;
		
		private PlagueArea(Location location, int wait) {
			super(TaskType.INFINITE, -1);
			setPeriod(TimeUnit.TICKS, 1);
			this.location = location;
		}
		
		@Override
		protected void run(int count) {
			if (count % 10 == 0) {
				for (Participant participant : getGame().getParticipants()) {
					if (participant.getAbility() instanceof AbstractMurderer) {
						for (final Location locs : circle2.toLocations(location).floor(location.getY())) {
							ParticleLib.REDSTONE.spawnParticle(participant.getPlayer(), locs, green);
						}
					}
				}	
			}
			for (Player player : LocationUtil.getNearbyEntities(Player.class, location, 5.5, 5.5, predicate)) {
				new Plague(getGame().getParticipant(player), random.nextInt(600) + 1200).start();
			}	
		}
		
	}
	
}
