package rainstar.abilitywar;

import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory;
import daybreak.abilitywar.ability.list.Assassin;
import daybreak.abilitywar.ability.list.Berserker;
import daybreak.abilitywar.ability.list.Chaos;
import daybreak.abilitywar.ability.list.Curse;
import daybreak.abilitywar.ability.list.Demigod;
import daybreak.abilitywar.ability.list.Developer;
import daybreak.abilitywar.ability.list.DevilBoots;
import daybreak.abilitywar.ability.list.DiceGod;
import daybreak.abilitywar.ability.list.Emperor;
import daybreak.abilitywar.ability.list.EnergyBlocker;
import daybreak.abilitywar.ability.list.Ferda;
import daybreak.abilitywar.ability.list.Flector;
import daybreak.abilitywar.ability.list.Flora;
import daybreak.abilitywar.ability.list.Glacier;
import daybreak.abilitywar.ability.list.Loki;
import daybreak.abilitywar.ability.list.Lunar;
import daybreak.abilitywar.ability.list.Muse;
import daybreak.abilitywar.ability.list.PenetrationArrow;
import daybreak.abilitywar.ability.list.Reverse;
import daybreak.abilitywar.ability.list.Ruber;
import daybreak.abilitywar.ability.list.Sniper;
import daybreak.abilitywar.ability.list.SoulEncroach;
import daybreak.abilitywar.ability.list.SurvivalInstinct;
import daybreak.abilitywar.ability.list.Terrorist;
import daybreak.abilitywar.ability.list.Themis;
import daybreak.abilitywar.ability.list.TimeRewind;
import daybreak.abilitywar.ability.list.Vampire;
import daybreak.abilitywar.ability.list.VictoryBySword;
import daybreak.abilitywar.ability.list.Void;
import daybreak.abilitywar.ability.list.Zeus;
import daybreak.abilitywar.game.list.lite.LiteAbilities;
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.list.murdermystery.CharacterType;
import daybreak.abilitywar.game.list.murdermystery.JobList;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.reflect.ReflectionUtil;
import rainstar.abilitywar.ability.*;
import rainstar.abilitywar.ability.beta.KnockbackPatch;
import rainstar.abilitywar.ability.chronos.Chronos;
import rainstar.abilitywar.ability.empty.Empty;
import rainstar.abilitywar.ability.killerbunny.KillerBunny;
import rainstar.abilitywar.ability.timestop.TimeStop;
import rainstar.abilitywar.game.chaos.overlap.Overlap;
import rainstar.abilitywar.game.lite.ability.HomingArrow;
import rainstar.abilitywar.game.murder.innocent.Astrologer;
import rainstar.abilitywar.game.murder.innocent.Athlete;
import rainstar.abilitywar.game.murder.innocent.Bartender;
import rainstar.abilitywar.game.murder.innocent.BodyGuard;
import rainstar.abilitywar.game.murder.innocent.Clown;
import rainstar.abilitywar.game.murder.innocent.Explorer;
import rainstar.abilitywar.game.murder.innocent.Frog;
import rainstar.abilitywar.game.murder.innocent.Gambler;
import rainstar.abilitywar.game.murder.innocent.Ghost;
import rainstar.abilitywar.game.murder.innocent.GoodPerson;
import rainstar.abilitywar.game.murder.innocent.GraveRobber;
import rainstar.abilitywar.game.murder.innocent.Hacker;
import rainstar.abilitywar.game.murder.innocent.Hermit;
import rainstar.abilitywar.game.murder.innocent.Hoarder;
import rainstar.abilitywar.game.murder.innocent.Idol;
import rainstar.abilitywar.game.murder.innocent.Informant;
import rainstar.abilitywar.game.murder.innocent.Investigator;
import rainstar.abilitywar.game.murder.innocent.Mediumship;
import rainstar.abilitywar.game.murder.innocent.Official;
import rainstar.abilitywar.game.murder.innocent.Painter;
import rainstar.abilitywar.game.murder.innocent.Priest;
import rainstar.abilitywar.game.murder.innocent.Profiler;
import rainstar.abilitywar.game.murder.innocent.Psycher;
import rainstar.abilitywar.game.murder.innocent.Rich;
import rainstar.abilitywar.game.murder.innocent.Soldier;
import rainstar.abilitywar.game.murder.innocent.Streamer;
import rainstar.abilitywar.game.murder.innocent.Tailor;
import rainstar.abilitywar.game.murder.innocent.Tracker;
import rainstar.abilitywar.game.murder.innocent.Watcher;
import rainstar.abilitywar.game.murder.murderer.BigShotMurderer;
import rainstar.abilitywar.game.murder.murderer.CorruptionDetectiveMurderer;
import rainstar.abilitywar.game.murder.murderer.CultLeaderMurderer;
import rainstar.abilitywar.game.murder.murderer.DrugDealerMurderer;
import rainstar.abilitywar.game.murder.murderer.ExecutionerMurderer;
import rainstar.abilitywar.game.murder.murderer.FanaticMurderer;
import rainstar.abilitywar.game.murder.murderer.GrimReaperMurderer;
import rainstar.abilitywar.game.murder.murderer.HitmanMurderer;
import rainstar.abilitywar.game.murder.murderer.JackMurderer;
import rainstar.abilitywar.game.murder.murderer.JudasMurderer;
import rainstar.abilitywar.game.murder.murderer.MadScientistMurderer;
import rainstar.abilitywar.game.murder.murderer.PhantomThiefMurderer;
import rainstar.abilitywar.game.murder.murderer.PlagueDoctorMurderer;
import rainstar.abilitywar.game.murder.murderer.PsychopathMurderer;
import rainstar.abilitywar.game.murder.murderer.ScammerMurderer;
import rainstar.abilitywar.game.murder.murderer.SerialMurderer;
import rainstar.abilitywar.game.murder.murderer.SilentMurderer;
import rainstar.abilitywar.game.murder.murderer.SpecterMurderer;
import rainstar.abilitywar.game.murder.murderer.SpyMurderer;
import rainstar.abilitywar.game.murder.murderer.TerroristMurderer;
import rainstar.abilitywar.game.murder.murderer.VampireMurderer;
import rainstar.abilitywar.game.murder.murderer.WeaponRobotMurderer;
import rainstar.abilitywar.game.murder.murderer.WitchMurderer;
import rainstar.abilitywar.game.murder.murderer.ZombieMurderer;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveDisarmament;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveFastRegen;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveGlower;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveGoldDetect;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveGoldGiver;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveHomingUp;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveInfiniteRegen;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveIntuition;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveMonitoring;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveNonCharge;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectivePsychometry;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveResponsibility;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveShield;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveSpeedUp;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveStakeOut;
import rainstar.abilitywar.game.murder.system.detectivebuffer.DetectiveTaxCut;
import rainstar.abilitywar.game.selectmix.NullAbility;
import rainstar.abilitywar.synergy.ASAP;
import rainstar.abilitywar.synergy.Abyss;
import rainstar.abilitywar.synergy.Accelerator;
import rainstar.abilitywar.synergy.AkashicRecords;
import rainstar.abilitywar.synergy.AliceInFreezer;
import rainstar.abilitywar.synergy.BadManner;
import rainstar.abilitywar.synergy.Berserk;
import rainstar.abilitywar.synergy.BugFix;
import rainstar.abilitywar.synergy.Cliche;
import rainstar.abilitywar.synergy.CowardlyMatch;
import rainstar.abilitywar.synergy.Crow;
import rainstar.abilitywar.synergy.Crown;
import rainstar.abilitywar.synergy.CurseOfGod;
import rainstar.abilitywar.synergy.DarkMatter;
import rainstar.abilitywar.synergy.Demisoda;
import rainstar.abilitywar.synergy.DemonLord;
import rainstar.abilitywar.synergy.DeusExMachina;
import rainstar.abilitywar.synergy.EasyToEvade;
import rainstar.abilitywar.synergy.Eden;
import rainstar.abilitywar.synergy.Fairy;
import rainstar.abilitywar.synergy.FingerExersize;
import rainstar.abilitywar.synergy.Gardener;
import rainstar.abilitywar.synergy.GhostRider;
import rainstar.abilitywar.synergy.Gravity;
import rainstar.abilitywar.synergy.HawkEye;
import rainstar.abilitywar.synergy.HealthCopy;
import rainstar.abilitywar.synergy.HomingPenetrationArrow;
import rainstar.abilitywar.synergy.HumanBaseball;
import rainstar.abilitywar.synergy.Infallibility;
import rainstar.abilitywar.synergy.Joker;
import rainstar.abilitywar.synergy.Judgement;
import rainstar.abilitywar.synergy.Kamikaze;
import rainstar.abilitywar.synergy.KimiwaJitsuniBakadana;
import rainstar.abilitywar.synergy.LaplaceDemon;
import rainstar.abilitywar.synergy.LightningGod;
import rainstar.abilitywar.synergy.LucidDream;
import rainstar.abilitywar.synergy.MeteorShower;
import rainstar.abilitywar.synergy.Mirror;
import rainstar.abilitywar.synergy.MovingSkill;
import rainstar.abilitywar.synergy.Nameless;
import rainstar.abilitywar.synergy.NaturalScienceStudent;
import rainstar.abilitywar.synergy.NineTailFoxSynergy;
import rainstar.abilitywar.synergy.Pact;
import rainstar.abilitywar.synergy.PatronSaint;
import rainstar.abilitywar.synergy.Recall;
import rainstar.abilitywar.synergy.Ruberium;
import rainstar.abilitywar.synergy.Sadism;
import rainstar.abilitywar.synergy.SetsunaTrip;
import rainstar.abilitywar.synergy.Sharper;
import rainstar.abilitywar.synergy.Singularity;
import rainstar.abilitywar.synergy.Sneak;
import rainstar.abilitywar.synergy.SocialDistancing;
import rainstar.abilitywar.synergy.StarryNight;
import rainstar.abilitywar.synergy.SwordofDamocles;
import rainstar.abilitywar.synergy.TeslaPlasma;
import rainstar.abilitywar.synergy.TimeBomb;
import rainstar.abilitywar.synergy.TimeTravel;
import rainstar.abilitywar.synergy.VampireCount;
import rainstar.abilitywar.synergy.Wadadadadada;
import rainstar.abilitywar.synergy.WraithForm;
import rainstar.abilitywar.synergy.YukiSnow;
import rainstar.abilitywar.synergy.chance.Chance;
import rainstar.abilitywar.synergy.heatabnormal.HeatAbnormal;
import rainstar.abilitywar.synergy.normalization.Normalization;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBerserker;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBlackCandle;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBombArrow;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientBrewer;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientChaos;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientHacker;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientSniper;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientTheHighPriestess;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientVoid;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientYeti;
import rainstar.abilitywar.synergy.normalization.ancientability.AncientZombie;

