package RainStarAbility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.Configuration;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.google.common.base.Predicate;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "카이로스", rank = Rank.S, species = Species.GOD, explain = {
		"기회의 신 카이로스.",
		"§7철괴 우클릭 §8- §a기회 부여§f: 다른 플레이어를 15칸 내에서 우클릭 해 대상의 능력이",
		" §dS 등급§f보다 낮을 경우 최소 한 등급 이상의 능력으로 재추첨합니다.",
		" 이때 대상에게 바꿔준 능력을 알 수 있습니다. $[CooldownConfig]",
		" 또한 바꿔준 대상에게서 기회 체력을 1칸 적립합니다.",
		"§7패시브 §8- §a마지막 기회§f: 치명적인 피해를 입었을 때 단 한 번",
		" 사망하지 않고 대신 체력 반 칸으로 부활합니다. 이때, 다른 플레이어에게",
		" 적립한 기회 체력만큼 각 대상의 체력을 가져와 즉시 회복하고, 회복한 체력이",
		" 전체 체력의 절반 이상일 경우 이 능력을 무작위로 재추첨합니다.",
		" 발동 이후 기회 부여 능력은 사용할 수 없습니다."})

@Tips(tip = {
        "카이로스는 다른 플레이어의 능력을 승격시켜 또 한 번의",
        "기회를 줌으로써, 대상과 자신 둘 다 이득을 취할 수 있는",
        "특별한 능력입니다. 또한 기회 체력을 일정 이상 적립하면",
        "카이로스 본인의 능력이 새로운 기회로 바뀌어 운이 좋다면",
        "상황을 타개할 강력한 능력이 되어 돌아올 수 있습니다.",
        "카이로스의 능력 변경 능력은 팀 게임에서 그 위력을 더욱 보여주는데,",
        "팀원의 불필요한 저등급 능력을 고등급 능력으로 바꿔칠 수 있습니다."
}, strong = {
        @Description(subject = "변수 창출", explain = {
                "대상의 능력을 바꾼다는 것은, 무한한 변수를",
                "창출할 수 있습니다. 또한 부활 후 능력 변경도",
                "변수를 창출하기 좋습니다."
        }),
        @Description(subject = "강력한 저등급 능력", explain = {
                "스토커 등 A등급이거나 그 이하임에도 강력한 성능을",
                "뽐내는 능력을 오히려 승격시켜 위기에서 탈출해보세요."
        }),
        @Description(subject = "팀 게임", explain = {
                "상술했듯 팀원의 불필요한 능력을 승격시켜",
                "좋은 능력으로 바꿔줄 수 있습니다."
        }),
        @Description(subject = "운", explain = {
                "상대나 자신이 원하는 능력이 걸리게 하려면",
                "상당한 행운이 필요합니다."
        })
}, weak = {
        @Description(subject = "고등급 능력", explain = {
                "고등급 능력에게 카이로스가 할 수 있는 것은 없습니다.",
                "전투를 가급적으로 피하세요."
        }),
        @Description(subject = "운", explain = {
                "상대나 자신이 원하는 능력이 걸리게 하려면",
                "상당한 행운이 필요합니다."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.SEVEN, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.EIGHT), difficulty = Difficulty.EASY)

public class Kairos extends AbilityBase implements ActiveHandler {
	
	public Kairos(Participant participant) {
		super(participant);
	}

	private final Cooldown cooldown = new Cooldown(CooldownConfig.getValue());
	private final ActionbarChannel ac = newActionbarChannel();
	private int stack = 0;
	private static final Circle circle = Circle.of(0.5, 30);
	private static final RGB gold = RGB.of(254, 228, 1);
	private Participant target = null;
	private Set<UUID> inv = new HashSet<>();
	private Map<Participant, Integer> getHp = new HashMap<>();
	private static final Random random = new Random();
	private boolean rebirth = false;
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler && ((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean apply(@Nullable Entity arg0) {
			return false;
		}
	};
	
	public static final SettingObject<Integer> CooldownConfig = abilitySettings.new SettingObject<Integer>(Kairos.class,
			"Cooldown", 20, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public AbilityRegistration getRandomAbility(Participant target, Rank rank) {
		final Set<AbilityRegistration> usedAbilities = new HashSet<>();
		for (Participant participant : getGame().getParticipants()) {
			if (participant.hasAbility() && participant.attributes().TARGETABLE.getValue()) {
				usedAbilities.add(participant.getAbility().getRegistration());
			}
		}
		
		final int criterion = rank.ordinal();
		final List<AbilityRegistration> registrations = AbilityList.values().stream().filter(
				ability -> !Configuration.Settings.isBlacklisted(ability.getManifest().name()) &&
				!usedAbilities.contains(ability) && (getParticipant().equals(target) || ability.getManifest().rank().ordinal() < criterion)
		).collect(Collectors.toList());
		return registrations.isEmpty() ? null : random.pick(registrations);
	}

	@Override
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && clicktype == ClickType.RIGHT_CLICK
				&& !cooldown.isCooldown() && !rebirth) {
			Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 15, predicate);
			if (player != null) {
				target = getGame().getParticipant(player);
				if (target.hasAbility() && !target.getAbility().isRestricted()) {
					AbilityBase ab = target.getAbility();
					if (ab.getRank().equals(Rank.C) || ab.getRank().equals(Rank.B) || ab.getRank().equals(Rank.A)) {
						try {
							target.setAbility(getRandomAbility(target, ab.getRank()).getAbilityClass());
						} catch (UnsupportedOperationException | ReflectiveOperationException e) {
							e.printStackTrace();
						}
						getPlayer().sendMessage("§e" + player.getName() + "§f님에게 §a기회§f를 드립니다.");
						getPlayer().sendMessage("이제 대상의 능력은 §e" + target.getAbility().getDisplayName() + "§f입니다.");
						target.getPlayer().sendMessage("당신에게 새 기회가 주어졌습니다. 이제 당신의 능력은 §e" + target.getAbility().getDisplayName() + "§f입니다.");
						new AbilityTimer(60) {

							private ActionbarChannel actionbarChannel;

							@Override
							protected void run(int count) {
								actionbarChannel.update("§7무적 §f: " + (getCount() / 10.0) + "초");
							}

							@Override
							protected void onStart() {
								inv.add(target.getPlayer().getUniqueId());
								actionbarChannel = target.actionbar().newChannel();
							}

							@Override
							protected void onEnd() {
								inv.remove(target.getPlayer().getUniqueId());
								if (actionbarChannel != null)
									actionbarChannel.unregister();
							}

							@Override
							protected void onSilentEnd() {
								if (actionbarChannel != null)
									actionbarChannel.unregister();
							}
						}.setPeriod(TimeUnit.TICKS, 1).start();
						getHp.put(target, getHp.getOrDefault(target, 0) + 2);
						SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(target.getPlayer(), 1, 1.3f);
						SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.5f);
						cooldown.start();
						stack++;
						ac.update("기회를 준 횟수: " + stack);
						return true;
					} else if (ab.getClass().equals(Mix.class)) {
						final Mix mix = (Mix) ab;
						final AbilityBase first = mix.getFirst(), second = mix.getSecond();
						
						if (mix.hasSynergy()) {
							getPlayer().sendMessage("§e" + player.getName() + "§f님은 §c기회§f를 받을 필요가 없습니다.");
							SoundLib.ENTITY_ENDERMAN_HURT.playSound(getPlayer(), 1, 0.7f);
							return false;
						}
						final Mix myAbility = (Mix) getParticipant().getAbility();
						if (this.equals(myAbility.getFirst())) {
							final boolean firstStatus = first.getRank().equals(Rank.C) || first.getRank().equals(Rank.B) || first.getRank().equals(Rank.A);
							if (firstStatus) {
								Class<? extends AbilityBase> firstClass = first.getClass();
								if (firstStatus) firstClass = getRandomAbility(target, first.getRank()).getAbilityClass();
								try {
									mix.setAbility(firstClass, mix.getSecond().getClass());
								} catch (ReflectiveOperationException e) {
									e.printStackTrace();
								}
								getPlayer().sendMessage("§e" + player.getName() + "§f님에게 §a기회§f를 드립니다.");
								getPlayer().sendMessage("이제 대상의 능력은 §e" + mix.getFirst().getDisplayName() + "§f입니다.");
								target.getPlayer().sendMessage("당신에게 새 기회가 주어졌습니다. 이제 당신의 능력은 §e" + mix.getFirst().getDisplayName() + "§f, §e" + mix.getSecond().getDisplayName() + "§f입니다.");
								new AbilityTimer(60) {

									private ActionbarChannel actionbarChannel;

									@Override
									protected void run(int count) {
										actionbarChannel.update("§8무적§f: " + (getCount() / 20.0) + "초");
									}

									@Override
									protected void onStart() {
										inv.add(target.getPlayer().getUniqueId());
										actionbarChannel = target.actionbar().newChannel();
									}

									@Override
									protected void onEnd() {
										inv.remove(target.getPlayer().getUniqueId());
										if (actionbarChannel != null)
											actionbarChannel.unregister();
									}

									@Override
									protected void onSilentEnd() {
										if (actionbarChannel != null)
											actionbarChannel.unregister();
									}
								}.setPeriod(TimeUnit.TICKS, 1).start();
								getHp.put(target, getHp.getOrDefault(target, 0) + 2);
								SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(target.getPlayer(), 1, 1.3f);;
								SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.5f);
								cooldown.start();
								stack++;
								ac.update("§b기회를 준 횟수§f: " + stack);
								return true;
							} else {
								getPlayer().sendMessage("§e" + player.getName() + "§f님은 §c기회§f를 받을 필요가 없습니다.");
								SoundLib.ENTITY_ENDERMAN_HURT.playSound(getPlayer(), 1, 0.7f);
								return false;
							}
						} else if (this.equals(myAbility.getSecond())) {
							final boolean secondStatus = second.getRank().equals(Rank.C) || second.getRank().equals(Rank.B) || second.getRank().equals(Rank.A);
							if (secondStatus) {
								Class<? extends AbilityBase> secondClass = second.getClass();
								if (secondStatus) secondClass = getRandomAbility(target, second.getRank()).getAbilityClass();
								try {
									mix.setAbility(mix.getFirst().getClass(), secondClass);
								} catch (ReflectiveOperationException e) {
									e.printStackTrace();
								}
								getPlayer().sendMessage("§e" + player.getName() + "§f님에게 §a기회§f를 드립니다.");
								getPlayer().sendMessage("이제 대상의 능력은 §e" + mix.getSecond().getDisplayName() + "§f입니다.");
								target.getPlayer().sendMessage("당신에게 새 기회가 주어졌습니다. 이제 당신의 능력은 §e" + mix.getFirst().getDisplayName() + "§f, §e" + mix.getSecond().getDisplayName() + "§f입니다.");
								new AbilityTimer(60) {

									private ActionbarChannel actionbarChannel;

									@Override
									protected void run(int count) {
										actionbarChannel.update("§8무적§f: " + (getCount() / 20.0) + "초");
									}

									@Override
									protected void onStart() {
										inv.add(target.getPlayer().getUniqueId());
										actionbarChannel = target.actionbar().newChannel();
									}

									@Override
									protected void onEnd() {
										inv.remove(target.getPlayer().getUniqueId());
										if (actionbarChannel != null)
											actionbarChannel.unregister();
									}

									@Override
									protected void onSilentEnd() {
										if (actionbarChannel != null)
											actionbarChannel.unregister();
									}
								}.setPeriod(TimeUnit.TICKS, 1).start();
								getHp.put(target, getHp.getOrDefault(target, 0) + 2);
								SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(target.getPlayer(), 1, 1.3f);;
								SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.5f);
								cooldown.start();
								stack++;
								ac.update("§b기회를 준 횟수§f: " + stack);
								return true;
							} else {
								getPlayer().sendMessage("§e" + player.getName() + "§f님은 §c기회§f를 받을 필요가 없습니다.");
								SoundLib.ENTITY_ENDERMAN_HURT.playSound(getPlayer(), 1, 0.7f);
								return false;
							}
						}
					} else {
							getPlayer().sendMessage("§e" + player.getName() + "§f님은 §c기회§f를 받을 필요가 없습니다.");
							SoundLib.ENTITY_ENDERMAN_HURT.playSound(getPlayer(), 1, 0.7f);
							return false;
						}
				} else {
					getPlayer().sendMessage("해당 플레이어는 받을 수 있는 기회가 없습니다.");
					return false;
				}
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (inv.contains(e.getDamager().getUniqueId()) || inv.contains(e.getEntity().getUniqueId())) {
		 	e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && getPlayer().getHealth() - e.getFinalDamage() <= 0 && !rebirth) {
			this.rebirth = true;
			int sum = 0;
			for (final Entry<Participant, Integer> entry : getHp.entrySet()) {
				final int health = entry.getValue();
				final Player player = entry.getKey().getPlayer();
				Healths.setHealth(player, player.getHealth() - health);
				sum += health;
			}
			Healths.setHealth(getPlayer(), 1 + sum);
			if (1 + sum >= (getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2)) {
				if (getParticipant().getAbility().getClass().equals(Mix.class)) {
					final Mix mix = (Mix) getParticipant().getAbility();
					final AbilityBase first = mix.getFirst(), second = mix.getSecond();
					final boolean firstStatus = first.getClass().equals(Kairos.class), 
							secondStatus = second.getClass().equals(Kairos.class);
					if (firstStatus || secondStatus) {
						Class<? extends AbilityBase> firstClass = first.getClass(), secondClass = second.getClass();
						if (firstStatus) firstClass = getRandomAbility(getParticipant(), first.getRank()).getAbilityClass();
						if (secondStatus) secondClass = getRandomAbility(getParticipant(), second.getRank()).getAbilityClass();
						try {
							mix.setAbility(firstClass, secondClass);
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					}
					getPlayer().sendMessage("§b마지막 기회§f를 붙잡아 §e카이로스§f 능력이 바뀌었습니다.");
					getPlayer().sendMessage("현재 능력: §e" + mix.getFirst().getDisplayName() + "§f, §e" + mix.getSecond().getDisplayName());
				} else {
					try {
						getParticipant().setAbility(getRandomAbility(getParticipant(), getParticipant().getAbility().getRank()).getAbilityClass());
					} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
						e1.printStackTrace();
					}
					getPlayer().sendMessage("§b마지막 기회§f를 붙잡아 §e카이로스§f 능력이 §e" + getParticipant().getAbility().getDisplayName() + "§f으로 바뀌었습니다.");
				}
			}
			new AbilityTimer(60) {
				@Override
				protected void run(int count) {
					Location center = getPlayer().getLocation().clone().add(0, 2, 0);
					for (Location loc : circle.toLocations(center)) {
						ParticleLib.REDSTONE.spawnParticle(loc, gold);
					}
				}
			}.setPeriod(TimeUnit.TICKS, 1).start();
			SoundLib.ENTITY_EVOKER_PREPARE_SUMMON.playSound(getPlayer().getLocation(), 1, 1);
			new BukkitRunnable() {
				@Override
				public void run() {
					SoundLib.ENTITY_EVOKER_PREPARE_SUMMON.playSound(getPlayer().getLocation(), 1, 1.3f);
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 10L);
			new BukkitRunnable() {
				@Override
				public void run() {
					SoundLib.ENTITY_EVOKER_PREPARE_SUMMON.playSound(getPlayer().getLocation(), 1, 2);
				}	
			}.runTaskLater(AbilityWar.getPlugin(), 30L);
			stack = 0;
			ac.unregister();
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onEntityShootBow(EntityShootBowEvent e) {
		if (inv.contains(e.getEntity().getUniqueId())) {
			e.setCancelled(true);
		}
	}
}