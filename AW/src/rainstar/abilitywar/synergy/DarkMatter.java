package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.Gradient;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil.Vectors;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.math.geometry.Crescent;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "암흑 물질", rank = Rank.L, species = Species.OTHERS, explain = {
		"§7패시브 §8- §9보이지 않는 질량§f: 다른 생명체를 공격하면 그 생명체에게",
		" $[DURATION]초간 §9표식§f을 부여합니다. §9표식§f이 부여된 대상은 $[RANGE]칸 내에서",
		" 내게 끌려오며, §9표식§f 당 끌려오는 세기가 강력해집니다. §9표식§f이 5개가 되면",
		" §9표식§f을 터뜨려서 $[STUN]초간 §e§n기절§f시키고 $[UNIT_COOLDOWN]를 가집니다.",
		"§7철괴 우클릭 §8- §e월진§f: §9표식§f이 자신에게 §a끌려올 지§f, §b밀어낼 지§f 정합니다.",
		" 끌려온다면 §a근거리§f, 밀어낸다면 §b원거리§f 피해를 $[DAMAGE_DECREASE]% 덜 받습니다.",
		"§7패시브 §8- §b은하의 밀도§f: 대상이 가진 벡터에 비례해 가하는 피해가 증가합니다.",
		"§b[§7아이디어 제공자§b] §e_LunaEunha_§f"
		})
public class DarkMatter extends Synergy implements ActiveHandler {
	
	public DarkMatter(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Double> RANGE = rsSynergySettings.new SettingObject<Double>(DarkMatter.class,
			"range", 12.0, "# 끌/밀어내는 범위", "# 단위: 칸") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> DURATION = rsSynergySettings.new SettingObject<Double>(DarkMatter.class,
			"duration", 6.0, "# 표식의 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> STUN = rsSynergySettings.new SettingObject<Double>(DarkMatter.class,
			"stun", 1.5, "# 표식 폭발의 기절 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DAMAGE_DECREASE = 
			rsSynergySettings.new SettingObject<Integer>(DarkMatter.class, "damage-decrease", 35,
            "# 대미지 감소율", "# 단위: %") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
    };
	
	public static final SettingObject<Integer> UNIT_COOLDOWN = 
			rsSynergySettings.new SettingObject<Integer>(DarkMatter.class, "unit-cooldown", 10,
            "# 유닛별 쿨타임", "# 단위: 초") {

        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        
		@Override
		public String toString() {
			return "§c유닛별 쿨타임 §7: §f" + getValue() + "초";
        }

    };
	
	private final double range = RANGE.getValue();
	private final int duration = (int) (DURATION.getValue() * 20);
	private final int stun = (int) (STUN.getValue() * 20);
	private final int unitCooldown = (int) ((UNIT_COOLDOWN.getValue() * 1000) * Wreck.calculateDecreasedAmount(100));
	private final double decrease = 1 - (DAMAGE_DECREASE.getValue() * 0.01);
	
	private final Map<UUID, Long> unitcooldowns = new HashMap<>();
	private ActionbarChannel ac = newActionbarChannel();
	private boolean grab = true;
	private int particleSide = 45;
	private static final List<RGB> gradation1 = Gradient.createGradient(30, RGB.of(250, 220, 157), RGB.of(235, 200, 21)), gradation2 = Gradient.createGradient(60, RGB.of(160, 215, 223), RGB.of(1, 221, 227), RGB.of(76, 44, 227), RGB.of(1, 221, 227), RGB.of(160, 215, 223), RGB.of(250, 250, 250));
	private int colorstack1 = 0, colorstack2 = 0;
	private boolean turns1, turns2;
	private static final Crescent crescent = Crescent.of(1, 20);
	private static final Note[] NOTES = {
			Note.natural(0, Tone.E),
			Note.sharp(1, Tone.G),
			Note.natural(1, Tone.A),
			Note.sharp(1, Tone.C)
	};
	
	private final Circle circle = Circle.of(range, (int) Math.min(range * 10, 200));
	
	private final Map<UUID, Stack> stackMap = new HashMap<>();
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
			ac.update(grab ? "§e인력" : "§b척력");
			passive.start();
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
			grab = !grab;
			ac.update(grab ? "§e인력" : "§b척력");
			SoundLib.BLOCK_ENCHANTMENT_TABLE_USE.playSound(getPlayer().getLocation(), 1, 2);
		}
		return false;
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {			
			if (colorstack1 == 29 || colorstack1 == 0) turns1 = !turns1;
			colorstack1 = turns1 ? Math.min(colorstack1 + 1, 29) : Math.max(colorstack1 - 1, 0);
			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(loc, gradation1.get(colorstack1));
			}
			
