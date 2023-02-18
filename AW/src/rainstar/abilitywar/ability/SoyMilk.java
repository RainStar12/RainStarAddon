package rainstar.abilitywar.ability;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "두유", rank = Rank.S, species = Species.OTHERS, explain = {
        "§7패시브 §8- §e꿀꺽꿀꺽§f: 모든 §d회복 효과§f를 받을 수 없습니다. 기본적으로 체력을",
        " 빠르게 획득해, §a총 3초에 체력 1을 획득§f합니다. §8(§7틱당 0.0167§8)",
        "§7철괴 우클릭 §8- §6두유 러버§f: 체력을 $[HEALTH_GAIN] 획득하고, $[DURATION]초간 §e꿀꺽꿀꺽§f이 봉인됩니다.",
        " 지속시간동안 근접 공격 쿨타임이 사라지며 근접 공격력이 §c$[DAMAGE_DECREASE]%§f 감소합니다.",
        " 이떼 근접 공격을 피격당한 적은 다음 기본 무적 시간이 초기화됩니다. $[COOLDOWN]",
		"§a[§e능력 제공자§a] §bduyu_999"
        },
        summarize = {
        ""
        })
public class SoyMilk extends AbilityBase implements ActiveHandler {
	
	public SoyMilk(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> HEALTH_GAIN = 
			abilitySettings.new SettingObject<Double>(SoyMilk.class, "health-gain", 6.0,
            "# 체력 획득량") {
		
        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> DURATION = 
			abilitySettings.new SettingObject<Integer>(SoyMilk.class, "duration", 30,
            "# 지속시간", "# 단위: 초") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> DAMAGE_DECREASE = 
			abilitySettings.new SettingObject<Integer>(SoyMilk.class, "damage-decrease", 80,
            "# 공격력 감소율", "# 단위: %") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(SoyMilk.class, "cooldown", 90,
            "# 쿨타임", "# 단위: 초") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
        
    };
    
    private final double healthgain = HEALTH_GAIN.getValue();
    private final double decrease = 1 - (DAMAGE_DECREASE.getValue() * 0.01);
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	public static final AttributeModifier PLUS_TEN = new AttributeModifier(UUID.fromString("24aff76f-ff3c-44ea-98c8-62fbde439b07"), "soymilk", 11, Operation.ADD_NUMBER);
	private final ActionbarChannel ac = newActionbarChannel();
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown()) {
	    	if (skill.isRunning()) getPlayer().sendMessage("§e[§f!§e] §f아직 스킬이 지속 중입니다.");
	    	else return skill.start();
	    }
	    return false;
	}
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) passive.start();
	}
    
    private AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
    	public void run(int count) {
			if (ServerVersion.getVersion() >= 13) {
				BlockData powder = Material.SAND.createBlockData();
	    		ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 0.8, 2, 0.8, 2, 0, powder);	
			} else {
				ItemStack powder = new ItemStack(Material.SAND);
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 0.8, 2, 0.8, 2, 0, powder.getData());
			}
    		Healths.setHealth(getPlayer(), getPlayer().getHealth() + 0.0167);
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private AbilityTimer skill = new AbilityTimer(DURATION.getValue()) {
    	
    	@Override
    	public void onStart() {
    		SoundLib.ENTITY_GENERIC_DRINK.playSound(getPlayer().getLocation());
    		Healths.setHealth(getPlayer(), getPlayer().getHealth() + healthgain);
    		getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).addModifier(PLUS_TEN);
    	}
    	
    	@Override
    	public void run(int count) {
    		ac.update("§e두유 금지 해제§7: §c" + count + "초");
    		passive.pause();
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).removeModifier(PLUS_TEN);
    		cooldown.start();
    		passive.resume();
    	}
    	
    }.setPeriod(TimeUnit.SECONDS, 1).register();
    
    @SubscribeEvent(onlyRelevant = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
    	if (passive.isRunning()) e.setCancelled(true);
    }
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {		
		if (skill.isRunning() && getPlayer().equals(e.getDamager())) {
			e.setDamage(e.getDamage() * decrease);
			if (e.getEntity() instanceof LivingEntity) {
				new BukkitRunnable() {
					@Override
					public void run() {
						((LivingEntity) e.getEntity()).setNoDamageTicks(0);
					}
				}.runTaskLater(AbilityWar.getPlugin(), 1L);
			}
			e.getEntity().setVelocity(getPlayer().getLocation().toVector().subtract(e.getEntity().getLocation().toVector()).multiply(0.1).setY(0));
		}
    }
    

}
