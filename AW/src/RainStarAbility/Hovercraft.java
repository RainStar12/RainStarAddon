package RainStarAbility;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.ClickType;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "호버크래프트", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7검 들고 F §8- §5호버링§f: 호버링을 시작하여 바라보는 방향으로",
		" 상승과 하강 없이 수평으로 계속해 나아갑니다. 연료를 전부 사용하거나",
		" 다시 검 들고 F를 사용할 경우 호버링 모드를 종료합니다.",
		" 호버링 모드 중에는 받는 피해가 25% 감소하며, 벡터 영향을 받지 않습니다.",
		" 또한 매 2초마다 아래에 가스를 분사해 닿은 적에게 중독 피해를 입힙니다.",
		" 이때 §a검을 우클릭§f하여 속력을 0~3단계로 조절 가능하고,",
		" §a철괴를 우클릭§f하여 자동 모드를 켜 가장 가까운 대상에게 자동 유도될 지",
		" 정할 수 있습니다.",
		"§7철괴 좌클릭 §8- §3상승 기류§f: 몸을 급격하게 높이 띄웁니다. $[COOLDOWN]",
		"§7패시브 §8- §b안전장치§f: 낙하 피해, 압사 피해를 무시합니다."})

@Materials(materials = {
		Material.WOOD_SWORD,
		Material.STONE_SWORD,
		Material.IRON_SWORD,
		Material.GOLD_SWORD,
		Material.DIAMOND_SWORD
	})

public class Hovercraft extends AbilityBase implements ActiveHandler {
	
	public Hovercraft(Participant participant) {
		super(participant);
	}
	
	private static final Set<Material> swords;
	private Hovering bullet = null;
	private double speed = 0;
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT) {
			
		}
		if (swords.contains(material)) {
			if (clicktype == ClickType.RIGHT_CLICK) {
				if (speed == 0.1) speed = 0.5;
				else if (speed == 0.5) speed = 1.0;
				else if (speed == 1.0) speed = 2.0;
				else if (speed == 2.0) speed = 0.1;
			}
		}
		return false;
	}

    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
    		if (bullet == null) {
        		new Hovering(getPlayer(), getPlayer().getLocation(), getPlayer().getLocation().getDirection().clone().setY(0).normalize()).start();
        		e.setCancelled(true);	
    		}
    	}
    }
    
    public class Hovering extends AbilityTimer implements Listener {
    	
    	private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Vector forward;
		private final Predicate<Entity> predicate;

		private Location lastLocation;
		
		private Hovering(LivingEntity shooter, Location startLocation, Vector arrowVelocity) {
			super(20);
			setPeriod(TimeUnit.TICKS, 1);
			Hovercraft.this.bullet = this;
			this.shooter = shooter;
			this.entity = new Hovering.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.forward = arrowVelocity.multiply(speed);
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity instanceof ArmorStand) return false;
					if (entity.equals(shooter)) return false;
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
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@EventHandler()
	    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
	    	if (swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
	    		this.stop(false);
	    		e.setCancelled(true);
	    	}
		}
		
		@Override
		protected void run(int i) {
			final Location newLocation = lastLocation.clone().add(forward);
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
					SoundLib.ENTITY_ARROW_HIT_PLAYER.playSound(getPlayer());
					stop(false);
					return;
				}
				for (LivingEntity livingEntity : LocationUtil.getConflictingEntities(LivingEntity.class, entity.getWorld(), entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(livingEntity)) {
						Damages.damageArrow(livingEntity, shooter, 5);
					}
				}
				shooter.teleport(location);
			}
			lastLocation = newLocation;
		}
		
		@Override
		protected void onEnd() {
			entity.remove();
			Hovercraft.this.bullet = null;
		}

		@Override
		protected void onSilentEnd() {
			entity.remove();
			Hovercraft.this.bullet = null;
		}

		public class ArrowEntity extends CustomEntity {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			protected void onRemove() {
			}

		}
    	
    }
	
}