			for (LivingEntity entity : LocationUtil.getEntitiesInCircle(LivingEntity.class, getPlayer().getLocation(), range, predicate)) {
				if (stackMap.containsKey(entity.getUniqueId())) {
					if (grab) entity.setVelocity(VectorUtil.validateVector(getPlayer().getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().setY(0).multiply(0.07 * stackMap.get(entity.getUniqueId()).stack)));
					else entity.setVelocity(VectorUtil.validateVector(entity.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().setY(0).multiply(0.07 * stackMap.get(entity.getUniqueId()).stack)));
				}
			}
		}

	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(e.getEntity())) {
			if ((e.getDamager() instanceof Player && grab) || (e.getDamager() instanceof Projectile && !grab)) e.setDamage(e.getDamage() * decrease);
		}
		
		if (getPlayer().equals(damager)) {
			double speed = e.getEntity().getVelocity().length();
			double bonus = 1 + Math.min(speed * 0.8, 2.0);
			new Holograms(e.getEntity().getLocation(), bonus).start();
			e.setDamage(e.getDamage()  * bonus);
			
			if (e.getEntity() instanceof LivingEntity && !e.isCancelled() && predicate.test(e.getEntity())) {
				new CutParticle(particleSide).start();
				SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer());
				particleSide *= -1;
				if (stackMap.containsKey(e.getEntity().getUniqueId())) {
					if (stackMap.get(e.getEntity().getUniqueId()).addStack()) {
						if (getGame().isParticipating(e.getEntity().getUniqueId()))
							Stun.apply(getGame().getParticipant(e.getEntity().getUniqueId()), TimeUnit.TICKS, stun);
					}
				} else if (System.currentTimeMillis() - unitcooldowns.getOrDefault(e.getEntity().getUniqueId(), 0L) >= unitCooldown) new Stack((LivingEntity) e.getEntity()).start();
			}
		}
	}
	
	@SubscribeEvent
	private void onDeath(EntityDeathEvent e) {
		if (stackMap.containsKey(e.getEntity().getUniqueId())) stackMap.get(e.getEntity().getUniqueId()).stop(true);
	}

	@SubscribeEvent
	private void onDeath(PlayerDeathEvent e) {
		if (stackMap.containsKey(e.getEntity().getUniqueId())) stackMap.get(e.getEntity().getUniqueId()).stop(true);
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerJoin(PlayerJoinEvent e) {
		for (Stack stack : stackMap.values()) {
			stack.hologram.display(getPlayer());
		}
	}
	
	private class Stack extends AbilityTimer {

		private final LivingEntity entity;
		private final IHologram hologram;
		private int stack = 0;

		private Stack(LivingEntity entity) {
			super(duration);
			setPeriod(TimeUnit.TICKS, 1);
			this.entity = entity;
			this.hologram = NMS.newHologram(entity.getWorld(), entity.getLocation().getX(), entity.getLocation().getY() + entity.getEyeHeight() + 0.6, entity.getLocation().getZ(), Strings.repeat("§b✦", stack).concat(Strings.repeat("§7✧", 5 - stack)));
			hologram.display(getPlayer());
			stackMap.put(entity.getUniqueId(), this);
			addStack();
		}

		@Override
		protected void run(int count) {
			hologram.teleport(entity.getWorld(), entity.getLocation().getX(), entity.getLocation().getY() + entity.getEyeHeight() + 0.6, entity.getLocation().getZ(), entity.getLocation().getYaw(), 0);
		}
		
		private boolean addStack() {
			setCount(duration);
			stack++;
			String[] colors = {"§b", "§3", "§f"};

			StringBuilder text = new StringBuilder();

			for (int i = 0; i < stack; i++) {
				text.append(colors[i % colors.length]).append("✦");
			}

			for (int i = stack; i < 5; i++) {
				text.append("§7✧");
			}

			hologram.setText(text.toString());
			SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(entity.getLocation(), 1, 1.75f);
			if (stack >= 5) {
				stop(false);
				new SoundTimer(getPlayer()).start();
				final long current = System.currentTimeMillis();
				unitcooldowns.put(entity.getUniqueId(), current);
				return true;
			}
			return false;
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			hologram.unregister();
			stackMap.remove(entity.getUniqueId());
		}
	}
	
	private class CutParticle extends AbilityTimer {

		private final Vector axis;
		private final Vector vector;
		private final Vectors crescentVectors;

		private CutParticle(double angle) {
			super(4);
			setPeriod(TimeUnit.TICKS, 1);
			this.axis = VectorUtil.rotateAroundAxis(VectorUtil.rotateAroundAxisY(getPlayer().getLocation().getDirection().setY(0).normalize(), 90), getPlayer().getLocation().getDirection().setY(0).normalize(), angle);
			this.vector = getPlayer().getLocation().getDirection().setY(0).normalize().multiply(0.5);
			this.crescentVectors = crescent.clone()
					.rotateAroundAxisY(-getPlayer().getLocation().getYaw())
					.rotateAroundAxis(getPlayer().getLocation().getDirection().setY(0).normalize(), (180 - angle) % 180)
					.rotateAroundAxis(axis, -75);
		}

		@Override
		protected void run(int count) {
			Location baseLoc = getPlayer().getLocation().clone().add(vector).add(0, 1.3, 0);
			if (colorstack2 == gradation2.size() - 1 || colorstack2 == 0)
			    turns2 = !turns2;

			colorstack2 = turns2 ? Math.min(colorstack2 + 1, gradation2.size() - 1) : Math.max(colorstack2 - 1, 0);
			
			for (Location loc : crescentVectors.toLocations(baseLoc)) {
				ParticleLib.REDSTONE.spawnParticle(loc, gradation2.get(colorstack2));
			}
			crescentVectors.rotateAroundAxis(axis, 40);
		}

	}
	
	private class SoundTimer extends AbilityTimer {

		private final Player center;

		private SoundTimer(Player center) {
			super(TaskType.NORMAL, NOTES.length);
			setPeriod(TimeUnit.TICKS, 3);
			this.center = center;
		}

		@Override
		protected void run(int count) {
			SoundLib.CHIME.playInstrument(center.getLocation(), .4f, NOTES[count - 1]);
		}

	}
	
	private class Holograms extends AbilityTimer {
		
		private final IHologram hologram;
		private Random random = new Random();
		private final DecimalFormat multiplyDF = new DecimalFormat("0.00");
		private double multiply;
		
		private Holograms(Location hitLoc, double multiply) {
			super(TaskType.REVERSE, 30);
			setPeriod(TimeUnit.TICKS, 1);
			this.hologram = NMS.newHologram(getPlayer().getWorld(), 
					hitLoc.getX() + (((random.nextDouble() * 2) - 1) * 0.5),
					hitLoc.getY() + 1.25 + (((random.nextDouble() * 2) - 1) * 0.25), 
					hitLoc.getZ() + (((random.nextDouble() * 2) - 1) * 0.5), 
					"§3§l× §b§l" + multiplyDF.format(multiply));
			this.multiply = multiply;
			for (Player player : getPlayer().getWorld().getPlayers()) {
				hologram.display(player);
			}
		}
		
		@Override
		protected void run(int count) {
			if (count <= 8) {
				if (count % 2 == 0) hologram.setText("§7§l× §8§l" + multiplyDF.format(multiply));
				else hologram.setText("§3§l× §b§l" + multiplyDF.format(multiply));
			} else hologram.setText("§3§l× §b§l" + multiplyDF.format(multiply));
			hologram.teleport(hologram.getLocation().add(0, 0.015, 0));	
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
		}
		
	}
	
}
