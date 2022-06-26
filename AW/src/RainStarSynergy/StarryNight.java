package RainStarSynergy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableMap;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(
		name = "별이 빛나는 밤", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7표식 §8- §e빛§f: 적에게 원거리 공격 시 §e달빛 표식§f을, 근거리 공격 시",
		" §e별빛 표식§f을 부여합니다. §e달빛 표식§f은 적을 끌어당기고,",
		" §e별빛 표식§f은 적을 중심으로 4칸 내 생명체를 끌어옵니다.",
		" 아무 표식 4개를 쌓으면 표식을 터뜨려 밤이 빠르게 다가오고,",
		" §8(§7달 표식 × $[STUN]§8)§f초간 기절시키며 §8(§7별 표식 × $[DAMAGE_INCREASE]§8)§f%의",
		" 추가 피해를 입힙니다. 같은 표식으로만 4개일 경우엔 6개로 취급합니다.",
		"§7패시브 §8- §3밤하늘§f: 밤에는 무작위 표식을 하나 더 추가해줍니다.",
		" 유성우 스킬을 사용할 수 있습니다.",
		"§7검 들고 F §8- §b유성우§f: 바라보는 방향으로 별빛 검기를 날립니다.",
		" 검기는 1초간 나아가며 §a근거리 피해§f를 입힙니다. $[COOLDOWN]",
		" §7스킬 재사용§f시, 검기가 가장 가까운 대상에게 선회하고 ",
		" 2초간 더 날아가며 §b원거리 피해§f를 입힙니다."
		})

public class StarryNight extends Synergy {
	
