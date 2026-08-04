package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Frost;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.base.math.geometry.vector.VectorIterator;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableSet;
import rainstar.abilitywar.effect.Chill;
import rainstar.abilitywar.effect.SnowflakeMark;

@AbilityManifest(name = "유키", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7패시브 §8- §b만년설§f: §b§n빙결§f 외 §n상태이상§f을 받을 때 §b§n빙결§f로 교체합니다.",
		" 자신이 가하는 모든 §5마법 피해량§f이 $[DAMAGE_INCREASE]% 증가합니다.",
		"§7검 우클릭 §8- §b아이스 볼트§f: 영창을 시작합니다. 다시 검을 우클릭하면",
		" 바라보는 방향으로 §3서리§f를 내뿜습니다. §3서리§f는 영창 시간 비례 마법 피해를 입히며",
		" 적중 대상이 §9§n냉기§f 상태라면 §b§n빙결§f시키고, 아니라면 §9§n냉기§f시킵니다. $[CAST_COOLDOWN]",
		"§7철괴 좌클릭 §8- §9블리자드§f: 자신을 포함한 $[RANGE]칸 내 모든 생명체의 §b§n빙결§f을 깨뜨립니다.",
		" 자신은 §b§n빙결§f 시간의 $[MULTIPLY]배만큼 §b신속 $[SPEED_AMPLIFIER]§f를 획득하고, 다른 생명체는",
		" 방어력이 3 감소하는 §b§n눈꽃 표식§f을 부여받습니다§8(§73중첩 가능§8). $[BREAK_COOLDOWN]",
		"§1[§9냉기§1]§f 이동 속도, 공격 속도, 회복력이 감소합니다.",
		" 지속시간과 쿨타임 타이머가 천천히 흐릅니다."
		},
		summarize = {
		"자신이 받는 모든 상태이상이 지속시간 절반의 §b§n빙결§f이 됩니다.",
		"§7검 우클릭 시§f 영창을 시작해 영창 중에 다시 우클릭하면 서리를 내뿜어",
		"적중 대상들을 §9냉기§f시키고, 이미 §9냉기§f 상태라면 §3빙결§f시킵니다. $[CAST_COOLDOWN]",
		"§7철괴 좌클릭 시§f 영창 없이 주변의 모든 빙결 상태의 플레이어의 §3빙결§f을",
		"깨트려 피해를 입히고 방어력이 레벨당 3씩 감소하는 §b눈꽃 표식§f을 부여합니다.",
		"§9냉기 상태의 적§f은 이동 속도, 공격 속도가 감소하고 회복력이 줄어듭니다."
		})

public class Yuki extends AbilityBase implements ActiveHandler {
	
	public Yuki(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Integer> CAST_COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(Yuki.class,
			"casting-cooldown", 10, "# 영창 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> BREAK_COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(Yuki.class,
			"break-cooldown", 20, "# 파괴 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> DAMAGE_INCREASE = rsAbilitySettings.new SettingObject<Integer>(Yuki.class,
			"damage-increase", 100, "# 마법 피해 증가량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> RANGE = rsAbilitySettings.new SettingObject<Double>(Yuki.class, 
			"range", 8.0, "# 블리자드 범위", "# 단위: 칸") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };

	public static final SettingObject<Double> MULTIPLY = rsAbilitySettings.new SettingObject<Double>(Yuki.class, 
			"multiply", 5.0, "# 신속 지속시간", "# 단위: 배") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> SPEED_AMPLIFIER = 
			rsAbilitySettings.new SettingObject<Integer>(Yuki.class, "speed-amplifier", 1,
            "# 신속 포션 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
		@Override
		public String toString() {
			return "" + (1 + getValue());
        }
		
    };
	
	private static final Vector zeroV = new Vector(0, 0, 0);
	
	private final Cooldown castcool = new Cooldown(CAST_COOLDOWN.getValue(), "영창", CooldownDecrease._25);
	private final Cooldown breakcool = new Cooldown(BREAK_COOLDOWN.getValue(), "파괴");
    private final double range = RANGE.getValue();
    private final double multiply = MULTIPLY.getValue();
	private final double damageIncrease = (DAMAGE_INCREASE.getValue() * 0.01) + 1;
    private final int speed = SPEED_AMPLIFIER.getValue();
	private static final RGB startColor = RGB.of(254, 254, 254), endColor = RGB.of(16, 248, 254);
	private final List<RGB> gradations = Gradient.createGradient(12, startColor, endColor);
	
	@SuppressWarnings("unused")
	private Bullet bullet = null;
	private int cast = 0;
	private int addDamage = 6;
	private int number = 1;
	
	private static final Set<Material> swords;
	
