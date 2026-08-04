package rainstar.abilitywar.game.murder.murderer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.event.GameEndEvent;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractDetective;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.FireworkUtil;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "머더: 사이비 교주", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"모든 시민을 교화시키세요!",
		"활과 화살을 반납하고 금으로 돌려받습니다.",
		"§8[§a신고식§8]§f 종교명을 정해 개설합니다. 모두가 이를 알게 됩니다.",
		"§8[§b포교§8]§f 검으로 공격한 대상은 §c광신도§f로 새로이 태어납니다.",
		"§c광신도§f는 §5머더§f이며, 검을 지급받습니다. §c쿨타임 §7: §f20초",
		"교주가 사망하면 모든 §c광신도§f가 같이 사망합니다.",
		"§8[§3심판§8]§f §3탐정§f만 남은 상황이 아니라면, §3탐정§f을 §b포교§f하려 할 경우 실패합니다.",
		"§8[§c십일조§8]§f 금 좌클릭으로 모든 §c광신도§f의 금을 절반씩 거둡니다. §c쿨타임 §7: §f30초",
		"§8[§b신앙심§8]§f 누군가가 자신을 화살로 죽이려 할 때 가진 금을 전부 소모하여",
		"§e소모한 금 §7× §c광신도 수 §7× §b0.5초§f간 사망을 지연시키고,",
		"사망 지연 간 §b포교§f의 §c쿨타임§f이 90% 줄어듭니다. §8(§71회용§8)"
})
public class CultLeaderMurderer extends AbstractMurderer {
	
	public CultLeaderMurderer(Participant participant) {
		super(participant);
	}
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			final PlayerInventory inventory = getPlayer().getInventory();
			final boolean hadSword = Items.isMurdererSword(inventory.getItem(1));
			final int arrowCount = getArrowCount();
			inventory.clear();
			final MurderMystery murderMystery = (MurderMystery) getGame();
			murderMystery.addGold(getParticipant(), arrowCount * 8);
			getPlayer().getInventory().setHeldItemSlot(0);
			murderMystery.updateGold(getParticipant());
			if (!hadSword) {
				getPlayer().sendMessage("§e15초 §f뒤에 §4살인자§c의 검§f을 얻습니다.");
				new AbilityTimer(1) {
					@Override
					protected void run(int count) {
						getPlayer().getInventory().setHeldItemSlot(0);
						inventory.setItem(1, Items.MURDERER_SWORD.getStack());
						getPlayer().sendMessage("§4살인자§c의 검§f을 들고 있을 때 더 빠르게 움직일 수 있습니다.");
						for (Player player : Bukkit.getOnlinePlayers()) {
							NMS.sendTitle(player, "§4머더§c가 검을 얻었습니다.", "", 10, 80, 10);
							new AbilityTimer(1) {
								@Override
								protected void onEnd() {
									NMS.clearTitle(player);
								}
							}.setInitialDelay(TimeUnit.SECONDS, 5).start();
						}
					}
				}.setInitialDelay(TimeUnit.SECONDS, 15).start();
			} else {
				getPlayer().getInventory().setHeldItemSlot(0);
				inventory.setItem(1, Items.MURDERER_SWORD.getStack());
			}
			PASSIVE.start();
			nameSelect.start();
			
