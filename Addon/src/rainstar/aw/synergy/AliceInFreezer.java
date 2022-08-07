package rainstar.aw.synergy;

import rainstar.aw.effect.Chill;
import rainstar.aw.effect.SnowflakeMark;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Frost;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@AbilityManifest(
		name = "Alice in 냉동고", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §bNonsense§f: 바라보는 방향으로 §3냉동 카드§f를 던져",
		" 적중 대상을 §b빙결§f시키고, §b눈꽃 모양§f으로 다시 퍼져나갑니다.",
		" 추가 발사체는 빙결시킬 때 퍼져나가지 않고 쿨타임을 일부 감소시킵니다.",
		" §3냉동 카드§f로 빙결된 적은 §b눈꽃 표식§f을 최대 §a7§f단계까지 §a1§f단계 획득하고,",
		" 추가 발사체로 빙결될 경우 §9냉기§f 효과를 얻게 됩니다. $[COOLDOWN]"
		})

public class AliceInFreezer extends Synergy implements ActiveHandler {

	public AliceInFreezer(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			synergySettings.new SettingObject<Integer>(AliceInFreezer.class, "cooldown", 17,
            "# 철괴 우클릭 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "카드", CooldownDecrease._50);
	private static final Circle circle1 = Circle.of(7, 6);
	private static final Circle circle2 = Circle.of(7, 8);
	private int number = 1;
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.RIGHT_CLICK && !cool.isCooldown()) {
			new CardBullet(getPlayer(), getPlayer().getEyeLocation().clone().subtract(0, 0.5, 0), getPlayer().getEyeLocation().getDirection().setY(0).normalize(), getPlayer().getLocation().getYaw()).start();
			music.start();
			return cool.start();	
		}
		return false;
	}
	
