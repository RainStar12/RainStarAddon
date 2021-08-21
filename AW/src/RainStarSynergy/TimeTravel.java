package RainStarSynergy;

import java.util.Collection;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import RainStarEffect.TimeDistortion;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.FastMath;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;

@AbilityManifest(name = "�ð� ����", rank = Rank.L, species = Species.HUMAN, explain = {
		"��7ö�� ��Ŭ�� ��8- ��d�ð� �����f:  ���� ���� ��� ���¸� �����մϴ�. $[RIGHT_COOLDOWN]",
		" ���� ���� �ߵ� �� ��� ��, ����� �ð����� �����մϴ�.",
		" �� �������δ� �ִ� ü���� ���� �̻��� ü���� ȸ���� �� �����ϴ�.",
		"��7��� ��8- ��a���� �����f: ġ������ ���ظ� �Ծ��� �� �� �� �� ������� �ʰ�",
		" ����� �ð����� �ڽ��� ���¸� �ǵ����ϴ�. ���� �ٽô� �ð��� ������ �� ������",
		" ��� ������ �ð����� �ð��� ���� ����˴ϴ�.",
		"��7ö�� ��Ŭ�� ��8- ��b�̷� �����f: $[DURATION]�ʰ� ��c���� ��f/ ��d���� �Ҵ� ��f�� ��3Ÿ���� �Ұ� ��f���°� �Ǹ�",
		" �ߵ��� �ٽ� ��Ŭ���Ͽ� ��� ��b�̷� �����f�� �׸��� �� �ֽ��ϴ�. $[LEFT_COOLDOWN]",
		" ��b�̷� �����f���� ���� ��, �ֺ� $[RANGE]ĭ �� �÷��̾��� �ð��� ��7�ְ��f�Ͽ�",
		" $[EFFECT]�ʰ� �̵� �ӵ��� ���� �ӵ��� ������ �ϰ�, �ڽ��� �������ϴ�.",
		" ���� �������� �޴� ù ���ظ� �����մϴ�. �� ȿ���� ��ø���� �ʽ��ϴ�.",
		"��2[��a!��2] ��b_Daybreak_��f���� �ð� ��ƼŬ ���ۿ� �����ּ̽��ϴ�."
})

public class TimeTravel extends Synergy implements ActiveHandler {
	
