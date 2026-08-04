package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.utils.annotations.Beta;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.concurrent.SimpleTimer.TaskType;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.utils.RankColor;

@AbilityManifest(name = "전생", rank = Rank.L, species = Species.HUMAN, explain = {
		"§d최대 체력§f이 §c90% 감소§f합니다. 받는 피해량이 §c$[DAMAGE_INCREASE]%§f 증가합니다.",
		"치명적인 피해를 받으면 §d최대 체력§f을 §d10% 복구§f받고 §d최대 체력§f으로 §a부활§f합니다.",
		"§d최대 체력§f을 전부 복구받고 나면 §a부활§f할 수 없습니다.",
		"무작위 §cSPECIAL 등급 능력§f을 하나 부여받습니다. 부활할 때마다 새로운 능력을",
		"받으며, 그 능력의 등급은 §c감소된 최대 체력의 확률§f로 강등될 수 있습니다.",
		"§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-",
		"$(EXPLAIN)",
		"§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-§a-§3-§b-",
		"§b[§7아이디어 제공자§b] §4okheebaek"
		})
public class Tensei extends AbilityBase implements ActiveHandler, TargetHandler {
	
	public Tensei(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> DAMAGE_INCREASE = rsAbilitySettings.new SettingObject<Integer>(Tensei.class,
			"damage-increase", 20, "# 항시 받는 피해량 증가량", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private final double damageIncrease = DAMAGE_INCREASE.getValue() * 0.01;
	private final Random random = new Random();
	private Rank nowRank = Rank.SPECIAL;
	private AbilityBase ability;
	private double maxHealth = 0;
	private double nowMaxHealth = 0;
	private int stack = 1;
	private int musicstack = 0;
	
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (maxHealth == 0) {
				maxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				nowMaxHealth = maxHealth * 0.1;
				reincarnation();
			}
			
			if (getPlayer().getHealth() > nowMaxHealth) Healths.setHealth(getPlayer(), nowMaxHealth);
			getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(nowMaxHealth);
		}
		
		if (update == Update.RESTRICTION_SET || update == Update.ABILITY_DESTROY) {
			if (maxHealth > 0) {
				getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
				Healths.setHealth(getPlayer(), maxHealth);
			}
		}
		
		if (update == Update.RESTRICTION_CLEAR) {
			if (ability != null) {
				ability.setRestricted(false);
			}
		} else if (update == Update.RESTRICTION_SET) {
			if (ability != null) {
				ability.setRestricted(true);
			}
		} else if (update == Update.ABILITY_DESTROY) {
			if (ability != null) {
				ability.destroy();
				ability = null;
			}
		}
	}
	
	public AbilityRegistration getRandomAbility(Rank rank) {
		final List<AbilityRegistration> registrations = AbilityList.values().stream().filter(
				ability -> ability.getAbilityClass().getAnnotation(Beta.class) == null && ability.isAvailable(getGame().getClass()) && !Configuration.Settings.isBlacklisted(ability.getManifest().name()) && ability.getManifest().rank().equals(rank)
		).collect(Collectors.toList());
		return registrations.isEmpty() ? null : random.pick(registrations);
	}
	
	public void reincarnation() {
		if (ability != null) {
			ability.destroy();
			ability = null;
		}
		AbilityRegistration clazz = null;
		clazz = getRandomAbility(nowRank);
		try {
			ability = AbilityBase.create(clazz, getParticipant());
			ability.setRestricted(false);
			NMS.sendTitle(getPlayer(), "§a§l" + stack + "§b§l회차", RankColor.getColor(ability.getRank()) + ability.getDisplayName(), 0, 100, 1);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
		switch(musicstack) {
		case 0:
			music1.start();
			break;
		case 1:
			music2.start();
			break;
		case 2:
			music3.start();
			break;
		case 3:
			music4.start();
			break;
		}
		musicstack = (musicstack == 3) ? 0 : musicstack + 1;
		
		new AbilityTimer(5) {
			
			@Override
			public void onEnd() {
				NMS.clearTitle(getPlayer());
			}
			
		}.setPeriod(TimeUnit.SECONDS, 1).start();
	}
	
	private final AbilityTimer music1 = new AbilityTimer(TaskType.NORMAL, 29) {

		@Override
		public void run(int count) {
			switch (count) {
			case 0:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.C));
				break;

			case 2:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 4:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 6:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 8:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 10:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.C));
				break;

			case 12:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				break;

