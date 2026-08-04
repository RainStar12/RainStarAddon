package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.boundary.CenteredBoundingBox;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "새벽", rank = Rank.SPECIAL, species = Species.SPECIAL, explain = {
		"§7패시브 §8- §e밝아오는 여명§f: 적을 공격할 때마다, §e여명§f 스택을 획득합니다.",
		" 가진 §e여명§f 스택당 §c공격력§f이 0.4%, §d회복 속도§f가 0.004/s 증가합니다.",
		"§7패시브 §8- §3새벽의 가호§f: 활성화된 동안 §b이동 속도§f가 $[SPEED_INCREASE]% 증가합니다.",
		" §5§n상태이상§f을 받으면 무효화하고 $[BLESS_DURATION]초간 §3무적§f 및 §5상태이상 면역§f,",
		" §3타게팅 불가 상태§f가 됩니다. 이후 §e여명§f을 $[BLESS_CONSUME] 소모하여 재활성화합니다.",
		"§7기본 공격 §8- §b하늘 가르기§f: 근접 공격력이 33% 감소합니다. 대신, 검을 휘두르면",
		" 전방으로 §a검기§f를 내보내 기본 무적을 무시하고 마법 피해를 추가로 입힙니다.",
		"§7검 우클릭 §8- §9하늘 찢기§f: 바라보는 방향으로 대시하며 5회 연속 §a검기§f 공격을 합니다.",
		" §e여명§f이 $[STACK_COUNT] 쌓일 때마다, 최대 4회까지 충전됩니다."
		})
public class Daybreak extends AbilityBase {
	
