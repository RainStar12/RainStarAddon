package RainStarAbility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "��ν�", rank = Rank.S, species = Species.GOD, explain = {
		"���� �� ���� ����� ��, ��ν�.",
		"��7�нú� ��8- ��c�ʸ��f: 5�� �̳��� �����ߴ� ����� ��� ���⿡ ó���� ��,",
		" ����� �� ��� ������ε� ������ ȸ���� �� �����ϴ�.",
		"��7ö�� ��Ŭ�� ��8- ��3����������f: �ٸ� �÷��̾ ö���� ��Ŭ���ϸ�",
		" ������ ��e������ü��f�� �˴ϴ�. �� �÷��̾�� �޴� ���ط��� ��c75%��f�� �ǰ�,",
		" ��� �ϳ��� ��c�����԰ų� ��dȸ�� ȿ����f�� ������ �ٸ� ����� ������ �޽��ϴ�.",
		" ���� �� �÷��̾�� ������ �ٲ� ������ 5���� �ֵ����� ���� ä ���� ���Ը���",
		" �� �� �ְ� �ǰ�, �ڽ��� �ֵ����� �Ѿ�� ���� �ֵ����� ������ ��밡 �˴ϴ�.",
		" ���� �ƴ� ��e������ü��f�� ��� �� ��� ȿ���� �����˴ϴ�. $[RIGHT_COOLDOWN]",
		"��7ö�� ��Ŭ�� ��8- ��3�������f: $[DURATION]�ʰ� �ڽ��� ����� ������ �߻�ü ��",
		" �ٸ� �ɷ��� Ÿ������ ��������ϴ�. $[LEFT_COOLDOWN]"
		},
		summarize = {
		"��7�ٸ� �÷��̾�� ö�� ��Ŭ����f�ϸ� ���� ��3��� ����ü��f�� �Ǿ�",
		"���ΰ� �޴� ���� ȿ�� �� ȸ�� ȿ��, �׸��� ������ ��ġ�� ��3������f�մϴ�.",
		"��7ö�� ��Ŭ����f���� $[DURATION]�ʰ� �߻�ü�� ��������� Ÿ���� �Ҵ� ���°� �˴ϴ�.",
		" $[LEFT_COOLDOWN]"
		})

public class Moros extends AbilityBase implements ActiveHandler, TargetHandler {

	public Moros(Participant participant) {
		super(participant);
	}
	
	private Map<Player, Mortal> mortal = new HashMap<>();
	private Set<Projectile> projectiles = new HashSet<>();
	private Player target;
	private final Cooldown leftcool = new Cooldown(LEFT_COOLDOWN.getValue(), "��Ŭ��");
	private final Cooldown rightcool = new Cooldown(RIGHT_COOLDOWN.getValue(), "��Ŭ��");
	private final ActionbarChannel ac = newActionbarChannel();
	private ActionbarChannel actionbarChannel;
	private static final RGB color1 = RGB.of(1, 204, 254);
	private static final RGB color2 = RGB.of(101, 224, 254);
	private static final RGB color3 = RGB.of(195, 243, 254);
	private static final RGB color4 = RGB.of(153, 102, 1);
	private Random random = new Random();
	private boolean leadership = false;
	
	private static final Points LAYER1 = Points.of(0.06, new boolean[][]{
		{false, false, false, false, false, false, false, true, true, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, true, true, false, false, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false},
		{false, false, false, true, false, false, false, false, false, false, false, false, false, false, false, false, true, false, false, false},
		{false, false, false, false, true, false, false, false, false, false, false, false, false, false, false, true, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, true, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
	});
	
	private static final Points LAYER2 = Points.of(0.06, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false},
		{false, false, false, true, true, false, false, false, true, true, true, true, true, true, true, true, true, false, false, false},
		{false, false, true, true, false, false, false, false, false, true, true, true, true, true, true, true, true, true, false, false},
		{false, false, true, true, false, false, false, false, false, true, true, true, true, true, true, true, true, true, false, false},
		{false, true, true, true, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, false},
		{false, true, true, true, true, false, false, false, true, true, true, true, true, true, true, true, true, true, true, false},
		{false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false},
		{false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false},
		{false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false},
		{false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false},
		{false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false},
		{false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false},
		{false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false},
		{false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false},
		{false, false, false, false, false, false, false, true, true, true, true, true, true, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
	});
	
