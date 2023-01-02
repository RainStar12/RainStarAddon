package rainstar.abilitywar.synergy;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.base.random.RouletteWheel;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.Corrosion;

@AbilityManifest(
		name = "데미소다", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7패시브 §8- §b스파클링§f: 불 속성의 피해, 모든 상태이상에 면역 효과를",
		" 가집니다. 또한 신발을 신고 물 속에 들어갈 때 물갈퀴 인챈트를 자동 획득합니다.",
		"§7패시브 §8- §a무슨 맛으로?§f: 피해를 입을 때마다 $[CHANCE_CONFIG]% 확률로 현재 내 탄산음료의 맛에",
		" 따른 제각기의 버프를 $[BUFF_DURATION]초간 획득합니다.",
		" 이 효과가 발동하면, 무작위의 맛으로 교체됩니다.",
		" §c자몽 맛§7: §c재생 §7| §6오렌지 맛§7: §6힘 §7| §e레몬 맛§7: §e흡수",
		" §a사과 맛§7: §a저항 §7| §b청포도 맛§7: §b신속 §7| §d복숭아 맛§7: §d치유",
		"§7철괴 좌클릭 §8- §b리프레쉬§f: $[DURATION_CONFIG]초간 탄산음료가 되어, 타게팅 불능 및 무적 상태가 됩니다.",
		" 탄산음료가 된 동안 지면에 맞닿아서만 이동할 수 있으며, 지속 시간이 끝날 때 ",
		" 탄산음료 상태가 해제되고 주변 $[RANGE_CONFIG]칸 내 적에게 $[EFFECT_DURATION]초간",
		" 부식 상태이상을 걸고 $[NO_CHANGE_DURATION]초간 현재의 맛을 고정시킵니다. $[COOLDOWN_CONFIG]",
		"§7상태이상 §8- §7부식§f: 철 광물을 사용하는 모든 아이템을 사용할 수 없습니다.",
		" 또한 갑옷의 방어력이 착용 광물에 비례해 희귀성이 낮을수록 더 많이 감소합니다."
		})

public class Demisoda extends Synergy implements ActiveHandler {
	