public class RainStarAbilityRegister {
	
	public static void registerAbility() {
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
		AbilityList.registerAbility(RussianRoulette.class); //62
		
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
		AbilityFactory.registerAbility(MilkyWay.class);
		AbilityList.registerAbility(MilkyWay.class);
		AbilityFactory.registerAbility(Daybreak.class);
		AbilityList.registerAbility(Daybreak.class); //100
		
		AbilityFactory.registerAbility(Eternal.class);
		AbilityList.registerAbility(Eternal.class);
		AbilityFactory.registerAbility(Velocity.class);
		AbilityList.registerAbility(Velocity.class);
		AbilityFactory.registerAbility(Metronome.class);
		AbilityList.registerAbility(Metronome.class);
		AbilityFactory.registerAbility(Apparition.class);
		AbilityList.registerAbility(Apparition.class);
		AbilityFactory.registerAbility(Giant.class);
		AbilityList.registerAbility(Giant.class);
		AbilityFactory.registerAbility(Nyx.class);
		AbilityList.registerAbility(Nyx.class);
		AbilityFactory.registerAbility("rainstar.abilitywar.ability.mirage." + ServerVersion.getName() + ".Mirage");
		AbilityList.registerAbility("rainstar.abilitywar.ability.mirage." + ServerVersion.getName() + ".Mirage");
		AbilityFactory.registerAbility(Afterimage.class);
		AbilityList.registerAbility(Afterimage.class);
		AbilityFactory.registerAbility(Pillage.class);
		AbilityList.registerAbility(Pillage.class);
		AbilityFactory.registerAbility(Spark.class);
		AbilityList.registerAbility(Spark.class); //110
		
		AbilityFactory.registerAbility(Unknown.class);
		AbilityList.registerAbility(Unknown.class);
		AbilityFactory.registerAbility(WindReaver.class);
		AbilityList.registerAbility(WindReaver.class);
		AbilityFactory.registerAbility(Jelly.class);
		AbilityList.registerAbility(Jelly.class);
		AbilityFactory.registerAbility(Quasar.class);
		AbilityList.registerAbility(Quasar.class);
		AbilityFactory.registerAbility(Mixologist.class);
		AbilityList.registerAbility(Mixologist.class);
		AbilityFactory.registerAbility(DrunkenFist.class);
		AbilityList.registerAbility(DrunkenFist.class);
		AbilityFactory.registerAbility(Tensei.class);
		AbilityList.registerAbility(Tensei.class);
		AbilityFactory.registerAbility(TimeFreeze.class);
		AbilityList.registerAbility(TimeFreeze.class);
		AbilityFactory.registerAbility(BlackJack.class);
		AbilityList.registerAbility(BlackJack.class);
		AbilityFactory.registerAbility(Fisherman.class);
		AbilityList.registerAbility(Fisherman.class); //120
		
		AbilityFactory.registerAbility(Darkness.class);
		AbilityList.registerAbility(Darkness.class);
		AbilityFactory.registerAbility(Endless.class);
		AbilityList.registerAbility(Endless.class);
	}
	
