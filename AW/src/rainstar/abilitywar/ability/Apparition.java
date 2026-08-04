package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.entity.health.event.PlayerSetHealthEvent;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.BindingSmoke;

@AbilityManifest(name = "영체화", rank = Rank.L, species = Species.UNDEAD, explain = {
		"§7철괴 우클릭 §8- §2불가침§f: $[DURATION]초간 §3관전 상태§f가 됩니다. $[COOLDOWN]",
		" 지속 도중에는 모든 체력 손실이 §2§l1§f 이하로 고정됩니다.",
		" 체력을 $[HP_RECOVERY]% 회복할 때마다, 남은 §c쿨타임§f이 절반으로 감소합니다.",
		"§7패시브 §8- §3영령계§f: 자신이 §3관전 상태§f인 동안은, 매 초마다",
		" $[RANGE]칸 내의 모든 대상에게 §9§n속박의 연막§f을 부여합니다.",
		"§7관전 시도 §8- §c영혼 폭발§f: §3관전 상태§f에서 다른 대상에게 빙의를 시도하면,",
		" 관전해있던 시간에 비례해 초당 §c$[DAMAGE_PERCENTAGE]§8(§7최대 $[MAX_PERCENTAGE]§8)§c%§f의 §b트루 대미지§f를 입힙니다.",
		" 이후 §3관전 상태§f가 해제되기 전까지는 다시 사용할 수 없습니다."
		})
public class Apparition extends AbilityBase implements ActiveHandler {
	
