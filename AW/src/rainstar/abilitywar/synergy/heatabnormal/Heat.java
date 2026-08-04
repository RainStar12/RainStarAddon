package rainstar.abilitywar.synergy.heatabnormal;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.Frost;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.ListenerModule;
import daybreak.abilitywar.game.module.ModuleBase;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import kotlin.ranges.RangesKt;
import rainstar.abilitywar.utils.Notes;

@ModuleBase(Heat.class)
public class Heat extends GameTimer implements ListenerModule {

	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity instanceof ArmorStand) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	private BossBar bossBar = null;
	private int heatStack = 0, decreasestop = 0, decrease = 60, maxCooldown = 0, cooldown = 0, frostDuration = 0, frostEffect = 0;
	private Set<Player> addedPlayer = new HashSet<>();
	private DecimalFormat df = new DecimalFormat("0.0");
	
	private static final List<DamageCause> DAMAGE_CAUSES = Arrays.asList(
				DamageCause.FIRE,
				DamageCause.FIRE_TICK,
				DamageCause.LAVA,
				DamageCause.HOT_FLOOR
			);
	
	public Heat(AbstractGame abstractGame) {
		abstractGame.super(TaskType.INFINITE, -1);
		setPeriod(TimeUnit.TICKS, 1);
	}
	
	@Override
	public void register() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		bossBar = Bukkit.createBossBar("§c열기§7 | " + (decreasestop > 0 ? "§b고정 중§7: §f" + df.format(decreasestop / 20.0) : "§6감소까지§7: §e" + df.format(decrease / 20.0)), BarColor.RED, BarStyle.SEGMENTED_20);
		bossBar.setProgress(1);
		for (Player player : Bukkit.getOnlinePlayers()) {
    		if (!addedPlayer.contains(player)) {
    			bossBar.addPlayer(player);
    			addedPlayer.add(player);
    		}
		}
		for (Participant participant : getGame().getParticipants()) {
			Player player = participant.getPlayer();
			if (!addedPlayer.contains(player)) {
    			bossBar.addPlayer(player);
    			addedPlayer.add(player);
    		}
		}
		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
		start();
	}

	@Override
	public void unregister() {
		if (bossBar != null) bossBar.removeAll();
		HandlerList.unregisterAll(this);
		stop(false);
	}
	
	@Override
	public void run(int count) {
		if (bossBar != null) {
    		for (Player player : Bukkit.getOnlinePlayers()) {
	    		if (!addedPlayer.contains(player)) {
	    			bossBar.addPlayer(player);
	    			addedPlayer.add(player);
	    		}
    		}
    		for (Participant participant : getGame().getParticipants()) {
    			Player player = participant.getPlayer();
    			if (!addedPlayer.contains(player)) {
	    			bossBar.addPlayer(player);
	    			addedPlayer.add(player);
	    		}
    		}
		}
		
		if (frostEffect > 0) {
			if (count % 6 == 0) {
				if (frostEffect == 10) {
					for (Player player : Bukkit.getOnlinePlayers()) {
						SoundLib.ITEM_ELYTRA_FLYING.playSound(player, 1, 1);
					}
				}
				frostEffect--;
				bossBar.setStyle(BarStyle.SEGMENTED_10);
				bossBar.setProgress(RangesKt.coerceIn((10 - frostEffect) / 10.0, 0, 1));
				for (Player player : Bukkit.getOnlinePlayers()) {
					SoundLib.BLOCK_GLASS_BREAK.playSound(player, 1, (float) (frostEffect * 0.2));
					ParticleLib.ITEM_CRACK.spawnParticle(player.getLocation().clone().add(0, 1, 0), 0.5, 1, 0.5, (40 - (frostEffect * 2)), 0.05, MaterialX.PACKED_ICE);
					ParticleLib.ITEM_CRACK.spawnParticle(player.getLocation().clone().add(0, 1, 0), 0.5, 1, 0.5, (40 - (frostEffect * 2)), 0.05, MaterialX.ICE);
				}
				
				if (frostEffect == 0) {
					for (Participant participant : getGame().getParticipants()) {
						if (predicate.test(participant.getPlayer())) {
							Frost.apply(participant, TimeUnit.TICKS, frostDuration);
						}
					}
					music.start();
				}	
			}
		} else {
			if (cooldown > 0) {
				cooldown--;
				bossBar.setStyle(BarStyle.SOLID);
	    		bossBar.setProgress(RangesKt.coerceIn(cooldown / (double) maxCooldown, 0, 1));
			} else {
				bossBar.setStyle(BarStyle.SEGMENTED_20);
				if (decreasestop > 0) decreasestop--;
				else {
					if (heatStack > 0) {
						if (decrease > 0) decrease--;
						else {
							decrease = 60;
							heatStack--;
						}
					} else decrease = 60;
				}
	    		bossBar.setProgress(RangesKt.coerceIn(heatStack / 20.0, 0, 1));
	    		
	    		if (count % 40 == 0) {
		    		for (Participant participant : getGame().getParticipants()) {
		    			if (predicate.test(participant.getPlayer())) {
		    				participant.getPlayer().setFireTicks(60);
		    			}
		    		}	
	    		}
			}
		}
		
		bossBar.setColor(cooldown > 0 ? BarColor.BLUE : BarColor.RED);
		if (cooldown > 0) bossBar.setTitle("§b빅 프리즈§7 | §3열기 쿨타임§7: §9" + df.format(cooldown / 20.0));
		else bossBar.setTitle("§c열기§7 | " + (decreasestop > 0 ? "§b고정 중§7: §f" + df.format(decreasestop / 20.0) : "§6감소까지§7: §e" + df.format(decrease / 20.0)));
	}
	
	public boolean heatUp(int stopDuration, int frostDuration, int cooldown, int decreaseCooldown) {
		if (this.cooldown <= 0) {
			heatStack = Math.min(20, heatStack + 1);
			if (heatStack >= 20) {
				heatStack = 0;
				frostEffect = 10;
				this.frostDuration = frostDuration;
				this.cooldown = cooldown;
				maxCooldown = cooldown;
			} else decreasestop = stopDuration;	
			return true;
		} else {
			this.cooldown = Math.max(0, this.cooldown - decreaseCooldown);
			return false;
		}
	}
	
	public int getHeat() {
		return heatStack;
	}
	
	@EventHandler()
	private void onEntityDamage(EntityDamageEvent e) {
		if (DAMAGE_CAUSES.contains(e.getCause())) {
			e.setDamage(e.getDamage() * (1 + (heatStack * 0.05)));
		}
	}
	
	public SimpleTimer music = new SimpleTimer(TaskType.NORMAL, 153) {
		
		@Override
		public void run(int count) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				switch (count) {
			    case 0:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(19));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(7));
			        break;

			    case 4:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(14));
			        SoundLib.BELL.playInstrument(player, Notes.getNote(14));
			        break;

			    case 8:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(19));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(8));
			        break;

			    case 12:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(19));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(12));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(6));
			        break;

			    case 16:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(7));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(2));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(14));
			        break;

			    case 20:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;

			    case 22:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(10));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(10));
			        break;

			    case 24:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;

			    case 26:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(12));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(0));
			        break;

			    case 28:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(7));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(2));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;
			        
			    case 34:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(9));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(9));
			        break;

			    case 36:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;

			    case 40:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(9));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(9));
			        break;

			    case 42:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;

			    case 44:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(6));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(3));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(9));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(9));
			        break;

			    case 48:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(9));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(9));
			        break;

			    case 52:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(12));
			        break;

			    case 56:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(14));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(2));
			        break;

			    case 58:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(15));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(3));
			        break;

			    case 60:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(16));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(7));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(4));
			        break;
			    
			    case 64:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(14));
			        SoundLib.BELL.playInstrument(player, Notes.getNote(14));
			        break;
			        
			    case 68:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(19));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(7));
			        break;
			        
			    case 72:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(18));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(6));
			        break;
			        
			    case 74:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(16));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(4));
			        break;
			        
			    case 76:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(14));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(7));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(2));
			        break;
			        
			    case 80:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;
			        
			    case 82:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(9));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(9));
			        break;
			        
			    case 84:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;
			        
			    case 86:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(9));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(9));
			        break;
			        
			    case 88:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;
			        
			    case 94:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;
			        
			    case 96:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(9));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(9));
			        break;
			        
			    case 98:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;
			        
			    case 100:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(9));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(9));
			        break;
			        
			    case 102:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;
			        
			    case 104:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(9));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(9));
			        break;
			        
			    case 106:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;
			        
			    case 108:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(9));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(9));
			        break;
			        
			    case 110:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;
			        
			    case 112:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(9));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(9));
			        break;
			        
			    case 114:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;
			        
			    case 116:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(11));
			        SoundLib.BASS_GUITAR.playInstrument(player, Notes.getNote(11));
			        break;
			        
			    case 118:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(19));
			        SoundLib.BELL.playInstrument(player, Notes.getNote(19));
			        break;
			        
			    case 120:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(18));
			        SoundLib.BELL.playInstrument(player, Notes.getNote(18));
			        break;
			        
			    case 122:
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(23));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(19));
			        SoundLib.PIANO.playInstrument(player, Notes.getNote(16));
			        SoundLib.BELL.playInstrument(player, Notes.getNote(16));
			        break;
				}	
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 2);
	
}
