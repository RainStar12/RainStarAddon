package rainstar.abilitywar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.Command;
import daybreak.abilitywar.Command.Condition;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.list.Assassin;
import daybreak.abilitywar.ability.list.Berserker;
import daybreak.abilitywar.ability.list.Curse;
import daybreak.abilitywar.ability.list.Demigod;
import daybreak.abilitywar.ability.list.Developer;
import daybreak.abilitywar.ability.list.DevilBoots;
import daybreak.abilitywar.ability.list.DiceGod;
import daybreak.abilitywar.ability.list.Emperor;
import daybreak.abilitywar.ability.list.EnergyBlocker;
import daybreak.abilitywar.ability.list.Ferda;
import daybreak.abilitywar.ability.list.Flector;
import daybreak.abilitywar.ability.list.Loki;
import daybreak.abilitywar.ability.list.Lunar;
import daybreak.abilitywar.ability.list.Muse;
import daybreak.abilitywar.ability.list.PenetrationArrow;
import daybreak.abilitywar.ability.list.Reverse;
import daybreak.abilitywar.ability.list.Ruber;
import daybreak.abilitywar.ability.list.Sniper;
import daybreak.abilitywar.ability.list.SurvivalInstinct;
import daybreak.abilitywar.ability.list.Terrorist;
import daybreak.abilitywar.ability.list.TimeRewind;
import daybreak.abilitywar.ability.list.Vampire;
import daybreak.abilitywar.ability.list.VictoryBySword;
import daybreak.abilitywar.ability.list.Void;
import daybreak.abilitywar.addon.Addon;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.event.GameEndEvent;
import daybreak.abilitywar.game.event.GameStartEvent;
import daybreak.abilitywar.game.event.participant.ParticipantDeathEvent;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.GameFactory;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.io.FileUtil;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;
import rainstar.abilitywar.ability.*;
import rainstar.abilitywar.ability.beta.DamageTester;
import rainstar.abilitywar.ability.beta.KnockbackPatch;
import rainstar.abilitywar.ability.beta.SpeedTester;
import rainstar.abilitywar.ability.chronos.Chronos;
import rainstar.abilitywar.ability.killerbunny.KillerBunny;
import rainstar.abilitywar.ability.timestop.TimeStop;
import rainstar.abilitywar.game.NoDelay;
import rainstar.abilitywar.game.RandomGame;
import rainstar.abilitywar.game.Chaos.GachaGame;
import rainstar.abilitywar.game.Chaos.KingsGambitGame;
import rainstar.abilitywar.game.Chaos.RockbottomGame;
import rainstar.abilitywar.game.Chaos.Overlap.Overlap;
import rainstar.abilitywar.game.Chaos.Overlap.OverlapGame;
import rainstar.abilitywar.game.SelectMix.NullAbility;
import rainstar.abilitywar.game.SelectMix.SelectMixGame;
import rainstar.abilitywar.synergy.*;
import rainstar.abilitywar.synergy.chance.Chance;
import rainstar.abilitywar.system.killreward.KillRewardGUI;
import rainstar.abilitywar.system.killreward.KillRewardSelectGUI;
import rainstar.abilitywar.utils.RankColor;

public class RainStarAddon extends Addon implements Listener {
	
	private File nodelay = new File("plugins/AbilityWar/nodelay.txt");
	private File hy = new File("plugins/AbilityWar/hy.txt");
	
	private final YamlConfiguration winconfig = YamlConfiguration.loadConfiguration(FileUtil.newFile("wincount.txt"));
	private final YamlConfiguration killconfig = YamlConfiguration.loadConfiguration(FileUtil.newFile("killcount.txt"));
	
	private Map<UUID, Integer> nowkillcount = new HashMap<>();
	private Random random = new Random();
	
