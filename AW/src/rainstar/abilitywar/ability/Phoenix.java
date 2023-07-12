package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Parrot.Variant;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Wing;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.Burn;

@AbilityManifest(
		name = "불사조", rank = Rank.S, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §b앵무새의 조언§f: 하얀 새가 나를 계속해서 따라다니며",
		" 새가 죽기 전까지 내게 계속해 조언해주며 §a버프§f를 줍니다.",
		"§7철괴 좌클릭 §8- §c불사의 힘§f: $[DURATION]초간 §c불사의 힘§f이 몸에 깃듭니다.",
		" §c불사의 힘§f이 깃들고 있는 동안은 사망 위기에 빠져도 주변 적을 §b7.9초§f만큼",
		" 추가로 불태우며 계속해 다시 되살아날 수 있으며, 천천히 §b비행§f할 수 있습니다.",
		" 이 능력은 단 한 번 사용 가능하며, §3최대 체력을 1/3만큼 소모§f합니다.",
		" 이후 근접 공격 §c추가 대미지§8(§7$[MAX_DAMAGE] - 부활한 횟수 * $[DECREASE_DAMAGE]§8)§f를 영구적으로 얻습니다.",
		"§7패시브 §8- §c최후§f: 게임 내 참가자가 자신을 포함해 단 둘이 남았을 때",
		" §c불사의 힘§f을 대가 없이 절반의 시간으로 다시 한 번 사용할 수 있습니다.",
		"§b[§7아이디어 제공자§b] §fLotear"
		},
		summarize = {
		"적 타격 시 앵무새가 죽기 전까지 확률적으로 버프를 줍니다.",
		"§7철괴 좌클릭 시§f 비행할 수 있으며, 상시 §a불사의 토템§f이 발동합니다.",
		"이 스킬은 한 번만 사용 가능하며 최대 체력을 1/3만큼 소모합니다.",
		"만일 1:1 상황이 된다면 스킬을 대가 없이 절반 지속으로 한 번 더 사용 가능합니다."
		})

public class Phoenix extends AbilityBase implements ActiveHandler {

	public Phoenix(Participant participant) {
		super(participant);
	}
	