	public TimeTravel(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> RIGHT_COOLDOWN = synergySettings.new SettingObject<Integer>(TimeTravel.class,
			"right-cooldown", 45, "# ���� ��Ÿ��",
			"# ��Ÿ�� ���� ȿ���� 50%������ �޽��ϴ�.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};	
	
	public static final SettingObject<Integer> LEFT_COOLDOWN = synergySettings.new SettingObject<Integer>(TimeTravel.class,
			"left-cooldown", 45, "# �̷� ��Ÿ��",
			"# ��Ÿ�� ���� ȿ���� 50%������ �޽��ϴ�.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};	
	
	public static final SettingObject<Integer> DURATION = synergySettings.new SettingObject<Integer>(TimeTravel.class,
			"duration", 12, "# �̷� ���� ���� �ð�") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> EFFECT = synergySettings.new SettingObject<Integer>(TimeTravel.class,
			"effect-duration", 7, "# ȿ�� ���� �ð�") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> RANGE = synergySettings.new SettingObject<Integer>(TimeTravel.class,
			"range", 7, "# ȿ�� ����", "��2[��c!��2] ��7����! ��ƼŬ�� ������� �ʽ��ϴ�.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final Cooldown PastTravel = new Cooldown(RIGHT_COOLDOWN.getValue(), "����", 50);
	private final Cooldown FutureTravel = new Cooldown(LEFT_COOLDOWN.getValue(), "�̷�", 50);
	private final int effect = EFFECT.getValue();
	private final int range = RANGE.getValue();
	private final ActionbarChannel ac = newActionbarChannel();
	private static final Circle circle = Circle.of(7, 100);
	private static final RGB color = RGB.of(36, 252, 254);
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	private boolean checkdeath = true;
	private boolean timeshield = false; 
	
	private Location saveloc = null;
	private Collection<PotionEffect> savepotion;
	private int savefiretick = 0;
	private float savefall = 0;
	private double savehp = 0;
	private ItemStack[] saveinv;
	private float flyspeed = 0;
	private GameMode orgGM = null;
	
	private Location saveloc2 = null;
	private Collection<PotionEffect> savepotion2;
	private int savefiretick2 = 0;
	private float savefall2 = 0;
	private double savehp2 = 0;
	
	public void pastTraveling() {
		if (checkdeath) {
			getPlayer().setHealth(savehp);	
			getPlayer().setFireTicks(savefiretick);
			getPlayer().setFallDistance(savefall);
			for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
				getPlayer().removePotionEffect(effect.getType());
			}
			getPlayer().addPotionEffects(savepotion);
		} else {
			getPlayer().setHealth(Math.min(savehp2, (getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2)));
			getPlayer().setFireTicks(savefiretick2);
			getPlayer().setFallDistance(savefall2);
			for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
				getPlayer().removePotionEffect(effect.getType());
			}
			getPlayer().addPotionEffects(savepotion2);
		}
		
		new BukkitRunnable() {
			
			@Override
			public void run() {
				if (checkdeath) {
					getPlayer().teleport(saveloc);
					getPlayer().getInventory().setContents(saveinv);
					SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
					ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0, 1, 0, 100, 0.3, MaterialX.CLOCK);
				} else {
					getPlayer().teleport(saveloc2);
					SoundLib.BELL.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					SoundLib.BELL.playInstrument(getPlayer(), Note.sharp(0, Tone.F));
					SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Tone.A));
					final Firework firework = getPlayer().getWorld().spawn(getPlayer().getEyeLocation(), Firework.class);
					final FireworkMeta meta = firework.getFireworkMeta();
					meta.addEffect(
							FireworkEffect.builder()
									.withColor(Color.fromRGB(32, 60, 255), Color.WHITE, Color.fromRGB(250, 213, 0))
									.with(Type.BALL)
									.build()
					);
					meta.setPower(0);
					firework.setFireworkMeta(meta);
					firework.setMetadata("TimeRewind", NULL_VALUE);
					new BukkitRunnable() {
						@Override
						public void run() {
							firework.detonate();
						}
					}.runTaskLater(AbilityWar.getPlugin(), 1L);
				}
				checkdeath = false;
			}
			
		}.runTaskLater(AbilityWar.getPlugin(), 1L);
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT)) {
	    	if (clicktype.equals(ClickType.RIGHT_CLICK)) {
	    		if (PastTravel.isCooldown()) return false;
	    		if (checkdeath) {
		    		savehp = getPlayer().getHealth();
		    		saveloc = getPlayer().getLocation();
		    		savefiretick = getPlayer().getFireTicks();
		    		savefall = getPlayer().getFallDistance();
		    		savepotion = getPlayer().getActivePotionEffects();
		    		saveinv = getPlayer().getInventory().getContents();
		    		getPlayer().sendMessage("��3[��b!��3] ��f�ð��� ��a�����f�Ͽ����ϴ�.");	
	    		} else {
	    			pastTraveling();
	    			getPlayer().sendMessage("��3[��b!��3] ��f�ð��� ��e�����f�մϴ�.");
	    		}
	    		return PastTravel.start();
	    	} else if (clicktype.equals(ClickType.LEFT_CLICK)) {
	    		if (FutureTravel.isCooldown()) return false;
	    		if (!traveling.isDuration()) {
	    			traveling.start();
	    		} else if (traveling.isDuration()) {
	    			traveling.stop(false);
	    		}
	    		return true;
	    	}
	    }
	    return false;
	}
	
	@SubscribeEvent
	private void onPlayerTeleport(PlayerTeleportEvent e) {
		if (traveling.isRunning() && getPlayer().equals(e.getPlayer())) {
			if (e.getCause() == TeleportCause.SPECTATE) e.setCancelled(true);
		}
	}
	
	@SubscribeEvent(priority = 6)
	private void onEntityDamage(EntityDamageEvent e) {
		if (timeshield && !e.isCancelled() && e.getEntity().equals(getPlayer())) {
			getPlayer().sendMessage("��3[��b!��3] ��c���� ���ء�f�� ��3�ð��� ��ȣ����f�� ��ȣ���־����ϴ�.");
			SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
			e.setCancelled(true);
			timeshield = false;
		}
		if (checkdeath && e.getEntity().equals(getPlayer())) {
			if (getPlayer().getHealth() - e.getFinalDamage() <= 0 && !e.isCancelled()) {
				ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0, 1, 0, 50, 0.3, MaterialX.CLOCK);
				SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
			    savehp2 = getPlayer().getHealth();
			    saveloc2 = getPlayer().getLocation();
			    savefiretick2 = getPlayer().getFireTicks();
			    savefall2 = getPlayer().getFallDistance();
			    savepotion2 = getPlayer().getActivePotionEffects();
			    getPlayer().sendMessage("��3[��b!��3] ��f�ð��� ��c���� ��a�����f�Ǿ����ϴ�.");	
				pastTraveling();
				e.setCancelled(true);
			}
		}
	}
	
	@SubscribeEvent(priority = 6)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().hasMetadata("TimeRewind")) {
			e.setCancelled(true);
		}
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = 6)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
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
	
	private final Duration traveling = new Duration(DURATION.getValue() * 20, FutureTravel) {
		
		@Override
		protected void onDurationStart() {
			ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0, MaterialX.WHITE_STAINED_GLASS_PANE);
			ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0, MaterialX.GRAY_STAINED_GLASS_PANE);
			ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0, MaterialX.LIGHT_GRAY_STAINED_GLASS_PANE);
			ParticleLib.ITEM_CRACK.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0, MaterialX.BLACK_STAINED_GLASS_PANE);
			flyspeed = getPlayer().getFlySpeed();
			orgGM = getPlayer().getGameMode();
		}
		
		@Override
		protected void onDurationProcess(int arg0) {
			getPlayer().setGameMode(GameMode.SPECTATOR);
			getParticipant().attributes().TARGETABLE.setValue(false);
			ac.update("��b�̷� ���� ��");
			getPlayer().setFlySpeed(0.15f);
		}
		
		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		protected void onDurationSilentEnd() {
			getPlayer().setGameMode(orgGM);
			SoundLib.ITEM_TOTEM_USE.playSound(getPlayer(), 1, 1.7f);
			PotionEffects.FAST_DIGGING.addPotionEffect(getPlayer(), effect * 20, 2, true);
			PotionEffects.SPEED.addPotionEffect(getPlayer(), effect * 20, 2, true);
			getParticipant().attributes().TARGETABLE.setValue(true);
			clockeffect.start();
			getPlayer().setFlySpeed(flyspeed);
			timeshield = true;
			
			for (Player p : LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), range, predicate)) {
				SoundLib.ENTITY_EVOKER_FANGS_ATTACK.playSound(p, 1, 0.7f);
				TimeDistortion.apply(getGame().getParticipant(p), TimeUnit.SECONDS, effect);
			}
			
			new AbilityTimer(20) {
				@Override
				protected void run(int count) {
					Location center = getPlayer().getLocation().clone().add(0, 2 - count * 0.1, 0);
					for (Location loc : circle.toLocations(center).floor(center.getY())) {
						ParticleLib.REDSTONE.spawnParticle(loc, color);
					}
				}
			}.setPeriod(TimeUnit.TICKS, 1).start();
			
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	private static final EulerAngle DEFAULT_EULER_ANGLE = new EulerAngle(Math.toRadians(270), 0, 0);
	private static final ItemStack CLOCK = MaterialX.CLOCK.createItem();

	private final AbilityTimer clockeffect = new AbilityTimer(TaskType.NORMAL, 100) {
		private ArmorStand[] armorStands;

		@Override
		protected void onStart() {
			this.armorStands = new ArmorStand[] {
					getPlayer().getWorld().spawn(getPlayer().getLocation(), ArmorStand.class),
					getPlayer().getWorld().spawn(getPlayer().getLocation(), ArmorStand.class)
			};
			for (ArmorStand armorStand : armorStands) {
				armorStand.setVisible(false);
				armorStand.setInvulnerable(true);
				armorStand.setGravity(false);
				armorStand.setRightArmPose(DEFAULT_EULER_ANGLE);
				armorStand.getEquipment().setItemInMainHand(CLOCK);
				NMS.removeBoundingBox(armorStand);
			}
		}

		@Override
		protected void run(int count) {
			for (int i = 0; i < 5; i++) {
				final int index = (count - 1) * 5 + i;
				final double t = index * 0.0155;
				armorStands[0].teleport(adjustLocation(getPlayer().getLocation().clone().add(FastMath.cos(t) * 0.8, count * 0.0155, FastMath.sin(t) * 0.8)));
				armorStands[1].teleport(adjustLocation(getPlayer().getLocation().clone().add(-FastMath.cos(t) * 0.8, count * 0.0155, -FastMath.sin(t) * 0.8)));
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			for (ArmorStand armorStand : armorStands) {
				armorStand.remove();
			}
			this.armorStands = null;
		}
	}.setPeriod(TimeUnit.TICKS, 1);

	private static Location adjustLocation(final Location location) {
		final Vector direction = location.getDirection().setY(0).normalize();
		return location.clone().subtract(0, 1, 0).subtract(direction.clone().multiply(0.75)).add(VectorUtil.rotateAroundAxisY(direction.clone(), 90).multiply(0.4));
	}
	
}
