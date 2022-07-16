package RainStarAbility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

import RainStarAbility.RainStar.Grab;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.block.Blocks;
import daybreak.abilitywar.utils.base.minecraft.block.IBlockSnapshot;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.BlockX;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "귤즙", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7게이지 §8- §e과즙§f: $[PERIOD]초마다 한 칸씩 최대 10칸을 보유 가능합니다.", 
		"§7화살 발사 §8- §6귤 화살§f: 적중 위치에 과즙 장판이 터집니다. $[ARROW_CONSUME]",
		" 만일 생명체를 적중시킨다면 $[GAUGE_RETURN]칸만큼 §e과즙 게이지§f를 돌려받습니다.",
		" 장판 위의 적은 §3끌리거나 밀리는§8(§7벡터§8)§f 효과가 대폭 감소합니다.",
		" 장판 위 적에게 주는 §b원거리 피해§f가 $[PROJECTILE_DAMAGE_INCREASE]% 증가합니다.",
		"§7철괴 우클릭 §8- §c과즙 폭발§f: 모든 과즙 게이지를 전부 소모하여 제자리에 터뜨려",
		" $[RANGE]칸 내의 적을 강하게 밀쳐내고 §7실명§f시킵니다. 이때 잠시 빨라집니다.",
		" 모든 효과§8(§7넉백, 실명, 신속§8)§f의 세기는 게이지에 비례합니다.",
		"§b[§7아이디어 제공자§b] §6Tangerine_Ring"
		})

public class TangerineJuice extends AbilityBase {
	
	public TangerineJuice(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> PERIOD = 
			abilitySettings.new SettingObject<Double>(TangerineJuice.class, "period", 5.0,
			"# 과즙이 차오르는 주기", "# WRECK 효과 50%까지 적용") {

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
	
	public static final SettingObject<Integer> PROJECTILE_DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Integer>(TangerineJuice.class, "projectile-damage-increase", 25,
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
	
	private final Map<Block, IBlockSnapshot> blockData = new HashMap<>();
	private final Set<Block> notchangedblocks = new HashSet<>();
	private final int returngauge = GAUGE_RETURN.getValue();
	private final int consume = ARROW_CONSUME.getValue();
	private final int fieldrange = FIELD_RANGE.getValue();
	private ActionbarChannel ac = newActionbarChannel();
	private int juicegauge = 0;
	
	private final int period = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(60) * (PERIOD.getValue() * 20) : (PERIOD.getValue() * 20));
	
	private Map<Projectile, ArrowParticle> arrowParticles = new HashMap<>();
	
	private AbilityTimer juice = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (period == 0) juicegauge = 10;
			else if (count % period == 0) juicegauge = Math.min(10, juicegauge + 1);
			ac.update(Strings.repeat("§7/", 10 - juicegauge) + Strings.repeat("§6/", juicegauge));
			
			
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
    @SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
    	if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
    		if (juicegauge >= consume) {
    			juicegauge -= consume;
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
    		
			for (int ranges = 0; ranges < fieldrange; ranges++) {
				for (Block block : LocationUtil.getBlocks2D(e.getEntity().getLocation(), ranges, true, true, true)) {
					Block belowBlock = block.getRelative(BlockFace.DOWN);
					if (MaterialX.ORANGE_CONCRETE.compare(belowBlock)) {
						block = belowBlock;
						belowBlock = belowBlock.getRelative(BlockFace.DOWN);
						notchangedblocks.add(belowBlock);
					} else blockData.putIfAbsent(belowBlock, Blocks.createSnapshot(belowBlock));
					BlockX.setType(belowBlock, MaterialX.ORANGE_CONCRETE);
				}
			}
    	}
    }
    
    @SubscribeEvent
    public void onEntityDamageByEvent(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && !getPlayer().equals(e.getEntity())) {
			Block b = LocationUtil.floorY(e.getEntity().getLocation()).getBlock();
			Block bb = LocationUtil.floorY(e.getEntity().getLocation()).clone().subtract(0, 1, 0).getBlock();
			if (blockData.containsKey(b) || blockData.containsKey(bb) || notchangedblocks.contains(b) || notchangedblocks.contains(bb)) {
				final double distance = Math.min(10, getPlayer().getLocation().distance(e.getEntity().getLocation()));
				if (distance >= 5) {
					
				}
			}
		}
    }
    
	@SubscribeEvent
	public void onBlockBreak(BlockBreakEvent e) {
		if (blockData.containsKey(e.getBlock()) || notchangedblocks.contains(e.getBlock())) {
			e.setCancelled(true);
		}
	}

	@SubscribeEvent
	public void onExplode(BlockExplodeEvent e) {
		e.blockList().removeIf(blockData::containsKey);
		e.blockList().removeIf(notchangedblocks::contains);
	}

	@SubscribeEvent
	public void onExplode(EntityExplodeEvent e) {
		e.blockList().removeIf(blockData::containsKey);
		e.blockList().removeIf(notchangedblocks::contains);
	}
    
    public class ArrowParticle extends AbilityTimer {
    	
		private Location lastloc;
		private Vector forward;
		private final Random random = new Random();
		private RGB orangecolor;
		private final double length;
		private final Projectile projectile;
		
		private final RGB orange1 = RGB.of(241, 129, 4), orange2 = RGB.of(230, 46, 1), orange3 = RGB.of(250, 72, 5),
				orange4 = RGB.of(255, 128, 1), orange5 = RGB.of(251, 173, 68), orange6 = RGB.of(252, 95, 10),
				orange7 = RGB.of(240, 72, 14), orange8 = RGB.of(254, 108, 20), orange9 = RGB.of(230, 74, 15);
		
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
    	
    	public ArrowParticle(Projectile projectile, double length) {
			super();
    		setPeriod(TimeUnit.TICKS, 1);
    		this.projectile = projectile;
    		this.length = length;
			this.forward = projectile.getLocation().clone().subtract(lastloc.clone()).toVector().normalize().multiply(length);
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
				orangecolor = orangecolors.get(random.nextInt(10));
				ParticleLib.REDSTONE.spawnParticle(location, orangecolor);
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
