package rainstar.abilitywar;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.Command;
import daybreak.abilitywar.Command.Condition;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.event.GameReadyEvent;
import daybreak.abilitywar.game.event.GameStartEvent;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.synergy.game.SynergyGame;
import daybreak.abilitywar.game.list.murdermystery.JobList;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.manager.GameFactory;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.TimeUtil.ParsedTime;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import rainstar.abilitywar.game.NoDelay;
import rainstar.abilitywar.game.RandomGame;
import rainstar.abilitywar.game.chaos.KingsGambitGame;
import rainstar.abilitywar.game.chaos.RockbottomGame;
import rainstar.abilitywar.game.chaos.hyperchaos.HyperChaosGame;
import rainstar.abilitywar.game.chaos.overlap.OverlapGame;
import rainstar.abilitywar.game.murder.system.JobListGUI;
import rainstar.abilitywar.game.murder.system.config.MurderWizard;
import rainstar.abilitywar.game.murder.system.config.statictype.CheckMurdererSword;
import rainstar.abilitywar.game.murder.system.config.statictype.DetectiveDetector;
import rainstar.abilitywar.game.murder.system.config.statictype.GhostChat;
import rainstar.abilitywar.game.murder.system.config.statictype.GhostJobs;
import rainstar.abilitywar.game.murder.system.config.statictype.MurderTeamChat;
import rainstar.abilitywar.game.murder.system.config.statictype.PreventReadyKill;
import rainstar.abilitywar.game.murder.system.config.variabletype.BlackModule;
import rainstar.abilitywar.game.murder.system.config.variabletype.DetectiveHoming;
import rainstar.abilitywar.game.murder.system.config.variabletype.DoubleMurderModule;
import rainstar.abilitywar.game.murder.system.config.variabletype.GoldRush;
import rainstar.abilitywar.game.murder.system.config.variabletype.HeadcountBalance;
import rainstar.abilitywar.game.murder.system.config.variabletype.MurderTimer;
import rainstar.abilitywar.game.murder.system.config.variabletype.NoKill;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveBuffer;
import rainstar.abilitywar.game.select.SelectGame;
import rainstar.abilitywar.game.selectmix.SelectMixGame;
import rainstar.abilitywar.system.command.CommandTabCompleter;
import rainstar.abilitywar.system.config.RainStarConfig;
import rainstar.abilitywar.system.config.RainStarConfig.ConfigOption;
import rainstar.abilitywar.system.killreward.KillRewardGUI;
import rainstar.abilitywar.system.killreward.KillRewardModule;

public class RainStarAddon extends Addon implements Listener {
	
