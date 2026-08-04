package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import kotlin.ranges.RangesKt;

@AbilityManifest(name = "머더: 배신자 유다", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 8개를 소모해 활과 화살을 얻을 수 있습니다.",
		"§8[§9배신의 값§8]§f 금 30개를 획득하고 시작합니다.",
		"30초마다 무작위로 신벌 중 하나를 10초간 받습니다.",
		"공격 불가 §7/§f 10칸 내 화살 자신에게 유도 §7/§f 구속 1/2/3 §7/§f 실명",
		"랜덤 화면 전환 §7/§f 슬롯바 무작위 교체 §7/§f 환청 §7/§f 멀미",
		"§c[§8흑마술§c]§f 검 우클릭 시 금 6개를 소모해 2.5초간 §3은신§f 상태가 되어",
		"이동 속도가 매우 높아집니다. 이때 누군가를 직접 해칠 수 없으며",
		"사람을 §b스쳐 지나갈 수§f 있습니다. §c쿨타임 §7: §f8초",
		"사용 시와 종료 시, 각각 0.3초간은 §3무적§f 상태가 됩니다.",
		"이후 §b지나친§f 대상들을 전부 §b지나친 수§f × 1.5초간 §e§n기절§f시키고,",
		"영구적으로 §b지나친 수§f당 §3무적§f의 지속 시간이 0.2초 증가합니다."
})
@SuppressWarnings("deprecation")
public class JudasMurderer extends AbstractMurderer {
	
	public JudasMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			final MurderMystery murderMystery = (MurderMystery) getGame();
			NMS.sendTitle(getPlayer(), "§e직업§f: §8배신자 유다", "§f죄악감을 이겨내고 모든 §a시민§f을 죽이세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			PASSIVE.start();
			passive.start();
			murderMystery.addGold(getParticipant(), 30);
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}

	private final AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", 1.4, Operation.ADD_SCALAR);
	
	private boolean particlerun = false;
	private PotionEffect invisible = new PotionEffect(PotionEffectType.INVISIBILITY, 999, 0, true, false);
	private ItemStack[] armors;
	private ItemStack mainhandItem, offhandItem;
	private int timer, invstack = 0;
	private String nowpunishment = "";
	private ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	private final Cooldown cooldown = new Cooldown(8);
	
	@SuppressWarnings("serial")
	private static final List<String> punishment = new ArrayList<String>() {
		{
			add("§c공격 불가");
			add("§c공격 불가");
			add("§c공격 불가");
			add("§c10칸 내 화살 자신에게 유도");
			add("§c10칸 내 화살 자신에게 유도");
			add("§c구속 1");
			add("§c구속 1");
			add("§c구속 2");
			add("§c구속 2");
			add("§c구속 3");
			add("§c실명");
			add("§c실명");
			add("§c실명");
			add("§c랜덤 화면 전환");
			add("§c슬롯바 무작위 교체");
			add("§c환청");
			add("§c환청");
			add("§c환청");
			add("§c멀미");
			add("§c멀미");
		}
	};
	
	private Set<Participant> participants = new HashSet<>();
	
	private List<Location> log = new ArrayList<>();

