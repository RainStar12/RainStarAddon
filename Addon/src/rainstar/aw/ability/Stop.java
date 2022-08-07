package rainstar.aw.ability;

import rainstar.aw.effect.Stiffen;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.google.common.base.Predicate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChatEvent;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

@AbilityManifest(name = "멈춰!", rank = Rank.A, species = Species.HUMAN, explain = {
		"채팅으로 §c멈춰!§f를 치면 10초 내로 자신에게 피해를 줬던",
		"모든 플레이어에게 $[STIFFEN_DURATION]초간 §8경직§f 상태이상을 부여합니다. $[COOLDOWN]",
		"§0[§8경직§0]§f 이동, 공격, 체력 회복이 불가능합니다."
		},
		summarize = {
		"채팅으로 §c멈춰!§f를 치면 10초 내로 자신에게 피해를 줬던",
		"모든 플레이어에게 $[STIFFEN_DURATION]초간 §8경직§f 상태이상을 부여합니다. $[COOLDOWN]",
		"§0[§8경직§0]§f 이동, 공격, 체력 회복이 불가능합니다."
		})

@SuppressWarnings("deprecation")
public class Stop extends AbilityBase {

	public Stop(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(Stop.class,
			"cooldown", 30, "# 멈춰! 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Double> STIFFEN_DURATION = abilitySettings.new SettingObject<Double>(Stop.class,
			"stiffen-duration", 1.5, "# 지속시간") {
		@Override
		public boolean condition(Double value) {
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
	
	private Set<Player> attackers = new HashSet<>();
	
	private final int duration = (int) (STIFFEN_DURATION.getValue() * 20);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
	@SubscribeEvent(ignoreCancelled = false)
	public void onPlayerChat(PlayerChatEvent e) {
		if (e.getPlayer().equals(getPlayer())) {
			if (e.getMessage().contains("멈춰!")) {
				e.setCancelled(true);
				if (!cooldown.isCooldown()) {
					if (!attackers.isEmpty()) {
						for (Player players : attackers) {
							if (predicate.test(players)) Stiffen.apply(getGame().getParticipant(players), TimeUnit.TICKS, duration);	
						}
						getPlayer().chat("§6[§e능력§6] §c멈춰!");
						cooldown.start();
						attackers.clear();
					} else {
						getPlayer().sendMessage("§4[§c!§4] §f10초 내로 피해를 준 사람이 없습니다.");
					}	
				}	
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (e.getEntity().equals(getPlayer()) && !getPlayer().equals(damager) && damager != null) {
			new AttackTimer(damager).start();
		}
	}
	
	public class AttackTimer extends AbilityTimer {
		
		private final Player player;
		
		public AttackTimer(Player player) {
			super(TaskType.REVERSE, 10);
			setPeriod(TimeUnit.SECONDS, 1);
			attackers.add(player);
			this.player = player;
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			attackers.remove(player);
		}
		
	}
	
}