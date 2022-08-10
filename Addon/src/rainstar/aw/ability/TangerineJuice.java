package rainstar.aw.ability;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.block.Blocks;
import daybreak.abilitywar.utils.base.minecraft.block.IBlockSnapshot;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.BlockX;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

@AbilityManifest(name = "귤즙", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7게이지 §8- §e과즙§f: $[PERIOD]초마다 한 칸씩 최대 10칸을 보유 가능합니다.",
		" 게이지 소모 이후엔 다음 게이지 차징까진 $[NEED_WAIT]초를 기다려야 합니다.",
		"§7화살 발사 §8- §6귤 화살§f: 적중 위치에 §a과즙 장판§f이 터집니다. $[ARROW_CONSUME]",
		" 만일 생명체를 적중시킨다면 $[GAUGE_RETURN]칸만큼 §e과즙 게이지§f를 돌려받습니다.",
		" §a장판§f 위의 적은 중심으로 끌려가며 이외 §3끌리거나 밀리는§8(§7벡터§8)§f 효과가 감소합니다.",
		" §a장판§f은 $[FIELD_RANGE]칸, $[FIELD_DURATION]초 유지되며 장판 위 적에게 주는 §b원거리 피해§f가 $[LONG_DISTANCE_DAMAGE_INCREASE]% 증가합니다.",
		"§7철괴 우클릭 §8- §c과즙 폭발§f: 과즙 게이지를 전부 소모하여 잠시간 §b신속 버프§f를 얻고,",
		" $[RANGE]칸 내의 생명체들을 강하게 밀쳐내고 §7실명§f시킵니다.",
		" 모든 효과§8(§7넉백, 실명, 신속§8)§f의 세기는 게이지에 비례합니다.",
		"§b[§7아이디어 제공자§b] §6Tangerine_Ring"
		},
		summarize = {
		"화살 발사 시 §e과즙 게이지§f를 소모하여 적중 위치에 §6장판§f을 터뜨립니다.",
		"생명체 적중 시 §e과즙 게이지§f를 일부 돌려받습니다. §6장판§f 위의 적은 중심으로 끌려가고",
		"이외의 §3벡터 효과§f를 무시합니다. 또한 §6장판§f 위 적에게 주는 §b원거리 피해§f가 증가합니다.",
		"§7철괴 우클릭으로§f 과즙 게이지를 전부 소모해 잠시간 신속 버프를 얻고",
		"주변 생명체를 강하게 밀쳐내고 §7실명§f시킵니다."
		})

public class TangerineJuice extends AbilityBase implements ActiveHandler {
	
	public TangerineJuice(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> PERIOD = 
			abilitySettings.new SettingObject<Double>(TangerineJuice.class, "period", 7.0,
			"# 과즙이 차오르는 주기", "# WRECK 효과 50%까지 적용") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> NEED_WAIT = 
			abilitySettings.new SettingObject<Double>(TangerineJuice.class, "need-wait", 15.0,
			"# 과즙 사용 후 대기시간", "# WRECK 효과 50%까지 적용") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> ARROW_CONSUME = 
			abilitySettings.new SettingObject<Integer>(TangerineJuice.class, "juice-consume:arrow", 3,
			"# 화살에 소모할 과즙 게이지") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
		@Override
		public String toString() {
			return "§c과즙 소모 §7: §b" + getValue();
        }

	};
	