	private BossBar bossBar = null;
	private static final Circle circle = Circle.of(0.25, 50);
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId())
					&& getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue())
					&& getGame().getParticipant(entity.getUniqueId()).hasAbility()
					&& !(getGame().getParticipant(entity.getUniqueId()).getAbility() instanceof AbstractMurderer);
		}
	};
	
	private final Predicate<Entity> arrowpredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (!(entity instanceof Arrow)) return false;
			if (((Arrow) entity).getShooter().equals(getPlayer())) return false;
			return true;
		}
	};
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			nowpunishment = punishment.get(random.nextInt(punishment.size()));
			debuff.start();
		}
		
	}.setPeriod(TimeUnit.SECONDS, 30).register();
	
	public AbilityTimer debuff = new AbilityTimer(200) {
		
		@Override
		public void onStart() {
			if (nowpunishment.contains("§c구속 1")) {
				PotionEffects.SLOW.addPotionEffect(getPlayer(), 200, 0, false, false, true);
			}
			if (nowpunishment.contains("§c구속 2")) {
				PotionEffects.SLOW.addPotionEffect(getPlayer(), 200, 1, false, false, true);
			}
			if (nowpunishment.contains("§c구속 3")) {
				PotionEffects.SLOW.addPotionEffect(getPlayer(), 200, 2, false, false, true);
			}
			if (nowpunishment.contains("§c실명")) {
				PotionEffects.BLINDNESS.addPotionEffect(getPlayer(), 200, 1, false, false, true);
			}
			if (nowpunishment.contains("§c멀미")) {
				PotionEffects.CONFUSION.addPotionEffect(getPlayer(), 200, 1, false, false, true);
			}
		}
		
		@Override
		public void run(int count) {
			ac.update(nowpunishment + "§7: §9" + df.format(count / 20.0) + "§f초");
			
			if (nowpunishment.equals("§c10칸 내 화살 자신에게 유도")) {
				for (Arrow arrow : LocationUtil.getNearbyEntities(Arrow.class, getPlayer().getLocation(), 10, 10, arrowpredicate)) {
					arrow.setGlowing(true);
					arrow.setGravity(false);
					arrow.setVelocity(VectorUtil.validateVector((getPlayer().getEyeLocation().toVector().subtract(arrow.getLocation().toVector()))));	
				}
			}
			
			if (nowpunishment.equals("§c랜덤 화면 전환")) {
				if (count % (random.nextInt(35) + 1) == 0) {
					if (Math.random() <= 0.65) {
						final Location location = getPlayer().getLocation();
						float yaw = location.getYaw() + random.nextInt(130) - 65;
						if (yaw > 180 || yaw < -180) {
							float mod = yaw % 180;
							if (mod < 0) {
								yaw = 180 + mod;
							} else if (mod > 0) {
								yaw = -180 + mod;
							}
						}
						NMS.rotateHead(getPlayer(), getPlayer(), yaw, location.getPitch() + random.nextInt(90) - 45);
					}	
				}
			}
			
			if (nowpunishment.equals("§c슬롯바 무작위 교체")) {
				if (count % (random.nextInt(20) + 1) == 0) {
					getPlayer().getInventory().setHeldItemSlot(random.nextInt(9));
					SoundLib.UI_BUTTON_CLICK.playSound(getPlayer());
				}
			}
			
			if (nowpunishment.equals("§c환청")) {
				if (count % (random.nextInt(10) + 1) == 0) SoundLib.ENTITY_ARROW_SHOOT.playSound(getPlayer(), getPlayer().getLocation().clone().add(random.nextInt(7), random.nextInt(5), random.nextInt(7)), 1, 1);
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onMurder(MurderEvent e) {
		if (e.getMurderer().getParticipant().equals(getParticipant()) && debuff.isRunning() && nowpunishment.equals("§c공격 불가")) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	private void onArrowKill(ArrowKillEvent e) {
		if (e.getShooter().equals(getParticipant()) && debuff.isRunning() && nowpunishment.equals("§c공격 불가")) {
			e.setCancelled(true);
		}
		if (e.getTarget().equals(getParticipant()) && inv.isRunning()) e.setCancelled(true);
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
			}
		}
		if (Items.isMurdererSword(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (!cooldown.isCooldown()) {
					MurderMystery murderMystery = (MurderMystery) getGame();
					if (particlerun) {
			    		getPlayer().sendMessage("§4[§c!§4] §c아직 능력의 효과가 지속 중입니다.");
			    	} else {
			    		if (murderMystery.consumeGold(getParticipant(), 6)) {
				    		armors = getPlayer().getInventory().getArmorContents();
					    	mainhandItem = getPlayer().getInventory().getItemInMainHand();
					    	offhandItem = getPlayer().getInventory().getItemInOffHand();
					    	shadowstep.start();
					    	getPlayer().sendMessage("§8[§7!§8] §5흑마술§c을 사용합니다.");
			    		}
			    	}	
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer()) && shadowstep.isRunning()) {
			for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 2, 2, predicate)) {
				if (!participants.contains(getGame().getParticipant(player))) {
					participants.add(getGame().getParticipant(player));
					SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), (float) 0.5, 2);
					new BukkitRunnable() {
						@Override
						public void run() {
							for (Location loc : circle.toLocations(player.getLocation()).floor(player.getLocation().getY())) {
								ParticleLib.SMOKE_LARGE.spawnParticle(loc, 0.15, 0.1, 0.15, 1, 0);
							}
						}	
					}.runTaskLater(AbilityWar.getPlugin(), (long) (20 - (timer * 0.5)));
				}
			}
		}
		if (participants.contains(getGame().getParticipant(e.getPlayer()))) {
			e.setTo(e.getFrom());
		}
	}
	
	private final AbilityTimer inv = new AbilityTimer(TaskType.REVERSE, 10) {
		
		@Override
		protected void run(int count) {
			ac2.update("§3무적§f: §c" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			ac2.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer shadowstep = new AbilityTimer(TaskType.REVERSE, 50) {
		
		@Override
		protected void onStart() {
    		inv.start();
    		inv.setCount(6 + (invstack * 4));
			try {
				getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
			} catch (IllegalArgumentException ignored) {
			}
			if (ServerVersion.getVersion() >= 13) {
				BlockData gravel = MaterialX.GRAVEL.getMaterial().createBlockData();
				BlockData dragonegg = MaterialX.DRAGON_EGG.getMaterial().createBlockData();
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 0.4, 1, 0.4, 120, 0, gravel);
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 0.4, 1, 0.4, 120, 0, dragonegg);
			} else {
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 0.4, 1, 0.4, 120, 0, new MaterialData(Material.GRAVEL));
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 0.4, 1, 0.4, 120, 0, new MaterialData(Material.DRAGON_EGG));	
			}
			SoundLib.ENTITY_PIG_SADDLE.playSound(getPlayer().getLocation(), 1, 0.85f);
			bossBar = Bukkit.createBossBar("§8흑마술", BarColor.WHITE, BarStyle.SOLID);
			bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (getPlayer().getInventory().getHelmet() == null && getPlayer().getInventory().getChestplate() == null
    				&& getPlayer().getInventory().getLeggings() == null && getPlayer().getInventory().getBoots() == null) {
    			armors = null;
    		}
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
			getPlayer().getInventory().setArmorContents(null);
			getParticipant().attributes().TARGETABLE.setValue(false);
			getPlayer().getInventory().setItemInMainHand(null);
			getPlayer().getInventory().setItemInOffHand(null);
			timer = 0;
		}

		@Override
		protected void run(int count) {
			if (count % 2 == 0) {
				log.add(getPlayer().getLocation());
			}
			timer++;
			bossBar.setProgress(RangesKt.coerceIn((double) count / 50, 0, 1));
		    getPlayer().addPotionEffect(invisible);
			if (count == 20) {
				new LogParticle().start();
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
	    	cooldown.start();
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
			if (armors != null) getPlayer().getInventory().setArmorContents(armors);
			getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
			if (!mainhandItem.getType().equals(Material.AIR)) getPlayer().getInventory().setItemInMainHand(mainhandItem);
			if (!offhandItem.getType().equals(Material.AIR)) getPlayer().getInventory().setItemInOffHand(offhandItem);
			getParticipant().attributes().TARGETABLE.setValue(true);
			bossBar.removeAll();
			SoundLib.ENTITY_BAT_TAKEOFF.playSound(getPlayer().getLocation(), 1, 0.7f);
			ParticleLib.EXPLOSION_NORMAL.spawnParticle(getPlayer().getLocation(), 0.1, 0.1, 0.1, 350, 0.1f);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private class LogParticle extends AbilityTimer {
		
		private final RGB color = RGB.BLACK;
		private List<Location> getlog = new ArrayList<>();
		
		private LogParticle() {
			super(20);
			setPeriod(TimeUnit.TICKS, 1);
		}
		
		@Override
		protected void onStart() {
			particlerun = true;
		}
		
    	@Override
    	protected void run(int count) {
    		getlog.add(log.get(0));
    		log.remove(0);
        	int momentCount = 0;
        	final ListIterator<Location> listIterator = getlog.listIterator(getlog.size() - 1);
        	if (!listIterator.hasPrevious()) return;
    		listIterator.previous();
    		while (listIterator.hasPrevious()) {
    			final Location base;
    			if (momentCount == 1) {
    				base = getPlayer().getLocation();
    				listIterator.previous();
    			} else {
    				base = listIterator.previous();
    			}
    			listIterator.next();
    			final Location previous = listIterator.next();
    			for (Iterator<Location> iterator = new Iterator<Location>() {
    				private final Vector vectorBetween = previous.toVector().subtract(base.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
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
    					return base.clone().add(unit.clone().multiply(cursor));
    				}
    			}; iterator.hasNext(); ) {
    				ParticleLib.REDSTONE.spawnParticle(getPlayer(), iterator.next().add(0, 0.1, 0), color);
    			}
    			listIterator.previous();
    			listIterator.previous();
    		}	
    	}
    	
    	@Override
    	protected void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	protected void onSilentEnd() {
        	int momentCount = 0;
        	final ListIterator<Location> listIterator = getlog.listIterator(getlog.size() - 1);
        	if (!listIterator.hasPrevious()) return;
    		listIterator.previous();
    		while (listIterator.hasPrevious()) {
    			final Location base;
    			if (momentCount == 1) {
    				base = getPlayer().getLocation();
    				listIterator.previous();
    			} else {
    				base = listIterator.previous();
    			}
    			listIterator.next();
    			final Location previous = listIterator.next();
    			for (Iterator<Location> iterator = new Iterator<Location>() {
    				private final Vector vectorBetween = previous.toVector().subtract(base.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
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
    					return base.clone().add(unit.clone().multiply(cursor));
    				}
    			}; iterator.hasNext(); ) {
    				ParticleLib.REDSTONE.spawnParticle(getPlayer(), iterator.next().add(0, 0.1, 0), RGB.RED);
    			}
    			listIterator.previous();
    			listIterator.previous();
    		}
    		SoundLib.BLOCK_ANVIL_LAND.playSound(getPlayer().getLocation(), 1, 1.77f);
    		for (Participant participant : participants) {
    			ParticleLib.ITEM_CRACK.spawnParticle(participant.getPlayer().getLocation(), .5f, 1f, .5f, 100, 0.35, MaterialX.REDSTONE);
    			Stun.apply(participant, TimeUnit.TICKS, 30 * participants.size());
    		}
    		invstack += participants.size();
			participants.clear();
    		particlerun = false;
    		log.clear();
    		getlog.clear();
    		inv.start();
    		inv.setCount(6 + (invstack * 4));
    	}

	}

}