	private final AbilityTimer music = new AbilityTimer() {

    	@Override
		public void run(int count) {
    		switch(number) {
    		case 1:
    		case 2:
    		case 3:
    		case 4:
    			switch(count) {
    			case 1:
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.F));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.F));	
    				break;
    			case 3: 
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
    				break;
    			case 5: 
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
					break;
    			case 7:	
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
    				break;
    			case 9:
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.7f, Note.natural(0, Tone.F));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.7f, Note.natural(0, Tone.F));
    				break;
    			case 11: 
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.7f, Note.natural(1, Tone.G));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.7f, Note.natural(1, Tone.G));
    				break;
    			case 13: 
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.7f, Note.natural(1, Tone.A));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.7f, Note.natural(1, Tone.A));
					break;
    			case 15:	
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.7f, Note.natural(1, Tone.C));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.7f, Note.natural(1, Tone.C));
					break;
    			case 17:
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.5f, Note.natural(0, Tone.F));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.5f, Note.natural(0, Tone.F));
    				break;
    			case 19: 
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.5f, Note.natural(1, Tone.G));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.5f, Note.natural(1, Tone.G));
    				break;
    			case 21: 
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.5f, Note.natural(1, Tone.A));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.5f, Note.natural(1, Tone.A));
					break;
    			case 23:	
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.5f, Note.natural(1, Tone.C));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.5f, Note.natural(1, Tone.C));
					break;
    			case 25:
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.3f, Note.natural(0, Tone.F));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.3f, Note.natural(0, Tone.F));
    				break;
    			case 27: 
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.3f, Note.natural(1, Tone.G));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.3f, Note.natural(1, Tone.G));
    				break;
    			case 29: 
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.3f, Note.natural(1, Tone.A));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.3f, Note.natural(1, Tone.A));
					break;
    			case 31:	
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), 0.3f, Note.natural(1, Tone.C));
    				SoundLib.BELL.playInstrument(getPlayer().getLocation(), 0.3f, Note.natural(1, Tone.C));
    				stop(false);
    				number++;
					break;
    			}
    		break;
    		case 5:
    		case 7:
    			switch(count) {
    			case 1:
    			case 3:
    			case 7:
    			case 11:
    			case 13:
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.F));
				 	if (count == 11) {
				 		SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
				 	}
    				if (count == 13) {
				 		stop(false);
					 	number++;
				 	}
				 	break;
    			case 5:
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
    				break;
    			case 9:
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.A));
    				break;
    			}
    		break;
    		case 6:
    		case 8:
    			switch(count) {
    			case 1:
    			case 5:
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.F));
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
    				break;
    			case 7:
    			case 11:
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.A));
    				break;
    			case 9:
    			case 13:
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.F));
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
    				break;
    			case 15:
    				SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.F));
    				stop(false);
    				if (number == 8) number = 1;
    				else number++;
    				break;
    			}
    		break;
    		}
    	}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	public class CardBullet extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final Predicate<Entity> predicate;
		private Set<Damageable> checkhit = new HashSet<>();
		private final float yaw;
		private double y;

		private RGB skyblue = RGB.of(0, 254, 254), skywhite = RGB.of(204, 254, 254), blue = RGB.of(153, 204, 254);
		private Location lastLocation;
		
		private final Points CARD1 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, true, false, false, false, false},
			{false, true, false, false, true, false, false, true, false},
			{false, false, true, false, true, false, true, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, false, true, true, true, false, false, false},
			{false, false, true, false, true, false, true, false, false},
			{false, true, false, false, true, false, false, true, false},
			{false, false, false, false, true, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private final Points CARD2 = Points.of(0.08, new boolean[][]{
			{true, true, true, true, true, true, true, true, true},
			{true, false, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, false, true, true, true, true},
			{true, false, true, true, false, true, true, false, true},
			{true, true, false, true, false, true, false, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, true, false, false, false, true, true, true},
			{true, true, false, true, false, true, false, true, true},
			{true, false, true, true, false, true, true, false, true},
			{true, true, true, true, false, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, true, true},
			{true, true, true, true, true, true, true, false, true},
			{true, true, true, true, true, true, true, true, true}
		});
		
		private final Points CARD3 = Points.of(0.08, new boolean[][]{
			{false, false, false, false, false, false, false, false, false},
			{false, true, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, false, false},
			{false, false, false, false, false, false, false, true, false},
			{false, false, false, false, false, false, false, false, false}
		});
		
		private CardBullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, float yaw) {
			super(5);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-1.25, -0.5, -1.25, 1.25, 0.5, 1.25);
			this.forward = arrowVelocity.multiply(3);
			this.lastLocation = startLocation;
			this.yaw = yaw;
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
		protected void onStart() {
			y = lastLocation.getY();
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(shooter.getLocation(), 1, 2f);
			CARD1.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			CARD2.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
			CARD3.rotateAroundAxisY(-yaw).rotateAroundAxis(VectorUtil.rotateAroundAxisY(forward.clone().normalize().setY(0), 90), 90);
		}
		
		@Override
		protected void run(int i) {
			final Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(1.5);
				private final int amount = (int) (vectorBetween.length() / 1.5);
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
				if (type.isSolid()) {
					stop(false);
					return;
				}
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(damageable) && !checkhit.contains(damageable)) {
						checkhit.add(damageable);
						Damages.damageMagic(damageable, getPlayer(), false, 10.5f);
						Frost.apply(getGame(), (LivingEntity) damageable, TimeUnit.TICKS, 70);
						if (damageable instanceof Player) {
							Player p = (Player) damageable;
		    				if (getGame().getParticipant(p).hasEffect(SnowflakeMark.registration)) {
		    					int level = getGame().getParticipant(p).getPrimaryEffect(SnowflakeMark.registration).getLevel();
		    					SnowflakeMark.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 25, Math.min(level + 1, 7));
		    				} else {
		    					SnowflakeMark.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 25, 1);
		    				}
						}
						Random random = new Random();
						if (random.nextBoolean()) {
							for (Location loc : circle1.toLocations(entity.getLocation()).floor(entity.getLocation().getY())) {
								loc.setY(y);
								new Bullet(shooter, entity.getLocation(), loc).start();
							}	
						} else {
							for (Location loc : circle2.toLocations(entity.getLocation()).floor(entity.getLocation().getY())) {
								loc.setY(y);
								new Bullet(shooter, entity.getLocation(), loc).start();
							}
						}
						cool.setCount((int) Math.max(cool.getCount() * 0.25, cool.getCount() - 2));	
						stop(false);
					}
				}
				for (Location loc : CARD1.toLocations(entity.getLocation())) {
					ParticleLib.REDSTONE.spawnParticle(loc, skyblue);
				}
				for (Location loc : CARD2.toLocations(entity.getLocation())) {
					ParticleLib.REDSTONE.spawnParticle(loc, skywhite);
				}
				for (Location loc : CARD3.toLocations(entity.getLocation())) {
					ParticleLib.REDSTONE.spawnParticle(loc, blue);
				}
			}
			lastLocation = newLocation;
		}
		
		@Override
		protected void onEnd() {
			entity.remove();
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
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
				new CardBullet(deflectedPlayer, lastLocation, newDirection, (yaw - 180)).start();
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
	
	public class Bullet extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Predicate<Entity> predicate;
		private Set<LivingEntity> damaged = new HashSet<>();

		private int stacks = 0;
		private boolean turns = true;
		
		private final Location endlocation;

		private RGB shootgradation;
		
		private final RGB shootgradation1 = RGB.of(40, 254, 254), shootgradation2 = RGB.of(72, 254, 254), shootgradation3 = RGB.of(104, 254, 254),
				shootgradation4 = RGB.of(138, 254, 254), shootgradation5 = RGB.of(172, 254, 254), shootgradation6 = RGB.of(204, 254, 254),
				shootgradation7 = RGB.of(214, 251, 254), shootgradation8 = RGB.of(224, 247, 253), shootgradation9 = RGB.of(234, 243, 252),
				shootgradation10 = RGB.of(245, 239, 251), shootgradation11 = RGB.of(254, 235, 250);
		
		@SuppressWarnings("serial")
		private List<RGB> shootgradations = new ArrayList<RGB>(){{
			add(shootgradation1);
			add(shootgradation2);
			add(shootgradation3);
			add(shootgradation4);
			add(shootgradation5);
			add(shootgradation6);
			add(shootgradation7);
			add(shootgradation8);
			add(shootgradation9);
			add(shootgradation10);
			add(shootgradation11);
		}};
		
		private Location lastLocation;
		
		private Bullet(LivingEntity shooter, Location startLocation, Location endLocation) {
			super(10);
			setPeriod(TimeUnit.TICKS, 1);
			this.shooter = shooter;
			this.entity = new ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.lastLocation = startLocation;
			this.endlocation = endLocation;
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
			final Location newLocation = lastLocation.clone().add(endlocation.clone().subtract(lastLocation.clone()).toVector().normalize().multiply(1.2));
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
				for (LivingEntity livingEntity : LocationUtil.getConflictingEntities(LivingEntity.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(livingEntity) && !damaged.contains(livingEntity)) {
						Damages.damageArrow(livingEntity, shooter, 7);
						if (livingEntity instanceof Player) {
							Player player = (Player) livingEntity;
							if (!getGame().getParticipant(player).hasEffect(Frost.registration)) {
								Chill.apply(getGame().getParticipant((Player) livingEntity), TimeUnit.TICKS, 300);
								cool.setCount((int) Math.max(cool.getCount() * 0.25, cool.getCount() - 2));	
							}
						}
						Frost.apply(getGame(), livingEntity, TimeUnit.TICKS, 40);
						damaged.add(livingEntity);
					}
				}
				if (turns) stacks++;
				else stacks--;
				if (stacks % (shootgradations.size() - 1) == 0) {
					turns = !turns;
				}
				shootgradation = shootgradations.get(stacks);
				ParticleLib.REDSTONE.spawnParticle(location, shootgradation);
			}
			lastLocation = newLocation;
		}
		
		@Override
		protected void onEnd() {
			entity.remove();
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
		}

		public class ArrowEntity extends CustomEntity {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
			}

		}
		
	}
	
}