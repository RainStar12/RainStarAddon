package RainStarAbility.chronos;

import java.text.DecimalFormat;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import RainStarEffect.TimeInterrupt;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "ũ�γ뽺", rank = Rank.S, species = Species.GOD, explain = {
		"�ð��� �� ũ�γ뽺.",
		"��7�нú� ��8- ��3�ð� ���ӡ�f: ���� ���� ũ�γ뽺�� �� ���̶� ������",
		" ������ �ð��� �ſ� ������ �帨�ϴ�. �ð��� �Ϸ縦 ������ ������,",
		" ũ�γ뽺 �������� ��� �ɷ� ��c��Ÿ�ӡ�f�� �ʱ�ȭ�˴ϴ�.",
		"��7ö�� ��Ŭ�� ��8- ��5�ð� �����f: �ֺ� $[RANGE]ĭ ���� ��� �÷��̾��� �ð��� $[DURATION]�ʰ� ������",
		" ��f�̵� �ӵ���7 �� ��f���� �ӵ���7 �� ��f��Ÿ���� ��e�����ԡ�f �帣�� �ϰ�,",
		" ��f���� �ð���7 �� ��f�⺻ ���� �ð���f�� ��b�����ԡ�f �帣�� �մϴ�. $[COOLDOWN]",
		" �� ȿ���� ���� ��� ���� ����� �� �������� �ڽ��� �ð��� �����Ͽ�,",
		" ȸ�� ��7����f �̵� ��7����f ���� �ӵ��� ��b�����f���� �⺻ ���� �ð��� ��e������f���ϴ�."
		},
		summarize = {
		"��3ũ�γ뽺��f�� �ִ� ������ �ð��� �ſ� ������ �帨�ϴ�.",
		"�Ϸ簡 ���� ������ ��3ũ�γ뽺��f�� ���� ��� �÷��̾�� ��Ÿ���� �ʱ�ȭ�˴ϴ�.",
		"��7ö�� ��Ŭ�� �á�f �ð��� �ְ���� �ֺ� �÷��̾�� �������,",
		"�� ȿ���� ���� ��� ���� ����� �ڽſ��� ������ �̴ϴ�."
		})

public class Chronos extends AbilityBase implements ActiveHandler {

	private static final SettingObject<Integer> TIME_SPEED = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "time-speed", 10, "# �ð� ������ �ӵ� ����") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> RANGE = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "range", 7, "# �ð� ������ ��Ÿ�") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> DURATION = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "duration", 7, "# �ð� ������ ���� �ð�") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> BUFF_DURATION = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "buff-duration", 7, "# �������� ��� �⺻ ���� ���ӽð�") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> BUFF_ADD_DURATION = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "buff-add-duration", 30, "# ������ ��� �ϳ��� ��� �߰� ���ӽð�", "# ����: ƽ (20ƽ = 1��)") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "cooldown", 140, "# ��Ÿ��") {
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
	
	public Chronos(Participant participant) {
		super(participant);
		if (!getGame().hasModule(TimeAcceleration.class)) {
			getGame().addModule(new TimeAcceleration(getGame(), getPlayer().getWorld(), TIME_SPEED.getValue()));
		}
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	private final int duration = DURATION.getValue();
	private final int buffduration = BUFF_DURATION.getValue();
	private final int addduration = BUFF_ADD_DURATION.getValue();
	private final int range = RANGE.getValue();
	private static final RGB color = RGB.of(25, 147, 168);
	private final DecimalFormat df = new DecimalFormat("0.0");
	
	private ActionbarChannel ac = newActionbarChannel();
	private AttributeModifier movespeed, attackspeed;
	private Circle circle = Circle.of(range, (range * 11));
	private int effectedcount = 0;	
	private boolean timechanged = false;
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
    	public void run(int count) {
    		if (count % 2 == 0) {
				for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, color);
				}
    		}
    		if (getPlayer().getWorld().getTime() >= 15000) {
    			timechanged = true;
    		}
    		if (getPlayer().getWorld().getTime() < 15000) {
    			if (timechanged) {
    				if (getParticipant().hasAbility() && !getParticipant().getAbility().isRestricted()) {
    					boolean cleared = false;
    					AbilityBase ab = getParticipant().getAbility();
    					for (GameTimer t : ab.getRunningTimers()) {
    						if (t instanceof Cooldown.CooldownTimer) {
    							t.setCount(0);
    							cleared = true;
    						}
    					}
    					if (cleared) {
    						getPlayer().sendMessage("��3[��b!��3] ��e�Ϸ��f�� ���� ��� �ɷ� ��c��Ÿ�ӡ�f�� �ʱ�ȭ�Ǿ����ϴ�.");
    						SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer());
    					}
    				}
    				timechanged = false;
    			}
    		}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
    private AbilityTimer buff = new AbilityTimer((buffduration * 20)) {
    	
    	@Override
    	public void onStart() {
    		ac.update("��5�ð� �����7: ��f" + df.format((buffduration * 20) + (effectedcount * (addduration))));
    		movespeed = new AttributeModifier(UUID.randomUUID(), "movespeed", 0.1, Operation.ADD_NUMBER);
    		getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(movespeed);
    		attackspeed = new AttributeModifier(UUID.randomUUID(), "attackspeed", 1.5, Operation.ADD_NUMBER);
    		getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).addModifier(attackspeed);
    		getPlayer().setMaximumNoDamageTicks(30);
    	}
    	
    	@Override
    	public void run(int count) {
    		if (count % 7 == 0) {
    			if (count % 14 == 0) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f);
    			else SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 2f);
    		}
    		ac.update("��5�ð� �����7: ��f" + df.format((double) count / 20) + "��");
    		if (getPlayer().getHealth() > 0) {
    			final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), 0.05, RegainReason.CUSTOM);
    			Bukkit.getPluginManager().callEvent(event);
    			if (!event.isCancelled()) {
    				Healths.setHealth(getPlayer(), getPlayer().getHealth() + 0.05);
    			}	
    		}
    	}
    	
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		ac.update(null);
    		getPlayer().setMaximumNoDamageTicks(20);
			getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).removeModifier(attackspeed);
			getPlayer().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(movespeed);
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.RIGHT_CLICK && !cool.isCooldown()) {
			effectedcount = 0;
			SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer(), 1, 1.75f);
			for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate)) {
				TimeInterrupt.apply(getGame().getParticipant(player), TimeUnit.TICKS, duration * 20);
				SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(player, 1, 1.25f);
				effectedcount++;
			}
			if (buff.isRunning()) buff.setCount(buff.getCount() + (buffduration * 20) + (effectedcount * (addduration)));
			else {
				buff.start();
				buff.setCount((buffduration * 20) + (effectedcount * (addduration)));
			}
			return cool.start();
		}
		return false;
	}
    
}
