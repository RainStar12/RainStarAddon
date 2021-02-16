package RainStarAbility;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.util.BlockIterator;

import RainStarEffect.Confusion;
import RainStarSynergy.BadManner;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.ability.AbilityBase.Cooldown;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(
		name = "쿠로", rank = Rank.A, species = Species.HUMAN, explain = {
		"마왕을 행세하는 §8어둠§f 속성의 중2병 검사, 쿠로.",
		"§7패시브 §8- §8어둠의 추종자§f: 자신이 있는 위치가 어두울수록 스킬 피해량이 증가합니다.",
		"§7근접 타격 후 F §8- §5차원 절단§f: 바라보는 방향으로 빠르게 질주합니다.",
		" 질주하며 지나간 공간을 절단시켜 주변 엔티티들을 끌어와 피해를 입히고",
		" 잠시간 차원의 저편으로 보내버립니다. $[COOLDOWN]",
		"§7패시브 §8- §c마안 개방§f: 치명적인 피해를 입었을 때, 체력을 최대 체력의 절반까지",
		" 즉시 회복하고 마안을 개방합니다."
		})

public class Kuro extends AbilityBase {

	public Kuro(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN 
	= abilitySettings.new SettingObject<Integer>(Kuro.class,
			"cooldown", 40, "# 차원 절단 쿨타임",
			"# 쿨타임 감소 효과를 최대 50%까지 받습니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue(), "차원 절단", CooldownDecrease._50);
		
	private static final Set<Material> swords;
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	private final AbilityTimer attacked = new AbilityTimer(7) {
		
		@Override
		public void run(int count) {
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
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
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (getPlayer().equals(e.getDamager())) {
			if (attacked.isRunning()) attacked.setCount(7);
			else attacked.start();
		}
	}
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (swords.contains(e.getOffHandItem().getType())) {
    		if (!cool.isCooldown() && attacked.isRunning()) {
    			Block lastEmpty = null;
    			try {
					for (BlockIterator iterator = new BlockIterator(getPlayer().getWorld(), getPlayer().getLocation().toVector(), getPlayer().getLocation().getDirection(), 1, 15); iterator.hasNext(); ) {
						final Block block = iterator.next();
						if (!block.getType().isSolid()) {
							lastEmpty = block;
						}
					}
				} catch (IllegalStateException ignored) {
				}
				if (lastEmpty != null) {
					new DimensionCutter(getPlayer().getLocation(), lastEmpty.getLocation()).start();
					getPlayer().teleport(LocationUtil.floorY(lastEmpty.getLocation()).setDirection(getPlayer().getLocation().getDirection()));
				} else {
					getPlayer().sendMessage("§4[§c!§4] §f바라보는 방향에 이동할 수 있는 곳이 없습니다.");
				}
    			e.setCancelled(true);
    		}
    	}
    }
	
	private class DimensionCutter extends AbilityTimer {
		
    	private Set<Damageable> damagedcheck = new HashSet<>();
    	private final Location startLoc;
    	private final Location endLoc;
		
		private DimensionCutter(Location startLoc, Location endLoc) {
			super(100);
			setPeriod(TimeUnit.TICKS, 1);
			this.startLoc = startLoc;
			this.endLoc = endLoc;
		}
		
    	@Override
    	protected void run(int count) {
    		if (count % 20 == 0) {
        		for (Location loc : Line.between(startLoc, endLoc, (int) Math.min(250, 5 * Math.sqrt(startLoc.distanceSquared(endLoc)))).toLocations(startLoc)) {
        			ParticleLib.PORTAL.spawnParticle(loc.clone().add(0, 1, 0), 0, 0, 0, 20, 0.2);
        		}
    		}
    		for (Damageable d : LocationUtil.rayTraceEntities(Damageable.class, startLoc, endLoc, 0.75, predicate)) {
    			if (!d.equals(getPlayer()) && !damagedcheck.contains(d)) {
        			Damages.damageMagic(d, getPlayer(), false, 3);
            		damagedcheck.add(d);
    			}
    		}
    	}
		
	}
    
}
