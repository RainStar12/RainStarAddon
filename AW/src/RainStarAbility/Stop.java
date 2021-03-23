package RainStarAbility;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.util.Vector;

import RainStarEffect.Stiffen;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "멈춰!", rank = Rank.A, species = Species.HUMAN, explain = {
		"§7채팅 §8- §c폭력 멈춰!§f: 자신이 3초 내로 공격하지 않은 대상에게",
		" 전투로 인한 피해를 받고 있는 도중 §c멈춰!§f라고 외칠 경우,",
		" 근방 $[LOOK_RANGE]블록 내의 모든 플레이어가 대상을 바라보며 멈춰를 외치고,",
		" 대상은 경직 상태이상에 걸리게 됩니다. 이 능력은 단 한 번만 사용 가능합니다.",
		"§7채팅 §8- §c능력 멈춰!§f: §c능력 멈춰!§f를 외치면 주변 $[ABILITY_STOP_RANGE]칸 내의",
		" 모든 플레이어의 능력 타이머가 $[ABILITY_STOP_DURATION]초간 멈추게 됩니다. $[COOLDOWN]",
		"§7상태이상 §8- §c경직§f: 이동, 공격, 체력 회복, 능력 사용이 불가합니다.",
		" 또한 받는 모든 피해를 90% 경감하여 받습니다."})

@SuppressWarnings("deprecation")
public class Stop extends AbilityBase {

	public Stop(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(Stop.class,
			"cooldown", 100, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> LOOK_RANGE = abilitySettings.new SettingObject<Integer>(Stop.class,
			"look-range", 10, "# 멈춰!를 따라해주는 플레이어 범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> ABILITY_STOP_RANGE = abilitySettings.new SettingObject<Integer>(Stop.class,
			"ability-stop-range", 8, "# 능력 멈춰! 범위") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> ABILITY_STOP_DURATION = abilitySettings.new SettingObject<Integer>(Stop.class,
			"ability-stop-duration", 5, "# 지속시간") {
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
	
	private final Predicate<Entity> notarget = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))) {
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
	
	private boolean onetime = true;
	private int stoprange = ABILITY_STOP_RANGE.getValue();
	private int lookrange = LOOK_RANGE.getValue();
	private int duration = ABILITY_STOP_DURATION.getValue();
	private Player damager;
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
	private final AbilityTimer damaged = new AbilityTimer(100) {
		
		@Override
		public void run(int count) {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer attacked = new AbilityTimer(60) {
		
		@Override
		public void run(int count) {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(ignoreCancelled = false)
	public void onPlayerChat(PlayerChatEvent e) {
		if (e.getPlayer().equals(getPlayer()) && e.getMessage().equals("멈춰!") && onetime) {
			e.setCancelled(true);
			if (damaged.isRunning() && !attacked.isRunning()) {
				for (Player p : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), lookrange, lookrange, predicate)) {
					Vector direction = damager.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector());
					float yaw = LocationUtil.getYaw(direction), pitch = LocationUtil.getPitch(direction);
					for (Player allplayer : Bukkit.getOnlinePlayers()) {
					    NMS.rotateHead(allplayer, p, yaw, pitch);	
					}
					p.chat("§6[§e능력§6] §c멈춰!");
				}
				Stiffen.apply(getGame().getParticipant(damager), TimeUnit.SECONDS, 10);
				onetime = false;
				getPlayer().chat("§6[§e능력§6] §c멈춰!");
			} else {
				getPlayer().sendMessage("§4[§c!§4] §f피해를 받은 적이 없거나 본인도 폭력을 행사하였습니다.");
			}
		}
		if (e.getPlayer().equals(getPlayer()) && e.getMessage().equals("능력 멈춰!")) {
			if (!cooldown.isCooldown()) {
				for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), stoprange, stoprange, notarget)) {
					Participant p = getGame().getParticipant(player);
					if (p.hasAbility() && !p.getAbility().isRestricted()) {
						AbilityBase ab = p.getAbility();
						for (GameTimer t : ab.getTimers()) {
							new Stopper(t).start();
						}
					}
				}
				getPlayer().chat("§6[§e능력§6] §c능력 멈춰!");
				cooldown.start();
			}
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (onetime && e.getEntity().equals(getPlayer())) {
			if (!e.getDamager().equals(getPlayer()) && e.getDamager() instanceof Player) {
				damager = (Player) e.getDamager();
				if (damaged.isRunning()) {
					damaged.setCount(60);
				} else {
					damaged.start();	
				}
			}
		}
	}
	
	private class Stopper extends AbilityTimer {
		
		private GameTimer t;
		private int getcount;
		
		private Stopper(GameTimer t) {
			super(TaskType.NORMAL, duration * 20);
			setPeriod(TimeUnit.TICKS, 1);
			this.t = t;
		}
		
		@Override
		protected void onStart() {
			getcount = t.getCount();
		}
		
		@Override
		protected void run(int count) {
			t.setCount(getcount);
		}
		
	}
	
}
