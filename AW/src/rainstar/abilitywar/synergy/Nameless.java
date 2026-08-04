package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.effect.Bleed;
import daybreak.abilitywar.game.manager.effect.Oppress;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;
import rainstar.abilitywar.utils.RankColor;

@AbilityManifest(name = "무명", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7§o이 능력의 효과는 알 수 없습니다."
		})
public class Nameless extends Synergy implements ActiveHandler, TargetHandler {

	public Nameless(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> MIN_ABILITY = rsSynergySettings.new SettingObject<Integer>(Nameless.class,
			"min-ability", 3, "# 내부 효과의 최소 개수", "# 이 수치가 최대치보다 크면 이 수치를 최대치로 취급합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> MAX_ABILITY = rsSynergySettings.new SettingObject<Integer>(Nameless.class,
			"max-ability", 5, "# 내부 효과의 최대 개수", "# 이 수치가 최소치보다 작으면 이 수치를 최소치로 취급합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	enum RandomSkill {
		UNTARGETABLE("§b타게팅 불가"),
		EFFECT_IMMUNE("§5상태이상 면역"),
		UNSTOPPABLE("§3저지 불가"),
		REVIVE("§a부활"),
		RANDOM_POTION_EFFECT("§6무작위 포션 효과"),
		UPGRADE_POTION_EFFECT("§5포션 효과 강화"),
		BACKATTACK("§9백어택"),
		PIERCE("§4피해 관통"),
		CRITICAL_DAMAGE_INCREASE("§4치명타 피해 강화"),
		DAMAGE_INCREASE("§c공격력 증가"),
		DAMAGE_DECREASE("§b받는 피해 감소"),
		OTHER_DAMAGE_IMMUNE("§2기타 피해 면역"),
		EVADE("§b회피"),
		HOMING("§5투사체 유도"),
		REGENERATION("§d재생"),
		ATTACK_KNOCKBACK_DECREASE("§e공격 넉백 감소"),
		OPRESSING("§c적 제압"),
		SPEED_UP("§b이동 속도 증가"),
		RANDOM_ABILITY("§b무작위 능력"),
		RANDOM_SYNERGY("§6무작위 시너지");
		
		private final String name;
		
		private RandomSkill(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
	}
	
	private static final List<PotionEffectType> GOOD_POTIONS = Arrays.asList(
		    PotionEffectType.REGENERATION,
		    PotionEffectType.SPEED,
		    PotionEffectType.FIRE_RESISTANCE,
		    PotionEffectType.HEAL,
		    PotionEffectType.NIGHT_VISION,
		    PotionEffectType.INCREASE_DAMAGE,
		    PotionEffectType.JUMP,
		    PotionEffectType.WATER_BREATHING,
		    PotionEffectType.INVISIBILITY,
		    PotionEffectType.LUCK,
		    PotionEffectType.ABSORPTION,
		    PotionEffectType.FAST_DIGGING,
		    PotionEffectType.HEALTH_BOOST,
		    PotionEffectType.SATURATION,
		    PotionEffectType.DAMAGE_RESISTANCE
		);
	
	private final int min = Math.min(MIN_ABILITY.getValue(), MAX_ABILITY.getValue());
	private final int max = Math.max(MIN_ABILITY.getValue(), MAX_ABILITY.getValue());
	
	private final List<RandomSkill> skillList = new ArrayList<>(Arrays.asList(RandomSkill.values()));
	private final List<RandomSkill> skills = new ArrayList<>();
	private final Random random = new Random();
	private boolean onetime = true;
	private int revive = 0, randomPotionPeriod = 0, potionUpgradePeriod = 0;
	private boolean attackCooldown = false;
	private AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", (random.nextInt(36) + 5) * 0.01, Operation.ADD_SCALAR);
	private AbilityBase ability, synergy;
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof ArmorStand) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
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
	
	public AbilityRegistration getRandomRankAbility() {
		final List<AbilityRegistration> registrations = AbilityList.values().stream().filter(
				ability -> ability.getAbilityClass().getAnnotation(Beta.class) == null && ability.isAvailable(getGame().getClass()) && !Configuration.Settings.isBlacklisted(ability.getManifest().name())
		).collect(Collectors.toList());
		return registrations.isEmpty() ? null : random.pick(registrations);
	}
	
    public AbilityRegistration getRandomSynergy() {
		Set<AbilityRegistration> synergies = new HashSet<>();
		
        for (AbilityRegistration synergy : SynergyFactory.getSynergies()) {
        	String name = synergy.getManifest().name();
        	if (!Configuration.Settings.isBlacklisted(name) && synergy.getAbilityClass().getAnnotation(Beta.class) == null) {
        		synergies.add(synergy);
        	}
        }
		
        Random r = new Random();
        return synergies.toArray(new AbilityRegistration[]{})[r.nextInt(synergies.size())];
    }
	
	private boolean isBehind(Entity owner, Entity target) {
	    Vector eye = owner.getLocation().getDirection().setY(0).normalize();
	    Vector toEntity = target.getLocation().getDirection().setY(0).normalize();
	    double dot = toEntity.dot(eye);
	    return dot >= 0.75D;
	}
	
	@SuppressWarnings("deprecation")
	public static boolean isCriticalHit(Player p, boolean attackcool) {
		return (!p.isOnGround() && p.getFallDistance() > 0.0F && 
	      !p.getLocation().getBlock().isLiquid() &&
	      attackcool == false &&
	      !p.isInsideVehicle() && !p.isSprinting() && p
	      .getActivePotionEffects().stream().noneMatch(pe -> (pe.getType() == PotionEffectType.BLINDNESS)));
	}
	
	private final Predicate<Effect> allEffectPredicate = new Predicate<Effect>() {
		@Override
		public boolean test(Effect effect) {
			return true;
		}

		@Override
		public boolean apply(@Nullable Effect arg0) {
			return false;
		}
	};
	
	private final Predicate<Effect> moveEffectPredicate = new Predicate<Effect>() {
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
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (onetime) {
				onetime = false;
				Collections.shuffle(skillList);
				int count = Math.min(skillList.size(), random.nextInt(max - min + 1) + min);
				skills.addAll(skillList.subList(0, count));
				
				if (skills.contains(RandomSkill.REVIVE)) revive = random.nextInt(3) + 1;
				if (skills.contains(RandomSkill.RANDOM_POTION_EFFECT)) randomPotionPeriod = random.nextInt(201) + 100;
				if (skills.contains(RandomSkill.UPGRADE_POTION_EFFECT)) potionUpgradePeriod = random.nextInt(1001) + 200;
				
				if (skills.contains(RandomSkill.RANDOM_ABILITY)) {
					try {
						this.ability = AbilityBase.create(getRandomRankAbility(), getParticipant());
						this.ability.setRestricted(false);
					} catch (ReflectiveOperationException e) {
						e.printStackTrace();
					}
				}
				
				if (skills.contains(RandomSkill.RANDOM_SYNERGY)) {
					try {
						this.synergy = AbilityBase.create(getRandomSynergy(), getParticipant());
						this.synergy.setRestricted(false);
					} catch (ReflectiveOperationException e) {
						e.printStackTrace();
					}
				}
			}
			
			passive.start();
		}
		
		if (update == Update.ABILITY_DESTROY) {
		    List<String> effects = new ArrayList<>();

		    for (RandomSkill skill : skills) {
		        switch (skill) {
		        case RANDOM_ABILITY:
		            effects.add(skill.getName() + ": " +
		                    (ability != null ? RankColor.getColor(ability.getRank()) + ability.getDisplayName() : "§7없음"));
		            break;

		        case RANDOM_SYNERGY:
		            effects.add(skill.getName() + ": " +
		                    (synergy != null ? RankColor.getColor(synergy.getRank()) + synergy.getDisplayName() : "§7없음"));
		            break;

		        default:
		            effects.add(skill.getName());
		            break;
		        }
		    }

		    Bukkit.broadcastMessage("§8[§7Nameless§8] §f효과: §7" + String.join("§f, §7", effects));
		}
		
		for (AbilityBase ability : abilities()) {
			if (ability == null) continue;

			switch (update) {
			case RESTRICTION_CLEAR:
				ability.setRestricted(false);
				break;
			case RESTRICTION_SET:
				ability.setRestricted(true);
				break;
			case ABILITY_DESTROY:
				ability.destroy();
				break;
			}
		}
		
		if (update == Update.ABILITY_DESTROY) {
			ability = null;
			synergy = null;
		}
	}
	
