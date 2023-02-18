package rainstar.abilitywar.ability;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "호조사", rank = Rank.S, species = Species.GOD, explain = {
		"§7철괴 §8- §b여의주§f: 철괴를 이용하여 자신이 원하는 스탯을 $[STAT_BOOST]초간 $[BOOST_PERCENTAGE]% §6강화§f합니다.",
		" §c공격력§8(§7좌클릭§8) §a/ §3방어력§8(§7우클릭§8) §a/ §b이동 속도§8(§7F키§8)",
		" §6강화§f된 스탯이 아닌 스탯은 $[STAT_WEAKEN]% 약화됩니다. $[COOLDOWN]",
		" 지속시간이 절반 이상 남았을 때 동일한 키입력으로 §6강화§f를 즉시 해제하고,",
		" §c쿨타임§f을 2/3만 가질 수 있습니다.",
		"§7패시브 §8- §c여우신§f: 적에게 치명타 피해를 입힐 때마다 꼬리를 하나 획득합니다.",
		" 꼬리는 다음 §6강화§f 수치를 $[TAIL_PER_BOOST]%씩 올려주며, 최대 9개까지 소지 가능합니다.",
		"§a[§e능력 제공자§a] §5Rodpog"
		},
		summarize = {
		""
		})
public class Hozosa extends AbilityBase implements ActiveHandler {
	
	public Hozosa(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> STAT_BOOST = 
			abilitySettings.new SettingObject<Double>(Hozosa.class, "stat-boost", 10.0,
			"# 스탯 부스트 지속시간", "# 단위: 초") {
		
		@Override
        public boolean condition(Double value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> BOOST_PERCENTAGE = 
			abilitySettings.new SettingObject<Integer>(Hozosa.class, "boost-percentage", 23,
            "# 기본적으로 강화되는 수치", "# 단위: %") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
	public static final SettingObject<Integer> STAT_WEAKEN = 
			abilitySettings.new SettingObject<Integer>(Hozosa.class, "stat-weaken", 15,
            "# 스탯 약화 수치", "# 단위: %") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };

	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Hozosa.class, "cooldown", 99,
            "# 쿨타임") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
        
    };
    
	public static final SettingObject<Integer> TAIL_PER_BOOST = 
			abilitySettings.new SettingObject<Integer>(Hozosa.class, "tail-per-boost", 3,
            "# 꼬리 하나당 추가 강화 수치", "# 단위: %") {
		
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
    };
    
    private final int duration = (int) (STAT_BOOST.getValue() * 20);
    private final int boostpercent = BOOST_PERCENTAGE.getValue();
    private final int weaken = STAT_WEAKEN.getValue();
    private final int tailper = TAIL_PER_BOOST.getValue();
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private AttributeModifier movespeed;
    private double attack = 1;
    private double defence = 1;
    private double speed = 0;
    private int type = 0;
    private int tail = 0;
    
    private void skip(int selecttype) {
    	if (type == selecttype && buff.getCount() >= (duration / 2.0)) {
    		buff.stop(false);
    		cooldown.setCount(cooldown.getCount() * 2 / 3);
    		getPlayer().sendMessage("§5[§d!§5] §a능력을 즉시 종료하였습니다.");    	
    	} else getPlayer().sendMessage("§5[§d!§5] §c아직 스탯 부스트가 끝나지 않았습니다.");
    }
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && !cooldown.isCooldown()) {
			int a = (clicktype.equals(ClickType.LEFT_CLICK) ? 0 : 1);
			if (buff.isRunning()) skip(a);
			else {
				type = a;
				return buff.start();
			}
		}
		return false;
	}
	
    private final AbilityTimer buff = new AbilityTimer() {
    	
    	@Override
    	public void onStart() {
    		attack = (type == 0 ? 1 + ((tail * tailper) + boostpercent * 0.01) : 1 - (weaken * 0.01));
    		defence = (type == 1 ? Math.max(0.01, 1 - (((tail * tailper) + boostpercent) * 0.01)) : 1 + (weaken * 0.01));
    		speed = (type == 2 ? ((tail * tailper) + boostpercent) * 0.01 : weaken * -0.01);
    		
    		movespeed = new AttributeModifier(UUID.randomUUID(), "movespeed", speed, Operation.ADD_SCALAR);
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(movespeed);
    	}
    	
    	@Override
    	public void run(int count) {
    		
    	}
    	
    	@Override
    	public void onEnd() {
    		
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		attack = 1;
    		defence = 1;
    		speed = 0;
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(movespeed);
    	}
    	
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    	Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager)) {
			e.setDamage(e.getDamage() * attack);
		}
		
		if (e.getEntity().equals(getPlayer())) {
			e.setDamage(e.getDamage() * defence);
		}
	}
    
}
