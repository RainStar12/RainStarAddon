package RainStarSynergy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.manager.effect.event.ParticipantEffectApplyEvent;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import daybreak.google.common.collect.Iterables;

@AbilityManifest(
		name = "테슬라<플라즈마>", rank = Rank.S, species = Species.HUMAN, explain = {
		"고화력 플라즈마 건을 난사하는 §e전기 속성§f의 천재 과학자, 테슬라.",
		"§7패시브 §8- §e변류§f: 기절 효과를 받을 때 기절의 지속을 절반으로 줄입니다.",
		" 기절 중에는 플라즈마 탄환의 폭발 위력이 1.5배 강력해집니다.",
		"§7활 장전 §8- §d플라즈마 건§f: 플라즈마 건이 장전되기 전까진 활을 쏠 수 없습니다.",
		" 장전 후 발사시 바라보는 방향으로 매우 빠르고 일직선으로 나아가는",
		" 강력한 플라즈마 탄환이 날아가, 적중 위치에 큰 폭발을 일으키며",
		" 폭발에 휘말린 모든 플레이어를 1.5초간 기절시킵니다.",
		" 발사할 때 큰 반동이 따르고, 자신도 폭발 피해를 입을 수 있습니다.",
		" 재장전에 걸리는 시간은 10초이며, 쿨타임 감소 효과를 받습니다.",
		" 게이지가 차오르는 동안은 이동할 수 없습니다."
		})

public class TeslaPlasma extends Synergy {

	public TeslaPlasma(Participant participant) {
		super(participant);
	}
	
	private boolean charged = false;
	private int chargestack = 0;
	private Bullet bullet = null;
	private int timer = (int) (Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(50) * 20 : 20);
	private static final Circle CIRCLE = Circle.of(0.5, 15);
	private final ActionbarChannel ac = newActionbarChannel();
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			charging.start();
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onParticipantEffectApply(ParticipantEffectApplyEvent e) {
		if (e.getEffectType().equals(Stun.registration)) {
			e.setDuration(TimeUnit.TICKS, (int) (e.getDuration() * 0.5));
		}
	}
	
