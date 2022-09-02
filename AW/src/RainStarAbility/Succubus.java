package RainStarAbility;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import RainStarAbility.HuntingDog.DogGui;
import RainStarEffect.Charm;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityBase.ClickType;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "서큐버스", rank = Rank.S, species = Species.UNDEAD, explain = {
        "§7철괴 우클릭 §8- §d달콤하게§f: $[RANGE]칸 내의 모든 적을 $[CHARM_DURATION]초간 §d유혹§f합니다.",
        " 이 스킬은 $[COUNT]번 사용 후 쿨타임을 가집니다. $[COOLDOWN]",
        "§7철괴 좌클릭 §8- §c아찔하게§f: $[RANGE]칸 내의 자신 외 모든 적의 §3상태이상§f을",
        " 전부 §c출혈 효과§f로 변경합니다. 이 스킬은 §c쿨타임§f이 없습니다."
        },
        summarize = {
        "§7철괴 우클릭으로§f 주변 적들을 §d유혹§f합니다. $[COUNT]번 쓰면 쿨타임이 생깁니다.",
        "§7철괴 좌클릭으로§f 주변 적들의 §3상태이상§f을 전부 §c출혈§f 효과로 바꿉니다."
        })
public class Succubus extends AbilityBase {

	public Succubus(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> RANGE = 
			abilitySettings.new SettingObject<Double>(Succubus.class, "skill-range", 7.5,
            "# 모든 스킬 범위", "# 단위: 칸") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> COUNT = 
			abilitySettings.new SettingObject<Integer>(Succubus.class, "count", 3,
            "# 철괴 우클릭 연속 사용 가능 횟수") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
	
	public static final SettingObject<Double> CHARM_DURATION = 
			abilitySettings.new SettingObject<Double>(Succubus.class, "charm-duration", 6.9,
            "# 유혹 지속 시간") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> CHARM_DECREASE = 
			abilitySettings.new SettingObject<Integer>(Succubus.class, "charm-decrease", 35,
            "# 유혹 도중 대미지 감소율 (단위: %)") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
	
	public static final SettingObject<Integer> CHARM_HEAL = 
			abilitySettings.new SettingObject<Integer>(Succubus.class, "charm-heal", 55,
            "# 유혹된 대상 타격시 회복률 (단위: %)") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Succubus.class, "cooldown", 115,
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
    
	private ActionbarChannel ac = newActionbarChannel();
    private final double range = RANGE.getValue();
    private final int count = COUNT.getValue();
    private final int duration = (int) (CHARM_DURATION.getValue() * 20);
    private final int decrease = CHARM_DECREASE.getValue();
    private final int heal = CHARM_HEAL.getValue();
    private int stack = 0;
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT)) {
	    	if (clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown()) {
	    		for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate)) {
	    			Participant p = getGame().getParticipant(player);
	    			Charm.apply(p, TimeUnit.TICKS, duration, getPlayer(), heal, decrease);
	    		}
	    		if (stack < count) {
	    			stack++;
	    			ac.update("§d유혹 가능 §f: §e" + (count - stack));
	    		} else {
	    			stack = 0;
	    			cooldown.start();
	    		}
	    		return true;
	    	} else if (clicktype.equals(ClickType.LEFT_CLICK)) {
	    		for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate)) {
	    			Participant p = getGame().getParticipant(player);
	    			
	    		}
	    	}
	    	
	    }
		return false;
	}
    
    
    
    
	
}
