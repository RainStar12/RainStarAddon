package rainstar.abilitywar.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Note.Tone;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Frost;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.LocationUtil.Locations;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.block.Blocks;
import daybreak.abilitywar.utils.base.minecraft.block.IBlockSnapshot;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.item.Skulls;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.BlockX;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.ItemLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import rainstar.abilitywar.effect.Chill;
import rainstar.abilitywar.effect.FrozenHeart;
import rainstar.abilitywar.effect.SnowflakeMark;

@AbilityManifest(
		name = "펭귄", rank = Rank.S, species = Species.ANIMAL, explain = {
		"§7철괴 좌클릭 §8- §b얼음 슬라이딩§f: 바라보는 방향 10칸으로 §b얼음 슬라이딩§f을 합니다.",
		" §b슬라이딩§f 도중엔 §3무적, 타게팅 불능§f이 됩니다. $[COOLDOWN]",
		" 적에게 부딪히면 피해를 줍니다. 피해는 대상이 §b빙결된 횟수§f에 비례합니다.",
		"§7눈 던지기 §8- §b얼음땡§f: 매 §d1.9초§f마다 §9눈덩이§f를 2개씩 획득하고,",
		" §9눈§f을 던져 맞은 대상은 점점 추위에 걸리다 §d스택§f을 다 쌓으면 §3얼립니다§f.",
		" 한 번 언 대상은 $[STACK_COOL]초간 §d스택§f이 쌓이지 않습니다.",
		" 10% 확률로 §9눈덩이§f에 돌이 들어갔을 수도 있습니다...",
		"§7패시브 §8- §b추위 면역§f: 얼음계 상태이상 효과를 절반만 받고",
		" 받을 때마다 체력을 $[HEAL_AMOUNT]만큼 §d회복§f합니다.",
		"§b[§7아이디어 제공자§b] §dDDony"
		},
		summarize = {
		"§7철괴 좌클릭으로§f 바라보는 방향으로 §b얼음 슬라이딩§f을 합니다.",
		"§b슬라이딩§f 중에는 타게팅 불가, 무적이며 적에게 부딪혀 피해를 줍니다.",
		"피해량은 대상이 빙결 상태이상을 받은 횟수에 비례합니다.",
		"일정 주기로 눈덩이를 획득해 눈을 던져 점점 추워지게 하다 얼립니다.",
		"얼음계 상태이상 효과를 절반만 받고 체력을 회복합니다."
		})

public class Penguin extends AbilityBase implements ActiveHandler {

	public Penguin(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(Penguin.class,
			"cooldown", 15, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> HEAL_AMOUNT = abilitySettings.new SettingObject<Integer>(Penguin.class,
			"heal-amount", 5, "# 추위 면역 체력 회복량") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DAMAGE = abilitySettings.new SettingObject<Integer>(Penguin.class,
			"damage", 10, "# 충돌 피해량") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> STACK_COOL = abilitySettings.new SettingObject<Integer>(Penguin.class,
			"stack-cooldown", 7, "# 각 플레이어별 스택 쿨타임 (단위: 초)") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> BASE_STACK_COOL = abilitySettings.new SettingObject<Integer>(Penguin.class,
			"base-stack-cooldown", 7, "# 기본 스택 쿨타임 (단위: 틱)", "# 1초 = 20틱, 1틱 = 0.05초") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (onetimeEntity.contains(entity)) return false;
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
	
	@Override
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	passive.start();
	    }
	}
	
	private Random random = new Random();
	
	private static final double radians = Math.toRadians(90);
	private static final Vector zeroV = new Vector(0, 0, 0);
	
	private Map<Player, Integer> froststack = new HashMap<>();
	private Set<LivingEntity> onetimeEntity = new HashSet<>();
	private final Map<Block, IBlockSnapshot> ices = new HashMap<>();
	
	private Material snowballMaterial = MaterialX.SNOWBALL.getMaterial();
	
