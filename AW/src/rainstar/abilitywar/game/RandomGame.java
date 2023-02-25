package rainstar.abilitywar.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.OperationNotSupportedException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Player;

import com.google.common.base.Strings;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.game.GameSettings.Setting;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.list.blind.BlindAbilityWar;
import daybreak.abilitywar.game.list.changeability.ChangeAbilityWar;
import daybreak.abilitywar.game.list.mix.MixGame;
import daybreak.abilitywar.game.list.mix.blind.MixBlindGame;
import daybreak.abilitywar.game.list.mix.changemix.ChangeMix;
import daybreak.abilitywar.game.list.mix.synergy.game.SynergyGame;
import daybreak.abilitywar.game.list.mix.triplemix.TripleMixGame;
import daybreak.abilitywar.game.list.oneability.OneAbility;
import daybreak.abilitywar.game.list.standard.StandardGame;
import daybreak.abilitywar.game.list.standard.WarGame;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.module.InfiniteDurability;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.Seasons;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.game.SelectMix.SelectMixGame;

@GameManifest(name = "랜덤", description = {
		"§f무슨 게임모드를 정할 지 고민되신다구요?",
		"§f그럴땐 랜덤이죠!",
		"",
		"§f게임모드별 전용 확률이 존재하고, WRECK도 같이 정해집니다.",
		"§f확률은 /aw config games를 통해 세부 조정 가능합니다."
		})
public class RandomGame extends Game {