	private static final Points LAYER = Points.of(0.12, new boolean[][]{
		{false, false, false, false, false, false, false, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, false, false, false, true, false, false, false, false, false},
		{false, false, false, false, false, false, true, true, true, false, false, false, false, false, false},
		{true, false, true, false, false, false, false, false, false, false, false, false, true, false, true},
		{false, false, true, false, false, false, false, true, false, false, false, false, true, false, false},
		{true, true, true, false, false, true, false, true, false, true, false, false, true, true, true},
		{false, false, false, true, false, false, true, true, true, false, false, true, false, false, false},
		{false, false, false, false, true, true, false, false, false, true, true, false, false, false, false},
		{false, true, true, false, false, false, false, true, false, false, false, false, true, true, false},
		{false, false, false, false, true, true, false, false, false, true, true, false, false, false, false},
		{false, false, false, true, false, false, true, true, true, false, false, true, false, false, false},
		{true, true, true, false, false, true, false, true, false, true, false, false, true, true, true},
		{false, false, true, false, false, false, false, true, false, false, false, false, true, false, false},
		{true, false, true, false, false, false, false, false, false, false, false, false, true, false, true},
		{false, false, false, false, false, false, true, true, true, false, false, false, false, false, false},
		{false, false, false, false, false, true, false, false, false, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, false, false, false, false, false, false, false}
	});
	
