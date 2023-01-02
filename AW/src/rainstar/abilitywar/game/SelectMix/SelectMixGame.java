package rainstar.abilitywar.game.SelectMix;

import com.google.common.base.Strings;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.config.Configuration.Settings.InvincibilitySettings;
import daybreak.abilitywar.config.game.GameSettings.Setting;
import daybreak.abilitywar.game.GameAliases;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.manager.object.AbilitySelect;
import daybreak.abilitywar.game.module.InfiniteDurability;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.Seasons;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.library.SoundLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import javax.naming.OperationNotSupportedException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameManifest(name = "셀렉트 믹스 능력자 전쟁", description = {
		"§f자기가 원하는 능력을 골라서 하는 믹스!",
		"§f가진 패 중 최강의 조합을 뽑아보세요!",
		"",
		"§7/aw config games로 리롤 횟수를 조절 가능합니다."
})
@GameAliases({"셀믹전", "셀믹"})
public class SelectMixGame extends AbstractMix {
	
	private final boolean invincible = InvincibilitySettings.isEnabled();
	
	public static final Setting<Integer> CHANGE = gameSettings.new Setting<Integer>(SelectMixGame.class, "change-count", 1, "# 리롤 기회") {
		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
	};

	public SelectMixGame() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
	}

	@Override
	protected void progressGame(int seconds) {
		switch (seconds) {
			case 1:
				List<String> lines = Messager.asList("§2==== §a게임 참여자 목록 §2====");
				int count = 0;
				for (Participant p : getParticipants()) {
					count++;
					lines.add("§a" + count + ". §f" + p.getPlayer().getName());
				}
				lines.add("§2총 인원수 : " + count + "명");
				lines.add("§2==========================");

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
						"§2SelectMixAbility §f- §a셀렉트 믹스 능력자 전쟁",
						"§e버전 §7: §f" + AbilityWar.getPlugin().getDescription().getVersion(),
						"§b모드 개발자 §7: §fRainStar_ 레인스타",
						"§9디스코드 §7: §f레인스타§7#0846"
				);

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
					Bukkit.broadcastMessage("§f       §aSelectMixAbility §f- §2셀렉트 믹스 능력자 전쟁  ");
					Bukkit.broadcastMessage("§f                   게임 시작                ");
					Bukkit.broadcastMessage(blocks);
				} else {
					for (String line : Messager.asList(
							"§a■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■",
							"§f       §aSelectMixAbility §f- §2셀렉트 믹스 능력자 전쟁  ",
							"§f                    게임 시작                ",
							"§a■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■")) {
						Bukkit.broadcastMessage(line);
					}
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

	public AbilitySelect newAbilitySelect() {
		return new AbilitySelect(this, getParticipants(), 1) {
			
			private Map<Participant, SelectMixGUI> guilist = new HashMap<>();
			
			@Override
			protected boolean changeAbility(Participant arg0) {
				return false;
			}

			@Override
			protected void drawAbility(Collection<? extends Participant> selectors) {
				for (Participant participant : selectors) {
					guilist.put(participant, new SelectMixGUI((MixParticipant) participant, SelectMixGame.this, CHANGE.getValue(), AbilityWar.getPlugin()));
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