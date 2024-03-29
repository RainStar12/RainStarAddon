package rainstar.abilitywar.game.Chaos.Overlap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.naming.OperationNotSupportedException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;

import com.google.common.base.Strings;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.Configuration.Settings.DeathSettings;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.AbstractGame.Observer;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.AbilitySelect;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.InfiniteDurability;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.Seasons;
import daybreak.abilitywar.utils.base.logging.Logger;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.utils.RankColor;

@GameManifest(name = "카오스 - 중첩", description = {
		"§5§kaaa§b중첩§5§kaaa",
		"",
		"§f적 처치 시, 대상의 능력을 흡수합니다.",
		"§f이 게임에서는 일부 능력이 등장하지 않습니다.",
		"§7제외 능력: 능력을 변경하는 능력 등"
		})
public class OverlapGame extends AbstractOverlap implements DefaultKitHandler, Observer {
	
	public OverlapGame() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
		setRestricted(Settings.InvincibilitySettings.isEnabled());
		attachObserver(this);
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}

	private static final Logger logger = Logger.getLogger(Game.class);

	@SuppressWarnings("serial")
	private List<String> NOT_CONTAINS = new ArrayList<String>() {
		{
			add("구미호");
			add("쿠로");
			add("카이로스");
			add("팬텀 시프");
			add("바이러스");
		}
	};
	
	@Override
	protected void progressGame(int seconds) {
		switch (seconds) {
			case 1:
				List<String> lines = Messager.asList("§3==== §b게임 참여자 목록 §3====");
				int count = 0;
				for (Participant p : getParticipants()) {
					count++;
					lines.add("§9" + count + ". §f" + p.getPlayer().getName());
				}
				lines.add("§1총 인원수 : " + count + "명");
				lines.add("§3===========================");

				for (String line : lines) {
					Bukkit.broadcastMessage(line);
				}

				if (getParticipants().size() < 2) {
					stop();
					Bukkit.broadcastMessage("§c최소 참가자 수를 충족하지 못하여 게임을 중지합니다. §8(§72명§8)");
				}
				break;
			case 3:
				lines = Messager.asList(
						"§5ChaosWar §f- §d혼돈의 전쟁",
						"§e버전 §7: §f4.0.1",
						"§b개발자 §7: §aRainStar_ 레인스타",
						"§9디스코드 §7: §a레인스타§7#0846"
				);

				GameCreditEvent event = new GameCreditEvent(this);
				Bukkit.getPluginManager().callEvent(event);
				lines.addAll(event.getCredits());

				for (String line : lines) {
					Bukkit.broadcastMessage(line);
				}
				break;
			case 5:
				if (Settings.getDrawAbility()) {
					for (String line : Messager.asList(
							"§f플러그인에 총 §b" + AbilityList.nameValues().size() + "개§f의 능력이 등록되어 있습니다.",
							"§7능력을 무작위로 할당합니다...")) {
						Bukkit.broadcastMessage(line);
					}
					try {
						startAbilitySelect();
					} catch (OperationNotSupportedException ignored) {
					}
				}
				break;
			case 6:
				if (Settings.getDrawAbility()) {
					Bukkit.broadcastMessage("§f모든 참가자가 능력을 §b확정§f했습니다.");
				} else {
					Bukkit.broadcastMessage("§f능력자 게임 설정에 따라 §b능력§f을 추첨하지 않습니다.");
				}
				break;
			case 8:
				Bukkit.broadcastMessage("§c검이 준비되었습니다.");
				Bukkit.broadcastMessage("§c잠시 후 게임이 시작됩니다.");
				break;
			case 10:
				Bukkit.broadcastMessage("§c게임이 §c5§c초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 11:
				Bukkit.broadcastMessage("§c게임이 §c4§c초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 12:
				Bukkit.broadcastMessage("§c게임이 §c3§c초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 13:
				Bukkit.broadcastMessage("§c게임이 §c2§c초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 14:
				Bukkit.broadcastMessage("§c게임이 §c1§c초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 15:
				if (Seasons.isChristmas()) {
					final String blocks = Strings.repeat("§c■§2■", 22);
					Bukkit.broadcastMessage(blocks);
					Bukkit.broadcastMessage("§f            §3Overlap §f- §b중첩  ");
					Bukkit.broadcastMessage("§f                   게임 시작                ");
					Bukkit.broadcastMessage(blocks);
				} else {
					for (String line : Messager.asList(
							"§9■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■",
							"§f             §3Overlap §f- §b중첩  ",
							"§f                    게임 시작                ",
							"§9■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■")) {
						Bukkit.broadcastMessage(line);
					}
				}

				giveDefaultKit(getParticipants());

				if (Settings.getSpawnEnable()) {
					Location spawn = Settings.getSpawnLocation().toBukkitLocation();
					for (Participant participant : getParticipants()) {
						participant.getPlayer().teleport(spawn);
					}
				}

				if (Settings.getNoHunger()) {
					Bukkit.broadcastMessage("§2배고픔 무제한§a이 적용됩니다.");
				} else {
					Bukkit.broadcastMessage("§4배고픔 무제한§c이 적용되지 않습니다.");
				}

				if (Settings.getInfiniteDurability()) {
					addModule(new InfiniteDurability());
				} else {
					Bukkit.broadcastMessage("§4내구도 무제한§c이 적용되지 않습니다.");
				}

				if (Settings.getClearWeather()) {
					for (World w : Bukkit.getWorlds()) {
						w.setStorm(false);
					}
				}
				
				if (isRestricted()) {
					getInvincibility().start(false);
				} else {
					Bukkit.broadcastMessage("§4초반 무적§c이 적용되지 않습니다.");
					setRestricted(false);
				}

				ScriptManager.runAll(this);
				
				startGame();
				break;
		}
	}

	@Override
	protected @Nonnull DeathManager newDeathManager() {
		return new DeathManager(this) {
			public void Operation(Participant victim) {
				switch (DeathSettings.getOperation()) {
					case 탈락:
						eliminate(victim);
						excludedPlayers.add(victim.getPlayer().getUniqueId());
						break;
					case 관전모드:
					case 없음:
						victim.getPlayer().setGameMode(GameMode.SPECTATOR);
						excludedPlayers.add(victim.getPlayer().getUniqueId());
						break;
				}
			}
		};
	}

	@Override
	public void update(GameUpdate update) {
		if (update == GameUpdate.END) {
			HandlerList.unregisterAll(this);
		}
	}

	@EventHandler()
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (e.getParticipant().getGame().getParticipant(e.getPlayer().getKiller()) != null) {
			Participant participant = e.getParticipant().getGame().getParticipant(e.getPlayer().getKiller());
			Overlap overlap = (Overlap) participant.getAbility();
			Overlap targetoverlap = (Overlap) e.getParticipant().getAbility();
			final StringJoiner joiner = new StringJoiner("§f, ");
			for (AbilityBase ability : targetoverlap.getAbilities()) {
				try {
					overlap.getAbilities().add(AbilityBase.create(ability.getRegistration(), participant));
					for (AbilityBase abil : overlap.getAbilities()) {
						abil.setRestricted(false);
					}
					joiner.add(RankColor.getColor(ability.getRank()) + ability.getDisplayName());
				} catch (ReflectiveOperationException e1) {
					e1.printStackTrace();
				}
			}
			participant.getPlayer().sendMessage("§5[§d!§5] " + joiner.toString() + "§f을 흡수하였습니다.");
		}
	}
	
	@Override
	public AbilitySelect newAbilitySelect() {
		return new AbilitySelect(this, getParticipants(), Settings.getAbilityChangeCount()) {

			List<AbilityRegistration> registrations;
			
			@Override
			protected void drawAbility(Collection<? extends Participant> selectors) {
				registrations = AbilityList.values().stream().filter(
						ability -> ability.isAvailable(getGame().getClass()) && !Configuration.Settings.isBlacklisted(ability.getManifest().name()) && !NOT_CONTAINS.contains(ability.getManifest().name())
				).collect(Collectors.toList());
				
				if (registrations.size() > 0) {
					Random random = new Random();

					for (Participant participant : selectors) {
						AbilityRegistration registration = registrations.get(random.nextInt(registrations.size()));
						try {
							participant.setAbility(Overlap.class);
							Overlap overlap = (Overlap) participant.getAbility();
							overlap.getAbilities().add(AbilityBase.create(registration, participant));

							final Player player = participant.getPlayer();
							player.sendMessage("§a능력이 할당되었습니다. §e/aw check§f로 확인하세요.");
							if (!hasDecided(participant)) {
								player.sendMessage("§e/aw yes §f명령어로 능력을 확정하거나, §e/aw no §f명령어로 능력을 변경하세요.");
							}
						} catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
							logger.error(ChatColor.YELLOW + participant.getPlayer().getName() + ChatColor.WHITE + "님에게 능력을 할당하는 도중 오류가 발생하였습니다.");
							logger.error("문제가 발생한 능력: " + ChatColor.AQUA + registration.getAbilityClass().getName());
						}
					}	
				} else {
					Messager.broadcastErrorMessage("사용 가능한 능력이 없습니다.");
					GameManager.stopGame();
				}
			}

			@Override
			protected boolean changeAbility(Participant participant) {
				Player p = participant.getPlayer();

				if (registrations.size() > 0) {
					Random random = new Random();

					if (participant.hasAbility()) {
						AbilityRegistration registration = registrations.get(random.nextInt(registrations.size()));
						try {
							participant.setAbility(Overlap.class);
							Overlap overlap = (Overlap) participant.getAbility();
							overlap.getAbilities().add(AbilityBase.create(registration, participant));

							final Player player = participant.getPlayer();
							player.sendMessage("§a능력이 할당되었습니다. §e/aw check§f로 확인하세요.");
							if (!hasDecided(participant)) {
								player.sendMessage("§e/aw yes §f명령어로 능력을 확정하거나, §e/aw no §f명령어로 능력을 변경하세요.");
							}
						} catch (Exception e) {
							logger.error(ChatColor.YELLOW + p.getName() + ChatColor.WHITE + "님의 능력을 변경하는 도중 오류가 발생하였습니다.");
							logger.error(ChatColor.WHITE + "문제가 발생한 능력: " + ChatColor.AQUA + registration.getAbilityClass().getName());
						}
					}
				} else {
					Messager.sendErrorMessage(p, "능력을 변경할 수 없습니다.");
				}

				return false;
			}
		};
	}
	
}
