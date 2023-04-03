package rainstar.abilitywar.ability;

import java.text.DecimalFormat;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.vector.VectorIterator;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "호조사", rank = Rank.L, species = Species.GOD, explain = {
		"§7철괴 §8- §b여의주§f: 철괴를 이용하여 자신이 원하는 스탯을 $[STAT_BOOST]초간 $[BOOST_PERCENTAGE]% §6강화§f합니다.",
		" §c공격력§8(§7좌클릭§8) §a/ §3방어력§8(§7우클릭§8) §a/ §b이동 속도§8(§7F키§8)",
		" §6강화§f된 스탯이 아닌 스탯은 $[STAT_WEAKEN]% §7약화§f됩니다. $[COOLDOWN]",
		" 지속시간이 절반 이상 남았을 때 동일한 키입력으로 §6강화§f를 즉시 해제하고,",
		" §c쿨타임§f을 절반만 가질 수 있습니다.",
		"§7패시브 §8- §c여우신§f: 적에게 치명타 피해를 입힐 때마다 꼬리를 하나 획득합니다.",
		" 꼬리는 다음 §6강화§f 수치를 $[TAIL_PER_BOOST]%씩 올려주며, 최대 9개까지 소지 가능합니다.",
		" 꼬리를 19개 소진할 때마다 다음 §b여의주§f 효과의 §7약화§f 수치가 §6강화§f 효과가 됩니다.",
		"§a[§e능력 제공자§a] §5Rodpog"
		},
		summarize = {
		"철괴를 이용하여 자신이 원하는 스탯을 일시적으로 §6강화§f할 수 있습니다.",
		"§6강화§f 중이 아닌 다른 두 스탯은 소폭 §7약화§f됩니다.",
		"§c공격력§8(§7좌클릭§8) §a/ §3방어력§8(§7우클릭§8) §a/ §b이동 속도§8(§7F키§8)",
		"지속시간이 절반 이상 남았을 때 동일한 키입력으로 즉시 종료 및 §c쿨타임§f이 줄어듭니다.",
		"적에게 치명타 피해를 입히면 §6꼬리§f를 획득해 다음 §6강화§f 수치를 증폭시킵니다.",
		"§6꼬리§f 19개 소진 시마다 다음 §6강화§f 시 §7약화§f 효과들도 같이 강화됩니다."
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
			abilitySettings.new SettingObject<Integer>(Hozosa.class, "cooldown", 70,
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
    
	@Override
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	attackCooldownChecker.start();
	    }
	}
    
    private final int duration = (int) (STAT_BOOST.getValue() * 20);
    private final int boostpercent = BOOST_PERCENTAGE.getValue();
    private final int weaken = STAT_WEAKEN.getValue();
    private final int tailper = TAIL_PER_BOOST.getValue();
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private AttributeModifier movespeed;
    private double attack = 1, defence = 1, speed = 0;
    private int type = 0, tail = 0, usedtail = 0;
    private final ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel(), ac3 = newActionbarChannel();
    private final DecimalFormat df = new DecimalFormat("0.0");
	
    private boolean attackCooldown = false, charged = false;
	
	private final AbilityTimer attackCooldownChecker = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (NMS.getAttackCooldown(getPlayer()) > 0.848 && attackCooldown) attackCooldown = false;
			else if (NMS.getAttackCooldown(getPlayer()) <= 0.848 && !attackCooldown) attackCooldown = true;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SuppressWarnings("deprecation")
	public static boolean isCriticalHit(Player p, boolean attackcool) {
		return (!p.isOnGround() && p.getFallDistance() > 0.0F && 
	      !p.getLocation().getBlock().isLiquid() &&
	      attackcool == false &&
	      !p.isInsideVehicle() && !p.isSprinting() && p
	      .getActivePotionEffects().stream().noneMatch(pe -> (pe.getType() == PotionEffectType.BLINDNESS)));
	}
    
    private void skip(int selecttype) {
    	if (type == selecttype && buff.getCount() >= (duration / 2.0)) {
    		buff.stop(false);
    		cooldown.setCount(cooldown.getCount() / 2);
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
	
    private final AbilityTimer buff = new AbilityTimer(duration) {
    	
		private VectorIterator iterator;
    	private String typename;
		
    	@Override
    	public void onStart() {
    		attack = (type == 0 ? 1 + (((tail * tailper) + boostpercent) * 0.01) : 1 + ((weaken * 0.01) * (charged ? 1 : -1)));    		
    		defence = (type == 1 ? Math.max(0.01, 1 - (((tail * tailper) + boostpercent) * 0.01)) : 1 + ((weaken * 0.01) * (charged ? -1 : 1)));
    		speed = (type == 2 ? ((tail * tailper) + boostpercent) * 0.01 : weaken * (0.01 * (charged ? 1 : -1)));
    		if (charged) charged = false;
    		
    		switch(type) {
    		case 0:
    			typename = "§c공격력";
    			break;
    		case 1:
    			typename = "§3방어력";
    			break;
    		case 2:
    			typename = "§b이동 속도";
    			break;
    		}
    		
    		movespeed = new AttributeModifier(UUID.randomUUID(), "movespeed", speed, Operation.ADD_SCALAR);
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(movespeed);
    		
    		SoundLib.ENTITY_EVOKER_PREPARE_SUMMON.playSound(getPlayer().getLocation(), 1, 2);
    		this.iterator = Circle.infiniteIteratorOf(2, 50);
    		
    		usedtail += tail;
    		if (usedtail >= 19) {
    			usedtail -= 19;
    			charged = true;
    		}
    		tail = 0;
    		ac2.update("§6꼬리§7: " + (charged ? "§a§l" : "§7") + tail);
    		ac3.update("§c사용한 꼬리 수§7: §f" + usedtail);
    	}
    	
    	@Override
    	public void run(int count) {
    		for (int j = 0; j < 5; j++) {
    			Location loc = getPlayer().getLocation().clone().add(iterator.next());
    			loc.setY(LocationUtil.getFloorYAt(loc.getWorld(), getPlayer().getLocation().getY(), loc.getBlockX(), loc.getBlockZ()) + 0.1);
    			ParticleLib.SPELL_WITCH.spawnParticle(loc, 0, 0, 0, 1, 0);	
    		}
    		ac1.update(typename + " §e강화§7: " + (count > duration / 2.0 ? "§a" : "§7") + df.format(count / 20.0) + "§f초");
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		attack = 1;
    		defence = 1;
    		speed = 0;
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(movespeed);
    		ac1.update(null);
    		cooldown.start();
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
			
			boolean isCrit = false;
			
			if (getPlayer().equals(e.getDamager()) && isCriticalHit(getPlayer(), attackCooldown)) isCrit = true;
			if (NMS.isArrow(e.getDamager()) && ((Arrow) e.getDamager()).isCritical()) isCrit = true;
			
			if (isCrit && tail < 9) {
				tail++;
				SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer().getLocation(), 1, 2);
				ParticleLib.SPELL_WITCH.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 20, 0.6f);
	    		ac2.update("§6꼬리§7: " + (charged ? "§a§l" : "§7") + tail);
			}
		}
		
		if (e.getEntity().equals(getPlayer())) {
			e.setDamage(e.getDamage() * defence);
		}
	}
	
	@SubscribeEvent
	public void onSwap(PlayerSwapHandItemsEvent e) {
		if (Material.IRON_INGOT.equals(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
			if (!cooldown.isCooldown()) {
				if (buff.isRunning()) skip(2);
				else {
					type = 2;
					final AbilityPreActiveSkillEvent event = new AbilityPreActiveSkillEvent(this, e.getOffHandItem().getType(), null);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						buff.start();	
					}
				}
			}
			e.setCancelled(true);
		}
	}
    
}