	public static final Setting<Integer> NORMAL_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "game-normal-chance", 15, 
			"# 일반 게임모드 등장확률", "# 게임모드 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> BLIND_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "game-blind-chance", 5, 
			"# 블라인드 게임모드 등장확률", "# 게임모드 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> BLIND_MIX_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "game-blind-mix-chance", 5, 
			"# 블라인드 믹스 게임모드 등장확률", "# 게임모드 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> ONEABILITY_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "game-oneability-chance", 1, 
			"# 단일전 게임모드 등장확률", "# 게임모드 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> CHANGE_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "game-change-chance", 4, 
			"# 체인지 게임모드 등장확률", "# 게임모드 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> CHANGE_MIX_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "game-change-mix-chance", 4, 
			"# 체인지 믹스 게임모드 등장확률", "# 게임모드 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> MIX_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "game-mix-chance", 40, 
			"# 믹스 게임모드 등장확률", "# 게임모드 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> SELECT_MIX_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "game-select-mix-chance", 20, 
			"# 셀렉트 믹스 게임모드 등장확률", "# 게임모드 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> SYNERGY_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "game-synergy-chance", 5, 
			"# 시너지 게임모드 등장확률", "# 게임모드 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> TRIPLE_MIX_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "game-triple-mix-chance", 1, 
			"# 트리플 믹스 게임모드 등장확률", "# 게임모드 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	
	public static final Setting<Integer> WRECK_0_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "wreck-0", 2, 
			"# WRECK 0% 등장확률", "# WRECK 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> WRECK_25_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "wreck-25", 8, 
			"# WRECK 25% 등장확률", "# WRECK 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> WRECK_50_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "wreck-50", 40, 
			"# WRECK 50% 등장확률", "# WRECK 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> WRECK_75_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "wreck-75", 40, 
			"# WRECK 75% 등장확률", "# WRECK 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> WRECK_90_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "wreck-90", 8, 
			"# WRECK 90% 등장확률", "# WRECK 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> WRECK_100_CHANCE = gameSettings.new Setting<Integer>(RandomGame.class, "wreck-0", 2, 
			"# WRECK 100% 등장확률", "# WRECK 확률의 총합은 정확히 100이어야 합니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public RandomGame() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
		setRestricted(Settings.InvincibilitySettings.isEnabled());
	}
	
	private final Random random = new Random();
	private final Map<String, Class<? extends AbstractGame>> gamemodes = new HashMap<>();
	private final List<String> gamemoderesults = new ArrayList<>();
	

	@Override
	protected void progressGame(int seconds) {
		switch (seconds) {
		case 1:
			if (NORMAL_CHANCE.getValue() + BLIND_CHANCE.getValue() + BLIND_MIX_CHANCE.getValue() + ONEABILITY_CHANCE.getValue() + 
					CHANGE_CHANCE.getValue() + CHANGE_MIX_CHANCE.getValue() + MIX_CHANCE.getValue() + SELECT_MIX_CHANCE.getValue() +
					SYNERGY_CHANCE.getValue() + TRIPLE_MIX_CHANCE.getValue() != 100) {
				Bukkit.broadcastMessage("§4[§c!§4] §f게임모드 확률 총합이 100%가 아닙니다.");
				GameManager.stopGame();
				break;
			} else if (WRECK_0_CHANCE.getValue() + WRECK_25_CHANCE.getValue() + WRECK_50_CHANCE.getValue() + WRECK_75_CHANCE.getValue() + 
					WRECK_90_CHANCE.getValue() + WRECK_100_CHANCE.getValue() != 100) {
				Bukkit.broadcastMessage("§4[§c!§4] §fWRECK 확률 총합이 100%가 아닙니다.");
				GameManager.stopGame();
				break;
			} else {
				gamemodes.put("§b일반", WarGame.class);
				gamemodes.put("§7블라인드", BlindAbilityWar.class);
				gamemodes.put("§8블라인드 믹스", MixBlindGame.class);
				gamemodes.put("§3단일", OneAbility.class);
				gamemodes.put("§d체인지", ChangeAbilityWar.class);
				gamemodes.put("§c체인지 믹스", ChangeMix.class);
				gamemodes.put("§5믹스", MixGame.class);
				gamemodes.put("§a셀렉트 믹스", SelectMixGame.class);
				gamemodes.put("§6시너지", SynergyGame.class);
				gamemodes.put("§e트리플 믹스", TripleMixGame.class);
				
				for (int a = 0; a < NORMAL_CHANCE.getValue(); a++) {
					gamemoderesults.add("§b일반");
				}
				for (int a = 0; a < BLIND_CHANCE.getValue(); a++) {
					gamemoderesults.add("§7블라인드");
				}
				for (int a = 0; a < BLIND_MIX_CHANCE.getValue(); a++) {
					gamemoderesults.add("§8블라인드 믹스");
				}
				for (int a = 0; a < ONEABILITY_CHANCE.getValue(); a++) {
					gamemoderesults.add("§3단일");
				}
				for (int a = 0; a < CHANGE_CHANCE.getValue(); a++) {
					gamemoderesults.add("§d체인지");
				}
				for (int a = 0; a < CHANGE_MIX_CHANCE.getValue(); a++) {
					gamemoderesults.add("§c체인지 믹스");
				}
				for (int a = 0; a < MIX_CHANCE.getValue(); a++) {
					gamemoderesults.add("§5믹스");
				}
				for (int a = 0; a < SELECT_MIX_CHANCE.getValue(); a++) {
					gamemoderesults.add("§a셀렉트 믹스");
				}
				for (int a = 0; a < SYNERGY_CHANCE.getValue(); a++) {
					gamemoderesults.add("§6시너지");
				}
				for (int a = 0; a < TRIPLE_MIX_CHANCE.getValue(); a++) {
					gamemoderesults.add("§e트리플 믹스");
				}
				
				
				
				break;
			}
		case 2:
		case 4:
		case 6:
		case 8:
		case 11:
		case 14:
		case 17:
		case 20:
		case 24:
		case 28:
		case 33:
		case 38:
		case 45:
		case 55:
		case 65:
		case 80:
			String gamemodename = random.pick(gamemodes.keySet().toArray(new String[0]));
			for (Player player : Bukkit.getOnlinePlayers()) {
				NMS.sendTitle(player, gamemodename, "", 0, 100, 0);
				SoundLib.PIANO.playInstrument(player, Note.natural(0, Tone.C));
				SoundLib.PIANO.playInstrument(player, Note.natural(0, Tone.E));
			}
			break;
			
		case 100:
			
			break;
		}
			
	}

}