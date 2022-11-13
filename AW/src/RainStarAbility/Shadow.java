package RainStarAbility;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityActiveSkillEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.manager.effect.Fear;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;
import kotlin.ranges.RangesKt;

@AbilityManifest(name = "섀도우", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7패시브 §8- §9가장 오래된 감정§f: §b공포 상태§f의 적에게 주는 피해가 $[FEAR_DAMAGE_INCREASE]% 증가합니다.",
		"§7검 우클릭 §8- §5흑마술§f: $[DARKARTS_DURATION]초간 내게 피해를 주는 대상은 $[DARKARTS_FEAR]초간 §b공포 상태§f가 됩니다.",
		" §5흑마술§f이 끝날 때, §b공포§f를 준 대상 수 × §d$[HEAL_AMOUNT]HP§f를 §d회복§f합니다. $[DARKARTS_COOLDOWN]",
		"§7철괴 우클릭 §8- §c그림자 베기§f: $[SHADOW_DURATION]초간 §3그림자 상태§f가 되어, 적을 근접 공격하면",
		" 대상의 방향으로 짧게 돌진 이후 $[RANGE]칸 내의 적들을 $[SHADOW_FEAR]초간 §b공포§f에 빠뜨립니다.",
		" 이때 적을 처치하면 §3그림자 상태§f를 $[SHADOW_ADD_DURATION]초 증가시키고, §3그림자 상태§f가 끝날 때",
		" 천천히 줄어드는 §c공격력 $[SHADOW_DAMAGE_INCREASE]% × 처치 수§f를 획득합니다. $[SHADOW_COOLDOWN]",
		"§b[§7아이디어 제공자§b] §dspace_kdd"
		},
		summarize = {
		"§b공포§f에 빠진 적에게 주는 피해량이 증가합니다.",
		"§7검 우클릭§f 시, $[DARKARTS_DURATION]초간 내게 피해를 주는 대상이 §b공포§f에 빠집니다.",
		"효과 종료 시, §b공포§f를 건 대상의 수에 비례해 체력을 회복합니다.",
		"§7철괴 우클릭§f 시, $[SHADOW_DURATION]초간 §3그림자 상태§f로 적을 근접 공격해",
		"대상의 방향으로 질주하고 주변을 짧은 §b공포§f에 빠뜨립니다.",
		"효과 종료 시, 처치한 적의 수만큼 천천히 줄어드는 추가 공격력을 획득합니다."
		})
public class Shadow extends AbilityBase implements ActiveHandler {

