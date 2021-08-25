package RainStarSynergy;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "����� �º�", rank = Rank.S, species = Species.HUMAN, explain = {
		"��3�Ĺ��f�� ���� ������ �� ���ط��� ��c$[DAMAGE_INCREASE]% ������f�ϰ� ��3�Ĺ��f���� �̵��մϴ�. ",
		"$[PERIOD]�ʸ��� ������ ��� ����� ��b��Ȯ�� �Ĺ��f���� �̵��մϴ�.",
		"�ٸ� ����ü�� �� ������ ���� ������ ���, ���ط��� ��c$[DAMAGE_DECREASE]% ���ҡ�f�մϴ�."
		})

public class CowardlyMatch extends Synergy {

	public CowardlyMatch(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> DAMAGE_INCREASE = synergySettings.new SettingObject<Integer>(CowardlyMatch.class,
			"damage-increase", 65, "# ����� ���� ����(����: %)") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> DAMAGE_DECREASE = synergySettings.new SettingObject<Integer>(CowardlyMatch.class,
			"damage-decrease", 25, "# ����� ���� ����(����: %)") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> PERIOD = synergySettings.new SettingObject<Integer>(CowardlyMatch.class,
			"period", 6, "# �Ĺ� �̵� ȿ�� �ֱ�", "# ��Ÿ�� ���Ұ� 50%���� ����˴ϴ�.") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	private boolean isFront(Entity owner, Entity target) {
	    Vector eye = owner.getLocation().getDirection().setY(0).normalize();
	    Vector toEntity = target.getLocation().getDirection().setY(0).normalize();
	    double dot = toEntity.dot(eye);
	    return dot <= -0.75D;
	}
	
	private boolean isBehind(Entity owner, Entity target) {
	    Vector eye = owner.getLocation().getDirection().setY(0).normalize();
	    Vector toEntity = target.getLocation().getDirection().setY(0).normalize();
	    double dot = toEntity.dot(eye);
	    return dot >= 0.75D;
	}
	
	private Location behindTeleport(Entity teleporter, Entity target) {
		final Location targetLoc = target.getLocation();
		final Vector targetDir = targetLoc.getDirection();
		Location backLoc = targetLoc.clone().add(targetDir.multiply(-.75));
		backLoc.setY(target.getLocation().getY());
		return backLoc;
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
    		ac.update("��b�� �Ĺ� �̵� ����");
		}
	}
	
	private ActionbarChannel ac = newActionbarChannel();
	private final double increase = 1 + (0.01 * DAMAGE_INCREASE.getValue());
	private final double decrease = 1 - (0.01 * DAMAGE_DECREASE.getValue());
	private final int period = (int) Math.ceil(Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(50) * PERIOD.getValue() : PERIOD.getValue());
	private boolean teleportable = true;
	
    private final AbilityTimer periodtimer = new AbilityTimer(period * 20) {
    	
    	@Override
		public void run(int count) {
    	}
    	
    	@Override
    	public void onEnd() {
    		teleportable = true;
    		ac.update("��b�� �Ĺ� �̵� ����");
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (getPlayer().equals(e.getDamager())) {
			if (isBehind(getPlayer(), e.getEntity())) {
				e.setDamage(e.getDamage() * increase);
				ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0.25, 1, 0.25, 25, 1, MaterialX.REDSTONE_BLOCK);
				SoundLib.ENTITY_EXPERIENCE_ORB_PICKUP.playSound(getPlayer());
				getPlayer().teleport(behindTeleport(getPlayer(), e.getEntity()));
			} else if (teleportable) {
				SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(getPlayer().getLocation(), 1, 1.5f);
				ParticleLib.DRAGON_BREATH.spawnParticle(getPlayer().getLocation(), 0.25, 0.25, 0.25, 50, 0);
				teleportable = false;
				ac.update(null);
				getPlayer().teleport(behindTeleport(getPlayer(), e.getEntity()));
				SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(getPlayer().getLocation(), 1, 1.5f);
				ParticleLib.DRAGON_BREATH.spawnParticle(getPlayer().getLocation(), 0.25, 0.25, 0.25, 50, 0);
				periodtimer.start();
			}
		}
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (getPlayer().equals(projectile.getShooter()) && teleportable) {
				SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(getPlayer().getLocation(), 1, 1.5f);
				ParticleLib.DRAGON_BREATH.spawnParticle(getPlayer().getLocation(), 0.25, 0.25, 0.25, 50, 0);
				teleportable = false;
				ac.update(null);
				getPlayer().teleport(behindTeleport(getPlayer(), e.getEntity()));
				SoundLib.ITEM_CHORUS_FRUIT_TELEPORT.playSound(getPlayer().getLocation(), 1, 1.5f);
				ParticleLib.DRAGON_BREATH.spawnParticle(getPlayer().getLocation(), 0.25, 0.25, 0.25, 50, 0);
				periodtimer.start();
			}
		}
		if (e.getDamager() instanceof Player && e.getEntity().equals(getPlayer())) {
			if (isFront(e.getDamager(), getPlayer())) {
				e.setDamage(e.getDamage() * decrease);
				SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation());
			}
		}
	}
	
}