package rainstar.abilitywar.game.murder.murderer;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.minecraft.FireworkUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.murder.system.Shield;

@AbilityManifest(name = "머더: 좀비", rank = Rank.SPECIAL, species = Species.UNDEAD, explain = {
		"모든 시민을 죽이세요!",
		"금 우클릭으로 금 16개를 소모해 §3실드§f를 1개 얻을 수 있습니다§8(§7최대 2§8)§f. §c쿨타임 §7: §f40초",
		"§8[§3진화§8]§f §3실드§f가 소모될 때 활과 화살을 1개 얻습니다.",
		"§8[§9아둔§8]§f 항상 구속 1을 받습니다. 배정 후 2초간 §e기절§f합니다.",
		"무작위 주기로 시선이 무작위 방향으로 뒤틀립니다.",
		"§8[§c폭주§8]§f 금 좌클릭으로 금 2개를 소모해 바라보는 방향으로 돌진합니다. §c쿨타임 §7: §f6초",
		"§8[§2감염§8]§f 처음으로 처치한 시민은 사망하는 대신, 이 직업이 됩니다."
})
public class ZombieMurderer extends AbstractMurderer {

	public ZombieMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			final PlayerInventory inventory = getPlayer().getInventory();
			getPlayer().getInventory().setHeldItemSlot(0);
			inventory.setItem(1, Items.MURDERER_SWORD.getStack());
			NMS.sendTitle(getPlayer(), "§e직업§f: §2좀비", "§f모든 §a시민§f을 §2감염§f시키세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			passive.start();
			PASSIVE.start();
			Stun.apply(getParticipant(), TimeUnit.SECONDS, 2);
		} else if (update == Update.ABILITY_DESTROY) {
			NMS.setInvisible(getPlayer(), false);
		}
	}
	
	private Random random = new Random();
	private final Cooldown dashcooldown = new Cooldown(6);
	private final Cooldown shieldcooldown = new Cooldown(40);
	private boolean onetime = true;
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			int randomnum = random.nextInt(85);
			if (randomnum == 0) {
				final Location location = getPlayer().getLocation();
				float yaw = location.getYaw() + random.nextInt(130) - 65;
				if (yaw > 180 || yaw < -180) {
					float mod = yaw % 180;
					if (mod < 0) {
						yaw = 180 + mod;
					} else if (mod > 0) {
						yaw = -180 + mod;
					}
				}
				NMS.rotateHead(getPlayer(), getPlayer(), yaw, location.getPitch() + random.nextInt(90) - 45);
			}
			PotionEffects.SLOW.addPotionEffect(getPlayer(), 20, 0, false, false, true);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (Shield.getShield(getParticipant()) < 2) {
					if (!shieldcooldown.isCooldown()) {
						final MurderMystery murderMystery = (MurderMystery) getGame();
						if (murderMystery.consumeGold(getParticipant(), 16)) {
							SoundLib.ENTITY_ZOMBIE_AMBIENT.playSound(getPlayer(), 1, 0.5f);
							Shield.addShield(getParticipant(), 1);
							getPlayer().sendMessage("§2[§3!§2] §3실드§f를 획득합니다...");
							shieldcooldown.start();
						}	
					}	
				} else getPlayer().sendMessage("§2[§3!§2] §c실드가 너무 많습니다. 최대 2개까지입니다.");
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (!dashcooldown.isCooldown()) {
					final MurderMystery murderMystery = (MurderMystery) getGame();
					if (murderMystery.consumeGold(getParticipant(), 2)) {
						SoundLib.ENTITY_ZOMBIE_INFECT.playSound(getPlayer(), 1, 1.7f);
						getPlayer().setVelocity(getPlayer().getLocation().getDirection().setY(0).normalize().multiply(1.4));
						dashcooldown.start();
					}
				}
			}
		}
	}
	
	@SubscribeEvent(priority = 18)
	private void onArrowKill(ArrowKillEvent e) {
		if (!e.isCancelled() && e.getShooter().equals(getParticipant()) && onetime) {
			e.setCancelled(true);
			if (e.getTarget().hasAbility()) {
				if (!(e.getTarget().getAbility() instanceof AbstractMurderer)) {
					try {
						e.getTarget().setAbility(ZombieMurderer.class);
						SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(getPlayer(), 1f, 0.85f);
						SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(e.getTarget().getPlayer(), 1f, 0.85f);
						getPlayer().sendMessage("§3[§2감염§3]§f §e" + e.getTarget().getPlayer().getName() + "§f님을 §2감염§f시켰습니다.");
						e.getTarget().getPlayer().sendMessage("§3[§2감염§3] §2" + getPlayer().getName() + "§f님이 당신을 §2감염§f시켰습니다.");
						boolean win = true;
						List<Participant> murderers = new ArrayList<>();
						List<Participant> zombies = new ArrayList<>();
						for (Participant participant : getGame().getParticipants()) {
							if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId())) {
								if (!(participant.getAbility() instanceof AbstractMurderer)) win = false;
								else if (participant.getAbility().getClass().equals(ZombieMurderer.class)) zombies.add(participant);
								else murderers.add(participant);
								if (!win) break;
							}
						}
						
						if (win) {
							Messager.clearChat();
							final StringJoiner normalmurderersName = new StringJoiner("§7, §c");
							final StringJoiner zombiemurderersName = new StringJoiner("§7, §2");
							for (Participant participant : murderers) {
								Player p = participant.getPlayer();
								SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(p);
								new SimpleTimer(TaskType.REVERSE, 8) {
									@Override
									protected void run(int seconds) {
										FireworkUtil.spawnWinnerFirework(p.getEyeLocation());
									}
								}.setPeriod(TimeUnit.TICKS, 4).start();
							}
							for (Participant participant : zombies) {
								Player p = participant.getPlayer();
								SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(p);
								new SimpleTimer(TaskType.REVERSE, 8) {
									@Override
									protected void run(int seconds) {
										FireworkUtil.spawnWinnerFirework(p.getEyeLocation());
									}
								}.setPeriod(TimeUnit.TICKS, 4).start();
							}
							for (Participant participant : murderers) {
								normalmurderersName.add("§c" + participant.getPlayer().getName());
							}
							for (Participant participant : zombies) {
								zombiemurderersName.add("§2" + participant.getPlayer().getName());
							}
							Bukkit.broadcastMessage("§2모두가 좀비에 감염되었습니다...");
							if (murderers.size() > 0) Bukkit.broadcastMessage("§4머더§f: §c" + normalmurderersName.toString() + zombiemurderersName.toString());
							GameManager.stopGame();
						}
					} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
						e1.printStackTrace();
					}
				}	
			}
			onetime = false;
		}
	}
	
	@SubscribeEvent(priority = 18)
	private void onMurder(MurderEvent e) {
		if (!e.isCancelled() && e.getMurderer().getParticipant().equals(getParticipant()) && onetime) {
			e.setCancelled(true);
			if (e.getTarget().hasAbility()) {
				if (!(e.getTarget().getAbility() instanceof AbstractMurderer)) {
					try {
						e.getTarget().setAbility(ZombieMurderer.class);
						SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(getPlayer(), 1f, 0.85f);
						SoundLib.ENTITY_ZOMBIE_VILLAGER_CURE.playSound(e.getTarget().getPlayer(), 1f, 0.85f);
						getPlayer().sendMessage("§3[§2감염§3]§f §e" + e.getTarget().getPlayer().getName() + "§f님을 §2감염§f시켰습니다.");
						e.getTarget().getPlayer().sendMessage("§3[§2감염§3] §2" + getPlayer().getName() + "§f님이 당신을 §2감염§f시켰습니다.");
						boolean win = true;
						List<Participant> murderers = new ArrayList<>();
						List<Participant> zombies = new ArrayList<>();
						for (Participant participant : getGame().getParticipants()) {
							if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId())) {
								if (!(participant.getAbility() instanceof AbstractMurderer)) win = false;
								else if (participant.getAbility().getClass().equals(ZombieMurderer.class)) zombies.add(participant);
								else murderers.add(participant);
								if (!win) break;
							}
						}
						
						if (win) {
							Messager.clearChat();
							final StringJoiner normalmurderersName = new StringJoiner("§7, §c");
							final StringJoiner zombiemurderersName = new StringJoiner("§7, §2");
							for (Participant participant : murderers) {
								Player p = participant.getPlayer();
								SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(p);
								new SimpleTimer(TaskType.REVERSE, 8) {
									@Override
									protected void run(int seconds) {
										FireworkUtil.spawnWinnerFirework(p.getEyeLocation());
									}
								}.setPeriod(TimeUnit.TICKS, 4).start();
							}
							for (Participant participant : zombies) {
								Player p = participant.getPlayer();
								SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(p);
								new SimpleTimer(TaskType.REVERSE, 8) {
									@Override
									protected void run(int seconds) {
										FireworkUtil.spawnWinnerFirework(p.getEyeLocation());
									}
								}.setPeriod(TimeUnit.TICKS, 4).start();
							}
							for (Participant participant : murderers) {
								normalmurderersName.add("§c" + participant.getPlayer().getName());
							}
							for (Participant participant : zombies) {
								zombiemurderersName.add("§2" + participant.getPlayer().getName());
							}
							Bukkit.broadcastMessage("§2모두가 좀비에 감염되었습니다...");
							if (murderers.size() > 0) Bukkit.broadcastMessage("§4머더§f: §c" + normalmurderersName.toString() + zombiemurderersName.toString());
							GameManager.stopGame();
						}
					} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
						e1.printStackTrace();
					}
				}	
			}
			onetime = false;
		}
	}
	
}