	private final Predicate<Entity> frostpredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity instanceof Player) {
				Participant participants = getGame().getParticipant((Player) entity);
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (!participants.hasEffect(Frost.registration)) return false;
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	@Override
	public boolean usesMaterial(Material material) {
		return (swords.contains(material) || material.equals(MaterialX.IRON_INGOT.getMaterial()));
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (swords.contains(material) && clicktype == ClickType.RIGHT_CLICK && !castcool.isCooldown()) {
			if (casting.isRunning()) {
				casting.stop(false);
				music.start();
				double damage = getPlayer().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
				if (getPlayer().getInventory().getItemInMainHand() != null && getPlayer().getInventory().getItemInMainHand().containsEnchantment(Enchantment.DAMAGE_ALL)) damage += ((getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL) + 1) * 0.5);
				new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1.5, 0), getPlayer().getLocation().getDirection(), cast, damage).start();	
				cast = 0;
				return castcool.start();
			} else if (!casting.isRunning() && getPlayer().isOnGround()) {
				casting.start();
			}
		}
		if (material == Material.IRON_INGOT && clicktype == ClickType.LEFT_CLICK && !breakcool.isCooldown() && !breaking.isRunning()) {
			breaking.start();
			if (circle.isRunning()) circle.stop(false);
			return circle.start();
		}
		return false;
	}
	
	private final AbilityTimer breaking = new AbilityTimer() {
		
		Location mylocation;
		
    	@Override
		public void onStart() {
    		mylocation = getPlayer().getLocation().clone();
    		SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(mylocation, 1, 1.3f);
    	}
		
    	@Override
		public void run(int count) {
    		Player p = LocationUtil.getNearestEntity(Player.class, mylocation, frostpredicate);
    		if (p != null) {
    			if (p.equals(getPlayer())) {
    				ParticleLib.BLOCK_CRACK.spawnParticle(p.getLocation(), 1, 1, 1, 200, 0.5, MaterialX.ICE);
    				SoundLib.BLOCK_GLASS_BREAK.playSound(p.getLocation(), 1, 1.2f);
    				getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (getParticipant().getPrimaryEffect(Frost.registration).getCount() * multiply), speed));
    				getParticipant().removeEffects(Frost.registration);
    				addDamage += 1;
    			} else if (LocationUtil.isInCircle(mylocation, p.getLocation(), range)) {
    				ParticleLib.BLOCK_CRACK.spawnParticle(p.getLocation(), 1, 1, 1, 200, 0.5, MaterialX.ICE);
    				SoundLib.BLOCK_GLASS_BREAK.playSound(p.getLocation(), 1, 1.2f);
    				getGame().getParticipant(p).removeEffects(Frost.registration);
    				Damages.damageMagic(p, getPlayer(), false, addDamage);
    				if (getGame().getParticipant(p).hasEffect(SnowflakeMark.registration)) {
    					SnowflakeMark.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 20, Math.min(getGame().getParticipant(p).getPrimaryEffect(SnowflakeMark.registration).getLevel() + 1, 3));
    				} else {
    					SnowflakeMark.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 20, 1);
    				}
    				addDamage += 1;
        		}	
    		} else {
    			stop(false);
    		}
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		breakcool.start();
    		addDamage = 4;
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer circle = new AbilityTimer(30) {
		
		private VectorIterator iterator;
		
    	@Override
		public void onStart() {
    		this.iterator = Circle.infiniteIteratorOf(10, 100);
    	}
		
    	@Override
		public void run(int i) {
			for (int j = 0; j < 5; j++) {
				Location loc = getPlayer().getLocation().clone().add(iterator.next());
				loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), getPlayer().getLocation().getY(), loc.getBlockX(), loc.getBlockZ()) + 0.1);
				ParticleLib.CLOUD.spawnParticle(loc, 0, 0, 0, 1, 0.1);
			}
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer casting = new AbilityTimer() {
		
    	@Override
		public void run(int count) {
    		if (count % 5 == 0) {
        		if (cast < 12) {
        			cast++;
        		}
        		final Location eyeLocation = getPlayer().getLocation().clone().add(getPlayer().getLocation().getDirection().add(new Vector(0, 2.5, 0)));
    			final float yaw = getPlayer().getLocation().getYaw();
    			for (Location loc : LAYER.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
    				ParticleLib.REDSTONE.spawnParticle(loc, gradations.get(cast - 1));
    			}
    			LAYER.rotateAroundAxisY(yaw);
			}
    		NMS.sendTitle(getPlayer(), Strings.repeat("§b§k|", cast).concat(Strings.repeat("§7§k|", 12 - cast)), "", 0, 100, 1);	
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		NMS.clearTitle(getPlayer());
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer music = new AbilityTimer() {

    	@Override
		public void run(int count) {
    		switch(number) {
    		case 1:
    		case 2:
    			switch(count) {
    			case 1: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
						break;
    			case 3: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
						break;
    			case 4: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.A));
						break;
    			case 6:	SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.F));
    					stop(false);
    					number++;
						break;
    			}
    		break;
    		case 3:
    			switch(count) {
    			case 1: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
						break;
    			case 4:
    			case 7: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
						break;
    			case 6: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
						break;
    			case 10: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.A));
						 break;
    			case 12: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(0, Tone.D));
				 		 break;
    			case 13: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
				 		 stop(false);
				 		 number++;
				 		 break;
    			}
    		break;
    		case 4:
    			switch(count) {
    			case 1: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
						break;
    			case 4:	SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
				 		break;
    			case 7: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.A));
				 		break;
    			case 10: SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
    					 stop(false);
    					 number = 1;
		 				 break;
    			}
    		break;
    		}
    	}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	@SubscribeEvent
	private void onPlayerMove(PlayerMoveEvent e) {
		if (casting.isRunning() && e.getPlayer().equals(getPlayer()) && cast < 12) getPlayer().setVelocity(zeroV);
	}
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		else if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		}
		
		if (getPlayer().equals(damager) && e.getCause().equals(DamageCause.MAGIC)) {
			e.setDamage(e.getDamage() * damageIncrease);
		}
	}

	public class Bullet extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final Predicate<Entity> predicate;
		private final Set<Player> attacked = new HashSet<>();
		private final double baseDamage;

		private int cast;
		private Location lastLocation;
		
		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, int cast, double baseDamage) {
			super((int) Math.max(2 + cast / 3, 2));
			setPeriod(TimeUnit.TICKS, 1);
			Yuki.this.bullet = this;
			this.cast = cast;
			this.shooter = shooter;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-2.5, -2.5, -2.5, 2.5, 2.5, 2.5);
			this.forward = arrowVelocity.multiply(2.5);
			this.lastLocation = startLocation;
			this.baseDamage = baseDamage;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (entity.equals(shooter)) return false;
					if (entity instanceof Player) {
						if (!getGame().isParticipating(entity.getUniqueId())
								|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
								|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
							return false;
						}
						if (getGame() instanceof Teamable) {
							final Teamable teamGame = (Teamable) getGame();
							final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(shooter.getUniqueId());
							if (participant != null) {
								return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
						}
					}
					return true;
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
		}
		
		@Override
		protected void run(int i) {
			final Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
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
					return lastLocation.clone().add(unit.clone().multiply(cursor));
				}
			}; iterator.hasNext(); ) {
				final Location location = iterator.next();
				entity.setLocation(location);
				final Block block = location.getBlock();
				final Material type = block.getType();
				if (type.isSolid() && !type.equals(Material.ICE)) {
					stop(false);
					return;
				}
				for (Player p : LocationUtil.getConflictingEntities(Player.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(p)) {
						Damages.damageMagic(p, (Player) shooter, true, (float) (cast * baseDamage * 0.08));
						if (getGame().getParticipant(p).hasEffect(Chill.registration) && !getGame().getParticipant(p).hasEffect(Frost.registration) && !attacked.contains(p) && p != null) {
							attacked.add(p);
							Frost.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 4);
						} else if (!getGame().getParticipant(p).hasEffect(Chill.registration) && !getGame().getParticipant(p).hasEffect(Frost.registration) && !attacked.contains(p) && p != null) {
							Chill.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 15);
							attacked.add(p);
						}
					}
				}
				ParticleLib.BLOCK_CRACK.spawnParticle(location, 0, 0, 0, 1, MaterialX.ICE);
				ParticleLib.SNOW_SHOVEL.spawnParticle(location, 0.75, 0.75, 0.75, 3, 0.1f);
			}
			lastLocation = newLocation;
		}
		
		@Override
		protected void onEnd() {
			entity.remove();
			attacked.clear();
			Yuki.this.bullet = null;
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
			attacked.clear();
			Yuki.this.bullet = null;
		}

		public class ArrowEntity extends CustomEntity implements Deflectable {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			public Vector getDirection() {
				return forward.clone();
			}

			@Override
			public void onDeflect(Participant deflector, Vector newDirection) {
				stop(false);
				final Player deflectedPlayer = deflector.getPlayer();
				new Bullet(deflectedPlayer, lastLocation, newDirection, 3, baseDamage).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
			}

		}
		
	}
	
}