	public Shadow(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> FEAR_DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Integer>(Shadow.class, "fear-damage-increase", 20,
			"# 공포 대상에게 피해량 증가 수치") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	public static final SettingObject<Double> DARKARTS_DURATION = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "darkarts-duration", 5.0,
			"# 흑마술 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> DARKARTS_FEAR = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "darkarts-fear", 1.5,
			"# 흑마술 공포 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> HEAL_AMOUNT = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "heal-amount", 4.0,
			"# 흑마술 종료 시 회복량") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> DARKARTS_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Shadow.class, "darkarts-cooldown", 70,
			"# 흑마술 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
		
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Double> SHADOW_DURATION = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "shadow-duration", 10.0,
			"# 그림자 베기 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "range", 2.0,
			"# 그림자 베기 범위") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> SHADOW_FEAR = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "shadow-fear", 1.0,
			"# 흑마술 공포 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> SHADOW_ADD_DURATION = 
			abilitySettings.new SettingObject<Double>(Shadow.class, "shadow-duration", 5.0,
			"# 그림자 베기 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> SHADOW_DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Integer>(Shadow.class, "shadow-damage-increase", 30,
			"# 그림자 베기 적 처치 공격력 증가 수치") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	public static final SettingObject<Integer> SHADOW_COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Shadow.class, "shadow-cooldown", 110,
			"# 그림자 베기 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
		
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	private final double feardamageincrease = 1 + (FEAR_DAMAGE_INCREASE.getValue() * 0.01);
	private final int shadowdamageincrease = SHADOW_DAMAGE_INCREASE.getValue();	
	private final int darkartsduration = (int) (DARKARTS_DURATION.getValue() * 20);
	private final int darkartsfear = (int) (DARKARTS_FEAR.getValue() * 20);
	private final int shadowduration = (int) (SHADOW_DURATION.getValue() * 20);
	private final int shadowaddduration = (int) (SHADOW_ADD_DURATION.getValue() * 20);
	private final int shadowfear = (int) (SHADOW_FEAR.getValue() * 20);	
	private final double healamount = HEAL_AMOUNT.getValue();
	private final double range = RANGE.getValue();
	private final Cooldown darkartscool = new Cooldown(DARKARTS_COOLDOWN.getValue());
	private final Cooldown shadowcool = new Cooldown(SHADOW_COOLDOWN.getValue());
	
	private final static RGB color = RGB.of(1, 128, 128), startColor = RGB.of(1, 1, 64), endColor = RGB.of(64, 1, 64);
	private final static List<RGB> gradations = Gradient.createGradient(30, startColor, endColor);
	private Circle circle = Circle.of(range, (int) (range * 12));
	private Set<UUID> attackers = new HashSet<>();
	private BossBar bossBar = null;
	private int killed = 0;
	private double addDamage = 0;
	private ActionbarChannel ac = newActionbarChannel(), ac2 = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private final Crescent crescent = Crescent.of(1.5, 15);
	private Random random = new Random();
	
	private static final Set<Material> swords;
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
			if (entity.equals(getPlayer()))
				return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
								.getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !shadow.isRunning() && !shadowcool.isCooldown()) {
	    	return shadow.start();
	    }
	    return false;
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (damager != null) {
			if (getPlayer().equals(damager)) {
				if (e.getEntity() instanceof Player && getGame().isParticipating((Player) e.getEntity())) {
					Participant p = getGame().getParticipant((Player) e.getEntity());
					if (p.hasEffect(Fear.registration)) {
						e.setDamage(e.getDamage() * feardamageincrease);
						SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(getPlayer().getLocation(), 0.35f, 2);
						ParticleLib.ITEM_CRACK.spawnParticle(p.getPlayer().getLocation(), .5f, 1f, .5f, 100, 0.35, MaterialX.BLACK_CONCRETE);
					}	
				}
				
				if (addDamage > 0) e.setDamage(e.getDamage() * (1 + (addDamage * 0.01)));
			}
			
			if (darkarts.isRunning() && e.getEntity().equals(getPlayer()) && !getPlayer().equals(damager) && getGame().isParticipating(damager)) {
	            ParticleLib.SMOKE_LARGE.spawnParticle(damager.getLocation(), 0.25, 0, 0.25, 50, 0.4);
				SoundLib.ENTITY_VEX_CHARGE.playSound(damager, 1, 0.65f);
				Fear.apply(getGame().getParticipant(damager), TimeUnit.TICKS, darkartsfear, getPlayer());
				attackers.add(damager.getUniqueId());
			}
			
			if (shadow.isRunning() && getPlayer().equals(e.getDamager())) {
				SoundLib.ITEM_ARMOR_EQUIP_CHAIN.playSound(getPlayer().getLocation(), 1, 1.5f);
				new CutParticle(180).start();
				getPlayer().setVelocity(e.getEntity().getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().multiply(0.75).setY(0));
				new BukkitRunnable() {
					@Override
					public void run() {
						for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
							ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
						}
						for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate)) {
							Fear.apply(getGame().getParticipant(player), TimeUnit.TICKS, shadowfear, getPlayer());
						}
					}	
				}.runTaskLater(AbilityWar.getPlugin(), 5L);
			}
		}
		
	}
	
	@SubscribeEvent
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (shadow.isRunning() && e.getPlayer().getKiller().equals(getPlayer())) {
			shadow.setCount(shadow.getCount() + shadowaddduration);
			killed++;
		}
	}
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getPlayer().equals(getPlayer()) && (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) && e.getItem() != null && swords.contains(e.getItem().getType()) && !darkarts.isRunning() && !darkartscool.isCooldown()) {
			final AbilityActiveSkillEvent event = new AbilityActiveSkillEvent(this, e.getItem().getType(), ClickType.RIGHT_CLICK);
			Bukkit.getPluginManager().callEvent(event);
			getPlayer().sendMessage("§d능력을 사용하였습니다.");
			darkarts.start();
		}
	}
	
	private AbilityTimer darkarts = new AbilityTimer(darkartsduration) {
		
		@Override
		public void onStart() {
			SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(getPlayer().getLocation(), 1, 2);
			ParticleLib.REVERSE_PORTAL.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0, 0, 0, 50, 1);
		}
		
		@Override
		public void run(int count) {
			switch(random.nextInt(4)) {
			case 0:
				ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.5, 1, 0.5, 1, 0, MaterialX.PURPLE_STAINED_GLASS);
				break;
			case 1:
				ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.5, 1, 0.5, 1, 0, MaterialX.GRAY_STAINED_GLASS_PANE);
				break;
			case 2:
				ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.5, 1, 0.5, 1, 0, MaterialX.MAGENTA_STAINED_GLASS_PANE);
				break;
			case 3:
				ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.5, 1, 0.5, 1, 0, MaterialX.BLACK_STAINED_GLASS_PANE);
				break;
			}
			ac.update("§5흑마술§f: §d" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), healamount * attackers.size(), RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				Healths.setHealth(getPlayer(), getPlayer().getHealth() + (healamount * attackers.size()));
				SoundLib.BLOCK_ANVIL_LAND.playSound(getPlayer().getLocation(), 1, 1.77f);
				ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), .5f, 1f, .5f, 100, 0.35, MaterialX.REDSTONE);
			}
			attackers.clear();
			ac.update(null);
			darkartscool.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer shadow = new AbilityTimer(shadowduration) {
		
		private double y = 1;
		private boolean yUp = false;
		private int stack = 0;
		private boolean reverse = false;
		private Location location;
		
		@Override
		public void onStart() {
			location = getPlayer().getLocation();
			SoundLib.ENTITY_PIG_SADDLE.playSound(getPlayer().getLocation(), 1, 0.85f);
		}
		
		@Override
		public void run(int count) {
			ac2.update("§9그림자§f: §3" + df.format(count / 20.0) + "§f초");
			
			double angle = Math.toRadians(count * 5);
			double x = Math.cos(angle);
			double z = Math.sin(angle);
				
			if (y >= 1.6) yUp = false;
			else if (y <= 0.4) yUp = true;
			stack = (reverse ? -1 : 1) + stack;
			if ((stack >= 29 && !reverse) || (stack <= 0 && reverse)) reverse = !reverse;
				
			y = yUp ? Math.min(1.6, y + 0.01) : Math.max(0.4, y - 0.01);
				
			location = getPlayer().getLocation().clone().add(x, y, z);   	
			ParticleLib.SMOKE_LARGE.spawnParticle(location, 0, 0, 0, 1, 0);
			ParticleLib.REDSTONE.spawnParticle(location, gradations.get(stack));
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			SoundLib.ENTITY_BAT_TAKEOFF.playSound(getPlayer().getLocation(), 1, 0.7f);
			ac2.update(null);
			shadowcool.start();
			if (killed > 0) {
				addDamage = addDamage + (killed * shadowdamageincrease);
				damageAdder.start();
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer damageAdder = new AbilityTimer() {
		
		double maxDamage;
		
    	@Override
    	public void onStart() {
    		maxDamage = addDamage;
    		bossBar = Bukkit.createBossBar("§c공격력 증가", BarColor.RED, BarStyle.SOLID);
    		bossBar.setProgress(addDamage / maxDamage);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		addDamage = addDamage - 0.05;
    		bossBar.setTitle("§c공격력 증가 §7: §e" + df.format(addDamage));
			bossBar.setProgress(RangesKt.coerceIn(addDamage / maxDamage, 0, 1));
			if (addDamage <= 0) stop(false);
    	}
    	
		@Override
		public void onEnd() {
			addDamage = 0;
			bossBar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			addDamage = 0;
			bossBar.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private class CutParticle extends AbilityTimer {

		private final Vector vector;
		private final Vectors crescentVectors;

		private CutParticle(double angle) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180);
		}

		@Override
		protected void run(int count) {
			Location baseLoc = getPlayer().getLocation().clone().add(vector).add(0, 1, 0);
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				switch(random.nextInt(3)) {
				case 0:
					ParticleLib.ITEM_CRACK.spawnParticle(loc, 0.5, 1, 0.5, 1, 0, MaterialX.BLACK_CONCRETE);
					break;
				case 1:
					ParticleLib.ITEM_CRACK.spawnParticle(loc, 0.5, 1, 0.5, 1, 0, MaterialX.OBSIDIAN);
					break;
				case 2:
					ParticleLib.ITEM_CRACK.spawnParticle(loc, 0.5, 1, 0.5, 1, 0, MaterialX.BLACK_STAINED_GLASS_PANE);
					break;
				}
			}
		}

	}
	
}
