package rainstar.abilitywar.synergy;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import kotlin.ranges.RangesKt;

@AbilityManifest(
		name = "백발백중",
		rank = Rank.S, 
		species = Species.HUMAN, 
		explain = {
		"다른 생명체를 바라보면 생명체가 §e발광§f합니다.",
		"이때 내 투사체는 해당 생명체에게 §5유도§f됩니다.",
		"빗맞히지 않고 아무 생명체나 6회 연속 적중 시,",
		"$[DURATION]초간 내 모든 투사체가 마지막 타격 대상에게 §5유도§f됩니다."
		})

public class Infallibility extends Synergy {

	public Infallibility(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			looking.start();
			stackchecker.start();
		}
	}
	
	public static final SettingObject<Integer> DURATION = synergySettings.new SettingObject<Integer>(Infallibility.class,
			"duration", 15, "# 자동 유도 지속시간") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	private final DecimalFormat df = new DecimalFormat("0.00");
	private final int duration = DURATION.getValue();
	private BossBar bossBar1 = null;
	private BossBar bossBar2 = null;
	private Set<Projectile> projectile = new HashSet<>();
	private LivingEntity target = null;
	private LivingEntity lastTarget = null;
	double lengths = 0;
	private PotionEffect glowing = new PotionEffect(PotionEffectType.GLOWING, 10, 1, true, false);
	private int stack = 0;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof ArmorStand) return false;
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
	
	private final AbilityTimer stackchecker = new AbilityTimer() {
		
		@Override
    	public void onStart() {
    		bossBar1 = Bukkit.createBossBar("§b연속 적중 스택", BarColor.PINK, BarStyle.SEGMENTED_6);
    		bossBar1.setProgress(0);
    		bossBar1.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar1.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		bossBar1.setProgress(((double) stack / 6));
    		if (stack == 6) stop(false);
    	}
    	
		@Override
		public void onEnd() {
			autoaim.start();
			bossBar1.removeAll();
		}

		@Override
		public void onSilentEnd() {
			autoaim.start();
			bossBar1.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer autoaim = new AbilityTimer(duration * 20) {
		
		@Override
    	public void onStart() {
			SoundLib.BLOCK_ENCHANTMENT_TABLE_USE.playSound(getPlayer(), 1, 1.3f);
    		bossBar2 = Bukkit.createBossBar("§5자동 조준§f: §d" + df.format(duration) + "§f초", BarColor.PURPLE, BarStyle.SOLID);
    		bossBar2.setProgress(1);
    		bossBar2.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar2.setVisible(true);
    	}
    	
    	@Override
		public void run(int count) {
    		target = lastTarget;
    		bossBar2.setTitle("§5자동 조준§f: §d" + df.format((double) count / 20) + "§f초");
    		bossBar2.setProgress(RangesKt.coerceIn((double) count / (duration * 20), 0, 1));
    	}
    	
		@Override
		public void onEnd() {
			stackchecker.start();
			bossBar2.removeAll();
			stack = 0;
		}

		@Override
		public void onSilentEnd() {
			stackchecker.start();
			bossBar2.removeAll();
			stack = 0;
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer looking = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (!autoaim.isRunning()) {
				if (LocationUtil.getEntityLookingAt(LivingEntity.class, getPlayer(), 100, predicate) != null) {
					if (target != LocationUtil.getEntityLookingAt(LivingEntity.class, getPlayer(), 100, predicate)) {
						target = LocationUtil.getEntityLookingAt(LivingEntity.class, getPlayer(), 100, predicate);
						lastTarget = target;
						SoundLib.BELL.playInstrument(getPlayer(), Note.natural(1, Tone.A));	
					}
					target.addPotionEffect(glowing);
				} else {
					target = null;
				}	
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar1 != null) bossBar1.addPlayer(e.getPlayer());
			if (bossBar2 != null) bossBar2.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar1 != null) bossBar1.removePlayer(e.getPlayer());
			if (bossBar2 != null) bossBar2.removePlayer(e.getPlayer());
		}
	}
	
	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter()) && e.getEntity() != null) {
			lengths = e.getEntity().getVelocity().length();
			projectile.add(e.getEntity());
			new Homing(e.getEntity()).start();
		}
	}
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (!autoaim.isRunning()) {
			if (projectile.contains(e.getEntity())) {
				if (e.getHitBlock() == null) {
					stack = Math.min(6, stack + 1);
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.C));
				} else if (e.getHitEntity() == null) {
					stack = Math.max(0, stack - 1);
					new AbilityTimer(3) {
						@Override
						public void run(int count) {
							switch(count) {
							case 3:
								SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(0, Tone.A));
								break;
							case 2:
								SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
								break;
							case 1:
								SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
								break;
							}
						}
					}.setPeriod(TimeUnit.TICKS, 2).start();
				}		
			}
		}
	}
	
	public class Homing extends AbilityTimer {
		
		private Projectile myprojectile;
		
		private Homing(Projectile projectiles) {
			super(TaskType.REVERSE, 300);
			setPeriod(TimeUnit.TICKS, 1);
			this.myprojectile = projectiles;
		}
		
		@Override
		protected void run(int arg0) {
			if (projectile.contains(myprojectile) && target != null) {
				myprojectile.setGravity(false);
				myprojectile.setVelocity(VectorUtil.validateVector((target.getLocation().clone().add(0, 1, 0).toVector()
						.subtract(myprojectile.getLocation().toVector())).normalize().multiply(lengths)));
			}
		}
		
		@Override
		protected void onEnd() {
			myprojectile.setGravity(true);
		}
		
		@Override
		protected void onSilentEnd() {
			onEnd();
		}
	}
	
}