	private static final SettingObject<Double> MAX_DAMAGE = abilitySettings.new SettingObject<Double>(Phoenix.class,
			"max-damage", 4.0, "# 최대 추가 공격력") {

		@Override
		public boolean condition(Double arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Double> DECREASE_DAMAGE = abilitySettings.new SettingObject<Double>(Phoenix.class,
			"decrease-damage", 0.8, "# 부활 시마다 공격력 감소") {

		@Override
		public boolean condition(Double arg0) {
			return arg0 >= 0.1;
		}

	};
	
	private static final SettingObject<Integer> HEALTH = abilitySettings.new SettingObject<Integer>(Phoenix.class,
			"health", 50, "# 하얀 새의 기본 체력") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> HEAL_AMOUNT = abilitySettings.new SettingObject<Integer>(Phoenix.class,
			"heal-amount", 5, "# 응원의 체력 회복량") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> DURATION = abilitySettings.new SettingObject<Integer>(Phoenix.class, 
			"duration", 15, "# 능력의 지속 시간") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	public static final SettingObject<Integer> CHANCE = abilitySettings.new SettingObject<Integer>(Phoenix.class,
			"chance", 20, "# 적 공격 시 조언 확률", "# 기준: n/100", "# 10으로 입력 시 10/100, 10%입니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> ADD_CHANCE = abilitySettings.new SettingObject<Integer>(Phoenix.class,
			"add-chance", 15, "# 조언 실패시 증가 확률치", "# 기준: n/100", "# 10으로 입력 시 10/100, 10%입니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final DecimalFormat df = new DecimalFormat("0.00");
	private boolean onetime = true;
	private boolean activeOnetime = true;
	private boolean onemorechance = false;
	private boolean revive = false;
	private ActionbarChannel ac = newActionbarChannel();
	
	private static final Vector up = new Vector(0, 1.5, 0);
	
	private Parrot parrot;
	private final double maxDamage = MAX_DAMAGE.getValue();
	private final double decDamage = DECREASE_DAMAGE.getValue();
	private final int birdHealth = HEALTH.getValue();
	private final int healAmount = HEAL_AMOUNT.getValue();
	private final int defaultChance = CHANCE.getValue();
	private final int addChance = ADD_CHANCE.getValue();
	private int chance = defaultChance;
	private int revivecount = 0;
	private double addDamage = 0;
	private double firstMaxHealth;
	private double nowMaxHealth = 0;
	
	private final List<Participant> participants = new ArrayList<>(getGame().getParticipants());
	
	private static final Pair<Wing, Wing> WING_DARK = Wing.of(new boolean[][]{
		{true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{true, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{true, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, true, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, true, true, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{true, false, false, true, true, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false},
		{true, false, false, false, false, true, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false},
		{false, true, false, false, false, false, false, true, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false},
		{false, true, true, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false},
		{false, false, true, true, true, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, true, false, false, true, true, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false},
		{false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false, false, false},
		{false, false, false, true, false, true, true, false, false, true, false, false, false, false, false, false, false, false, true, true, false, false, false},
		{false, false, false, true, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, true, true, false},
		{false, false, false, false, true, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true},
		{false, false, false, false, true, false, true, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, true},
		{false, false, false, false, true, false, false, false, true, true, false, false, false, false, true, false, false, false, false, false, false, false, true},
		{false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, false, false, false, false, false, false, true, false},
		{false, false, false, false, false, false, true, true, true, false, false, true, true, false, false, false, true, false, false, false, true, false, false},
		{false, false, false, false, false, false, false, false, false, true, true, false, false, false, false, false, true, false, false, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, true, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, true, true, true, true, false, true, false, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false, false, false, false}
		});
	
	private static final Pair<Wing, Wing> WING_NORMAL = Wing.of(new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, true, false, false, false, false, false, true, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, false, false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, false, false, false, true, false, false, true, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, true, false, false, false, true, true, true, true, false, false, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, true, true, false, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, true, true, true, false, true, false, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, false, false, true, true, true, false, false, false, true, false, false, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, true, true, false, false, true, false, false, true, true, false, false, false, false, false, false, true, false},
		{false, false, false, false, false, false, false, false, true, true, false, false, false, false, true, false, false, true, false, false, false, true, false},
		{false, false, false, false, false, true, false, false, false, false, false, false, false, true, false, true, true, false, false, true, false, false, false},
		{false, false, false, false, false, false, true, true, true, false, false, true, true, false, false, false, true, true, true, false, true, false, false},
		{false, false, false, false, false, false, false, false, false, true, true, false, false, false, false, true, false, false, false, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, true, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, true, true, true, true, true, false, false, false, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
		});
	
	private static final Pair<Wing, Wing> WING_LIGHT = Wing.of(new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, true, true, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, true, true, true, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, true, true, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, true, true, false, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, true, true, true, false, true, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, true, true, false, false, true, true, false, false, true, true, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, false, false, false, false, true, true, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, true, true, false, false, true, true, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, false, false, false, true, false, true, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, true, false, false, false, true, true, true, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, true, true, false, false, false, true, true, true, true, true, true, false, false, false},
		{false, false, false, false, false, false, false, false, true, true, false, false, true, false, false, true, true, true, true, true, true, false, false},
		{false, false, false, false, false, true, false, false, false, false, false, true, true, true, false, true, true, false, true, true, true, false, false},
		{false, false, false, false, false, false, true, true, false, false, true, true, true, false, false, false, false, true, true, false, true, true, false},
		{false, false, false, false, false, false, false, false, false, true, true, false, false, false, true, true, false, false, false, true, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, true, true, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, true, true, true, true, false, false, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
		});
	
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
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (onetime) {
				firstMaxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				parrot = getPlayer().getWorld().spawn(getPlayer().getLocation(), Parrot.class);
				parrot.setOwner(getPlayer());
				parrot.setVariant(Variant.GRAY);
				parrot.setAdult();
				parrot.setTamed(true);
				parrot.setCustomName("§e" + getPlayer().getName() + "§f의 새");
				parrot.setCustomNameVisible(true);
				parrot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(1, birdHealth));
				parrot.setHealth(Math.max(1, birdHealth));
				onetime = false;
			}
			if (nowMaxHealth != 0) {
				getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(nowMaxHealth);	
			}
		}
		if (update == Update.RESTRICTION_SET || update == Update.ABILITY_DESTROY) {
			if (firstMaxHealth >= 1) {
				getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(firstMaxHealth);	
			}
		}
		if (update == Update.ABILITY_DESTROY) {
			if (parrot != null && !parrot.isDead()) parrot.setHealth(0);
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT)) {
	    	if (clicktype.equals(ClickType.LEFT_CLICK)) {
	    		if (activeOnetime) {
		    		activeOnetime = false;
		    		return skill.start();	
	    		} else if (onemorechance) {
	    			return skill.start();
	    		}
	    	}
	    }
		return false;
	}
	
