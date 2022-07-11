package RainStarSynergy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableMap;

@AbilityManifest(
		name = "어비스", rank = Rank.L, species = Species.OTHERS, explain = {
		"§a---------------------------------",
		"$(EXPLAIN)",
		"§a---------------------------------",
		"철괴로 대상을 15칸 내에서 우클릭하여 능력을 두 배로§8(§7능력당 두 개씩§8)§f 복제합니다.",
		"복제한 능력은 웅크린 채 철괴 좌클릭으로 복제를 해제할 수 있습니다. $[COOLDOWN]",
		"능력을 복제한 후 $[MAX_WAIT]초 이내에 복제 해제를 시도하면 해제하지 않고 대신",
		"능력을 분해하여 영구적인 공격력 $[INCREASE]%를 획득 가능합니다.",
		"능력을 분해할 때마다 쿨타임이 매번 현재의 $[COOLDOWN_MULTIPLY]%씩 증가합니다.",
		"복제한 능력과 같은 능력을 가진 대상에겐 능력 하나당 받는 피해가 $[DAMAGE_INCREASE]% 증가합니다."
		})

public class Abyss extends Synergy implements ActiveHandler, TargetHandler {

	public Abyss(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = synergySettings.new SettingObject<Integer>(Abyss.class, "cooldown", 100, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Integer> INCREASE = synergySettings.new SettingObject<Integer>(Abyss.class, "damage-increase", 20, "# 영구 공격력 증가 (단위: %)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> COOLDOWN_MULTIPLY = synergySettings.new SettingObject<Integer>(Abyss.class, "cooldown-multiply", 33, "# 분해 시 쿨타임 증가율 (단위: %)") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> DAMAGE_INCREASE = synergySettings.new SettingObject<Integer>(Abyss.class, "harm-increase", 10, "# 받는 피해량 증가") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> MAX_WAIT = synergySettings.new SettingObject<Integer>(Abyss.class, "dismantle-available", 10, "# 능력 분해가 가능한 최대 시간", "# 분해가 가능한 동안은 복제된 능력을 사용할 수 없습니다.") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	private static final ImmutableMap<Rank, String> rankcolor = ImmutableMap.<Rank, String>builder()
			.put(Rank.C, "§e")
			.put(Rank.B, "§b")
			.put(Rank.A, "§a")
			.put(Rank.S, "§d")
			.put(Rank.L, "§6")
			.put(Rank.SPECIAL, "§c")
			.build();

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
	
	private List<AbilityBase> abilities = new ArrayList<>();
	private double increase = 1;
	private double coolmultiply = 1;
	private final int dmgmultiply = DAMAGE_INCREASE.getValue();

	@SuppressWarnings("unused")
	private final Object EXPLAIN = new Object() {
		@Override
		public String toString() {
			if (abilities.isEmpty()) {
				final StringJoiner joiner = new StringJoiner("\n");
				if (increase > 1) {
					joiner.add("§c분해 횟수§7: §b" + (int) (((increase - 1) / (INCREASE.getValue() * 0.01)) + 1) + "§f회");
					joiner.add("§c현재 공격력§7: §e" + df.format(increase) + "§f배");
				}
				joiner.add("능력을 복제할 수 있습니다.");
				return joiner.toString();
			} else {
				final StringJoiner joiner = new StringJoiner("\n");
				int a = 1;
				if (increase > 1) {
					joiner.add("§c분해 횟수§7: §b" + (int) (((increase - 1) / (INCREASE.getValue() * 0.01)) + 1) + "§f회");
					joiner.add("§c현재 공격력§7: §e" + df.format(increase) + "§f배");
				}
				for (AbilityBase ability : abilities) {
					a++;
					if (a % 2 == 0) {
						joiner.add("§a복제한 능력 §f| §7[§b" + ability.getName() + "§7] ×2 " + ability.getRank().getRankName() + " " + ability.getSpecies().getSpeciesName());
						for (final Iterator<String> iterator = ability.getExplanation(); iterator.hasNext();) {
							joiner.add("§f" + iterator.next());
						}	
					}	
				}
				return joiner.toString();
			}
		}
	};

	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue(), "어비스", 50);
	private ActionbarChannel ac = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private final int maxwait = MAX_WAIT.getValue() * 20;
	
	private AbilityTimer dismantle = new AbilityTimer(maxwait) {
		
		@Override
		public void run(int count) {
			ac.update("§b분해 가능§7: §e" + df.format(count / (double) 20) + "§f초");
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			ac.update(null);
		}
		
		
	}.setPeriod(TimeUnit.TICKS, 1).register();

	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK) {
			if (getPlayer().isSneaking()) {
				if (!abilities.isEmpty()) {
					for (AbilityBase ability : abilities) {
						try {
							if (ability != null) {
								ability.destroy();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					cooldown.start();
					abilities.clear();
					if (dismantle.isRunning()) {
						increase += (INCREASE.getValue() * 0.01);
						coolmultiply *= 1 + (COOLDOWN_MULTIPLY.getValue() * 0.01);
						dismantle.stop(false);
						getParticipant().getPlayer().sendMessage("§4[§c!§4] §c능력을 분해하여 쿨타임이 증가하고 공격력이 상시 상승합니다.");
						SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), 0.8f, 0.5f);
						SoundLib.BLOCK_LAVA_EXTINGUISH.playSound(getPlayer().getLocation(), 1, 0.5f);
						SoundLib.BLOCK_ANVIL_DESTROY.playSound(getPlayer().getLocation(), 1, 0.75f);
					} else getParticipant().getPlayer().sendMessage("§3[§b!§3] §b능력을 삭제하였습니다.");
					cooldown.setCount((int) (COOLDOWN.getValue() * coolmultiply));
				}
			}
		}
		if (!abilities.isEmpty()) {
			if (!dismantle.isRunning()) {
				boolean actived = false;
				boolean dontchange = false;
				for (AbilityBase ability : abilities) {
					 if (ability instanceof ActiveHandler) {
						 if (actived == true) dontchange = true;
						 actived = ((ActiveHandler) ability).ActiveSkill(material, clickType);
					 }
				}
				if (dontchange == true) actived = true;
				if (actived) return true;	
			} else getPlayer().sendMessage("§4[§c!§4] §f능력 분해가 가능한 동안은 복제된 능력을 사용할 수 없습니다.");
		} else {
			if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
				if (!cooldown.isCooldown()) {
					Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 30, predicate);
					if (player != null) {
						final Participant target = getGame().getParticipant(player);
						if (target.hasAbility() && !target.getAbility().isRestricted()) {
							final AbilityBase targetAbility = target.getAbility();
							if (targetAbility.getClass().equals(Mix.class)) {
								final Mix targetMix = (Mix) targetAbility;
								if (targetMix.hasAbility()) {
									if (targetMix.hasSynergy()) {
										if (targetMix.getSynergy().getClass() != Abyss.class) {
											try {
												this.abilities.add(AbilityBase.create(targetMix.getSynergy().getClass(), getParticipant()));
												this.abilities.add(AbilityBase.create(targetMix.getSynergy().getClass(), getParticipant()));
												for (AbilityBase ability : abilities) {
													ability.setRestricted(false);
												}
												getPlayer().sendMessage("§2[§a!§2] §b능력을 복제하였습니다. 당신의 능력은 " + rankcolor.get(targetMix.getSynergy().getRank()) + targetMix.getSynergy().getName() + "§7×2 §b 입니다.");
												dismantle.start();
											} catch (ReflectiveOperationException e) {
												e.printStackTrace();
											}	
										} else getPlayer().sendMessage("§4[§c!§4] §c어비스는 복제할 수 없습니다.");
									} else {
										final AbilityBase first = targetMix.getFirst(), second = targetMix.getSecond();
										
										try {
											final Class<? extends AbilityBase> clazz1 = first.getClass();
											final Class<? extends AbilityBase> clazz2 = second.getClass();
											if (clazz1 != Abyss.class) {
												this.abilities.add(AbilityBase.create(clazz1, getParticipant()));
												this.abilities.add(AbilityBase.create(clazz1, getParticipant()));
												for (AbilityBase ability : abilities) {
													ability.setRestricted(false);
												}
											} else getPlayer().sendMessage("§4[§c!§4] §c어비스는 복제할 수 없습니다.");
											if (clazz2 != Abyss.class) {
												this.abilities.add(AbilityBase.create(clazz2, getParticipant()));
												this.abilities.add(AbilityBase.create(clazz2, getParticipant()));
												for (AbilityBase ability : abilities) {
													ability.setRestricted(false);
												}
											} else getPlayer().sendMessage("§4[§c!§4] §c어비스는 복제할 수 없습니다.");
											getPlayer().sendMessage("§2[§a!§2] §b능력을 복제하였습니다. 당신의 능력은 §3[" + rankcolor.get(first.getRank()) + first.getName() + "§7 + " + rankcolor.get(second.getRank()) + second.getName() + "§3]§7×2 §b 입니다.");
											dismantle.start();
										} catch (ReflectiveOperationException e) {
											e.printStackTrace();
										}
									}

								}
							} else {
								try {
									final Class<? extends AbilityBase> clazz = targetAbility.getClass();
									if (clazz != Abyss.class) {
										this.abilities.add(AbilityBase.create(clazz, getParticipant()));
										this.abilities.add(AbilityBase.create(clazz, getParticipant()));
										for (AbilityBase ability : abilities) {
											ability.setRestricted(false);
										}
										getPlayer().sendMessage("§2[§a!§2] §b능력을 복제하였습니다. 당신의 능력은 " + rankcolor.get(targetAbility.getRank()) + targetAbility.getName() + "§7×2§b 입니다.");
										dismantle.start();
									} else getPlayer().sendMessage("§4[§c!§4] §c어비스는 복제할 수 없습니다.");
								} catch (ReflectiveOperationException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null) {
				if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();	
			}
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager)) {
			e.setDamage(e.getDamage() * increase);
		}
		
		if (e.getEntity().equals(getPlayer()) && damager != null) {
			Participant participant = getGame().getParticipant(damager);
			if (participant.hasAbility()) {
				int count = 0;
				if (participant.getAbility().getClass().equals(Mix.class)) {
					Mix mix = (Mix) participant.getAbility();
					if (mix.hasSynergy()) {
						AbilityBase synergy = mix.getSynergy();
						for (AbilityBase ability : abilities) {
							if (ability.getClass().equals(synergy.getClass())) {
								count += 2;
							}
						}
						e.setDamage(e.getDamage() * (1 + (dmgmultiply * 0.01 * count)));
					} else {
						AbilityBase first = mix.getFirst();
						AbilityBase second = mix.getSecond();
						for (AbilityBase ability : abilities) {
							if (ability.getClass().equals(first.getClass()) || ability.getClass().equals(second.getClass())) {
								count += 1;
							}
						}
						e.setDamage(e.getDamage() * (1 + (dmgmultiply * 0.01 * count)));	
					}
				} else {
					for (AbilityBase ability : abilities) {
						if (ability.getClass().equals(participant.getAbility().getClass())) {
							count += 1;
						}
					}
					e.setDamage(e.getDamage() * (1 + (dmgmultiply * 0.01 * count)));
				}	
			}
		}
	}
	
	@Override
	public void TargetSkill(Material material, LivingEntity entity) {
		if (!abilities.isEmpty()) {
			if (!dismantle.isRunning()) {
				for (AbilityBase ability : abilities) {
					if (ability instanceof TargetHandler) {
						((TargetHandler) ability).TargetSkill(material, entity);
					}	
				}
			} else getPlayer().sendMessage("§4[§c!§4] §f능력 분해가 가능한 동안은 복제된 능력을 사용할 수 없습니다.");
		}
	}

	@Override
	public Set<GameTimer> getTimers() {
		Set<GameTimer> timers = new HashSet<>();
		for (AbilityBase ability : abilities) {
			timers.addAll(SetUnion.union(super.getTimers(), ability.getTimers()));	
		}
		return timers;
	}

	@Override
	public Set<GameTimer> getRunningTimers() {
		Set<GameTimer> timers = new HashSet<>();
		for (AbilityBase ability : abilities) {
			timers.addAll(SetUnion.union(super.getRunningTimers(), ability.getRunningTimers()));	
		}
		return timers;
	}

	@Override
	public boolean usesMaterial(Material material) {
		for (AbilityBase ability : abilities) {
			if (ability != null) {
				return ability.usesMaterial(material);
			}	
		}
		return super.usesMaterial(material);
	}

	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			if (!abilities.isEmpty()) {
				for (AbilityBase ability : abilities) {
					ability.setRestricted(false);
				}
			}
		} else if (update == Update.RESTRICTION_SET) {
			if (!abilities.isEmpty()) {
				for (AbilityBase ability : abilities) {
					ability.setRestricted(true);
				}
			}
		} else if (update == Update.ABILITY_DESTROY) {
			if (!abilities.isEmpty()) {
				for (AbilityBase ability : abilities) {
					ability.destroy();
				}
				abilities.clear();
			}
		}
	}
	
}