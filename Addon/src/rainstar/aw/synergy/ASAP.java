package rainstar.aw.synergy;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@AbilityManifest(
		name = "속전속결", rank = Rank.B, species = Species.HUMAN, explain = {
		"§c주는 대미지 §f및 §c받는 대미지§f가 기본적으로 0이 됩니다.",
		"같은 대상을 7회 타격 시 대상은 §c사망§f합니다.",
		"같은 대상에게서 7회 피격 시 즉시 §c사망§f합니다.",
		"이 효과로 §c사망§f한 적은 부활 효과를 무시합니다."
		})

public class ASAP extends Synergy {

	public ASAP(Participant participant) {
		super(participant);
	}
	
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
	
	private final Map<Player, Stack> stackMap = new HashMap<>();
	private final Map<Player, MyStack> deathMap = new HashMap<>();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager() instanceof Player && e.getEntity().equals(getPlayer()) && !e.isCancelled()) {
			e.setDamage(0);
		}
		if (e.getEntity() instanceof Player && !e.getEntity().equals(getPlayer()) && !e.isCancelled() && e.getDamager().equals(getPlayer())) {
			Player p = (Player) e.getEntity();	
			if (getGame().getParticipant(p).hasAbility()) {
				AbilityBase ab = getGame().getParticipant(p).getAbility();
				final Mix mix = (Mix) ab;
				if (mix.hasSynergy()) {
					if (mix.getSynergy().getClass().equals(ASAP.class)) {
						e.setCancelled(true);
						getPlayer().sendMessage("[§c!§f] 대결이 빛의 속도로 끝났습니다...");
						p.sendMessage("[§c!§f] 대결이 빛의 속도로 끝났습니다...");
						if (!p.isDead()) p.setHealth(0);
						if (!getPlayer().isDead()) getPlayer().setHealth(0);
					}
				}

			}
		}
		if (!e.isCancelled() && e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
			if (e.getDamager().equals(getPlayer()) && predicate.test(e.getEntity())) {
				e.setDamage(0);
				if (stackMap.containsKey(e.getEntity())) {
					if (stackMap.get(e.getEntity()).addStack()) {
						Player target = (Player) e.getEntity();
						e.setCancelled(true);
						if (!target.isDead()) target.setHealth(0);
					}
				} else new Stack((Player) e.getEntity()).start();
			}
			if (e.getEntity().equals(getPlayer()) && predicate.test(e.getDamager())) {
				if (deathMap.containsKey(e.getDamager())) {
					if (deathMap.get(e.getDamager()).addMyStack()) {
						e.setCancelled(true);
					}
				} else new MyStack((Player) e.getDamager()).start();
			}
		}
		if (NMS.isArrow(e.getDamager()) && !e.isCancelled() && e.getEntity() instanceof Player) {
			Arrow arrow = (Arrow) e.getDamager();
			if (getPlayer().equals(arrow.getShooter()) && predicate.test(e.getEntity())) {
				e.setDamage(0);
				if (stackMap.containsKey(e.getEntity())) {
					if (stackMap.get(e.getEntity()).addStack()) {
						Player target = (Player) e.getEntity();
						e.setCancelled(true);
						if (!target.isDead()) target.setHealth(0);
					}
				} else new Stack((Player) e.getEntity()).start();
			}
			if (e.getEntity().equals(getPlayer()) && arrow.getShooter() instanceof Player) {
				if (predicate.test((Player) arrow.getShooter())) {
					if (deathMap.containsKey(arrow.getShooter())) {
						if (deathMap.get(arrow.getShooter()).addMyStack()) {
							e.setCancelled(true);
						}
					} else new MyStack((Player) arrow.getShooter()).start();
				}	
			}
		}
	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		
		private Stack(Player player) {
			super(15);
			setPeriod(TimeUnit.TICKS, 4);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("§4§lX", stack).concat(Strings.repeat("§7§lX", 7 - stack)));
			hologram.display(getPlayer());
			stackMap.put(player, this);
			addStack();
		}

		@Override
		protected void run(int count) {
			hologram.teleport(player.getWorld(), player.getLocation().getX(), 
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					player.getLocation().getYaw(), 0);
		}

		private boolean addStack() {
			setCount(15);
			stack++;
			hologram.setText(Strings.repeat("§4§lX", stack).concat(Strings.repeat("§7§lX", 7 - stack)));
			SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(player, 0.5f, 1.7f);
			switch(stack) {
			case 1: SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.C));
			break;
			case 2: SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.C));
			break;
			case 3: SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.C));
			break;
			case 4: SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
			break;
			case 5: SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
			break;
			case 6: SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
			break;
			}
			if (stack >= 7) {
				stop(false);
				getGame().new GameTimer(TaskType.NORMAL, 3) {
					@Override
					protected void run(int count) {
						SoundLib.PIANO.playInstrument(getPlayer().getLocation(), 1.3f, Note.natural(0, Tone.C));
						SoundLib.PIANO.playInstrument(getPlayer().getLocation(), 1.3f, Note.flat(0, Tone.E));
						SoundLib.PIANO.playInstrument(getPlayer().getLocation(), 1.3f, Note.natural(0, Tone.G));
					}
				}.setPeriod(TimeUnit.TICKS, 7).start();
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
			stackMap.remove(player);
		}
		
	}
	
	private class MyStack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		
		private MyStack(Player player) {
			super(15);
			setPeriod(TimeUnit.TICKS, 4);
			this.player = player;
			this.hologram = NMS.newHologram(getPlayer().getWorld(), getPlayer().getLocation().getX(),
					getPlayer().getLocation().getY() + getPlayer().getEyeHeight() + 0.6, getPlayer().getLocation().getZ(), 
					Strings.repeat("§4§lX", stack).concat(Strings.repeat("§7§lX", 7 - stack)));
			hologram.display(this.player);
			deathMap.put(player, this);
			addMyStack();
		}

		@Override
		protected void run(int count) {
			hologram.teleport(getPlayer().getWorld(), getPlayer().getLocation().getX(), 
					getPlayer().getLocation().getY() + getPlayer().getEyeHeight() + 0.6, getPlayer().getLocation().getZ(), 
					getPlayer().getLocation().getYaw(), 0);
		}

		private boolean addMyStack() {
			setCount(15);
			stack++;
			SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(getPlayer(), 0.5f, 1.7f);
			switch(stack) {
			case 1: SoundLib.PIANO.playInstrument(player, Note.natural(0, Tone.C));
			break;
			case 2: SoundLib.PIANO.playInstrument(player, Note.natural(0, Tone.C));
			break;
			case 3: SoundLib.PIANO.playInstrument(player, Note.natural(0, Tone.C));
			break;
			case 4: SoundLib.PIANO.playInstrument(player, Note.natural(0, Tone.D));
			break;
			case 5: SoundLib.PIANO.playInstrument(player, Note.natural(0, Tone.D));
			break;
			case 6: SoundLib.PIANO.playInstrument(player, Note.natural(0, Tone.D));
			break;
			}
			hologram.setText(Strings.repeat("§4§lX", stack).concat(Strings.repeat("§7§lX", 7 - stack)));
			if (stack >= 7) {
				if (!getPlayer().isDead()) getPlayer().setHealth(0);
				stop(false);
				getGame().new GameTimer(TaskType.NORMAL, 3) {
					@Override
					protected void run(int count) {
						SoundLib.PIANO.playInstrument(getPlayer().getLocation(), 1.3f, Note.natural(0, Tone.C));
						SoundLib.PIANO.playInstrument(getPlayer().getLocation(), 1.3f, Note.flat(0, Tone.E));
						SoundLib.PIANO.playInstrument(getPlayer().getLocation(), 1.3f, Note.natural(0, Tone.G));
					}
				}.setPeriod(TimeUnit.TICKS, 7).start();
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			hologram.unregister();
			deathMap.remove(player);
		}
		
	}
	
}