	public Demisoda(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> DURATION_CONFIG 
	= synergySettings.new SettingObject<Integer>(Demisoda.class,
			"duration", 7, "# 물 상태 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> EFFECT_DURATION 
	= synergySettings.new SettingObject<Integer>(Demisoda.class,
			"effect-duration", 7, "# 부식 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> BUFF_DURATION 
	= synergySettings.new SettingObject<Integer>(Demisoda.class,
			"buff-duration", 5, "# 효과 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> CHANCE_CONFIG 
	= synergySettings.new SettingObject<Integer>(Demisoda.class,
			"chance", 22, "# 피격시 버프를 받을 확률",
			"# 30 = 30%") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1 && value <= 100;
		}

	};
	
	public static final SettingObject<Integer> RANGE_CONFIG 
	= synergySettings.new SettingObject<Integer>(Demisoda.class,
			"range", 5, "# 부식 범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> NO_CHANGE_DURATION 
	= synergySettings.new SettingObject<Integer>(Demisoda.class,
			"no-change-duration", 10, "# 맛 고정 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> COOLDOWN_CONFIG 
	= synergySettings.new SettingObject<Integer>(Demisoda.class,
			"cooldown", 30, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof ArmorStand) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private final Predicate<Block> blockpredicate = new Predicate<Block>() {
		@Override
		public boolean test(Block block) {
			if (!block.getType().isSolid() && !block.isLiquid()) {
				return true;
			}
			return false;
		}

		@Override
		public boolean apply(@Nullable Block arg0) {
			return false;
		}
	};
	
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	passive.start();
	    }
	}
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN_CONFIG.getValue(), CooldownDecrease._25);
	private int taste = new Random().nextInt(6);
	
	private static final RGB GRAPEFRUIT = RGB.of(255, 92, 92), ORANGE = RGB.of(254, 94, 1), LEMON = RGB.of(254, 215, 54), 
			APPLE = RGB.of(183, 240, 177), GREENGRAPE = RGB.of(1, 216, 254), PEACH = RGB.of(254, 178, 217);

	private final ActionbarChannel ac = newActionbarChannel();
	
	private static final int particleCount = 20;
	private static final double yDiff = 0.6 / particleCount;
	private static final Circle circle = Circle.of(0.5, particleCount);
	private final RouletteWheel rouletteWheel = new RouletteWheel();
	private final RouletteWheel.Slice positive = rouletteWheel.newSlice(CHANCE_CONFIG.getValue()), negative = rouletteWheel.newSlice(100 - positive.getWeight());
	
	private static boolean isWater(final Block block) {
		return block.getType().name().endsWith("WATER");
	}
	
	private final AbilityTimer nochange = new AbilityTimer(NO_CHANGE_DURATION.getValue()) {
		
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
	private final AbilityTimer passive = new AbilityTimer() {
		
    	@Override
		public void run(int count) {
    		if (getPlayer().getFireTicks() > 0) {
    			getPlayer().setFireTicks(0);
    		}
    		if (isWater(getPlayer().getLocation().getBlock())) {
    			if (getPlayer().getInventory().getBoots() != null) {
    				ItemStack enchantedboots = getPlayer().getInventory().getBoots();
    				if (enchantedboots.containsEnchantment(Enchantment.DEPTH_STRIDER) && enchantedboots.getEnchantmentLevel(Enchantment.DEPTH_STRIDER) < 3) {
    					enchantedboots.addEnchantment(Enchantment.DEPTH_STRIDER, 3);
    					getPlayer().getInventory().setBoots(enchantedboots);	
    				}
    				if (!enchantedboots.containsEnchantment(Enchantment.DEPTH_STRIDER)) {
    					enchantedboots.addEnchantment(Enchantment.DEPTH_STRIDER, 3);
    					getPlayer().getInventory().setBoots(enchantedboots);
    				}	
    			}
				getPlayer().setRemainingAir(getPlayer().getMaximumAir());
    		} else {
    			if (getPlayer().getInventory().getBoots() != null) {
    				ItemStack enchantedboots = getPlayer().getInventory().getBoots();
    				if (enchantedboots.containsEnchantment(Enchantment.DEPTH_STRIDER)) {
    					enchantedboots.removeEnchantment(Enchantment.DEPTH_STRIDER);
    					getPlayer().getInventory().setBoots(enchantedboots);
    				}
    			}
    		}
    		switch(taste) {
    		case 0:
    			ac.update((nochange.isRunning() ? "§c§l" : "§c") + "자몽 맛");
    			break;
    		case 1:
    			ac.update((nochange.isRunning() ? "§6§l" : "§6") + "오렌지 맛");
    			break;
    		case 2:
    			ac.update((nochange.isRunning() ? "§e§l" : "§e") + "레몬 맛");
    			break;
    		case 3:
    			ac.update((nochange.isRunning() ? "§a§l" : "§a") + "사과 맛");
    			break;
    		case 4:
    			ac.update((nochange.isRunning() ? "§b§l" : "§b") + "청포도 맛");
    			break;
    		case 5:
    			ac.update((nochange.isRunning() ? "§d§l" : "§d") + "복숭아 맛");
    			break;
    		}
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(onlyRelevant = true)
	public void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		e.setCancelled(true);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getCause() == DamageCause.FIRE || e.getCause() == DamageCause.FIRE_TICK || e.getCause() == DamageCause.LAVA) {
			e.setCancelled(true);		
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		final RouletteWheel.Slice select = rouletteWheel.select();
		if (select == positive) {
			switch(taste) {
				case 0:
					showHelix(GRAPEFRUIT);
					PotionEffects.REGENERATION.addPotionEffect(getPlayer(), BUFF_DURATION.getValue() * 20, 1, true);
					break;
				case 1:
					showHelix(ORANGE);
					PotionEffects.INCREASE_DAMAGE.addPotionEffect(getPlayer(), BUFF_DURATION.getValue() * 20, 1, true);
					break;
				case 2:
					showHelix(LEMON);
					PotionEffects.ABSORPTION.addPotionEffect(getPlayer(), BUFF_DURATION.getValue() * 20, 1, true);
					break;
				case 3:
					showHelix(APPLE);
					PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), BUFF_DURATION.getValue() * 20, 1, true);
					break;
				case 4:
					showHelix(GREENGRAPE);
					PotionEffects.SPEED.addPotionEffect(getPlayer(), BUFF_DURATION.getValue() * 20, 3, true);
					break;
				case 5:
					showHelix(PEACH);
					PotionEffects.HEAL.addPotionEffect(getPlayer(), 1, 0, true);
					break;
			}
			negative.increaseWeight(5);
			positive.resetWeight();
			if (!nochange.isRunning()) taste = new Random().nextInt(6);
		} else {
			positive.increaseWeight(5);
			negative.resetWeight();
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onGameModeChange(PlayerGameModeChangeEvent e) {
		if (skill.isRunning() && getPlayer().getGameMode() == GameMode.SPECTATOR) e.setCancelled(true);
	}

	@SubscribeEvent(onlyRelevant = true)
	public void onPlayerTeleport(PlayerTeleportEvent e) {
		if (skill.isRunning() && getPlayer().getGameMode() == GameMode.SPECTATOR) {
			if (e.getCause() == TeleportCause.SPECTATE) e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onPlayerMove(PlayerMoveEvent e) {
		if (skill.isRunning()) {
			Location checkLoc = e.getTo().clone().add(0, 0.8, 0);
			Material m = checkLoc.clone().add(0, 0.2, 0).getBlock().getType();
			Material lm = checkLoc.clone().add(0.5, 0, 0).getBlock().getType();
			Material rm = checkLoc.clone().add(0, 0, 0.5).getBlock().getType();
			Material lmm = checkLoc.clone().subtract(0.5, 0, 0).getBlock().getType();
			Material rmm = checkLoc.clone().subtract(0, 0, 0.5).getBlock().getType();
			Block b = checkLoc.clone().add(0, 0.2, 0).getBlock();
			Block lb = checkLoc.clone().add(0.5, 0, 0).getBlock();
			Block rb = checkLoc.clone().add(0, 0, 0.5).getBlock();
			Block lmb = checkLoc.clone().subtract(0.5, 0, 0).getBlock();
			Block rmb = checkLoc.clone().subtract(0, 0, 0.5).getBlock();
			if (Math.abs(LocationUtil.floorY(e.getTo(), blockpredicate).clone().subtract(0, 1, 0).getY() - e.getFrom().getY()) <= 1) {
				e.setTo(LocationUtil.floorY(e.getTo(), blockpredicate).subtract(0, 1, 0));
			} else {
				if (m.isSolid() && rm.isSolid() && lm.isSolid() && lmm.isSolid() && rmm.isSolid()) {
					e.setTo(e.getFrom());
				}
				if (!m.isSolid() && !rm.isSolid() && !lm.isSolid() && !lmm.isSolid() && !rmm.isSolid() &&
						!b.isLiquid() && !lb.isLiquid() && !rb.isLiquid() && !lmb.isLiquid() && !rmb.isLiquid()) {
					e.setTo(e.getFrom());
				}	
			}
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK) && !cooldown.isCooldown()) {
	    	if (!skill.isDuration()) {
	    		getPlayer().teleport(LocationUtil.floorY(getPlayer().getLocation(), blockpredicate).subtract(0, 1, 0));
	    		skill.start();
	    		nochange.start();
	    		return true;
	    	} else {
	    		skill.stop(false);
	    	}
	    }
		return false;
	}

	private final Duration skill = new Duration(DURATION_CONFIG.getValue() * 20, cooldown) {
		
		private GameMode originalMode;
		private float flySpeed;

		@Override
		protected void onDurationStart() {
			this.originalMode = getPlayer().getGameMode();
			if (originalMode == GameMode.SPECTATOR) originalMode = GameMode.SURVIVAL;
			this.flySpeed = getPlayer().getFlySpeed();
			getParticipant().attributes().TARGETABLE.setValue(false);
			getPlayer().setGameMode(GameMode.SPECTATOR);
			ParticleLib.DRIP_WATER.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.5, 0.5, 0.5, 200, 1);
			SoundLib.ENTITY_PLAYER_SPLASH.playSound(getPlayer().getLocation(), 1, 1.5f);
		}
		
		@Override
		protected void onDurationProcess(int count) {
			if (getPlayer().getSpectatorTarget() != null) getPlayer().setSpectatorTarget(null);
			getPlayer().setFlySpeed(0.15f);
			ParticleLib.WATER_SPLASH.spawnParticle(getPlayer().getEyeLocation().clone().subtract(0, 0.4, 0), 0, 0, 0, 10, 0);
			if (count % 10 == 0) {
				SoundLib.BLOCK_WATER_AMBIENT.playSound(getPlayer().getLocation(), 1, 1.2f);
			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}

		@Override
		protected void onDurationSilentEnd() {
			SoundLib.ENTITY_PLAYER_SWIM.playSound(getPlayer().getLocation(), 1, 1f);
			SoundLib.ENTITY_PLAYER_SWIM.playSound(getPlayer().getLocation(), 1, 1.2f);
			SoundLib.ENTITY_PLAYER_SWIM.playSound(getPlayer().getLocation(), 1, 1.4f);
			getPlayer().teleport(LocationUtil.floorY(getPlayer().getLocation()));
			ParticleLib.WATER_SPLASH.spawnParticle(getPlayer().getLocation().clone().add(0, 0, 0), 1, 0, 1, 500, 1);
			getPlayer().setGameMode(originalMode);
			getPlayer().setFlySpeed(flySpeed);
			getPlayer().setFlying(false);
			getParticipant().attributes().TARGETABLE.setValue(true);
			NMS.setInvisible(getPlayer(), false);
			for (Player p : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), RANGE_CONFIG.getValue(), RANGE_CONFIG.getValue(), predicate)) {
				Corrosion.apply(getGame().getParticipant(p), TimeUnit.SECONDS, EFFECT_DURATION.getValue());
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);

	private void showHelix(final RGB color) {
		new AbilityTimer((particleCount * 3) / 2) {
			int count = 0;

			@Override
			protected void run(int a) {
				for (int i = 0; i < 2; i++) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer().getLocation().clone().add(circle.get(count % 20)).add(0, count * yDiff, 0), color);
					count++;
				}
			}
		}.setPeriod(TimeUnit.TICKS, 1).start();
	}
	
}