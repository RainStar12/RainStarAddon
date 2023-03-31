package rainstar.abilitywar.game.Chaos;

import java.util.List;

import javax.annotation.Nonnull;
import javax.naming.OperationNotSupportedException;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerQuitEvent;

import com.google.common.base.Strings;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.Configuration.Settings.DeathSettings;
import daybreak.abilitywar.config.game.GameSettings.Setting;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.AbstractGame.GameUpdate;
import daybreak.abilitywar.game.AbstractGame.Observer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.interfaces.Winnable;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.InfiniteDurability;
import daybreak.abilitywar.game.module.Invincibility;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.Seasons;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.Chaos.KingsGambitGame.KingsDamocles;

@GameManifest(name = "카오스 - 평준화", description = {
		"§5§ka§a평준화§5§ka",
		"",
		"§f능력의 등급이 낮을수록, 해당 능력을 복수로 획득합니다.",
		"§eC등급 10개 §7| §bB등급 7개 §7| §aA등급 5개 §7| §dS등급 3개 §7 | §6L등급 2개",
		"§cSPECIAL 등급 1개"
		})
public class LevelingGame extends Game implements DefaultKitHandler, Winnable, Observer {
	
	public LevelingGame() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
		setRestricted(Settings.InvincibilitySettings.isEnabled());
		attachObserver(this);
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}

	@Override
	protected void progressGame(int seconds) {
		switch (seconds) {
			case 1:
				List<String> lines = Messager.asList("§2==== §b게임 참여자 목록 §2====");
				int count = 0;
				for (Participant p : getParticipants()) {
					count++;
					lines.add("§a" + count + ". §f" + p.getPlayer().getName());
				}
				lines.add("§3총 인원수 : " + count + "명");
				lines.add("§2===========================");

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
					for (Participant participant : getParticipants()) {
						if (participant.hasAbility()) {
							switch(participant.getAbility().getRank()) {
							case C:
								for (int a = 0; a < 9; a++) {
									
								}
								break;
							}
						}
					}
				} else {
					Bukkit.broadcastMessage("§f능력자 게임 설정에 따라 §b능력§f을 추첨하지 않습니다.");
				}
				break;
			case 8:
				Bukkit.broadcastMessage("§a평준화 작업을 완료했습니다.");
				Bukkit.broadcastMessage("§e잠시 후 게임이 시작됩니다.");
				break;
			case 10:
				Bukkit.broadcastMessage("§e게임이 §c5§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 11:
				Bukkit.broadcastMessage("§e게임이 §c4§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 12:
				Bukkit.broadcastMessage("§e게임이 §c3§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 13:
				Bukkit.broadcastMessage("§e게임이 §c2§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 14:
				Bukkit.broadcastMessage("§e게임이 §c1§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 15:
				if (Seasons.isChristmas()) {
					final String blocks = Strings.repeat("§c■§2■", 22);
					Bukkit.broadcastMessage(blocks);
					Bukkit.broadcastMessage("§f            §2Leveling §f- §a평준화  ");
					Bukkit.broadcastMessage("§f                   게임 시작                ");
					Bukkit.broadcastMessage(blocks);
				} else {
					for (String line : Messager.asList(
							"§3■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■",
							"§f             §2Leveling §f- §a평준화  ",
							"§f                    게임 시작                ",
							"§3■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■")) {
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

	@EventHandler
	private void onPlayerQuit(PlayerQuitEvent e) {
		Player player = e.getPlayer();
		if (isParticipating(player)) {
			Participant quitParticipant = getParticipant(player);
			getDeathManager().Operation(quitParticipant);
			Player winner = null;
			for (Participant participant : getParticipants()) {
				if (!getDeathManager().isExcluded(player)) {
					if (winner == null) {
						winner = player;
					} else {
						return;
					}
				}
			}
			if (winner != null) Win(getParticipant(winner));
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
				Player winner = null;
				for (Participant participant : getParticipants()) {
					Player player = participant.getPlayer();
					if (!isExcluded(player)) {
						if (winner == null) {
							winner = player;
						} else {
							return;
						}
					}
				}
				if (winner != null) Win(getParticipant(winner));
			}
		};
	}

	@Override
	public void update(GameUpdate update) {
		if (update == GameUpdate.END) {
			HandlerList.unregisterAll(this);
		}
	}

}
