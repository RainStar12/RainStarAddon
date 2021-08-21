package RainStarSynergy;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@SuppressWarnings("deprecation")
@AbilityManifest(name = "�ǰ� ���簡 �ȴٰ�", rank = Rank.L, species = Species.HUMAN, explain = {
		"��7�нú� ��8- ��eũ����Ż�� �����̴١�f: ��� ü���� �� ĭ �̻� ������ ���� �� ���", 
		" ü�·��� �Ѵ� ���ط��� ���´ٸ�, ���� ��� ü�¸��� �����˴ϴ�.",
		"��7�нú� ��8- ��c�纣���� \"��\"�̰��f: � �����δ� ȸ�� ȿ���� ���� ��",
		" �� ��� ü�·��� �� �ִ� ü���� ������ ��ȸ���� �ʴ´ٸ�",
		" ȸ�� ȿ����ŭ�� ��� ü���� �Ź� �߰� ȹ���մϴ�.",
		"��7�нú� ��8- ��aü���� ������ �ۼ� �����ϰ� ���ݾơ�f: � �����δ� ���ظ� ���� ������",
		" 1.3���� ���ظ� �Խ��ϴ�. �ڿ� ȸ�� �ӵ��� �ſ� �������ϴ�.",
		"��7ö�� ��Ŭ�� ��8- ��4�ǰ� ���簡 �ȴٰ��f: ��븦 �ٶ󺸰� �� �ɷ��� ����ϸ�",
		" ��󿡰Լ� ü���� �� ĭ �����մϴ�. $[COOLDOWN_CONFIG]",
		" ���� ü���� ���� ���� ��� �� ĭ �Ҹ��� ��󿡰� �߰� ���ظ� �����ϴ�."
		})

public class HealthCopy extends Synergy implements ActiveHandler {
	
	public HealthCopy(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN_CONFIG = synergySettings.new SettingObject<Integer>(HealthCopy.class, "cooldown", 6,
			"# ���� ��Ÿ��") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (!(entity instanceof Player)) return false;
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
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		if (!cancelled) {
    			final double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
    			if (getPlayer().getHealth() < maxHP) {
    				final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), 0.05, RegainReason.CUSTOM);
    				Bukkit.getPluginManager().callEvent(event);
    				if (!event.isCancelled()) {
    					getPlayer().setHealth(Math.min(getPlayer().getHealth() + 0.05, maxHP));
    				}
    			}	
    		}
    	}
    	
    }.setPeriod(TimeUnit.TICKS, 1).register();
    
	private final Cooldown cooldown = new Cooldown(COOLDOWN_CONFIG.getValue(), "����", 0);
	private boolean cancelled = false;
	
	private static final Note[] notes = new Note[] {
			Note.natural(0, Tone.B), Note.sharp(0, Tone.D), Note.natural(0, Tone.G),
			Note.sharp(0, Tone.G), Note.natural(0, Tone.B)
	};
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent e) {
		if (getPlayer().getHealth() - e.getFinalDamage() > 0) {
			new AbilityTimer(5) {
				
				@Override
				public void onStart() {
					cancelled = true;
				}
				
				@Override
				public void onEnd() {
					onSilentEnd();
				}
				
				@Override
				public void onSilentEnd() {
					cancelled = false;	
				}
				
			}.setPeriod(TimeUnit.TICKS, 1).start();
		}
		if (e.getEntity().equals(getPlayer()) && !getPlayer().isDead()) {
			e.setDamage(e.getDamage() * 1.3);
			if (getPlayer().getHealth() - e.getFinalDamage() > 0) {
				float yellowheart = NMS.getAbsorptionHearts(getPlayer());
	    		if (yellowheart >= 2) {
	    			float lostyellow = (float) e.getDamage(DamageModifier.ABSORPTION);
	        		if (yellowheart + lostyellow == 0) {
	            		if (e.getFinalDamage() > 0) {
	            			e.setDamage(0);
	                    	NMS.setAbsorptionHearts(getPlayer(), 0);
	                	}
	        		}
	    		}	
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (e.getEntity().equals(getPlayer()) && NMS.getAbsorptionHearts(getPlayer()) < (getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.5)) {
			if (!cancelled) NMS.setAbsorptionHearts(getPlayer(), (float) (NMS.getAbsorptionHearts(getPlayer()) + e.getAmount()));
		}
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
			if (cooldown.isCooldown()) return false;
			final Player target = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 20, predicate);
			if (target != null) {
				if (getPlayer().getHealth() == getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
					if (Damages.canDamage(target, getPlayer(), DamageCause.MAGIC, 3)) {
						Healths.setHealth(getPlayer(), getPlayer().getHealth() - 2);
						final Participant participant = getGame().getParticipant(target);
						new Transfusion(participant, true, 2).start();
						return cooldown.start();
					}
				} else {
					if (Damages.canDamage(target, getPlayer(), DamageCause.MAGIC, 1)) {
						final Participant participant = getGame().getParticipant(target);
						new Transfusion(participant, true, 1).start();
						return cooldown.start();
					}
				}
			}
		}
		return false;
	}
	
	private class Transfusion extends AbilityTimer {

		private final Player target;
		private final boolean damage;
		private final double amount;
		private final Location currentLocation;

		private Transfusion(final Participant target, final boolean damage, final double amount) {
			super();
			setPeriod(TimeUnit.TICKS, 1);
			this.target = target.getPlayer();
			this.damage = damage;
			this.amount = amount;
			this.currentLocation = this.target.getLocation().clone().add(0, 1, 0);
		}

		@Override
		protected void onStart() {
			if (!damage) return;
			Healths.setHealth(target, target.getHealth() - amount);
			NMS.broadcastEntityEffect(target, (byte) 2);
			SoundLib.ENTITY_PLAYER_HURT.playSound(target.getLocation());
		}

		@Override
		protected void run(int count) {
			final Location playerLocation = getPlayer().getLocation().clone().add(0, 1, 0);
			if (playerLocation.getWorld() != currentLocation.getWorld()) {
				stop(true);
				return;
			}
			final Vector direction = playerLocation.toVector().subtract(currentLocation.toVector()).normalize().multiply(.1);
			for (int i = 0; i < 5; i++) {
				currentLocation.setX(currentLocation.getX() + direction.getX());
				currentLocation.setY(currentLocation.getY() + direction.getY());
				currentLocation.setZ(currentLocation.getZ() + direction.getZ());
				ParticleLib.REDSTONE.spawnParticle(currentLocation, RGB.RED);
				if (playerLocation.distanceSquared(currentLocation) <= .04) {
					stop(false);
					return;
				}
			}
			if (count >= 300) {
				stop(true);
			}
		}

		@Override
		protected void onEnd() {
			if (getPlayer().isDead()) return;
			Healths.setHealth(getPlayer(), Math.max(1, getPlayer().getHealth() + amount));
			if (damage) {
				SoundLib.ENTITY_PLAYER_BREATH.playSound(getPlayer(), 100f, .7757f);
				SoundLib.ENTITY_PLAYER_BREATH.playSound(getPlayer(), 100f, 2f);
				for (Note note : notes) {
					SoundLib.PIANO.playInstrument(getPlayer(), note);
				}
			}
		}
	}

}
