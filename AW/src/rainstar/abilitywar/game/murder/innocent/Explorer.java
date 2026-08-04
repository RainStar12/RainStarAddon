package rainstar.abilitywar.game.murder.innocent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
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
import org.bukkit.inventory.ItemStack;
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
import daybreak.abilitywar.game.list.murdermystery.ability.Murderer;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.system.Shield;

@AbilityManifest(name = "시민: 탐험가", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§3긴 여행§8]§f 자신으로부터 150칸 이내 무작위 위치에 탐험지역이 생성됩니다.",
		"탐험지역에 도착하면 §8[§6유물§8]§f을 획득하고 탐험지역이 재생성됩니다.",
		"기본적으로 §b이동 속도 20% 증가§f를 받습니다.",
		"§8[§6유물§8]§f 확률에 따라 다른 등급의 유물을 획득하고, 효과가 각기 다릅니다:",
		"§b일반§8(§760%§8)§7: §f금 1~12개 혹은 화살 1~3발을 획득합니다.",
		"§a레어§8(§725%§8)§7: §f보호막 1~3개 / 투명화 30~60초를 얻습니다.",
		"§d에픽§8(§712%§8)§7: §f머더 직업명 한 글자 / 이속 40%로 강화 / 금 24~48개를 얻습니다.",
		"§e전설§8(§73%§8)§7: §f영구 유도력 2칸을 얻습니다. §8(§7중첩 가능§8)",
		"§8[§b안전한 여행§8]§f 금 좌클릭으로 금을 매 초마다 소모해 탐험지역 도착 전까지",
		"§b타게팅 불가 상태§f가 됩니다. 도착 혹은 금 부족 시 해제됩니다.",
		"금 부족으로 종료 시 탐험지역을 도착하기 전까지는 재사용할 수 없습니다."
})		
public class Explorer extends AbstractInnocent {