			case 14:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 16:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 18:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 20:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 22:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.C));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
				break;

			case 24:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.G));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
				break;

			case 26:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.G));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
				break;

			case 28:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.C));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
				break;
			}
		}

	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	private final AbilityTimer music2 = new AbilityTimer(TaskType.NORMAL, 29) {

		@Override
		public void run(int count) {
			switch (count) {
			case 0:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.C));
				break;

			case 2:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 4:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 6:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 8:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 10:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.C));
				break;

			case 12:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				break;

			case 14:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 16:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 18:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 20:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 22:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 24:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
				break;

			case 26:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 28:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.F));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.sharp(1, Tone.A));
				break;
			}
		}

	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	private final AbilityTimer music3 = new AbilityTimer(TaskType.NORMAL, 29) {

		@Override
		public void run(int count) {
			switch (count) {
			case 0:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				break;

			case 2:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 4:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 6:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 8:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.C));
				break;

			case 10:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.G));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
				break;

			case 12:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.G));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
				break;

			case 14:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 16:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 18:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 20:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 22:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.C));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
				break;

			case 24:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.G));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
				break;

			case 26:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.G));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.G));
				break;

			case 28:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.sharp(0, Tone.C));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
				break;
			}
		}

	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	private final AbilityTimer music4 = new AbilityTimer(TaskType.NORMAL, 29) {

		@Override
		public void run(int count) {
			switch (count) {
			case 0:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
				break;

			case 3:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
				break;

			case 4:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;

			case 8:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
				break;

			case 11:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
				break;

			case 12:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.A));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.F));
				break;
				
			case 17:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.E));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.E));
				break;
			case 18:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.D));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.D));
				break;
			case 19:
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(0, Tone.C));
				SoundLib.PIANO.playInstrument(getPlayer().getLocation(), Note.natural(1, Tone.C));
				break;
			}
		}

	}.setPeriod(TimeUnit.TICKS, 2).register();
	
	@SuppressWarnings("unused")
	private final Object EXPLAIN = new Object() {
		@Override
		public String toString() {
			final StringJoiner joiner = new StringJoiner("\n");
			if (ability == null) joiner.add("?");
			else {
				joiner.add("§b" + ability.getName() + " " + ability.getRank().getRankName() + " " + ability.getSpecies().getSpeciesName());
				for (final Iterator<String> iterator = ability.getExplanation(); iterator.hasNext();) {
					joiner.add("§f" + iterator.next());
				}
			}
			return joiner.toString();
		}
	};
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (ability != null) {
			return ability instanceof ActiveHandler && ((ActiveHandler) ability).ActiveSkill(material, clickType);	
		}
		return false;
	}
	
	@Override
	public void TargetSkill(Material material, LivingEntity entity) {
		if (ability != null) {
			if (ability instanceof TargetHandler) {
			((TargetHandler) ability).TargetSkill(material, entity);
			}
		}
	}
	
	@Override
	public Set<GameTimer> getTimers() {
		return ability != null ? SetUnion.union(super.getTimers(), ability.getTimers()) : super.getTimers();
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		return ability != null ? SetUnion.union(super.getRunningTimers(), ability.getRunningTimers()) : super.getRunningTimers();
	}

	@Override
	public boolean usesMaterial(Material material) {
		if (ability != null) {
			return ability.usesMaterial(material);
		}
		return super.usesMaterial(material);
	}
	
	@SubscribeEvent(priority = 1001)
	private void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() - e.getFinalDamage() <= 0) {
			if (nowMaxHealth != maxHealth) {
				e.setCancelled(true);
				stack++;
				nowMaxHealth = (maxHealth * stack * 0.1);
				getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(nowMaxHealth);
				Healths.setHealth(getPlayer(), nowMaxHealth);
				ParticleLib.TOTEM.spawnParticle(getPlayer().getLocation(), 0, 0, 0, 100, 1);
				SoundLib.ITEM_TOTEM_USE.playSound(getPlayer().getLocation(), 1, 1.25f);
				
				double currentMaxHealth = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				double decreaseRate = Math.max(0, (maxHealth - currentMaxHealth) / maxHealth);
				
				if (random.nextDouble() < decreaseRate) {
					switch(nowRank) {
					case SPECIAL:
						nowRank = Rank.L;
						break;
					case L:
						nowRank = Rank.S;
						break;
					case S:
						nowRank = Rank.A;
						break;
					case A:
						nowRank = Rank.B;
						break;
					case B:
						nowRank = Rank.C;
						break;
					case C:
						break;
					}
				}
				reincarnation();
			}
		}
	}
	
	@SubscribeEvent(priority = 1001)
	private void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = 1001)
	private void onEntityDamageByEntityRevive(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = 1)
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getEntity().equals(getPlayer())) e.setDamage(e.getDamage() * (1 + damageIncrease));
	}
	
}
