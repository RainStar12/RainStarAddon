package RainStarAbility;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import RainStarEffect.Chill;
import RainStarEffect.SnowflakeMark;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
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
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.base.math.geometry.vector.VectorIterator;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "유키", rank = Rank.S, species = Species.HUMAN, explain = {
		"§b얼음 속성§f의 눈꽃 마법사, 유키.",
		"§7지팡이 우클릭 §8- §b프로스트바이트§f: 영창을 시작합니다. 다시 지팡이를 우클릭하면",
		" 바라보는 방향으로 서리를 내뿜습니다. 서리는 영창한 만큼의 마법 대미지를 입히며",
		" 적중 대상이 냉기 상태라면 빙결시키고, 아니라면 냉기시킵니다. $[CAST_COOLDOWN]",
		" 눈과 얼음 위에서 서리의 대미지 및 사거리가 1씩 증가하며 신속 버프를 받습니다.",
		" §7적 처치 시 눈사람 소환§f: $[SNOWMAN_SPAWN]",
		"§7지팡이 좌클릭 §8- §b앱솔루트 제로§f: 주변의 모든 빙결 상태이상을 가진 플레이어를",
		" 얼음을 깨트려 마법 대미지를 입힙니다. 또한 대상에게 방어력이 2 감소하는",
		" 눈꽃 표식을 15초간 부여시킵니다. $[BREAK_COOLDOWN]",
		" 눈꽃 표식은 최대 3번까지 중첩됩니다.",
		"§7상태이상 §8- §9냉기§f: 이동 속도, 공격 속도가 감속하고 회복력이 줄어듭니다.",
		" 또한 지속시간과 쿨타임 타이머가 천천히 흐르게 됩니다."
		},
		summarize = {
		"§7지팡이 우클릭 시§f 영창을 시작해 영창 중에 다시 우클릭하면 서리를 내뿜어",
		"적중 대상들을 §9냉기§f시키고, 이미 §9냉기§f 상태라면 §3빙결§f시킵니다. $[CAST_COOLDOWN]",
		"§7지팡이 좌클릭 시§f 영창 없이 주변의 모든 빙결 상태의 플레이어의 §3빙결§f을",
		"깨트려 피해를 입히고 방어력이 레벨당 2씩 감소하는 §b눈꽃 표식§f을 부여합니다.",
		"§9냉기 상태의 적§f은 이동 속도, 공격 속도가 감소하고 회복력이 줄어듭니다."
		})

@Materials(materials = {
		Material.STICK
	})

public class Yuki extends AbilityBase implements ActiveHandler {
	
	public Yuki(Participant participant) {
		super(participant);
	}
	
	private boolean given = true;
	private Random random = new Random();
	private static final Vector zeroV = new Vector(0, 0, 0);
	
	private final Cooldown castcool = new Cooldown(CAST_COOLDOWN.getValue(), "영창", CooldownDecrease._25);
	private final Cooldown breakcool = new Cooldown(BREAK_COOLDOWN.getValue(), "파괴");
	private boolean snowman = SNOWMAN_SPAWN.getValue();
	
	@SuppressWarnings("unused")
	private Bullet bullet = null;
	private int cast = 0;
	private int addDamage = 6;
	private int number = 1;
	private static final RGB color1 = RGB.of(254, 254, 254), color2 = RGB.of(238, 254, 254), color3 = RGB.of(227, 253, 254),
			color4 = RGB.of(210, 253, 254), color5 = RGB.of(200, 253, 254), color6 = RGB.of(190, 252, 254),
			color7 = RGB.of(172, 252, 254), color8 = RGB.of(159, 252, 254), color9 = RGB.of(136, 252, 254),
			color10 = RGB.of(117, 252, 254), color11 = RGB.of(100, 250, 254), color12 = RGB.of(81, 250, 254);
	
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
	
