package RainStarAbility;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import RainStarAbility.timestop.TimeStop;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "회중시계", rank = Rank.L, species = Species.OTHERS, explain = {
        "§7철괴 좌클릭 §8- §b빨리감기§f: §b이동 속도§f, §d회복 속도§f가 증가하는 §b시간 가속§f을 켜고 끕니다.",
        "§7패시브 §8- §3시간의 흐름§f: §b시간 가속§f을 연속 유지할수록 점점 그 효과가 증가합니다.",
        " §b시간 가속§f 해제 시나 $[MAX_TIME]초 사용 시 사용 시간 비례 §c쿨타임§f을 가지게 됩니다.",
        "§7가속 간 사망 §8- §a되감기§f: $[RANGE]칸 내 적의 시간을 $[DURATION]초간 정지시킵니다.",
        " 이 안에 정지된 적을 처치한다면 §b가속§f 간 가장 체력이 많은 시간으로 §a역행§f합니다.",
        " §a역행§f하지 못하면 사망합니다. 정지된 적은 받는 피해가 $[DECREASE]% 감소합니다.",
        "§a[§e능력 제공자§a] §eLUCKY7_cross"
        },
        summarize = {
        "§7철괴 좌클릭§f으로 §b이동 속도§f, §d회복 속도§f가 증가하는 §b시간 가속§f을 켜고 끕니다.",
        "§b시간 가속§f은 연속 유지할수록 효과가 증가하나 해제 시 유지 시간 비례 쿨타임을 가집니다.",
        "§b가속§f 중 사망 시, 잠시간 주변 적의 시간을 멈추고 이 안에 정지된 적을",
        "처치할 경우 지난 §b가속§f 중 체력이 가장 많던 때로 §a역행§f합니다."
        })
public class PocketWatch extends AbilityBase implements ActiveHandler {
	
