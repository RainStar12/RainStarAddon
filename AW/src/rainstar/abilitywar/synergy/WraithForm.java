package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.text.DecimalFormat;

import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.EvilSpirit;
import daybreak.abilitywar.game.manager.effect.Fear;
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

@AbilityManifest(name = "유령의 형상", rank = Rank.L, species = Species.UNDEAD, explain = {
		"§7철괴 좌클릭 §8- §3범접 불가§f: $[INTANGIBLE_DURATION]초간 §2불가침§f이 됩니다. $[INTANGIBLE_COOLDOWN]",
		" 사용 후 영구적으로 받는 피해가 $[PERMANENT_DAMAGE_INCREASE]% 증가합니다.",
		"§7철괴 우클릭 §8- §b영체화§f: $[APPARITION_DURATION]초간 §3관전 상태§f가 됩니다. $[APPARITION_COOLDOWN]",
		" 자신이 §2불가침§f 상태였다면 그 시간만큼 §3관전§f 시간이 늘어납니다.",
		" 체력을 $[HP_RECOVERY]% 회복할 때마다, 남은 §c쿨타임§f이 절반으로 감소합니다.",
		"§7패시브 §8- §3영령계§f: 자신이 §3관전 상태§f인 동안은 §2불가침§f이 흐르지 않고,",
		" 매 초마다 $[RANGE]칸 내의 모든 대상에게 §b§n공포§f, §c§n악령§f을 부여합니다.",
		"§7관전 시도 §8- §c영혼 폭발§f: §3관전 상태§f에서 다른 대상에게 빙의를 시도하면,",
		" 관전해있던 시간에 비례해 초당 §c$[DAMAGE_PERCENTAGE]§8(§7최대 $[MAX_PERCENTAGE]§8)§c%§f의 §b트루 대미지§f를 입힙니다.",
		" 이후 §3관전 상태§f가 해제되기 전까지는 다시 사용할 수 없습니다.",
		"§7상태 §8- §2불가침§f: 모든 체력 손실이 §2§l1§f 이하로 고정됩니다."
		})
public class WraithForm extends Synergy implements ActiveHandler {
	
