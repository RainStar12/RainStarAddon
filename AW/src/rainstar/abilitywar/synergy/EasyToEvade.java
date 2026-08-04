package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.raytrace.RayTrace;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "하지만 이렇게 간단하게 피했습니다", rank = Rank.S, species = Species.HUMAN, explain = {
		"피해를 받지 않은 상태에서, 사거리 $[RANGE] 이내의 적이 근접 공격을 2회 실패하거나,",
		"범위 내에 투사체가 착탄될 때마다 §d회§b피§f 스택을 최대 $[MAX_EVADE]까지 쌓습니다.",
		"피해를 받으면, §d회§b피§f 스택을 소모하여 피해를 무효화하고 살짝 뒤로 이동합니다.",
		"이때 그 적에게 짧은 §7실명§f을 부여합니다."
		})
public class EasyToEvade extends Synergy {
	
	public EasyToEvade(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> RANGE =
			rsSynergySettings.new SettingObject<Double>(EasyToEvade.class, "range", 5.0,
			"# 회피 스택 증가 범위", "# 단위: 칸") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> MAX_EVADE =
			rsSynergySettings.new SettingObject<Integer>(EasyToEvade.class, "max-evade", 5,
			"# 최대 회피 스택") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final double range = RANGE.getValue();
	private final int maxEvade = MAX_EVADE.getValue();
	private int evade = 0;
	private ActionbarChannel ac = newActionbarChannel();
	private Circle circle = Circle.of(range, (int) (range * 12));
	private static final List<RGB> gradation = Gradient.createGradient(100, RGB.of(254, 244, 252), RGB.of(230, 235, 251));
	private int colorstack = 0;
	private boolean colordirection = false;
	private Set<Player> attackFail = new HashSet<>();
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
    	public void run(int count) {
			if (colorstack == 99 || colorstack == 0) colordirection = !colordirection;
			colorstack = colordirection ? Math.min(colorstack + 1, 99) : Math.max(colorstack - 1, 0);
			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, gradation.get(colorstack));
			}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 2).register();
	
	private void addEvadeStack() {
		if (evade >= maxEvade) return;
		
		evade++;
		
		SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer(), 1f, 1.7f);
		ParticleLib.END_ROD.spawnParticle(getPlayer().getLocation().clone().add(0, 1, 0), 0.2, 0.4, 0.2, 15, 0);
		
		ac.update("§d회§b피§7: §3" + evade);
	}
	
	@SubscribeEvent
	public void onSwing(PlayerAnimationEvent e) {
		if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
		if (e.getPlayer().getInventory().getItemInMainHand().getType().isBlock()) return;

		Player attacker = e.getPlayer();
		if (attacker.equals(getPlayer())) return;
		if (!attacker.isValid() || attacker.isDead()) return;
		if (attacker.getLocation().distanceSquared(getPlayer().getLocation()) > range * range) return;

		Location start = attacker.getEyeLocation();
		Vector dir = start.getDirection().normalize().multiply(0.35);
		Location last = start.clone();

		CustomEntity dummy = getGame().new CustomEntity(attacker.getWorld(), start.getX(), start.getY(), start.getZ()) {

			@Override
			protected void onRemove() {
				// TODO Auto-generated method stub
				
			}}.resizeBoundingBox(-0.3, -0.3, -0.3, 0.3, 0.3, 0.3);

		for (double d = 0; d <= 3.2; d += 0.35) {
			Location loc = last.clone().add(dir);
			dummy.setLocation(loc);

			if (RayTrace.hitsBlock(loc.getWorld(), last.getX(), last.getY(), last.getZ(), loc.getX(), loc.getY(), loc.getZ())) break;

			boolean hit = false;
			for (@SuppressWarnings("unused") LivingEntity entity : LocationUtil.getConflictingEntities(LivingEntity.class, loc.getWorld(), dummy.getBoundingBox(), entity -> !entity.equals(attacker))) {
				hit = true;
				break;
			}

			if (hit) {
				dummy.remove();
				return;
			}

			last = loc;
		}

		dummy.remove();
		if (attackFail.contains(attacker)) {
			addEvadeStack();
			attackFail.remove(attacker);
		} else attackFail.add(attacker);
	}
	
	@SubscribeEvent(ignoreCancelled = true)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (damager != null && evade > 0 && getPlayer().equals(e.getEntity()) && !e.getCause().equals(DamageCause.THORNS)) {
			evade--;
			e.setCancelled(true);
			SoundLib.ENTITY_WITCH_THROW.playSound(getPlayer().getLocation(), 1, 2f);
			ParticleLib.CLOUD.spawnParticle(getPlayer().getLocation(), 0.7, 0, 0.7, 25, 0);
			getPlayer().setVelocity(VectorUtil.validateVector(getPlayer().getLocation().getDirection().clone().setY(0).normalize().multiply(-1.5)));
			damager.sendMessage("§c[§e능력§c] §b" + getPlayer().getName() + "§7: §b§o하지만! ...이렇게 간단하게 피했습니다!");
			PotionEffects.BLINDNESS.addPotionEffect(damager, 20, 0, true);
			ac.update("§d회§b피§7: §3" + evade);
		}
	}
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.getHitEntity() == null && !getPlayer().equals(e.getEntity().getShooter())) {
			if (e.getHitBlock().getLocation().distanceSquared(getPlayer().getLocation()) <= range * range) {
				addEvadeStack();
			}
		}
	}

}
