package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Fear;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.IWorldBorder;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "다모클레스의 칼", rank = Rank.L, species = Species.HUMAN, explain = {
		"적을 공격하면 그 대상의 머리 위에 §3검§f을 생성합니다.",
		"매 틱마다 §b$[CHANCE_NUMERATOR]§7/§b$[CHANCE_DENOMINATOR]§f의 확률로 대상의 머리 위 §3검§f이 떨어집니다.",
		"그 $[CHANCE_FEAR]배의 확률로, $[FEAR_DURATION]초간 §b§n공포§f에 빠뜨립니다.",
		"대상이 피해를 받을 때마다 피해량의 10배만큼 분모의 숫자가 줄어듭니다.",
		"§3검§f은 대상에게 §4치명적인 피해§8(§721억 대미지§8)§f를 입힙니다."
		})
public class SwordofDamocles extends Synergy {
	
	public SwordofDamocles(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> CHANCE_NUMERATOR = 
			rsSynergySettings.new SettingObject<Integer>(SwordofDamocles.class, "chance-numerator", 1,
			"# 확률 분자") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	public static final SettingObject<Integer> CHANCE_DENOMINATOR = 
			rsSynergySettings.new SettingObject<Integer>(SwordofDamocles.class, "chance-denominator", 10000,
			"# 확률 분모") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	public static final SettingObject<Integer> CHANCE_FEAR = 
			rsSynergySettings.new SettingObject<Integer>(SwordofDamocles.class, "chance-fear", 10,
			"# 공포 확률", "# 단위: 배", "# 분자에 곱해집니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> FEAR_DURATION = 
			rsSynergySettings.new SettingObject<Double>(SwordofDamocles.class, "fear-duration", 5.0,
			"# 공포 지속시간", "# 단위: 초") {

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
	
	private final int nume = CHANCE_NUMERATOR.getValue(), deno = CHANCE_DENOMINATOR.getValue();
	private final int fearChance = CHANCE_FEAR.getValue();
	private final int fearDuration = (int) (FEAR_DURATION.getValue() * 20);
	
	private final Map<UUID, DamoclesSword> swords = new HashMap<>();
	private final Random random = new Random();
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.ABILITY_DESTROY) {
			for (DamoclesSword sword : swords.values()) {
				sword.stop(false);
			}
			swords.clear();
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && e.getEntity() instanceof Player) {
			Player target = (Player) e.getEntity();
			if (predicate.test(target) && !e.isCancelled() && !swords.containsKey(target.getUniqueId())) {
				DamoclesSword sword = new DamoclesSword(target);
				swords.put(target.getUniqueId(), sword);
				sword.start();
			}
		}
	}
	
	@SubscribeEvent(priority = 1000, ignoreCancelled = true)
	public void onEntityTriggerByEntity(EntityDamageByEntityEvent e) {
		onEntityTrigger(e);
	}
	
	@SubscribeEvent(priority = 1000, ignoreCancelled = true)
	public void onEntityTriggerByBlock(EntityDamageByBlockEvent e) {
		onEntityTrigger(e);
	}
	
	@SubscribeEvent(priority = 1000, ignoreCancelled = true)
	public void onEntityTrigger(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player)) return;

		DamoclesSword sword = swords.get(e.getEntity().getUniqueId());