	private AbilityBase[] abilities() {
		return new AbilityBase[] { ability, synergy };
	}
	
	private final AbilityTimer addSpeed = new AbilityTimer() {
		
		@Override
		public void onStart() {
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void onStart() {
			if (skills.contains(RandomSkill.SPEED_UP)) addSpeed.start();
		}
		
		@Override
		public void run(int count) {
			if (skills.contains(RandomSkill.UNTARGETABLE)) getParticipant().attributes().TARGETABLE.setValue(false);
			if (skills.contains(RandomSkill.EFFECT_IMMUNE)) getParticipant().removeEffects(allEffectPredicate);
			if (skills.contains(RandomSkill.UNSTOPPABLE)) getParticipant().removeEffects(moveEffectPredicate);
			if (skills.contains(RandomSkill.RANDOM_POTION_EFFECT) && count % randomPotionPeriod == 0) {
				PotionEffect pe = new PotionEffect(random.pick(GOOD_POTIONS), random.nextInt(121) + 20, random.nextInt(2));
				getPlayer().addPotionEffect(pe);
				SoundLib.BLOCK_BREWING_STAND_BREW.playSound(getPlayer().getLocation(), 1, 1.5f);
			}
			if (skills.contains(RandomSkill.UPGRADE_POTION_EFFECT) && count % potionUpgradePeriod == 0) {
				for (PotionEffect pe : getPlayer().getActivePotionEffects()) {
	    			if (GOOD_POTIONS.contains(pe.getType())) {
	    				PotionEffect newpe = new PotionEffect(pe.getType(), pe.getDuration(), pe.getAmplifier() + 1, false, true);
	    				getPlayer().removePotionEffect(pe.getType());
	    				getPlayer().addPotionEffect(newpe);
	    			}
				}
				SoundLib.BLOCK_BREWING_STAND_BREW.playSound(getPlayer().getLocation(), 1, 0.75f);
			}
			if (skills.contains(RandomSkill.REGENERATION)) {
				EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), (random.nextInt(400) + 1) * 0.0001, RegainReason.REGEN);
	    		if (!event.isCancelled()) {
	    			Healths.setHealth(getPlayer(), Math.min(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), getPlayer().getHealth() + event.getAmount()));
	    		}
			}
			
