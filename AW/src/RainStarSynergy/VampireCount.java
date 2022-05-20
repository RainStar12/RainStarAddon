package RainStarSynergy;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.google.common.base.Predicate;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.aw.Arc;

@AbilityManifest(name = "뱀파이어 백작", rank = Rank.L, species = Species.UNDEAD, explain = {
		"§7패시브 §8- §3밤의 귀족§f: 밤에는 흡혈로 얻는 회복량이 1.25배 증가합니다.",
		" 또한 §a발검§7/§e납검§f 쿨타임이 2배로 줄어듭니다.",
		"§7철괴 우클릭 §8- §a발검§7/§e납검§f: §4혈검§f을 빼내거나 집어넣습니다. $[COOLDOWN]",
		" §4혈검§f을 빼낼 때§8(§7발검할 때§8)§f에는 전방에 범위 피해를 입히고 흡혈합니다.",
		"§6[§e납검§6] §c피의 잔§f: $[DRAIN]초마다 주변 $[RANGE]칸 내 생명체들의 체력을 흡혈합니다.",
		" 생명체가 나랑 가까우면 가까울수록 대상당 흡혈량이 §d$[ENTITY_MAX_DRAIN]HP§f까지 증가합니다.",
		"§2[§a발검§2] §4혈검술§f: 근접 공격 시 체력을 §d$[USE_HP]HP§f 소모하여 §c$[DAMAGE_MULTIPLY]배§f의 피해를 입힙니다.",
		" 이후 대상이 잃은 체력의 핏방울이 튀어 줏을 시 피를 회복합니다.",
		"§3[§b종족야생 콜라보 능력§3]"
		})
public class VampireCount extends Synergy implements ActiveHandler {

