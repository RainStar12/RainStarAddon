package RainStarAbility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import RainStarEffect.Mute;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.ability.event.AbilityPreTargetEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "선견지명", rank = Rank.S, species = Species.HUMAN, explain = {
		"검 들고 F키로 바라보고 있는 대상을 $[FORESIGHT_DURATION]초간 지정합니다. $[COOLDOWN]",
		"대상이 §a액티브 §3/ §6타게팅 §f스킬을 사용할 때 대상을 $[SKILL_DURATION]초간 §3침묵§f시키고,",
		"§3침묵§f되어있는 동안은 스킬을 자신이 대신 §b사용§f합니다. §8(§7패시브도 획득 가능§8)",
		"§9[§3침묵§9] §a액티브§f, §6타게팅§f 스킬을 사용할 수 없습니다."
		},
		summarize = {
		""
		})
public class Foresight extends AbilityBase implements ActiveHandler, TargetHandler {
	
	public Foresight(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Foresight.class, "cooldown", 77,
			"# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
		
		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Double> FORESIGHT_DURATION = 
			abilitySettings.new SettingObject<Double>(Foresight.class, "darkarts-duration", 5.0,
			"# 지정 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> SKILL_DURATION = 
			abilitySettings.new SettingObject<Double>(Foresight.class, "darkarts-duration", 10.0,
			"# 복제한 스킬 지속시간") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private static final Points CLOSED_EYE = Points.of(0.12, new boolean[][]{
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, false, true, true, false, false, false, false, false, false, false, true, true, false, false},
		{false, true, false, false, true, true, true, true, true, true, true, false, false, true, false},
		{false, false, false, false, true, false, false, true, false, false, true, false, false, false, false},
		{false, false, false, true, false, false, false, true, false, false, false, true, false, false, false},
		{false, false, false, false, false, false, false, true, false, false, false, false, false, false, false}
	});
	
	private static final Points OPEN_EYE = Points.of(0.12, new boolean[][]{
		{false, false, false, false, true, true, true, true, true, true, true, false, false, false, false},
		{false, false, true, true, false, false, true, true, true, false, false, true, true, false, false},
		{false, true, false, false, false, true, true, true, false, true, false, false, false, true, false},
		{true, false, false, false, false, true, true, true, true, true, false, false, false, false, true},
		{false, true, false, false, false, true, true, true, true, true, false, false, false, true, false},
		{false, false, true, true, false, false, true, true, true, false, false, true, true, false, false},
		{false, false, false, false, true, true, true, true, true, true, true, false, false, false, false}
	});	
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final int foresightdur = (int) (FORESIGHT_DURATION.getValue() * 20);
	private final int skilldur = (int) (SKILL_DURATION.getValue() * 20);
	private static final RGB color = RGB.of(1, 35, 49);

	private Player target = null;
	private static final Set<Material> swords;
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	@SubscribeEvent
	public void onSwap(PlayerSwapHandItemsEvent e) {
		if (swords.contains(e.getOffHandItem().getType()) && e.getPlayer().equals(getPlayer())) {
			if (!cooldown.isCooldown() && !targettimer.isRunning()) {
				if (LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 30, predicate) != null) {
					Player p = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 30, predicate);
					target = p;
					targettimer.start();
				}
			}
			e.setCancelled(true);
		}
	}
	
	public AbilityTimer targettimer = new AbilityTimer(foresightdur) {
		
		private List<Participant> showplayers;
		
		@Override
		public void onStart() {
			showplayers = new ArrayList<>(getGame().getParticipants());
			showplayers.remove(getGame().getParticipant(target));
		}
		
		@Override
		public void run(int count) {
			for (Participant participant : showplayers) {
				final Location headLocation = participant.getPlayer().getEyeLocation().clone().add(0, 1.5, 0);
				final Location baseLocation = headLocation.clone().subtract(0, 1.4, 0);
				final float yaw = participant.getPlayer().getLocation().getYaw();
				for (Location loc : CLOSED_EYE.rotateAroundAxisY(-yaw).toLocations(baseLocation)) {
					ParticleLib.REDSTONE.spawnParticle(loc, color);
				}
				CLOSED_EYE.rotateAroundAxisY(yaw);	
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		
		return false;
	}
	
	@Override
	public void TargetSkill(Material material, LivingEntity entity) {
		
	}
	
	@SubscribeEvent
	public void onPreActiveSkill(AbilityPreActiveSkillEvent e) {
		if (e.getParticipant().getPlayer().equals(target)) {
			e.setCancelled(true);
			Mute.apply(getParticipant(), TimeUnit.TICKS, skilldur);
			
		}
	}
	
	@SubscribeEvent
	public void onPreTargetSkill(AbilityPreTargetEvent e) {
		if (e.getParticipant().getPlayer().equals(target)) {
			e.setCancelled(true);
			Mute.apply(getParticipant(), TimeUnit.TICKS, skilldur);
		}
	}
	
	

}