	private final AbilityTimer buff = new AbilityTimer() {

		@Override
		public void run(int count) {
			Material m = getPlayer().getLocation().getBlock().getType();
			Material bm = getPlayer().getLocation().clone().subtract(0, 1, 0).getBlock().getType();
			if (m.equals(Material.SNOW) || bm.equals(Material.SNOW) || bm.equals(Material.SNOW_BLOCK) || bm.equals(Material.ICE) || bm.equals(Material.PACKED_ICE)) {
				PotionEffects.SPEED.addPotionEffect(getPlayer(), 5, 1, true);
			}
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final Predicate<Entity> frostpredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				Participant participants = getGame().getParticipant((Player) entity);
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
				if (!participants.hasEffect(Frost.registration)) return false;
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	public static final SettingObject<Integer> CAST_COOLDOWN 
	= abilitySettings.new SettingObject<Integer>(Yuki.class,
			"cast-cooldown", 6, "# 영창 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> BREAK_COOLDOWN 
	= abilitySettings.new SettingObject<Integer>(Yuki.class,
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
	
	public static final SettingObject<Boolean> SNOWMAN_SPAWN 
	= abilitySettings.new SettingObject<Boolean>(Yuki.class,
			"snowman-spawn", true, "# 유키가 플레이어를 죽일 때 눈사람을",
			"# 소환할 지 여부를 정합니다.") {
		
		@Override
		public String toString() {
			return getValue() ? "§b켜짐" : "§c꺼짐";
        }
		
	};
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR && given) {			
			ItemStack stick = new ItemStack(Material.STICK, 1);
			ItemMeta stickmeta = stick.getItemMeta();	
			
			if (random.nextInt(100) == 0) {
				stickmeta.setDisplayName("§6딱총나무 지팡이");
			} else {
				stickmeta.setDisplayName("§b설한의 지팡이");
			}		
			stickmeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
			stickmeta.addEnchant(Enchantment.KNOCKBACK, 2, true);
			stickmeta.setUnbreakable(true);		
			stick.setItemMeta(stickmeta);
			
			getPlayer().getInventory().addItem(stick);
			given = false;
		}
		if (update == Update.RESTRICTION_CLEAR) {
			buff.start();
		}
	}
	
	@Override
	public boolean usesMaterial(Material material) {
		return (material == MaterialX.STICK.getMaterial());
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.STICK && clicktype == ClickType.RIGHT_CLICK && !castcool.isCooldown()) {
			if (casting.isRunning()) {
				casting.stop(false);
				music.start();
				Material m = getPlayer().getLocation().getBlock().getType();
				Material bm = getPlayer().getLocation().subtract(0, 1, 0).getBlock().getType();
				if (m.equals(Material.SNOW) || bm.equals(Material.SNOW) || bm.equals(Material.SNOW_BLOCK) || bm.equals(Material.ICE) || bm.equals(Material.PACKED_ICE)) {
					new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1.5, 0), getPlayer().getLocation().getDirection(), cast + 3).start();
				} else {
					new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1.5, 0), getPlayer().getLocation().getDirection(), cast).start();	
				}
				cast = 0;
				return castcool.start();
			} else if (!casting.isRunning() && getPlayer().isOnGround()) {
				casting.start();
			}
		}
		if (material == Material.STICK && clicktype == ClickType.LEFT_CLICK && !breakcool.isCooldown() && !breaking.isRunning()) {
			breaking.start();
			if (circle.isRunning()) circle.stop(false);
			circle.start();
			return true;
		}
		return false;
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (snowman) {
			if (e.getEntity() instanceof Player && getPlayer().equals(e.getEntity().getKiller())) {
				Snowman snowman = e.getEntity().getWorld().spawn(e.getEntity().getLocation(), Snowman.class);
				snowman.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(50);
				snowman.setHealth(50);
			}	
		}
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
        		if (LocationUtil.isInCircle(mylocation, p.getLocation(), 10) && getGame().getParticipant(p).hasEffect(Frost.registration)) {
    				ParticleLib.BLOCK_CRACK.spawnParticle(p.getLocation(), 1, 1, 1, 200, 0.5, MaterialX.ICE);
    				SoundLib.BLOCK_GLASS_BREAK.playSound(p.getLocation(), 1, 1.2f);
    				getGame().getParticipant(p).removeEffects(Frost.registration);
    				Damages.damageMagic(p, getPlayer(), false, addDamage);
    				if (getGame().getParticipant(p).hasEffect(SnowflakeMark.registration)) {
    					if (getGame().getParticipant(p).getPrimaryEffect(SnowflakeMark.registration).getLevel() == 1) {
    						SnowflakeMark.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 20, 2);
    					} else if (getGame().getParticipant(p).getPrimaryEffect(SnowflakeMark.registration).getLevel() >= 2) {
    						SnowflakeMark.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 20, 3);
    					}
    				} else {
    					SnowflakeMark.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 20, 1);
    				}
    				addDamage += 2;
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
    		addDamage = 6;
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
    				switch(cast) {
    				case 1: ParticleLib.REDSTONE.spawnParticle(loc, color1);
    				break;
    				case 2: ParticleLib.REDSTONE.spawnParticle(loc, color2);
    				break;
    				case 3: ParticleLib.REDSTONE.spawnParticle(loc, color3);
    				break;
    				case 4: ParticleLib.REDSTONE.spawnParticle(loc, color4);
    				break;
    				case 5: ParticleLib.REDSTONE.spawnParticle(loc, color5);
    				break;
    				case 6: ParticleLib.REDSTONE.spawnParticle(loc, color6);
    				break;
    				case 7: ParticleLib.REDSTONE.spawnParticle(loc, color7);
    				break;
    				case 8: ParticleLib.REDSTONE.spawnParticle(loc, color8);
    				break;
    				case 9: ParticleLib.REDSTONE.spawnParticle(loc, color9);
    				break;
    				case 10: ParticleLib.REDSTONE.spawnParticle(loc, color10);
    				break;
    				case 11: ParticleLib.REDSTONE.spawnParticle(loc, color11);
    				break;
    				case 12: ParticleLib.REDSTONE.spawnParticle(loc, color12);
    				break;
    				}
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
	public void onPlayerMove(PlayerMoveEvent e) {
		if (casting.isRunning() && e.getPlayer().equals(getPlayer()) && cast < 12) getPlayer().setVelocity(zeroV);
	}

	public class Bullet extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final Predicate<Entity> predicate;
		private final Set<Player> attacked = new HashSet<>();

		private int cast;
		private Location lastLocation;
		
		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, int cast) {
			super((int) Math.max(2 + cast / 3, 2));
			setPeriod(TimeUnit.TICKS, 1);
			Yuki.this.bullet = this;
			this.cast = cast;
			this.shooter = shooter;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-2.5, -2.5, -2.5, 2.5, 2.5, 2.5);
			this.forward = arrowVelocity.multiply(2.5);
			this.lastLocation = startLocation;
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
						Damages.damageMagic(p, (Player) shooter, true, cast / 3);
						if (getGame().getParticipant(p).hasEffect(Chill.registration) && !getGame().getParticipant(p).hasEffect(Frost.registration) && !attacked.contains(p) && p != null) {
							attacked.add(p);
							Frost.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 3);
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
				new Bullet(deflectedPlayer, lastLocation, newDirection, 3).start();
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