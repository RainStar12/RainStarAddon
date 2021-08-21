package RainStarAbility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration.Flag;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.SubscribeEvent.Priority;
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
import daybreak.abilitywar.game.list.mix.synergy.SynergyFactory;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.google.common.base.Predicate;
import net.md_5.bungee.api.ChatColor;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "카이로스", rank = Rank.L, species = Species.GOD, explain = {
		"기회의 신 카이로스.",
		"§7철괴 우클릭 §8- §a기회 부여§f: 다른 플레이어를 15칸 내에서 우클릭 해 대상의 능력이",
		" §dS 등급§f보다 낮을 경우 최소 한 등급 이상의 능력으로 §b재추첨§f합니다.",
		" 이때 대상에게 바꿔준 능력을 알 수 있습니다. $[COOLDOWN]",
		" 또한 바꿔준 대상에게서 §e기회 체력§f을 1.5칸 적립합니다. 대상은 나에게",
		" 다음 공격 피해를 입히지 못하고 강하게 넉백당합니다.",
		"§7패시브 §8- §a마지막 기회§f: §c치명적인 피해를 입었을 때§f 단 한 번",
		" 사망하지 않고 대신 체력 반 칸으로 §d부활§f합니다. 이때, 다른 플레이어에게",
		" 적립한 §e기회 체력§f만큼 각 대상의 체력을 가져와 즉시 §d회복§f합니다.",
		" 또한 최대 15초간 GUI가 열려 회복한 체력에 비례해 최대 5가지의 능력 중",
		" 한 가지의 능력을 선택해 이 능력을 변경할 수 있습니다."
		},
		summarize = {
		"§7철괴 우클릭§f으로 다른 플레이어를 15칸 내에서 바라보고 우클릭하면",
		"대상의 능력이 §dS 등급§f보다 낮을 경우 최소 한 등급 이상의 능력으로 재추첨합니다.",
		"바꿔준 대상에게 한 번당 §c기회 체력§f 1.5칸씩을 적립하여 사망 위기일 때 대상들에게",
		"§c기회 체력§f을 가져와 단 한 번 부활합니다. 부활 시 기회 체력 적립에 비례해",
		"최대 5가지의 선택지 중 하나의 능력을 선택해 부활합니다."
		})

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
        @Description(subject = "능력 선택", explain = {
                "본인의 부활 시 능력을 선택해오는 것은",
                "자신이 원하는 능력을 확정시키는 능력으로,",
                "엄청난 성능을 자랑합니다. 특히 그것이 믹스 능력자라면",
                "다른 하나의 조합을 통해 시너지를 고를 수도 있겠죠."
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

	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final ActionbarChannel ac = newActionbarChannel();
	private int stack = 0;
	private static final Circle circle = Circle.of(0.5, 30);
	private static final RGB gold = RGB.of(254, 228, 1);
	private Participant target = null;
	private Set<UUID> inv = new HashSet<>();
	private Map<Participant, Integer> getHp = new HashMap<>();
	private Set<Player> knockback = new HashSet<>();
	private static final Random random = new Random();
	private boolean rebirth = false;
	private final int chance = CHANCE.getValue();
	
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
	
	public static final SettingObject<Integer> COOLDOWN = abilitySettings.new SettingObject<Integer>(Kairos.class,
			"cooldown", 20, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> CHANCE = abilitySettings.new SettingObject<Integer>(Kairos.class,
			"chance", 10, "# 시너지 등장 확률", "# 기준: n/1000", "# 10으로 입력 시 10/1000, 1%입니다.") {
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
	
	public AbilityRegistration getRealRandomAbility(Participant target) {
		final Set<AbilityRegistration> usedAbilities = new HashSet<>();
		for (Participant participant : getGame().getParticipants()) {
			if (participant.hasAbility() && participant.attributes().TARGETABLE.getValue()) {
				usedAbilities.add(participant.getAbility().getRegistration());
			}
		}
		
		final List<AbilityRegistration> registrations = AbilityList.values().stream().filter(
				ability -> !Configuration.Settings.isBlacklisted(ability.getManifest().name()) &&
				!usedAbilities.contains(ability)
		).collect(Collectors.toList());
		return registrations.isEmpty() ? null : random.pick(registrations);
	}

    public AbilityRegistration getRandomSynergy() {

		Set<AbilityRegistration> synergies = new HashSet<>();
		
        for (AbilityRegistration synergy : SynergyFactory.getSynergies()) {
        	String name = synergy.getManifest().name();
        	if (!Configuration.Settings.isBlacklisted(name) && !name.equals("기회")) {
        		synergies.add(synergy);
        	}
        }
		
        Random r = new Random();
        return synergies.toArray(new AbilityRegistration[]{})[r.nextInt(synergies.size())];
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
						getPlayer().sendMessage("§b[§a!§b] §e" + player.getName() + "§f님에게 §a기회§f를 드립니다.");
						getPlayer().sendMessage("§b[§a!§b] §f이제 대상의 능력은 §e" + target.getAbility().getDisplayName() + "§f입니다.");
						target.getPlayer().sendMessage("§b[§a!§b] §e" + getPlayer().getName() + "§f님이 당신에게 기회를 주어 능력이 §e" + target.getAbility().getDisplayName() + "§f" + KoreanUtil.getJosa(target.getAbility().getDisplayName(), Josa.이가) + " 되었습니다.");
						new AbilityTimer(150) {

							private ActionbarChannel actionbarChannel;

							@Override
							protected void run(int count) {
								actionbarChannel.update("§7무적 §f: " + (getCount() / 20.0) + "초");
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
						getHp.put(target, getHp.getOrDefault(target, 0) + 3);
						knockback.add(target.getPlayer());
						SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(target.getPlayer(), 1, 1.3f);
						SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.5f);
						cooldown.start();
						stack++;
						ac.update("§b기회를 준 횟수§7: §e" + stack);
						return true;
					} else if (ab.getClass().equals(Mix.class)) {
						final Mix mix = (Mix) ab;
						final AbilityBase first = mix.getFirst(), second = mix.getSecond();
						
						if (mix.hasSynergy()) {
							getPlayer().sendMessage("§b[§a!§b] §e" + player.getName() + "§f님은 §c기회§f를 받을 필요가 없습니다.");
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
								getPlayer().sendMessage("§b[§a!§b] §e" + player.getName() + "§f님에게 §a기회§f를 드립니다.");
								getPlayer().sendMessage("§b[§a!§b] §f이제 대상의 능력은 §e" + mix.getFirst().getDisplayName() + "§f입니다.");
								target.getPlayer().sendMessage("§b[§a!§b] §e" + getPlayer().getName() + "§f님이 당신에게 기회를 주어 능력이 §e" + mix.getFirst().getDisplayName() + "§f, §e" + mix.getSecond().getDisplayName() + "§f" + KoreanUtil.getJosa(mix.getSecond().getDisplayName(), Josa.이가) + " 되었습니다.");
								new AbilityTimer(150) {

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
								getHp.put(target, getHp.getOrDefault(target, 0) + 3);
								knockback.add(target.getPlayer());
								SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(target.getPlayer(), 1, 1.3f);;
								SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.5f);
								cooldown.start();
								stack++;
								ac.update("§b기회를 준 횟수§7: §e" + stack);
								return true;
							} else {
								getPlayer().sendMessage("§b[§a!§b] §e" + player.getName() + "§f님은 §c기회§f를 받을 필요가 없습니다.");
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
								getPlayer().sendMessage("§b[§a!§b] §e" + player.getName() + "§f님에게 §a기회§f를 드립니다.");
								getPlayer().sendMessage("§b[§a!§b] §f이제 대상의 능력은 §e" + mix.getSecond().getDisplayName() + "§f입니다.");
								target.getPlayer().sendMessage("§b[§a!§b] §e" + getPlayer().getName() + "§f님이 당신에게 기회를 주어 능력이 §e" + mix.getFirst().getDisplayName() + "§f, §e" + mix.getSecond().getDisplayName() + "§f" + KoreanUtil.getJosa(mix.getSecond().getDisplayName(), Josa.이가) + " 되었습니다.");
								new AbilityTimer(150) {

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
								getHp.put(target, getHp.getOrDefault(target, 0) + 3);
								knockback.add(target.getPlayer());
								SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(target.getPlayer(), 1, 1.3f);;
								SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer(), 1, 1.5f);
								cooldown.start();
								stack++;
								ac.update("§b기회를 준 횟수§7: §e" + stack);
								return true;
							} else {
								getPlayer().sendMessage("§b[§a!§b] §e" + player.getName() + "§f님은 §c기회§f를 받을 필요가 없습니다.");
								SoundLib.ENTITY_ENDERMAN_HURT.playSound(getPlayer(), 1, 0.7f);
								return false;
							}
						}
					} else {
							getPlayer().sendMessage("§b[§a!§b] §e" + player.getName() + "§f님은 §c기회§f를 받을 필요가 없습니다.");
							SoundLib.ENTITY_ENDERMAN_HURT.playSound(getPlayer(), 1, 0.7f);
							return false;
						}
				} else {
					getPlayer().sendMessage("§b[§a!§b] §f해당 플레이어는 받을 수 있는 기회가 없습니다.");
					return false;
				}
			}
		}
		return false;
	}

	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (inv.contains(e.getDamager().getUniqueId()) || inv.contains(e.getEntity().getUniqueId())) {
		 	e.setCancelled(true);
		}
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() != null) {
				Player player = (Player) projectile.getShooter();
				if (inv.contains(player.getUniqueId())) {
					e.setCancelled(true);
				}
				if (knockback.contains(player)) {
					knockback.remove(player);
					player.setVelocity(VectorUtil.validateVector(player.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().clone().setY(0).multiply(3.5)));	
					e.setCancelled(true);
				}
			}
		}
		if (knockback.contains(e.getDamager()) && e.getEntity().equals(getPlayer())) {
			knockback.remove(e.getDamager());
			e.getDamager().setVelocity(VectorUtil.validateVector(e.getDamager().getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().clone().setY(0).multiply(3.5)));	
			e.setCancelled(true);
		}
		onEntityDamage(e);
	}
	
	@SubscribeEvent(priority = Priority.HIGHEST)
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}

	@SubscribeEvent(priority = Priority.HIGHEST)
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
			if (sum >= 3) {
				new AbilitySelect(sum / 3).start();
			}
			e.setCancelled(true);
		}
	}
	
	@SubscribeEvent
	public void onEntityShootBow(EntityShootBowEvent e) {
		if (inv.contains(e.getEntity().getUniqueId())) {
			e.setCancelled(true);
		}
	}
	
	public class AbilitySelect extends AbilityTimer implements Listener {
		
		private final ItemStack NULL = (new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
		
		private final List<AbilityRegistration> values = new ArrayList<>();
		private Set<AbilityRegistration> synergies = new HashSet<>();
		
		private Map<Integer, AbilityRegistration> slots = new HashMap<>();
		
		private AbilityRegistration selected;
		
		private final Inventory gui;
		private int getHps;
		
		public AbilitySelect(int getHps) {
			super(TaskType.REVERSE, 300);
			setPeriod(TimeUnit.TICKS, 1);
			gui = Bukkit.createInventory(null, 9, "§0능력을 선택해주세요.");
			this.getHps = Math.min(5, getHps);
		}
		
		protected void onStart() {
			SoundLib.BLOCK_ENDER_CHEST_OPEN.playSound(getPlayer().getLocation(), 1f, 0.5f);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			getPlayer().openInventory(gui);
			for (int i = 0; i < getHps; i++) {
				if (getParticipant().getAbility().getClass().equals(Mix.class)) {
					final Mix mix = (Mix) getParticipant().getAbility();
					final AbilityBase first = mix.getFirst(), second = mix.getSecond();
					final boolean firstStatus = first.getClass().equals(Kairos.class), 
							secondStatus = second.getClass().equals(Kairos.class);
					if (firstStatus && secondStatus) {
						AbilityRegistration randomSynergy = getRandomSynergy();
						values.add(randomSynergy);
						selected = randomSynergy;
						synergies.add(randomSynergy);
					} else {
						Random random = new Random();
						if (random.nextInt(1000) <= chance) {
							AbilityRegistration randomSynergy = getRandomSynergy();
							values.add(randomSynergy);
							selected = randomSynergy;
							synergies.add(randomSynergy);
						} else {
							AbilityRegistration randomAbility = getRealRandomAbility(getParticipant());
							values.add(randomAbility);
							selected = randomAbility;
						}
					}
				} else {
					Random random = new Random();
					if (random.nextInt(1000) <= chance) {
						AbilityRegistration randomSynergy = getRandomSynergy();
						values.add(randomSynergy);
						selected = randomSynergy;
						synergies.add(randomSynergy);
					} else {
						AbilityRegistration randomAbility = getRealRandomAbility(getParticipant());
						values.add(randomAbility);
						selected = randomAbility;
					}
				}
			}
		}
		
		@Override
		protected void run(int arg0) {
			placeItem(getHps);
			getPlayer().setGameMode(GameMode.SPECTATOR);
			if (arg0 == 60) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f); 
			if (arg0 == 40) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f); 
			if (arg0 == 20) SoundLib.BLOCK_NOTE_BLOCK_SNARE.playSound(getPlayer(), 1, 1.7f); 
		}
		
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			if (getParticipant().getAbility().getClass().equals(Mix.class)) {
				final Mix mix = (Mix) getParticipant().getAbility();
				final AbilityBase first = mix.getFirst(), second = mix.getSecond();
				final boolean firstStatus = first.getClass().equals(Kairos.class), 
						secondStatus = second.getClass().equals(Kairos.class);
				if (firstStatus && secondStatus) {
					try {
						mix.setAbility(SynergyFactory.getSynergyBase(selected).getLeft().getAbilityClass(), SynergyFactory.getSynergyBase(selected).getRight().getAbilityClass());
					} catch (ReflectiveOperationException e1) {
						e1.printStackTrace();
					}
				} else if (firstStatus || secondStatus) {
					Class<? extends AbilityBase> firstClass = first.getClass(), secondClass = second.getClass();
					if (firstStatus) firstClass = selected.getAbilityClass();
					if (secondStatus) secondClass = selected.getAbilityClass();
					try {
						mix.setAbility(firstClass, secondClass);
					} catch (ReflectiveOperationException e1) {
						e1.printStackTrace();
					}
				}
				getPlayer().sendMessage("§b[§a!§b] 마지막 기회§f를 붙잡아 §e카이로스§f 능력이 바뀌었습니다.");
				getPlayer().sendMessage("§b[§a!§b] §f현재 능력: §e" + mix.getFirst().getDisplayName() + "§f, §e" + mix.getSecond().getDisplayName());
			} else {
				try {
					getParticipant().setAbility(selected.getAbilityClass());
				} catch (UnsupportedOperationException | ReflectiveOperationException e1) {
					e1.printStackTrace();
				}
				getPlayer().sendMessage("§b[§a!§b] 마지막 기회§f를 붙잡아 §e카이로스§f 능력이 §e" + getParticipant().getAbility().getDisplayName() + "§f" + KoreanUtil.getJosa(getParticipant().getAbility().getDisplayName(), Josa.으로로) + " 바뀌었습니다.");
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
			HandlerList.unregisterAll(this);
			getPlayer().setGameMode(GameMode.SURVIVAL);
			getPlayer().closeInventory();
		}
		
		private MaterialX getRankBlock(Rank rank) {
			if (rank.equals(Rank.C)) {
				return MaterialX.YELLOW_CONCRETE;
			} else if (rank.equals(Rank.B)) {
				return MaterialX.LIGHT_BLUE_CONCRETE;
			} else if (rank.equals(Rank.A)) {
				return MaterialX.LIME_CONCRETE;
			} else if (rank.equals(Rank.S)) {
				return MaterialX.MAGENTA_CONCRETE;
			} else if (rank.equals(Rank.L)) {
				return MaterialX.ORANGE_CONCRETE;
			} else if (rank.equals(Rank.SPECIAL)) {
				return MaterialX.RED_CONCRETE;
			}
			return null;
		}
		
		private void placeItem(int number) {
			for (int i = 0; i < number; i++) {
				ItemStack item = new ItemBuilder(getRankBlock(values.get(i).getManifest().rank())).build();
				ItemMeta meta = item.getItemMeta();
				if (synergies.contains(values.get(i))) meta.addEnchant(Enchantment.LOOT_BONUS_BLOCKS, 1, true);
				meta.setDisplayName(ChatColor.AQUA + values.get(i).getManifest().name());
				final StringJoiner joiner = new StringJoiner(ChatColor.WHITE + ", ");
				if (values.get(i).hasFlag(Flag.ACTIVE_SKILL)) joiner.add(ChatColor.GREEN + "액티브");
				if (values.get(i).hasFlag(Flag.TARGET_SKILL)) joiner.add(ChatColor.GOLD + "타게팅");
				if (values.get(i).hasFlag(Flag.BETA)) joiner.add(ChatColor.DARK_AQUA + "베타");
				final List<String> lore = Messager.asList(
						"§f등급: " + values.get(i).getManifest().rank().getRankName(),
						"§f종류: " + values.get(i).getManifest().species().getSpeciesName(),
						joiner.toString(),
						"");
				for (final String line : values.get(i).getManifest().explain()) {
					lore.add(ChatColor.WHITE.toString().concat(line));
				}
				lore.add("");
				lore.add("§2» §f이 능력을 부여하려면 클릭하세요.");
				meta.setLore(lore);
				item.setItemMeta(meta);
				switch(number) {
				case 1:
					gui.setItem(4, item);
					slots.put(4, values.get(i));
					break;
				case 2:
					switch(i) {
					case 0:
						gui.setItem(2, item);
						slots.put(2, values.get(i));
						break;
					case 1:
						gui.setItem(6, item);
						slots.put(6, values.get(i));
						break;
					}
					break;
				case 3:
					switch(i) {
					case 0:
						gui.setItem(1, item);
						slots.put(1, values.get(i));
						break;
					case 1:
						gui.setItem(4, item);
						slots.put(4, values.get(i));
						break;
					case 2:
						gui.setItem(7, item);
						slots.put(7, values.get(i));
						break;
					}
					break;
				case 4:
					switch(i) {
					case 0:
						gui.setItem(1, item);
						slots.put(1, values.get(i));
						break;
					case 1:
						gui.setItem(3, item);
						slots.put(3, values.get(i));
						break;
					case 2:
						gui.setItem(5, item);
						slots.put(5, values.get(i));
						break;
					case 3:
						gui.setItem(7, item);
						slots.put(7, values.get(i));
						break;
					}
					break;
				case 5:
					switch(i) {
					case 0:
						gui.setItem(0, item);
						slots.put(0, values.get(i));
						break;
					case 1:
						gui.setItem(2, item);
						slots.put(2, values.get(i));
						break;
					case 2:
						gui.setItem(4, item);
						slots.put(4, values.get(i));
						break;
					case 3:
						gui.setItem(6, item);
						slots.put(6, values.get(i));
						break;
					case 4:
						gui.setItem(8, item);
						slots.put(8, values.get(i));
						break;
					}
					break;
				}
				
				for (int j = 0; j < 8; j++) {
					if (gui.getItem(j) == null) {
						gui.setItem(j, NULL);
					}
				}
			}
		}
		
		@EventHandler
		private void onInventoryClose(InventoryCloseEvent e) {
			if (e.getInventory().equals(gui)) stop(false);
		}

		@EventHandler
		private void onQuit(PlayerQuitEvent e) {
			if (e.getPlayer().getUniqueId().equals(getPlayer().getUniqueId())) stop(false);
		}
		
		@EventHandler
		private void onPlayerMove(PlayerMoveEvent e) {
			if (e.getPlayer().equals(getPlayer())) e.setCancelled(true);
		}

		@EventHandler
		private void onInventoryClick(InventoryClickEvent e) {
			if (e.getInventory().equals(gui)) {
				e.setCancelled(true);
				if (slots.containsKey(e.getSlot())) {
					selected = slots.get(e.getSlot());
					getPlayer().closeInventory();
				}
			}
		}
		
	}
	
}