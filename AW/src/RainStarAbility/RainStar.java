package RainStarAbility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.World;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import RainStarEffect.Charm;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.CustomEntity;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.manager.effect.Stun;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.boundary.CenteredBoundingBox;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.base.random.RouletteWheel;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(name = "레인스타", rank = Rank.SPECIAL, species = Species.SPECIAL, explain = {
		"§7패시브 §8- §5점성술§f: 열두 별자리 중 하나를 배정받습니다.", 
		"$(EXPLAIN)", 
		"§7검 공격 §8- §a별소나기§f: 다른 플레이어를 근접 공격할 때마다 20%의 확률로",
		" 내 주변을 맴도는 별소나기를 소환합니다. 이 확률은 발동 실패시마다 20%씩", 
		" 증가하며 총 일곱 개의 별소나기만 소지 가능합니다.",
		" 별소나기는 누군가를 5회 이상 타격했을 때 사라집니다.", 
		"§7활 공격 §8- §e스타더스트§f: 화살을 발사할 때 화살을 따라다니는 혜성 투사체가",
		" 같이 소환됩니다. 혜성은 화살이 적중한 위치로부터 0.5초간 가장 가까운 적에게로", 
		" 유도됩니다. 만약 엔티티를 맞혔을 경우, 대신 대상을 발화시킵니다. $[ARROW_COOLDOWN]",
		"§7철괴 우클릭 §8- §3은하수의 밤§f: 7칸의 필드를 $[FIELD_DURATION]초간 전개합니다. 필드 위에서 별소나기가",
		" 항상 발동하며, 근접 공격 피해량이 1.3배로 증가합니다. $[RIGHT_COOLDOWN]", 
		"§7철괴 좌클릭 §8- §b별의 일주§f: 점성술의 별자리 효과를 다음 별자리로 넘깁니다.",
		" 또한 $[MODE_DURATION]초간 §b수호 모드§f가 되어 별소나기에 적중한 적에게 피해를 주는 대신", 
		" 줄 피해의 10%만큼 내 체력을 회복합니다. $[LEFT_COOLDOWN]" })

public class RainStar extends AbilityBase implements ActiveHandler {

	public RainStar(Participant participant) {
		super(participant);
	}