	private final Set<Player> penguinspin = new HashSet<>();
	private final Set<Player> stackcoolSet = new HashSet<>();
	private final Map<Player, Stack> stackMap = new HashMap<>();
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	private final int healamount = HEAL_AMOUNT.getValue();
	private final int damage = DAMAGE.getValue();
	private final int basestackcool = BASE_STACK_COOL.getValue();
	private int soundstack = 0;
	
	private final static Color sky = Color.fromRGB(72, 254, 254), mint = Color.fromRGB(236, 254, 254), snow = Color.fromRGB(28, 254, 243),
			magenta = Color.fromRGB(254, 85, 140), pink = Color.fromRGB(254, 174, 201), sakura = Color.fromRGB(254, 236, 242),
			teal = Color.TEAL;
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._50);
	private final Duration skill = new Duration(20, cool) {
		
		Vector godirection;
		
		@Override
		protected void onDurationStart() {
			final World world = getPlayer().getWorld();
			final Location playerLocation = getPlayer().getLocation();
			Vector direction = playerLocation.getDirection().clone().normalize();
			godirection = direction.setY(0).normalize();
			Locations locations = new Locations();
			for (Vector vector : Line.between(playerLocation, playerLocation.clone().add(direction), 2)) {
				final double originX = vector.getX();
				final double originZ = vector.getZ();
				locations.add(playerLocation.clone().add(vector.clone()
						.setX(rotateX(originX, originZ, radians))
						.setZ(rotateZ(originX, originZ, radians))));
				locations.add(playerLocation.clone().add(vector.clone()
						.setX(rotateX(originX, originZ, radians * 3))
						.setZ(rotateZ(originX, originZ, radians * 3))));
			}
			direction.multiply(0.75);
			getPlayer().teleport(LocationUtil.floorY(getPlayer().getLocation()));
			new AbilityTimer(20) {
				Set<String> set = new HashSet<>();

				@Override
				protected void run(int count) {
					MaterialX material;
					if (random.nextBoolean()) {
						material = MaterialX.ICE;
					} else {
						material = MaterialX.PACKED_ICE;
					}
					
					locations.add(direction);
					for (Location location : locations) {
						if (set.add(location.getBlockX() + ":" + location.getBlockZ())) {
							final Block block = world.getBlockAt(
									location.getBlockX(),
									LocationUtil.getFloorYAt(world, playerLocation.getY(), location.getBlockX(), location.getBlockZ()) - 1,
									location.getBlockZ()
							);
							if (!ices.containsKey(block)) {
								ices.put(block, Blocks.createSnapshot(block));
								BlockX.setType(block, material);
							}
						}
					}
					if (isBlockObstructing()) {
						this.stop(false);
					}
				}
			}.setPeriod(TimeUnit.TICKS, 1).start();
		}

		@Override
		protected void onDurationProcess(int seconds) {
			SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer().getLocation());
			getPlayer().setVelocity(godirection.clone().multiply(1.25));
			getPlayer().setGliding(true);	
			float yaw = LocationUtil.getYaw(godirection), pitch = LocationUtil.getPitch(godirection);
			for (Player player : Bukkit.getOnlinePlayers()) {
			    NMS.rotateHead(player, getPlayer(), yaw, pitch);	
			}
			for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, getPlayer().getLocation(), 2, 2, predicate)) {
				if (!onetimeEntity.contains(livingEntity)) onetimeEntity.add(livingEntity);
				if (!froststack.containsKey(livingEntity)) livingEntity.damage(damage, getPlayer());
				else livingEntity.damage(damage * (1 + (froststack.get(livingEntity) * 0.1)), getPlayer());
				if (livingEntity instanceof Player) {
					Player p = (Player) livingEntity;
					if (!stackcoolSet.contains(p) && !penguinspin.contains(p)) {
						if (stackMap.containsKey(p)) {
							if (stackMap.get(p).addDoubleStack()) {
								new PenguinSpin(p).start();
							}
						} else new Stack(p).start();	
					}
				}
			}
			getParticipant().attributes().TARGETABLE.setValue(false);
			
			if (isBlockObstructing()) {
				this.stop(false);
			}
		}

		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}

		@Override
		protected void onDurationSilentEnd() {
			for (IBlockSnapshot snapshot : ices.values()) {
				snapshot.apply();
			}
			getPlayer().setVelocity(zeroV);
			ices.clear();
			getParticipant().attributes().TARGETABLE.setValue(true);
			onetimeEntity.clear();
		}
		
		private boolean isBlockObstructing() {
			final Location playerLocation = getPlayer().getLocation().clone();
			final Vector direction = playerLocation.getDirection().setY(0).normalize().multiply(.75);
			return isBlockObstructing(playerLocation, direction)
					|| isBlockObstructing(playerLocation, VectorUtil.rotateAroundAxisY(direction.clone(), 45))
					|| isBlockObstructing(playerLocation, VectorUtil.rotateAroundAxisY(direction.clone(), -45));
		}

		private boolean isBlockObstructing(Location location, Vector direction) {
			final Location front = location.clone().add(direction);
			final WorldBorder worldBorder = getPlayer().getWorld().getWorldBorder();
			return checkBlock(front.getBlock()) || checkBlock(front.clone().add(0, 1, 0).getBlock()) || (worldBorder.isInside(location) && !worldBorder.isInside(front));
		}

		private boolean checkBlock(final Block block) {
			return !block.isEmpty() && block.getType().isSolid();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	private final AbilityTimer stackminicool = new AbilityTimer(basestackcool) {
	
		@Override
		public void run(int count) {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (!getPlayer().getInventory().contains(snowballMaterial, 64)) {
				Material m = getPlayer().getLocation().getBlock().getType();
				Material bm = getPlayer().getLocation().clone().subtract(0, 1, 0).getBlock().getType();
				if (m.equals(Material.SNOW) || bm.equals(Material.SNOW) || bm.equals(Material.SNOW_BLOCK) || bm.equals(Material.ICE) || bm.equals(Material.PACKED_ICE)) {
					if (count % 19 == 0) 
						ItemLib.addItem(getPlayer().getInventory(), snowballMaterial, 2);
				} else {
					if (count % 38 == 0) {
						ItemLib.addItem(getPlayer().getInventory(), snowballMaterial, 2);
					}	
				}	
			}
		}
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (skill.isRunning() && e.getEntity().equals(getPlayer())) {
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
		if (e.getDamager().hasMetadata("StarFirework")) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (skill.isRunning() && e.getPlayer().equals(getPlayer())) {
			ParticleLib.BLOCK_CRACK.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 7, 0.75, MaterialX.ICE);
		}
	}
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.getEntity() instanceof Snowball && e.getHitEntity() instanceof Player) {
			if (getPlayer().equals(e.getEntity().getShooter())) {
				Player p = (Player) e.getHitEntity();
				if (!stackcoolSet.contains(p) && !penguinspin.contains(p) && !stackminicool.isRunning()) {
					if (predicate.test(p)) {
						if (stackMap.containsKey(p)) {
							if (stackMap.get(p).addStack()) {
								new PenguinSpin(p).start();
							}
						} else new Stack(p).start();
						if (random.nextInt(5) == 0) {
							Damages.damageArrow(p, getPlayer(), 6);
						}
						stackminicool.start();
					}	
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			if (e.getEffectType().equals(Frost.registration) || e.getEffectType().equals(Chill.registration) || 
					e.getEffectType().equals(FrozenHeart.registration) || e.getEffectType().equals(SnowflakeMark.registration)) {
				e.setDuration(TimeUnit.TICKS, (int) (e.getDuration() * 0.5));
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), healamount, RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					Healths.setHealth(getPlayer(), getPlayer().getHealth() + healamount);
				}
				SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer().getLocation(), 1, 2);
			}	
		} else {
			if (e.getEffectType().equals(Frost.registration)) {
				if (froststack.containsKey(e.getPlayer())) froststack.put(e.getPlayer(), froststack.get(e.getPlayer()) + 1);
				else froststack.put(e.getPlayer(), 1);
			}
		}
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT) {
			if (clickType == ClickType.LEFT_CLICK && !skill.isDuration() && !cool.isCooldown()) {
				return skill.start();
			}
		}
		return false;
	}
	
	private double rotateX(double x, double z, double radians) {
		return (x * FastMath.cos(radians)) + (z * FastMath.sin(radians));
	}

	private double rotateZ(double x, double z, double radians) {
		return (-x * FastMath.sin(radians)) + (z * FastMath.cos(radians));
	}

	private class PenguinSpin extends AbilityTimer {
		
		private boolean changed = false;
		private final Player player;
		private final ArmorStand[] penguins = new ArmorStand[5];
		
		private PenguinSpin(Player player) {
			super(30);
			setPeriod(TimeUnit.TICKS, 1);
			this.player = player;
			penguinspin.add(player);
			for (int a = 0; a < 5; a++) {
				penguins[a] = player.getWorld().spawn(player.getLocation().clone(), ArmorStand.class);
				penguins[a].setVisible(false);
				penguins[a].setInvulnerable(true);
				penguins[a].setSmall(true);
				NMS.removeBoundingBox(penguins[a]);
				
				ItemStack penguin_head = Skulls.createCustomSkull("d60d04d29367df0c1cac1da4041042238856df53329a535a1f08dbe96ef4a485");
				ItemStack ddony_head = Skulls.createSkull("DDony");
				
				EntityEquipment equipment = penguins[a].getEquipment();
				
				if (!changed) {
					if (getPlayer().getName().equals("DDony")) {
						equipment.setHelmet(ddony_head);
						changed = true;
					} else {
						if (random.nextInt(50) == 0) {
							equipment.setHelmet(ddony_head);
							changed = true;
						} else equipment.setHelmet(penguin_head);
					}
				} else equipment.setHelmet(penguin_head);
			}
		}
		
		@Override
		public void run(int count) {
			for (int iteration = 0; iteration < 5; iteration++) {
                double angle = Math.toRadians(72.0D * iteration + (count * 3));
                double x = Math.cos(angle);
                double z = Math.sin(angle);
                Vector direction = player.getEyeLocation().toVector().subtract(penguins[iteration].getEyeLocation().toVector());
                penguins[iteration].teleport(player.getLocation().clone().add(x, -0.5, z).setDirection(direction));            
            }
		}
		
		@Override
		public void onEnd() {
			for (ArmorStand stand : penguins) {
				stand.remove();
			}
			final Firework firework = getPlayer().getWorld().spawn(player.getLocation(), Firework.class);
			final FireworkMeta meta = firework.getFireworkMeta();
			if (changed) {
				switch(random.nextInt(10)) {
				case 0:
				case 1:
					meta.addEffect(
							FireworkEffect.builder()
							.withColor(magenta, pink, sakura)
							.with(Type.BURST)
							.build()
					);
					break;
				case 2:
				case 3:
					meta.addEffect(
							FireworkEffect.builder()
							.withColor(magenta, pink, sakura, mint, sky, snow, teal)
							.with(Type.BURST)
							.build()
					);
					break;
				case 4:
				case 5:
				case 6:
				case 7:
				case 8:
				case 9:
					meta.addEffect(
							FireworkEffect.builder()
							.withColor(mint, sky, snow, teal)
							.with(Type.BURST)
							.build()
					);
					break;
				}
			} else {
				meta.addEffect(
					FireworkEffect.builder()
						.withColor(mint, sky, snow, teal)
						.with(Type.BURST)
						.build()
				);	
			}
			meta.setPower(0);
			firework.setFireworkMeta(meta);
			firework.setMetadata("StarFirework", NULL_VALUE);
			new BukkitRunnable() {
				@Override
				public void run() {
					firework.detonate();
				}
			}.runTaskLater(AbilityWar.getPlugin(), 1L);
			
			new BukkitRunnable() {
				@Override
				public void run() {
					Frost.apply(getGame().getParticipant(player), TimeUnit.TICKS, 60);
				}
			}.runTaskLater(AbilityWar.getPlugin(), 2L);
			
			penguinspin.remove(player);
			if (!stackcoolSet.contains(player)) new StackCool(player).start();
		}
		
		@Override
		public void onSilentEnd() {
			onEnd();
		}
		
	}
	
	private class StackCool extends AbilityTimer {
		
		private Player player;
		
		private StackCool(Player player) {
			super(STACK_COOL.getValue() * 20);
			setPeriod(TimeUnit.TICKS, 1);
			stackcoolSet.add(player);
			this.player = player;
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			stackcoolSet.remove(player);
		}
		
	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		
		private Stack(Player player) {
			super(15);
			setPeriod(TimeUnit.TICKS, 4);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("§b☃", stack).concat(Strings.repeat("§7☃", 7 - stack)));
			hologram.display(getPlayer());
			stackMap.put(player, this);
			addStack();
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
		}

		private boolean addDoubleStack() {
			setCount(15);
			stack = Math.min(7, stack + 2);
			soundstack++;
			switch(soundstack) {
			case 1:
			case 7:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.natural(0, Tone.F));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.natural(0, Tone.F));
				break;
			case 2:
			case 6:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.sharp(1, Tone.F));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.sharp(1, Tone.F));
				break;
			case 3:
			case 5:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.sharp(1, Tone.G));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.sharp(1, Tone.G));
				break;
			case 4:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.sharp(1, Tone.A));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.sharp(1, Tone.A));
				break;
			case 8:
			case 15:
			case 16:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.sharp(0, Tone.D));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.sharp(0, Tone.D));
				break;
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.sharp(0, Tone.C));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.sharp(0, Tone.C));
				break;
			case 17:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.natural(0, Tone.F));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.natural(0, Tone.F));
				soundstack = 0;
				break;
			}
			hologram.setText(Strings.repeat("§b☃", stack).concat(Strings.repeat("§7☃", 7 - stack)));
			Chill.apply(getGame().getParticipant(player), TimeUnit.SECONDS, stackMap.get(player).stack);
			
			if (stack >= 7) {
				stop(false);
				return true;
			} else {
				return false;
			}
		}
		
		private boolean addStack() {
			setCount(15);
			stack++;
			soundstack++;
			switch(soundstack) {
			case 1:
			case 7:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.natural(0, Tone.F));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.natural(0, Tone.F));
				break;
			case 2:
			case 6:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.sharp(1, Tone.F));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.sharp(1, Tone.F));
				break;
			case 3:
			case 5:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.sharp(1, Tone.G));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.sharp(1, Tone.G));
				break;
			case 4:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.sharp(1, Tone.A));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.sharp(1, Tone.A));
				break;
			case 8:
			case 15:
			case 16:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.sharp(0, Tone.D));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.sharp(0, Tone.D));
				break;
			case 9:
			case 10:
			case 11:
			case 12:
			case 13:
			case 14:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.sharp(0, Tone.C));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.sharp(0, Tone.C));
				break;
			case 17:
				SoundLib.CHIME.playInstrument(player.getLocation(), Note.natural(0, Tone.F));
				SoundLib.BELL.playInstrument(player.getLocation(), Note.natural(0, Tone.F));
				soundstack = 0;
				break;
			}
			hologram.setText(Strings.repeat("§b☃", stack).concat(Strings.repeat("§7☃", 7 - stack)));
			Chill.apply(getGame().getParticipant(player), TimeUnit.SECONDS, stackMap.get(player).stack);
			
			if (stack >= 7) {
				stop(false);
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
	
}