	public VampireCount(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> DRAIN = synergySettings.new SettingObject<Integer>(
			VampireCount.class, "drain-period", 4, "# 흡혈 주기", "# (단위: 초)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> RANGE = synergySettings.new SettingObject<Double>(
			VampireCount.class, "range", 8.0, "# 흡혈 범위", "# (단위: 칸)") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> USE_HP = synergySettings.new SettingObject<Double>(
			VampireCount.class, "use-hp", 1.5, "# 발검 체력 소모량") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> DAMAGE_MULTIPLY = synergySettings.new SettingObject<Double>(
			VampireCount.class, "damage-multiply", 1.4, "# 발검의 대미지 배율") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> ENTITY_MAX_DRAIN = synergySettings.new SettingObject<Double>(
			VampireCount.class, "entity-max-drain", 2.0, "# 한 흡혈로 얻는 대상당 최대 흡혈량") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = synergySettings.new SettingObject<Integer>(
			VampireCount.class, "cooldown", 15, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (Math.abs(entity.getLocation().getY() - getPlayer().getLocation().getY()) > 10) return false;
			if (entity instanceof Player) {
				Player player = (Player) entity;
				if (player.equals(getPlayer())) return false;
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
	
	private final Predicate<Entity> allExceptPredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private final double range = RANGE.getValue();
	private final double entity_max_drain = ENTITY_MAX_DRAIN.getValue();
	private final double use_hp = USE_HP.getValue();
	private final double damage_multiply = DAMAGE_MULTIPLY.getValue();
	private ActionbarChannel ac = newActionbarChannel();
	private BossBar bossBar = null;
	private final DecimalFormat df = new DecimalFormat("0.0");
	private final Crescent crescent = Crescent.of(0.85, 20);
	private final Arc arc = Arc.of(4, 120);
	private int particleSide = 45;
	private int arcSide = 180;
	private final Circle circle = Circle.of(range, (int) (range * 10));
	private boolean bs = false;
	private int stack = 0;
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._25);
	private final int drainperiod = (DRAIN.getValue() * 20);
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown()) {
			bs = !bs;
			if (bs) {
				new LateralCutParticle(arcSide).start();
				arcSide *= -1;
				SoundLib.ENTITY_WITCH_AMBIENT.playSound(getPlayer().getLocation(), 1f, 1.75f);
				SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer().getLocation(), 1f, 0.5f);
				for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, getPlayer().getLocation(), 6, 2.5, predicate)) {
					Vector direction = getPlayer().getLocation().getDirection().setY(0).normalize();
					Vector targetLocation = livingEntity.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).setY(0).normalize();
					double dot = targetLocation.dot(direction);
					if (dot > 0.475D) {
						livingEntity.damage(14, getPlayer());
					}
				}
			} else {
				SoundLib.ITEM_ARMOR_EQUIP_IRON.playSound(getPlayer().getLocation(), 1, 0.85f);
				SoundLib.ITEM_ARMOR_EQUIP_CHAIN.playSound(getPlayer().getLocation(), 1, 0.85f);
				if (bloodgrail.isRunning()) bloodgrail.stop(false);
			}
			return cooldown.start();
	    }
	    return false;
	}
	
	private static boolean isNight(long worldtime) {
		return worldtime >= 13000;
	}
	
	private AbilityTimer passive = new AbilityTimer(TaskType.INFINITE, -1) {
		
		@Override
		public void run(int count) {
			ac.update(isNight(getPlayer().getWorld().getTime()) ? "§5§l밤" : "§e§l낮");
			if (isNight(getPlayer().getWorld().getTime()) && cooldown.isRunning() && count % 20 == 0) {
				cooldown.setCount(cooldown.getCount() - 1);
			}
			if (!bs && !bloodgrail.isRunning()) bloodgrail.start();
			else if (bs && bloodgrail.isRunning()) bloodgrail.stop(false);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);

	private AbilityTimer bloodgrail = new AbilityTimer(TaskType.REVERSE, drainperiod) {
		
		@Override
		public void onStart() {
			bossBar = Bukkit.createBossBar("§4[§c피의 잔§4] §b다음 흡혈§7: §e10.0§f초", BarColor.RED, BarStyle.SOLID);
			bossBar.setProgress(0);
    		bossBar.addPlayer(getPlayer());
    		bossBar.setVisible(true);
		}
		
		@Override
		public void run(int count) {
			bossBar.setTitle("§4[§c피의 잔§4] §b다음 흡혈§7: §e" + df.format((double) count / 20) + "§f초");
			bossBar.setProgress(((double) (drainperiod - count + 1) / drainperiod));
			if (count == 1) {
				this.setCount(drainperiod);
				if (LocationUtil.getEntitiesInCircle(LivingEntity.class, getPlayer().getLocation(), range, predicate).size() > 0) {
					SoundLib.ENTITY_WANDERING_TRADER_DRINK_MILK.playSound(getPlayer().getLocation(), 1, 0.95f);
					stack++;
					double temp = (double) stack;
					int soundnumber = (int) (temp - (Math.ceil(temp / SOUND_RUNNABLES.size()) - 1) * SOUND_RUNNABLES.size()) - 1;
					SOUND_RUNNABLES.get(soundnumber).run();
					for (LivingEntity livingEntity : LocationUtil.getEntitiesInCircle(LivingEntity.class, getPlayer().getLocation(), 8, predicate)) {
						livingEntity.damage(0.5, getPlayer());
						Location myLoc = getPlayer().getLocation().clone();
						Location targetLoc = livingEntity.getLocation().clone();
						myLoc.setY(0);
						targetLoc.setY(0);
						double distance = Math.sqrt(myLoc.distanceSquared(targetLoc));
						if (livingEntity instanceof Player) Healths.setHealth((Player) livingEntity, Math.max(0, livingEntity.getHealth() - ((Math.max(0, (8 - distance) / 8)) + 1)));
						else livingEntity.setHealth(Math.max(0, livingEntity.getHealth() - ((Math.max(0, (range - distance) / range) * entity_max_drain))));
						double heal = (isNight(getPlayer().getWorld().getTime()) ? 1.25 : 1) * (((Math.max(0, (range - distance) / range)) + 1));
						final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), heal, RegainReason.CUSTOM);
						Bukkit.getPluginManager().callEvent(event);
						if (!event.isCancelled()) {
							Healths.setHealth(getPlayer(), getPlayer().getHealth() + heal);	
							for (Location location : Line.between(getPlayer().getLocation().clone().add(0, 1, 0), 
									livingEntity.getLocation().clone().add(0, 1, 0), 20).toLocations(getPlayer().getLocation())) {
								ParticleLib.DAMAGE_INDICATOR.spawnParticle(location.clone().add(0, 1, 0), 0, 0, 0, 1, 0);
							}
						}
					}
					for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
						for (Player player : LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), 8, allExceptPredicate)) {
							ParticleLib.ITEM_CRACK.spawnParticle(player, loc, 0, 0, 0, 1, 0.35, Material.REDSTONE_BLOCK);	
						}
					}	
				}	
			}
		}
		
		@Override
		public void onEnd() {
			bossBar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId()) && bloodgrail.isRunning()) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (getPlayer().equals(e.getDamager()) && bs) {
			stack++;
			double temp = (double) stack;
			int soundnumber = (int) (temp - (Math.ceil(temp / SOUND_RUNNABLES.size()) - 1) * SOUND_RUNNABLES.size()) - 1;
			SOUND_RUNNABLES.get(soundnumber).run();
			Healths.setHealth(getPlayer(), Math.max(1, getPlayer().getHealth() - use_hp));
			e.setDamage(e.getDamage() * damage_multiply);
			particleSide *= -1;
			SoundLib.UI_STONECUTTER_TAKE_RESULT.playSound(getPlayer().getLocation(), 1, 1.25f);
			new CutParticle(particleSide).start();
			new Bloods(e.getEntity().getLocation(), (e.getEntity() instanceof Player) ? (e.getFinalDamage() * 1.1) : (e.getFinalDamage() * 0.3)).start();
		}
	}
	
	private class LateralCutParticle extends AbilityTimer {

		private final Vector vector;
		private final Vectors arcVectors;

		private LateralCutParticle(double angle) {
			super(TaskType.REVERSE, 4);
			setPeriod(TimeUnit.TICKS, 1);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.arcVectors = arc.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180);
		}
		
		@Override
		protected void run(int count) {
			Location baseLoc = getPlayer().getLocation().clone().add(vector).add(0, 1.3, 0);
			for (Location loc : arcVectors.toLocations(baseLoc)) {
				ParticleLib.REDSTONE.spawnParticle(loc, RGB.RED);
				ParticleLib.ITEM_CRACK.spawnParticle(loc, 0, 0, 0, 1, 0.02, MaterialX.NETHER_WART_BLOCK);
			}
		}

	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector axis;
		private final Vector vector;
		private final Vectors crescentVectors;

		private CutParticle(double angle) {
			super(TaskType.REVERSE, 4);
			setPeriod(TimeUnit.TICKS, 1);
			this.axis = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(getPlayer().getLocation().getDirection().setY(0).normalize(), 90), 
					getPlayer().getLocation().getDirection().setY(0).normalize(), angle);
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
				ParticleLib.DRIP_LAVA.spawnParticle(loc, 0, 0, 0, 1, 0);
			}
			crescentVectors.rotateAroundAxis(axis, 40);
		}

	}
	
	public class Bloods extends AbilityTimer implements Listener {
		
		private final Location location;
		private final double heal;
		private Item blood;
		private final Random random = new Random();
		
		public Bloods(Location location, Double heal) {
			super(TaskType.REVERSE, 200);
			setPeriod(TimeUnit.TICKS, 1);
			this.location = location;
			this.heal = heal;
		}
		
		@Override
		public void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			ItemStack bloodtype = new ItemStack(Material.REDSTONE);
			blood = location.getWorld().dropItem(location, bloodtype);
			blood.setVelocity(new Vector(((random.nextDouble() * 2) - 1) / 2.5, .55, ((random.nextDouble() * 2) - 1) / 2.5));
		}
		
		@Override
		public void run(int count) {
			if (count <= 50) {
				if (count % 2 == 0) blood.setGlowing(true);
				else blood.setGlowing(false);
			}
		}
 		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			HandlerList.unregisterAll(this);
			blood.remove();
		}
		
		@EventHandler
		public void onEntityPickup(EntityPickupItemEvent e) {
			if (e.getItem().equals(blood)) {
				e.setCancelled(true);
				if (e.getEntity().equals(getPlayer()) && this.getCount() < 190) {
					this.stop(false);
					double healamount = (isNight(getPlayer().getWorld().getTime()) ? 1.25 : 1) * heal;
					final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), healamount, RegainReason.CUSTOM);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						SoundLib.ENTITY_ITEM_PICKUP.playSound(getPlayer().getLocation(), 1, 0.65f);
						Healths.setHealth(getPlayer(), getPlayer().getHealth() + healamount);
						ParticleLib.HEART.spawnParticle(getPlayer().getLocation(), 0.2, 2, 0.2, 2, 0);
					}
				}
			}
		}
		
	}
	
	private final List<Runnable> SOUND_RUNNABLES = Arrays.asList(

			() -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.G));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.G));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.G));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.G));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.C));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.C));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.C));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.C));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.C));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.D));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.G));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.C));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.D));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.G));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(2, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(2, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(2, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(2, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(2, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(2, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(2, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(2, Tone.F));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
			}, () -> {
				SoundLib.GUITAR.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.C));
				stack = 0;
			}

	);
	
}