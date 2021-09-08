package RainStarAbility;

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

import RainStarEffect.Burn;
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

@AbilityManifest(
		name = "�һ���", rank = Rank.S, species = Species.ANIMAL, explain = {
		"��7�нú� ��8- ��b�޹����� �����f: �Ͼ� ���� ���� ����ؼ� ����ٴϸ�",
		" ���� �ױ� ������ ���� ����� �������ָ� ��a������f�� �ݴϴ�.",
		"��7ö�� ��Ŭ�� ��8- ��c�һ��� ����f: $[DURATION]�ʰ� ��c�һ��� ����f�� ���� ���ϴ�.",
		" ��c�һ��� ����f�� ���� �ִ� ������ ��� ���⿡ ������ �ֺ� ���� ��b7.9�ʡ�f��ŭ",
		" �߰��� ���¿�� ����� �ٽ� �ǻ�Ƴ� �� ������, õõ�� ��b�����f�� �� �ֽ��ϴ�.",
		" �� �ɷ��� �� �� �� ��� �����ϸ�, ��3�ִ� ü���� 1/3�� �Ҹ��f�մϴ�.",
		" ���� ���� ���� ��c�߰� �������8(��7$[MAX_DAMAGE] - ��Ȱ�� Ƚ�� * $[DECREASE_DAMAGE]��8)��f�� ���������� ����ϴ�.",
		"��7�нú� ��8- ��c���ġ�f: ���� �� �����ڰ� �ڽ��� ������ �� ���� ������ ��",
		" ��c�һ��� ����f�� �밡 ���� ������ �ð����� �ٽ� �� �� ����� �� �ֽ��ϴ�.",
		"��b[��7���̵�� �����ڡ�b] ��fLotear"
		})

public class Phoenix extends AbilityBase implements ActiveHandler {

	public Phoenix(Participant participant) {
		super(participant);
	}
	
	private static final SettingObject<Double> MAX_DAMAGE = abilitySettings.new SettingObject<Double>(Phoenix.class,
			"max-damage", 4.0, "# �ִ� �߰� ���ݷ�") {

		@Override
		public boolean condition(Double arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Double> DECREASE_DAMAGE = abilitySettings.new SettingObject<Double>(Phoenix.class,
			"decrease-damage", 0.8, "# ��Ȱ �ø��� ���ݷ� ����") {

		@Override
		public boolean condition(Double arg0) {
			return arg0 >= 0.1;
		}

	};
	
	private static final SettingObject<Integer> HEALTH = abilitySettings.new SettingObject<Integer>(Phoenix.class,
			"health", 50, "# �Ͼ� ���� �⺻ ü��") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> HEAL_AMOUNT = abilitySettings.new SettingObject<Integer>(Phoenix.class,
			"heal-amount", 5, "# ������ ü�� ȸ����") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> DURATION = abilitySettings.new SettingObject<Integer>(Phoenix.class, 
			"duration", 15, "# �ɷ��� ���� �ð�") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	public static final SettingObject<Integer> CHANCE = abilitySettings.new SettingObject<Integer>(Phoenix.class,
			"chance", 20, "# �� ���� �� ���� Ȯ��", "# ����: n/100", "# 10���� �Է� �� 10/100, 10%�Դϴ�.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> ADD_CHANCE = abilitySettings.new SettingObject<Integer>(Phoenix.class,
			"add-chance", 15, "# ���� ���н� ���� Ȯ��ġ", "# ����: n/100", "# 10���� �Է� �� 10/100, 10%�Դϴ�.") {
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
				parrot.setCustomName("��e" + getPlayer().getName() + "��f�� ��");
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
			if (parrot != null) parrot.setHealth(0);
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
			ac.update("��c�߰� ���ݷ¡�7: ��e" + df.format(addDamage));
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
						getPlayer().sendMessage("��c[��6!��c] ��f�����ڰ� ��c�ѡ�f�� ���� ��a�ɷ��� �ٽ� ����� �� �ֽ��ϴ�.");
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
				e.getEntity().sendMessage("��a���� ������� ���� �ʽ��ϴ�.");
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
						getPlayer().sendMessage("��b�Ͼ� ����7 > ��f���� ��c�� " + df.format(p.getHealth() - e.getFinalDamage()) + "��f�� ü���� ���� �� ����.");
						break;
					case 1:
						if (getGame().getParticipant(p).hasAbility()) {
							AbilityBase ab = getGame().getParticipant(p).getAbility();
							if (ab.getClass().equals(Mix.class)) {
								Mix mix = (Mix) ab;
								AbilityBase first = mix.getFirst();
								AbilityBase second = mix.getSecond();
								getPlayer().sendMessage("��b�Ͼ� ����7 > ��f���� ��e�ɷ¡�f�� " + first.getRank().getRankName() + " " + first.getSpecies().getSpeciesName() + "��f + " +
								second.getRank().getRankName() + " " + second.getSpecies().getSpeciesName() + "��f�̾�.");
							} else getPlayer().sendMessage("��b�Ͼ� ����7 > ��f���� ��e�ɷ¡�f�� " + ab.getRank().getRankName() + " " + ab.getSpecies().getSpeciesName() + "��f�̾�.");	
						} else getPlayer().sendMessage("��b�Ͼ� ����7 > ��f���� ��e�ɷ¡�f�� ����.");
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
						if (potioncount == 0 && applecount == 0 && pearlcount == 0 && totemcount == 0) getPlayer().sendMessage("��b�Ͼ� ����7 > ��f���� �ƹ��� ��3�Ҹ� �����ۡ�f�� ������ ���� �ʾ�.");
						else getPlayer().sendMessage("��b�Ͼ� ����7 > ��f���� ��d" + potioncount + "��f���� ��d���ǡ�f, ��e" + applecount + "��f���� ��eȲ�� �����f, ��3"
								+ pearlcount + "��f���� ��3���� ���֡�f, ��a" + totemcount + "��f���� ��a�һ��� ���ۡ�f�� ������ �־�.");
						break;
					case 3:
					case 4:
						getPlayer().sendMessage("��b�Ͼ� ����7 > ��f����!");
						SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer().getLocation(), 0.75f, 1.45f);
						switch(random.nextInt(4)) {
						case 0:
							getPlayer().sendMessage("��b[��e!��b] ��aü���� �Ϻ� ȸ���˴ϴ�.");
							final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), healAmount, RegainReason.CUSTOM);
							Bukkit.getPluginManager().callEvent(event);
							if (!event.isCancelled()) {
								Healths.setHealth(getPlayer(), getPlayer().getHealth() + healAmount);
							}
							break;
						case 1:
							getPlayer().sendMessage("��b[��e!��b] ��b��ð� �ż� ������ �޽��ϴ�.");
							PotionEffects.SPEED.addPotionEffect(getPlayer(), 350, 1, true);
							break;
						case 2:
							getPlayer().sendMessage("��b[��e!��b] ��8��ð� ���� ������ �޽��ϴ�.");
							PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), 200, 1, true);
							break;
						case 3:
							getPlayer().sendMessage("��b[��e!��b] ��c�ش� ���ط��� 2��� �������ϴ�.");
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