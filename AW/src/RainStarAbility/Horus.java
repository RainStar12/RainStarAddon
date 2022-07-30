package RainStarAbility;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.TreeMap;

import static java.util.stream.Collectors.*;

import java.text.DecimalFormat;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.google.common.collect.ImmutableSet;

import RainStarEffect.AncientCurse;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "호루스", rank = Rank.S, species = Species.HUMAN, explain = {
		"$[PERIOD]초마다 지난 $[PERIOD]초간 남들에게 §c최종 피해를 가장 많이 준 적§f에게",
		"§5고대 저주§f를 부여하고, §5저주 단계§f만큼 체력을 감소시킵니다.",
		"그 적이 $[PERIOD]초간 입힌 피해의 $[PERCENTAGE]%만큼 자신의 §c공격력§f이 증가합니다.",
		"§0[§5고대 저주§0]§f 중복 부여 시마다 단계가 상승합니다. 저주 단계 1당",
		" 공격력 및 받는 피해량이 각각 10%씩 증가합니다."
		},
		summarize = {
		"일정 주기마다 가장 피해를 많이 준 적을 산출해냅니다.",
		"대상에게 §5고대 저주§f를 부여하고, §5저주 단계§f만큼 체력을 감소시킵니다.",
		"산출된 피해량의 일부만큼 공격력이 증가합니다.",
		"§0[§5고대 저주§0]§f 부여할 때마다 단계 및 공격력 / 받는 피해가 증가합니다."
		})

public class Horus extends AbilityBase {
	
	public Horus(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> PERIOD = 
			abilitySettings.new SettingObject<Integer>(Horus.class, "period", 15,
            "# 피해를 많이 준 적을 산출하는 주기") {
        @Override
        public boolean condition(Integer value) {
            return value >= 1;
        }
    };
    
	public static final SettingObject<Integer> PERCENTAGE = 
			abilitySettings.new SettingObject<Integer>(Horus.class, "percentage", 25,
            "# 산출된 피해량만큼 공격 증가량", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
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
    
    private final int period = PERIOD.getValue();
    private final double percentage = PERCENTAGE.getValue() * 0.01;
    private final DecimalFormat df = new DecimalFormat("0.00");
    private double bestDamage = 0;
    private Map<Player, Double> damageCollector = new HashMap<>();
    private ActionbarChannel ac = newActionbarChannel();
    
    @Override
    public void onUpdate(Update update) {
    	if (update == Update.RESTRICTION_CLEAR) {
    		timer.start();
    		ac.update("§c추가 대미지§f: §e" + df.format(bestDamage));
    	}
    }
    
    private AbilityTimer timer = new AbilityTimer() {
    	
    	@Override
    	public void run(int count) {
    		Entry<Double, Set<Player>> highestKey = damageCollector.entrySet().stream()
    			    .collect(groupingBy(Entry::getValue, TreeMap::new, mapping(Entry::getKey, toSet())))
    			    .lastEntry();
    		Set<Player> result = highestKey != null ? highestKey.getValue() : ImmutableSet.of();
    		
    		if (!result.isEmpty()) {
        		for (Player player : result) {
        			bestDamage = damageCollector.get(player) * percentage;
        			AncientCurse.apply(getGame().getParticipant(player), TimeUnit.SECONDS, period + 1);
        			double level = getGame().getParticipant(player).getPrimaryEffect(AncientCurse.registration).getLevel();
        			Healths.setHealth(player, Math.max(1, player.getHealth() - level));
        			new CurseParticle(player).start();
        		}
    		} else bestDamage = 0;
    		
    		ac.update("§c추가 대미지§f: §e" + df.format(bestDamage));
    	}
    	
    }.setPeriod(TimeUnit.SECONDS, period).register();
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (damager != null) {
			if (!getPlayer().equals(damager)) {
				if (predicate.test(damager)) damageCollector.put(damager, damageCollector.getOrDefault(damager, 0.0) + e.getFinalDamage());
			} else e.setDamage(e.getDamage() + bestDamage);
		}
    }
    
    private class CurseParticle extends AbilityTimer {
    	
    	private double y = 1;
		private boolean yUp = false;
		private Location location;
		private final Player player;
		
    	private CurseParticle(Player player) {
    		super(TaskType.REVERSE, 60);
    		setPeriod(TimeUnit.TICKS, 1);
    		this.player = player;
    	}
		
		@Override
		public void onStart() {
			location = player.getLocation();
		}
		
		@Override
		public void run(int count) {
			double angle = Math.toRadians(count * 5);
			double x = Math.cos(angle);
			double z = Math.sin(angle);
				
			if (y >= 1.6) yUp = false;
			else if (y <= 0.4) yUp = true;
				
			y = yUp ? Math.min(1.6, y + 0.01) : Math.max(0.4, y - 0.01);
				
			location = player.getLocation().clone().add(x, y, z);   	
			ParticleLib.SPELL_WITCH.spawnParticle(location, 0, 0, 0, 1, 0);
			ParticleLib.ENCHANTMENT_TABLE.spawnParticle(location, 0, 0, 0, 1, 0.1);
			ParticleLib.ENCHANTMENT_TABLE.spawnParticle(location, 0, 0, 0, 1, 0);
		}
		
		@Override
		public void onEnd() {
		}
		
		@Override
		public void onSilentEnd() {
			onEnd();
		}
    	
    }

}