	public Explorer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a탐험가", "§f세상 곳곳을 탐험해보세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void run(int count) {
				}

				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			passive.start();
			speed.start();
		}
	}
	
	private Location spot;
	private final Random random = new Random();
	private final Circle circle = Circle.of(3, 33);
	private static final RGB MINT = RGB.of(9, 254, 204);
	private ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel(), ac3 = newActionbarChannel();
	private final Map<Participant, Integer> murderOrders = new HashMap<>();
	private int homing = 0;
	private DecimalFormat df = new DecimalFormat("0.0");
	private boolean useable = true;
	private final AttributeModifier modifier1 = new AttributeModifier(UUID.randomUUID(), "addspeed", 0.15, Operation.ADD_SCALAR);
	private final AttributeModifier modifier2 = new AttributeModifier(UUID.randomUUID(), "addspeedup", 0.4, Operation.ADD_SCALAR);
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue());
		}
	};
	
	private void addArrows(int count) {
		ItemStack stack = getPlayer().getInventory().getItem(3);
		if (stack != null && stack.getType() == Material.ARROW) {
			if (stack.getAmount() < 64) {
				stack.setAmount(stack.getAmount() + count);
				getPlayer().getInventory().setItem(3, stack);
				getPlayer().sendMessage("§8+ §f" + count + " 화살");
			}
		} else {
			getPlayer().getInventory().setItem(3, new ItemStack(Material.ARROW, count));
			getPlayer().sendMessage("§8+ §f" + count + " 화살");
		}
		if (!hasBow()) {
			getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
		}
	}
	
	private void getMurderJobNames() {
		List<String> joblist = new ArrayList<>();
		for (Participant participant : getGame().getParticipants()) {
			if (participant.getAbility() instanceof AbstractMurderer) {
				if (participant.getAbility().getClass().equals(Murderer.class)) {
					if (!murderOrders.containsKey(participant)) murderOrders.put(participant, murderOrders.size() + 1);
					joblist.add("§c머더 " + murderOrders.get(participant) + "§7: §5직업 없음");	
				} else {
					char jobchar;
					while(true) {
						jobchar = participant.getAbility().getDisplayName().charAt(random.nextInt(participant.getAbility().getDisplayName().length()));
						if (jobchar != ' ' && jobchar != '머' && jobchar != '더' && jobchar != ':') break;
					}
					if (!murderOrders.containsKey(participant)) murderOrders.put(participant, murderOrders.size() + 1);
					joblist.add("§c머더 " + murderOrders.get(participant) + "§7: §5" + Character.toLowerCase(jobchar));	
				}
			}
		}
		
		for (String jobs : joblist) {
			getPlayer().sendMessage(jobs);
		}
	}
	
	private void artifactGacha() {
		final MurderMystery murderMystery = (MurderMystery) getGame();
		int quality = random.nextInt(100) + 1;
		String qualityname = "§b일반";
		if (quality <= 60) {
			qualityname = "§b일반";
			if (random.nextInt(10) < 7) murderMystery.addGold(getParticipant(), random.nextInt(12) + 1);
			else addArrows(random.nextInt(3) + 1);
		} else if (quality <= 85) {
			qualityname = "§a레어";
			if (random.nextBoolean()) {
				if (random.nextInt(10) < 6) Shield.addShield(getParticipant(), 1);
				else if (random.nextInt(10) < 8) Shield.addShield(getParticipant(), 2);
				else Shield.addShield(getParticipant(), 3);
			} else PotionEffects.INVISIBILITY.addPotionEffect(getPlayer(), random.nextInt(601) + 600, 0, false, false, true);
		} else if (quality <= 97) {
			qualityname = "§d§l에픽";
			if (!speedup.isRunning()) {
				switch(random.nextInt(3)) {
				case 0:
					getMurderJobNames();
					break;
				case 1:
					speed.stop(false);
					speedup.start();
					getPlayer().sendMessage("§bSPEED UP!");
					break;
				case 2:
					murderMystery.addGold(getParticipant(), random.nextInt(25) + 24);
					break;
				}
			} else {
				if (random.nextBoolean()) getMurderJobNames();
				else murderMystery.addGold(getParticipant(), random.nextInt(25) + 24);
			}
		} else {
			qualityname = "§e§l전설";
			homing += 2;
			ac1.update("§5유도력 §d+" + homing);
		}
		SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer(), 1, 2);
		getPlayer().sendMessage("§3[§b!§3] " + qualityname + "§f 유물을 획득하셨습니다!");
	}
	
	private AbilityTimer speed = new AbilityTimer() {
		
		@Override
		public void onStart() {
			try {
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier1);
			} catch (IllegalArgumentException ignored) {
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier1);
		}
		
	}.register();
	
	private AbilityTimer speedup = new AbilityTimer() {
		
		@Override
		public void onStart() {
			try {
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier2);
			} catch (IllegalArgumentException ignored) {
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier2);
		}
		
	}.register();
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (spot == null) {
				Location loc = null;
				while(true) {
					Location location = LocationUtil.getRandomLocations(getPlayer().getLocation(), 150, 1).get(0);
					WorldBorder border = location.getWorld().getWorldBorder();
					if (border.isInside(location)) {
						loc = location;
						break;
					}
				}				
				loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), getPlayer().getLocation().getY(), loc.getBlockX(), loc.getBlockZ()));
				spot = loc;
			} else {
				if (count % 2 == 0) {
					for (Location location : circle.toLocations(spot).floor(spot.getY())) {
						ParticleLib.REDSTONE.spawnParticle(getPlayer(), location, MINT);
					}	
				}
				Location from = getPlayer().getLocation().clone();
				Location to = spot.clone();
				from.setY(0);
				to.setY(0);
				ac3.update("§a거리§7: §b" + df.format(Math.sqrt(from.distanceSquared(to))) + "M");
				if (LocationUtil.isInCircle(spot, getPlayer().getLocation(), 3)) {
					artifactGacha();
					if (!useable) useable = true;
					if (travelmode.isRunning()) travelmode.stop(false);
					spot = null;
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer travelmode = new AbilityTimer() {
		
		@Override
		public void onStart() {
			ac2.update("§3안전한 여행 중");
		}
		
		@Override
		public void run(int count) {
			getParticipant().attributes().TARGETABLE.setValue(false);
			if (count % 20 == 0) {
				final MurderMystery murderMystery = (MurderMystery) getGame();
				if (!murderMystery.consumeGold(getParticipant(), 1)) {
					travelmode.stop(false);
					useable = false;
				}
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			getParticipant().attributes().TARGETABLE.setValue(true);
			ac2.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
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
				if (useable) travelmode.start();	
				else getPlayer().sendMessage("§4[§c!§4] §3탐험 지역§c에 도착해야 재사용 가능합니다.");
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
			if (homing > 0) {
				new Homing((Arrow) e.getEntity(), e.getEntity().getVelocity().length()).start();
			}
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
					if (arrow.getLocation().distanceSquared(p.getEyeLocation()) <= (homing * homing)) {
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

}
