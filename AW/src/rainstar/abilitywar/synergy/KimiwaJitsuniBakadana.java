package rainstar.abilitywar.synergy;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsSynergySettings;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.effect.Idiot;

@AbilityManifest(name = "너는 정말 바보구나", rank = Rank.A, species = Species.OTHERS, explain = {
		"철괴 우클릭 시, $[RANGE]칸 내의 모든 대상에게 §c§n멍청함§f을 $[IDIOT_DURATION]초 부여합니다. $[COOLDOWN]",
		"§8[§c멍청함§8]§f 능력 사용법을 잊어버려 능력이 비활성화됩니다.",
		" 공격을 하면 §84.1§f초 후에 그 사실을 잊어버려 피해를 받은 사람이 다시 회복합니다.",
		" 이때, §c31§f% 확률로 자신의 시야가 무작위로 돌아갑니다."
		})
public class KimiwaJitsuniBakadana extends Synergy implements ActiveHandler {

	public KimiwaJitsuniBakadana(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> RANGE = rsSynergySettings.new SettingObject<Double>(KimiwaJitsuniBakadana.class,
			"range", 13.0, "# 멍청함 부여 범위", "# 단위: 칸") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Double> IDIOT_DURATION = rsSynergySettings.new SettingObject<Double>(KimiwaJitsuniBakadana.class,
			"idiot-duration", 10.0, "# 멍청함 지속시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> COOLDOWN = rsSynergySettings.new SettingObject<Integer>(KimiwaJitsuniBakadana.class,
			"cooldown", 40, "# 쿨타임", "# 주의! 쿨타임 감소 효과를 50%까지만 받습니다.") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	private final double range = RANGE.getValue();
	private final int idiotDuration = (int) (IDIOT_DURATION.getValue() * 20);
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), CooldownDecrease._25);
	private Circle circle = Circle.of(range, (int) Math.min(200, range * 10));
	private final static RGB tetoColor = RGB.of(239, 119, 147);
	
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
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
	private AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			for (Location loc : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
				ParticleLib.REDSTONE.spawnParticle(getPlayer(), loc, tetoColor);
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	private final AbilityTimer music = new AbilityTimer(TaskType.NORMAL, 10) {
		
		@Override
		public void run(int count) {
			switch(count) {
			case 0:
				SoundLib.FLUTE.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.F));
				break;
			case 1:
			case 2:
			case 3:
			case 5:
				SoundLib.FLUTE.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
				break;
			case 4:
				SoundLib.FLUTE.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.B));
				break;
			case 6:
			case 9:
				SoundLib.FLUTE.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				break;
			case 7:
				SoundLib.FLUTE.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
				break;
			case 8:
				SoundLib.FLUTE.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.B));
				break;
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 4).register();
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
			if (!cooldown.isCooldown()) {
				for (Player player : LocationUtil.getNearbyEntities(Player.class, getPlayer().getLocation(), range, range, predicate)) {
					Idiot.apply(getGame().getParticipant(player), TimeUnit.TICKS, idiotDuration);
				}
				
				music.start();
				return cooldown.start();
			}
		}
		return false;
	}
	
}