	@SuppressWarnings("unchecked")
	public static void registerSynergy() throws ClassNotFoundException {
		SynergyFactory.registerSynergy(PrecisionAiming.class, Sniper.class, HawkEye.class);
		SynergyFactory.registerSynergy(AntiGravity.class, AntiGravity.class, Gravity.class);
		SynergyFactory.registerSynergy(TimeRewind.class, TimeStop.class, TimeTravel.class);
		SynergyFactory.registerSynergy(Dash.class, Dash.class, Accelerator.class);
		SynergyFactory.registerSynergy(EnergyBlocker.class, Indecision.class, SocialDistancing.class);
		SynergyFactory.registerSynergy(PrecisionAiming.class, PrecisionAiming.class, Infallibility.class);
		SynergyFactory.registerSynergy(Kairos.class, Kairos.class, Chance.class);
		SynergyFactory.registerSynergy(GlassCannon.class, VictoryBySword.class, ASAP.class);
		SynergyFactory.registerSynergy(GuardianAngel.class, GuardianAngel.class, PatronSaint.class);
		SynergyFactory.registerSynergy(PrecisionAiming.class, PenetrationArrow.class, HomingPenetrationArrow.class); //10
		
		SynergyFactory.registerSynergy(NineTailFox.class, NineTailFox.class, NineTailFoxSynergy.class);
		SynergyFactory.registerSynergy(MultiHit.class, MultiHit.class, Wadadadadada.class);
		SynergyFactory.registerSynergy(DiceGod.class, Moros.class, LaplaceDemon.class);
		SynergyFactory.registerSynergy(Terrorist.class, TimeStop.class, TimeBomb.class);
		SynergyFactory.registerSynergy(Void.class, Mira.class, SetsunaTrip.class);
		SynergyFactory.registerSynergy(Demigod.class, Soda.class, Demisoda.class);
		SynergyFactory.registerSynergy(Yuki.class, Yuki.class, YukiSnow.class);
		SynergyFactory.registerSynergy(Ruber.class, Crystal.class, HealthCopy.class);
		SynergyFactory.registerSynergy(Tesla.class, Tesla.class, TeslaPlasma.class);
		SynergyFactory.registerSynergy((Class<? extends AbilityBase>) Class.forName("daybreak.abilitywar.ability.list.clown." + ServerVersion.getName() + ".Clown"), Alice.class, Joker.class); //20
		
		SynergyFactory.registerSynergy(Teabagging.class, LittleDevil.class, BadManner.class);
		SynergyFactory.registerSynergy(Dash.class, Divinity.class, MovingSkill.class);
		SynergyFactory.registerSynergy(Curse.class, Divinity.class, CurseOfGod.class);
		SynergyFactory.registerSynergy(DevilBoots.class, Accel.class, GhostRider.class);
		SynergyFactory.registerSynergy(KuroEye.class, KuroEye.class, DemonLord.class);
		SynergyFactory.registerSynergy(Ferda.class, Crystal.class, Fairy.class);
		SynergyFactory.registerSynergy(Kuro.class, Assassin.class, Crow.class);
		SynergyFactory.registerSynergy(Echo.class, Flector.class, Mirror.class);
		SynergyFactory.registerSynergy(Alice.class, Yuki.class, AliceInFreezer.class);
		SynergyFactory.registerSynergy(Berserker.class, Butcher.class, Berserk.class); //30
		
		SynergyFactory.registerSynergy(RandomNumberControl.class, RandomNumberControl.class, DeusExMachina.class);
		SynergyFactory.registerSynergy(Reverse.class, AntiGravity.class, Recall.class);
		SynergyFactory.registerSynergy(HealthFreak.class, Teabagging.class, FingerExersize.class);
		SynergyFactory.registerSynergy(Protagonist.class, SurvivalInstinct.class, Cliche.class);
		SynergyFactory.registerSynergy(Flector.class, Stop.class, HumanBaseball.class);
		SynergyFactory.registerSynergy(HeadtoHead.class, Loki.class, CowardlyMatch.class);
		SynergyFactory.registerSynergy(Empty.class, Empty.class, Abyss.class);	
		SynergyFactory.registerSynergy(Vampire.class, Executioner.class, VampireCount.class);
		SynergyFactory.registerSynergy(King.class, Emperor.class, Crown.class);
		SynergyFactory.registerSynergy(ForbiddenFruit.class, Muse.class, Eden.class); //40
		
		SynergyFactory.registerSynergy(Lunar.class, Stella.class, StarryNight.class);
		SynergyFactory.registerSynergy(Reverse.class, Mazochist.class, Sadism.class);
		SynergyFactory.registerSynergy(Developer.class, NullAbility.class, BugFix.class);
		SynergyFactory.registerSynergy(X.class, Pyromaniac.class, Kamikaze.class);
		SynergyFactory.registerSynergy(Afterimage.class, Afterimage.class, EasyToEvade.class);
		SynergyFactory.registerSynergy(RainStar.class, Stella.class, MeteorShower.class);
		SynergyFactory.registerSynergy(Daybreak.class, Developer.class, Normalization.class);
		SynergyFactory.registerSynergy(GreenGarden.class, Flora.class, Gardener.class);
		SynergyFactory.registerSynergy(Damocles.class, King.class, SwordofDamocles.class);
		SynergyFactory.registerSynergy(Zeus.class, Spark.class, LightningGod.class); //50
		
		SynergyFactory.registerSynergy(Apparition.class, Apparition.class, WraithForm.class);
		SynergyFactory.registerSynergy(Fool.class, Fool.class, KimiwaJitsuniBakadana.class);
		SynergyFactory.registerSynergy(Ruber.class, Luciferium.class, Ruberium.class);
		SynergyFactory.registerSynergy(Lunar.class, MilkyWay.class, DarkMatter.class);
		SynergyFactory.registerSynergy(Devil.class, SoulEncroach.class, Pact.class);
		SynergyFactory.registerSynergy(Quasar.class, Chaos.class, Singularity.class);
		SynergyFactory.registerSynergy(Air.class, Unknown.class, Nameless.class);
		SynergyFactory.registerSynergy(Inferno.class, Glacier.class, HeatAbnormal.class);
		SynergyFactory.registerSynergy(SkyWhale.class, SkyWhale.class, LucidDream.class);
		SynergyFactory.registerSynergy(Executioner.class, Themis.class, Judgement.class); //60
		
		//+4
		new BukkitRunnable() {
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
				try {
					SynergyFactory.registerSynergy(Mira.class, (Class<? extends AbilityBase>) ReflectionUtil.ClassUtil.forName("com.cokes86.cokesaddon.ability.list.Keily"), Sneak.class);
		        } catch (ClassNotFoundException e) {
		            e.printStackTrace();
		            Messager.sendConsoleMessage("§4[§c!§4] §e버그가 아닙니다! 코크스 애드온을 설치해야 이용 가능한 시너지입니다.");
		        }
		    }   
		}.runTaskLater(AbilityWar.getPlugin(), 10L);
		
	}
	
	public static void registerBetaAbility() {
		AbilityFactory.registerAbility(Flex.class);
		AbilityList.registerAbility(Flex.class);
		AbilityFactory.registerAbility(KnockbackPatch.class);
		AbilityList.registerAbility(KnockbackPatch.class);
	}

	public static void registerSpecialAbility() {
		AbilityFactory.registerAbility(NullAbility.class);
		AbilityFactory.registerAbility(Overlap.class);
	    AbilityFactory.registerAbility(AncientBerserker.class);
	    AbilityFactory.registerAbility(AncientBlackCandle.class);
	    AbilityFactory.registerAbility(AncientBombArrow.class);
	    AbilityFactory.registerAbility(AncientBrewer.class);
	    AbilityFactory.registerAbility(AncientChaos.class);
	    AbilityFactory.registerAbility(AncientHacker.class);
	    AbilityFactory.registerAbility(AncientSniper.class);
	    AbilityFactory.registerAbility(AncientTheHighPriestess.class);
	    AbilityFactory.registerAbility(AncientVoid.class);
	    AbilityFactory.registerAbility(AncientYeti.class);
	    AbilityFactory.registerAbility(AncientZombie.class);
	}
	
	public static void registerLiteAbility() {
		LiteAbilities.registerAbility(Accel.class);
		LiteAbilities.registerAbility(Air.class);
		LiteAbilities.registerAbility(AntiGravity.class);
		LiteAbilities.registerAbility(BullsEye.class);
		LiteAbilities.registerAbility(Butcher.class);
		LiteAbilities.registerAbility(Butterfly.class);
		LiteAbilities.registerAbility(Chatterbox.class);
		LiteAbilities.registerAbility(Damocles.class);
		LiteAbilities.registerAbility(Daydream.class);
		LiteAbilities.registerAbility(Delusion.class);
		
		LiteAbilities.registerAbility(Detection.class);
		LiteAbilities.registerAbility(Earthquake.class);
		LiteAbilities.registerAbility(Echo.class);
		LiteAbilities.registerAbility(Empty.class);
		LiteAbilities.registerAbility(Encore.class);
		LiteAbilities.registerAbility(Fingun.class);
		LiteAbilities.registerAbility(FirstAid.class);
		LiteAbilities.registerAbility(Fool.class);
		LiteAbilities.registerAbility(Foresight.class);
		LiteAbilities.registerAbility(GlassCannon.class);
		
		LiteAbilities.registerAbility(GlitchedCrown.class);
		LiteAbilities.registerAbility(GreatCommunicator.class);
		LiteAbilities.registerAbility(HeadtoHead.class);
		LiteAbilities.registerAbility(HealthFreak.class);
		LiteAbilities.registerAbility(Horus.class);
		LiteAbilities.registerAbility(HuntingDog.class);
		LiteAbilities.registerAbility(Indecision.class);
		LiteAbilities.registerAbility(Kid.class);
		LiteAbilities.registerAbility(King.class);
		LiteAbilities.registerAbility(LingeringArrow.class);
		
		LiteAbilities.registerAbility(LittleDevil.class);
		LiteAbilities.registerAbility(Luciferium.class);
		LiteAbilities.registerAbility(LuckSurvive.class);
		LiteAbilities.registerAbility(Mazochist.class);
		LiteAbilities.registerAbility(Medusa.class);
		LiteAbilities.registerAbility(Megalodon.class);
		LiteAbilities.registerAbility(MultiHit.class);
		LiteAbilities.registerAbility(OneShotOneKill.class);
		LiteAbilities.registerAbility(Panda.class);
		LiteAbilities.registerAbility(PrecisionAiming.class);
		
		LiteAbilities.registerAbility(Protagonist.class);
		LiteAbilities.registerAbility(RandomNumberControl.class);
		LiteAbilities.registerAbility(SoyMilk.class);
		LiteAbilities.registerAbility(Stop.class);
		LiteAbilities.registerAbility(Succubus.class);
		LiteAbilities.registerAbility(Teabagging.class);
		LiteAbilities.registerAbility(Tekkai.class);
		LiteAbilities.registerAbility(Vaccine.class);
		LiteAbilities.registerAbility(Zero.class);
		AbilityFactory.registerAbility(HomingArrow.class);
		LiteAbilities.registerAbility(HomingArrow.class);
	}
	
	public static void registerMurderAbility() {
		//innocent
		AbilityFactory.registerAbility(GoodPerson.class);
		JobList.registerJob(GoodPerson.class, "선량한 시민", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Hacker.class);
		JobList.registerJob(Hacker.class, "해커", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Hermit.class);
		JobList.registerJob(Hermit.class, "은둔자", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Athlete.class);
		JobList.registerJob(Athlete.class, "육상선수", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Rich.class);
		JobList.registerJob(Rich.class, "부자", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Soldier.class);
		JobList.registerJob(Soldier.class, "군인", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Watcher.class);
		JobList.registerJob(Watcher.class, "감시자", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Psycher.class);
		JobList.registerJob(Psycher.class, "초능력자", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Profiler.class);
		JobList.registerJob(Profiler.class, "프로파일러", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Clown.class);
		JobList.registerJob(Clown.class, "광대", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Streamer.class);
		JobList.registerJob(Streamer.class, "스트리머", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Investigator.class);
		JobList.registerJob(Investigator.class, "수사관", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Painter.class);
		JobList.registerJob(Painter.class, "화가", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Astrologer.class);
		JobList.registerJob(Astrologer.class, "점성술사", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(BodyGuard.class);
		JobList.registerJob(BodyGuard.class, "경호원", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Official.class);
		JobList.registerJob(Official.class, "공무원", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Bartender.class);
		JobList.registerJob(Bartender.class, "바텐더", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Frog.class);
		AbilityFactory.registerAbility(Mediumship.class);
		JobList.registerJob(Mediumship.class, "영매사", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Gambler.class);
		JobList.registerJob(Gambler.class, "도박꾼", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Hoarder.class);
		JobList.registerJob(Hoarder.class, "수집가", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Informant.class);
		JobList.registerJob(Informant.class, "정보상", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(GraveRobber.class);
		JobList.registerJob(GraveRobber.class, "도굴꾼", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Idol.class);
		JobList.registerJob(Idol.class, "아이돌", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Explorer.class);
		JobList.registerJob(Explorer.class, "탐험가", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Ghost.class);
		JobList.registerJob(Ghost.class, "유령", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Tracker.class);
		JobList.registerJob(Tracker.class, "추적자", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Tailor.class);
		JobList.registerJob(Tailor.class, "재봉사", CharacterType.INNOCENT);
		AbilityFactory.registerAbility(Priest.class);
		JobList.registerJob(Priest.class, "사제", CharacterType.INNOCENT);
		
		//murderer
		AbilityFactory.registerAbility(ScammerMurderer.class);
		JobList.registerJob(ScammerMurderer.class, "사기꾼", CharacterType.MURDER);
		AbilityFactory.registerAbility(SpyMurderer.class);
		JobList.registerJob(SpyMurderer.class, "스파이", CharacterType.MURDER);
		AbilityFactory.registerAbility(SerialMurderer.class);
		JobList.registerJob(SerialMurderer.class, "연쇄살인범", CharacterType.MURDER);
		AbilityFactory.registerAbility(TerroristMurderer.class);
		JobList.registerJob(TerroristMurderer.class, "테러리스트", CharacterType.MURDER);
		AbilityFactory.registerAbility(HitmanMurderer.class);
		JobList.registerJob(HitmanMurderer.class, "살인청부업자", CharacterType.MURDER);
		AbilityFactory.registerAbility(BigShotMurderer.class);
		JobList.registerJob(BigShotMurderer.class, "거물", CharacterType.MURDER);
		AbilityFactory.registerAbility(CorruptionDetectiveMurderer.class);
		AbilityFactory.registerAbility(PlagueDoctorMurderer.class);
		JobList.registerJob(PlagueDoctorMurderer.class, "역병의사", CharacterType.MURDER);
		AbilityFactory.registerAbility(DrugDealerMurderer.class);
		JobList.registerJob(DrugDealerMurderer.class, "마약상", CharacterType.MURDER);
		AbilityFactory.registerAbility(MadScientistMurderer.class);
		JobList.registerJob(MadScientistMurderer.class, "매드 사이언티스트", CharacterType.MURDER);
		AbilityFactory.registerAbility(VampireMurderer.class);
		JobList.registerJob(VampireMurderer.class, "뱀파이어", CharacterType.MURDER);
		AbilityFactory.registerAbility(PhantomThiefMurderer.class);
		JobList.registerJob(PhantomThiefMurderer.class, "괴도", CharacterType.MURDER);
		AbilityFactory.registerAbility(WeaponRobotMurderer.class);
		JobList.registerJob(WeaponRobotMurderer.class, "개조병기", CharacterType.MURDER);
		AbilityFactory.registerAbility(CultLeaderMurderer.class);
		JobList.registerJob(CultLeaderMurderer.class, "사이비 교주", CharacterType.MURDER);
		AbilityFactory.registerAbility(FanaticMurderer.class);
		AbilityFactory.registerAbility(SilentMurderer.class);
		JobList.registerJob(SilentMurderer.class, "사일런트", CharacterType.MURDER);
		AbilityFactory.registerAbility(JudasMurderer.class);
		JobList.registerJob(JudasMurderer.class, "배신자 유다", CharacterType.MURDER);
		AbilityFactory.registerAbility(WitchMurderer.class);
		JobList.registerJob(WitchMurderer.class, "마녀", CharacterType.MURDER);
		AbilityFactory.registerAbility(GrimReaperMurderer.class);
		JobList.registerJob(GrimReaperMurderer.class, "사신", CharacterType.MURDER);
		AbilityFactory.registerAbility(JackMurderer.class);
		JobList.registerJob(JackMurderer.class, "사형수 잭", CharacterType.MURDER);
		AbilityFactory.registerAbility(ZombieMurderer.class);
		JobList.registerJob(ZombieMurderer.class, "좀비", CharacterType.MURDER);
		AbilityFactory.registerAbility(PsychopathMurderer.class);
		JobList.registerJob(PsychopathMurderer.class, "사이코패스", CharacterType.MURDER);
		AbilityFactory.registerAbility(SpecterMurderer.class);
		JobList.registerJob(SpecterMurderer.class, "망령", CharacterType.MURDER);
		AbilityFactory.registerAbility(ExecutionerMurderer.class);
		JobList.registerJob(ExecutionerMurderer.class, "처형인", CharacterType.MURDER);
		
		//detective
		AbilityFactory.registerAbility(DetectiveDisarmament.class);
		AbilityFactory.registerAbility(DetectiveFastRegen.class);
		AbilityFactory.registerAbility(DetectiveGlower.class);
		AbilityFactory.registerAbility(DetectiveGoldDetect.class);
		AbilityFactory.registerAbility(DetectiveGoldGiver.class);
		AbilityFactory.registerAbility(DetectiveHomingUp.class);
		AbilityFactory.registerAbility(DetectiveInfiniteRegen.class);
		AbilityFactory.registerAbility(DetectiveIntuition.class);
		AbilityFactory.registerAbility(DetectiveMonitoring.class);
		AbilityFactory.registerAbility(DetectiveNonCharge.class);
		AbilityFactory.registerAbility(DetectivePsychometry.class);
		AbilityFactory.registerAbility(DetectiveResponsibility.class);
		AbilityFactory.registerAbility(DetectiveShield.class);
		AbilityFactory.registerAbility(DetectiveSpeedUp.class);
		AbilityFactory.registerAbility(DetectiveStakeOut.class);
		AbilityFactory.registerAbility(DetectiveTaxCut.class);
	}

}
