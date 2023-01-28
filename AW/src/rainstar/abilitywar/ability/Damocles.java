package rainstar.abilitywar.ability;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.minecraft.nms.IWorldBorder;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "다모클레스", rank = Rank.L, species = Species.HUMAN, explain = {
		"모든 §c공격력§f이 §c$[DAMAGE_MULTIPLY]§f배가 됩니다.",
		"피격 후 매 틱마다 §b$[CHANCE_NUMERATOR]§7/§b$[CHANCE_DENOMINATOR]§f의 확률로 머리 위 §3검§f이 떨어집니다.",
		"또한 매번 피격 시 받은 최종 피해량의 $[MULTIPLY]배만큼§8(§7최소 1번§8)§f 확률을 추가로 시도합니다.",
		"§3검§f은 사용자에게 §4치명적인 피해§8(§721억 대미지§8)§f를 입힙니다."
		},
		summarize = {
		"기본 공격력이 §c$[DAMAGE_INCREASE]§f배가 되지만 피격 이후 매 순간 매우 낮은 확률로 즉사합니다."
		})

public class Damocles extends AbilityBase {

	public Damocles(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> DAMAGE_MULTIPLY = 
			abilitySettings.new SettingObject<Double>(Damocles.class, "damage-multiply", 1.4,
			"# 공격력 배수") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> MULTIPLY = 
			abilitySettings.new SettingObject<Double>(Damocles.class, "damage-multiply-for-chance", 3.0,
			"# 피해량의 배수만큼 확률 추가 시도") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> CHANCE_NUMERATOR = 
			abilitySettings.new SettingObject<Integer>(Damocles.class, "chance-numerator", 1,
			"# 확률 분자") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	public static final SettingObject<Integer> CHANCE_DENOMINATOR = 
			abilitySettings.new SettingObject<Integer>(Damocles.class, "chance-denominator", 10000,
			"# 확률 분모") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

	};
	
