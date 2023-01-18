package rainstar.abilitywar.ability.chronos;

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
import rainstar.abilitywar.effect.TimeInterrupt;

@AbilityManifest(name = "크로노스", rank = Rank.S, species = Species.GOD, explain = {
		"시간의 신 크로노스.",
		"§7공유 패시브 §8- §3시간 가속§f: 게임 내에 크로노스가 한 명이라도 있으면",
		" 세상의 시간이 매우 빠르게 흐릅니다. 시간이 하루를 일주할 때마다,",
		" 크로노스 소유자의 모든 능력 §c쿨타임§f이 초기화됩니다.",
		"§7철괴 우클릭 §8- §5시간 지배§f: 주변 $[RANGE]칸 내의 모든 플레이어의 시간을 $[DURATION]초간 조작해",
		" 대상에게 §3§n시간 간섭§f을 부여합니다. $[COOLDOWN]",
		" 이 효과를 받은 사람 수에 비례해 더 오랫동안 자신의 시간을 조작하여,",
		" 회복 §7·§f 이동 §7·§f 공격 속도가 §b빨라§f지고 기본 무적 시간이 §e느려§f집니다.",
		"§9[§3시간 간섭§9] §f이동 속도§7 · §f공격 속도§7 · §f쿨타임을 §e느리게§f 흐르게 하고,",
		" §f지속 시간§7 · §f기본 무적 시간§f을 §b빠르게§f 흐르게 합니다."
		},
		summarize = {
		"§3크로노스§f가 있는 세상은 시간이 매우 빠르게 흐릅니다.",
		"하루가 지날 때마다 §3크로노스§f를 가진 모든 플레이어는 쿨타임이 초기화됩니다.",
		"§7철괴 우클릭 시§f 시간을 왜곡시켜 주변 플레이어에게 디버프를,",
		"이 효과를 받은 사람 수에 비례해 자신에게 버프를 겁니다."
		})

public class Chronos extends AbilityBase implements ActiveHandler {

	private static final SettingObject<Integer> TIME_SPEED = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "time-speed", 10, "# 시간 가속의 속도 배율") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> RANGE = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "range", 10, "# 시간 지배의 사거리") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> DURATION = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "duration", 7, "# 시간 조작의 지속 시간") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> BUFF_DURATION = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "buff-duration", 7, "# 조작으로 얻는 기본 버프 지속시간") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	private static final SettingObject<Integer> BUFF_ADD_DURATION = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "buff-add-duration", 30, "# 조작한 대상 하나당 얻는 추가 지속시간", "# 단위: 틱 (20틱 = 1초)") {

		@Override
		public boolean condition(Integer arg0) {
			return arg0 >= 1;
		}

	};
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Chronos.class, "cooldown", 80, "# 쿨타임") {
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
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), 50);
	private final int duration = DURATION.getValue();
	private final int buffduration = BUFF_DURATION.getValue();
	private final int addduration = BUFF_ADD_DURATION.getValue();
	private final int range = RANGE.getValue();
	private static final RGB color = RGB.of(25, 147, 168);
	private final DecimalFormat df = new DecimalFormat("0.0");
	
	private ActionbarChannel ac = newActionbarChannel();
	private AttributeModifier movespeed, attackspeed;
	private Circle circle = Circle.of(range, (range * 12));
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
    						getPlayer().sendMessage("§3[§b!§3] §e하루§f가 지나 모든 능력 §c쿨타임§f이 초기화되었습니다.");
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
    		ac.update("§5시간 지배§7: §f" + df.format((buffduration * 20) + (effectedcount * (addduration))));
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
    		ac.update("§5시간 지배§7: §f" + df.format((double) count / 20) + "초");
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