			if (NMS.getAttackCooldown(getPlayer()) > 0.848 && attackCooldown) attackCooldown = false;
			else if (NMS.getAttackCooldown(getPlayer()) <= 0.848 && !attackCooldown) attackCooldown = true;
		}
		
		@Override
		public void onSilentEnd() {
			getParticipant().attributes().TARGETABLE.setValue(true);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
		
	@SubscribeEvent(onlyRelevant = true)
	private void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		if (skills.contains(RandomSkill.EFFECT_IMMUNE)) e.setCancelled(true);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onVelocity(PlayerVelocityEvent e) {
		if (skills.contains(RandomSkill.UNSTOPPABLE)) e.setCancelled(true);
	}
	
	@SubscribeEvent
	private void onEntityResurrectEvent(EntityResurrectEvent e) {
		if (e.getEntity().equals(getPlayer()) && revive > 0 && skills.contains(RandomSkill.REVIVE)) {
			ItemStack leftHand = getPlayer().getInventory().getItemInOffHand();
			e.setCancelled(false);
			getPlayer().getInventory().setItemInOffHand(leftHand);
			revive--;
		}
	}
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && !e.getEntity().equals(getPlayer())) {
			if (skills.contains(RandomSkill.BACKATTACK) && isBehind(getPlayer(), e.getEntity())) {
				e.setDamage(e.getDamage() * (1 + ((random.nextInt(41) + 10) * 0.01)));
				ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0.25, 1, 0.25, 25, 1, MaterialX.REDSTONE_BLOCK);
				SoundLib.BLOCK_ANVIL_LAND.playSound(e.getEntity().getLocation(), 1, 1.8f);
				if (e.getEntity() instanceof Player && getGame().isParticipating((Player) e.getEntity())) {
					if (!getGame().getParticipant((Player) e.getEntity()).hasEffect(Bleed.registration)) Bleed.apply(getGame().getParticipant((Player) e.getEntity()), TimeUnit.TICKS, random.nextInt(81) + 20, random.nextInt(39) + 2);
				}
			}
			
			if (skills.contains(RandomSkill.CRITICAL_DAMAGE_INCREASE)) {
				boolean crit = false;
				if (NMS.isArrow(e.getDamager())) {
					Arrow arrow = (Arrow) e.getDamager();
					if (arrow.isCritical()) crit = true;
				} else if (getPlayer().equals(damager) && !e.getCause().equals(DamageCause.THORNS)) crit = isCriticalHit(damager, attackCooldown);
				
				if (crit) {
					e.setDamage(e.getDamage() * (1 + ((random.nextInt(26) + 25) * 0.01)));
					ParticleLib.CRIT.spawnParticle(e.getEntity().getLocation().clone().add(0, 1, 0), .3f, .3f, .3f, 100, 1);
				}
			}
			
			if (skills.contains(RandomSkill.DAMAGE_INCREASE)) e.setDamage(e.getDamage() * (1 + ((random.nextInt(21) + 5) * 0.01)));
		
			if (skills.contains(RandomSkill.OPRESSING) && e.getEntity() instanceof Player) {
				Player victim = (Player) e.getEntity();
				if (getGame().isParticipating(victim)) {
					Oppress.apply(getGame().getParticipant(victim), TimeUnit.TICKS, random.nextInt(31) + 10);
				}
			}
		}
		
		if (skills.contains(RandomSkill.ATTACK_KNOCKBACK_DECREASE) && e.getDamager().equals(getPlayer())) {
			e.getEntity().setVelocity(getPlayer().getLocation().toVector().subtract(e.getEntity().getLocation().toVector()).multiply(0.25).setY(0));
		}
		
		if (e.getEntity().equals(getPlayer())) {
			if (skills.contains(RandomSkill.DAMAGE_DECREASE)) e.setDamage(e.getDamage() * (1 - ((random.nextInt(21) + 5) * 0.01)));
		
			if (skills.contains(RandomSkill.OTHER_DAMAGE_IMMUNE)) {
				if (e.getCause().equals(DamageCause.ENTITY_ATTACK) || e.getCause().equals(DamageCause.ENTITY_SWEEP_ATTACK)) {
					if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Projectile)) e.setCancelled(true);
				}
				onEntityDamage(e);	
			}
			
			if (skills.contains(RandomSkill.EVADE)) {
				if (random.nextInt(100) < random.nextInt(36) + 5) {
	    			e.setCancelled(true);
	    			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.7f);
	    			getPlayer().setNoDamageTicks(20);
	    		}
			}
		}
	}
	
	@SubscribeEvent(priority = 10)
	private void onEntityDamageByEntityPierce(EntityDamageByEntityEvent e) {
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager)) {
			if (skills.contains(RandomSkill.PIERCE) && e.getEntity() instanceof Player) {
				Player victim = (Player) e.getEntity();
				double trueDamage = (e.getDamage() * ((random.nextInt(21) + 10) * 0.01));
				e.setDamage(e.getDamage() - trueDamage);
				if (Damages.canDamage(victim, DamageCause.ENTITY_ATTACK, trueDamage)) {
					Healths.setHealth(victim, Math.max(1, victim.getHealth() - trueDamage));	
				}
				ParticleLib.CRIT_MAGIC.spawnParticle(victim.getLocation(), 0.25, 1, 0.25, 100, 1);
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (skills.contains(RandomSkill.OTHER_DAMAGE_IMMUNE)) {
			if (!e.getCause().equals(DamageCause.ENTITY_ATTACK) && !e.getCause().equals(DamageCause.ENTITY_SWEEP_ATTACK) && !e.getCause().equals(DamageCause.PROJECTILE) && !e.getCause().equals(DamageCause.VOID) && !e.getCause().equals(DamageCause.SUFFOCATION)) e.setCancelled(true);	
		}
		
		if (skills.contains(RandomSkill.EVADE)) {
			if (random.nextInt(100) < random.nextInt(36) + 5) {
    			e.setCancelled(true);
    			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1, 1.7f);
    			getPlayer().setNoDamageTicks(20);
    		}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (skills.contains(RandomSkill.HOMING) && getPlayer().equals(e.getEntity().getShooter())) {
			new Homing(e.getEntity(), e.getEntity().getVelocity().length(), (random.nextInt(41) + 10) * 0.1).start();
		}
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		boolean activated = false;

		if (ability instanceof ActiveHandler) {
			activated |= ((ActiveHandler) ability).ActiveSkill(material, clickType);
		}

		if (synergy instanceof ActiveHandler) {
			activated |= ((ActiveHandler) synergy).ActiveSkill(material, clickType);
		}

		return activated;
	}
	
	@Override
	public void TargetSkill(Material material, LivingEntity entity) {
		for (AbilityBase ability : abilities()) {
			if (ability instanceof TargetHandler) {
				((TargetHandler) ability).TargetSkill(material, entity);
			}
		}
	}
	
	@Override
	public Set<GameTimer> getTimers() {
		Set<GameTimer> timers = super.getTimers();

		for (AbilityBase ability : abilities()) {
			if (ability != null) {
				timers = SetUnion.union(timers, ability.getTimers());
			}
		}

		return timers;
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		Set<GameTimer> timers = super.getRunningTimers();

		for (AbilityBase ability : abilities()) {
			if (ability != null) {
				timers = SetUnion.union(timers, ability.getRunningTimers());
			}
		}

		return timers;
	}

	@Override
	public boolean usesMaterial(Material material) {
		for (AbilityBase ability : abilities()) {
			if (ability != null && ability.usesMaterial(material)) {
				return true;
			}
		}

		return super.usesMaterial(material);
	}
	
	class Homing extends AbilityTimer implements Listener {
		
		private final Projectile projectile;
		private final double lengths;
		private final double range;
		
		private Homing(Projectile projectile, double length, double range) {
			super(TaskType.REVERSE, 600);
			setPeriod(TimeUnit.TICKS, 1);
			this.projectile = projectile;
			this.lengths = length;
			this.range = range;
		}
		
		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		protected void run(int arg0) {
			if (projectile != null) {
				Player p = LocationUtil.getNearestEntity(Player.class, projectile.getLocation(), predicate);
				if (p != null) {
					if (projectile.getLocation().distanceSquared(p.getEyeLocation()) <= (range * range)) {
						projectile.setGravity(false);
						projectile.setVelocity(VectorUtil.validateVector((p.getPlayer().getEyeLocation().toVector()
									.subtract(projectile.getLocation().toVector())).normalize().multiply(lengths)));	
					}
				}
			}
		}
		
		@EventHandler
		public void onProjectileHit(ProjectileHitEvent e) {
			if (e.getEntity().equals(projectile)) {
				if (e.getHitBlock() != null) stop(false);
				else {
					this.pause();
					projectile.setGravity(true);
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
			projectile.setGravity(true);
		}
		
		@Override
		protected void onSilentEnd() {
			onEnd();
		}
	}
	
}