	@Override
	public void onEnable() {
		RainStarAbilityRegister.registerAbility();
		RainStarAbilityRegister.registerBetaAbility();
		RainStarAbilityRegister.registerLiteAbility();
		RainStarAbilityRegister.registerMurderAbility();
		RainStarAbilityRegister.registerSpecialAbility();
		try {
			RainStarAbilityRegister.registerSynergy();
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		
		GameFactory.registerMode(SelectGame.class);
		GameFactory.registerMode(SelectMixGame.class);
		GameFactory.registerMode(RandomGame.class);
		
		//Chaos Game
		GameFactory.registerMode(KingsGambitGame.class);
		GameFactory.registerMode(RockbottomGame.class);
		GameFactory.registerMode(OverlapGame.class);
		GameFactory.registerMode(HyperChaosGame.class);
		
		KillRewardGUI.update();
		
		Bukkit.getPluginManager().registerEvents(this, getPlugin());
		
		RainStarEffectFactory.load();
		
    	//command
    	
		getPlugin().getCommands().getMainCommand().addSubCommand("delay", new Command(Condition.OP) {
			@Override
			protected boolean onCommand(CommandSender sender, String command, String[] args) {
				if (GameManager.isGameRunning()) {
					Messager.sendErrorMessage(sender, "게임이 진행되는 도중에는 변경할 수 없습니다.");
				} else {
					RainStarConfig.set(ConfigOption.NO_DELAY, !RainStarConfig.getBoolean(ConfigOption.NO_DELAY));
					sender.sendMessage("§3[§b!§3] §f공격 쿨타임 제거 옵션을 §c" + (RainStarConfig.getBoolean(ConfigOption.NO_DELAY) ? "" : "비") + "활성화§f합니다.");
				}
				return true;
			}
		});
		
		getPlugin().getCommands().getMainCommand().addSubCommand("killreward", new Command(Condition.OP) {
			@Override
			protected boolean onCommand(CommandSender sender, String command, String[] args) {
				if (sender instanceof Player) {
					Player opener = (Player) sender;
					new KillRewardGUI(opener, AbilityWar.getPlugin());
				}
				return true;
			}
		});
		
		getPlugin().getCommands().getMainCommand().addSubCommand("joblist", new Command(Condition.PLAYER) {
			@Override
			protected boolean onCommand(CommandSender sender, String command, String[] args) {
				if (args.length == 0) {
					if (sender instanceof Player) new JobListGUI((Player) sender, AbilityWar.getPlugin()).openGUI(1); else Messager.sendErrorMessage(sender, "사용법 §7: §f/" + command + " abilities <능력>");
				} else {
					final String name = String.join(" ", args);
					if (AbilityFactory.isRegistered(name) && (JobList.isRegistered(name) || AbilityFactory.isRegistered(name))) {
						for (String line : Formatter.formatInfo(AbilityFactory.getByName(name))) {
							sender.sendMessage(line);
						}
					} else Messager.sendErrorMessage(sender, name + KoreanUtil.getJosa(name, Josa.은는) + " 존재하지 않는 직업입니다.");
				}
				return true;
			}
		});
		
		Command murderCommand = new Command(Condition.OP) {

			@Override
			protected boolean onCommand(CommandSender sender, String command, String[] args) {
				if (args.length == 0) {
					sender.sendMessage("§4[§c!§4] §f사용법: /aw murder <config|time>");
					return true;
				}

				return false;
			}
		};

		murderCommand.addSubCommand("config", new Command(Condition.OP) {

			@Override
			protected boolean onCommand(CommandSender sender, String command, String[] args) {
				if (sender instanceof Player) new MurderWizard((Player) sender, getPlugin()).show();
				return true;
			}

		});

		murderCommand.addSubCommand("time", new Command(Condition.OP) {

			@Override
			protected boolean onCommand(CommandSender sender, String command, String[] args) {
				if (!GameManager.isGameRunning() || !GameManager.isGameOf(MurderMystery.class)) {
					sender.sendMessage("§4[§c!§4] §f현재 머더 게임이 진행 중이 아닙니다.");
					return true;
				}

				if (args.length < 2) {
					sender.sendMessage("§4[§c!§4] §f사용법: /aw murder time <add|set> <초>");
					return true;
				}

				MurderTimer timer = GameManager.getGame().getModule(MurderTimer.class);

				try {
					int value = Integer.parseInt(args[1]);

					switch (args[0].toLowerCase()) {
						case "add":
							timer.addcount(value * 20);
							break;

						case "set":
							timer.setcount(value * 20);
							break;

						default:
							sender.sendMessage("§4[§c!§4] §fadd 또는 set만 사용할 수 있습니다.");
							return true;
					}

					ParsedTime parsedTime = ParsedTime.parse(timer.getCount() / 20);

					sender.sendMessage(String.format(
							"§4[§c!§4] §f시간을 §c%d분 %d초§f로 조정하였습니다.",
							parsedTime.minutes,
							parsedTime.seconds
					));

				} catch (NumberFormatException e) {
					sender.sendMessage("§4[§c!§4] §f숫자를 입력해주세요.");
				}

				return true;
			}

		});

		getPlugin().getCommands().getMainCommand().addSubCommand("murder", murderCommand);
		
		Bukkit.getPluginManager().registerEvents(new CommandTabCompleter(), getPlugin());
		
		Bukkit.broadcastMessage("§a레인스타 애드온§8(§7v 2.0.0§8)§e이 적용되었습니다.");
		Bukkit.broadcastMessage("§e능력 §f122개 §7/ §d시너지 §f64개 적용 완료.");
	}
	
	@EventHandler()
	public void onGameStart(GameStartEvent e) {
		if (e.getGame() instanceof MurderMystery) {
			if (RainStarConfig.getBoolean(ConfigOption.MURDER_TIMER)) {
				int time = RainStarConfig.getInt(ConfigOption.MURDER_TIMER_TIME);
				if (RainStarConfig.getBoolean(ConfigOption.GOLD_RUSH)) {
					Bukkit.broadcastMessage("§2[§a!§2] §e§lG§a§lO§e§lL§a§lD §e§lR§a§lU§e§lS§a§lH§e§l!§a§l!§e§l!");
					e.getGame().addModule(new GoldRush(e.getGame(), time));
				}
				e.getGame().addModule(new MurderTimer(e.getGame(), time));
				e.getGame().addModule(new CheckMurdererSword(e.getGame(), (time * 20)));
				if (RainStarConfig.getBoolean(ConfigOption.NO_KILL)) {
					e.getGame().addModule(new NoKill(e.getGame(), RainStarConfig.getInt(ConfigOption.NO_KILL_TIME) * 20));
				}
			}
			e.getGame().addModule(new MurderTeamChat(e.getGame()));
			e.getGame().addModule(new DetectiveBuffer(e.getGame()));
			e.getGame().addModule(new GhostJobs());
			e.getGame().addModule(new GhostChat(e.getGame()));
			if (RainStarConfig.getBoolean(ConfigOption.BLACK_COMPASS)) {
				e.getGame().addModule(new BlackModule());
			}
			if (RainStarConfig.getBoolean(ConfigOption.DOUBLE_MURDER)) {
				Bukkit.broadcastMessage("§4[§c!§4] §5머더§f가 §c2배로 등장§f하고, §a시민§f의 §e금 획득량§f이 §e2배§f가 됩니다.");
			} else if (RainStarConfig.getBoolean(ConfigOption.HEAD_COUNT_BALANCE)) {
				e.getGame().addModule(new HeadcountBalance(e.getGame()));
			}
			Bukkit.broadcastMessage("§4[§c!§4] §c머더 팀§f은 채팅 맨 앞에 §c§l`§f를 붙여§8(§7쉬프트 없이 ~키§8)§c 머더 채팅§f을 이용할 수 있습니다.");
			Bukkit.broadcastMessage("§4[§c예시§4] §c`머더 누구임?");
		}
	}
	
	@EventHandler()
	public void onGameReady(GameReadyEvent e) {
		if (e.getGame() instanceof MurderMystery) {
			e.getGame().addModule(new DetectiveDetector(e.getGame()));
			
			if (RainStarConfig.getBoolean(ConfigOption.DETECTIVE_HOMING)) {
				e.getGame().addModule(new DetectiveHoming(e.getGame()));
				Bukkit.broadcastMessage("§4[§c!§4] §5탐정 유도 적용.");
			}
			
			if (RainStarConfig.getBoolean(ConfigOption.DOUBLE_MURDER)) {
				e.getGame().addModule(new DoubleMurderModule(e.getGame()));
				Bukkit.broadcastMessage("§4[§c!§4] §c더블 머더 적용.");
			}
			if (RainStarConfig.getBoolean(ConfigOption.MURDER_TIMER)) {
				if (RainStarConfig.getBoolean(ConfigOption.GOLD_RUSH)) Bukkit.broadcastMessage("§4[§c!§4] §e골드 러시 적용.");
				if (RainStarConfig.getBoolean(ConfigOption.NO_KILL)) e.getGame().addModule(new PreventReadyKill(e.getGame()));
			}
		} else {
			if (KillRewardGUI.status.equals(KillRewardGUI.Status.ENABLE)) {
				e.getGame().addModule(new KillRewardModule());
			}	
		}
		
		if (RainStarConfig.getBoolean(ConfigOption.NO_DELAY)) {
			Bukkit.broadcastMessage("§2[§a!§2] §b공격 쿨타임 제거!");
			e.getGame().addModule(new NoDelay(e.getGame()));
		}
	}
	
	public static File getAddonFile(String name) {
		return new File("plugins/AbilityWar/Addon/RainStarAddon/" + name);
	}
	
	@EventHandler()
	public void onGameCredit(GameCreditEvent e) {
		e.addCredit("§a레인스타 애드온§f이 적용되었습니다. §e능력 §f122개 적용 완료.");
		if (e.getGame() instanceof AbstractMix || e.getGame() instanceof SynergyGame) {
			e.addCredit("§d시너지 §f64개 적용 완료.");
		}
		e.addCredit("§a레인스타 애드온 §f개발자 : RainStar_ [§9디스코드 §f: §7@rainstar§f]");
	}

}