	@Override
	public void onEnable() {
		AbilityFactory.registerAbility(PrecisionAiming.class);
		AbilityList.registerAbility(PrecisionAiming.class);
		AbilityFactory.registerAbility(Butcher.class);
		AbilityList.registerAbility(Butcher.class);
		AbilityFactory.registerAbility(Indecision.class);
		AbilityList.registerAbility(Indecision.class);
		AbilityFactory.registerAbility(GlassCannon.class);
		AbilityList.registerAbility(GlassCannon.class);
		AbilityFactory.registerAbility(Detection.class);
		AbilityList.registerAbility(Detection.class);
		AbilityFactory.registerAbility(Empty.class);
		AbilityList.registerAbility(Empty.class);
		AbilityFactory.registerAbility(LingeringArrow.class);
		AbilityList.registerAbility(LingeringArrow.class);
		AbilityFactory.registerAbility(TimeStop.class);
		AbilityList.registerAbility(TimeStop.class);
		AbilityFactory.registerAbility(AntiGravity.class);
		AbilityList.registerAbility(AntiGravity.class);
		AbilityFactory.registerAbility(Accel.class);
		AbilityList.registerAbility(Accel.class); //10
		
		AbilityFactory.registerAbility(Chronos.class);
		AbilityList.registerAbility(Chronos.class);
		AbilityFactory.registerAbility(Crystal.class);
		AbilityList.registerAbility(Crystal.class);
		AbilityFactory.registerAbility(MultiHit.class);
		AbilityList.registerAbility(MultiHit.class);
		AbilityFactory.registerAbility(NineTailFox.class);
		AbilityList.registerAbility(NineTailFox.class);
		AbilityFactory.registerAbility(NineTailFoxC.class);
		AbilityFactory.registerAbility(Echo.class);
		AbilityList.registerAbility(Echo.class);
		AbilityFactory.registerAbility(Dash.class);
		AbilityList.registerAbility(Dash.class);
		AbilityFactory.registerAbility(Revenger.class);
		AbilityList.registerAbility(Revenger.class);
		AbilityFactory.registerAbility(Kairos.class);
		AbilityList.registerAbility(Kairos.class);
		AbilityFactory.registerAbility(LittleDevil.class);
		AbilityList.registerAbility(LittleDevil.class);
		AbilityFactory.registerAbility(Mira.class);
		AbilityList.registerAbility(Mira.class); //20
		
		AbilityFactory.registerAbility(GuardianAngel.class);
		AbilityList.registerAbility(GuardianAngel.class);
		AbilityFactory.registerAbility(Stella.class);
		AbilityList.registerAbility(Stella.class);
		AbilityFactory.registerAbility(Moros.class);
		AbilityList.registerAbility(Moros.class);
		AbilityFactory.registerAbility(Yuki.class);
		AbilityList.registerAbility(Yuki.class);
		AbilityFactory.registerAbility(Executioner.class);
		AbilityList.registerAbility(Executioner.class);
		AbilityFactory.registerAbility(Tesla.class);
		AbilityList.registerAbility(Tesla.class);
		AbilityFactory.registerAbility(RainStar.class);
		AbilityList.registerAbility(RainStar.class);
		AbilityFactory.registerAbility(Soda.class);
		AbilityList.registerAbility(Soda.class);
		AbilityFactory.registerAbility(Alice.class);
		AbilityList.registerAbility(Alice.class);
		AbilityFactory.registerAbility(Teabagging.class);
		AbilityList.registerAbility(Teabagging.class); //30
		
		AbilityFactory.registerAbility(OneShotOneKill.class);
		AbilityList.registerAbility(OneShotOneKill.class);
		AbilityFactory.registerAbility(Kuro.class);
		AbilityList.registerAbility(Kuro.class);
		AbilityFactory.registerAbility(KuroEye.class);
		AbilityFactory.registerAbility(Divinity.class);
		AbilityList.registerAbility(Divinity.class);
		AbilityFactory.registerAbility(Protagonist.class);
		AbilityList.registerAbility(Protagonist.class);
		AbilityFactory.registerAbility(Stop.class);
		AbilityList.registerAbility(Stop.class);
		AbilityFactory.registerAbility(Inferno.class);
		AbilityList.registerAbility(Inferno.class);
		AbilityFactory.registerAbility(RandomNumberControl.class);
		AbilityList.registerAbility(RandomNumberControl.class);
		AbilityFactory.registerAbility(HealthFreak.class);
		AbilityList.registerAbility(HealthFreak.class);
		AbilityFactory.registerAbility(Citrus.class);
		AbilityList.registerAbility(Citrus.class);
		AbilityFactory.registerAbility(HeadtoHead.class);
		AbilityList.registerAbility(HeadtoHead.class); //40
		
		AbilityFactory.registerAbility(HuntingDog.class);
		AbilityList.registerAbility(HuntingDog.class);
		AbilityFactory.registerAbility(Phoenix.class);
		AbilityList.registerAbility(Phoenix.class);
		AbilityFactory.registerAbility(Penguin.class);
		AbilityList.registerAbility(Penguin.class);
		AbilityFactory.registerAbility(Megalodon.class);
		AbilityList.registerAbility(Megalodon.class);
		AbilityFactory.registerAbility(Butterfly.class);
		AbilityList.registerAbility(Butterfly.class);
		AbilityFactory.registerAbility(IslandRabbit.class);
		AbilityList.registerAbility(IslandRabbit.class);
		AbilityFactory.registerAbility(Dinosaur.class);
		AbilityList.registerAbility(Dinosaur.class);
		AbilityFactory.registerAbility(Panda.class);
		AbilityList.registerAbility(Panda.class);
		AbilityFactory.registerAbility(Squirrel.class);
		AbilityList.registerAbility(Squirrel.class);
		AbilityFactory.registerAbility(FoxCrystalBall.class);
		AbilityList.registerAbility(FoxCrystalBall.class); //50
		
		AbilityFactory.registerAbility(SkyWhale.class);
		AbilityList.registerAbility(SkyWhale.class);
		AbilityFactory.registerAbility(Fool.class);
		AbilityList.registerAbility(Fool.class);
		AbilityFactory.registerAbility(Minotauros.class);
		AbilityList.registerAbility(Minotauros.class);
		AbilityFactory.registerAbility(Air.class);
		AbilityList.registerAbility(Air.class);
		AbilityFactory.registerAbility(Tekkai.class);
		AbilityList.registerAbility(Tekkai.class);
		AbilityFactory.registerAbility(Delusion.class);
		AbilityList.registerAbility(Delusion.class);
		AbilityFactory.registerAbility(Damocles.class);
		AbilityList.registerAbility(Damocles.class);
		AbilityFactory.registerAbility(Devil.class);
		AbilityList.registerAbility(Devil.class);
		AbilityFactory.registerAbility(Fingun.class);
		AbilityList.registerAbility(Fingun.class);
		AbilityFactory.registerAbility(RussianRoulette.class);
		AbilityList.registerAbility(RussianRoulette.class); //60
		
		AbilityFactory.registerAbility(LuckSurvive.class);
		AbilityList.registerAbility(LuckSurvive.class);
		AbilityFactory.registerAbility(King.class);
		AbilityList.registerAbility(King.class);
		AbilityFactory.registerAbility(Daydream.class);
		AbilityList.registerAbility(Daydream.class);
		AbilityFactory.registerAbility(ForbiddenFruit.class);
		AbilityList.registerAbility(ForbiddenFruit.class);
		AbilityFactory.registerAbility(FirstAid.class);
		AbilityList.registerAbility(FirstAid.class);
		AbilityFactory.registerAbility(Nun.class);
		AbilityList.registerAbility(Nun.class);
		AbilityFactory.registerAbility(Vaccine.class);
		AbilityList.registerAbility(Vaccine.class);
		AbilityFactory.registerAbility(Medusa.class);
		AbilityList.registerAbility(Medusa.class);
		AbilityFactory.registerAbility(MadScientist.class);
		AbilityList.registerAbility(MadScientist.class);
		AbilityFactory.registerAbility("rainstar.abilitywar.ability.theonering." + ServerVersion.getName() + ".TheOneRing");
		AbilityList.registerAbility("rainstar.abilitywar.ability.theonering." + ServerVersion.getName() + ".TheOneRing"); //70
		
		AbilityFactory.registerAbility(Alte.class);
		AbilityList.registerAbility(Alte.class);
		AbilityFactory.registerAbility(Luciferium.class);
		AbilityList.registerAbility(Luciferium.class);
		AbilityFactory.registerAbility(Suguri.class);
		AbilityList.registerAbility(Suguri.class);
		AbilityFactory.registerAbility(Mazochist.class);
		AbilityList.registerAbility(Mazochist.class);
		AbilityFactory.registerAbility(TangerineJuice.class);
		AbilityList.registerAbility(TangerineJuice.class);
		AbilityFactory.registerAbility(Horus.class);
		AbilityList.registerAbility(Horus.class);
		AbilityFactory.registerAbility(Encore.class);
		AbilityList.registerAbility(Encore.class);
		AbilityFactory.registerAbility(Zero.class);
		AbilityList.registerAbility(Zero.class);
		AbilityFactory.registerAbility(Chatterbox.class);
		AbilityList.registerAbility(Chatterbox.class);
		AbilityFactory.registerAbility(Succubus.class);
		AbilityList.registerAbility(Succubus.class); //80
		
		AbilityFactory.registerAbility(GlitchedCrown.class);
		AbilityList.registerAbility(GlitchedCrown.class);
		AbilityFactory.registerAbility(Rival.class);
		AbilityList.registerAbility(Rival.class);
		AbilityFactory.registerAbility(PocketWatch.class);
		AbilityList.registerAbility(PocketWatch.class);
		AbilityFactory.registerAbility(Earthquake.class);
		AbilityList.registerAbility(Earthquake.class);
		AbilityFactory.registerAbility(Buff.class);
		AbilityList.registerAbility(Buff.class);
		AbilityFactory.registerAbility(Shadow.class);
		AbilityList.registerAbility(Shadow.class);
		AbilityFactory.registerAbility(Foresight.class);
		AbilityList.registerAbility(Foresight.class);
		AbilityFactory.registerAbility(X.class);
		AbilityList.registerAbility(X.class);
		AbilityFactory.registerAbility(Raincloud.class);
        AbilityList.registerAbility(Raincloud.class);
		AbilityFactory.registerAbility("rainstar.abilitywar.ability.silent." + ServerVersion.getName() + ".Silent");
		AbilityList.registerAbility("rainstar.abilitywar.ability.silent." + ServerVersion.getName() + ".Silent"); //90
		
		AbilityFactory.registerAbility(GreenGarden.class);
        AbilityList.registerAbility(GreenGarden.class);
		AbilityFactory.registerAbility(SoyMilk.class);
        AbilityList.registerAbility(SoyMilk.class);
		AbilityFactory.registerAbility(Hozosa.class);
        AbilityList.registerAbility(Hozosa.class);
		AbilityFactory.registerAbility(GreatCommunicator.class);
        AbilityList.registerAbility(GreatCommunicator.class);
		AbilityFactory.registerAbility(Kid.class);
        AbilityList.registerAbility(Kid.class);
		AbilityFactory.registerAbility(Pyromaniac.class);
        AbilityList.registerAbility(Pyromaniac.class);
		AbilityFactory.registerAbility(BullsEye.class);
        AbilityList.registerAbility(BullsEye.class);
		AbilityFactory.registerAbility(KillerBunny.class);
        AbilityList.registerAbility(KillerBunny.class);
        
        
        
    	//beta
		AbilityFactory.registerAbility(Flex.class);
		AbilityList.registerAbility(Flex.class);
		AbilityFactory.registerAbility(KnockbackPatch.class);
		AbilityList.registerAbility(KnockbackPatch.class);
		AbilityFactory.registerAbility(DamageTester.class);
		AbilityList.registerAbility(DamageTester.class);
		AbilityFactory.registerAbility(SpeedTester.class);
		AbilityList.registerAbility(SpeedTester.class);
		
		//event
		AbilityFactory.registerAbility(NullAbility.class);
        AbilityFactory.registerAbility(Overlap.class);
		
        
		
		SynergyFactory.registerSynergy(PrecisionAiming.class, Sniper.class, HawkEye.class);
		SynergyFactory.registerSynergy(AntiGravity.class, AntiGravity.class, Gravity.class);
		SynergyFactory.registerSynergy(TimeRewind.class, TimeStop.class, TimeTravel.class);
		SynergyFactory.registerSynergy(Dash.class, Dash.class, Accelerator.class);
		SynergyFactory.registerSynergy(EnergyBlocker.class, Indecision.class, SocialDistancing.class);
		SynergyFactory.registerSynergy(PrecisionAiming.class, PrecisionAiming.class, Infallibility.class);
		SynergyFactory.registerSynergy(Kairos.class, Kairos.class, Chance.class);
		SynergyFactory.registerSynergy(GlassCannon.class, VictoryBySword.class, ASAP.class);
		SynergyFactory.registerSynergy(GuardianAngel.class, GuardianAngel.class, PatronSaint.class);
		SynergyFactory.registerSynergy(PrecisionAiming.class, PenetrationArrow.class, HomingPenetrationArrow.class);
		SynergyFactory.registerSynergy(NineTailFox.class, NineTailFox.class, NineTailFoxSynergy.class);
		SynergyFactory.registerSynergy(MultiHit.class, MultiHit.class, Wadadadadada.class);
		SynergyFactory.registerSynergy(DiceGod.class, Moros.class, LaplaceDemon.class);
		SynergyFactory.registerSynergy(Terrorist.class, TimeStop.class, TimeBomb.class);
		SynergyFactory.registerSynergy(Void.class, Mira.class, MomentaryTrip.class);
		SynergyFactory.registerSynergy(Demigod.class, Soda.class, Demisoda.class);
		SynergyFactory.registerSynergy(Yuki.class, Yuki.class, YukiSnow.class);
		SynergyFactory.registerSynergy(Ruber.class, Crystal.class, HealthCopy.class);
		SynergyFactory.registerSynergy(Tesla.class, Tesla.class, TeslaPlasma.class);
		SynergyFactory.registerSynergy(Teabagging.class, LittleDevil.class, BadManner.class);
		SynergyFactory.registerSynergy(Dash.class, Divinity.class, MovingSkill.class);
		SynergyFactory.registerSynergy(Curse.class, Divinity.class, CurseOfGod.class);
		SynergyFactory.registerSynergy(DevilBoots.class, Accel.class, GhostRider.class);
		SynergyFactory.registerSynergy(KuroEye.class, KuroEye.class, DemonLord.class);
		SynergyFactory.registerSynergy(Ferda.class, Crystal.class, Fairy.class);
		SynergyFactory.registerSynergy(Kuro.class, Assassin.class, Crow.class);
		SynergyFactory.registerSynergy(Echo.class, Flector.class, Mirror.class);
		SynergyFactory.registerSynergy(Alice.class, Yuki.class, AliceInFreezer.class);
		SynergyFactory.registerSynergy(Berserker.class, Butcher.class, Berserk.class);
		SynergyFactory.registerSynergy(RandomNumberControl.class, RandomNumberControl.class, DeusExMachina.class);
		SynergyFactory.registerSynergy(Reverse.class, AntiGravity.class, Recall.class);
		SynergyFactory.registerSynergy(HealthFreak.class, Teabagging.class, FingerExersize.class);
		SynergyFactory.registerSynergy(Protagonist.class, SurvivalInstinct.class, Cliche.class);
		SynergyFactory.registerSynergy(Flector.class, Stop.class, HumanBaseball.class);
		SynergyFactory.registerSynergy(HeadtoHead.class, Loki.class, CowardlyMatch.class);
		SynergyFactory.registerSynergy(Empty.class, Empty.class, Abyss.class);	
		SynergyFactory.registerSynergy(Vampire.class, Executioner.class, VampireCount.class);
		SynergyFactory.registerSynergy(King.class, Emperor.class, Crown.class);
		SynergyFactory.registerSynergy(ForbiddenFruit.class, Muse.class, Eden.class);
		SynergyFactory.registerSynergy(Lunar.class, Stella.class, StarryNight.class);
		SynergyFactory.registerSynergy(Reverse.class, Mazochist.class, Sadism.class);
		SynergyFactory.registerSynergy(Developer.class, NullAbility.class, BugFix.class);
		
		new BukkitRunnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				try {
					SynergyFactory.registerSynergy(Empty.class, (Class<? extends AbilityBase>) ReflectionUtil.ClassUtil.forName("com.cokes86.cokesaddon.ability.list.DataMining"), AkashicRecords.class);
	            } catch (ClassNotFoundException e) {
	                e.printStackTrace();
	                Messager.sendConsoleMessage("§4[§c!§4] §e버그가 아닙니다! 코크스 애드온을 설치해야 이용 가능한 시너지입니다.");
	            }
				try {
					SynergyFactory.registerSynergy((Class<? extends AbilityBase>) ReflectionUtil.ClassUtil.forName("com.cokes86.cokesaddon.ability.list.Cokes"), Citrus.class, NaturalScienceStudent.class);
	            } catch (ClassNotFoundException e) {
	                e.printStackTrace();
	                Messager.sendConsoleMessage("§4[§c!§4] §e버그가 아닙니다! 코크스 애드온을 설치해야 이용 가능한 시너지입니다.");
	            }
				try {
					SynergyFactory.registerSynergy((Class<? extends AbilityBase>) ReflectionUtil.ClassUtil.forName("me.breakofday.yeomryo.abilities.Kaiji"), Moros.class, Sharper.class);
	            } catch (ClassNotFoundException e) {
	                e.printStackTrace();
	                Messager.sendConsoleMessage("§4[§c!§4] §e버그가 아닙니다! 염료 애드온을 설치해야 이용 가능한 시너지입니다.");
	            }
	        }   
	    }.runTaskLater(AbilityWar.getPlugin(), 10L);
	    
	    GameFactory.registerMode(SelectMixGame.class);
	    GameFactory.registerMode(RandomGame.class);
	    
	    //Chaos Game
	    GameFactory.registerMode(KingsGambitGame.class);
	    GameFactory.registerMode(RockbottomGame.class);
	    GameFactory.registerMode(GachaGame.class);
	    GameFactory.registerMode(OverlapGame.class);
	    
		Bukkit.broadcastMessage("§a레인스타 애드온§e이 적용되었습니다.");
		Bukkit.broadcastMessage("§e능력 §f98개 §7/ §d시너지 §f44개 적용 완료.");
		
		Bukkit.getPluginManager().registerEvents(this, getPlugin());
		
        RainStarEffectFactory.load();
        
    	if (ServerVersion.getVersion() == 12) {
    		SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_12_R1.Clown.class, Alice.class, Joker.class);
    	} else if (ServerVersion.getVersion() == 13) {
    		if (ServerVersion.getRelease() == 1) {
    			SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_13_R1.Clown.class, Alice.class, Joker.class);	
    		} else {
    			SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_13_R2.Clown.class, Alice.class, Joker.class);
    		}
    	} else if (ServerVersion.getVersion() == 14) {
    		SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_14_R1.Clown.class, Alice.class, Joker.class);
    	} else if (ServerVersion.getVersion() == 15) {
    		SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_15_R1.Clown.class, Alice.class, Joker.class);
    	} else if (ServerVersion.getVersion() == 16) {
    		if (ServerVersion.getRelease() == 1) {
    			SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_16_R1.Clown.class, Alice.class, Joker.class);
    		} else if (ServerVersion.getRelease() == 2) {
    			SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_16_R2.Clown.class, Alice.class, Joker.class);
    		} else {
    			SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_16_R3.Clown.class, Alice.class, Joker.class);
    		}
    	} else if (ServerVersion.getVersion() == 17) {
    		SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_17_R1.Clown.class, Alice.class, Joker.class);
    	}
		
		if (hy.exists()) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				int count = winconfig.get(player.getUniqueId().toString()) == null ? 0 : (int) winconfig.get(player.getUniqueId().toString());
				player.setPlayerListName(player.getGameMode().equals(GameMode.SPECTATOR) ? "§8§o[§7§o" + count + "§8§o] §7§o" + player.getName() : "§a[§e" + count + "§a] §f" + player.getName());
			}	
		}
		
		getPlugin().getCommands().getMainCommand().addSubCommand("delay", new Command(Condition.OP) {
			@Override
			protected boolean onCommand(CommandSender sender, String command, String[] args) {
				if (GameManager.isGameRunning()) {
					Messager.sendErrorMessage(sender, "게임이 진행되는 도중에는 변경할 수 없습니다.");
				} else {
					if (nodelay.exists()) {
						nodelay.delete();
						sender.sendMessage("§3[§b!§3] §f공격 쿨타임 제거 옵션을 §c비활성화§f합니다.");
					} else {
						try {
							nodelay.createNewFile();
							sender.sendMessage("§3[§b!§3] §f공격 쿨타임 제거 옵션을 §a활성화§f합니다.");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				return true;
			}
		});
		
		getPlugin().getCommands().getMainCommand().addSubCommand("hy", new Command(Condition.OP) {
			@Override
			protected boolean onCommand(CommandSender sender, String command, String[] args) {
				if (hy.exists()) {
					hy.delete();
					sender.sendMessage("§3[§b!§3] §f사망 메시지 옵션을 §c비활성화§f합니다.");
				} else {
					try {
						hy.createNewFile();
						sender.sendMessage("§3[§b!§3] §f사망 메시지 옵션을 §a활성화§f합니다.");
					} catch (IOException e) {
						e.printStackTrace();
					}
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
		
		getPlugin().getCommands().getMainCommand().addSubCommand("bc", new Command(Condition.OP) {
			@Override
			protected boolean onCommand(CommandSender sender, String command, String[] args) {
				final StringJoiner joiner = new StringJoiner("");
				for (String arg : args) {
					joiner.add(arg);
				}
				String message = "§3[§bAbilityWar§3] §a" + joiner.toString();
				Bukkit.broadcastMessage(message);
				return true;
			}
		});
		
		getPlugin().getCommands().getMainCommand().addSubCommand("win", new Command(Condition.OP) {
			@Override
			protected boolean onCommand(CommandSender sender, String command, String[] args) {
				if (!hy.exists()) {
					sender.sendMessage("§4[§c!§4] §f사망 메시지 옵션을 먼저 활성화해주세요. /aw hy");
				} else {
					if (args.length >= 2) {
						if (args[0].equals("add")) {
							if (Bukkit.getPlayer(args[1]).isOnline()) {
								Player player = Bukkit.getPlayer(args[1]);
								int count = (winconfig.get(player.getUniqueId().toString()) == null ? 0 : (int) winconfig.get(player.getUniqueId().toString())) + 1;
								winconfig.set(player.getUniqueId().toString(), count);
								player.setPlayerListName(player.getGameMode().equals(GameMode.SPECTATOR) ? "§8§o[§7§o" + count + "§8§o] §7§o" + player.getName() : "§a[§e" + count + "§a] §f" + player.getName());
								try {
									winconfig.save(FileUtil.newFile("wincount.txt"));
								} catch (IOException e) {
									e.printStackTrace();
								}
								sender.sendMessage("§a[§e!§a] §b" + player.getName()+ "§f님은 이제 §e" + count + "§f승입니다.");
							}
						}
						if (args[0].equals("del")) {
							if (Bukkit.getPlayer(args[1]).isOnline()) {
								Player player = Bukkit.getPlayer(args[1]);
								int count = (winconfig.get(player.getUniqueId().toString()) == null ? 0 : (int) winconfig.get(player.getUniqueId().toString())) - 1;
								winconfig.set(player.getUniqueId().toString(), count);
								player.setPlayerListName(player.getGameMode().equals(GameMode.SPECTATOR) ? "§8§o[§7§o" + count + "§8§o] §7§o" + player.getName() : "§a[§e" + count + "§a] §f" + player.getName());
								try {
									winconfig.save(FileUtil.newFile("wincount.txt"));
								} catch (IOException e) {
									e.printStackTrace();
								}
								sender.sendMessage("§a[§e!§a] §b" + player.getName()+ "§f님은 이제 §e" + count + "§f승입니다.");
							}
						}
					}	
					if (args[0].equals("list")) {
						Map<Player, Integer> winmap = new HashMap<>();
						for (String strings : winconfig.getKeys(false)) {
							Player player = Bukkit.getPlayer(UUID.fromString(strings));
							winmap.put(player, winconfig.getInt(strings));
						}
						List<Player> playerList = new ArrayList<>(winmap.keySet());
						int stack = 0;
						Collections.sort(playerList, (value1, value2) -> (winmap.get(value2).compareTo(winmap.get(value1))));
						sender.sendMessage("§a========= §e우승 TOP10 §a=========");
						for (Player player : playerList) {
							stack++;
							sender.sendMessage((stack <= 3 ? "§2§l" : "§2") + stack + ". §b" + player.getName() + " §a[§e" + winmap.get(player) + "§a]");
							if (stack == 10) break;
						}
						sender.sendMessage("§a=============================");
					}
				}
				return true;
			}
		});
		
		getPlugin().getCommands().getMainCommand().addSubCommand("kill", new Command(Condition.OP) {
			@Override
			protected boolean onCommand(CommandSender sender, String command, String[] args) {
				if (!hy.exists()) {
					sender.sendMessage("§4[§c!§4] §f사망 메시지 옵션을 먼저 활성화해주세요. /aw hy");
				} else {
					if (args.length >= 2) {
						if (args[0].equals("add")) {
							if (Bukkit.getPlayer(args[1]).isOnline()) {
								Player player = Bukkit.getPlayer(args[1]);
								int count = (killconfig.get(player.getUniqueId().toString()) == null ? 0 : (int) killconfig.get(player.getUniqueId().toString())) + 1;
								killconfig.set(player.getUniqueId().toString(), count);
								try {
									killconfig.save(FileUtil.newFile("killcount.txt"));
								} catch (IOException e) {
									e.printStackTrace();
								}
								sender.sendMessage("§4[§c!§4] §b" + player.getName()+ "§f님은 이제 전체 §c" + count + "§f킬입니다.");
							}
						}
						if (args[0].equals("del")) {
							if (Bukkit.getPlayer(args[1]).isOnline()) {
								Player player = Bukkit.getPlayer(args[1]);
								int count = (killconfig.get(player.getUniqueId().toString()) == null ? 0 : (int) killconfig.get(player.getUniqueId().toString())) - 1;
								killconfig.set(player.getUniqueId().toString(), count);
								try {
									killconfig.save(FileUtil.newFile("killcount.txt"));
								} catch (IOException e) {
									e.printStackTrace();
								}
								sender.sendMessage("§4[§c!§4] §b" + player.getName()+ "§f님은 이제 전체 §c" + count + "§f킬입니다.");
							}
						}
					}
					if (args[0].equals("list")) {
						Map<Player, Integer> killmap = new HashMap<>();
						for (String strings : killconfig.getKeys(false)) {
							Player player = Bukkit.getPlayer(UUID.fromString(strings));
							killmap.put(player, killconfig.getInt(strings));
						}
						List<Player> playerList = new ArrayList<>(killmap.keySet());
						int stack = 0;
						Collections.sort(playerList, (value1, value2) -> (killmap.get(value2).compareTo(killmap.get(value1))));
						sender.sendMessage("§5========= §c킬 TOP10 §5=========");
						for (Player player : playerList) {
							stack++;
							sender.sendMessage((stack <= 3 ? "§4§l" : "§4") + ". §e" + player.getName() + "§4[§c" + killmap.get(player) + "§4]");
							if (stack == 10) break;
						}
						sender.sendMessage("§5============================");
					}
				}
				return true;
			}
		});
	}
	
	@EventHandler()
	public void onChangeGameMode(PlayerGameModeChangeEvent e) {
		if (hy.exists()) {
			Player player = e.getPlayer();
			int count = winconfig.get(player.getUniqueId().toString()) == null ? 0 : (int) winconfig.get(player.getUniqueId().toString());
			player.setPlayerListName(player.getGameMode().equals(GameMode.SPECTATOR) ? "§8§o[§7§o" + count + "§8§o] §7§o" + player.getName() : "§a[§e" + count + "§a] §f" + player.getName());	
		}
	}
	
	@EventHandler()
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (hy.exists()) {
			Player player = e.getPlayer();
			int count = winconfig.get(player.getUniqueId().toString()) == null ? 0 : (int) winconfig.get(player.getUniqueId().toString());
			player.setPlayerListName(player.getGameMode().equals(GameMode.SPECTATOR) ? "§8§o[§7§o" + count + "§8§o] §7§o" + player.getName() : "§a[§e" + count + "§a] §f" + player.getName());	
		}
	}
	
	@EventHandler()
	public void onGameStart(GameStartEvent e) {
		if (nodelay.exists()) {
			Bukkit.broadcastMessage("§2[§a!§2] §c공격 쿨타임 제거!");
			e.getGame().addModule(new NoDelay(e.getGame()));
		}
	}
	
	@EventHandler()
	public void onGameEnd(GameEndEvent e) {
		if (!nowkillcount.isEmpty()) nowkillcount.clear();
	}
	
	@EventHandler()
	public void onParticipantDeath(ParticipantDeathEvent e) {
		if (hy.exists()) {
			String killerabilityname = "";
			String deathabilityname = "";
			if (e.getParticipant().getGame().getParticipant(e.getPlayer().getKiller()) != null) {
				if (e.getParticipant().getGame().getParticipant(e.getPlayer().getKiller()).hasAbility()) {
					AbilityBase ab = e.getParticipant().getGame().getParticipant(e.getPlayer().getKiller()).getAbility();
					if (ab.getClass().equals(Mix.class)) {
						Mix mix = (Mix) ab;
						if (mix.hasAbility()) {
							if (mix.hasSynergy()) {
								Synergy synergy = mix.getSynergy();
								Pair<AbilityRegistration, AbilityRegistration> base = SynergyFactory.getSynergyBase(synergy.getRegistration());
								killerabilityname = RankColor.getColor(synergy.getRank()) + synergy.getName() + "§8(§7" + base.getLeft().getManifest().name() + " + " + base.getRight().getManifest().name() + "§8)";
							} else {
								killerabilityname = RankColor.getColor(mix.getFirst().getRank()) + mix.getFirst().getName() + " §f+ " + RankColor.getColor(mix.getSecond().getRank()) + mix.getSecond().getName();
							}
						} else killerabilityname = "§7능력 없음";
					} else killerabilityname = RankColor.getColor(ab.getRank()) + ab.getDisplayName();
				} else killerabilityname = "§7능력 없음";
			}
			
			if (e.getParticipant().hasAbility()) {
				AbilityBase ab = e.getParticipant().getAbility();
				if (ab.getClass().equals(Mix.class)) {
					Mix mix = (Mix) ab;
					if (mix.hasAbility()) {
						if (mix.hasSynergy()) {
							Synergy synergy = mix.getSynergy();
							Pair<AbilityRegistration, AbilityRegistration> base = SynergyFactory.getSynergyBase(synergy.getRegistration());
							deathabilityname = RankColor.getColor(synergy.getRank()) + synergy.getName() + "§8(§7" + base.getLeft().getManifest().name() + " + " + base.getRight().getManifest().name() + "§8)";
						} else {
							deathabilityname = RankColor.getColor(mix.getFirst().getRank()) + mix.getFirst().getName() + " §f+ " + RankColor.getColor(mix.getSecond().getRank()) + mix.getSecond().getName();
						}
					} else deathabilityname = "§7능력 없음";
				} else deathabilityname = RankColor.getColor(ab.getRank()) + ab.getDisplayName();
			} else deathabilityname = "§7능력 없음";
			
			if (e.getPlayer().getKiller() != null) {
				Player killer = e.getPlayer().getKiller();
				nowkillcount.put(killer.getUniqueId(), nowkillcount.getOrDefault(killer.getUniqueId(), 0) + 1);
				int count = (killconfig.get(killer.getUniqueId().toString()) == null ? 0 : (int) killconfig.get(killer.getUniqueId().toString())) + 1;
				killconfig.set(killer.getUniqueId().toString(), count);
				try {
					killconfig.save(FileUtil.newFile("killcount.txt"));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				Bukkit.broadcastMessage("§f[§c능력§f] §c" + e.getPlayer().getName() + "§8[§7" + deathabilityname + "§8]§f님이 §a" + e.getPlayer().getKiller().getName() + "§8[§7" + killerabilityname + "§8]§f님에게 살해당했습니다. §c킬 수§7: §c(§e" + nowkillcount.get(killer.getUniqueId()) + "§7/§e" + count + "§c)");
			} else Bukkit.broadcastMessage("§f[§c능력§f] §c" + e.getPlayer().getName() + "§8[" + deathabilityname + "§8]§f님이 사망하였습니다.");
			int left = e.getParticipant().getGame().getParticipants().size();
			for (Participant participant : e.getParticipant().getGame().getParticipants()) {
				if ((e.getParticipant().getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) e.getParticipant().getGame()).getDeathManager().isExcluded(participant.getPlayer().getUniqueId()))) {
					left--;
				}
			}
			Bukkit.broadcastMessage("§4[§c!§4]§f 현재 참가자가 §c" + (left - 1) + "§f명 남았습니다.");
		}
		
		if (KillRewardGUI.status.equals(KillRewardGUI.Status.ENABLE)) {
			if (e.getPlayer().getKiller() != null) {
				Player killer = e.getPlayer().getKiller();
				if (KillRewardGUI.type.equals(KillRewardGUI.Type.ALL)) {
					killer.getInventory().addItem(KillRewardGUI.getItems().toArray(new ItemStack[0]));
				}
				if (KillRewardGUI.type.equals(KillRewardGUI.Type.RANDOM)) {
					killer.getInventory().addItem(KillRewardGUI.getItems().get(random.nextInt(KillRewardGUI.getItems().size())));
				}
				if (KillRewardGUI.type.equals(KillRewardGUI.Type.SELECT)) {
					new KillRewardSelectGUI(killer, AbilityWar.getPlugin());
				}	
			}
		}
	}
	
	@EventHandler()
	public void onGameCredit(GameCreditEvent e) {
		e.addCredit("§a레인스타 애드온§f이 적용되었습니다. §e능력 §f98개 적용 완료.");
		if (e.getGame() instanceof AbstractMix) {
			e.addCredit("§d시너지 §f44개 적용 완료.");
		}
		e.addCredit("§a레인스타 애드온 §f개발자 : RainStar_ [§9디스코드 §f: RainStar§7#0846§f]");
	}

}