	public WraithForm(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> INTANGIBLE_DURATION = rsSynergySettings.new SettingObject<Double>(WraithForm.class,
			"intangible-duration", 5.0, "# 범접 불가 불가침 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> INTANGIBLE_COOLDOWN = rsSynergySettings.new SettingObject<Integer>(WraithForm.class,
			"intangible-cooldown", 20, "# 범접 불가 쿨타임", "# 주의! 쿨타임 감소 효과를 25%까지 받습니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> PERMANENT_DAMAGE_INCREASE = rsSynergySettings.new SettingObject<Integer>(WraithForm.class,
			"permanent-damage-increase", 10, "# 영구 받는 피해 증가량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> APPARITION_DURATION = rsSynergySettings.new SettingObject<Double>(WraithForm.class,
			"apparition-duration", 7.0, "# 영체화 관전 상태 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> APPARITION_COOLDOWN = rsSynergySettings.new SettingObject<Integer>(WraithForm.class,
			"apparition-cooldown", 100, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> HP_RECOVERY = rsSynergySettings.new SettingObject<Integer>(WraithForm.class,
			"hp-recovery", 20, "# 쿨타임 감소 조건의 체력 회복 요구량", "# 단위: 최대 체력의 %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> RANGE = rsSynergySettings.new SettingObject<Double>(WraithForm.class,
			"range", 8.0, "# 공포 및 악령 부여 범위", "# 단위: 칸") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DAMAGE_PERCENTAGE = rsSynergySettings.new SettingObject<Double>(WraithForm.class,
			"damage-percentage", 5.0, "# 영혼 폭발의 초당 피해량", "# 단위: 최대 체력의 %") {
		@Override
		public boolean condition(Double value) {
			return value >= 0.0;
		}
	};
	
	public static final SettingObject<Double> MAX_PERCENTAGE = rsSynergySettings.new SettingObject<Double>(WraithForm.class,
			"max-percentage", 75.0, "# 영혼 폭발의 최대 피해량", "# 단위: 최대 체력의 %") {
		@Override
		public boolean condition(Double value) {
			return value >= 0.0;
		}
	};
	
	private int apparitionDuration = (int) (APPARITION_DURATION.getValue() * 20);
	private int intangibleDuration = (int) (INTANGIBLE_DURATION.getValue() * 20);
	private Cooldown apparitionCooldown = new Cooldown(APPARITION_COOLDOWN.getValue());
	private Cooldown intangibleCooldown = new Cooldown(INTANGIBLE_COOLDOWN.getValue(), CooldownDecrease._25);
	private final double healpercentage = HP_RECOVERY.getValue() * 0.01;
	private final double healhp = healpercentage * getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
	private final double damageIncrease = PERMANENT_DAMAGE_INCREASE.getValue() * 0.01;
	private double range = RANGE.getValue();
	private double damagepercentage = DAMAGE_PERCENTAGE.getValue() * 0.01;
	private double maxpercentage = MAX_PERCENTAGE.getValue() * 0.01;
	private Circle circle = Circle.of(range, (int) (range * 10));
	private boolean useable = true;
	private GameMode previousGamemode;
	private double healstack = 0;
	private int specterstack = 0;
	private ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel(), ac3 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0"), df2 = new DecimalFormat("0");
	private int usestack = 0;
	
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
			ac3.update("§c받는 피해 증가량§7: §c" + df2.format(usestack * damageIncrease * 100));
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
						Fear.apply(getGame().getParticipant(player), TimeUnit.TICKS, 13, getPlayer());
						EvilSpirit.apply(getGame().getParticipant(player), TimeUnit.TICKS, 41);
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
	
	public AbilityTimer skill = new AbilityTimer(apparitionDuration) {
	
		@Override
		public void onStart() {
			if (intangible.isRunning()) this.setCount(apparitionDuration + intangible.getCount());
			ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.3, 0, 0.3, 20, 1);
			SoundLib.ENTITY_VEX_AMBIENT.playSound(getPlayer().getLocation(), 0.75f, 1.75f);
			SoundLib.ITEM_ARMOR_EQUIP_ELYTRA.playSound(getPlayer().getLocation(), 1, 0.5f);
			previousGamemode = getPlayer().getGameMode();
			getPlayer().setGameMode(GameMode.SPECTATOR);
			ac1.update("§3관전§7: §b" + df.format(apparitionDuration * 0.05) + "§f초");
		}
		
		@Override
		public void run(int count) {
			ac1.update("§3관전§7: §b" + df.format(count * 0.05) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac1.update(null);
			getPlayer().setGameMode(previousGamemode == GameMode.SPECTATOR ? GameMode.SURVIVAL : previousGamemode);
			apparitionCooldown.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer intangible = new AbilityTimer(intangibleDuration) {
		
		@Override
		public void onStart() {
			ac2.update("§2불가침§7: §b" + df.format(intangibleDuration * 0.05) + "§f초");
		}
		
		@Override
		public void run(int count) {
			ac2.update("§2불가침§7: §b" + df.format(count * 0.05) + "§f초");
			if (getPlayer().getGameMode().equals(GameMode.SPECTATOR)) this.setCount(this.getCount() + 1);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac2.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(priority = 999)
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity().equals(getPlayer()) && !e.isCancelled()) {
			healstack += e.getAmount();
			if (healstack >= healhp) {
				while (healstack >= healhp) {
					if (apparitionCooldown.isRunning()) apparitionCooldown.setCount((int) (apparitionCooldown.getCount() * 0.5));
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
		if (material == Material.IRON_INGOT) {
			if (clickType == ClickType.RIGHT_CLICK) {
				if (!apparitionCooldown.isCooldown() && !skill.isRunning()) {
					return skill.start();
				}	
			} else if (clickType == ClickType.LEFT_CLICK) {
				if (!intangibleCooldown.isCooldown()) {
					if (intangible.isRunning()) intangible.setCount(intangible.getCount() + intangibleDuration);
					else intangible.start();
					usestack++;
					ac3.update("§c받는 피해 증가량§7: §c" + df2.format(usestack * damageIncrease * 100));
					return intangibleCooldown.start();
				}
			}
		}
		return false;
	}
	
	@SubscribeEvent(priority = 999, onlyRelevant = true, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			e.setDamage(e.getDamage() * (1 + (damageIncrease * usestack)));
			if (intangible.isRunning()) {
				e.setDamage(Math.min(1, e.getDamage()));
			}
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
		if (intangible.isRunning() && e.getPlayer().equals(getPlayer())) {
			if (e.getPlayer().getHealth() - e.getHealth() > 1) {
				e.setHealth(e.getPlayer().getHealth() - 1);	
			}
		}
	}

}
