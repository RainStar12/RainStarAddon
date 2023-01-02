package rainstar.abilitywar.ability;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LingeringPotion;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.potion.PotionEffect;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.Corrosion;

@SuppressWarnings("deprecation")
@AbilityManifest(
		name = "소다", rank = Rank.L, species = Species.OTHERS, explain = {
		"§3물 속성§f의 꼬마 정령, 소다.",
		"§7패시브 §8- §3순수§f: 불 속성의 피해, 모든 상태이상, 모든 포션 효과에 면역 효과를",
		" 가집니다. 또한 신발을 신고 물 속에 들어갈 때 물갈퀴 인챈트를 자동 획득합니다.",
		"§7철괴 좌클릭 §8- §3스플래쉬§f: $[DURATION_CONFIG]초간 물이 되어, 타게팅 불능 및 무적 상태가 됩니다.",
		" 물이 된 동안 지면에 맞닿아서만 이동할 수 있으며, 지속 시간이 끝날 때 물 상태가",
		" 해제되고 주변 $[RANGE_CONFIG]칸 내 적에게 $[EFFECT_DURATION]초간 부식 상태이상을 겁니다. $[COOLDOWN_CONFIG]",
		"§7상태이상 §8- §7부식§f: 철 광물을 사용하는 모든 아이템을 사용할 수 없습니다.",
		" 또한 갑옷의 방어력이 착용 광물에 비례해 희귀성이 낮을수록 더 많이 감소합니다."
		},
		summarize = {
		"모든 불 피해, 상태이상, 포션 효과에 면역이 생깁니다.",
		"§7철괴 좌클릭 시§f §b물§f 상태가 되어 지면에 맞닿아 이동할 수 있으며,",
		"§b물§f 상태가 해제될 때 주변 적에게 §7부식§f 상태이상을 겁니다.",
		"§7부식§f된 적은 방어력이 감소하고 철 아이템을 사용할 수 없습니다."
		})

public class Soda extends AbilityBase implements ActiveHandler {
	
	public Soda(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> DURATION_CONFIG 
	= abilitySettings.new SettingObject<Integer>(Soda.class,
			"duration", 7, "# 물 상태 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> EFFECT_DURATION 
	= abilitySettings.new SettingObject<Integer>(Soda.class,
			"effect-duration", 5, "# 부식 지속시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> RANGE_CONFIG 
	= abilitySettings.new SettingObject<Integer>(Soda.class,
			"range", 5, "# 부식 범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> COOLDOWN_CONFIG 
	= abilitySettings.new SettingObject<Integer>(Soda.class,
			"cooldown", 40, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	private final Predicate<Effect> effectpredicate = new Predicate<Effect>() {
		@Override
		public boolean test(Effect effect) {
			return true;
		}

		@Override
		public boolean apply(@Nullable Effect arg0) {
			return false;
		}
	};
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
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
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN_CONFIG.getValue(), CooldownDecrease._50);
	private boolean potiondrank = false;
	
	private static boolean isWater(final Block block) {
		return block.getType().name().endsWith("WATER");
	}
	
	private final AbilityTimer passive = new AbilityTimer() {
		
    	@Override
		public void run(int count) {
    		for (PotionEffect pe : getPlayer().getActivePotionEffects()) {
    			getPlayer().removePotionEffect(pe.getType());
    		}
    		if (getPlayer().getFireTicks() > 0) {
    			getPlayer().setFireTicks(0);
    		}
			getParticipant().removeEffects(effectpredicate);
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
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer potiondrankchecker = new AbilityTimer(1) {
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		potiondrank = false;
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(onlyRelevant = true)
	public void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		e.setCancelled(true);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getCause() == DamageCause.FIRE || e.getCause() == DamageCause.FIRE_TICK || e.getCause() == DamageCause.HOT_FLOOR ||
				e.getCause() == DamageCause.LAVA || e.getCause() == DamageCause.WITHER || e.getCause() == DamageCause.POISON) {
			e.setCancelled(true);		
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getEntity().equals(getPlayer())) {
			if (e.getDamager() instanceof SplashPotion) {
				e.setCancelled(true);
			}
			if (e.getDamager() instanceof LingeringPotion) {
				e.setCancelled(true);
			}
			if (e.getDamager() instanceof AreaEffectCloud) {
				e.setCancelled(true);
			}	
		}
		if (e.getDamager().equals(getPlayer()) && e.getEntity().equals(getPlayer()) && e.getCause() == DamageCause.ENTITY_ATTACK && potiondrank) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
		if (e.getItem().getType().equals(Material.POTION)) {
			potiondrankchecker.start();
			potiondrank = true;
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getRegainReason() == RegainReason.MAGIC) {
			e.setCancelled(true);
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
			Material m = checkLoc.clone().add(0, 0.25, 0).getBlock().getType();
			Material lm = checkLoc.clone().add(0.5, 0, 0).getBlock().getType();
			Material rm = checkLoc.clone().add(0, 0, 0.5).getBlock().getType();
			Material lmm = checkLoc.clone().subtract(0.5, 0, 0).getBlock().getType();
			Material rmm = checkLoc.clone().subtract(0, 0, 0.5).getBlock().getType();
			Block b = checkLoc.clone().add(0, 0.25, 0).getBlock();
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
	    		return skill.start();
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
	
}