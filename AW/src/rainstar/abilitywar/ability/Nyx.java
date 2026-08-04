package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Rooted;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "닉스", rank = Rank.L, species = Species.GOD, explain = {
		"§7패시브 §8- §8암흑 도래§f: 세계의 시간이 §e낮§f에서 §5밤§f이 될 때, §9고요한 자정§f 상태가 됩니다.",
		" §5밤§f에서 §e낮§f이 될 때, 잃은 체력의 $[RECOVERY]%를 회복합니다.",
		"§7패시브 §8- §3밤의 여신§f: §5밤§f에는 항상 신속을 1 얻고 상태이상을 받지 않습니다.",
		"§7패시브 §8- §c권능 소실§f: §e낮§f에는 공격력이 $[DEAL_DAMAGE_DECREASE]%, 받는 피해량이 $[TAKE_DAMAGE_DECREASE]% 감소합니다.",
		"§7검 우클릭 §8- §b빛과 어둠§f: §e낮§5밤§f이 바뀔 때까지 세계의 시간을 매우 빠르게 §3가속§f시킵니다.",
		" 이후, $[UNTARGETABLE_DURATION]초간 §3타게팅 불가§f 상태가 됩니다. $[COOLDOWN]",
		"§7상태 §8- §9고요한 자정§f: $[MIDNIGHT_DURATION]초간 §b무적§f 및 공격력이 대폭 감소하고, §a근접 공격§f할 때마다",
		" 기본 무적을 무시, §2묶어두고§f, 자신의 모든 능력 §c쿨타임§f이 1초씩 감소합니다.",
		" 종료될 때 §7(타격 횟수 × √밤낮 변경 횟수 × 1.2)§f%만큼 다음 §c근접 공격력§f이 강화됩니다.",
		"§b[§7아이디어 제공자§b] §8mqt0§f"
		})
public class Nyx extends AbilityBase {
	
	public Nyx(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> RECOVERY = rsAbilitySettings.new SettingObject<Integer>(Nyx.class,
			"recovery", 40, "# 잃은 체력 회복량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DEAL_DAMAGE_DECREASE = rsAbilitySettings.new SettingObject<Integer>(Nyx.class,
			"deal-damage-decrease", 30, "# 공격력 감소 수치", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> TAKE_DAMAGE_DECREASE = rsAbilitySettings.new SettingObject<Integer>(Nyx.class,
			"take-damage-decrease", 20, "# 받는 피해 감소 수치", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(Nyx.class,
			"cooldown", 60, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Double> UNTARGETABLE_DURATION = rsAbilitySettings.new SettingObject<Double>(Nyx.class,
			"untargetable-duration", 5.0, "# 타게팅 불가 지속 시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> MIDNIGHT_DURATION = rsAbilitySettings.new SettingObject<Double>(Nyx.class,
			"midnight-duration", 7.0, "# 고요한 자정 지속 시간") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
	private final double recovery = RECOVERY.getValue() * 0.01;
	private final double dealDamageDecrease = DEAL_DAMAGE_DECREASE.getValue() * 0.01;
	private final double takeDamageDecrease = TAKE_DAMAGE_DECREASE.getValue() * 0.01;
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._50);
	private final int midnightDuration = (int) (MIDNIGHT_DURATION.getValue() * 20);
	private final int untargetableDuration = (int) (UNTARGETABLE_DURATION.getValue() * 20);
	private ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel(), ac3 = newActionbarChannel();
	private DecimalFormat df = new DecimalFormat("0.0");
	private final List<RGB> gradations = Gradient.createGradient(30, RGB.of(104, 65, 128), RGB.of(42, 215, 219));
	private int attackstack = 0, timestack = 0;
	private boolean nextDamageUp = false;
	private BossBar bossBar = null;
	
	private boolean wasNight = false;
	
	private static boolean isNight(long worldtime) {
		return worldtime >= 12300;
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			long time = getPlayer().getWorld().getTime();
			if (wasNight && !isNight(time)) {
				wasNight = false;
				timestack++;
				double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), ((maxHealth - getPlayer().getHealth()) * recovery), RegainReason.CUSTOM);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					Healths.setHealth(getPlayer(), getPlayer().getHealth() + event.getAmount());
					SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 2f);
					ParticleLib.HEART.spawnParticle(getPlayer().getEyeLocation(), 1, 1, 1, 10, 1);
				}
			} else if (!wasNight && isNight(time)) {
				wasNight = true;
				timestack++;
				if (!midnight.isRunning()) midnight.start();
				else midnight.setCount(midnightDuration);
			}
			
			if (isNight(time)) {
				ac1.update("§5§l밤");
				PotionEffects.SPEED.addPotionEffect(getPlayer(), 40, 0, false);
			} else {
				ac1.update("§e§l낮");
			}
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer timeControl = new AbilityTimer() {
		
		private boolean startTime;
		
		@Override
		public void onStart() {
			SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.75f);
			startTime = isNight(getPlayer().getWorld().getTime());
		}
		
