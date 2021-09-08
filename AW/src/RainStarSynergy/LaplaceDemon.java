package RainStarSynergy;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "라플라스의 악마", rank = Rank.S, species = Species.UNDEAD, explain = {
		"§7철괴 우클릭 §8- §c결정론§f: 주사위를 굴려 현재 상황에서 가장 필요한 값을",
		" 도출해냅니다. 상단의 조건일수록 우선순위가 높습니다. $[COOLDOWN]",
		" 내 피가 33% 이하일 경우 §7- §d재생",
		" 최근 5초 내 내가 근접 공격했을 경우 §7- §6힘",
		" 최근 5초 내 내가 피해입었을 경우 §7- §8저항",
		" 최근 5초 내 내가 화염 피해를 입었을 경우 §7- §c화염 저항",
		" 주변 9칸 내 다른 플레이어가 있을 경우 §7- §b신속",
		" 해당사항이 없을 경우 §7- §e성급함",
		})

public class LaplaceDemon extends Synergy implements ActiveHandler {
	
	public LaplaceDemon(Participant participant) {
		super(participant);
	}
	
	private boolean regeneration = false;
	private boolean power = false;
	private boolean resistance = false;
	private boolean fireimmune = false;
	private boolean speed = false;
	private ActionbarChannel ac = newActionbarChannel();
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	
	public static final SettingObject<Integer> COOLDOWN = 
			synergySettings.new SettingObject<Integer>(LaplaceDemon.class, "cooldown", 14,
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
			passive.start();
		}
	}
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
			if (getPlayer().getHealth() <= getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 3) regeneration = true;
			else regeneration = false;
			if (LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 9, 9, predicate).size() > 0) speed = true;
			else speed = false;
			
			if (!getPlayer().hasPotionEffect(PotionEffectType.REGENERATION) && regeneration == true) {
				ac.update("§d재생");
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.INCREASE_DAMAGE) && power == true) {
				ac.update("§6힘");
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) && resistance == true) {
				ac.update("§8저항");
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) && fireimmune == true) {
				ac.update("§c화염 저항");
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.SPEED) && speed == true) {
				ac.update("§b신속");
			} else {
				ac.update("§e성급함");
			}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private final AbilityTimer attacked = new AbilityTimer(100) {
    	
    	@Override
    	public void run(int count) {
    		power = true;
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		power = false;
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private final AbilityTimer damaged = new AbilityTimer(100) {
    	
    	@Override
    	public void run(int count) {
    		resistance = true;
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		resistance = false;
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private final AbilityTimer firedamaged = new AbilityTimer(100) {
    	
    	@Override
    	public void run(int count) {
    		fireimmune = true;
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		fireimmune = false;
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			if (!e.getCause().equals(DamageCause.FIRE) && !e.getCause().equals(DamageCause.FIRE_TICK) && !e.getCause().equals(DamageCause.LAVA)) {
				if (damaged.isRunning()) damaged.setCount(100);
				else damaged.start();
			}
			if (e.getCause().equals(DamageCause.FIRE) || e.getCause().equals(DamageCause.FIRE_TICK) || e.getCause().equals(DamageCause.LAVA)) {
				if (firedamaged.isRunning()) firedamaged.setCount(100);
				else firedamaged.start();
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
			if (attacked.isRunning()) attacked.setCount(100);
			else attacked.start();
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.RIGHT_CLICK && !cool.isCooldown()) {
			if (!getPlayer().hasPotionEffect(PotionEffectType.REGENERATION) && regeneration == true) {
				getPlayer().sendMessage("§d재생 §f효과를 받습니다.");
				PotionEffects.REGENERATION.addPotionEffect(getPlayer(), 300, 1, true);
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.INCREASE_DAMAGE) && power == true) {
				getPlayer().sendMessage("§6힘 §f효과를 받습니다.");
				PotionEffects.INCREASE_DAMAGE.addPotionEffect(getPlayer(), 300, 1, true);
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) && resistance == true) {
				getPlayer().sendMessage("§8저항 §f효과를 받습니다.");
				PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), 300, 1, true);
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) && fireimmune == true) {
				getPlayer().sendMessage("§c화염 저항 §f효과를 받습니다.");
				PotionEffects.FIRE_RESISTANCE.addPotionEffect(getPlayer(), 300, 1, true);
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.SPEED) && speed == true) {
				getPlayer().sendMessage("§b신속 §f효과를 받습니다.");
				PotionEffects.SPEED.addPotionEffect(getPlayer(), 300, 1, true);
			} else {
				getPlayer().sendMessage("§e성급함 §f효과를 받습니다.");
				PotionEffects.FAST_DIGGING.addPotionEffect(getPlayer(), 300, 1, true);
			}
			return cool.start();
		}
		return false;
	}
	
}