	public StarryNight(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN =
			synergySettings.new SettingObject<Integer>(StarryNight.class, "cooldown", 12, 
					"# 쿨타임", "# 주의! 쿨타임 감소를 33%까지만 받습니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Double> STACK_COOL = 
			synergySettings.new SettingObject<Double>(StarryNight.class, "stack-cooldown", 1.0,
            "# 표식을 쌓을 때 내부 쿨타임") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> DAMAGE_INCREASE = 
			synergySettings.new SettingObject<Integer>(StarryNight.class, "damage-increase", 15,
            "# 별 스택 당 공격력 증가 수치") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Double> STUN = 
			synergySettings.new SettingObject<Double>(StarryNight.class, "stun-duration", 0.3,
            "# 달 스택 당 기절 지속시간 증가") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
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
	
	private static final ImmutableMap<Boolean, String> type = ImmutableMap.<Boolean, String>builder()
			.put(true, "§e✭")
			.put(false, "§e🌙")
			.build();
	
	private static boolean isNight(long time) {
		return time > 12300 && time < 23850;
	}

	private void updateTime(World world) {
		final long diff = 15000 - world.getTime();
		if (diff < 0) {
			if (-diff > 2000) world.setTime(world.getTime() - 1500);
			else world.setTime(15000);
		} else if (diff > 0) {
			if (diff > 2000) world.setTime(world.getTime() + 1500);
			else world.setTime(15000);
		}
	}
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), 33);
	private static final Set<Material> swords;
	private Bullet bullet = null;
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	private Random random = new Random();
	private final int stun = (int) (STUN.getValue() * 20);
	private final double increase = DAMAGE_INCREASE.getValue() * 0.01;
	private final Map<Player, Stack> stackMap = new HashMap<>();
	private final double stackcool = STACK_COOL.getValue();
	private static final RGB COLOR = RGB.of(235, 200, 21);
	private static final Crescent crescent = Crescent.of(1, 20);
	private int particleSide = 45;
	private static final Note[] NOTES = {
			Note.natural(0, Tone.E),
			Note.sharp(1, Tone.G),
			Note.natural(1, Tone.A),
			Note.sharp(1, Tone.C)
	};
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
    		if (bullet != null) {
    			if (!bullet.changed) {
    				bullet.setCount(bullet.getCount() + 20);
    				bullet.changed = true;
    	    		if (ServerVersion.getVersion() >= 13) {
    	    			BlockData diamond = MaterialX.DIAMOND_BLOCK.getMaterial().createBlockData();
    	    			ParticleLib.FALLING_DUST.spawnParticle(bullet.lastLocation.clone(), 0.1, 0.1, 0.1, 15, 0, diamond);
    	    		} else {
    	    			ParticleLib.FALLING_DUST.spawnParticle(bullet.lastLocation.clone(), 0.1, 0.1, 0.1, 15, 0, new MaterialData(Material.DIAMOND_BLOCK));
    	    		}
    	    		bullet.forward = VectorUtil.validateVector(LocationUtil.getNearestEntity(Player.class, bullet.lastLocation, predicate).getLocation().toVector().subtract((bullet.lastLocation.toVector()))).normalize().multiply(2.5);
    				SoundLib.BLOCK_END_PORTAL_FRAME_FILL.playSound(bullet.lastLocation, 1.5f, 1.35f);
    			}
        	} else if (!cooldown.isCooldown()) {
        		final ItemStack mainHand = getPlayer().getInventory().getItemInMainHand();
				new Bullet(getPlayer(), getPlayer().getLocation().clone().add(0, 1.5, 0), getPlayer().getLocation().getDirection().multiply(.4), mainHand.getEnchantmentLevel(Enchantment.DAMAGE_ALL), getPlayer().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue()).start();
        	}	
    		e.setCancelled(true);
    	}
    }
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().hasMetadata("StarFirework") || e.getDamager().hasMetadata("MoonFirework")) {
			e.setCancelled(true);
		}
		Participant target;
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player
				&& !e.isCancelled() && predicate.test(e.getEntity())) {
			target = getGame().getParticipant((Player) e.getEntity());
			if (stackMap.containsKey(e.getEntity())) {
				if (stackMap.get(e.getEntity()).getCount() < (300 - (stackcool * 20))) {
					if (stackMap.get(e.getEntity()).addStack(true)) {
						int starstack = 0;
						int moonstack = 0;
						for (Boolean booleans : stackMap.get(e.getEntity()).stacks) {
							if (booleans) starstack++;
							else moonstack++;
						}
						if (starstack == 4) starstack = 6;
						if (moonstack == 4) moonstack = 6;
						if (isNight(getPlayer().getWorld().getTime())) {
							if (random.nextBoolean()) {
								getPlayer().sendMessage("§6[§e!§6] §b별 §f스택 추가!");
								starstack++;
							} else {
								getPlayer().sendMessage("§3[§b!§3] §e달 §f스택 추가!");
								moonstack++;
							}
						}
						e.setDamage(e.getDamage() * (starstack * increase));
						Stun.apply(target, TimeUnit.TICKS, (moonstack * stun));
						final Firework firework = getPlayer().getWorld().spawn(((Player) e.getEntity()).getEyeLocation(), Firework.class);
						final FireworkMeta meta = firework.getFireworkMeta();
						meta.addEffect(
								FireworkEffect.builder()
										.withColor(Color.fromRGB(254, 254, 108), Color.fromRGB(72, 254, 254))
										.with(Type.STAR)
										.build()
						);
						meta.setPower(0);
						firework.setFireworkMeta(meta);
						firework.setMetadata("StarFirework", NULL_VALUE);
						new SoundTimer(getPlayer(), true).start();
						new BukkitRunnable() {
							@Override
							public void run() {
								firework.detonate();
							}
						}.runTaskLater(AbilityWar.getPlugin(), 1L);
						updateTime(getPlayer().getWorld());
						stackMap.get(e.getEntity()).stop(false);
					} else {
						Location loc = target.getPlayer().getLocation();
						for (int iteration = 0; iteration < 5; iteration++) {
							double angle = Math.toRadians(72.0D * iteration);
							double nextAngle = Math.toRadians(72.0D * (iteration + 2));
							double x = Math.cos(angle) * 1.5D;
							double z = Math.sin(angle) * 1.5D;
							double x2 = Math.cos(nextAngle) * 1.5D;
							double z2 = Math.sin(nextAngle) * 1.5D;
							double deltaX = x2 - x;
							double deltaZ = z2 - z;
							for (double d = 0.0D; d < 1.0D; d += 0.125D) {
								loc.add(x + deltaX * d, 0.0D, z + deltaZ * d);
								ParticleLib.REDSTONE.spawnParticle(loc, COLOR);
								loc.subtract(x + deltaX * d, 0.0D, z + deltaZ * d);
							}
						}
					}
				}
			} else {
				new Stack((Player) e.getEntity(), true).start();
				Location loc = target.getPlayer().getLocation();
				for (int iteration = 0; iteration < 5; iteration++) {
					double angle = Math.toRadians(72.0D * iteration);
					double nextAngle = Math.toRadians(72.0D * (iteration + 2));
					double x = Math.cos(angle) * 1.5D;
					double z = Math.sin(angle) * 1.5D;
					double x2 = Math.cos(nextAngle) * 1.5D;
					double z2 = Math.sin(nextAngle) * 1.5D;
					double deltaX = x2 - x;
					double deltaZ = z2 - z;
					for (double d = 0.0D; d < 1.0D; d += 0.125D) {
						loc.add(x + deltaX * d, 0.0D, z + deltaZ * d);
						ParticleLib.REDSTONE.spawnParticle(loc, COLOR);
						loc.subtract(x + deltaX * d, 0.0D, z + deltaZ * d);
					}
				}
			}
		}
		if (NMS.isArrow(e.getDamager()) && e.getEntity() instanceof Player
				&& !e.isCancelled() && predicate.test(e.getEntity())) {
			Arrow arrow = (Arrow) e.getDamager();
			if (getPlayer().equals(arrow.getShooter())) {
				target = getGame().getParticipant((Player) e.getEntity());
				if (stackMap.containsKey(e.getEntity())) {
					if (stackMap.get(e.getEntity()).getCount() < (300 - (stackcool * 20))) {
						if (stackMap.get(e.getEntity()).addStack(false)) {
							int starstack = 0;
							int moonstack = 0;
							for (Boolean booleans : stackMap.get(e.getEntity()).stacks) {
								if (booleans) starstack++;
								else moonstack++;
							}
							if (starstack == 4) starstack = 6;
							if (moonstack == 4) moonstack = 6;
							if (isNight(getPlayer().getWorld().getTime())) {
								if (random.nextBoolean()) {
									getPlayer().sendMessage("§6[§e!§6] §b별 §f스택 추가!");
									starstack++;
								} else {
									getPlayer().sendMessage("§3[§b!§3] §e달 §f스택 추가!");
									moonstack++;
								}
							}
							e.setDamage(e.getDamage() * (starstack * increase));
							Stun.apply(target, TimeUnit.TICKS, (moonstack * stun));
							final Firework firework = getPlayer().getWorld().spawn(((Player) e.getEntity()).getEyeLocation(), Firework.class);
							final FireworkMeta meta = firework.getFireworkMeta();
							meta.addEffect(
									FireworkEffect.builder()
											.withColor(Color.fromRGB(254, 254, 108), Color.fromRGB(72, 254, 254))
											.with(Type.BALL_LARGE)
											.build()
							);
							meta.setPower(0);
							firework.setFireworkMeta(meta);
							firework.setMetadata("MoonFirework", NULL_VALUE);
							new SoundTimer(getPlayer(), false).start();
							new BukkitRunnable() {
								@Override
								public void run() {
									firework.detonate();
								}
							}.runTaskLater(AbilityWar.getPlugin(), 1L);
							updateTime(getPlayer().getWorld());
							stackMap.get(e.getEntity()).stop(false);
						} else {
							new CutParticle(particleSide).start();
							particleSide *= -1;
						}
					}
				} else {
					new CutParticle(particleSide).start();
					particleSide *= -1;
					new Stack((Player) e.getEntity(), false).start();
				}
			}
		}
	}
	
	private class SoundTimer extends AbilityTimer {

		private final Player center;
		private final boolean type;

		private SoundTimer(Player center, boolean type) {
			super(TaskType.NORMAL, NOTES.length);
			setPeriod(TimeUnit.TICKS, 3);
			this.type = type;
			this.center = center;
		}

		@Override
		protected void run(int count) {
			SoundLib.BELL.playInstrument(center.getLocation(), .4f, type ? NOTES[4 - count] : NOTES[count - 1]);
		}

	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		private List<Boolean> stacks = new ArrayList<>();
		
		private Stack(Player player, boolean stacktype) {
			super(20);
			setPeriod(TimeUnit.TICKS, 4);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					"§7????");
			hologram.display(getPlayer());
			stackMap.put(player, this);
			addStack(stacktype);
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
		}

		private boolean addStack(boolean stacktype) {
			setCount(300);
			if (stacktype) {
				for (LivingEntity entity : LocationUtil.getNearbyEntities(LivingEntity.class, player.getLocation(), 4, 4, predicate)) {
					entity.setVelocity(player.getLocation().toVector().subtract(entity.getLocation().toVector()).multiply(0.75));
				}
			} else player.setVelocity(getPlayer().getLocation().toVector().subtract(player.getLocation().toVector()).multiply(0.6).setY(0));
			stack++;
			stacks.add(stacktype);
			String string = "";
			for (Boolean booleans : stacks) {
				string = string + type.get(booleans);
			}
			hologram.setText(string + Strings.repeat("§7?", 4 - stack));
			if (stack >= 4) {
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
			stackMap.remove(player);
		}
		
	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector axis;
		private final Vector vector;
		private final Vectors crescentVectors;

		private CutParticle(double angle) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.axis = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(getPlayer().getLocation().getDirection().setY(0).normalize(), 90), getPlayer().getLocation().getDirection().setY(0).normalize(), angle);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis, -75);
		}

		@Override
		protected void run(int count) {
			Location baseLoc = getPlayer().getLocation().clone().add(vector).add(0, 1.3, 0);
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				ParticleLib.REDSTONE.spawnParticle(loc, COLOR);
			}
			crescentVectors.rotateAroundAxis(axis, 40);
		}

	}
	
	public class Bullet extends AbilityTimer {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private Vector forward;
		private final int sharpnessEnchant;
		private final double damage;
		private final Predicate<Entity> predicate;
		private Set<LivingEntity> hitEntity = new HashSet<>();
		private boolean changed = false;
		
		private Location lastLocation;
		
		private int stacks = 0;
		private boolean turns = true;
		private RGB gradation;

		private final RGB gradation1 = RGB.of(3, 212, 168), gradation2 = RGB.of(8, 212, 178),
				gradation3 = RGB.of(15, 213, 190), gradation4 = RGB.of(18, 211, 198), gradation5 = RGB.of(27, 214, 213),
				gradation6 = RGB.of(29, 210, 220), gradation7 = RGB.of(30, 207, 225), gradation8 = RGB.of(24, 196, 223),
				gradation9 = RGB.of(23, 191, 226), gradation10 = RGB.of(19, 182, 226),
				gradation11 = RGB.of(16, 174, 227), gradation12 = RGB.of(13, 166, 228),
				gradation13 = RGB.of(10, 159, 228), gradation14 = RGB.of(7, 151, 229),
				gradation15 = RGB.of(3, 143, 229), gradation16 = RGB.of(1, 135, 230), gradation17 = RGB.of(1, 126, 222),
				gradation18 = RGB.of(1, 118, 214), gradation19 = RGB.of(1, 109, 207), gradation20 = RGB.of(1, 101, 199),
				gradation21 = RGB.of(1, 92, 191);
		
		private List<RGB> gradations = new ArrayList<RGB>() {
			{
				add(gradation1);
				add(gradation2);
				add(gradation3);
				add(gradation4);
				add(gradation5);
				add(gradation6);
				add(gradation7);
				add(gradation8);
				add(gradation9);
				add(gradation10);
				add(gradation11);
				add(gradation12);
				add(gradation13);
				add(gradation14);
				add(gradation15);
				add(gradation16);
				add(gradation17);
				add(gradation18);
				add(gradation19);
				add(gradation20);
				add(gradation21);
			}
		};

		private Bullet(LivingEntity shooter, Location startLocation, Vector arrowVelocity, int sharpnessEnchant, double damage) {
			super(20);
			setPeriod(TimeUnit.TICKS, 1);
			StarryNight.this.bullet = this;
			this.shooter = shooter;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-1.2, -1.2, -1.2, 1.2, 1.2, 1.2);
			this.forward = arrowVelocity.multiply(2.5);
			this.sharpnessEnchant = sharpnessEnchant;
			this.damage = damage;
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (entity.equals(shooter)) return false;
					if (hitEntity.contains(entity)) return false;
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
			SoundLib.BLOCK_END_PORTAL_FRAME_FILL.playSound(shooter.getLocation(), 1.5f, 0.65f);
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
				if (turns)
					stacks++;
				else
					stacks--;
				if (stacks % (gradations.size() - 1) == 0) {
					turns = !turns;
				}
				gradation = gradations.get(stacks);
				final Location location = iterator.next();
				entity.setLocation(location);
				if (!isRunning()) {
					return;
				}
				for (LivingEntity livingEntity : LocationUtil.getConflictingEntities(LivingEntity.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(livingEntity)) {
						if (changed) Damages.damageArrow(livingEntity, getPlayer(), (float) (EnchantLib.getDamageWithSharpnessEnchantment(damage, sharpnessEnchant) * 1.3));
						else livingEntity.damage((float) (EnchantLib.getDamageWithSharpnessEnchantment(damage, sharpnessEnchant)) * 1.3, getPlayer());
						hitEntity.add(livingEntity);
					}
				}
				ParticleLib.REDSTONE.spawnParticle(location, gradation);
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			StarryNight.this.bullet = null;
			entity.remove();
			cooldown.start();
		}

		@Override
		protected void onSilentEnd() {
			StarryNight.this.bullet = null;
			entity.remove();
			cooldown.start();
		}

		public class ArrowEntity extends CustomEntity {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			protected void onRemove() {
				Bullet.this.stop(false);
			}

		}

	}

}