	private final Duration skill = new Duration(DURATION.getValue() * 20) {
		
		private Wing leftWing1, leftWing2, leftWing3,
					rightWing1, rightWing2, rightWing3;
		private RGB red = RGB.of(200, 75, 28), orange = RGB.of(253, 140, 28), yellow = RGB.of(254, 236, 140),
				black = RGB.of(15, 50, 105), gray = RGB.of(123, 148, 177), white = RGB.of(231, 246, 249);
		private Random random = new Random();
		private int randomnum = 0;
		
		@Override
		protected void onDurationStart() {
			this.leftWing1 = WING_DARK.getLeft().clone();
			this.rightWing1 = WING_DARK.getRight().clone();
			this.leftWing2 = WING_NORMAL.getLeft().clone();
			this.rightWing2 = WING_NORMAL.getRight().clone();
			this.leftWing3 = WING_LIGHT.getLeft().clone();
			this.rightWing3 = WING_LIGHT.getRight().clone();
			if (!onemorechance) {
				double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth * 2 / 3);
				nowMaxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();	
			} else {
    			onemorechance = false;
				this.setCount(DURATION.getValue() * 10);
			}
			
			SoundLib.ENTITY_BAT_DEATH.playSound(getPlayer().getLocation(), 2.5f, 0.65f);
			SoundLib.ENTITY_BAT_DEATH.playSound(getPlayer().getLocation(), 2.5f, 0.65f);
			SoundLib.ENTITY_DRAGON_FIREBALL_EXPLODE.playSound(getPlayer().getLocation(), 2.5f, 0.65f);
			ParticleLib.FLAME.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 150, 0.35);
			
			getPlayer().getWorld().createExplosion(getPlayer().getLocation(), 2f, false, false);
			getPlayer().setVelocity(up);
			
			getPlayer().setAllowFlight(true);
			