	public static final SettingObject<Integer> GAUGE_RETURN = 
			abilitySettings.new SettingObject<Integer>(TangerineJuice.class, "gauge-return", 1,
			"# 생명체 적중 시 돌려받는 과즙량") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> LONG_DISTANCE_DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Integer>(TangerineJuice.class, "long-distance-damage-increase", 25,
			"# 장판 위 적에게 주는 추가 대미지") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(TangerineJuice.class, "range", 3.0,
			"# 과즙 폭발 사거리") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> FIELD_RANGE = 
			abilitySettings.new SettingObject<Integer>(TangerineJuice.class, "field-range", 5,
			"# 과즙 장판 범위") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> FIELD_DURATION = 
			abilitySettings.new SettingObject<Integer>(TangerineJuice.class, "field-duration", 10,
			"# 과즙 장판 지속시간", "# 단위: 초") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
								.getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()),
							participant = getParticipant();
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
	
	private final int period = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(50) * (PERIOD.getValue() * 20) : (PERIOD.getValue() * 20));
	private final int wait = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(50) * (NEED_WAIT.getValue() * 20) : (NEED_WAIT.getValue() * 20));
	private final int consume = ARROW_CONSUME.getValue();
	private final int returngauge = GAUGE_RETURN.getValue();
	private final double dmgIncrease = 1 + (LONG_DISTANCE_DAMAGE_INCREASE.getValue() * 0.01);
	private final double range = RANGE.getValue();
	private final int fieldrange = FIELD_RANGE.getValue();
	private final int fieldduration = FIELD_DURATION.getValue() * 5;
	private final Set<Vector> vectors = new HashSet<>();
	private final Random random = new Random();
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	private final RGB orange1 = RGB.of(241, 129, 4), orange2 = RGB.of(230, 46, 1), orange3 = RGB.of(250, 72, 5),
			orange4 = RGB.of(255, 128, 1), orange5 = RGB.of(251, 173, 68), orange6 = RGB.of(252, 95, 10),
			orange7 = RGB.of(240, 72, 14), orange8 = RGB.of(254, 108, 20), orange9 = RGB.of(230, 74, 15);
	
	@SuppressWarnings("serial")
	private List<RGB> orangecolors = new ArrayList<RGB>() {
		{
			add(orange1);
			add(orange2);
			add(orange3);
			add(orange4);
			add(orange5);
			add(orange6);
			add(orange7);
			add(orange8);
			add(orange9);
			add(RGB.ORANGE);
		}
	};
	
	private ActionbarChannel ac = newActionbarChannel();
	private int juicegauge = 0;
	
	private Map<Projectile, ArrowParticle> arrowParticles = new HashMap<>();
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) juice.start();
	}
	
	private AbilityTimer needwait = new AbilityTimer(wait) {
		
		@Override
		public void run(int count) {
			ac.update(Strings.repeat("§3/", 10 - juicegauge) + Strings.repeat("§6/", juicegauge));
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer juice = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (!needwait.isRunning() && juicegauge < 10) {
				if (period == 0) {
					SoundLib.ITEM_BUCKET_FILL.playSound(getPlayer().getLocation(), 1, 2);
					ParticleLib.DRIP_LAVA.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 7, 1);
					juicegauge = 10;
				} else if (count % period == 0) {
					SoundLib.ITEM_BUCKET_FILL.playSound(getPlayer().getLocation(), 1, 2);
					ParticleLib.DRIP_LAVA.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 7, 1);
					juicegauge = Math.min(10, juicegauge + 1);
				}
				ac.update(Strings.repeat("§7/", 10 - juicegauge) + Strings.repeat("§6/", juicegauge));	
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
    @SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
    	if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
    		if (juicegauge >= consume) {
    			juicegauge -= consume;
    			if (needwait.isRunning()) needwait.setCount(wait);
    			else needwait.start();
    			new ArrowParticle(e.getEntity(), e.getEntity().getVelocity().length()).start();
    		}
    	}
    }
    
    @SubscribeEvent
    public void onProjectileHit(ProjectileHitEvent e) {
    	if (arrowParticles.containsKey(e.getEntity())) {
    		arrowParticles.get(e.getEntity()).stop(false);
    		if (e.getHitEntity() != null) juicegauge = Math.min(10, juicegauge + returngauge);
    		ParticleLib.EXPLOSION_LARGE.spawnParticle(e.getEntity().getLocation());
    		e.getEntity().getWorld().createExplosion(e.getEntity().getLocation(), 1.2f, false, false);
    		new Field(fieldduration, e.getEntity().getLocation()).start();
    	}
    }
    
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().hasMetadata("Firework")) {
			e.setCancelled(true);
		}
	}
    
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (juicegauge > 1) {
				for (LivingEntity entity : LocationUtil.getNearbyEntities(LivingEntity.class, getPlayer().getLocation(), range, range, predicate)) {
					entity.setVelocity(entity.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().multiply(1 + (juicegauge * 0.2)).setY(0));
					PotionEffects.BLINDNESS.addPotionEffect(entity, (juicegauge * 20), 0, true);
				}
				final Firework firework = getPlayer().getWorld().spawn(getPlayer().getLocation(), Firework.class);
				final FireworkMeta meta = firework.getFireworkMeta();
				meta.addEffect(
						FireworkEffect.builder()
						.withColor(random.pick(orangecolors).getColor(), random.pick(orangecolors).getColor(), random.pick(orangecolors).getColor())
						.with(Type.BALL_LARGE)
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
				ParticleLib.EXPLOSION_HUGE.spawnParticle(getPlayer().getLocation());
				SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 1, 1.5f);
				PotionEffects.SPEED.addPotionEffect(getPlayer(), (juicegauge * 10), (int) (juicegauge * 0.2), isDestroyed());
    			if (needwait.isRunning()) needwait.setCount(wait);
    			else needwait.start();
				juicegauge = 0;
				return true;
			} else getPlayer().sendMessage("§6[§e!§6] §f최소 §c한 칸 이상§f의 §e과즙 게이지§f가 필요합니다.");
		}
		return false;
	}
    
	public class Field extends AbilityTimer implements Listener {
		
		private final Location center;
		private final Map<Block, IBlockSnapshot> blockData = new HashMap<>();
		private final Set<Block> notchangedblocks = new HashSet<>();
		
		public Field(int duration, Location center) {
			super(TaskType.NORMAL, duration);
			setPeriod(TimeUnit.TICKS, 4);
			this.center = center;
		}
		
		@Override
		public void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		public void run(int count) {
			if (count <= fieldrange) {
				for (Block block : LocationUtil.getBlocks2D(center, count, true, true, true)) {
					Block belowBlock = block.getRelative(BlockFace.DOWN);
					if (MaterialX.ORANGE_CONCRETE.compare(belowBlock)) {
						block = belowBlock;
						belowBlock = belowBlock.getRelative(BlockFace.DOWN);
						notchangedblocks.add(belowBlock);
					} else {
						blockData.putIfAbsent(belowBlock, Blocks.createSnapshot(belowBlock));
					}
					BlockX.setType(belowBlock, MaterialX.ORANGE_CONCRETE);
				}
			}
			
			for (Player players : LocationUtil.getEntitiesInCircle(Player.class, center, fieldrange, predicate)) {
				Vector vector = VectorUtil.validateVector(center.toVector().subtract(players.getLocation().toVector()).normalize().setY(0).multiply(0.08));
				vectors.add(vector);
				players.setVelocity(vector);
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			HandlerList.unregisterAll(this);
			for (Entry<Block, IBlockSnapshot> entry : blockData.entrySet()) {
				Block key = entry.getKey();
				if (MaterialX.ORANGE_CONCRETE.compare(key)) {
					entry.getValue().apply();
				}
			}
			blockData.clear();
			notchangedblocks.clear();
		}
		
		@EventHandler()
		public void onBlockBreak(BlockBreakEvent e) {
			if (blockData.containsKey(e.getBlock()) || notchangedblocks.contains(e.getBlock())) {
				e.setCancelled(true);
			}
		}

		@EventHandler()
		public void onExplode(BlockExplodeEvent e) {
			e.blockList().removeIf(blockData::containsKey);
			e.blockList().removeIf(notchangedblocks::contains);
		}

		@EventHandler()
		public void onExplode(EntityExplodeEvent e) {
			e.blockList().removeIf(blockData::containsKey);
			e.blockList().removeIf(notchangedblocks::contains);
		}
		
		@EventHandler()
		private void onVelocity(PlayerVelocityEvent e) {
			if (predicate.test(e.getPlayer())) {
				Block b = LocationUtil.floorY(e.getPlayer().getLocation()).getBlock();
				Block bm = LocationUtil.floorY(e.getPlayer().getLocation()).clone().subtract(0, 1, 0).getBlock();
				if (blockData.containsKey(b) || blockData.containsKey(bm) || notchangedblocks.contains(b) || notchangedblocks.contains(bm)) {
					if (!vectors.contains(e.getVelocity())) e.setVelocity(e.getVelocity().multiply(0.25));
					else vectors.remove(e.getVelocity());
				}	
			}
		}
		
	    @EventHandler()
	    public void onEntityDamageByEvent(EntityDamageByEntityEvent e) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (getPlayer().equals(damager) && !getPlayer().equals(e.getEntity())) {
				Block b = LocationUtil.floorY(e.getEntity().getLocation()).getBlock();
				Block bm = LocationUtil.floorY(e.getEntity().getLocation()).clone().subtract(0, 1, 0).getBlock();
				if (blockData.containsKey(b) || blockData.containsKey(bm) || notchangedblocks.contains(b) || notchangedblocks.contains(bm)) {
					final double distance = Math.min(10, getPlayer().getLocation().distance(e.getEntity().getLocation()));
					if (distance >= 5 || e.getDamager() instanceof Projectile) {
						e.setDamage(e.getDamage() * dmgIncrease);
					}
				}
			}
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
				ParticleLib.REDSTONE.spawnParticle(location, random.pick(orangecolors));
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
