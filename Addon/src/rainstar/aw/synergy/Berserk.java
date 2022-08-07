package rainstar.aw.synergy;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

@AbilityManifest(
		name = "베르세르크", rank = Rank.S, species = Species.DEMIGOD, explain = {
		"§7철괴 우클릭 §8- §6전사의 힘§f: 광전사 모드에 돌입합니다. $[COOLDOWN]",
		" 마지막으로 사용했을 때 당시의 체력까지 복구해야 사용 가능합니다.",
		"§7모드 §8- §c광전사§f: 체력이 반칸이 되기 전까지 계속하여 유지됩니다.",
		" 매 초 내 최대 체력의 10%를 §3소모§f하고, 회복 효과를 받을 수 없습니다.",
		" §b무적 상태§f가 되고 §b이동 속도, 공격 속도가 대폭 증가§f합니다.",
		" 내 남은 체력에 반비례하여 추가 피해량이 최대 $[MAX_DAMAGE]까지 증가합니다.",
		" 근접 공격만 가능합니다.",
		"§7적 처치 §4- §c버서크§f: 최대 체력의 1/3만큼 체력을 특수 §d회복§f합니다.",
		" 또한 특수 회복한 회복량의 절반만큼 §e흡수 체력§f을 획득합니다.",
		"§7모드 종료 §8- §b부작용§f: 광전사 모드 돌입 전 체력으로 복구하기 전까지",
		" §d회복력§f이 증가하나 이동력과 공격 속도, 공격력이 급감합니다."
		})

@SuppressWarnings("deprecation")
public class Berserk extends Synergy implements ActiveHandler {

