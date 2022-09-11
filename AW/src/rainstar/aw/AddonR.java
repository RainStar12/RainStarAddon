package rainstar.aw;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import RainStarAbility.*;
import RainStarAbility.chronos.Chronos;
import RainStarAbility.theonering.v1_12_R1.TheOneRing;
import RainStarAbility.timestop.TimeStop;
import RainStarSynergy.*;
import RainStarSynergy.chance.Chance;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.list.Assassin;
import daybreak.abilitywar.ability.list.Berserker;
import daybreak.abilitywar.ability.list.Curse;
import daybreak.abilitywar.ability.list.Demigod;
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
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;

public class AddonR extends Addon implements Listener {
	
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
		AbilityFactory.registerAbility(LightningCounter.class);
		AbilityList.registerAbility(LightningCounter.class); //10
		
		AbilityFactory.registerAbility(Chronos.class);
		AbilityList.registerAbility(Chronos.class);
		AbilityFactory.registerAbility(Crystal.class);
		AbilityList.registerAbility(Crystal.class);
		AbilityFactory.registerAbility(MultiHit.class);
		AbilityList.registerAbility(MultiHit.class);
		AbilityFactory.registerAbility(NineTailFox.class);
		AbilityList.registerAbility(NineTailFox.class);
		AbilityFactory.registerAbility(NineTailFoxC.class);
		AbilityFactory.registerAbility(NineTailFoxCP.class);
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
		AbilityFactory.registerAbility(Tolerance.class);
		AbilityList.registerAbility(Tolerance.class);
		AbilityFactory.registerAbility(Medusa.class);
		AbilityList.registerAbility(Medusa.class);
		AbilityFactory.registerAbility(MadScientist.class);
		AbilityList.registerAbility(MadScientist.class);
		
		if (ServerVersion.getVersion() == 12) {
    		AbilityFactory.registerAbility(TheOneRing.class);
    		AbilityList.registerAbility(TheOneRing.class);
    	} else if (ServerVersion.getVersion() == 13) {
    		if (ServerVersion.getRelease() == 1) {
        		AbilityFactory.registerAbility(RainStarAbility.theonering.v1_13_R1.TheOneRing.class);
        		AbilityList.registerAbility(RainStarAbility.theonering.v1_13_R1.TheOneRing.class);	
    		} else {
        		AbilityFactory.registerAbility(RainStarAbility.theonering.v1_13_R2.TheOneRing.class);
        		AbilityList.registerAbility(RainStarAbility.theonering.v1_13_R2.TheOneRing.class);
    		}
    	} else if (ServerVersion.getVersion() == 14) {
    		AbilityFactory.registerAbility(RainStarAbility.theonering.v1_14_R1.TheOneRing.class);
    		AbilityList.registerAbility(RainStarAbility.theonering.v1_14_R1.TheOneRing.class);
    	} else if (ServerVersion.getVersion() == 15) {
    		AbilityFactory.registerAbility(RainStarAbility.theonering.v1_15_R1.TheOneRing.class);
    		AbilityList.registerAbility(RainStarAbility.theonering.v1_15_R1.TheOneRing.class);
    	} else if (ServerVersion.getVersion() == 16) {
    		if (ServerVersion.getRelease() == 1) {
        		AbilityFactory.registerAbility(RainStarAbility.theonering.v1_16_R1.TheOneRing.class);
        		AbilityList.registerAbility(RainStarAbility.theonering.v1_16_R1.TheOneRing.class);
    		} else if (ServerVersion.getRelease() == 2) {
        		AbilityFactory.registerAbility(RainStarAbility.theonering.v1_16_R2.TheOneRing.class);
        		AbilityList.registerAbility(RainStarAbility.theonering.v1_16_R2.TheOneRing.class);
    		} else {
        		AbilityFactory.registerAbility(RainStarAbility.theonering.v1_16_R3.TheOneRing.class);
        		AbilityList.registerAbility(RainStarAbility.theonering.v1_16_R3.TheOneRing.class);
    		}
    	} else if (ServerVersion.getVersion() == 17) {
    		AbilityFactory.registerAbility(RainStarAbility.theonering.v1_17_R1.TheOneRing.class);
    		AbilityList.registerAbility(RainStarAbility.theonering.v1_17_R1.TheOneRing.class);
    	}
		//70
		
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
		SynergyFactory.registerSynergy(DevilBoots.class, LightningCounter.class, GhostRider.class);
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
		
		new BukkitRunnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				try {
					SynergyFactory.registerSynergy(Empty.class, (Class<? extends AbilityBase>) ReflectionUtil.ClassUtil.forName("cokes86.addon.ability.list.DataMining"), AkashicRecords.class);
	            } catch (ClassNotFoundException e) {
	                e.printStackTrace();
	            }
				try {
					SynergyFactory.registerSynergy((Class<? extends AbilityBase>) ReflectionUtil.ClassUtil.forName("cokes86.addon.ability.list.Cokes"), Citrus.class, NaturalScienceStudent.class);
	            } catch (ClassNotFoundException e) {
	                e.printStackTrace();
	            }
				try {
					SynergyFactory.registerSynergy((Class<? extends AbilityBase>) ReflectionUtil.ClassUtil.forName("me.breakofday.yeomryo.abilities.Kaiji"), Moros.class, Sharper.class);
	            } catch (ClassNotFoundException e) {
	                e.printStackTrace();
	            }
	        }   
	    }.runTaskLater(AbilityWar.getPlugin(), 10L);
		
		Bukkit.broadcastMessage("§a레인스타 애드온§e이 적용되었습니다.");
		Bukkit.broadcastMessage("§e능력 §f82개 §7/ §d시너지 §f43개 적용 완료.");
		
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
    	
    	//beta
		AbilityFactory.registerAbility(Flex.class);
		AbilityList.registerAbility(Flex.class);
		AbilityFactory.registerAbility(KnockbackPatch.class);
		AbilityList.registerAbility(KnockbackPatch.class);
		
	}
	
	@EventHandler()
	public void onGameCredit(GameCreditEvent e) {
		e.addCredit("§a레인스타 애드온§f이 적용되었습니다. §e능력 §f82개 적용 완료.");
		if (e.getGame() instanceof AbstractMix) {
			e.addCredit("§d시너지 §f43개 적용 완료.");
		}
		e.addCredit("§a레인스타 애드온 §f개발자 : RainStar_ [§9디스코드 §f: RainStar§7#0846§f]");
	}

}