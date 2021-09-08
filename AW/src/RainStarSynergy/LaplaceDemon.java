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

@AbilityManifest(name = "���ö��� �Ǹ�", rank = Rank.S, species = Species.UNDEAD, explain = {
		"��7ö�� ��Ŭ�� ��8- ��c�����С�f: �ֻ����� ���� ���� ��Ȳ���� ���� �ʿ��� ����",
		" �����س��ϴ�. ����� �����ϼ��� �켱������ �����ϴ�. $[COOLDOWN]",
		" �� �ǰ� 33% ������ ��� ��7- ��d���",
		" �ֱ� 5�� �� ���� ���� �������� ��� ��7- ��6��",
		" �ֱ� 5�� �� ���� �����Ծ��� ��� ��7- ��8����",
		" �ֱ� 5�� �� ���� ȭ�� ���ظ� �Ծ��� ��� ��7- ��cȭ�� ����",
		" �ֺ� 9ĭ �� �ٸ� �÷��̾ ���� ��� ��7- ��b�ż�",
		" �ش������ ���� ��� ��7- ��e������",
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
            "# ��Ÿ��") {
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
				ac.update("��d���");
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.INCREASE_DAMAGE) && power == true) {
				ac.update("��6��");
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) && resistance == true) {
				ac.update("��8����");
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) && fireimmune == true) {
				ac.update("��cȭ�� ����");
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.SPEED) && speed == true) {
				ac.update("��b�ż�");
			} else {
				ac.update("��e������");
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
				getPlayer().sendMessage("��d��� ��fȿ���� �޽��ϴ�.");
				PotionEffects.REGENERATION.addPotionEffect(getPlayer(), 300, 1, true);
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.INCREASE_DAMAGE) && power == true) {
				getPlayer().sendMessage("��6�� ��fȿ���� �޽��ϴ�.");
				PotionEffects.INCREASE_DAMAGE.addPotionEffect(getPlayer(), 300, 1, true);
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) && resistance == true) {
				getPlayer().sendMessage("��8���� ��fȿ���� �޽��ϴ�.");
				PotionEffects.DAMAGE_RESISTANCE.addPotionEffect(getPlayer(), 300, 1, true);
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) && fireimmune == true) {
				getPlayer().sendMessage("��cȭ�� ���� ��fȿ���� �޽��ϴ�.");
				PotionEffects.FIRE_RESISTANCE.addPotionEffect(getPlayer(), 300, 1, true);
			} else if (!getPlayer().hasPotionEffect(PotionEffectType.SPEED) && speed == true) {
				getPlayer().sendMessage("��b�ż� ��fȿ���� �޽��ϴ�.");
				PotionEffects.SPEED.addPotionEffect(getPlayer(), 300, 1, true);
			} else {
				getPlayer().sendMessage("��e������ ��fȿ���� �޽��ϴ�.");
				PotionEffects.FAST_DIGGING.addPotionEffect(getPlayer(), 300, 1, true);
			}
			return cool.start();
		}
		return false;
	}
	
}