	public Berserk(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = synergySettings.new SettingObject<Integer>(Berserk.class, 
			"cooldown", 60, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> MAX_DAMAGE = synergySettings.new SettingObject<Integer>(Berserk.class, 
			"add-max-damage", 9, "# 스킬 지속 중", "# 최대 체력 반비례 추가 피해량 최대치", "# 7일 경우 최대 7까지 대미지가 증가합니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	public static final SettingObject<Integer> MIN_DAMAGE = synergySettings.new SettingObject<Integer>(Berserk.class, 
			"remove-min-damage", 5, "# 리스크 지속 중", "# 최대 체력 비례 감소 피해량 최대치", "# 5일 경우 최대 5까지 대미지가 감소합니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	private final Predicate<Effect> effectpredicate = new Predicate<Effect>() {
		@Override
		public boolean test(Effect effect) {
			final ImmutableSet<EffectType> effectType = effect.getRegistration().getEffectType();
			return effectType.contains(EffectType.MOVEMENT_RESTRICTION) || effectType.contains(EffectType.MOVEMENT_INTERRUPT);
		}

		@Override
		public boolean apply(@Nullable Effect arg0) {
			return false;
		}
	};
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	private final int maxDamage = MAX_DAMAGE.getValue();
	private final int minDamage = MIN_DAMAGE.getValue();
	private boolean jumped = false;
	
	private int particleSide = 15;
	
	private final Crescent crescent = Crescent.of(1, 20);
	
	private double startHealth = 0;
	
	private AttributeModifier movespeed, attackspeed, attackspeeddown;
	
	private static final Set<Material> bows;
	
	static {
		if (MaterialX.CROSSBOW.isSupported()) {
			bows = ImmutableSet.of(MaterialX.BOW.getMaterial(), MaterialX.CROSSBOW.getMaterial());
		} else {
			bows = ImmutableSet.of(MaterialX.BOW.getMaterial());
		}
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (!cool.isCooldown()) {
				if (!skill.isRunning()) {
					if (!risk.isRunning()) {
						skill.start();		
					} else {
						getPlayer().sendMessage("§4[§c!§4] §f아직 체력이 복구되지 않았습니다.");
					}
				} else {
					getPlayer().sendMessage("§4[§c!§4] §f능력이 지속 중입니다.");
				}
			}
		}
		return false;
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(PlayerInteractEvent e) {
		if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) && e.getItem() != null && bows.contains(e.getItem().getType()) && skill.isRunning()) {
			getPlayer().updateInventory();
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onEntityShootBow(EntityShootBowEvent e) {
		if (skill.isRunning()) e.setCancelled(true);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (skill.isRunning()) e.setCancelled(true);
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && skill.isRunning()) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().equals(getPlayer())) {
			double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			double nowHealth = getPlayer().getHealth();
			if (skill.isRunning()) {
				e.setDamage(e.getDamage() + (((maxHealth - (nowHealth - 1)) / maxHealth) * maxDamage));
				SoundLib.ENTITY_POLAR_BEAR_WARNING.playSound(getPlayer().getLocation(), 1f, 0.95f);
    			new CutParticle(particleSide).start();
    			particleSide *= -1;
    			e.getEntity().getWorld().strikeLightningEffect(e.getEntity().getLocation());
			} else if (risk.isRunning()) {
				e.setDamage(Math.max(1, e.getDamage() - ((1 / (maxHealth - nowHealth)) * minDamage)));	
			}
		}
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (getPlayer().equals(projectile.getShooter()) && skill.isRunning()) {
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			SoundLib.ENTITY_GHAST_DEATH.playSound(getPlayer().getLocation(), 1, 0.5f);
			ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0.25, 0.5, 0.25, 150, 0.77, MaterialX.REDSTONE_BLOCK);
			risk.stop(false);
		}
		if (e.getEntity().getKiller() != null) {
			if (e.getEntity().getKiller().equals(getPlayer()) && skill.isRunning()) {
				new AbilityTimer(5) {				
					private final Location startLocation = e.getEntity().getLocation().clone();
					
					@Override
					protected void run(int count) {
						ParticleLib.DAMAGE_INDICATOR.spawnParticle(startLocation.clone().add(Line.vectorAt(startLocation, getPlayer().getLocation(), 5, 5 - count)), 0, 0, 0, 1, 0);
						ParticleLib.HEART.spawnParticle(startLocation.clone().add(Line.vectorAt(startLocation, getPlayer().getLocation(), 5, 5 - count)), 0, 0, 0, 1, 0);					
					}
					
					@Override
					protected void onEnd() {
						double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
						double healed = Healths.setHealth(getPlayer(), getPlayer().getHealth() + (maxHealth / 3));
						NMS.setAbsorptionHearts(getPlayer(), (float) (NMS.getAbsorptionHearts(getPlayer()) + (healed / 2)));
						SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(getPlayer().getLocation(), 1, 0.5f);	
					}
					
				}.setPeriod(TimeUnit.TICKS, 2).start();
			}	
		}
	}
	
	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity().equals(getPlayer()) && skill.isRunning()) e.setCancelled(true);
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (risk.isRunning() && e.getPlayer().equals(getPlayer())) {
			if (!jumped) getPlayer().setVelocity(getPlayer().getVelocity().multiply(0.7));
			if (e.getTo().getY() > e.getFrom().getY()) {
				jumped = true;
			}
			if (getPlayer().isOnGround()) {
				jumped = false;
			}
		}
	}
	
	private final AbilityTimer skill = new AbilityTimer() {
		
		private boolean fiftypercent = true;
		private boolean thirtypercent = true;
		private boolean tenpercent = true;
		
		@Override
    	public void onStart() {
			ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0.25, 0.5, 0.25, 150, 0.77, MaterialX.REDSTONE_BLOCK);
        	SoundLib.ENTITY_WITHER_SHOOT.playSound(getPlayer().getLocation(), 1, 0.5f);
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation());
			getPlayer().setVelocity(VectorUtil.validateVector(getPlayer().getLocation().getDirection().clone().normalize().multiply(new Vector(1.5, 1, 1.5))));
			getPlayer().getWorld().strikeLightningEffect(getPlayer().getLocation());
			startHealth = getPlayer().getHealth();
    		movespeed = new AttributeModifier(UUID.randomUUID(), "movespeed", 0.15, Operation.ADD_NUMBER);
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(movespeed);
    		attackspeed = new AttributeModifier(UUID.randomUUID(), "attackspeed", 2, Operation.ADD_NUMBER);
    		getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).addModifier(attackspeed);
    	}
    	
    	@Override
		public void run(int count) {
    		double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    		if (getPlayer().getHealth() <= 1) {
    			stop(false);
    		} else {
    			if (count % 10 == 0) {
                	getPlayer().setHealth(Math.max(1, getPlayer().getHealth() - (maxHealth / 20)));
                	if (getPlayer().getHealth() <= 4.5) {
                		SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), 0.75f, 1.3f);
                		SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), 0.75f, 1f);
                	}
    			}
    			if (getPlayer().getHealth() <= (maxHealth * 0.5) && fiftypercent) {
    				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation());
    				fiftypercent = false;
    			}
    			if (getPlayer().getHealth() <= (maxHealth * 0.3) && thirtypercent) {
    				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation());
    				thirtypercent = false;
    			}
    			if (getPlayer().getHealth() <= (maxHealth * 0.1) && tenpercent) {
    				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation());
    				tenpercent = false;
    			}
    			if (getPlayer().getHealth() > (maxHealth * 0.5) && !fiftypercent) fiftypercent = true;
    			if (getPlayer().getHealth() > (maxHealth * 0.3) && !thirtypercent) thirtypercent = true;
    			if (getPlayer().getHealth() > (maxHealth * 0.1) && !tenpercent) tenpercent = true;
                ParticleLib.BLOCK_CRACK.spawnParticle(getPlayer().getLocation(), 0.25, 1, 0.25, 5, 1, MaterialX.NETHER_WART_BLOCK);
    		}
			if (getPlayer().hasPotionEffect(PotionEffectType.SLOW)) getPlayer().removePotionEffect(PotionEffectType.SLOW);
			if (getPlayer().hasPotionEffect(PotionEffectType.BLINDNESS)) getPlayer().removePotionEffect(PotionEffectType.BLINDNESS);
			if (getPlayer().hasPotionEffect(PotionEffectType.LEVITATION)) getPlayer().removePotionEffect(PotionEffectType.LEVITATION);
			getParticipant().removeEffects(effectpredicate);
    	}
    	
		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		public void onSilentEnd() {
			SoundLib.ENTITY_GENERIC_EXTINGUISH_FIRE.playSound(getPlayer().getLocation(), 1, 0.75f);
			SoundLib.ITEM_BOTTLE_FILL_DRAGONBREATH.playSound(getPlayer().getLocation(), 1, 0.6f);
			SoundLib.BLOCK_IRON_DOOR_CLOSE.playSound(getPlayer().getLocation(), 1, 0.55f);
			getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).removeModifier(attackspeed);
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(movespeed);
			risk.start();
			cool.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer risk = new AbilityTimer() {
		
		@Override
    	public void onStart() {
    		attackspeeddown = new AttributeModifier(UUID.randomUUID(), "attackspeeddown", -1, Operation.ADD_NUMBER);
    		getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).addModifier(attackspeeddown);
		}
		
    	@Override
		public void run(int count) {
    		if (getPlayer().getHealth() >= startHealth) {
    			stop(false);
    		} else {
    			double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), maxHealth * 0.0025, RegainReason.REGEN);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
	            	Healths.setHealth(getPlayer(), getPlayer().getHealth() + maxHealth * 0.0025);		
				}
    		}
			
    	}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		public void onSilentEnd() {
			getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).removeModifier(attackspeeddown);
		}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private class CutParticle extends AbilityTimer {

		private final Vector axis;
		private final Vector vector;
		private final Vectors crescentVectors1;

		private CutParticle(double angle) {
			super(1);
			setPeriod(TimeUnit.TICKS, 1);
			this.axis = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(getPlayer().getLocation().getDirection().setY(0).normalize(), 90), getPlayer().getLocation().getDirection().setY(0).normalize(), angle);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors1 = crescent.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis, -15);
		}
		
		@Override
		protected void run(int count) {
			Location baseLoc = getPlayer().getLocation().clone().add(vector).add(0, 1.3, 0);
			for (Location loc : crescentVectors1.toLocations(baseLoc)) {
				ParticleLib.BLOCK_CRACK.spawnParticle(loc, 0, 0, 0, 1, 0.15, MaterialX.REDSTONE_BLOCK);
			}
		}

	}
	
}