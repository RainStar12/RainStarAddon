package rainstar.aw;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import RainStarAbility.Alice;
import RainStarAbility.AntiGravity;
import RainStarAbility.Butcher;
import RainStarAbility.Butterfly;
import RainStarAbility.Citrus;
import RainStarAbility.Crystal;
import RainStarAbility.Dash;
import RainStarAbility.Detection;
import RainStarAbility.Dinosaur;
import RainStarAbility.Divinity;
import RainStarAbility.Echo;
import RainStarAbility.Empty;
import RainStarAbility.Executioner;
import RainStarAbility.Flex;
import RainStarAbility.Fool;
import RainStarAbility.FoxCrystalBall;
import RainStarAbility.GlassCannon;
import RainStarAbility.GuardianAngel;
import RainStarAbility.HealthFreak;
import RainStarAbility.HuntingDog;
import RainStarAbility.KnockbackPatch;
import RainStarAbility.Indecision;
import RainStarAbility.Inferno;
import RainStarAbility.IslandRabbit;
import RainStarAbility.Kairos;
import RainStarAbility.Kuro;
import RainStarAbility.KuroEye;
import RainStarAbility.LightningCounter;
import RainStarAbility.LingeringArrow;
import RainStarAbility.LittleDevil;
import RainStarAbility.Megalodon;
import RainStarAbility.Minotauros;
import RainStarAbility.Mira;
import RainStarAbility.Moros;
import RainStarAbility.MultiHit;
import RainStarAbility.NineTailFox;
import RainStarAbility.NineTailFoxC;
import RainStarAbility.NineTailFoxCP;
import RainStarAbility.OneShotOneKill;
import RainStarAbility.Panda;
import RainStarAbility.Penguin;
import RainStarAbility.Phoenix;
import RainStarAbility.PrecisionAiming;
import RainStarAbility.Protagonist;
import RainStarAbility.RainStar;
import RainStarAbility.RandomNumberControl;
import RainStarAbility.Revenger;
import RainStarAbility.SkyWhale;
import RainStarAbility.Soda;
import RainStarAbility.Squirrel;
import RainStarAbility.Stella;
import RainStarAbility.Stop;
import RainStarAbility.Teabagging;
import RainStarAbility.Tesla;
import RainStarAbility.TimeStop;
import RainStarAbility.HeadtoHead;
import RainStarAbility.Yuki;
import RainStarAbility.chronos.Chronos;
import RainStarSynergy.ASAP;
import RainStarSynergy.Abyss;
import RainStarSynergy.Accelerator;
import RainStarSynergy.AkashicRecords;
import RainStarSynergy.AliceInFreezer;
import RainStarSynergy.BadManner;
import RainStarSynergy.Berserk;
import RainStarSynergy.Cliche;
import RainStarSynergy.CowardlyMatch;
import RainStarSynergy.Crow;
import RainStarSynergy.CurseOfGod;
import RainStarSynergy.Demisoda;
import RainStarSynergy.DemonLord;
import RainStarSynergy.DeusExMachina;
import RainStarSynergy.Fairy;
import RainStarSynergy.FingerExersize;
import RainStarSynergy.GhostRider;
import RainStarSynergy.Gravity;
import RainStarSynergy.HawkEye;
import RainStarSynergy.HealthCopy;
import RainStarSynergy.HomingPenetrationArrow;
import RainStarSynergy.HumanBaseball;
import RainStarSynergy.Infallibility;
import RainStarSynergy.Joker;
import RainStarSynergy.LaplaceDemon;
import RainStarSynergy.Mirror;
import RainStarSynergy.MomentaryTrip;
import RainStarSynergy.MovingSkill;
import RainStarSynergy.NaturalScienceStudent;
import RainStarSynergy.NineTailFoxSynergy;
import RainStarSynergy.PatronSaint;
import RainStarSynergy.Recall;
import RainStarSynergy.Sharper;
import RainStarSynergy.SocialDistancing;
import RainStarSynergy.TeslaPlasma;
import RainStarSynergy.TimeBomb;
import RainStarSynergy.TimeTravel;
import RainStarSynergy.Wadadadadada;
import RainStarSynergy.YukiSnow;
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
import daybreak.abilitywar.ability.list.EnergyBlocker;
import daybreak.abilitywar.ability.list.Ferda;
import daybreak.abilitywar.ability.list.Flector;
import daybreak.abilitywar.ability.list.Loki;
import daybreak.abilitywar.ability.list.Magician;
import daybreak.abilitywar.ability.list.PenetrationArrow;
import daybreak.abilitywar.ability.list.Reverse;
import daybreak.abilitywar.ability.list.Ruber;
import daybreak.abilitywar.ability.list.Sniper;
import daybreak.abilitywar.ability.list.SurvivalInstinct;
import daybreak.abilitywar.ability.list.Terrorist;
import daybreak.abilitywar.ability.list.TimeRewind;
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
		AbilityList.registerAbility(LightningCounter.class);
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
		AbilityFactory.registerAbility(Flex.class);
		AbilityList.registerAbility(Flex.class);
		AbilityFactory.registerAbility(Dash.class);
		AbilityList.registerAbility(Dash.class);
		AbilityFactory.registerAbility(Revenger.class);
		AbilityList.registerAbility(Revenger.class);
		AbilityFactory.registerAbility(Kairos.class);
		AbilityList.registerAbility(Kairos.class);
		AbilityFactory.registerAbility(LittleDevil.class);
		AbilityList.registerAbility(LittleDevil.class);
		AbilityFactory.registerAbility(Mira.class);
		AbilityList.registerAbility(Mira.class);
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
		AbilityList.registerAbility(Teabagging.class);
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
		AbilityList.registerAbility(HeadtoHead.class);
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
		AbilityList.registerAbility(FoxCrystalBall.class);
		AbilityFactory.registerAbility(SkyWhale.class);
		AbilityList.registerAbility(SkyWhale.class);
		AbilityFactory.registerAbility(Fool.class);
		AbilityList.registerAbility(Fool.class);
		AbilityFactory.registerAbility(Minotauros.class);
		AbilityList.registerAbility(Minotauros.class);
		
		AbilityFactory.registerAbility(KnockbackPatch.class);
		AbilityList.registerAbility(KnockbackPatch.class);
		
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
		
		new BukkitRunnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				try {
					SynergyFactory.registerSynergy(Empty.class, (Class<? extends AbilityBase>) ReflectionUtil.ClassUtil.forName("cokes86.addon.ability.list.DataMining"), AkashicRecords.class);
					SynergyFactory.registerSynergy((Class<? extends AbilityBase>) ReflectionUtil.ClassUtil.forName("cokes86.addon.ability.list.Cokes"), Citrus.class, NaturalScienceStudent.class);
					SynergyFactory.registerSynergy((Class<? extends AbilityBase>) ReflectionUtil.ClassUtil.forName("me.breakofday.yeomryo.abilities.Kaiji"), Moros.class, Sharper.class);
	            } catch (ClassNotFoundException e) {
	                e.printStackTrace();
	            }
	        }   
	    }.runTaskLater(AbilityWar.getPlugin(), 10L);
		
		Bukkit.broadcastMessage("§a레인스타 애드온§e이 적용되었습니다.");
		Bukkit.broadcastMessage("§e능력 §f53개 §7/ §d시너지 §f40개 적용 완료.");
		
		Bukkit.getPluginManager().registerEvents(this, getPlugin());
		
        RainStarEffectFactory.load();
        
    	
    	if (ServerVersion.getVersion() == 12) {
    		SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_12_R1.Clown.class, Alice.class, Joker.class);
    	} else if (ServerVersion.getVersion() == 13) {
    		SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_13_R1.Clown.class, Alice.class, Joker.class);
    	} else if (ServerVersion.getVersion() == 14) {
    		SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_14_R1.Clown.class, Alice.class, Joker.class);
    	} else if (ServerVersion.getVersion() == 15) {
    		SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_15_R1.Clown.class, Alice.class, Joker.class);
    	} else if (ServerVersion.getVersion() == 16) {
    		SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_16_R1.Clown.class, Alice.class, Joker.class);
    	} else if (ServerVersion.getVersion() == 17) {
    		SynergyFactory.registerSynergy(daybreak.abilitywar.ability.list.clown.v1_17_R1.Clown.class, Alice.class, Joker.class);
    	}
		
	}
	
	@EventHandler()
	public void onGameCredit(GameCreditEvent e) {
		e.addCredit("§a레인스타 애드온§f이 적용되었습니다. §e능력 §f53개 적용 완료.");
		if (e.getGame() instanceof AbstractMix) {
			e.addCredit("§d시너지 §f40개 적용 완료.");
		}
		e.addCredit("§a레인스타 애드온 §f개발자 : RainStar_ [§9디스코드 §f: RainStar§7#0846§f]");
	}

}