	public PocketWatch(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> MAX_TIME = abilitySettings.new SettingObject<Integer>(PocketWatch.class,
			"max-time", 30, "# 시간 가속 최대 시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> MULTIPLY = abilitySettings.new SettingObject<Double>(PocketWatch.class,
			"multiply-cooldown", 3.3, "# 사용한 시간의 쿨타임 배수") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> RANGE = abilitySettings.new SettingObject<Double>(PocketWatch.class,
			"range", 7.0, "# 시간 정지 범위") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DURATION = abilitySettings.new SettingObject<Double>(PocketWatch.class,
			"duration", 4.5, "# 시간 정지 시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DECREASE = abilitySettings.new SettingObject<Integer>(PocketWatch.class,
			"decrease", 20, "# 받는 피해 감소량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private final double range = RANGE.getValue();
	private final int duration = (int) (DURATION.getValue() * 20);
	private final int maxtime = MAX_TIME.getValue() * 20;
	private final double multiply = MULTIPLY.getValue();
	private final double decrease = 1 - (DECREASE.getValue() * 0.01);
	private BossBar bossbar = null;
	private int usedtime = 0;
	private int cooldown = 0;
	private int effectcount = 0;
	private final DecimalFormat df = new DecimalFormat("0.0");
	private final Circle circle = Circle.of(0.5, 15);
	private static final RGB startColor = RGB.of(1, 254, 254), endColor = RGB.of(1, 147, 147);
	private final List<RGB> gradations = Gradient.createGradient(15, startColor, endColor);
	private Set<Player> stopped = new HashSet<>();
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	private Location rewindLocation;
	private double rewindHP;
	
	private static final EulerAngle DEFAULT_EULER_ANGLE = new EulerAngle(Math.toRadians(270), 0, 0);
	private static final ItemStack CLOCK = MaterialX.CLOCK.createItem();
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
					return false;
				}
				if (getGame().getParticipant(entity.getUniqueId()).hasAbility()) {
					AbilityBase ab = getGame().getParticipant(entity.getUniqueId()).getAbility();
					if (ab.getClass().equals(TimeStop.class)) {
						return false;
					} else if (ab.getClass().equals(Mix.class)) {
						Mix mix = (Mix) ab;
						if (mix.hasAbility()) {
							if (!mix.hasSynergy()) {
								if (mix.getFirst().getClass().equals(TimeStop.class)
										|| mix.getSecond().getClass().equals(TimeStop.class)) {
										return false;
								}
							}
						}
					}
				}
			}
			return true;
		}
	};
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) bossbarUpdater.start();
		if (update == AbilityBase.Update.RESTRICTION_SET || update == AbilityBase.Update.ABILITY_DESTROY) {
			getPlayer().setSprinting(false);
		    getPlayer().setWalkSpeed(0.2F);
		}
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK && !rewind.isRunning()) {
			if (timeaccel.isRunning()) timeaccel.stop(false);
			else {
				if (cooldown != 0) {
					getPlayer().sendMessage("§4[§c!§4] §f아직 쿨타임입니다.");
					return false;
				} else {
					effectcount = 0;
					timeaccel.start();
				}
			}
			return true;
		}
		return false;
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (stopped.contains(e.getEntity()) && getPlayer().equals(e.getEntity().getKiller())) {
			rewind.stop(true);
			Healths.setHealth(getPlayer(), rewindHP);
			getPlayer().teleport(rewindLocation);
			
			SoundLib.BELL.playInstrument(getPlayer(), Note.natural(0, Tone.D));
			SoundLib.BELL.playInstrument(getPlayer(), Note.sharp(0, Tone.F));
			SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			final Firework firework = getPlayer().getWorld().spawn(getPlayer().getEyeLocation(), Firework.class);
			final FireworkMeta meta = firework.getFireworkMeta();
			meta.addEffect(
					FireworkEffect.builder()
							.withColor(Color.fromRGB(32, 60, 255), Color.WHITE, Color.fromRGB(250, 213, 0))
							.with(Type.BALL)
							.build()
			);
			meta.setPower(0);
			firework.setFireworkMeta(meta);
			firework.setMetadata("time-rewind", NULL_VALUE);
			new BukkitRunnable() {
				@Override
				public void run() {
					firework.detonate();
				}
			}.runTaskLater(AbilityWar.getPlugin(), 1L);
		}
	}
	
	@SubscribeEvent
	public void onPlayerSetHealth(PlayerSetHealthEvent e) {
		if (rewind.isRunning() && (e.getPlayer().equals(getPlayer()) || stopped.contains(e.getPlayer()))) e.setCancelled(true);
	}
	
	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (rewind.isRunning() && (e.getEntity().equals(getPlayer()) || stopped.contains(e.getEntity()))) e.setCancelled(true);
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (stopped.contains(e.getPlayer())) e.setTo(e.getFrom());
	}
	
	@SubscribeEvent(priority = 1000)
	public void onDeath(EntityDamageByEntityEvent e) {
		if (timeaccel.isRunning() && !e.isCancelled() && e.getEntity().equals(getPlayer()) && !rewind.isRunning()) {
			if (getPlayer().getHealth() - e.getFinalDamage() <= 0) {
				e.setCancelled(true);
				Healths.setHealth(getPlayer(), 1);
				rewind.start();
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().hasMetadata("time-rewind")) {
			e.setCancelled(true);
		}
		
		if (rewind.isRunning()) {
			if (e.getEntity().equals(getPlayer())) {
				e.setCancelled(true);
			}
			
			if (stopped.contains(e.getEntity())) {
				e.setDamage(e.getDamage() * decrease);
			}
		}
	}
	
	private final AbilityTimer bossbarUpdater = new AbilityTimer() {
		
		@Override
		public void onStart() {
    		bossbar = Bukkit.createBossBar("§e사용 가능", BarColor.WHITE, BarStyle.SOLID);
    		bossbar.setProgress(1);
    		bossbar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossbar.setVisible(true);
		}
		
    	@Override
		public void run(int count) {
    		if (timeaccel.isRunning()) {
    			bossbar.setColor(BarColor.BLUE);
    			bossbar.setProgress(usedtime / (double) maxtime);
    			bossbar.setTitle("§b시간 가속§f: §e" + df.format(usedtime / 20.0) + "§7/§c" + df.format(maxtime / 20.0));
    		} else {
    			if (cooldown == 0) {
    				if (count % 2 == 0) {
        				if (effectcount <= 5) {
        					SoundLib.BELL.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
        					effectcount++;
        					bossbar.setColor(effectcount % 2 == 0 ? BarColor.WHITE : BarColor.YELLOW);
        					bossbar.setTitle("§e사용 가능");
        				}
    				}
    			} else {
    				cooldown--;
    				bossbar.setColor(BarColor.RED);
        			bossbar.setProgress(cooldown / (maxtime * multiply));
        			bossbar.setTitle("§b쿨타임§f: §e" + df.format(cooldown / 20.0) + "§f초");
    			}
    		}
    	}
    	
		@Override
		public void onEnd() {
			bossbar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossbar.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer rewind = new AbilityTimer(duration) {
		
		private List<ArmorStand> armorstands = new ArrayList<>();
		
		@Override
		public void onStart() {
			SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
			NMS.sendTitle(getPlayer(), "§c사망 지연", "§e" + df.format(duration / 20.0), 0, 20, 0);
			timeaccel.stop(true);
			for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate)) {
				stopped.add(player);
				ArmorStand armorstand = player.getWorld().spawn(player.getLocation().clone().add(0, 2, 0), ArmorStand.class);
				armorstand.setVisible(false);
				armorstand.setInvulnerable(true);
				armorstand.setGravity(false);
				armorstand.setRightArmPose(DEFAULT_EULER_ANGLE);
				armorstand.getEquipment().setItemInMainHand(CLOCK);
				NMS.removeBoundingBox(armorstand);
				armorstands.add(armorstand);
			}
		}
		
		@Override
		public void run(int count) {
			NMS.sendTitle(getPlayer(), "§c사망 지연", "§e" + df.format(duration / 20.0), 0, 20, 0);
		}
		
		@Override
		public void onEnd() {
			rewindHP = 0;
			rewindLocation = null;
			getPlayer().damage(Integer.MAX_VALUE);
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			NMS.clearTitle(getPlayer());
			for (ArmorStand armorstand : armorstands) {
				armorstand.remove();
			}
			armorstands.clear();
			stopped.clear();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer timeaccel = new AbilityTimer() {
		
		private double y;
		private boolean add;
		private int stack;
		
		@Override
		public void onStart() {
			faster.start();
			SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(getPlayer().getLocation(), 1, 1.75f);
		}
		
    	@Override
		public void run(int count) {
    		if (rewindHP < getPlayer().getHealth()) {
    			rewindHP = getPlayer().getHealth();
    			rewindLocation = getPlayer().getLocation();
    		}
    		
    		if (count % 2 == 0) {
    			double nowmultiply = 1 + (usedtime * 2 / (double) maxtime);
    			
    			if (add && y >= 0.8) add = false;
    			else if (!add && y <= 0) add = true;

    			y = add ? Math.min(0.8, y + (0.1 * nowmultiply)) : Math.max(0, y - (0.1 * nowmultiply));
    			stack = stack < 15 ? stack + 1 : 0;
    			
				for (Location loc : circle.toLocations(getPlayer().getLocation().add(0, y, 0))) {
					ParticleLib.REDSTONE.spawnParticle(loc, gradations.get(Math.max(0, stack - 1)));
				}
    		}
    		
    		if (usedtime < maxtime) {
    			usedtime++;
    			getPlayer().setWalkSpeed((float) (getPlayer().getWalkSpeed() + 0.0003));
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), usedtime * 0.0001, RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					Healths.setHealth(getPlayer(), getPlayer().getHealth() + (usedtime * 0.00015));
				}
    		} else stop(false);
    	}
    	
    	@Override
    	public void onEnd() {
    		rewindHP = 0;
    		rewindLocation = null;
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		slower.start();
    		getPlayer().setSprinting(false);
    		getPlayer().setWalkSpeed(0.2f);
    		cooldown = (int) (Wreck.isEnabled(GameManager.getGame()) ? (Math.max(((100 - Settings.getCooldownDecrease().getPercentage()) / 100.0), 0.75) * (usedtime * multiply)) : (usedtime * multiply));
    		usedtime = 0;
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer faster = new AbilityTimer(TaskType.REVERSE, 98) {
		
		@Override
		public void onStart() {
			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f);
		}
		
    	@Override
    	public void run(int count) {
    		switch(count) {
    		case 78:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 2f);
    			break;
    		case 60:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f);
    			break;
    		case 44:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 2f);
    			break;
    		case 30:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f);
    			break;
    		case 18:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 2f);
    			break;
    		case 8:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f);
    			break;
    		}
    	}
    	
    	@Override
    	public void onEnd() {
    		SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 2f);
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();

	private final AbilityTimer slower = new AbilityTimer(TaskType.NORMAL, 98) {
		
		@Override
		public void onStart() {
    		SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 2f);
		}
		
    	@Override
    	public void run(int count) {
    		switch(count) {
    		case 78:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 2f);
    			break;
    		case 60:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f);
    			break;
    		case 44:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 2f);
    			break;
    		case 30:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f);
    			break;
    		case 18:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 2f);
    			break;
    		case 8:
    			SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f);
    			break;
    		}
    	}
    	
    	@Override
    	public void onEnd() {
    		SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f);
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
}