			NMS.sendTitle(getPlayer(), "§e직업§f: §5사이비 교주", "§f모든 §a시민§f에게 진리를 설파하세요.", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
		} else if (update == Update.ABILITY_DESTROY) {
			if (!end) {
				Bukkit.broadcastMessage("§8[§3일심§8]§f §e" + religionName + "§f에 소속된 모든 광신도가 사망합니다...");
				if (!belivers.isEmpty()) {
					List<Participant> beliverlist = new ArrayList<>();
					for (Participant participant : belivers) {
						beliverlist.add(participant);
					}	
					for (Participant beliver : beliverlist) {
						beliver.getPlayer().setHealth(0);
					}
				}
			}
			NMS.setInvisible(getPlayer(), false);	
		}
	}
	
	private String religionName = null;
	private final ActionbarChannel ac = newActionbarChannel();
	private final Set<Participant> belivers = new HashSet<>();
	private final Cooldown cooldown = new Cooldown(20, "포교"), skillcooldown = new Cooldown(30, "십일조");
	private boolean onetime = true;
	private static final Circle circle = Circle.of(0.7, 30);
	private final DecimalFormat df = new DecimalFormat("0.0");
	private boolean end = false;
	
	private final AbilityTimer nameSelect = new AbilityTimer(120) {
		@Override
		protected void onStart() {
			getPlayer().sendMessage("§8[§a신고식§8] §f창시할 종교의 이름을 채팅창에 `을 붙여 입력하세요. (최대 15글자)");
			getPlayer().sendMessage("§8[§a신고식§8] §f예시: §e'`능력자'§f를 입력했다면, 종교의 이름은 §e'능력자교'§f가 됩니다.");
		}

		@Override
		protected void run(int count) {
			ac.update("§5종교§d의 이름을 정하세요: §e" + count + "");
		}

		@Override
		protected void onEnd() {
			ac.update(null);
			newReligion("이름 없는 종교");
			getPlayer().sendMessage("§8[§a신고식§8] §f종교의 이름이 선택되지 않아 이름이 임의로 설정되었습니다.");
		}
		
		@Override
		protected void onSilentEnd() {
			ac.update(null);
		}

	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
	private final AbilityTimer nodeath = new AbilityTimer(10) {
		
		@Override
		public void run(int count) {
			ac.update("§c사망 지연§7: §e" + df.format(count / 20.0) + "§f초");
		}
		
		@Override
		public void onEnd() {
			getPlayer().setHealth(0);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private void newReligion(String name) {
		religionName = name;
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.broadcastMessage("§8[§a신고식§8] §5" + name + "§f가 창시되었습니다.");
			}
		}.runTask(AbilityWar.getPlugin());
	}
	
	@SubscribeEvent
	private void onGameEnd(GameEndEvent e) {
		if (getGame().equals(e.getGame())) end = true;
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (!skillcooldown.isCooldown()) {
					if (belivers.size() > 0) {
						MurderMystery murderMystery = (MurderMystery) getGame();
						getPlayer().sendMessage("§8[§c십일조§8] §f신자들에게서 금을 얻어냅니다.");
						for (Participant beliver : belivers) {
							int amount = murderMystery.getGold(beliver) / 2;
							murderMystery.addGold(getParticipant(), amount);
							murderMystery.consumeGold(beliver, amount);
							beliver.getPlayer().sendMessage("§8[§c십일조§8] §f교주 §5" + getPlayer().getName() + "§f님께 §e금 " + amount + "개§f를 바칩니다...");
						}	
					}
					skillcooldown.start();
				}
			}
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
		if (nameSelect.isRunning() && e.getMessage().charAt(0) == '`') {
			e.setCancelled(true);
			String name = e.getMessage();
			name = name.substring(1);
			if (name.length() <= 31) {
				NewReligionEvent newReligionEvent = new NewReligionEvent(e.isAsynchronous(), name.concat("교"));
				Bukkit.getPluginManager().callEvent(newReligionEvent);
				if (!newReligionEvent.isCancelled()) {
					nameSelect.stop(true);
					newReligion(name + "교");
				} else {
					getPlayer().sendMessage(String.valueOf(newReligionEvent.cancelMessage));
				}
			} else {
				getPlayer().sendMessage("§8[§a신고식§8] §c종교 이름은 최대 30글자입니다.");
			}
		}
	}
	
	@SubscribeEvent(priority = 7)
	private void onArrowKill(ArrowKillEvent e) {
		if (!e.isCancelled()) {
			if (!(e.getShooter().getAbility() instanceof AbstractMurderer) && e.getTarget().equals(getParticipant())) {
				if (onetime) {
					e.setCancelled(true);
					for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
						ParticleLib.TOTEM.spawnParticle(loc.clone().add(0, 2, 0), 0, 0, 0, 1, 0);
					}
					SoundLib.ENTITY_EVOKER_PREPARE_SUMMON.playSound(getPlayer().getLocation(), 1, 1);
					new BukkitRunnable() {
						@Override
						public void run() {
							SoundLib.ENTITY_EVOKER_PREPARE_SUMMON.playSound(getPlayer().getLocation(), 1, 1.3f);
						}	
					}.runTaskLater(AbilityWar.getPlugin(), 10L);
					new BukkitRunnable() {
						@Override
						public void run() {
							SoundLib.ENTITY_EVOKER_PREPARE_SUMMON.playSound(getPlayer().getLocation(), 1, 2);
						}	
					}.runTaskLater(AbilityWar.getPlugin(), 30L);
					onetime = false;
					final MurderMystery murderMystery = (MurderMystery) getGame();
					nodeath.start();
					nodeath.setCount(murderMystery.getGold(getParticipant()) * belivers.size() * 10);
					if (cooldown.isRunning() && cooldown.getCount() > 4) cooldown.setCount(4);
				} else if (nodeath.isRunning()) {
					e.setCancelled(true);
					for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
						ParticleLib.TOTEM.spawnParticle(loc.clone().add(0, 2, 0), 0, 0, 0, 1, 0);
					}
					SoundLib.ENTITY_EVOKER_PREPARE_SUMMON.playSound(getPlayer().getLocation(), 1, 2f);
				}
			}	
		}
	}
	
	@SubscribeEvent(priority = 10)
	private void onArrowKilledFanatic(ArrowKillEvent e) {
		if (!e.isCancelled()) {
			if (belivers.contains(e.getTarget())) {
				belivers.remove(e.getTarget());
			}	
		}
	}
	
	@SubscribeEvent(priority = 0)
	private void onMurder(MurderEvent e) {
		if (e.getMurderer().getParticipant().equals(getParticipant())) {
			e.setCancelled(true);
			if (!cooldown.isCooldown()) {
				if (e.getTarget().hasAbility()) {
					if (e.getTarget().getAbility().getClass().getSimpleName().contains("Detective") && !e.getTarget().getAbility().getClass().equals(CorruptionDetectiveMurderer.class)) {
						boolean fail = false;
						for (Participant participant : getGame().getParticipants()) {
							if (participant.getAbility() instanceof AbstractInnocent && !(participant.getAbility() instanceof AbstractDetective)) {
								fail = true;
								break;
							}
						}
						if (fail) getPlayer().sendMessage("§8[§3심판§8] §c포교에 실패하였습니다...");
						else {
							try {
								AbilityRegistration registration = e.getTarget().getAbility().getRegistration();
								e.getTarget().setAbility(FanaticMurderer.class);
								FanaticMurderer murderer = (FanaticMurderer) e.getTarget().getAbility();
								murderer.setInsideAbility(registration);
								belivers.add(e.getTarget());
								getPlayer().sendMessage("§8[§b포교§8]§f §e" + e.getTarget().getPlayer().getName() + "§f님을 포교하였습니다. §5" + religionName + " §f만세!");
								e.getTarget().getPlayer().sendMessage("§8[§b포교§8] §f교주 §5" + getPlayer().getName() + "§f님이 당신을 §e" + religionName + "§f에 포교하였습니다. §5" + religionName + " §f만세!");
								cooldown.start();
								if (nodeath.isRunning()) cooldown.setCount(4);
								boolean win = true;
								List<Participant> murderers = new ArrayList<>();
								for (Participant participant : getGame().getParticipants()) {
									if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId())) {
										if (!(participant.getAbility() instanceof AbstractMurderer)) win = false;
										else murderers.add(participant);
										if (!win) break;
									}
								}
								
								if (win) {
									Messager.clearChat();
									List<Participant> normalmurderers = new ArrayList<>();
									final StringJoiner normalmurderersName = new StringJoiner("§c, ");
									final StringJoiner beliversName = new StringJoiner("§d, ");
									for (Participant participant : murderers) {
										if (!belivers.contains(participant) && !getParticipant().equals(participant)) normalmurderers.add(participant);
										Player p = participant.getPlayer();
										SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(p);
										new SimpleTimer(TaskType.REVERSE, 8) {
											@Override
											protected void run(int seconds) {
												FireworkUtil.spawnWinnerFirework(p.getEyeLocation());
											}
										}.setPeriod(TimeUnit.TICKS, 4).start();
									}
									for (Participant participant : normalmurderers) {
										normalmurderersName.add(participant.getPlayer().getName());
									}
									for (Participant participant : belivers) {
										beliversName.add(participant.getPlayer().getName());
									}
									Bukkit.broadcastMessage("§c교단 §5" + religionName + "§a의 우승입니다!");
									Bukkit.broadcastMessage("§5교주§f: §c" + getPlayer().getName());
									if (normalmurderers.size() > 0) Bukkit.broadcastMessage("§4머더§f: §c" + normalmurderersName.toString());
									Bukkit.broadcastMessage("§3신자§f: §d" + beliversName.toString());
									GameManager.stopGame();
								}
							} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
								e1.printStackTrace();
							}
						}
					} else if (!(e.getTarget().getAbility() instanceof AbstractMurderer)) {
						try {
							AbilityRegistration registration = e.getTarget().getAbility().getRegistration();
							e.getTarget().setAbility(FanaticMurderer.class);
							FanaticMurderer murderer = (FanaticMurderer) e.getTarget().getAbility();
							murderer.setInsideAbility(registration);
							belivers.add(e.getTarget());
							getPlayer().sendMessage("§8[§b포교§8]§f §e" + e.getTarget().getPlayer().getName() + "§f님을 포교하였습니다. §5" + religionName + " §f만세!");
							e.getTarget().getPlayer().sendMessage("§8[§b포교§8] §f교주 §5" + getPlayer().getName() + "§f님이 당신을 §e" + religionName + "§f에 포교하였습니다. §5" + religionName + " §f만세!");
							cooldown.start();
							if (nodeath.isRunning()) cooldown.setCount(4);
							boolean win = true;
							List<Participant> murderers = new ArrayList<>();
							for (Participant participant : getGame().getParticipants()) {
								if (!((MurderMystery) getGame()).isDead(participant.getPlayer().getUniqueId())) {
									if (!(participant.getAbility() instanceof AbstractMurderer)) win = false;
									else murderers.add(participant);
									if (!win) break;
								}
							}
							
							if (win) {
								Messager.clearChat();
								List<Participant> normalmurderers = new ArrayList<>();
								final StringJoiner normalmurderersName = new StringJoiner("§c, ");
								final StringJoiner beliversName = new StringJoiner("§d, ");
								for (Participant participant : murderers) {
									if (!belivers.contains(participant) && !getParticipant().equals(participant)) normalmurderers.add(participant);
									Player p = participant.getPlayer();
									SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(p);
									new SimpleTimer(TaskType.REVERSE, 8) {
										@Override
										protected void run(int seconds) {
											FireworkUtil.spawnWinnerFirework(p.getEyeLocation());
										}
									}.setPeriod(TimeUnit.TICKS, 4).start();
								}
								for (Participant participant : normalmurderers) {
									normalmurderersName.add(participant.getPlayer().getName());
								}
								for (Participant participant : belivers) {
									beliversName.add(participant.getPlayer().getName());
								}
								Bukkit.broadcastMessage("§c교단 §5" + religionName + "§a의 우승입니다!");
								Bukkit.broadcastMessage("§5교주§f: §c" + getPlayer().getName());
								if (normalmurderers.size() > 0) Bukkit.broadcastMessage("§4머더§f: §c" + normalmurderersName.toString());
								Bukkit.broadcastMessage("§3신자§f: §d" + beliversName.toString());
								GameManager.stopGame();
							}
						} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					}	
				}
			}
		}
	}
	
	public static class ReligionEvent extends Event {

		private static final HandlerList handlers = new HandlerList();

		public static HandlerList getHandlerList() {
			return handlers;
		}

		@Override
		public HandlerList getHandlers() {
			return handlers;
		}

		private final Participant leader;

		private ReligionEvent(boolean async, CultLeaderMurderer cultLeader) {
			super(async);
			this.leader = cultLeader.getParticipant();
		}

		public Participant getLeader() {
			return leader;
		}

	}
	
	public class NewReligionEvent extends CultLeaderMurderer.ReligionEvent implements Cancellable {

		private final String name;

		private NewReligionEvent(boolean async, String name) {
			super(async, CultLeaderMurderer.this);
			this.name = name;
		}

		public String getName() {
			return name;
		}

		private boolean cancelled = false;
		private String cancelMessage = null;

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public void setCancelled(boolean cancelled) {
			this.cancelled = cancelled;
		}

		public void setCancelMessage(String cancelMessage) {
			this.cancelMessage = cancelMessage;
		}

	}
	
}
