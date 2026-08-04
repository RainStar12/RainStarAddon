package rainstar.abilitywar.game.chaos.hyperchaos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.OperationNotSupportedException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.google.common.base.Strings;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.config.Configuration.Settings.InvincibilitySettings;
import daybreak.abilitywar.config.game.GameSettings.Setting;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.GameAliases;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.object.AbilitySelect;
import daybreak.abilitywar.game.module.InfiniteDurability;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.Seasons;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.SoundLib;

@GameManifest(name = "대혼돈", description = {
		"§5§kaaa§4대혼돈§5§kaaa",
		"",
		"§f블라인드셀렉트믹스시너지모드",
		"§f경고! 이 게임모드는 개뇌절입니다."
		})
@GameAliases({"블셀믹시전", "블셀믹시", "블라인드셀렉트믹스시너지", "블라인드셀렉트믹스시너지모드"})
public class HyperChaosGame extends AbstractMix {

private final boolean invincible = InvincibilitySettings.isEnabled();
	
	public static final Setting<Integer> CHANGE = gameSettings.new Setting<Integer>(HyperChaosGame.class, "change", 2, "# 리롤 기회") {
		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
	};
	
	public static final Setting<Integer> SYNERGY_CHANGE = gameSettings.new Setting<Integer>(HyperChaosGame.class, "synergy-chance", 100, "# 시너지 등장 확률", "# 기준: n/1000", "# 10으로 입력 시 10/1000, 1%입니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
	};