	public Daybreak(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> SPEED_INCREASE = rsAbilitySettings.new SettingObject<Integer>(
			Daybreak.class, "speed-increase", 20, "# 이동 속도 증가량") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> BLESS_DURATION = rsAbilitySettings.new SettingObject<Double>(
			Daybreak.class, "bless-duration", 5.0, "# 가호 지속 시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> BLESS_CONSUME = rsAbilitySettings.new SettingObject<Integer>(
			Daybreak.class, "bless-consume", 20, "# 가호 재활성에 필요한 여명 소모량") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> STACK_COUNT = rsAbilitySettings.new SettingObject<Integer>(
			Daybreak.class, "stack-count", 10, "# 하늘 찢기 충전에 필요한 여명") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> CUT_COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(
			Daybreak.class, "cut-cooldown", 20, "# 하늘 가르기 내부 쿨타임", "# 단위: 틱 (20틱 = 1초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private int blessduration = (int) (BLESS_DURATION.getValue() * 20);
	private int stackcount = STACK_COUNT.getValue(), blessconsume = BLESS_CONSUME.getValue();
	private int dawn = 0, stack = 0, charge = 0;
	private ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel(), ac3 = newActionbarChannel(), ac4 = newActionbarChannel();
	private static final Set<Material> swords;
	private boolean state = false;
	private int cutcool = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(40) * CUT_COOLDOWN.getValue() : CUT_COOLDOWN.getValue());
	private boolean blessing = true;
	private DecimalFormat df = new DecimalFormat("0.0");
	private AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "addspeed", SPEED_INCREASE.getValue() * 0.01, Operation.ADD_SCALAR);
	private boolean modifieradded = false;
	private static final int particleCount = 40;
	private static final List<RGB> gradation = Gradient.createGradient(100, RGB.of(136, 68, 211), RGB.of(79, 129, 159), RGB.of(37, 174, 120));
	private static final Circle circle = Circle.of(0.5, particleCount);
	private static final double yDiff = 0.6 / particleCount;
	private int colorstack = 0;
	private boolean colordirection = false;
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}

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
	};
	
	private static final Component[] components = new Component[]{
			new Component(RGB.of(3, 102, 252), 2.8, 1.35),
			new Component(RGB.of(3, 102, 252), 2.8, 1.3),
			new Component(RGB.of(80, 118, 242), 2.8, 1.25),
			new Component(RGB.of(80, 118, 242), 2.8, 1.2),
			new Component(RGB.of(92, 183, 242), 2.8, 1.15),
			new Component(RGB.of(92, 183, 242), 2.8, 1.1),
			new Component(RGB.WHITE, 2.8, 1.05),
			new Component(RGB.WHITE, 2.8, 1)
	};
	
	@Override
	public void onUpdate(Update update) {
		if (update.equals(Update.RESTRICTION_CLEAR)) {
			ac1.update("§e여명§7: §b" + dawn);
			ac2.update(Strings.repeat("§9✘", charge).concat(Strings.repeat("§7✘", 4 - charge)));
			ac3.update((blessing ? "§3" : "§7") + "새벽의 가호");
			passive.start();
			showHelix();
		}
	}
	
	public void stackAdd() {
		dawn++;
		stack++;
		ac1.update("§e여명§7: §b" + dawn);
		if (stack >= stackcount) {
			stack -= stackcount;
			charge = Math.min(4, charge + 1);
			ac2.update(Strings.repeat("§9✘", charge).concat(Strings.repeat("§7✘", 4 - charge)));
		}
	}
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), dawn * 0.0002, RegainReason.REGEN);
    		if (!event.isCancelled()) {
    			Healths.setHealth(getPlayer(), Math.min(getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), getPlayer().getHealth() + event.getAmount()));
    		}
    		if (!inv.isRunning() && blessing == false && dawn >= blessconsume) {
    			dawn -= blessconsume;
    			blessing = true;
    			ac1.update("§e여명§7: §b" + dawn);
    		}
    		ac3.update((blessing ? "§3" : "§7") + "새벽의 가호");
    		
    		if (blessing && !modifieradded) {
    			modifieradded = true;
    			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
    		}
    		
    		if (!blessing && modifieradded) {
    			modifieradded = false;
    			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
    		}
    	}
    	
    	@Override
    	public void onEnd() {
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(modifier);
    	}
	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private final AbilityTimer inv = new AbilityTimer(blessduration) {
    	
		private double y;
		private boolean add;
		private int stack;
    	
    	@Override
    	public void onStart() {
    		getGame().getParticipant(getPlayer()).attributes().TARGETABLE.setValue(false);
    		ac4.update("§3무적§f: §e" + df.format(blessduration / 20.0) + "§f초");
			y = 0.0;
			add = true;
			stack = 0;
    	}
    	
    	@Override
    	public void run(int count) {
    		ac4.update("§3무적§f: §e" + df.format(count / 20.0) + "§f초");
			if (add && y >= 2.0) add = false;
			else if (!add && y <= 0) add = true;

			y = add ? y + 0.1 : y - 0.1;
			stack = stack < 100 ? stack + 1 : 0;
			
			for (Location location : circle.toLocations(getPlayer().getLocation().add(0, y, 0))) {
				ParticleLib.REDSTONE.spawnParticle(location, gradation.get(Math.max(0, stack - 1)));
			}
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		getGame().getParticipant(getPlayer()).attributes().TARGETABLE.setValue(true);
    		showHelix();
    		ac4.update(null);
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(final PlayerInteractEvent e) {
		if (e.getItem() != null && swords.contains(e.getItem().getType())) {
			if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				cut();
			} else if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (charge > 0) {
					new Dash(getPlayer().getLocation().clone().add(0, 1, 0), getPlayer().getLocation().getDirection(), RGB.AQUA, RGB.WHITE).start();
					charge--;
					ac2.update(Strings.repeat("§9✘", charge).concat(Strings.repeat("§7✘", 4 - charge)));
				}
			}
		}
	}

	@SubscribeEvent
	private void onDamageCut(final EntityDamageByEntityEvent e) {
		if (getPlayer().equals(e.getDamager())) {
			if (swords.contains(getPlayer().getInventory().getItemInMainHand().getType())) {
				cut();
			}
		}
	}
	
	@SubscribeEvent(ignoreCancelled = true)
	private void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter().equals(getPlayer())) {
				damager = getPlayer();
			}
		}
		if (!e.getEntity().equals(getPlayer()) && !(e.getEntity() instanceof ArmorStand) && (getPlayer().equals(e.getDamager()) || getPlayer().equals(damager))) {
			if (getPlayer().equals(e.getDamager())) e.setDamage(e.getDamage() * 0.66);
			e.setDamage(e.getDamage() * (1 + (dawn * 0.004)));
			stackAdd();
		}
		
		if (e.getEntity().equals(getPlayer()) && inv.isRunning()) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(ignoreCancelled = true)
	private void onEffect(ParticipantPreEffectApplyEvent e) {
		if (e.getParticipant().equals(getParticipant())) {
			if (blessing) {
				inv.start();
				blessing = false;
				e.setCancelled(true);
				SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer().getLocation(), 1, 1.5f);
				final Firework firework = getPlayer().getWorld().spawn(getPlayer().getLocation(), Firework.class);
				final FireworkMeta meta = firework.getFireworkMeta();
				Color color1 = RGB.of(67, 191, 243).getColor(), color2 = RGB.of(52, 101, 161).getColor(), color3 = RGB.of(54, 171, 234).getColor(), 
						color4 = RGB.of(254, 254, 254).getColor(), color5 = RGB.of(192, 235, 251).getColor(), color6 = RGB.of(56, 185, 248).getColor(),
						color7 = RGB.of(12, 139, 252).getColor(), color8 = RGB.of(254, 223, 134).getColor();
				meta.addEffect(
						FireworkEffect.builder()
						.withColor(color1, color2, color3, color4, color5, color6, color7, color8)
						.withTrail()
						.withFlicker()
						.with(Type.BURST)
						.build()
				);	
				meta.setPower(0);
				firework.setFireworkMeta(meta);
				firework.setMetadata("Firework", NULL_VALUE);
				new BukkitRunnable() {
					@Override
					public void run() {
						firework.detonate();
					}
				}.runTaskLater(AbilityWar.getPlugin(), 1L);
			}
			if (inv.isRunning()) {
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByFirework(EntityDamageByEntityEvent e) {
		if (e.getDamager().hasMetadata("Firework")) {
			e.setCancelled(true);
		}
	}
	
	private void showHelix() {
		new AbilityTimer((particleCount * 3) / 2) {
			int count = 0;

			@Override
			protected void run(int a) {
				for (int i = 0; i < 2; i++) {
					if (colorstack == 99 || colorstack == 0) colordirection = !colordirection;
					colorstack = colordirection ? Math.min(colorstack + 1, 99) : Math.max(colorstack - 1, 0);
					ParticleLib.REDSTONE.spawnParticle(getPlayer().getLocation().clone().add(circle.get(count % 40)).add(0, count * yDiff, 0), gradation.get(colorstack));
					count++;
				}
			}
			
			@Override
			protected void onEnd() {
				if (!inv.isRunning()) {
					showHelix();
				}
			}
		}.setPeriod(TimeUnit.TICKS, 1).start();
	}
	
	private AbilityTimer cut_cooldown = new AbilityTimer(cutcool) {
		
	}.setPeriod(TimeUnit.TICKS, 1).register();

	private void cut() {
		if (!cut_cooldown.isRunning()) {
			cut_cooldown.start();
			new Cut(getPlayer().getLocation().clone().add(0, 1.5, 0), (state = !state) ? 25 : -25, null).start();
			SoundLib.ENTITY_WITHER_SHOOT.playSound(getPlayer().getLocation());
		}
	}
	
	private static class Component {

		private final RGB color;
		private final double radius, length;

		private Component(RGB color, double radius, double length) {
			this.color = color;
			this.radius = radius;
			this.length = length;
		}

	}

	public class Cut extends AbilityTimer {

		private final CenteredBoundingBox boundingBox = CenteredBoundingBox.of(new Vector(), -1, -1, -1, 1, 1, 1);
		private final Location base;
		private final double angle;
		private final int sharpness;
		private final Consumer<LivingEntity> onDamage;
		private final Set<LivingEntity> damaged = new HashSet<>();

		private Cut(Location base, double angle, Consumer<LivingEntity> onDamage) {
			super(angle < 0 ? TaskType.REVERSE : TaskType.NORMAL, 3);
			setPeriod(TimeUnit.TICKS, 1);
			this.base = base;
			this.angle = angle;
			this.sharpness = getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL);
			this.onDamage = onDamage;
		}

		@Override
		protected void run(int count) {
			final Vector direction = base.getDirection().setY(0).normalize(), axis = VectorUtil.rotateAroundAxisY(direction.clone(), 90);
			for (int i = 0; i < 10; i++) {
				final double radians = 0.10471975511965977461542144610932 * ((count - 1) * 10 + i);
				final double cos = FastMath.cos(radians), sin = FastMath.sin(radians);
				for (Component component : components) {
					final Location loc = base.clone().add(
							VectorUtil.rotateAroundAxis(
									VectorUtil.rotateAroundAxis(
											VectorUtil.rotateAroundAxisY(new Vector(cos, 0, component.length * sin), -base.getYaw()), direction, angle
									), axis, base.getPitch()
							).multiply(component.radius)
					);
					ParticleLib.REDSTONE.spawnParticle(loc, component.color);
					boundingBox.setCenter(loc);
					for (LivingEntity entity : LocationUtil.getConflictingEntities(LivingEntity.class, getPlayer().getWorld(), boundingBox, predicate)) {
						if (!damaged.contains(entity)) {
							entity.setNoDamageTicks(0);
							Damages.damageMagic(entity, getPlayer(), false, (float) (EnchantLib.getDamageWithSharpnessEnchantment(getPlayer().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue(), sharpness) * 0.66));
							if (onDamage != null) onDamage.accept(entity);
							damaged.add(entity);
						}
					}
				}
			}
		}

	}

	private final double startRadius = 1.25, distance = 12.5;

	public class Dash extends AbilityTimer {

		private final Vector direction;
		private final double yaw;
		private final RGB color, circleColor;
		private Location lastLocation;
		private double radius = startRadius, radians = 0.0;

		private Dash(Location startLocation, Vector direction, RGB color, RGB circleColor) {
			super(5);
			setPeriod(TimeUnit.TICKS, 1);
			this.direction = direction.setY(0).normalize().multiply(distance / 5);
			this.yaw = LocationUtil.getYaw(direction);
			this.color = color;
			this.circleColor = circleColor;
			this.lastLocation = startLocation;
		}

		@Override
		protected void run(int count) {
			final Location newLocation = lastLocation.clone().add(direction);
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
				Location location = iterator.next();
				ParticleLib.REDSTONE.spawnParticle(location, color);
				radians += 0.15707963267948966192313216916398;
				radius -= (startRadius / (distance * 10));
				ParticleLib.REDSTONE.spawnParticle(location.clone().add(
						VectorUtil.rotateAroundAxisY(new Vector(FastMath.cos(radians), FastMath.sin(radians), 0), -yaw).multiply(radius)
				), circleColor);
			}
			lastLocation = newLocation;
			getPlayer().setVelocity(direction);
			if (count % 2 != 0) {
				final Location base = newLocation.clone();
				base.setDirection(direction);
				new Cut(base, (state = !state) ? 25 : -25, null).start();
				SoundLib.ENTITY_WITHER_SHOOT.playSound(base);
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			getPlayer().setVelocity(VectorUtil.ZERO);
		}
	}

}