	private boolean fallingstarted = false;
	private boolean fallen = false;
	private boolean checked = false;
	private double damagemultiply = DAMAGE_MULTIPLY.getValue();
	private int nume = CHANCE_NUMERATOR.getValue(), deno = CHANCE_DENOMINATOR.getValue();
    private int time = 0;
    private final double multiply = MULTIPLY.getValue();
	private Random random = new Random();
	private ArmorStand armorstand = null;
	private ActionbarChannel ac = newActionbarChannel();
	
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (armorstand == null) armorstand = getPlayer().getLocation().getWorld().spawn(getPlayer().getLocation().clone().add(0, 3, 0), ArmorStand.class);
			if (checked) falling.start();
			armorstand.setRightArmPose(new EulerAngle(Math.toRadians(80), 0, 0));
			armorstand.setMetadata("Damocles", new FixedMetadataValue(AbilityWar.getPlugin(), null));
			armorstand.setVisible(false);
			armorstand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
			armorstand.setGravity(false);
			armorstand.setInvulnerable(true);
			NMS.removeBoundingBox(armorstand);
		}
		if (update == Update.ABILITY_DESTROY) {
			armorstand.remove();
		}
	}
	
	@SubscribeEvent
	private void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (e.getRightClicked().hasMetadata("Damocles")) e.setCancelled(true);
	}
	
	private AbilityTimer timer = new AbilityTimer() {
		
		@Override
		public void onStart() {
		}
		
		@Override
		public void run(int count) {
			int randomvalue = random.nextInt(deno);
			ac.update("§b" + randomvalue);
            if (!falling.isRunning()) time++;
			if (randomvalue < nume && !falling.isRunning()) {
				falling.start();
				this.stop(false);
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	private AbilityTimer falling = new AbilityTimer(TaskType.REVERSE, 60) {
		
		private IWorldBorder worldBorder;
		
		@Override
		public void onStart() {
			fallingstarted = true;
			getPlayer().sendMessage("§c§o불길한 예감이 듭니다.");
			SoundLib.AMBIENT_CAVE.playSound(getPlayer(), 1, 0.5f);
			worldBorder = NMS.createWorldBorder(getPlayer().getWorld().getWorldBorder());
			worldBorder.setWarningDistance(Integer.MAX_VALUE);
			NMS.setWorldBorder(getPlayer(), worldBorder);
		}
		
		@Override
		public void run(int count) {
			if (count == 5) {
				SoundLib.ENTITY_ITEM_BREAK.playSound(armorstand.getLocation(), 1, 2f);
				armorstand.setGravity(true);
				armorstand.setVelocity(new Vector(0, -2, 0));
			}
		}
		
		@Override
		public void onEnd() {
			armorstand.setGravity(true);
			armorstand.setVelocity(new Vector(0, -6, 0));
			new BukkitRunnable() {		
				@Override
				public void run() {
					armorstand.setGravity(false);
					armorstand.teleport(armorstand.getLocation().clone().add(0, -0.6, 0));
				}
			}.runTaskLater(AbilityWar.getPlugin(), 3L);
			SoundLib.BLOCK_ANVIL_LAND.playSound(armorstand.getLocation(), 1, 1.4f);
			getPlayer().damage(Integer.MAX_VALUE);
			getPlayer().getWorld().spawn(getPlayer().getLocation().clone().add(1000, 0, 1000), Zombie.class).damage(Integer.MAX_VALUE, getPlayer());
            if (getPlayer().isDead()) {
                Bukkit.broadcastMessage("§c운이 없는 자, 힘의 대가를 치르다.");
                Bukkit.broadcastMessage("§3[§b다모클레스§3] §b" + nume + "§7/§b" + deno + "§f의 확률로 §e" + (time / 20.0) + "§f초를 버티고 사망하셨습니다.");
            }
			fallen = true;
			fallingstarted = false;
			NMS.resetWorldBorder(getPlayer());
		}
		
		@Override
		public void onSilentEnd() {
			if (!fallen) checked = true;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	@SubscribeEvent(onlyRelevant = true)
	public void onPlayerMove(PlayerMoveEvent e) {
		if (!fallen) {
			Location loc = getPlayer().getLocation().clone().add(0, 3, 0);
			loc.add(getPlayer().getLocation().getDirection().clone().setY(0).normalize().multiply(0.7));
			loc.add(VectorUtil.rotateAroundAxisY(getPlayer().getLocation().getDirection().clone().setY(0).normalize().multiply(0.4), 90));
			armorstand.teleport(loc);
		}
	}
	
	@SubscribeEvent
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (fallingstarted) {
			armorstand.setGravity(true);
			armorstand.setVelocity(new Vector(0, -6, 0));
			new BukkitRunnable() {		
				@Override
				public void run() {
					armorstand.setGravity(false);
					armorstand.teleport(armorstand.getLocation().clone().add(0, -0.6, 0));
				}
			}.runTaskLater(AbilityWar.getPlugin(), 3L);
			SoundLib.BLOCK_ANVIL_LAND.playSound(armorstand.getLocation(), 1, 1.4f);
			getPlayer().damage(Integer.MAX_VALUE);
			getPlayer().setHealth(0);
			Bukkit.broadcastMessage("§c잔머리를 꾀한 자, 죽음을 §l절대§c 피하지 못하리라.");
            Bukkit.broadcastMessage("§3[§b다모클레스§3] §b" + nume + "§7/§b" + deno + "§f의 확률로 §e" + (time / 20.0) + "§f초를 버티고 사망하셨습니다.");
		}
	}
	
	@SubscribeEvent(priority = 1000)
	public void onDamaged(EntityDamageByEntityEvent e) {
		if (!e.isCancelled()) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (!fallen && e.getEntity().equals(getPlayer()) && !getPlayer().equals(damager) && !falling.isRunning()) {
				if (!timer.isRunning()) timer.start();
				if ((e.getFinalDamage() * multiply) < 1) {
					int randomvalue = random.nextInt(deno);
					ActionbarChannel actionbar = newActionbarChannel();
					actionbar.update("§b" + randomvalue);
		            if (!falling.isRunning()) time++;
					if (randomvalue < nume && !falling.isRunning()) {
						falling.start();
					}
					new BukkitRunnable() {					
						@Override
						public void run() {
							actionbar.unregister();
						}
					}.runTaskLater(AbilityWar.getPlugin(), 1L);
				} else {
					for (int a = 1; a <= (e.getFinalDamage() * multiply); a++) {
						int randomvalue = random.nextInt(deno);
						ActionbarChannel actionbar = newActionbarChannel();
						actionbar.update("§b" + randomvalue);
			            if (!falling.isRunning()) time++;
						if (randomvalue < nume && !falling.isRunning()) {
							falling.start();
						}
						new BukkitRunnable() {					
							@Override
							public void run() {
								actionbar.unregister();
							}
						}.runTaskLater(AbilityWar.getPlugin(), 1L);
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
		
		if (getPlayer().equals(damager)) e.setDamage(e.getDamage() * damagemultiply);
	}
	
}