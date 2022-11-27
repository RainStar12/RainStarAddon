package RainStarAbility;

import java.text.DecimalFormat;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Infection;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "X-Infected", rank = Rank.S, species = Species.UNDEAD, explain = {
		"철괴 우클릭 시 $[DURATION]초간 §d10HP§f의 §2좀비§f가 되어 컨트롤이 불가능해집니다. $[COOLDOWN]",
		"§2좀비§f는 내 최종 공격력의 §c$[DAMAGE]%§f의 피해를 주고, 이동 속도가 빠릅니다.",
		"§2좀비화§f 도중 §c사망 시§f 지속시간이 즉시 종료되고 $[HEALTH_LOSE]%의 체력을 잃습니다.",
		"§2좀비§f가 피해를 준 대상은 $[INFECT_DURATION]초간 §5감염§f됩니다."
		},
		summarize = {
		"§7철괴 우클릭 시§f §2좀비화§f하여 공격력과 이동 속도가 증가합니다.",
		"§2좀비화§f 중에는 자신이 이동을 컨트롤 불가능하며, §2좀비§f AI가 대체합니다.",
		"§2좀비화§f 중 사망 시 지속시간이 즉시 종료되고 체력을 일부 잃습니다.",
		"§2좀비§f가 피해를 준 대상은 짧게 §5감염§f됩니다."
		})
public class XInfected extends AbilityBase implements ActiveHandler {
	
	public XInfected(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(XInfected.class, "cooldown", 85,
            "# 좀비화 지속 시간") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
    };
	
	public static final SettingObject<Double> DURATION = 
			abilitySettings.new SettingObject<Double>(XInfected.class, "zombie-duration", 13.0,
            "# 좀비화 지속 시간") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> DAMAGE = 
			abilitySettings.new SettingObject<Integer>(XInfected.class, "damage-percentage", 115,
            "# 좀비가 주는 피해량 비율") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Integer> HEALTH_LOSE = 
			abilitySettings.new SettingObject<Integer>(XInfected.class, "health-lose", 25,
            "# 좀비화 간 사망 시 잃는 체력", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
    
	public static final SettingObject<Double> INFECT_DURATION = 
			abilitySettings.new SettingObject<Double>(XInfected.class, "infect-duration", 0.7,
            "# 감염 부여 시간") {
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
    };
    
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    private final int duration = (int) (DURATION.getValue() * 20);
    private final double damagemultiply = DAMAGE.getValue() * 0.01;
    private final double losehealth = HEALTH_LOSE.getValue() * 0.01;
    private final int infectduration = (int) (INFECT_DURATION.getValue() * 20);
    private Zombie zombie;
    private double damage;
    private final DecimalFormat df = new DecimalFormat("0.0");
    private ActionbarChannel ac = newActionbarChannel();
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype.equals(ClickType.RIGHT_CLICK) && !infected.isRunning() && !cooldown.isCooldown()) {
			return infected.start();
		}
		return false;
	}
    
    private AbilityTimer infected = new AbilityTimer(duration) {
    	
    	@SuppressWarnings("deprecation")
		@Override
    	public void onStart() {
    		getPlayer().setGameMode(GameMode.SPECTATOR);
    		
    		zombie = getPlayer().getWorld().spawn(getPlayer().getLocation(), Zombie.class);
			if (ServerVersion.getVersion() >= 16) zombie.setAdult();
			else zombie.setBaby(false);
			zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.45);
			
			zombie.getEquipment().setItemInMainHand(null);
			zombie.getEquipment().setArmorContents(getPlayer().getInventory().getArmorContents());
			zombie.getEquipment().setItemInMainHandDropChance(0);
			zombie.getEquipment().setBootsDropChance(0);
			zombie.getEquipment().setChestplateDropChance(0);
			zombie.getEquipment().setHelmetDropChance(0);
			zombie.getEquipment().setLeggingsDropChance(0);
			zombie.setCustomName("§2" + getPlayer().getName());
			zombie.setCustomNameVisible(true);
			zombie.setConversionTime(Integer.MAX_VALUE);
			
			zombie.setHealth(10);
			
			getPlayer().setSpectatorTarget(zombie);
			
			SoundLib.ENTITY_ZOMBIE_VILLAGER_CONVERTED.playSound(getPlayer().getLocation(), 1, 1.2f);
			ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0.25, 0.5, 0.25, 150, 0.77, MaterialX.REDSTONE_BLOCK);
    	}
    	
    	@Override
    	public void run(int count) {
    		ac.update("§2좀비화§f: " + df.format(count / 20.0));
    		if (zombie.isDead()) this.stop(true);
    		else {
    			getPlayer().setSpectatorTarget(zombie);
    			Vector direction = zombie.getLocation().getDirection();
    			float yaw = LocationUtil.getYaw(direction), pitch = LocationUtil.getPitch(direction);
    			NMS.rotateHead(getPlayer(), getPlayer(), yaw, pitch);
    		}
    	}
    	
    	@Override
    	public void onEnd() {
			SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(getPlayer().getLocation(), 1, 1.85f);
    		getPlayer().setGameMode(GameMode.SURVIVAL);
    		ac.update(null);
    		zombie.remove();
    		cooldown.start();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		onEnd();
    		getPlayer().setHealth(Math.max(1, getPlayer().getHealth() - getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * losehealth));
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().equals(zombie)) {
			e.setDamage(damage * damagemultiply);
			if (e.getEntity() instanceof Player) {
				Player player = (Player) e.getEntity();
				if (getGame().isParticipating(player)) Infection.apply(getGame().getParticipant(player), TimeUnit.TICKS, infectduration);
			}
		}
	}

}