	@SubscribeEvent
	public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getPlayer().equals(getPlayer()) && chargestack >= 7 && chargestack < 10) {
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(EntityShootBowEvent e) {
		if (getPlayer().equals(e.getEntity()) && NMS.isArrow(e.getProjectile())) {
			if (charged) {
				SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 7, 1.75f);
				SoundLib.ENTITY_WITHER_HURT.playSound(getPlayer().getLocation(), 7, 1.4f);
				Arrow arrow = (Arrow) e.getProjectile();
				new Bullet(getPlayer(), arrow.getVelocity(), arrow.getLocation()).start();
				getPlayer().setVelocity(getPlayer().getLocation().getDirection().setY((getPlayer().getLocation().getDirection().getY() * 0.3)).multiply(-2));
				charged = false;
				charging.stop(false);
				chargestack = 0;
			}
			e.setCancelled(true);
		}
	}
	
	private final AbilityTimer charging = new AbilityTimer() {
		
		@Override
		public void run(int count) {
    		if (timer != 0) {
        		if (count % timer == 0 && chargestack < 10) {
    				chargestack++;
    				if (chargestack == 10) {
    					ac.update(Strings.repeat("§b§l⚡", chargestack - 7).concat(Strings.repeat("§7§l⚡", 3 - (chargestack - 7))));
    				} else if (chargestack >= 8) {
    					ac.update(Strings.repeat("§e§l⚡", chargestack - 7).concat(Strings.repeat("§7§l⚡", 3 - (chargestack - 7))));	
    				}
    				switch(chargestack) {
    				case 8:
    				case 9: SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer(), 1, 1.05f);
    						break;
    				case 10: SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer(), 1, 1.05f);
        					 charged = true;
    						 break;
    				}
        		}	
    		} else {
        		if (chargestack < 10) {
    				chargestack++;
    				if (chargestack == 10) {
    					ac.update(Strings.repeat("§b§l⚡", chargestack - 7).concat(Strings.repeat("§7§l⚡", 3 - (chargestack - 7))));
    				} else if (chargestack >= 8) {
    					ac.update(Strings.repeat("§e§l⚡", chargestack - 7).concat(Strings.repeat("§7§l⚡", 3 - (chargestack - 7))));	
    				}
    				switch(chargestack) {
    				case 8:
    				case 9: SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer(), 1, 1.05f);
    						break;
    				case 10: SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer(), 1, 1.05f);
        					 charged = true;
    						 break;
    				}
        		}
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
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public class Bullet extends AbilityTimer {
		
		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Predicate<Entity> predicate;
		private final Iterator<Vector> twist1;
		private final Iterator<Vector> twist2;
		private Vector forward;
		private int stacks = 0;
		private boolean turns = true;

		private RGB shootgradationA;
		private RGB shootgradationB;
		
		private final RGB shootgradation1 = RGB.of(154, 87, 205), shootgradation2 = RGB.of(155, 104, 198), shootgradation3 = RGB.of(156, 120, 191),
				shootgradation4 = RGB.of(157, 138, 183), shootgradation5 = RGB.of(158, 155, 176), shootgradation6 = RGB.of(159, 172, 169),
				shootgradation7 = RGB.of(160, 189, 162), shootgradation8 = RGB.of(160, 205, 155), shootgradation9 = RGB.of(161, 222, 147),
				shootgradation10 = RGB.of(161, 238, 140), shootgradation11 = RGB.of(162, 254, 133);
		
		private List<RGB> shootgradations1 = new ArrayList<RGB>(){{
			add(shootgradation1);
			add(shootgradation2);
			add(shootgradation3);
			add(shootgradation4);
			add(shootgradation5);
			add(shootgradation6);
			add(shootgradation7);
			add(shootgradation8);
			add(shootgradation9);
			add(shootgradation10);
			add(shootgradation11);
		}};
		
		private List<RGB> shootgradations2 = new ArrayList<RGB>(){{
			add(shootgradation11);
			add(shootgradation10);
			add(shootgradation9);
			add(shootgradation8);
			add(shootgradation7);
			add(shootgradation6);
			add(shootgradation5);
			add(shootgradation4);
			add(shootgradation3);
			add(shootgradation2);
			add(shootgradation1);
		}};
		
		private Location lastLocation;
		
		private Bullet(LivingEntity shooter, Vector arrowVelocity, Location startLocation) {
			super(20);
			setPeriod(TimeUnit.TICKS, 1);
			TeslaPlasma.this.bullet = this;
			this.shooter = shooter;
			this.entity = new Bullet.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.twist1 = Iterables.cycle(CIRCLE.clone().rotateAroundAxisY(-shooter.getLocation().getYaw()).rotateAroundAxis(VectorUtil.rotateAroundAxisY(shooter.getLocation().getDirection().setY(0).normalize(), 90), shooter.getLocation().getPitch() + 90)).iterator();
			this.twist2 = Iterables.cycle(CIRCLE.clone().rotateAroundAxisY(-shooter.getLocation().getYaw()).rotateAroundAxis(VectorUtil.rotateAroundAxisY(shooter.getLocation().getDirection().setY(0).normalize(), 90), -(shooter.getLocation().getPitch() + 90))).iterator();
			this.forward = arrowVelocity.multiply(7);
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity.equals(shooter)) return false;
					if (entity instanceof ArmorStand) return false;
					if (entity instanceof Player) {
						if (!getGame().isParticipating(entity.getUniqueId())
								|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
								|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
							return false;
						}
						if (getGame() instanceof Teamable) {
							final Teamable teamGame = (Teamable) getGame();
							final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()), participant = teamGame.getParticipant(shooter.getUniqueId());
							if (participant != null) {
								return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant) || (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
						}
					}
					return true;
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
		}
		
		@Override
		protected void run(int i) {
			Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()), unit = vectorBetween.clone().normalize().multiply(.1);
				private final int amount = (int) (vectorBetween.length() / 0.1);
				private int cursor = 0;

				@Override
				public boolean hasNext() {
					return cursor < amount;
				}

				@Override
				public Location next() {
					if (cursor >= amount) throw new NoSuchElementException();
					cursor++;
					return lastLocation.clone().add(unit.clone().multiply(cursor));
				}
			}; iterator.hasNext(); ) {
				final Location location = iterator.next();
				entity.setLocation(location);
				final Block block = location.getBlock();
				final Material type = block.getType();
				if (type.isSolid()) {
					ParticleLib.EXPLOSION_HUGE.spawnParticle(entity.getLocation());
					if (getGame().getParticipant((Player) shooter).hasEffect(Stun.registration)) {
						shooter.getWorld().createExplosion(entity.getLocation(), 4.5f, false, true);
					} else {
						shooter.getWorld().createExplosion(entity.getLocation(), 3f, false, true);	
					}
					for (Player players : LocationUtil.getEntitiesInCircle(Player.class, entity.getLocation(), 6.5, predicate)) {
						Stun.apply(getGame().getParticipant(players), TimeUnit.TICKS, 30);
						players.getWorld().strikeLightningEffect(players.getLocation());
					}
					stop(false);
					return;
				}
				for (Player player : LocationUtil.getConflictingEntities(Player.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(player)) {
						ParticleLib.EXPLOSION_HUGE.spawnParticle(entity.getLocation());
						if (getGame().getParticipant((Player) shooter).hasEffect(Stun.registration)) {
							shooter.getWorld().createExplosion(entity.getLocation(), 4.5f, false, true);
						} else {
							shooter.getWorld().createExplosion(entity.getLocation(), 3f, false, true);	
						}
						for (Player players : LocationUtil.getEntitiesInCircle(Player.class, entity.getLocation(), 6.5, predicate)) {
							Stun.apply(getGame().getParticipant(players), TimeUnit.TICKS, 30);
							players.getWorld().strikeLightningEffect(players.getLocation());
						}
						stop(false);
						return;
					}
				}	
				if (i % 4 == 0) {
					if (turns) stacks++;
					else stacks--;
					if (stacks % (shootgradations1.size() - 1) == 0) {
						turns = !turns;
					}
					shootgradationA = shootgradations1.get(stacks);
					shootgradationB = shootgradations2.get(stacks);
				}
				if (i % 2 == 0) {
					ParticleLib.END_ROD.spawnParticle(location, 0, 0, 0, 1, 0.05);
					ParticleLib.REDSTONE.spawnParticle(location.add(twist1.next()), shootgradationA);
					ParticleLib.REDSTONE.spawnParticle(location.add(twist2.next()), shootgradationB);
				}
			}
			lastLocation = newLocation;
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
			TeslaPlasma.this.bullet = null;
			charging.start();
		}

		public class ArrowEntity extends CustomEntity implements Deflectable {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}
			
			@Override
			public Vector getDirection() {
				return forward.clone();
			}

			@Override
			public void onDeflect(Participant deflector, Vector newDirection) {
				stop(false);
				Player deflectedPlayer = deflector.getPlayer();
				new Bullet(deflectedPlayer, newDirection, lastLocation).start();
			}
			
			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
				Bullet.this.stop(false);
			}

		}
		
	}
	
}