		if (sword != null) {
			sword.reduceDenominator(e.getDamage());
		}
	}
	
	@SubscribeEvent
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();

		DamoclesSword sword = swords.get(player.getUniqueId());

		if (sword != null) {
			sword.reJoin();
		}
	}
	
	@SubscribeEvent
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player player = e.getPlayer();

		DamoclesSword sword = swords.get(player.getUniqueId());

		if (sword != null) {
			sword.quit();
		}
	}
	
	private class DamoclesSword extends AbilityTimer {

		private final Player target;
		private ArmorStand armorStand;
		private int denominator = deno;
		private FallingTimer falling = null;
		private int time = 0;
		private final ActionbarChannel ac;
		private boolean fallingDamage = false;

		public DamoclesSword(Player target) {
			super(TaskType.INFINITE, -1);
			this.target = target;
			setPeriod(TimeUnit.TICKS, 1);
			ac = getGame().getParticipant(target).actionbar().newChannel();
		}

		@Override
		protected void onStart() {
			armorStand = target.getWorld().spawn(target.getLocation().clone().add(0, 3, 0), ArmorStand.class);
			armorStand.setVisible(false);
			armorStand.setGravity(false);
			armorStand.setInvulnerable(true);
			armorStand.setMetadata("DamoclesSword", new FixedMetadataValue(AbilityWar.getPlugin(), null));
			armorStand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
			armorStand.setRightArmPose(new EulerAngle(Math.toRadians(80), 0, 0));
			NMS.removeBoundingBox(armorStand);
		}

		@Override
		protected void run(int count) {
			if (target.isDead()) {
				stop(false);
				return;
			}
			if (getPlayer().isDead()) {
				stop(false);
				return;
			}
			if (falling != null && falling.isRunning()) return;
			
			time++;

			Location loc = target.getLocation().clone().add(0, 3, 0);
			loc.add(target.getLocation().getDirection().clone().setY(0).normalize().multiply(0.7));
			loc.add(VectorUtil.rotateAroundAxisY(target.getLocation().getDirection().clone().setY(0).normalize().multiply(0.4), 90));
			armorStand.teleport(loc);

			int randomnum = random.nextInt(denominator);
			
			ac.update("§c확률§f: §b" + nume + "§7/§b" + denominator + " §7→ §c" + randomnum);
			
			if (randomnum < nume * fearChance) {
				Fear.apply(getGame().getParticipant(target), TimeUnit.TICKS, fearDuration, getPlayer());
				SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(target, 1, 1);
			}
			if (randomnum < nume) {
				falling = new FallingTimer();
				falling.start();
			}
		}

		public void reduceDenominator(double damage) {
			if (!fallingDamage) denominator = Math.max(1, denominator - (int) Math.floor(damage * 10));
		}
		
		public void quit() {
			if (falling == null || !falling.isRunning()) return;
			
			falling.pause();
		}
		
		public void reJoin() {
			if (falling == null || !falling.isPaused()) return;

			armorStand.setGravity(true);
			armorStand.setVelocity(new Vector(0, -6, 0));

			new BukkitRunnable() {
				@Override
				public void run() {
					if (armorStand != null) {
						armorStand.setGravity(false);
						armorStand.teleport(armorStand.getLocation().clone().add(0, -0.6, 0));
					}
				}
			}.runTaskLater(AbilityWar.getPlugin(), 3L);

			SoundLib.BLOCK_ANVIL_LAND.playSound(armorStand.getLocation(), 1, 1.4f);

			target.setHealth(0);

			Bukkit.broadcastMessage("§c잔머리를 꾀한 자, 죽음을 §l절대§c 피하지 못하리라.");
			Bukkit.broadcastMessage("§3[§b다모클레스의 칼§3] §b" + nume + "§7/§b" + denominator + "§f의 확률로 §e" + (time / 20.0) + "§f초를 버티고 사망하셨습니다.");

			stop(false);
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			if (falling != null && falling.isRunning()) falling.stop(false);
			swords.remove(target.getUniqueId());
			if (armorStand != null) armorStand.remove();
			ac.unregister();
		}
		
		private class FallingTimer extends AbilityTimer {

			private IWorldBorder worldBorder;

			public FallingTimer() {
				super(TaskType.REVERSE, 60);
				setPeriod(TimeUnit.TICKS, 1);
			}

			@Override
			protected void onStart() {
				target.sendMessage("§c§o불길한 예감이 듭니다.");
				SoundLib.AMBIENT_CAVE.playSound(target, 1, 0.5f);
				worldBorder = NMS.createWorldBorder(target.getWorld().getWorldBorder());
				worldBorder.setWarningDistance(Integer.MAX_VALUE);
				NMS.setWorldBorder(target, worldBorder);
			}

			@Override
			protected void run(int count) {
				if (count == 5) {
					SoundLib.ENTITY_ITEM_BREAK.playSound(armorStand.getLocation(), 1, 2f);
					armorStand.setGravity(true);
					armorStand.setVelocity(new Vector(0, -2, 0));
				}
			}

			@Override
			protected void onEnd() {
				armorStand.setGravity(true);
				armorStand.setVelocity(new Vector(0, -6, 0));

				new BukkitRunnable() {
					@Override
					public void run() {
						if (armorStand != null && armorStand.isValid()) {
							armorStand.setGravity(false);
							armorStand.teleport(armorStand.getLocation().clone().add(0, -0.6, 0));
						}
					}
				}.runTaskLater(AbilityWar.getPlugin(), 3L);

				SoundLib.BLOCK_ANVIL_LAND.playSound(armorStand.getLocation(), 1, 1.4f);

				fallingDamage = true;
				target.damage(Integer.MAX_VALUE, getPlayer());
				target.getWorld().spawn(target.getLocation().clone().add(1000, 0, 1000), Zombie.class).damage(Integer.MAX_VALUE, getPlayer());

				NMS.resetWorldBorder(target);

				if (target.isDead()) {
	                Bukkit.broadcastMessage("§3[§b다모클레스의 칼§3] §b" + nume + "§7/§b" + denominator + "§f의 확률로 §e" + (time / 20.0) + "§f초를 버티고 사망하셨습니다.");
				}

				DamoclesSword.this.stop(false);
			}

			@Override
			protected void onSilentEnd() {
				NMS.resetWorldBorder(target);
			}
		}
		
	}

}