	public static final SettingObject<Integer> LEFT_COOLDOWN = abilitySettings.new SettingObject<Integer>(
			RainStar.class, "left-cooldown", 77, "# 좌클릭 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};

	public static final SettingObject<Integer> RIGHT_COOLDOWN = abilitySettings.new SettingObject<Integer>(
			RainStar.class, "right-cooldown", 127, "# 우클릭 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};

	public static final SettingObject<Integer> ARROW_COOLDOWN = abilitySettings.new SettingObject<Integer>(
			RainStar.class, "arrow-cooldown", 17, "# 화살 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};

	public static final SettingObject<Integer> FIELD_DURATION = abilitySettings.new SettingObject<Integer>(
			RainStar.class, "field-duration", 12, "# 필드 지속시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};

	public static final SettingObject<Integer> MODE_DURATION = abilitySettings.new SettingObject<Integer>(
			RainStar.class, "mode-duration", 7, "# 모드 지속시간") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};

	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer()))
				return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
								.getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()),
							participant = getParticipant();
					return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
							|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};

	private final Cooldown leftcool = new Cooldown(LEFT_COOLDOWN.getValue(), "일주");
	private final Cooldown rightcool = new Cooldown(RIGHT_COOLDOWN.getValue(), "은하수");
	private final Cooldown arrowcool = new Cooldown(ARROW_COOLDOWN.getValue(), "혜성", CooldownDecrease._50);

	private Circle circle = Circle.of(7, 140);
	private Circle crabcircle1 = Circle.of(1, 3);
	private Circle crabcircle2 = Circle.of(2, 5);
	private int constellation = new Random().nextInt(12);
	private AttributeModifier armorincrease = new AttributeModifier(UUID.randomUUID(), "armor_increase", 3,
			Operation.ADD_NUMBER);
	private Bullet bullet = null;
	private Bullet2 bullet2 = null;
	private final Circle[] circles = newCircleArray(8);
	private final Bullets bullets = new Bullets();
	private final RouletteWheel rouletteWheel = new RouletteWheel();
	private final RouletteWheel.Slice positive = rouletteWheel.newSlice(20),
			negative = rouletteWheel.newSlice(100 - positive.getWeight());
	private Location mylocation;
	private final ActionbarChannel ac1 = newActionbarChannel();
	private final ActionbarChannel ac2 = newActionbarChannel();
	private PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 30, 3, true, false);
	private PotionEffect poison = new PotionEffect(PotionEffectType.POISON, 60, 1, true, false);
	private int soundplaycount = 0;

	private RGB lemonlime;
	private int lemonlimestacks = 0;
	private boolean lemonlimeturns = true;

	private final RGB lemonlime1 = RGB.of(254, 254, 1), lemonlime2 = RGB.of(235, 254, 8),
			lemonlime3 = RGB.of(215, 254, 16), lemonlime4 = RGB.of(196, 254, 24), lemonlime5 = RGB.of(176, 254, 33);

	private List<RGB> lemonlimes = new ArrayList<RGB>() {
		{
			add(lemonlime1);
			add(lemonlime2);
			add(lemonlime3);
			add(lemonlime4);
			add(lemonlime5);
		}
	};

	private RGB rainbow;
	private int rainbowstack = 0;

	private final RGB rainbow1 = RGB.of(166, 3, 171), rainbow2 = RGB.of(87, 14, 211), rainbow3 = RGB.of(8, 25, 251),
			rainbow4 = RGB.of(17, 83, 162), rainbow5 = RGB.of(26, 141, 72), rainbow6 = RGB.of(128, 192, 40),
			rainbow7 = RGB.of(254, 254, 1), rainbow8 = RGB.of(241, 161, 11), rainbow9 = RGB.of(241, 105, 35),
			rainbow10 = RGB.of(235, 44, 61), rainbow11 = RGB.of(232, 23, 102), rainbow12 = RGB.of(198, 13, 137);

	private List<RGB> rainbows = new ArrayList<RGB>() {
		{
			add(rainbow1);
			add(rainbow2);
			add(rainbow3);
			add(rainbow4);
			add(rainbow5);
			add(rainbow6);
			add(rainbow7);
			add(rainbow8);
			add(rainbow9);
			add(rainbow10);
			add(rainbow11);
			add(rainbow12);
		}
	};

	private int stacks = 0;
	private boolean turns = true;

	private RGB gradation;

	private final RGB gradation1 = RGB.of(3, 212, 168), gradation2 = RGB.of(8, 212, 178),
			gradation3 = RGB.of(15, 213, 190), gradation4 = RGB.of(18, 211, 198), gradation5 = RGB.of(27, 214, 213),
			gradation6 = RGB.of(29, 210, 220), gradation7 = RGB.of(30, 207, 225), gradation8 = RGB.of(24, 196, 223),
			gradation9 = RGB.of(23, 191, 226), gradation10 = RGB.of(19, 182, 226), gradation11 = RGB.of(16, 174, 227),
			gradation12 = RGB.of(13, 166, 228), gradation13 = RGB.of(10, 159, 228), gradation14 = RGB.of(7, 151, 229),
			gradation15 = RGB.of(3, 143, 229), gradation16 = RGB.of(1, 135, 230), gradation17 = RGB.of(1, 126, 222),
			gradation18 = RGB.of(1, 118, 214), gradation19 = RGB.of(1, 109, 207), gradation20 = RGB.of(1, 101, 199),
			gradation21 = RGB.of(1, 92, 191);

	private List<RGB> gradations = new ArrayList<RGB>() {
		{
			add(gradation1);
			add(gradation2);
			add(gradation3);
			add(gradation4);
			add(gradation5);
			add(gradation6);
			add(gradation7);
			add(gradation8);
			add(gradation9);
			add(gradation10);
			add(gradation11);
			add(gradation12);
			add(gradation13);
			add(gradation14);
			add(gradation15);
			add(gradation16);
			add(gradation17);
			add(gradation18);
			add(gradation19);
			add(gradation20);
			add(gradation21);
		}
	};

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			bullets.start();
			passive.start();
		}
	}

	private final Object EXPLAIN = new Object() {
		@Override
		public String toString() {
			final StringJoiner joiner = new StringJoiner("\n");
			switch (constellation) {
			case 0:
				joiner.add(" §b양자리§f: 체력이 25% 이하가 되는 피해를 엔티티에게 받을 때");
				joiner.add(" 잃은 체력만큼의 흡수 체력이 생성됩니다.");
				break;
			case 1:
				joiner.add(" §b황소자리§f: 다른 플레이어를 바라보고 달릴 때 신속 버프를 획득합니다.");
				joiner.add(" 또한 혜성의 타격이 더 큰 넉백을 가집니다.");
				break;
			case 2:
				joiner.add(" §b쌍둥이자리§f: 별소나기가 한 번에 두 개씩 생겨납니다.");
				break;
			case 3:
				joiner.add(" §b게자리§f: 혜성을 발사해 대상을 맞힐 때 20% 확률로");
				joiner.add(" 대상을 잠시 기절시키고 집게를 소환해 범위 마법 피해를 입힙니다.");
				break;
			case 4:
				joiner.add(" §b사자자리§f: 내 체력에 반비례해 근접 피해가 강화됩니다.");
				break;
			case 5:
				joiner.add(" §b처녀자리§f: 별소나기가 타격한 적이 10%의 확률로 2초간");
				joiner.add(" 약화된 유혹 상태에 빠집니다.");
				break;
			case 6:
				joiner.add(" §b천칭자리§f: 별소나기의 대미지만큼 방어력이 상승하지만,");
				joiner.add(" 별소나기의 대미지가 감소합니다.");
				break;
			case 7:
				joiner.add(" §b전갈자리§f: 혜성이 타격한 적이 일정 시간 중독 피해를 입습니다.");
				break;
			case 8:
				joiner.add(" §b궁수자리§f: 화살이 중력을 무시하고 더 빨리 날아갑니다.");
				joiner.add(" 혜성 지속 시간이 늘어나고, 혜성의 피해가 강화됩니다.");
				break;
			case 9:
				joiner.add(" §b염소자리§f: 모든 회복 효과를 받을 때 1.3배로 강화됩니다.");
				break;
			case 10:
				joiner.add(" §b물병자리§f: 혜성이 적중할 때 시듦의 은하수가 생성되어 지속 피해를 입힙니다.");
				break;
			case 11:
				joiner.add(" §b물고기자리§f: 혜성이 적을 맞힐 때 대상을 맞힐 때 대상을");
				joiner.add(" 화살의 위치까지 끌어당깁니다.");
				break;
			}
			return joiner.toString();
		}
	};

	private final AbilityTimer armorAdding = new AbilityTimer() {

		@Override
		public void onStart() {
			getPlayer().getAttribute(Attribute.GENERIC_ARMOR).addModifier(armorincrease);
		}

		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		public void onSilentEnd() {
			getPlayer().getAttribute(Attribute.GENERIC_ARMOR).removeModifier(armorincrease);
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();

	private final AbilityTimer passive = new AbilityTimer() {

		@Override
		public void run(int count) {
			switch (constellation) {
			case 0:
				ac1.update("§b양자리");
				break;
			case 1:
				ac1.update("§b황소자리");
				Player p = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 100, predicate);
				if (p != null)
					getPlayer().addPotionEffect(speed);
				break;
			case 2:
				ac1.update("§b쌍둥이자리");
				break;
			case 3:
				ac1.update("§b게자리");
				break;
			case 4:
				ac1.update("§b사자자리");
				break;
			case 5:
				ac1.update("§b처녀자리");
				break;
			case 6:
				ac1.update("§b천칭자리");
				if (!armorAdding.isRunning())
					armorAdding.start();
				break;
			case 7:
				ac1.update("§b전갈자리");
				break;
			case 8:
				ac1.update("§b궁수자리");
				break;
			case 9:
				ac1.update("§b염소자리");
				break;
			case 10:
				ac1.update("§b물병자리");
				break;
			case 11:
				ac1.update("§b물고기자리");
				break;
			}
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();

	private final AbilityTimer protection = new AbilityTimer(MODE_DURATION.getValue() * 20) {

		@Override
		public void run(int count) {
			ac2.update("§a✚");
		}

		@Override
		public void onEnd() {
			onSilentEnd();
		}

		@Override
		public void onSilentEnd() {
			ac2.update(null);
		}

	}.setPeriod(TimeUnit.TICKS, 1).register();

	private final AbilityTimer music = new AbilityTimer(TaskType.NORMAL, 121) {
		
		private final Note NOTE_17 = new Note(17), NOTE_19 = new Note(19), NOTE_22 = new Note(22),
				NOTE_24 = new Note(24), NOTE_8 = new Note(8), NOTE_10 = new Note(10), NOTE_12 = new Note(12),
				NOTE_14 = new Note(14), NOTE_15 = new Note(15);

		@Override
		public void run(int count) {
			switch (count) {
			case 0:
				playSound0();
				break;
			case 2:
				playSound2();
				break;
			case 4:
				playSound4();
				break;
			case 6:
				playSound6();
				break;
			case 16:
				playSound16();
				break;
			case 22:
				playSound22();
				break;
			case 26:
				playSound26();
				break;
			case 28:
				playSound28();
				break;
			case 30:
				playSound30();
				break;
			case 32:
				playSound32();
				break;
			case 36:
				playSound36();
				break;
			case 40:
				playSound40();
				break;
			case 42:
				playSound42();
				break;
			case 44:
				playSound44();
				break;
			case 46:
				playSound46();
				break;
			case 48:
				playSound48();
				break;
			case 50:
				playSound50();
				break;
			case 54:
				playSound54();
				break;
			case 58:
				playSound58();
				break;
			case 60:
				playSound60();
				break;
			case 66:
				playSound66();
				break;
			case 68:
				playSound68();
				break;
			case 70:
				playSound70();
				break;
			case 78:
				playSound78();
				break;
			case 80:
				playSound80();
				break;
			case 84:
				playSound84();
				break;
			case 86:
				playSound86();
				break;
			case 90:
				playSound90();
				break;
			case 92:
				playSound92();
				break;
			case 98:
				playSound98();
				break;
			case 100:
				playSound100();
				break;
			case 102:
				playSound102();
				break;
			case 104:
				playSound104();
				break;
			case 106:
				playSound106();
				break;
			case 108:
				playSound108();
				break;
			case 110:
				playSound110();
				break;
			case 112:
				playSound112();
				break;
			case 114:
				playSound114();
				break;
			case 116:
				playSound116();
				break;
			case 118:
				playSound118();
				break;
			case 120:
				playSound120();
				break;
			}
		}

		public void playSound0() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_19);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_14);
		}

		public void playSound2() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_17);
		}

		public void playSound4() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_14);
		}

		public void playSound6() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_22);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_19);
		}

		public void playSound16() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_24);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_19);
		}

		public void playSound22() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_19);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound26() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_17);
		}

		public void playSound28() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_17);
		}

		public void playSound30() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound32() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_12);
		}

		public void playSound36() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_8);
		}

		public void playSound40() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_17);
		}

		public void playSound42() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound44() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound46() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound48() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
		}

		public void playSound50() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_17);
		}

		public void playSound54() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_19);
		}

		public void playSound58() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_22);
		}

		public void playSound60() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_19);
		}

		public void playSound66() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_17);
		}

		public void playSound68() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound70() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_12);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound78() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound80() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_22);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound84() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_24);
		}

		public void playSound86() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_22);
		}

		public void playSound90() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound92() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound98() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
		}

		public void playSound100() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
		}

		public void playSound102() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
		}

		public void playSound104() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_17);
		}

		public void playSound106() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound108() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound110() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}

		public void playSound112() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
		}

		public void playSound114() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_17);
		}

		public void playSound116() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
		}

		public void playSound118() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_17);
		}

		public void playSound120() {
			SoundLib.CHIME.playInstrument(mylocation, NOTE_10);
			SoundLib.CHIME.playInstrument(mylocation, NOTE_15);
		}
	}.setPeriod(TimeUnit.TICKS, 2);

	@SubscribeEvent
	public void onEntityRegainHealth(EntityRegainHealthEvent e) {
		if (constellation == 9 && e.getEntity().equals(getPlayer())) {
			e.setAmount(e.getAmount() * 1.3);
		}
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (constellation == 0 && e.getEntity().equals(getPlayer())) {
			if (getPlayer().getHealth() >= getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 4) {
				if (getPlayer().getHealth() - e.getFinalDamage() <= getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 4) {
					NMS.setAbsorptionHearts(getPlayer(), (float) (NMS.getAbsorptionHearts(getPlayer()) + e.getFinalDamage()));
				}
			}
		}
		if (e.getDamager().equals(getPlayer()) && e.getEntity() instanceof Player) {
			if (skill.isRunning() && LocationUtil.isInCircle(mylocation, getPlayer().getLocation(), 7)) {
				if (bullets.bullets.size() != 7) {
					bullets.add(new Bullet(getPlayer(), getPlayer().getLocation()));
					soundplaycount++;
    				switch(soundplaycount) {
    				case 1: 				
    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
    					break;
    				case 2: 				
    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
    					break;
    				case 3: 				
    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
    					break;
    				case 4: 				
    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.C));
    					break;
    				case 5: 				
    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
    					break;
    				case 6: 				
    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(2, Tone.F));
    					break;
    				case 7: 				
    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.C));
    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
    					soundplaycount = 0;
    					break;
    				}
					if (constellation == 2) bullets.add(new Bullet(getPlayer(), getPlayer().getLocation()));
				}
				e.setDamage(e.getDamage() * 1.3);
			} else {
				final RouletteWheel.Slice select = rouletteWheel.select();
				if (select == positive) {
					if (bullets.bullets.size() != 7) {
						bullets.add(new Bullet(getPlayer(), getPlayer().getLocation()));
						if (constellation == 2) bullets.add(new Bullet(getPlayer(), getPlayer().getLocation()));
						soundplaycount++;
	    				switch(soundplaycount) {
	    				case 1: 				
	    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
	    					break;
	    				case 2: 				
	    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
	    					break;
	    				case 3: 				
	    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
	    					break;
	    				case 4: 				
	    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.C));
	    					break;
	    				case 5: 				
	    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
	    					break;
	    				case 6: 				
	    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(2, Tone.F));
	    					break;
	    				case 7: 				
	    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.C));
	    					SoundLib.CHIME.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
	    					soundplaycount = 0;
	    					break;
	    				}
					}
					positive.resetWeight();
				} else {
					positive.increaseWeight(20);
					negative.resetWeight();
				}
			}
			if (constellation == 4) {
				double maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				e.setDamage(e.getDamage() + (3 * (1 - (getPlayer().getHealth() / maxHealth))));
			}
		}
	}

	@SubscribeEvent
	public void onProjectileLaunch(ProjectileLaunchEvent e) {
		if (e.getEntity() instanceof Arrow) {
			Arrow arrow = (Arrow) e.getEntity();
			if (getPlayer().equals(arrow.getShooter()) && !arrowcool.isRunning()) {
				new Bullet2(getPlayer(), getPlayer().getLocation().clone().add(0, 1, 0), arrow,
						arrow.getVelocity().length()).start();
				arrowcool.start();
			}
			if (constellation == 8) {
				new AbilityTimer(100) {

					@Override
					public void onStart() {
						arrow.setGravity(false);
						arrow.setVelocity(arrow.getVelocity().multiply(1.1));
					}

					@Override
					public void onEnd() {
						onSilentEnd();
					}

					@Override
					public void onSilentEnd() {
						arrow.setGravity(true);
					}

				}.setPeriod(TimeUnit.TICKS, 1).start();
			}
		}
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT) {
			if (clickType == ClickType.RIGHT_CLICK) {
				if (!skill.isDuration() && !rightcool.isCooldown()) {
					skill.start();
					return true;
				}
			} else if (clickType == ClickType.LEFT_CLICK) {
				if (!leftcool.isCooldown()) {
					if (constellation == 11)
						constellation = 0;
					else
						constellation++;
					switch (constellation) {
					case 0:
						getPlayer().sendMessage("§b양자리§f: 체력이 25% 이하가 되는 피해를 엔티티에게 받을 때 잃은 체력만큼의 흡수 체력이 생성됩니다.");
						break;
					case 1:
						getPlayer().sendMessage("§b황소자리§f: 다른 플레이어를 바라보고 달릴 때 신속 버프를 획득합니다. 또한 혜성의 타격이 더 큰 넉백을 가집니다.");
						break;
					case 2:
						getPlayer().sendMessage("§b쌍둥이자리§f: 별소나기가 한 번에 두 개씩 생겨납니다.");
						break;
					case 3:
						getPlayer().sendMessage("§b게자리§f: 혜성을 발사해 대상을 맞힐 때 20% 확률로 대상을 잠시 기절시키고 집게를 소환해 범위 마법 피해를 입힙니다.");
						break;
					case 4:
						getPlayer().sendMessage("§b사자자리§f: 내 체력에 반비례해 근접 피해가 강화됩니다.");
						break;
					case 5:
						getPlayer().sendMessage("§b처녀자리§f: 별소나기가 타격한 적이 10%의 확률로 2초간 약화된 유혹 상태에 빠집니다.");
						break;
					case 6:
						getPlayer().sendMessage("§b천칭자리§f: 별소나기의 대미지만큼 방어력이 상승하지만, 별소나기의 대미지가 감소합니다.");
						break;
					case 7:
						getPlayer().sendMessage("§b전갈자리§f: 혜성이 타격한 적이 일정 시간 중독 피해를 입습니다.");
						break;
					case 8:
						getPlayer().sendMessage("§b궁수자리§f: 화살이 중력을 무시하고 날아가며 혜성 지속 시간이 늘어납니다. 또한 혜성의 피해가 강화됩니다.");
						break;
					case 9:
						getPlayer().sendMessage("§b염소자리§f: 모든 회복 효과를 받을 때 1.3배로 강화됩니다.");
						break;
					case 10:
						getPlayer().sendMessage("§b물병자리§f: 혜성이 적중할 때 시듦의 은하수가 생성되어 지속 피해를 입힙니다.");
						break;
					case 11:
						getPlayer().sendMessage("§b물고기자리§f: 혜성이 적을 맞힐 때 대상을 맞힐 때 대상을 화살의 위치까지 끌어당깁니다.");
						break;
					}
					if (armorAdding.isRunning())
						armorAdding.stop(false);
					if (!protection.isRunning()) {
						protection.start();
					} else {
						protection.setCount(MODE_DURATION.getValue() * 20);
					}
					SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.2f);
					return leftcool.start();	
				}
			}
		}
		return false;
	}

	private final Duration skill = new Duration(FIELD_DURATION.getValue() * 20, rightcool) {

		@Override
		protected void onDurationStart() {
			mylocation = LocationUtil.floorY(getPlayer().getLocation().clone());
			music.start();
		}

		@Override
		protected void onDurationProcess(int count) {
			if (count % 40 == 0) {
				ParticleLib.VILLAGER_HAPPY.spawnParticle(mylocation.clone().add(0, 2, 0), 4, 2, 4, 200, 0);
			}
			if (count % 4 == 0) {
				if (lemonlimeturns)
					lemonlimestacks++;
				else
					lemonlimestacks--;
				if (lemonlimestacks % (lemonlimes.size() - 1) == 0) {
					lemonlimeturns = !lemonlimeturns;
				}
				lemonlime = lemonlimes.get(lemonlimestacks);
				if (rainbowstack >= 11) {
					rainbowstack = 0;
				} else {
					rainbowstack++;
				}
				rainbow = rainbows.get(rainbowstack);
				if (turns)
					stacks++;
				else
					stacks--;
				if (stacks % (gradations.size() - 1) == 0) {
					turns = !turns;
				}
				gradation = gradations.get(stacks);
				for (int iteration = 0; iteration < 5; iteration++) {
					double angle = Math.toRadians(72.0D * iteration);
					double nextAngle = Math.toRadians(72.0D * (iteration + 2));
					double x = Math.cos(angle) * 5.5D;
					double z = Math.sin(angle) * 5.5D;
					double x2 = Math.cos(nextAngle) * 5.5D;
					double z2 = Math.sin(nextAngle) * 5.5D;
					double deltaX = x2 - x;
					double deltaZ = z2 - z;
					for (double d = 0.0D; d < 1.0D; d += 0.03D) {
						mylocation.add(x + deltaX * d, 0.0D, z + deltaZ * d);
						ParticleLib.REDSTONE.spawnParticle(mylocation, gradation);
						mylocation.subtract(x + deltaX * d, 0.0D, z + deltaZ * d);
					}

					for (int iteration2 = 0; iteration2 < 5; iteration2++) {
						double smallAngle = Math.toRadians(72.0D * iteration2);
						double smallNextAngle = Math.toRadians(72.0D * (iteration2 + 2));
						double smallX = Math.cos(smallAngle) * 1.5D;
						double smallZ = Math.sin(smallAngle) * 1.5D;
						double smallX2 = Math.cos(smallNextAngle) * 1.5D;
						double smallZ2 = Math.sin(smallNextAngle) * 1.5D;
						double sDeltaX = smallX2 - smallX;
						double sDeltaZ = smallZ2 - smallZ;

						double sideAngle = Math.toRadians(72.0D * (iteration + 1));

						double locationx = (x + Math.cos(sideAngle) * 5.5D) / 2;
						double locationz = (z + Math.sin(sideAngle) * 5.5D) / 2;

						mylocation.add(locationx, 0.0D, locationz);
						for (double d2 = 0.0D; d2 < 1.0D; d2 += 0.125D) {
							mylocation.add(smallX + sDeltaX * d2, 0.0D, smallZ + sDeltaZ * d2);
							ParticleLib.REDSTONE.spawnParticle(mylocation, rainbow);
							mylocation.subtract(smallX + sDeltaX * d2, 0.0D, smallZ + sDeltaZ * d2);
						}
						mylocation.subtract(locationx, 0.0D, locationz);
					}
				}
				for (Location loc : circle.toLocations(mylocation).floor(mylocation.getY())) {
					ParticleLib.REDSTONE.spawnParticle(loc, lemonlime);
				}
			}
		}

		@Override
		protected void onDurationEnd() {
			onDurationSilentEnd();
		}

		@Override
		protected void onDurationSilentEnd() {
		}

	}.setPeriod(TimeUnit.TICKS, 1);

	private static Circle[] newCircleArray(final int maxAmount) {
		final Circle[] circles = new Circle[maxAmount];
		for (int i = 0; i < circles.length; i++) {
			circles[i] = Circle.of(2.5, i + 1);
		}
		return circles;
	}

	private class Bullets extends AbilityTimer {

		private final List<Bullet> bullets = new ArrayList<Bullet>() {
			@Override
			public boolean add(Bullet o) {
				if (size() < 7)
					return super.add(o);
				return false;
			}
		};

		private double rotation = 0.0;

		private Bullets() {
			setPeriod(TimeUnit.TICKS, 1);
		}

		private void add(Bullet Bullet) {
			bullets.add(Bullet);
		}

		@Override
		protected void run(int count) {
			if (bullets.isEmpty())
				return;
			final List<Vector> circle = circles[bullets.size() - 1].clone().rotateAroundAxisY(rotation += 5);
			final Location playerLocation = getPlayer().getLocation().clone().add(0, 1, 0);
			for (int i = 0; i < (circle.size() - 1); i++) {
				if (i < bullets.size()) {
					final Vector vector = circle.get(i);
					final Bullet bullet = bullets.get(i);
					bullet.updateLocation(playerLocation.clone().add(vector));
				}
			}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}
	}

	public class Bullet {

		private final LivingEntity shooter;
		private final CenteredBoundingBox boundingBox;
		private final Predicate<Entity> predicate;
		private Set<Damageable> hitcheck = new HashSet<>();

		private Map<Bullet, Integer> attackcount = new HashMap<>();
		private int stacks = 0;
		private boolean turns = true;

		private RGB gradation;

		private final RGB gradation1 = RGB.of(3, 212, 168), gradation2 = RGB.of(8, 212, 178),
				gradation3 = RGB.of(15, 213, 190), gradation4 = RGB.of(18, 211, 198), gradation5 = RGB.of(27, 214, 213),
				gradation6 = RGB.of(29, 210, 220), gradation7 = RGB.of(30, 207, 225), gradation8 = RGB.of(24, 196, 223),
				gradation9 = RGB.of(23, 191, 226), gradation10 = RGB.of(19, 182, 226),
				gradation11 = RGB.of(16, 174, 227), gradation12 = RGB.of(13, 166, 228),
				gradation13 = RGB.of(10, 159, 228), gradation14 = RGB.of(7, 151, 229),
				gradation15 = RGB.of(3, 143, 229), gradation16 = RGB.of(1, 135, 230), gradation17 = RGB.of(1, 126, 222),
				gradation18 = RGB.of(1, 118, 214), gradation19 = RGB.of(1, 109, 207), gradation20 = RGB.of(1, 101, 199),
				gradation21 = RGB.of(1, 92, 191);

		private List<RGB> gradations = new ArrayList<RGB>() {
			{
				add(gradation1);
				add(gradation2);
				add(gradation3);
				add(gradation4);
				add(gradation5);
				add(gradation6);
				add(gradation7);
				add(gradation8);
				add(gradation9);
				add(gradation10);
				add(gradation11);
				add(gradation12);
				add(gradation13);
				add(gradation14);
				add(gradation15);
				add(gradation16);
				add(gradation17);
				add(gradation18);
				add(gradation19);
				add(gradation20);
				add(gradation21);
			}
		};

		private Bullet(LivingEntity shooter, Location startLocation) {
			this.shooter = shooter;
			this.boundingBox = CenteredBoundingBox.of(startLocation, -.75, -.75, -.75, .75, .75, .75);
			this.lastLocation = startLocation;
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity.equals(shooter))
						return false;
					if (entity instanceof Player) {
						if (!getGame().isParticipating(entity.getUniqueId())
								|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
										.getDeathManager().isExcluded(entity.getUniqueId()))
								|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
							return false;
						}
						if (getGame() instanceof Teamable) {
							final Teamable teamGame = (Teamable) getGame();
							final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()),
									participant = teamGame.getParticipant(shooter.getUniqueId());
							if (participant != null) {
								return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
										|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
						}
					}
					return true;
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
		}

		private Location lastLocation;

		private void updateLocation(final Location newLocation) {
			if (turns)
				stacks++;
			else
				stacks--;
			if (stacks % (gradations.size() - 1) == 0) {
				turns = !turns;
			}
			gradation = gradations.get(stacks);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()),
						unit = vectorBetween.clone().normalize().multiply(.1);
				private final int amount = (int) (vectorBetween.length() / .1);
				private int cursor = 0;

				@Override
				public boolean hasNext() {
					return cursor < amount;
				}

				@Override
				public Location next() {
					if (cursor >= amount)
						throw new NoSuchElementException();
					cursor++;
					return lastLocation.clone().add(unit.clone().multiply(cursor));
				}
			};iterator.hasNext();) {
				final Location location = iterator.next();
				boundingBox.setCenter(location);
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, shooter.getWorld(),
						boundingBox, predicate)) {
					if (!shooter.equals(damageable) && !hitcheck.contains(damageable) && !damageable.isInvulnerable()) {
						if (protection.isRunning()) {
							final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), 0.5,
									RegainReason.CUSTOM);
							Bukkit.getPluginManager().callEvent(event);
							if (!event.isCancelled()) {
								Healths.setHealth(getPlayer(), getPlayer().getHealth() + 0.5);
							}
						} else {
							if (constellation == 6) {
								Damages.damageMagic(damageable, (Player) shooter, false, 3);
							} else {
								Damages.damageMagic(damageable, (Player) shooter, false, 5);
							}
						}
						if (constellation == 5) {
							int random = new Random().nextInt(10);
							if (random == 0) {
								if (damageable instanceof Player) {
									Player p = (Player) damageable;
									Charm.apply(getGame().getParticipant(p), TimeUnit.SECONDS, 2, getPlayer(), 30, 30);
								}
							}
						}
						if (attackcount.containsKey(this)) {
							attackcount.put(this, (attackcount.get(this) + 1));
							if (attackcount.get(this) >= 5) {
								if (!bullets.bullets.isEmpty()) {
									bullets.bullets.remove(this);
								}
							}
						} else {
							attackcount.put(this, 1);
						}
						new AbilityTimer(10) {

							@Override
							public void onStart() {
								hitcheck.add(damageable);
							}

							@Override
							public void onEnd() {
								onSilentEnd();
							}

							@Override
							public void onSilentEnd() {
								hitcheck.remove(damageable);
							}

						}.setPeriod(TimeUnit.TICKS, 1).start();
					}
				}
				ParticleLib.REDSTONE.spawnParticle(location, gradation);
			}
			lastLocation = newLocation;
		}

	}

	public class Bullet2 extends AbilityTimer implements Listener {

		private final LivingEntity shooter;
		private final CustomEntity entity;
		private final Predicate<Entity> predicate;
		private final Set<Player> hitcheck = new HashSet<>();
		private Vector forward;
		private int stacks = 0;
		private boolean turns = true;
		private boolean add = false;
		private Arrow arrow;
		private boolean hit;
		private double length;
		private Location arrowLoc;
		private boolean spawnAEC = true;

		private RGB gradation;

		private final RGB gradation1 = RGB.of(1, 1, 1), gradation2 = RGB.of(1, 2, 17), gradation3 = RGB.of(2, 4, 34),
				gradation4 = RGB.of(3, 6, 52), gradation5 = RGB.of(4, 8, 70), gradation6 = RGB.of(5, 10, 87),
				gradation7 = RGB.of(7, 13, 106), gradation8 = RGB.of(8, 15, 123), gradation9 = RGB.of(10, 18, 140),
				gradation10 = RGB.of(12, 20, 150), gradation11 = RGB.of(14, 22, 159), gradation12 = RGB.of(16, 23, 168),
				gradation13 = RGB.of(19, 25, 179), gradation14 = RGB.of(22, 27, 189), gradation15 = RGB.of(24, 28, 199),
				gradation16 = RGB.of(50, 21, 203), gradation17 = RGB.of(74, 15, 207), gradation18 = RGB.of(112, 5, 212),
				gradation19 = RGB.of(122, 25, 188), gradation20 = RGB.of(131, 44, 166),
				gradation21 = RGB.of(140, 61, 145);

		private List<RGB> gradations = new ArrayList<RGB>() {
			{
				add(gradation1);
				add(gradation2);
				add(gradation3);
				add(gradation4);
				add(gradation5);
				add(gradation6);
				add(gradation7);
				add(gradation8);
				add(gradation9);
				add(gradation10);
				add(gradation11);
				add(gradation12);
				add(gradation13);
				add(gradation14);
				add(gradation15);
				add(gradation16);
				add(gradation17);
				add(gradation18);
				add(gradation19);
				add(gradation20);
				add(gradation21);
			}
		};

		private Location lastLocation;

		private Bullet2(LivingEntity shooter, Location startLocation, Arrow arrow, double length) {
			super(200);
			setPeriod(TimeUnit.TICKS, 1);
			RainStar.this.bullet2 = this;
			this.shooter = shooter;
			this.arrow = arrow;
			this.length = length;
			this.entity = new Bullet2.ArrowEntity(startLocation.getWorld(), startLocation.getX(), startLocation.getY(),
					startLocation.getZ()).resizeBoundingBox(-.75, -.75, -.75, .75, .75, .75);
			this.lastLocation = startLocation;
			this.forward = arrow.getLocation().clone().subtract(entity.getLocation().clone()).toVector().normalize()
					.multiply(length);
			this.predicate = new Predicate<Entity>() {
				@Override
				public boolean test(Entity entity) {
					if (entity.equals(shooter))
						return false;
					if (entity instanceof ArmorStand)
						return false;
					if (entity instanceof Player) {
						if (entity.equals(getPlayer()))
							return true;
						if (!getGame().isParticipating(entity.getUniqueId())
								|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame())
										.getDeathManager().isExcluded(entity.getUniqueId()))
								|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
							return false;
						}
						if (getGame() instanceof Teamable) {
							final Teamable teamGame = (Teamable) getGame();
							final Participant entityParticipant = teamGame.getParticipant(entity.getUniqueId()),
									participant = teamGame.getParticipant(shooter.getUniqueId());
							if (participant != null) {
								return !teamGame.hasTeam(entityParticipant) || !teamGame.hasTeam(participant)
										|| (!teamGame.getTeam(entityParticipant).equals(teamGame.getTeam(participant)));
							}
						}
					}
					if (hitcheck.contains(entity))
						return false;
					return true;
				}

				@Override
				public boolean apply(@Nullable Entity arg0) {
					return false;
				}
			};
		}

		@Override
		protected void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}

		@Override
		protected void run(int i) {
			if (constellation == 1)
				arrow.setKnockbackStrength(arrow.getKnockbackStrength() + 1);
			if (hit) {
				Damageable nearest = LocationUtil.getNearestEntity(Damageable.class, entity.getLocation(), predicate);
				if (nearest != null) {
					this.forward = nearest.getLocation().add(0, 1, 0).clone().subtract(lastLocation.clone()).toVector()
							.normalize().multiply(length);
				} else {
					stop(false);
				}
			} else
				this.forward = arrow.getLocation().clone().subtract(entity.getLocation().clone()).toVector().normalize()
						.multiply(length);
			Location newLocation = lastLocation.clone().add(forward);
			for (Iterator<Location> iterator = new Iterator<Location>() {
				private final Vector vectorBetween = newLocation.toVector().subtract(lastLocation.toVector()),
						unit = vectorBetween.clone().normalize().multiply(.1);
				private final int amount = (int) (vectorBetween.length() / 0.1);
				private int cursor = 0;

				@Override
				public boolean hasNext() {
					return cursor < amount;
				}

				@Override
				public Location next() {
					if (cursor >= amount)
						throw new NoSuchElementException();
					cursor++;
					return lastLocation.clone().add(unit.clone().multiply(cursor));
				}
			};iterator.hasNext();) {
				final Location location = iterator.next();
				entity.setLocation(location);
				for (Damageable damageable : LocationUtil.getConflictingEntities(Damageable.class, entity.getWorld(),
						entity.getBoundingBox(), predicate)) {
					if (!shooter.equals(damageable)) {
						if (constellation == 8) {
							Damages.damageArrow(damageable, getPlayer(), 9);
						} else {
							Damages.damageArrow(damageable, getPlayer(), 7);
						}
						if (constellation == 1)
							damageable.setVelocity(damageable.getVelocity().multiply(new Vector(1.1, 1, 1.1)));
						if (constellation == 3) {
							int random = new Random().nextInt(5);
							if (random == 0) {
								Location hitloc = damageable.getLocation().clone();
								if (damageable instanceof Player) {
									Stun.apply(getGame().getParticipant((Player) damageable), TimeUnit.TICKS, 7);
								}
								new BukkitRunnable() {
									@Override
									public void run() {
										hitloc.getWorld().spawn(hitloc, EvokerFangs.class);
									}
								}.runTaskLater(AbilityWar.getPlugin(), 3L);
								new BukkitRunnable() {
									@Override
									public void run() {
										for (Location loc : crabcircle1.toLocations(hitloc).floor(hitloc.getY())) {
											loc.getWorld().spawn(loc, EvokerFangs.class);
										}
									}
								}.runTaskLater(AbilityWar.getPlugin(), 6L);
								new BukkitRunnable() {
									@Override
									public void run() {
										for (Location loc : crabcircle2.toLocations(hitloc).floor(hitloc.getY())) {
											loc.getWorld().spawn(loc, EvokerFangs.class);
										}
									}
								}.runTaskLater(AbilityWar.getPlugin(), 9L);
							}
						}
						if (constellation == 7 && damageable instanceof Player) {
							Player p = (Player) damageable;
							p.addPotionEffect(poison);
						}
						if (constellation == 10 && spawnAEC) {
							AreaEffectCloud AEC = damageable.getWorld().spawn(damageable.getLocation().add(0, 0.2, 0),
									AreaEffectCloud.class);
							AEC.setDuration(100);
							AEC.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 200, 0), true);
							AEC.setColor(Color.BLACK);
							AEC.setWaitTime(0);
							spawnAEC = false;
						}
						if (constellation == 11) {
							new Grab(damageable, arrowLoc).start();
						}
						this.stop(false);
					}
				}
				add = !add;
				if (add) {
					if (turns)
						stacks++;
					else
						stacks--;
					if (stacks % (gradations.size() - 1) == 0) {
						turns = !turns;
					}
					gradation = gradations.get(stacks);
				}
				ParticleLib.REDSTONE.spawnParticle(location, gradation);
			}
			lastLocation = newLocation;
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			hitcheck.clear();
			entity.remove();
			RainStar.this.bullet2 = null;
		}

		@EventHandler
		private void onProjectileHit(final ProjectileHitEvent e) {
			if (getPlayer().equals(e.getEntity().getShooter()) && e.getEntity().equals(arrow)) {
				if (e.getHitEntity() != null) {
					if (!e.getHitEntity().equals(getPlayer())) {
						if (constellation == 3) {
							int random = new Random().nextInt(5);
							if (random == 0) {
								Location hitloc = e.getHitEntity().getLocation().clone();
								if (e.getHitEntity() instanceof Player) {
									Stun.apply(getGame().getParticipant((Player) e.getHitEntity()), TimeUnit.TICKS, 7);
								}
								new BukkitRunnable() {
									@Override
									public void run() {
										hitloc.getWorld().spawn(hitloc, EvokerFangs.class);
									}
								}.runTaskLater(AbilityWar.getPlugin(), 3L);
								new BukkitRunnable() {
									@Override
									public void run() {
										for (Location loc : crabcircle1.toLocations(hitloc).floor(hitloc.getY())) {
											loc.getWorld().spawn(loc, EvokerFangs.class);
										}
									}
								}.runTaskLater(AbilityWar.getPlugin(), 6L);
								new BukkitRunnable() {
									@Override
									public void run() {
										for (Location loc : crabcircle2.toLocations(hitloc).floor(hitloc.getY())) {
											loc.getWorld().spawn(loc, EvokerFangs.class);
										}
									}
								}.runTaskLater(AbilityWar.getPlugin(), 9L);
							}
						}
						if (constellation == 7 && e.getHitEntity() instanceof Player) {
							Player p = (Player) e.getHitEntity();
							p.addPotionEffect(poison);
						}
						if (constellation == 10 && spawnAEC) {
							AreaEffectCloud AEC = e.getHitEntity().getWorld()
									.spawn(e.getHitEntity().getLocation().add(0, 0.2, 0), AreaEffectCloud.class);
							AEC.setDuration(100);
							AEC.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 200, 0), true);
							AEC.setColor(Color.BLACK);
							AEC.setWaitTime(0);
							spawnAEC = false;
						}
						e.getHitEntity().setFireTicks(e.getHitEntity().getFireTicks() + 100);
						stop(false);
					}
				}
				if (e.getHitBlock() != null) {
					if (constellation == 8) {
						setCount(20);
					} else {
						setCount(10);
					}
					if (constellation == 11)
						arrowLoc = arrow.getLocation().clone();
					hit = true;
				}
			}
		}

		public class ArrowEntity extends CustomEntity implements Deflectable {

			public ArrowEntity(World world, double x, double y, double z) {
				getGame().super(world, x, y, z);
			}

			@Override
			public Vector getDirection() {
				return forward.clone();
			}

			@Override
			public void onDeflect(Participant deflector, Vector newDirection) {
				stop(false);
				final Player deflectedPlayer = deflector.getPlayer();
				new Bullet2(deflectedPlayer, lastLocation, arrow, length).start();
			}

			@Override
			public ProjectileSource getShooter() {
				return shooter;
			}

			@Override
			protected void onRemove() {
			}

		}

	}

	class Grab extends AbilityTimer {

		private Damageable damageable;
		private Location arrowLocation;

		private Grab(Damageable damageable, Location arrowLocation) {
			super(40);
			this.damageable = damageable;
			this.arrowLocation = arrowLocation;
			setPeriod(TimeUnit.TICKS, 1);
		}

		@Override
		protected void onStart() {
			SoundLib.ENTITY_FISHING_BOBBER_RETRIEVE.playSound(damageable.getLocation(), 1, 0.5f);
		}

		@Override
		protected void run(int count) {
			if (damageable != null && arrowLocation != null && damageable.getLocation() != null ) {
				if (damageable.getLocation().distanceSquared(arrowLocation) >= 4) {
					damageable.setVelocity(VectorUtil.validateVector(arrowLocation.toVector()
							.subtract(damageable.getLocation().toVector()).normalize().multiply(3)));
				} else {
					damageable.setVelocity(new Vector(0, 0, 0));
					stop(false);
				}	
			}
		}

	}

}