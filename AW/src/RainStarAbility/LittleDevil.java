package RainStarAbility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.config.enums.CooldownDecrease;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.team.interfaces.Teamable;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.ItemLib;
import daybreak.google.common.base.Predicate;

@AbilityManifest(
		name = "꼬마 악마",
		rank = Rank.A, 
		species = Species.UNDEAD, 
		explain = {
		"§7철괴 우클릭 §8- §6트릭 오어 트릿!§f: 주변 $[RANGE_CONFIG]칸 내의 모든 플레이어의",
		" 인벤토리에 포션 등의 소모형 아이템이 있다면 한 개 받아갑니다.",
		" 만약 소모형 아이템이 단 하나도 없거나 이미 트릿을 시전한 대상에겐 장난을 쳐",
		" 대상의 핫바 1~9번 아이템을 무작위로 뒤섞습니다. $[COOLDOWN_CONFIG]"
		})

@Tips(tip = {
        "다른 플레이어의 소모템을 가져와 대상의 전략을 방해하고,",
        "나 자신은 그 소모템을 사용하여 안정성이나 기동성 등을 꾀할 수",
        "있습니다. 만일 대상에게 소모템이 없다면 대상의 아이템을 봉인해",
        "공격 회피나 능력 봉인 등 변수를 창출할 수 있고, 생존력이 높아집니다."
}, strong = {
        @Description(subject = "변수 창출", explain = {
                "대상의 소모형 아이템을 가져와 전략 방해 및",
                "자신에게 이득이 되는 버프를 걸 수 있습니다."
        }),
        @Description(subject = "소모형 아이템이 많은 플레이어", explain = {
                "대상의 소모형 아이템이 많으면 많을수록",
                "능력을 쓸 때마다 계속해서 가져올 수 있습니다."
        })
}, weak = {
        @Description(subject = "타이밍", explain = {
                "원하는 아이템을 봉인하기 위해서는 대상의 손을",
                "잘 보며 타이밍을 맞춰야 합니다."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.THREE, crowdControl = Level.FOUR, mobility = Level.ZERO, utility = Level.SIX), difficulty = Difficulty.VERY_EASY)

public class LittleDevil extends AbilityBase implements ActiveHandler {
	
	public LittleDevil(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN_CONFIG 
	= abilitySettings.new SettingObject<Integer>(LittleDevil.class,
			"cooldown", 60, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> RANGE_CONFIG 
	= abilitySettings.new SettingObject<Integer>(LittleDevil.class,
			"range", 10, "# 능력 범위") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN_CONFIG.getValue(), CooldownDecrease._50);
	private Set<Player> playercount = new HashSet<>();
	private static final EulerAngle DEFAULT_EULER_ANGLE = new EulerAngle(Math.toRadians(270), 0, 0);
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			if (entity instanceof Player) {
				if (!getGame().isParticipating(entity.getUniqueId())
						|| (getGame() instanceof DeathManager.Handler &&
								((DeathManager.Handler) getGame()).getDeathManager().isExcluded(entity.getUniqueId()))
						|| !getGame().getParticipant(entity.getUniqueId()).attributes().TARGETABLE.getValue()) {
					return false;
				}
				if (getGame() instanceof Teamable) {
					final Teamable teamGame = (Teamable) getGame();
					final Participant entityParticipant = teamGame.getParticipant(
							entity.getUniqueId()), participant = getParticipant();
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
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown()) {
			List<Player> players = LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), RANGE_CONFIG.getValue(), predicate);
	    	if (players.size() != 0) {
				for (Player p : players) {
					SoundLib.ENTITY_WITCH_AMBIENT.playSound(p, 1, 1.8f);
					Inventory inventory = p.getPlayer().getInventory();
					List<ItemStack> list = new CopyOnWriteArrayList<>(inventory.getContents());
					for (ItemStack itemStack : list) {
						if (itemStack == null) {
							list.remove(itemStack);
							continue;
						}
						if (!playercount.contains(p)) {
							if (itemStack.getType() == Material.POTION || itemStack.getType() == Material.LINGERING_POTION ||
									itemStack.getType() == Material.SPLASH_POTION || itemStack.getType() == Material.ENDER_PEARL
									|| itemStack.getType() == Material.GOLDEN_APPLE || itemStack.getType() == MaterialX.TOTEM_OF_UNDYING.getMaterial()) {
								if (itemStack.getType() == Material.POTION || itemStack.getType() == Material.LINGERING_POTION ||
									itemStack.getType() == Material.SPLASH_POTION) {
									p.getInventory().removeItem(itemStack);
									new Stealing(itemStack, p.getLocation()).start();
									playercount.add(p);
									break;
								} else {
									ItemLib.removeItem(p.getInventory(), itemStack.getType(), 1);
									new Stealing(itemStack, p.getLocation()).start();
									playercount.add(p);
									break;
								}
							} else {
								list.remove(itemStack);	
							}	
						} else {
							list.remove(itemStack);
						}
					}

					if (list.size() == 0) {
						Inventory inv = p.getPlayer().getInventory();
						List<ItemStack> slots = new ArrayList<>();
						slots.add(inv.getItem(0));
						slots.add(inv.getItem(1));
						slots.add(inv.getItem(2));
						slots.add(inv.getItem(3));
						slots.add(inv.getItem(4));
						slots.add(inv.getItem(5));
						slots.add(inv.getItem(6));
						slots.add(inv.getItem(7));
						slots.add(inv.getItem(8));
						Collections.shuffle(slots);
						for (int a=0;a<9;a++){ inv.setItem(a, slots.get(a)); }
					}
				}	
				cooldown.start();
				return true;
	    	} else {
	    		getPlayer().sendMessage("§6[§e!§6] §f주변 §6" + RANGE_CONFIG.getValue() + "칸§f 내에 장난을 칠 대상이 없습니다.");
	    		return false;
	    	}
	    }
		return false;
	}
	
	public class Stealing extends AbilityTimer {
		
		private final ItemStack item;
		private final ArmorStand hologram;
		private final Location startLocation;
		
		public Stealing(ItemStack item, Location startLocation) {
			super(TaskType.REVERSE, 10);
			setPeriod(TimeUnit.TICKS, 1);
			this.item = item;
			this.startLocation = startLocation;
			this.hologram = startLocation.getWorld().spawn(startLocation.clone(), ArmorStand.class);
			hologram.setRightArmPose(DEFAULT_EULER_ANGLE);
			hologram.setVisible(false);
			hologram.setGravity(false);
			hologram.setInvulnerable(true);
			NMS.removeBoundingBox(hologram);
			hologram.getEquipment().setItemInMainHand(item);
			hologram.setCustomNameVisible(false);
		}
		
		@Override
		public void run(int count) {
			hologram.teleport(startLocation.clone().add(Line.vectorAt(startLocation, getPlayer().getLocation(), 10, 10 - count)));
			if (hologram.getLocation().distanceSquared(getPlayer().getLocation().clone().add(0, 1, 0)) <= 2) {
				this.stop(false);
			}
		}
		
		@Override
		public void onEnd() {
			getPlayer().getInventory().addItem(item);
			SoundLib.ENTITY_ITEM_PICKUP.playSound(getPlayer());
			hologram.remove();
		}
		
		@Override
		public void onSilentEnd() {
			hologram.remove();
		}
		
	}
	
}