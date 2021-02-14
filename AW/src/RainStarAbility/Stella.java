package RainStarAbility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.nms.IHologram;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.base.Strings;

@AbilityManifest(name = "스텔라", rank = Rank.A, species = Species.OTHERS, explain = {
		"§7공격 §8- §e반짝반짝 작은별♪§f: 다른 플레이어를 근접 공격할 때마다",
		" 대상에게 별무리 표식을 부여합니다. 표식은 공명 효과로도 부여 가능합니다.",
		" 대상이 가진 표식이 5개가 되면 대상이 발광 중이 아니라면 표식을 제거하고",
		" 발광을 부여합니다. 발광 중이라면 해제하고 1초간 기절시킵니다.",
		"§7철괴 우클릭 §8- §e별의 소녀§f: 주변 10칸의 플레이어들을 발광시키고 3칸 내",
		" 플레이어들을 $[DurationConfig]초간 지속해 발광시키며 또한 게임에 존재하는",
		" 별무리 표식의 수에 비례해 공명 피해를 강화합니다. $[CooldownConfig]",
		"§7패시브 §8- §b공명§f: 발광 효과를 가진 플레이어를 근접 공격할 때",
		" 피해량의 10%만큼의 마법 피해를 대상의 범위 3칸 내 플레이어들과",
		" 발광 효과를 받고 있는 플레이어들에게 입힙니다. $[PassiveCool]"})

public class Stella extends AbilityBase implements ActiveHandler {

	public Stella(Participant participant) {
		super(participant);
	}
	