		@Override
		public void run(int count) {
			if ((startTime && !isNight(getPlayer().getWorld().getTime())) || (!startTime && isNight(getPlayer().getWorld().getTime()))) stop(false);
			getPlayer().getWorld().setTime(getPlayer().getWorld().getTime() + 350);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			untargetable.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer untargetable = new AbilityTimer(untargetableDuration) {
		
		@Override
		public void onStart() {
			getParticipant().attributes().TARGETABLE.setValue(false);
			ac2.update("§3타게팅 불가§7: §f" + df.format(untargetableDuration / 20.0) + "초");
		}
		
		@Override
		public void run(int count) {
			ParticleLib.TOWN_AURA.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.65, 0.65, 0.65, 20, 1);
			ac2.update("§3타게팅 불가§7: §f" + df.format(count / 20.0) + "초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac2.update(null);
			getParticipant().attributes().TARGETABLE.setValue(true);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer midnight = new AbilityTimer(midnightDuration) {
		
		private double size;
		private boolean add;
		private int stack;
		private Circle circle;
		
		@Override
		public void onStart() {
			bossBar = Bukkit.createBossBar("§9고요한 자정§7: §5" + df.format(midnightDuration / 20.0) + "§f초", BarColor.PURPLE, BarStyle.SOLID);
    		bossBar.setProgress(1);
			bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
			size = 0.75;
			add = true;
			stack = 0;
			circle = Circle.of(size, (int) (size * 33));
		}
		
		@Override
		public void run(int count) {
			if (add && size >= 2.0) add = false;
			else if (!add && size <= 0.75) add = true;

			size = add ? size + 0.1 : size - 0.1;
			stack = stack < 30 ? stack + 1 : 0;
			
			circle = Circle.of(size, (int) (size * 33));
			
			for (Location location : circle.toLocations(getPlayer().getLocation().add(0, 1, 0))) {
				ParticleLib.REDSTONE.spawnParticle(location, gradations.get(Math.max(0, stack - 1)));
			}
			
			bossBar.setTitle("§9고요한 자정§7: §5" + df.format(count / 20.0) + "§f초");
			bossBar.setProgress((double) count / midnightDuration);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
			ac3.update("§c다음 공격력§7: §e+" + df.format(attackstack * Math.sqrt(timestack) * 1.2) + "%");
			NMS.sendTitle(getPlayer(), "", "§4[§c§l+" + df.format(attackstack * Math.sqrt(timestack) * 1.2) + "%§4]", 0, 100, 0);
			nextDamageUp = true;
			getParticipant().attributes().TARGETABLE.setValue(true);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent()
	private void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getPlayer().equals(getPlayer()) && e.getItem() != null && (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
			ItemStack mainhand = getPlayer().getInventory().getItemInMainHand();
			if ((e.getItem().getType().toString().endsWith("SWORD") && mainhand.getType().toString().endsWith("SWORD"))) {
				if (!cooldown.isCooldown()) {
					timeControl.start();
					cooldown.start();
				}
			}
		}
	}
	
	@SubscribeEvent()
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		long time = e.getEntity().getWorld().getTime();
		if (e.getEntity().equals(getPlayer())) {
			if (midnight.isRunning()) e.setCancelled(true);
			else if (!isNight(time)) e.setDamage(e.getDamage() * (1 - takeDamageDecrease));
		}
		
		Player damager = null;
		if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		else if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		}
		
		if (damager != null && getPlayer().equals(damager) && e.getEntity() instanceof Player) {
			if (!isNight(time)) e.setDamage(e.getDamage() * (1 - dealDamageDecrease));
			if (midnight.isRunning()) {
				Player victim = (Player) e.getEntity();
				SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(getPlayer().getLocation(), 1, 2);
				ParticleLib.ITEM_CRACK.spawnParticle(victim.getLocation().clone().add(0, 1, 0), 0, 0, 0, 45, 0.3, MaterialX.CLOCK);
				e.setDamage(e.getDamage() * 0.05);
				if (getPlayer().equals(e.getDamager())) {
					AbilityBase ab = getParticipant().getAbility();
					for (GameTimer t : ab.getRunningTimers()) {
						if (t instanceof Cooldown.CooldownTimer) {
							t.setCount(Math.max(0, t.getCount() - 1));
						}
					}
					if (getGame().isParticipating(victim)) Rooted.apply(getGame().getParticipant(victim), TimeUnit.TICKS, 1);
					new BukkitRunnable() {
						@Override
						public void run() {
							victim.setNoDamageTicks(0);
						}
					}.runTaskLater(AbilityWar.getPlugin(), 1L);
					attackstack++;
					ac3.update("§c다음 공격력§7: §e+" + df.format(attackstack * Math.sqrt(timestack) * 1.2) + "%");
					NMS.sendTitle(getPlayer(), "", "§c+" + df.format(attackstack * Math.sqrt(timestack) * 1.2) + "%", 0, 20, 0);
				}
			}
			
			if (nextDamageUp && getPlayer().equals(e.getDamager()) && !e.getCause().equals(DamageCause.THORNS)) {
				e.setDamage(e.getDamage() + (e.getDamage() * (attackstack * Math.sqrt(timestack) * 0.013)));
				ParticleLib.ITEM_CRACK.spawnParticle(e.getEntity().getLocation().clone().add(0, 1, 0), 0, 0, 0, 100, 0.3, MaterialX.NETHER_STAR);
				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(e.getEntity().getLocation(), 1, 0.5f);
				ac3.update(null);
				NMS.clearTitle(getPlayer());
				attackstack = 0;
				nextDamageUp = false;
			}
		}
	}
	
	@SubscribeEvent()
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			long time = e.getEntity().getWorld().getTime();
			if (midnight.isRunning()) e.setCancelled(true);
			else if (!isNight(time)) e.setDamage(e.getDamage() * (1 - takeDamageDecrease));
		}
	}
	
	@SubscribeEvent()
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer())) {
			long time = e.getEntity().getWorld().getTime();
			if (midnight.isRunning()) e.setCancelled(true);
			else if (!isNight(time)) e.setDamage(e.getDamage() * (1 - takeDamageDecrease));
		}
	}
	
	@SubscribeEvent()
	private void onParticipantPreEffectApply(ParticipantPreEffectApplyEvent e) {
		if (e.getParticipant().equals(getParticipant()) && isNight(e.getPlayer().getWorld().getTime())) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent()
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId()) && midnight.isRunning()) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent()
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}
	
}