	public HyperChaosGame() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
	}

	@Override
	protected void progressGame(int seconds) {
		switch (seconds) {
			case 1:
				List<String> lines = Messager.asList("§8==== §5게임 참여자 목록 §8====");
				int count = 0;
				for (Participant p : getParticipants()) {
					count++;
					lines.add("§5" + count + ". §7" + p.getPlayer().getName());
				}
				lines.add("§5총 인원수 : " + count + "명");
				lines.add("§8==========================");

				for (String line : lines) {
					Bukkit.broadcastMessage(line);
				}

				if (getParticipants().size() < 1) {
					stop();
					Bukkit.broadcastMessage("§c최소 참가자 수를 충족하지 못하여 게임을 중지합니다. §8(§71명§8)");
				}
				break;
			case 3:
				lines = Messager.asList(
						"§8HyperChaos §f- §5대혼돈",
						"§8BlindSelectMixSynergyAbility §f- §5블라인드 셀렉트 믹스 시너지 능력자 전쟁",
						"§e버전 §7: §f" + AbilityWar.getPlugin().getDescription().getVersion(),
						"§b모드 개발자 §7: §fRainStar_ 레인스타",
						"§9디스코드 §7: §f레인스타§7#0846"
				);
				
				SoundLib.ENTITY_WITHER_SPAWN.broadcastSound();

				GameCreditEvent event = new GameCreditEvent(this);
				Bukkit.getPluginManager().callEvent(event);
				lines.addAll(event.getCredits());

				for (String line : lines) {
					Bukkit.broadcastMessage(line);
				}
				break;
			case 5:
				if (Configuration.Settings.getDrawAbility()) {
					try {
						startAbilitySelect();
					} catch (OperationNotSupportedException ignored) {
					}
				}
				break;
			case 6:
				Bukkit.broadcastMessage("§e잠시 후 게임이 시작됩니다.");
				break;
			case 8:
				Bukkit.broadcastMessage("§e게임이 §c5§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 9:
				Bukkit.broadcastMessage("§e게임이 §c4§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 10:
				Bukkit.broadcastMessage("§e게임이 §c3§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 11:
				Bukkit.broadcastMessage("§e게임이 §c2§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 12:
				Bukkit.broadcastMessage("§e게임이 §c1§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 13:
				if (Seasons.isChristmas()) {
					final String blocks = Strings.repeat("§c■§2■", 22);
					Bukkit.broadcastMessage(blocks);
					Bukkit.broadcastMessage("§f              §8HyperChaos §f- §5대혼돈  ");
					Bukkit.broadcastMessage("§f §8BlindSelectMixSynergyAbility §f- §5블라인드 셀렉트 믹스 시너지 능력자 전쟁 ");
					Bukkit.broadcastMessage("§f                   게임 시작                ");
					Bukkit.broadcastMessage(blocks);
				} else {
					final String blocks = Strings.repeat("§5■§8■", 22);
					Bukkit.broadcastMessage(blocks);
					Bukkit.broadcastMessage("§f              §8HyperChaos §f- §5대혼돈  ");
					Bukkit.broadcastMessage("§f §8BlindSelectMixSynergyAbility §f- §5블라인드 셀렉트 믹스 시너지 능력자 전쟁 ");
					Bukkit.broadcastMessage("§f                   게임 시작                ");
					Bukkit.broadcastMessage(blocks);
				}

				giveDefaultKit(getParticipants());

				if (Configuration.Settings.getSpawnEnable()) {
					Location spawn = Configuration.Settings.getSpawnLocation().toBukkitLocation();
					for (Participant participant : getParticipants()) {
						participant.getPlayer().teleport(spawn);
					}
				}

				if (Configuration.Settings.getNoHunger()) {
					Bukkit.broadcastMessage("§2배고픔 무제한§a이 적용됩니다.");
				} else {
					Bukkit.broadcastMessage("§4배고픔 무제한§c이 적용되지 않습니다.");
				}

				if (Configuration.Settings.getInfiniteDurability()) {
					addModule(new InfiniteDurability());
				} else {
					Bukkit.broadcastMessage("§4내구도 무제한§c이 적용되지 않습니다.");
				}

				if (Configuration.Settings.getClearWeather()) {
					for (World w : Bukkit.getWorlds()) {
						w.setStorm(false);
					}
				}

				if (invincible) {
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
	
	private static List<String> getBlindInfo(Mix mix) {
		final List<String> info = new ArrayList<>();
		info.add(Formatter.formatTitle(32, ChatColor.GREEN, ChatColor.YELLOW, "능력 정보"));
		info.add("§b믹스 §f[" + (mix.isRestricted() ? "§7능력 비활성화됨" : "§a능력 활성화됨") + "§f] §cSPECIAL 등급 §8기타");
		info.add("§a---------------------------------");
		if (mix.hasSynergy()) {
			final Synergy synergy = mix.getSynergy();
			final Pair<AbilityRegistration, AbilityRegistration> synergyBase = SynergyFactory.getSynergyBase(synergy.getRegistration());
			info.add("§f시너지: §a" + HyperChaosGUI.getBlindedName(synergyBase.getLeft()) + "§r §f+ §a" + HyperChaosGUI.getBlindedName(synergyBase.getRight()) + "§r");
			info.add("§a---------------------------------");
			info.add("§b" + HyperChaosGUI.getBlindedName(synergy.getRegistration()) + "§r " + (mix.getSynergy().isRestricted() ? "§f[§7능력 비활성화됨§f]" : "§f[§a능력 활성화됨§f]") + " §8§k---§r §7§kBLIND§r");
			info.add("§kBLINDBLINDBLINDBLINDBLINDBLIND");
			info.add("§a---------------------------------");
		} else {
			info.addAll(getBlindInfo(mix.getFirst()));
			info.addAll(getBlindInfo(mix.getSecond()));
		}
		return info;
	}

	private static List<String> getBlindInfo(AbilityBase ability) {
		if (ability != null) {
			return Arrays.asList(
					"§b" + HyperChaosGUI.getBlindedName(ability.getRegistration()) + "§r " + (ability.isRestricted() ? "§f[§7능력 비활성화됨§f]" : "§f[§a능력 활성화됨§f]") + " §8§k---§r §7§kBLIND§r",
					"§kBLINDBLINDBLINDBLINDBLINDBLIND",
					"§a---------------------------------"
			);
		} else {
			return Collections.singletonList("§f능력이 없습니다.");
		}
	}
	
	@Override
	public void executeCommand(CommandType commandType, CommandSender sender, String command, String[] args, Plugin plugin) {
		switch (commandType) {
			case ABILITY_CHECK: {
				final Player player = (Player) sender;
				if (GameManager.isGameRunning()) {
					final AbstractGame game = GameManager.getGame();
					if (game.isParticipating(player)) {
						final MixParticipant participant = getParticipant(player);
						for (String line : getBlindInfo(participant.getAbility())) {
							player.sendMessage(line);
						}
					} else {
						Messager.sendErrorMessage(sender, "게임에 참가하고 있지 않습니다.");
					}
				} else {
					Messager.sendErrorMessage(sender, "게임이 진행되고 있지 않습니다.");
				}
			}
			break;
			case ABILITY_SUMMARIZE: {
				if (GameManager.isGameRunning()) {
					Messager.sendErrorMessage(sender, "이 게임에서 사용할 수 없는 명령어입니다.");
				} else Messager.sendErrorMessage(sender, "게임이 진행되고 있지 않습니다.");
			}
			break;
			case TIP_CHECK: {
				final Player player = (Player) sender;
				if (GameManager.isGameRunning()) {
					final AbstractGame game = GameManager.getGame();
					if (game.isParticipating(player)) {
						Messager.sendErrorMessage(sender, "이 게임에서 사용할 수 없는 명령어입니다. §8(§7/aw abtip <능력> 명령어를 사용하세요.§8)");
					} else {
						Messager.sendErrorMessage(sender, "게임에 참가하고 있지 않습니다. §8(§7/aw abtip <능력> 명령어를 사용하세요.§8)");
					}
				} else {
					Messager.sendErrorMessage(sender, "게임이 진행되고 있지 않습니다. §8(§7/aw abtip <능력> 명령어를 사용하세요.§8)");
				}
			}
			break;
			default: {
				super.executeCommand(commandType, sender, command, args, plugin);
			}
			break;
		}
	}

	public AbilitySelect newAbilitySelect() {
		return new AbilitySelect(this, getParticipants(), 1) {
			
			private Map<Participant, HyperChaosGUI> guilist = new HashMap<>();
			
			@Override
			protected boolean changeAbility(Participant arg0) {
				return false;
			}

			@Override
			protected void drawAbility(Collection<? extends Participant> selectors) {
				for (Participant participant : selectors) {
					guilist.put(participant, new HyperChaosGUI((MixParticipant) participant, HyperChaosGame.this, CHANGE.getValue(), SYNERGY_CHANGE.getValue(), AbilityWar.getPlugin()));
				}	
			}
			
			@Override
			public void run(int count) {
				int stack = 0;
				for (Participant participant : guilist.keySet()) {
					if (guilist.get(participant).decide) stack++;
				}
				if (stack == guilist.size()) stop(false);
			}
			
			@Override
			protected void onSkip(final String admin) {
				Bukkit.broadcastMessage(
						admin != null ? (
								"§f관리자 §e" + admin + "§f" + KoreanUtil.getJosa(admin.replaceAll("_", ""), Josa.이가) + " 모든 참가자의 능력을 강제로 확정했습니다."
						) : "§e모든 참가자§f의 능력이 강제로 확정되었습니다."
				);
				for (Participant participant : guilist.keySet()) {
					guilist.get(participant).skip();
				}
			}
			
			@Override
			protected void onChange(final Participant participant) {
			}
			
			@Override
			protected void onDecision(final Participant participant) {
			}

		};
	}
	
}