	protected void onUpdate(AbilityBase.Update update) {
	    if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
	    	checkglow.start();
	    }
	}
	
	private final Map<Player, Stack> stackMap = new HashMap<>();
	private final Set<Participant> glowMap = new HashSet<>();
	private Participant target;
	private final Cooldown cool = new Cooldown(CooldownConfig.getValue(), CooldownDecrease._50);
	private final Cooldown passivecool = new Cooldown(PassiveCool.getValue(), "공명", CooldownDecrease._50);
	private PotionEffect glowing = new PotionEffect(PotionEffectType.GLOWING, 150, 10, true, false);
	private PotionEffect powerglowing = new PotionEffect(PotionEffectType.GLOWING, 200, 15, true, false);
	private int stargroup = 0;
	private final ActionbarChannel ac = newActionbarChannel();
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	private static final Note Do = Note.natural(0, Tone.C), Re = Note.natural(0, Tone.D), Mi = Note.natural(0, Tone.E), 
			Fa = Note.natural(0, Tone.F), Sol = Note.natural(1, Tone.G), La = Note.natural(1, Tone.A);
		
	public static final SettingObject<Integer> CooldownConfig 
	= abilitySettings.new SettingObject<Integer>(Stella.class,
			"Cooldown", 40, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> PassiveCool 
	= abilitySettings.new SettingObject<Integer>(Stella.class,
			"Passive Cooldown", 5, "# 공명 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> DurationConfig 
	= abilitySettings.new SettingObject<Integer>(Stella.class,
			"Duration", 7, "# 지속 시간") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
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
	
	private final Predicate<Entity> soundpredicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
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
	
	private final Duration duration = new Duration(DurationConfig.getValue() * 20, cool) {
		
		@Override
		public void onDurationStart() {
			music.start();
			for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, predicate)) {
				player.addPotionEffect(powerglowing);
			}
		}
		
		@Override
		public void onDurationProcess(int count) {
			ac.update("§b별무리 표식§f: §e" + stargroup + "§f개");
			ParticleLib.VILLAGER_HAPPY.spawnParticle(getPlayer().getLocation(), 0, 1.5, 0, 5);
			for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 3, 3, predicate)) {
				player.addPotionEffect(powerglowing);
			}
		}
		
		@Override
		public void onDurationEnd() {
			onDurationSilentEnd();
		}
		
		@Override
		public void onDurationSilentEnd() {
			ac.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1);
	
	private final AbilityTimer music = new AbilityTimer(TaskType.NORMAL, 15) {
		
		@Override
		public void run(int count) {
			for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), 10, 10, soundpredicate)) {
				switch(count) {
				case 1:
				case 2:
				case 15:
						SoundLib.BELL.playInstrument(player, Do);
						break;
				case 13:
				case 14:
						SoundLib.BELL.playInstrument(player, Re);
						break;
				case 11:
				case 12:
						SoundLib.BELL.playInstrument(player, Mi);
						break;
				case 9:
				case 10:
						SoundLib.BELL.playInstrument(player, Fa);
						break;
				case 3:
				case 4:
				case 7:
						SoundLib.BELL.playInstrument(player, Sol);
						break;
				case 5:
				case 6:
						SoundLib.BELL.playInstrument(player, La);
						break;
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 6).register();
	
	private final AbilityTimer checkglow = new AbilityTimer() {
		   
    	@Override
		public void run(int count) {
			for (Participant participants : getGame().getParticipants()) {
				if (participants.getPlayer().hasPotionEffect(PotionEffectType.GLOWING) && !glowMap.contains(participants)) glowMap.add(participants);
				else if (!participants.getPlayer().hasPotionEffect(PotionEffectType.GLOWING) && glowMap.contains(participants)) glowMap.remove(participants);
			}
    	}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager().hasMetadata("StarFirework")) {
			e.setCancelled(true);
		}
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player
				&& !e.isCancelled() && predicate.test(e.getEntity())) {
			target = getGame().getParticipant((Player) e.getEntity());
			Player p = (Player) e.getEntity();
			if (stackMap.containsKey(e.getEntity())) {
				if (stackMap.get(e.getEntity()).addStack()) {
					if (!glowMap.contains(target)) {
						p.addPotionEffect(glowing);
						final Firework firework = getPlayer().getWorld().spawn(p.getEyeLocation(), Firework.class);
						final FireworkMeta meta = firework.getFireworkMeta();
						meta.addEffect(
								FireworkEffect.builder()
										.withColor(Color.WHITE)
										.with(Type.STAR)
										.build()
						);
						meta.setPower(0);
						firework.setFireworkMeta(meta);
						firework.setMetadata("StarFirework", NULL_VALUE);
						new BukkitRunnable() {
							@Override
							public void run() {
								firework.detonate();
							}
						}.runTaskLater(AbilityWar.getPlugin(), 1L);
					} else {
						p.removePotionEffect(PotionEffectType.GLOWING);
						p.setGlowing(false);
						Stun.apply(target, TimeUnit.TICKS, 20);
						final Firework firework = getPlayer().getWorld().spawn(p.getEyeLocation(), Firework.class);
						final FireworkMeta meta = firework.getFireworkMeta();
						meta.addEffect(
								FireworkEffect.builder()
										.withColor(Color.fromRGB(254, 254, 108), Color.fromRGB(72, 254, 254))
										.with(Type.STAR)
										.build()
						);
						meta.setPower(0);
						firework.setFireworkMeta(meta);
						firework.setMetadata("StarFirework", NULL_VALUE);
						new BukkitRunnable() {
							@Override
							public void run() {
								firework.detonate();
							}
						}.runTaskLater(AbilityWar.getPlugin(), 1L);
					}
				}
			} else new Stack((Player) e.getEntity()).start();
			if (!passivecool.isRunning() && p.hasPotionEffect(PotionEffectType.GLOWING)) {
				for (Participant participant : getGame().getParticipants()) {
					if (!participant.equals(getParticipant()) && !participant.equals(p)) {
						if (LocationUtil.isInCircle(e.getEntity().getLocation(), participant.getPlayer().getLocation(), 3)) {
							if (!glowMap.contains(participant)) {
								if (duration.isRunning()) {
									Damages.damageMagic(participant.getPlayer(), getPlayer(), true, (float) Math.min(e.getDamage() / 10, 7));
								} else {
									Damages.damageMagic(participant.getPlayer(), getPlayer(), true, (float) Math.min((e.getDamage() / 10) + (stargroup / 2), 7));
								}
								if (stackMap.containsKey(participant.getPlayer())) {
									if (stackMap.get(participant.getPlayer()).addStack()) {
										Player player = participant.getPlayer();
										if (glowMap.contains(participant)) {
											player.removePotionEffect(PotionEffectType.GLOWING);
											player.setGlowing(false);
											Stun.apply(participant, TimeUnit.TICKS, 20);
										} else {
											player.addPotionEffect(glowing);
										}
									}
								} else new Stack(participant.getPlayer()).start();
								passivecool.start();
							}
						} else {
							if (glowMap.contains(participant)) {
								if (duration.isRunning()) {
									Damages.damageMagic(participant.getPlayer(), getPlayer(), true, (float) Math.min(e.getDamage() / 10, 7));
								} else {
									Damages.damageMagic(participant.getPlayer(), getPlayer(), true, (float) Math.min((e.getDamage() / 10) + (stargroup / 2), 7));
								}
							}
							if (stackMap.containsKey(participant.getPlayer())) {
								if (stackMap.get(participant.getPlayer()).addStack()) {
									Player player = participant.getPlayer();
									if (glowMap.contains(participant)) {
										player.removePotionEffect(PotionEffectType.GLOWING);
										player.setGlowing(false);
										Stun.apply(participant, TimeUnit.TICKS, 20);
									} else {
										player.addPotionEffect(glowing);
									}
								}
							} else new Stack(participant.getPlayer()).start();
							passivecool.start();
						}	
					}
				}
			}
		}
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) &&
	    		!cool.isCooldown() && !duration.isDuration()) {
	    	duration.start();
	    	return true;
	    }
	    return false;
	}
	
	private class Stack extends AbilityTimer {
		
		private final Player player;
		private final IHologram hologram;
		private int stack = 0;
		
		private Stack(Player player) {
			super(75);
			setPeriod(TimeUnit.TICKS, 4);
			this.player = player;
			this.hologram = NMS.newHologram(player.getWorld(), player.getLocation().getX(),
					player.getLocation().getY() + player.getEyeHeight() + 0.6, player.getLocation().getZ(), 
					Strings.repeat("§e✭", stack).concat(Strings.repeat("§e✩", 5 - stack)));
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
			setCount(75);
			stack++;
			stargroup++;
			hologram.setText(Strings.repeat("§e✭", stack).concat(Strings.repeat("§e✩", 5 - stack)));
			if (stack >= 5) {
				stop(false);
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
			stargroup -= stack;
			hologram.unregister();
			stackMap.remove(player);
		}
		
	}
	
}
