package RainStarAbility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
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
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;

@AbilityManifest(name = "다모클레스", rank = Rank.L, species = Species.HUMAN, explain = {
		"모든 §c공격력§f이 §c$[DAMAGE_INCREASE]§f배가 됩니다.",
		"피격 후 매 틱마다 §b$[CHANCE_NUMERATOR]§7/§b$[CHANCE_DENOMINATOR]§f의 확률로 머리 위 §3검§f이 떨어집니다.",
		"§3검§f은 치명적 피해를 입히고 이 능력을 잃게 만듭니다."
		},
		summarize = {
		"기본 공격력이 §c$[DAMAGE_INCREASE]§f배가 되지만 피격 이후 매 순간 매우 낮은 확률로 즉사합니다."
		})

public class Damocles extends AbilityBase {

	public Damocles(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> DAMAGE_INCREASE = 
			abilitySettings.new SettingObject<Double>(Damocles.class, "damage-increase", 1.5,
			"# 공격력 배수") {

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
	
	private boolean fallen = false;
	private boolean checked = false;
	private double damagemultiply = DAMAGE_INCREASE.getValue();
	private int nume = CHANCE_NUMERATOR.getValue(), deno = CHANCE_DENOMINATOR.getValue();
    private int time = 0;
	private Random random = new Random();
	private ArmorStand armorstand = getPlayer().getLocation().getWorld().spawn(getPlayer().getLocation().clone().add(0, 3, 0), ArmorStand.class);
	private ActionbarChannel ac = newActionbarChannel();
	
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
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
		
		@Override
		public void onStart() {
			getPlayer().sendMessage("§c§o불길한 예감이 듭니다.");
			SoundLib.AMBIENT_CAVE.playSound(getPlayer(), 1, 0.5f);
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
            if (getPlayer().isDead()) {
                Bukkit.broadcastMessage("§c운이 없는 자, 힘의 대가를 치르다.");
                Bukkit.broadcastMessage("§3[§b다모클레스§3] §b" + nume + "§7/§b" + deno + "§f의 확률로 §e" + (time / 20.0) + "§f초를 버티고 사망하셨습니다.");
            }
			fallen = true;
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
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (!fallen) {
			Player damager = null;
			if (e.getDamager() instanceof Projectile) {
				Projectile projectile = (Projectile) e.getDamager();
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
			} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
			
			if (getPlayer().equals(damager)) e.setDamage(e.getDamage() * damagemultiply);
			
			if (e.getEntity().equals(getPlayer()) && !getPlayer().equals(damager) && !timer.isRunning() && !falling.isRunning()) timer.start();
		}
	}
	
}