	private static final Points LAYER3 = Points.of(0.06, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false}
	});
	
	private static final Points LAYER4 = Points.of(0.06, new boolean[][]{
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
		{false, false, false, false, false, true, true, false, false, false, false, false, false, true, true, false, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false},
		{false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false},
		{false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false},
		{false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false},
		{false, false, false, false, false, false, true, true, true, true, true, true, true, true, false, false, false, false, false, false},
	});
	
	public static final SettingObject<Integer> LEFT_COOLDOWN 
	= abilitySettings.new SettingObject<Integer>(Moros.class,
			"left-cooldown", 40, "# ��Ŭ�� ��Ÿ��") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> RIGHT_COOLDOWN 
	= abilitySettings.new SettingObject<Integer>(Moros.class,
			"right-cooldown", 80, "# ��Ŭ�� ��Ÿ��") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> DURATION 
	= abilitySettings.new SettingObject<Integer>(Moros.class,
			"duration", 5, "# ���� �ð�") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_SET || update == AbilityBase.Update.ABILITY_DESTROY) {
	    	if (target != null) {
	    		actionbarChannel.unregister();
	    	}
	    }
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	if (target != null) {
				actionbarChannel.update("��3������ü��f: ��e" + getPlayer().getName());	
	    	}
	    }
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
	
	private final AbilityTimer particle = new AbilityTimer(5) {
		
    	@Override
		public void run(int count) {
			final Location eyeLocation = getPlayer().getLocation().clone().add(getPlayer().getLocation().getDirection().add(new Vector(0, 0.5, 0)));
			final float yaw = getPlayer().getLocation().getYaw();
			for (Location loc : LAYER1.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color1);
			}
			for (Location loc : LAYER2.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color2);
			}
			for (Location loc : LAYER3.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color3);
			}
			for (Location loc : LAYER4.rotateAroundAxisY(-yaw).toLocations(eyeLocation)) {
				ParticleLib.REDSTONE.spawnParticle(loc, color4);
			}
			LAYER1.rotateAroundAxisY(yaw);
			LAYER2.rotateAroundAxisY(yaw);
			LAYER3.rotateAroundAxisY(yaw);
			LAYER4.rotateAroundAxisY(yaw);
    	}
		
	}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 4).register();
	
	private final Duration duration = new Duration(DURATION.getValue() * 20, leftcool) {
		
		@Override
		public void onDurationStart() {
			particle.start();
			SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), (float) 0.7, 2);
		}
		
		@Override
		public void onDurationProcess(int count) {
			for (Projectile projectile : LocationUtil.getNearbyEntities(Projectile.class, getPlayer().getLocation(), 5, 5, predicate)) {
				if (!getPlayer().equals(projectile.getShooter()) && !projectiles.contains(projectile)) {
					if (random.nextBoolean() == true) {
						projectile.setVelocity(VectorUtil.rotateAroundAxisY(projectile.getVelocity(), 60));	
					} else {
						projectile.setVelocity(VectorUtil.rotateAroundAxisY(projectile.getVelocity(), -60));
					}
					projectiles.add(projectile);
				}
			}
			getParticipant().attributes().TARGETABLE.setValue(false);
		}
		
		@Override
		public void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		public void onDurationSilentEnd() {
			getParticipant().attributes().TARGETABLE.setValue(true);
			projectiles.clear();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	private final AbilityTimer slotchanging = new AbilityTimer(100) {
		
    	@Override
		public void run(int count) {
    	}
		
    	@Override
    	public void onEnd() {
    		onSilentEnd();
    	}
    	
    	@Override
    	public void onSilentEnd() {
    		leadership = !leadership;
    	}
    	
	}.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onSlotChange(PlayerItemHeldEvent e) {
		if (target != null) {
			if (slotchanging.isRunning()) {
				if (e.getPlayer().equals(getPlayer()) && e.getNewSlot() != target.getInventory().getHeldItemSlot() && leadership) {
					target.getInventory().setHeldItemSlot(e.getNewSlot());
				}
				if (e.getPlayer().equals(target) && e.getNewSlot() != target.getInventory().getHeldItemSlot() && leadership) {
					e.setCancelled(true);
				}
				if (e.getPlayer().equals(getPlayer()) && e.getNewSlot() != getPlayer().getInventory().getHeldItemSlot() && !leadership) {
					e.setCancelled(true);
				}
				if (e.getPlayer().equals(target) && e.getNewSlot() != getPlayer().getInventory().getHeldItemSlot() && !leadership) {
					getPlayer().getInventory().setHeldItemSlot(e.getNewSlot());
				}
			} else {
				if (leadership && e.getPlayer().equals(getPlayer())) {
					slotchanging.start();
				}
				if (!leadership && e.getPlayer().equals(target)) {
					slotchanging.start();
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player) {
			if (e.getDamager().equals(getPlayer()) && !e.isCancelled() && !e.getEntity().equals(getPlayer())) {
				if (!mortal.containsKey(e.getEntity())) {
					mortal.put((Player) e.getEntity(), new Mortal((Player) e.getEntity()));
					mortal.get(e.getEntity()).start();
				} else mortal.get(e.getEntity()).addDamage();
			}
			if (NMS.isArrow(e.getDamager())) {
				Arrow arrow = (Arrow) e.getDamager();
				if (getPlayer().equals(arrow.getShooter()) && !e.isCancelled() && !e.getEntity().equals(getPlayer()) && e.getEntity() != null) {
					if (!mortal.containsKey(e.getEntity())) {
						mortal.put((Player) e.getEntity(), new Mortal((Player) e.getEntity()));
						mortal.get(e.getEntity()).start();
					} else mortal.get(e.getEntity()).addDamage();
				}
			}
			if (mortal.containsKey(e.getEntity())) {
				Player player = (Player) e.getEntity();
				if (player.getHealth() - e.getFinalDamage() <= 0) {
					e.setCancelled(true);
					player.setHealth(0);
				}
			}
			if (target != null && e.getDamager() != getPlayer() && e.getDamager() != target) {
				e.setDamage(e.getDamage() * 0.75);
				if (e.getEntity().equals(getPlayer())) {
					Damages.damageFixed(target, getPlayer(), (float) e.getFinalDamage());
				} else if (e.getEntity().equals(target)) {
					Damages.damageFixed(getPlayer(), target, (float) e.getFinalDamage());
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (target != null) {
			if (e.getEntity().equals(target)) {
				target = null;
				ac.update(null);
				actionbarChannel.unregister();
				rightcool.start();
			}
			if (e.getEntity().equals(getPlayer())) {
				actionbarChannel.unregister();
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (target != null) {
			if (e.getEntity().equals(getPlayer())) Healths.setHealth(target, target.getHealth() + e.getAmount());
			if (e.getEntity().equals(target)) Healths.setHealth(getPlayer(), getPlayer().getHealth() + e.getAmount());
		}
	}
	
	public void TargetSkill(Material material, LivingEntity entity) {
		if (material.equals(Material.IRON_INGOT) && entity instanceof Player && target == null && !rightcool.isCooldown()) {
			target = (Player) entity;
			ac.update("��3������ü��f: ��e" + target.getName());
			actionbarChannel = getGame().getParticipant(target).actionbar().newChannel();
			actionbarChannel.update("��3������ü��f: ��e" + getPlayer().getName());	
		}
	}
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK) && !leftcool.isCooldown() && !duration.isDuration()) {
	    	return duration.start();
	    }
		return false;
	}
	
    class Mortal extends AbilityTimer {
    	
    	Player player;
		ActionbarChannel actionbarChannel = newActionbarChannel();
    	
    	private Mortal(Player player) {
			super(TaskType.REVERSE, 5);
			setPeriod(TimeUnit.SECONDS, 1);
			this.player = player;
    	}
    	
		@Override
		protected void onStart() {
			actionbarChannel = getGame().getParticipant(player).actionbar().newChannel();
		}
		
		@Override
		protected void run(int count) {
			actionbarChannel.update("��4�ʸ��f: " + count + "��");
		}
		
		private void addDamage() {
			if (isRunning()) {
				setCount(5);
				actionbarChannel.update("��4�ʸ��f: " + getCount() + "��");
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			mortal.remove(player);
			if (actionbarChannel != null) {
				actionbarChannel.unregister();	
			}
		}
    	
    }
	
}