			randomnum = random.nextInt(10);
		}
		
		@Override
		protected void onDurationProcess(int count) {
			if (count % 10 == 0) {
				float yaw = getPlayer().getLocation().getYaw();
				if (!revive) {
					if (getPlayer().getName().equals("Lotear")) {
						for (Location loc : leftWing1.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
							ParticleLib.REDSTONE.spawnParticle(loc, black);
						}
						leftWing1.rotateAroundAxisY(yaw);
						for (Location loc : leftWing2.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
							ParticleLib.REDSTONE.spawnParticle(loc, gray);
						}
						leftWing2.rotateAroundAxisY(yaw);
						for (Location loc : leftWing3.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
							ParticleLib.REDSTONE.spawnParticle(loc, white);
						}
						leftWing3.rotateAroundAxisY(yaw);
						for (Location loc : rightWing1.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
							ParticleLib.REDSTONE.spawnParticle(loc, black);
						}
						rightWing1.rotateAroundAxisY(yaw);
						for (Location loc : rightWing2.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
							ParticleLib.REDSTONE.spawnParticle(loc, gray);
						}
						rightWing2.rotateAroundAxisY(yaw);
						for (Location loc : rightWing3.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
							ParticleLib.REDSTONE.spawnParticle(loc, white);
						}
						rightWing3.rotateAroundAxisY(yaw);
					} else {
						if (randomnum <= 1) {
							for (Location loc : leftWing1.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, black);
							}
							leftWing1.rotateAroundAxisY(yaw);
							for (Location loc : leftWing2.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, gray);
							}
							leftWing2.rotateAroundAxisY(yaw);
							for (Location loc : leftWing3.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, white);
							}
							leftWing3.rotateAroundAxisY(yaw);
							for (Location loc : rightWing1.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, black);
							}
							rightWing1.rotateAroundAxisY(yaw);
							for (Location loc : rightWing2.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, gray);
							}
							rightWing2.rotateAroundAxisY(yaw);
							for (Location loc : rightWing3.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, white);
							}
							rightWing3.rotateAroundAxisY(yaw);
						} else {
							for (Location loc : leftWing1.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, red);
							}
							leftWing1.rotateAroundAxisY(yaw);
							for (Location loc : leftWing2.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, orange);
							}
							leftWing2.rotateAroundAxisY(yaw);
							for (Location loc : leftWing3.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, yellow);
							}
							leftWing3.rotateAroundAxisY(yaw);
							for (Location loc : rightWing1.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, red);
							}
							rightWing1.rotateAroundAxisY(yaw);
							for (Location loc : rightWing2.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, orange);
							}
							rightWing2.rotateAroundAxisY(yaw);
							for (Location loc : rightWing3.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
								ParticleLib.REDSTONE.spawnParticle(loc, yellow);
							}
							rightWing3.rotateAroundAxisY(yaw);
						}
					}
				} else {
					for (Location loc : leftWing1.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
						ParticleLib.REDSTONE.spawnParticle(loc, red);
					}
					leftWing1.rotateAroundAxisY(yaw);
					for (Location loc : leftWing2.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
						ParticleLib.REDSTONE.spawnParticle(loc, orange);
					}
					leftWing2.rotateAroundAxisY(yaw);
					for (Location loc : leftWing3.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
						ParticleLib.REDSTONE.spawnParticle(loc, yellow);
					}
					leftWing3.rotateAroundAxisY(yaw);
					for (Location loc : rightWing1.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
						ParticleLib.REDSTONE.spawnParticle(loc, red);
					}
					rightWing1.rotateAroundAxisY(yaw);
					for (Location loc : rightWing2.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
						ParticleLib.REDSTONE.spawnParticle(loc, orange);
					}
					rightWing2.rotateAroundAxisY(yaw);
					for (Location loc : rightWing3.rotateAroundAxisY(-yaw).toLocations(getPlayer().getLocation().clone().subtract(0, 0.5, 0))) {
						ParticleLib.REDSTONE.spawnParticle(loc, yellow);
					}
					rightWing3.rotateAroundAxisY(yaw);
				}
			}
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			revive = false;
			addDamage = Math.max(0, addDamage + (maxDamage - (decDamage * revivecount)));
			ac.update("§c추가 공격력§7: §e" + df.format(addDamage));
			revivecount = 0;
			getPlayer().setAllowFlight(getPlayer().getGameMode() != GameMode.SURVIVAL && getPlayer().getGameMode() != GameMode.ADVENTURE);
			getPlayer().setFlying(getPlayer().getGameMode() != GameMode.SURVIVAL && getPlayer().getGameMode() != GameMode.ADVENTURE);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	@SubscribeEvent(priority = 99)
	public void onEntityResurrectEvent(EntityResurrectEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (skill.isRunning()) {
				ItemStack leftHand = getPlayer().getInventory().getItemInOffHand();
				e.setCancelled(false);
				getPlayer().getInventory().setItemInOffHand(leftHand);
				for (LivingEntity livingEntity : LocationUtil.getNearbyEntities(LivingEntity.class, getPlayer().getLocation(), 10, 10, predicate)) {
					if (!livingEntity.equals(parrot)) {
						livingEntity.setFireTicks(livingEntity.getFireTicks() + 158);
						if (livingEntity instanceof Player) {
							Burn.apply(getGame().getParticipant((Player) livingEntity), TimeUnit.TICKS, 79);
						}
					}
				}
				ParticleLib.FLAME.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 250, 0.15);
				revive = true;
				revivecount++;
			}
		}
	}
	
	@SubscribeEvent(priority = 99)
	private void onPlayerDeath(PlayerDeathEvent e) {
		if (!e.getEntity().equals(getPlayer())) {
			if (getGame() instanceof DeathManager.Handler) {
				final DeathManager.Handler game = (DeathManager.Handler) getGame();
				if (game.getDeathManager().isExcluded(e.getEntity().getUniqueId())) {
					participants.remove(getGame().getParticipant(e.getEntity().getUniqueId()));
					if (participants.size() == 2) {
						onemorechance = true;
						getPlayer().sendMessage("§c[§6!§c] §f생존자가 §c둘§f만 남아 §a능력을 다시 사용할 수 있습니다.");
						SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer().getLocation(), 0.75f, 1.75f);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (e.getCause().equals(DamageCause.FALL)) {
				e.setCancelled(true);
				e.getEntity().sendMessage("§a낙하 대미지를 받지 않습니다.");
				SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound((Player) e.getEntity());
			}
			if (skill.isRunning()) {
				if (e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION)) {
					e.setCancelled(true);
				}	
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (e.getDamager().equals(getPlayer())) {
			e.setDamage(e.getDamage() + addDamage);
			if (e.getEntity() instanceof Player && !parrot.isDead()) {
				Player p = (Player) e.getEntity();
				Random random = new Random();
				if ((random.nextInt(100) + 1) <= chance) {
					SoundLib.ENTITY_PARROT_AMBIENT.playSound(getPlayer().getLocation());
					switch(random.nextInt(5)) {
					case 0:
						getPlayer().sendMessage("§b하얀 새§7 > §f적은 §c♥ " + df.format(p.getHealth() - e.getFinalDamage()) + "§f의 체력이 남은 것 같아.");
						break;
					case 1:
						if (getGame().getParticipant(p).hasAbility()) {
							AbilityBase ab = getGame().getParticipant(p).getAbility();
							if (ab.getClass().equals(Mix.class)) {
								Mix mix = (Mix) ab;
								AbilityBase first = mix.getFirst();
								AbilityBase second = mix.getSecond();
								getPlayer().sendMessage("§b하얀 새§7 > §f적의 §e능력§f은 " + first.getRank().getRankName() + " " + first.getSpecies().getSpeciesName() + "§f + " +
								second.getRank().getRankName() + " " + second.getSpecies().getSpeciesName() + "§f이야.");
							} else getPlayer().sendMessage("§b하얀 새§7 > §f적의 §e능력§f은 " + ab.getRank().getRankName() + " " + ab.getSpecies().getSpeciesName() + "§f이야.");	
						} else getPlayer().sendMessage("§b하얀 새§7 > §f적은 §e능력§f이 없어.");
						break;
					case 2:
						Inventory inventory = p.getPlayer().getInventory();
						List<ItemStack> list = new CopyOnWriteArrayList<>(inventory.getContents());
						int potioncount = 0;
						int applecount = 0;
						int pearlcount = 0;
						int totemcount = 0;
						for (ItemStack itemStack : list) {
							if (itemStack == null) {
								continue;
							}
							if (itemStack.getType() == Material.POTION || itemStack.getType() == Material.LINGERING_POTION ||
									itemStack.getType() == Material.SPLASH_POTION || itemStack.getType() == Material.ENDER_PEARL
									|| itemStack.getType() == Material.GOLDEN_APPLE || itemStack.getType() == MaterialX.TOTEM_OF_UNDYING.getMaterial()) {
								if (itemStack.getType() == Material.POTION || itemStack.getType() == Material.LINGERING_POTION ||
									itemStack.getType() == Material.SPLASH_POTION) {
									potioncount++;
									list.remove(itemStack);	
								} else if (itemStack.getType() == Material.ENDER_PEARL) {
									pearlcount++;
									list.remove(itemStack);	
								} else if (itemStack.getType() == Material.GOLDEN_APPLE) {
									applecount++;
									list.remove(itemStack);	
								} else if (itemStack.getType() == Material.TOTEM_OF_UNDYING) {
									totemcount++;
									list.remove(itemStack);	
								}
							}
						}
						if (potioncount == 0 && applecount == 0 && pearlcount == 0 && totemcount == 0) getPlayer().sendMessage("§b하얀 새§7 > §f적은 아무런 §3소모성 아이템§f을 가지고 있지 않아.");
						else getPlayer().sendMessage("§b하얀 새§7 > §f적은 §d" + potioncount + "§f개의 §d포션§f, §e" + applecount + "§f개의 §e황금 사과§f, §3"
								+ pearlcount + "§f개의 §3엔더 진주§f, §a" + totemcount + "§f개의 §a불사의 토템§f을 가지고 있어.");
						break;
					case 3:
					case 4:
						getPlayer().sendMessage("§b하얀 새§7 > §f힘내!");
						SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer().getLocation(), 0.75f, 1.45f);
						switch(random.nextInt(4)) {
						case 0:
							getPlayer().sendMessage("§b[§e!§b] §a체력이 일부 회복됩니다.");
							final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), healAmount, RegainReason.CUSTOM);
							Bukkit.getPluginManager().callEvent(event);
							if (!event.isCancelled()) {
								Healths.setHealth(getPlayer(), getPlayer().getHealth() + event.getAmount());
							}
							break;
						case 1:
							getPlayer().sendMessage("§b[§e!§b] §b잠시간 신속 버프를 받습니다.");
							PotionEffects.SPEED.addPotionEffect(getPlayer(), 350, 1, true);
							break;
						case 2:
							getPlayer().sendMessage("§b[§e!§b] §8잠시간 저항 버프를 받습니다.");
							PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), 200, 1, true);
							break;
						case 3:
							getPlayer().sendMessage("§b[§e!§b] §c해당 피해량을 2배로 입혔습니다.");
							e.setDamage(e.getDamage() * 2);
							break;
						}
						break;
					}
					chance = defaultChance;
				} else {
					chance += addChance;
				}	
			}
		}
	}
	
}