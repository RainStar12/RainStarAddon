package RainStarAbility.theonering;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "절대반지", rank = Rank.S, species = Species.HUMAN, explain = {
		"철괴 우클릭 시 §7은신§f하여 능력의 지정 대상이 되지 않습니다.",
		"§7은신§f 간 공격력이 $[INCREASE]% 증가하고, 신속 $[AMPLIFIER] 효과를 얻습니다.",
		"또한 지속적으로 §5광분 수치§f가 쌓입니다. $[RANGE]칸 내 플레이어가 있다면 3배로 쌓입니다.",
		"§5광분 수치§f가 최대치에 달하면 무작위로 화면이 전환되고, 피해를 입습니다.",
		"반지를 해제 시 매우 천천히 수치가 내려갑니다. §8(§cW§oR§eE§aC§bK §7적용§8)",
		"§3[§b종족야생 콜라보 능력§3]"
		},
		summarize = {
		"철괴 우클릭으로 §7은신§f 상태를 ON / OFF 가능합니다.",
		"§7은신§f 중에는 §c공격력 증가§f 및 §b신속§f 버프를 획득합니다.",
		"다만 §7은신§f 상태를 오래 지속할 경우 피해를 입습니다."
		})

public abstract class AbstractTheOneRing extends AbilityBase implements ActiveHandler {

	public AbstractTheOneRing(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> AMPLIFIER = 
			abilitySettings.new SettingObject<Integer>(AbstractTheOneRing.class, "amplifier", 0,
            "# 신속 효과 계수", "# 주의! 0부터 시작합니다.", "# 0일 때 포션 효과 계수는 1레벨,", "# 1일 때 포션 효과 계수는 2레벨입니다.") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
		@Override
		public String toString() {
			return "" + (1 + getValue());
        }
    };
	
	public static final SettingObject<Integer> INCREASE = 
			abilitySettings.new SettingObject<Integer>(AbstractTheOneRing.class, "damage-increase", 20,
            "# 은신 중 공격력 증가 수치", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Double> ADD_CRAZY = 
			abilitySettings.new SettingObject<Double>(AbstractTheOneRing.class, "add-crazy", 1.5,
			"# 0.05초당 증가하는 광분 수치", "# 1000이 되면 이후부터는 피해를 입습니다.") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Double> SUBTRACT_CRAZY = 
			abilitySettings.new SettingObject<Double>(AbstractTheOneRing.class, "subtract-crazy", 0.5,
			"# 0.05초당 감소하는 광분 수치", "# 1000이 되면 이후부터는 피해를 입습니다.", "# WRECK에 의한 영향을 받습니다.") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(AbstractTheOneRing.class, "range", 5.0,
			"# 플레이어 감지 거리", "# 단위: 칸") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
		
	};
	
	private final double increase = 1 + (INCREASE.getValue() * 0.01);
	private PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 200, AMPLIFIER.getValue(), true, false);
	private final Random random = new Random();
	private final double range = RANGE.getValue();
	private final double addcrazy = (ADD_CRAZY.getValue() * 0.001);
	private final double subtractcrazy = (SUBTRACT_CRAZY.getValue() * 0.001);
	private double crazy = 0;
	private BossBar bossBar = null;
	
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
	
	protected abstract void hide0();

	protected abstract void show0();

	private void hide() {
		this.hiding = true;
		getParticipant().attributes().TARGETABLE.setValue(false);
		hide0();
	}

	private void show() {
		this.hiding = false;
		getPlayer().removePotionEffect(PotionEffectType.SPEED);
		getParticipant().attributes().TARGETABLE.setValue(true);
		show0();
	}

	private boolean hiding = false;

	public boolean isHiding() {
		return hiding;
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_SET || update == Update.ABILITY_DESTROY) {
			show();
		}
		
		if (update == Update.RESTRICTION_CLEAR) {
			crazyupdater.start();
		}
	}
	
	private final AbilityTimer crazyupdater = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("광분", BarColor.PURPLE, BarStyle.SOLID);
    		bossBar.setProgress(crazy);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		if (hiding) {
    			getPlayer().addPotionEffect(speed);
    			crazy = Math.min(1, crazy + addcrazy);
    			if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate) != null) crazy = Math.min(1, crazy + (addcrazy * 2));
    		} else {
    			if (Wreck.isEnabled(GameManager.getGame())) {
    				final CooldownDecrease cooldownDecrease = Settings.getCooldownDecrease();
    				if (cooldownDecrease == CooldownDecrease._100) {
    					crazy = 0;
    				} else {
    					crazy = Math.max(0, crazy - ((1 + (cooldownDecrease.getPercentage() * 0.01)) * subtractcrazy)); 
    				}
    			} else crazy = Math.max(0, crazy - subtractcrazy);
    		}
    		
    		if ((hiding && crazy >= 1) || (!hiding && crazy >= 0.95)) {
				if (count % 20 == 0) {
    				if (Damages.canDamage(getPlayer(), getPlayer(), DamageCause.MAGIC, 1)) {
    					Damages.damageMagic(getPlayer(), getPlayer(), false, 1);
    					if (getPlayer().getHealth() <= 1) {
    						ItemStack[] armorcontents = getPlayer().getInventory().getArmorContents();
    						ItemStack mainhand = getPlayer().getInventory().getItemInMainHand();
    						Zombie zombie = getPlayer().getWorld().spawn(getPlayer().getLocation(), Zombie.class);
    						zombie.setCustomName("§7" + getPlayer().getCustomName());
    						zombie.setCustomNameVisible(true);
    						zombie.getEquipment().setArmorContents(armorcontents);
    						zombie.getEquipment().setItemInMainHand(mainhand);
    						zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.15);
    						Healths.setHealth(getPlayer(), 0);
    					} else Healths.setHealth(getPlayer(), Math.max(1, getPlayer().getHealth() - 1));  
                    }	
				}
				if (count % 40 == 0) {
					if (Math.random() <= 0.65) {
						final Player player = getPlayer();
						final Location location = player.getLocation();
						float yaw = location.getYaw() + random.nextInt(130) - 65;
						if (yaw > 180 || yaw < -180) {
							float mod = yaw % 180;
							if (mod < 0) {
								yaw = 180 + mod;
							} else if (mod > 0) {
								yaw = -180 + mod;
							}
						}
						NMS.rotateHead(player, player, yaw, location.getPitch() + random.nextInt(90) - 45);
					}
				}
				bossBar.setColor(BarColor.RED);
    		} else bossBar.setColor(BarColor.PURPLE);
    		bossBar.setProgress(crazy);
    	}
    	
		@Override
		public void onEnd() {
			bossBar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (hiding) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (getPlayer().equals(damager)) {
				e.setDamage(e.getDamage() * increase);
			}	
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (hiding) show();
			else hide();
		}
		return false;
	}
	
}
