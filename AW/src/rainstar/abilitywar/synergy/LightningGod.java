package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Oppress;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.ElectricShock;

@AbilityManifest(name = "뇌신", rank = Rank.L, species = Species.GOD, explain = {
		"§7기본 공격 §8- §e벼락§f: 바라보는 방향으로 §e번개§f를 연속으로 떨어뜨릴 수 있습니다.",
		" §e번개§f는 $[RANGE_THOR]칸까지 나아가며 떨어집니다. 사용 후 $[PERIOD]초 후에 재사용 가능합니다.",
		"§7패시브 §8- §b집전§f: 자신으로부터 $[RANGE_LIGHTNING]칸 이내의 §e번개§f는 적중 시 추가로",
		" 손에 든 무기 피해량의 $[DAMAGE]%를 §b트루 대미지§f로 입힙니다.",
		" 또한 대상을 $[ELECTRIC_SHOCK_DURATION]초간 §d§n감전§f시키고, 이미 대상이 가진 §d§n감전§f이 $[ELECTRIC_SHOCK_TURN_STUN]초 이상이라면",
		" 대신 $[STUN_DURATION]초간 §e§n기절§f시킵니다.",
		"§7패시브 §8- §b천공의 지배자§f: §c§n제압§f되지 않습니다. §b공중§f에 있는 동안 타게팅의 대상이",
		" 되지 않습니다. 낙하, 번개, 화염, 감전 피해를 입지 않습니다."
		})
public class LightningGod extends Synergy {

