package RainStarAbility;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import RainStarEffect.Mute;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.NotAvailable;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.ability.event.AbilityPreTargetEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.triplemix.AbstractTripleMix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Points;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableMap;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "선견지명", rank = Rank.S, species = Species.HUMAN, explain = {
		"검 들고 F키로 바라보고 있는 대상을 $[FORESIGHT_DURATION]초간 지정합니다. $[COOLDOWN]",
		"대상이 §a액티브 §3/ §6타게팅 §f스킬을 사용할 때 대상을 $[SKILL_DURATION]초간 §3침묵§f시키고,",
		"§3침묵§f되어있는 동안은 스킬을 자신이 대신 §b사용§f합니다. §8(§7패시브도 획득 가능§8)",
		"§9[§3침묵§9] §a액티브§f, §6타게팅§f 스킬을 사용할 수 없습니다."
		},
		summarize = {
		"검 들고 F키로 바라보고 있는 대상을 $[FORESIGHT_DURATION]초간 지정합니다.",
		"지정된 대상이 §a액티브 §3/ §6타게팅 §f스킬을 사용하면 대상을 §3침묵§f시킵니다.",
		"§3침묵§f된 대상이 사용하려던 스킬을 자신이 대신하여 사용합니다.",
		"§9[§3침묵§9] §a액티브§f, §6타게팅§f 스킬을 사용할 수 없습니다."
		})

@NotAvailable({AbstractMix.class, AbstractTripleMix.class})
public class Foresight extends AbilityBase {
	
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
	
	private static final Points CLOSED_EYE = Points.of(0.17, new boolean[][]{
		{true, false, false, false, false, false, false, false, false, false, false, false, false, false, true},
		{false, true, false, false, false, false, false, false, false, false, false, false, false, true, false},
		{false, false, true, true, false, false, false, false, false, false, false, true, true, false, false},
		{false, true, false, false, true, true, true, true, true, true, true, false, false, true, false},
		{false, false, false, false, true, false, false, true, false, false, true, false, false, false, false},
		{false, false, false, true, false, false, false, true, false, false, false, true, false, false, false},
		{false, false, false, false, false, false, false, true, false, false, false, false, false, false, false}
	});
	
	private static final Points OPEN_EYE = Points.of(0.17, new boolean[][]{
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
	private final ActionbarChannel ac = newActionbarChannel();
	private AbilityBase ab;
	private Player target = null;
	private final DecimalFormat df = new DecimalFormat("0.0");
	
	private static final ImmutableMap<Rank, String> rankcolor = ImmutableMap.<Rank, String>builder()
			.put(Rank.C, "§e")
			.put(Rank.B, "§b")
			.put(Rank.A, "§a")
			.put(Rank.S, "§d")
			.put(Rank.L, "§6")
			.put(Rank.SPECIAL, "§c")
			.build();
	
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
			if (!cooldown.isCooldown() && !targettimer.isRunning() && !skilltimer.isRunning()) {
				if (LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 30, predicate) != null) {
					final AbilityPreActiveSkillEvent event = new AbilityPreActiveSkillEvent(this, e.getOffHandItem().getType(), null);
					Bukkit.getPluginManager().callEvent(event);
					if (!event.isCancelled()) {
						Player p = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 30, predicate);
						target = p;
						targettimer.start();
					}		
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
			if (count % 4 == 0) {
				for (Participant participant : showplayers) {
					final Location headLocation = target.getEyeLocation().clone().add(0, 1.7, 0);
					final Location baseLocation = headLocation.clone().subtract(0, 1.6, 0);
					final float yaw = participant.getPlayer().getLocation().getYaw();
					for (Location loc : CLOSED_EYE.rotateAroundAxisY(-yaw).toLocations(baseLocation)) {
						ParticleLib.REDSTONE.spawnParticle(participant.getPlayer(), loc, color);
					}
					CLOSED_EYE.rotateAroundAxisY(yaw);	
				}	
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			target = null;
			showplayers.clear();
			if (!skilltimer.isRunning()) cooldown.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer skilltimer = new AbilityTimer(skilldur) {
		
		@Override
		public void run(int count) {
			ac.update(rankcolor.get(ab.getRank()) + ab.getDisplayName() + "§7: §a" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
			ab.destroy();
			if (!cooldown.isRunning()) cooldown.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onPreActiveSkill(AbilityPreActiveSkillEvent e) {
		if (!e.getParticipant().hasEffect(Mute.registration) && !e.isCancelled() && e.getParticipant().getPlayer().equals(target)) {
			e.setCancelled(true);
			Mute.apply(getGame().getParticipant(target), TimeUnit.TICKS, skilldur);
			new OpenEyeParticle(getGame().getParticipant(target)).start();
			SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
			try {
				ab = AbilityBase.create(e.getAbility().getRegistration(), getParticipant());
				ab.setRestricted(false);
				((ActiveHandler) ab).ActiveSkill(e.getMaterial(), e.getClickType());
				getPlayer().sendMessage("§3[§b선견지명§3] " + rankcolor.get(ab.getRank()) + ab.getDisplayName() + "§f" + KoreanUtil.getJosa(ab.getDisplayName(), Josa.을를) + " 예측하고 저지하였습니다.");
				skilltimer.start();
			} catch (ReflectiveOperationException e1) {
				e1.printStackTrace();
			}
			targettimer.stop(false);
		}
	}
	
	@SubscribeEvent
	public void onPreTargetSkill(AbilityPreTargetEvent e) {
		if (!e.getParticipant().hasEffect(Mute.registration) && !e.isCancelled() && e.getParticipant().getPlayer().equals(target)) {
			e.setCancelled(true);
			Mute.apply(getGame().getParticipant(target), TimeUnit.TICKS, skilldur);
			new OpenEyeParticle(getGame().getParticipant(target)).start();
			SoundLib.BLOCK_END_PORTAL_SPAWN.playSound(getPlayer().getLocation(), 1, 1.5f);
			try {
				ab = AbilityBase.create(e.getAbility().getRegistration(), getParticipant());
				ab.setRestricted(false);
				getPlayer().sendMessage("§3[§b선견지명§3] " + rankcolor.get(ab.getRank()) + ab.getDisplayName() + "§f" + KoreanUtil.getJosa(ab.getDisplayName(), Josa.을를) + " 예측하고 저지하였습니다.");
				((TargetHandler) ab).TargetSkill(Material.IRON_INGOT, target);
				skilltimer.start();
			} catch (ReflectiveOperationException e1) {
				e1.printStackTrace();
			}
			targettimer.stop(false);
		}
	}
	
	private class OpenEyeParticle extends AbilityTimer {
		
		private final Participant participant;
		
		public OpenEyeParticle(Participant participant) {
			super(TaskType.INFINITE, -1);
			setPeriod(TimeUnit.TICKS, 4);
			this.participant = participant;
		}
		
		@Override
		public void run(int count) {
			if (!participant.hasEffect(Mute.registration)) this.stop(false);
			else {
				final Location headLocation = participant.getPlayer().getEyeLocation().clone().add(0, 1.7, 0);
				final Location baseLocation = headLocation.clone().subtract(0, 1.6, 0);
				final float yaw = participant.getPlayer().getLocation().getYaw();
				for (Location loc : OPEN_EYE.rotateAroundAxisY(-yaw).toLocations(baseLocation)) {
					ParticleLib.REDSTONE.spawnParticle(loc, color);
				}
				CLOSED_EYE.rotateAroundAxisY(yaw);	
			}
		}
		
	}
	

}
