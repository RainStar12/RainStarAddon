package RainStarAbility;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import RainStarSynergy.Sharper;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.Cooldown;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.Multiset.Entry;

@AbilityManifest(name = "애로우 레인", rank = Rank.A, species = Species.HUMAN, explain = {
		"화살을 발사할 때 화살비가 내립니다. §[COOLDOWN]"
		})

public class ArrowRain extends AbilityBase {

	public ArrowRain(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(Sharper.class,
			"cooldown", 20, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	private final Predicate<Block> blockpredicate = new Predicate<Block>() {
		@Override
		public boolean test(Block block) {
			if (!block.getType().isSolid() && !block.isLiquid()) {
				return true;
			}
			return false;
		}

		@Override
		public boolean apply(@Nullable Block arg0) {
			return false;
		}
	};
	
	private final Cooldown cool = new Cooldown(COOLDOWN.getValue());
	
	private Arrow arrow;
	private Set<Arrow> arrowSet = new HashSet<>();
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && NMS.isArrow(e.getEntity())) {
			if (!cool.isRunning()) {
				arrow = (Arrow) e.getEntity();
				cool.start();
			}
		}
	}
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (arrow != null) {
			if (getPlayer().equals(e.getEntity().getShooter()) && e.getEntity().equals(arrow)) {
				for (Location location : LocationUtil.getRandomLocations(e.getEntity().getLocation(), 5, 25)) {
					arrowSet.add(e.getEntity().getWorld().spawn(LocationUtil.floorY(location, blockpredicate).add(0, 7.5, 0), Arrow.class));
					for (Arrow arrow : arrowSet) {
						arrow.setGlowing(true);
						arrow.setShooter(getPlayer());
					}
				}
			}
		}
		if (arrowSet.contains(e.getEntity())) {
			arrowSet.remove(e.getEntity());
			e.getEntity().remove();
		}
	}
	
}