	public LightningGod(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> PERIOD =
			rsSynergySettings.new SettingObject<Double>(LightningGod.class, "period", 7.5,
			"# 벼락 쿨타임", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> RANGE_THOR =
			rsSynergySettings.new SettingObject<Double>(LightningGod.class, "thor-range", 10.0,
			"# 벼락의 번개 사거리", "# 단위: 칸") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> RANGE_LIGHTNING =
			rsSynergySettings.new SettingObject<Double>(LightningGod.class, "lightning-range", 15.0,
			"# 집전의 번개 효과적용 사거리", "# 단위: 칸") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DAMAGE =
			rsSynergySettings.new SettingObject<Integer>(LightningGod.class, "damage", 18,
			"# 집전의 번개 추가 피해량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> ELECTRIC_SHOCK_DURATION =
			rsSynergySettings.new SettingObject<Double>(LightningGod.class, "electric-shock-duration", 1.5,
			"# 집전의 감전 지속시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> ELECTRIC_SHOCK_TURN_STUN =
			rsSynergySettings.new SettingObject<Double>(LightningGod.class, "electric-shock-turn-stun", 10.0,
			"# 집전의 감전이 기절로 바뀌는 기준치", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> STUN_DURATION =
			rsSynergySettings.new SettingObject<Double>(LightningGod.class, "stun-duration", 1.0,
			"# 집전의 기절 지속시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	private final int lightningCool = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(50) * PERIOD.getValue() * 20 : PERIOD.getValue() * 20);
	private final double thorRange = RANGE_THOR.getValue(), lightningRange = RANGE_LIGHTNING.getValue();
	private final double lightningDamage = DAMAGE.getValue() * 0.01;
	private final int electricShockDuration = (int) (ELECTRIC_SHOCK_DURATION.getValue() * 20);
	private final int electricShockTurn = (int) (ELECTRIC_SHOCK_TURN_STUN.getValue() * 20);
	private final int stunDuration = (int) (STUN_DURATION.getValue() * 20);
	
	private Circle circle = Circle.of(lightningRange, (int) Math.min(200, lightningRange * 10));
	private ActionbarChannel ac = newActionbarChannel();
	private final List<RGB> gradation = Gradient.createGradient(30, RGB.of(254, 254, 254), RGB.of(254, 246, 135), RGB.of(251, 204, 20));
	
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
	
	private AbilityTimer passive = new AbilityTimer() {
		
		private int stack = 0;
		private boolean turns = true;
		
		@SuppressWarnings("deprecation")
		@Override
		public void run(int count) {
			ac.update((lightningCooldown.isRunning() ? "§7" : "§e") + "§l⚡");
			
			if (turns) stack++;
			else stack--;
			if (stack % (gradation.size() - 1) == 0) {
				turns = !turns;
			}
			
			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, gradation.get(stack));
			}
			
			if (!getPlayer().isOnGround()) {
				getParticipant().attributes().TARGETABLE.setValue(false);
			} else {
				getParticipant().attributes().TARGETABLE.setValue(true);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private AbilityTimer lightningCooldown = new AbilityTimer(lightningCool) {
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(final PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (!lightningCooldown.isRunning()) {
				new Thor(getPlayer().getLocation(), getPlayer().getLocation().getDirection().clone().setY(0).normalize(), lightningRange).start();
				lightningCooldown.start();
			}
		}
	}
	
	@SubscribeEvent(ignoreCancelled = true)
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			if (e.getEntity().equals(getPlayer())) {
				if (!e.isCancelled() && getPlayer().equals(e.getEntity()) && e.getCause().equals(DamageCause.FALL)) {
					e.setCancelled(true);
					getPlayer().sendMessage("§b낙하 피해를 입지 않습니다.");
					SoundLib.ENTITY_BAT_TAKEOFF.playSound(getPlayer());
				}
				
				if (e.getCause().equals(DamageCause.LIGHTNING) || e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION) || e.getCause().equals(DamageCause.FIRE) || e.getCause().equals(DamageCause.FIRE_TICK)) {
					e.setCancelled(true);
				}
			} else if (e.getCause().equals(DamageCause.LIGHTNING)) {
				Player player = (Player) e.getEntity();
				if (player.getLocation().distanceSquared(getPlayer().getLocation()) <= lightningRange * lightningRange) {
					double damage = getPlayer().getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue();
					if (getPlayer().getInventory().getItemInMainHand() != null && getPlayer().getInventory().getItemInMainHand().containsEnchantment(Enchantment.DAMAGE_ALL)) damage += ((getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL) + 1) * 0.5);
					damage *= lightningDamage;
					if (!e.isCancelled()) Healths.setHealth(player, Math.max(1, player.getHealth() - damage));
					if (predicate.test(player)) {
						Participant participant = getGame().getParticipant(player);
						if (participant.hasEffect(ElectricShock.registration) && participant.getEffects(ElectricShock.registration).iterator().next().getRemainingTicks() >= electricShockTurn) {
							Stun.apply(participant, TimeUnit.TICKS, stunDuration);
						} else ElectricShock.apply(participant, TimeUnit.TICKS, electricShockDuration);
					}
				}
			}	
		}
	}
	
	@SubscribeEvent
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		
		if (e.getDamager() instanceof ArmorStand && e.getEntity().equals(getPlayer())) {
			ArmorStand stand = (ArmorStand) e.getDamager();
		    if (stand.hasMetadata("ElectricShock")) {
		        e.setCancelled(true);
		    }
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onEffectApply(ParticipantPreEffectApplyEvent e) {
		if (e.getEffectType() == Oppress.registration) {
			e.setCancelled(true);
		}
	}
	
	private class Thor extends AbilityTimer {

		private final Vector direction;
		private final Location startLocation;
		private Location current;

		public Thor(Location startLocation, Vector direction, double range) {
			super(TaskType.REVERSE, 100);
			setPeriod(TimeUnit.TICKS, 2);
			this.startLocation = startLocation.clone();
			this.current = startLocation.clone();
			this.direction = direction;
		}

		@Override
		protected void run(int count) {
			current.add(direction);
			current.getWorld().strikeLightning(LocationUtil.floorY(current));
			if (current.distanceSquared(startLocation) >= thorRange * thorRange) {
				stop(false);
				return;
			}
		}
	}
	
}