	public Apparition(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> DURATION = rsAbilitySettings.new SettingObject<Double>(Apparition.class,
			"duration", 5.0, "# 관전 상태 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(Apparition.class,
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
	
	public static final SettingObject<Integer> HP_RECOVERY = rsAbilitySettings.new SettingObject<Integer>(Apparition.class,
			"hp-recovery", 25, "# 쿨타임 감소 조건의 체력 회복 요구량", "# 단위: 최대 체력의 %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> RANGE = rsAbilitySettings.new SettingObject<Double>(Apparition.class,
			"range", 6.0, "# 속박의 연막 부여 범위", "# 단위: 칸") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DAMAGE_PERCENTAGE = rsAbilitySettings.new SettingObject<Double>(Apparition.class,
			"damage-percentage", 4.0, "# 영혼 폭발의 초당 피해량", "# 단위: 최대 체력의 %") {
		@Override
		public boolean condition(Double value) {
			return value >= 0.0;
		}
	};
	
	public static final SettingObject<Double> MAX_PERCENTAGE = rsAbilitySettings.new SettingObject<Double>(Apparition.class,
			"max-percentage", 50.0, "# 영혼 폭발의 최대 피해량", "# 단위: 최대 체력의 %") {
		@Override
		public boolean condition(Double value) {
			return value >= 0.0;
		}
	};
	
	private int duration = (int) (DURATION.getValue() * 20);
	private Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final double healpercentage = HP_RECOVERY.getValue() * 0.01;
	private final double healhp = healpercentage * getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
	private double range = RANGE.getValue();
	private double damagepercentage = DAMAGE_PERCENTAGE.getValue() * 0.01;
	private double maxpercentage = MAX_PERCENTAGE.getValue() * 0.01;
	private Circle circle = Circle.of(range, (int) (range * 10));
	private boolean useable = true;
	private GameMode previousGamemode;
	private double healstack = 0;
	private int specterstack = 0;
	private ActionbarChannel ac = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	
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
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (getPlayer().getGameMode().equals(GameMode.SPECTATOR)) {
				specterstack++;
				if (count % 2 == 0) {
					for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
						if (ServerVersion.getVersion() >= 16) ParticleLib.SOUL.spawnParticle(getPlayer(), loc, 0, 0, 0, 1, 0);
						else ParticleLib.EXPLOSION_NORMAL.spawnParticle(getPlayer(), loc, 0, 0, 0, 1, 0);
					}	
				}
				if (count % 20 == 0) {
					for (Player player : LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), range, predicate)) {
						BindingSmoke.apply(getGame().getParticipant(player), TimeUnit.TICKS, 13);
					}	
				}
				if (useable && getPlayer().getSpectatorTarget() != null && getPlayer().getSpectatorTarget() instanceof Player) {
					Player target = (Player) getPlayer().getSpectatorTarget();
					SoundLib.ENTITY_GHAST_SCREAM.playSound(target.getLocation(), 0.7f, 0.5f);
					SoundLib.ENTITY_GHAST_SCREAM.playSound(target.getLocation(), 0.7f, 1f);
					SoundLib.ENTITY_GHAST_SCREAM.playSound(target.getLocation(), 0.7f, 1.5f);
					SoundLib.ENTITY_GHAST_SCREAM.playSound(target.getLocation(), 0.7f, 2f);
					ParticleLib.EXPLOSION_HUGE.spawnParticle(target.getLocation(), 0, 0, 0, 1, 0);
					SoundLib.ENTITY_GENERIC_EXPLODE.playSound(target.getLocation(), 0.5f, 1);
					getPlayer().setSpectatorTarget(null);
					double maxhp = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
					double damage = Math.min(maxhp * maxpercentage, specterstack * 0.05 * damagepercentage * maxhp);
					if (Damages.canDamage(target, getPlayer(), DamageCause.MAGIC, damage)) {
						Damages.damageMagic(target, getPlayer(), true, 1f);
						Healths.setHealth(target, target.getHealth() - damage);
					}
					useable = false;
				}
			} else specterstack = 0;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer skill = new AbilityTimer(duration) {
	
		@Override
		public void onStart() {
			ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.3, 0, 0.3, 20, 1);
			SoundLib.ENTITY_VEX_AMBIENT.playSound(getPlayer().getLocation(), 0.75f, 1.75f);
			SoundLib.ITEM_ARMOR_EQUIP_ELYTRA.playSound(getPlayer().getLocation(), 1, 0.5f);
			previousGamemode = getPlayer().getGameMode();
			getPlayer().setGameMode(GameMode.SPECTATOR);
			ac.update("§2불가침§7: §b" + df.format(duration * 0.05) + "§f초");
		}
		
		@Override
		public void run(int count) {
			ac.update("§2불가침§7: §b" + df.format(count * 0.05) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
			getPlayer().setGameMode(previousGamemode == GameMode.SPECTATOR ? GameMode.SURVIVAL : previousGamemode);
			cooldown.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(priority = 999)
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity().equals(getPlayer()) && !e.isCancelled()) {
			healstack += e.getAmount();
			if (healstack >= healhp) {
				while (healstack >= healhp) {
					if (cooldown.isRunning()) cooldown.setCount((int) (cooldown.getCount() * 0.5));
					healstack -= healhp;
					SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 2);
				}
			}
		}
	}
	
	@SubscribeEvent(priority = 999)
	public void onGameModeChange(PlayerGameModeChangeEvent e) {
		if (!e.isCancelled() && e.getPlayer().equals(getPlayer())) {
			if (!e.getPlayer().getGameMode().equals(GameMode.SPECTATOR) && e.getNewGameMode().equals(GameMode.SPECTATOR)) {
				useable = true;
			}
		}
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (!cooldown.isCooldown() && !skill.isRunning()) {
				return skill.start();
			}
		}
		return false;
	}
	
	@SubscribeEvent(priority = 999, onlyRelevant = true, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (skill.isRunning() && e.getEntity().equals(getPlayer())) {
			e.setDamage(Math.min(1, e.getDamage()));
		}
	}
	
	@SubscribeEvent(priority = 999, ignoreCancelled = true)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = 999, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent(priority = 999, ignoreCancelled = true)
	public void onSetHealth(PlayerSetHealthEvent e) {
		if (skill.isRunning() && e.getPlayer().equals(getPlayer())) {
			if (e.getPlayer().getHealth() - e.getHealth() > 1) {
				e.setHealth(e.getPlayer().getHealth() - 1);	
			}
		}
	}

}
