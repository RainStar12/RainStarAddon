package rainstar.abilitywar.synergy.normalization.ancientability;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.ParticleLib;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.util.function.Predicate;

@AbilityManifest(name = "고대의 보이드", rank = Rank.SPECIAL, species = Species.OTHERS, explain = {
        "§5공허§f의 존재 보이드. 철괴를 우클릭하면 보이드가 §5공허§f를 통하여",
        "제일 가까이 있는 플레이어에게 §d텔레포트§f합니다. $[COOLDOWN_CONFIG]",
        "§d텔레포트§f를 하고 난 후 $[INVINCIBILITY_DURATION_CONFIG]초간 데미지를 입지 않습니다."
		})
public class AncientVoid extends AbilityBase implements ActiveHandler {
	
	public AncientVoid(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Integer> COOLDOWN_CONFIG = rsAbilitySettings.new SettingObject<Integer>(AncientVoid.class, "cooldown", 100,
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

	public static final SettingObject<Integer> INVINCIBILITY_DURATION_CONFIG = rsAbilitySettings.new SettingObject<Integer>(AncientVoid.class, "invincibility-duration", 100,
			"# 순간이동 후 무적 지속시간 (틱 단위)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

		@Override
		public String toString() {
			return String.valueOf(getValue() / 20.0);
		}
	};

	private final Cooldown cooldown = new Cooldown(COOLDOWN_CONFIG.getValue());
	private final int duration = INVINCIBILITY_DURATION_CONFIG.getValue();

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
	};

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT) {
			if (clickType == ClickType.RIGHT_CLICK) {
				if (!cooldown.isCooldown()) {
					final Player target = LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate);
					if (target != null) {
						getPlayer().sendMessage("§d" + target.getName() + "§f에게 §5순간 이동§f합니다.");
						getPlayer().teleport(target);
						ParticleLib.DRAGON_BREATH.spawnParticle(getPlayer().getLocation(), 1, 1, 1, 20);
						new Invincibility(duration).start();
						cooldown.start();
					} else {
						getPlayer().sendMessage("§5가장 가까운 플레이어§f가 존재하지 않습니다.");
					}
				}
			}
		}

		return false;
	}

	private class Invincibility extends AbilityTimer implements Listener {

		private final ActionbarChannel actionbarChannel = newActionbarChannel();

		private Invincibility(final int ticks) {
			super(ticks);
			setPeriod(TimeUnit.TICKS, 1);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		protected void onStart() {
			getParticipant().attributes().TARGETABLE.setValue(false);
		}

		@EventHandler
		private void onEntityDamage(EntityDamageEvent e) {
			if (getPlayer().equals(e.getEntity()) && isRunning()) {
				e.setCancelled(true);
				ParticleLib.DRAGON_BREATH.spawnParticle(getPlayer().getLocation(), 1, 1, 1, 20, 0);
			}
		}

		@EventHandler
		private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
			this.onEntityDamage(e);
		}

		@EventHandler
		private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
			this.onEntityDamage(e);
		}

		@Override
		protected void run(int count) {
			actionbarChannel.update("§d무적§f: " + (getCount() / 20.0) + "초");
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			actionbarChannel.unregister();
			HandlerList.unregisterAll(this);
			getParticipant().attributes().TARGETABLE.setValue(true);
		}
	}
	
}
