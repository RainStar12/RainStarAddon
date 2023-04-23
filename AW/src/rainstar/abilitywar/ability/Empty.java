package rainstar.abilitywar.ability;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityFactory.AbilityRegistration;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.Tips;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.Materials;
import daybreak.abilitywar.ability.Tips.Description;
import daybreak.abilitywar.ability.Tips.Difficulty;
import daybreak.abilitywar.ability.Tips.Level;
import daybreak.abilitywar.ability.Tips.Stats;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.decorator.TargetHandler;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.list.mix.AbstractMix;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.utils.base.collect.SetUnion;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.google.common.base.Predicate;
import rainstar.abilitywar.utils.RankColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@AbilityManifest(name = "[ ]", rank = Rank.L, species = Species.OTHERS, explain = {
		"§a---------------------------------",
		"$(EXPLAIN)",
		"§a---------------------------------",
		"철괴로 대상을 30칸 내에서 우클릭하여 능력을 §2복제§f합니다.",
		"웅크린 채 철괴 좌클릭으로 §2복제§f를 해제할 수 있습니다. §c쿨타임 §7: §f갖고 있던 쿨타임 / 2",
		"능력당 한 번, §2복제§f 시 해당 §b능력이 담긴 책§f을 획득합니다.",
		"§2복제§f된 능력이 없을 때 §b능력이 담긴 책§f을 우클릭하여 해당 능력을 §2복제§f합니다."
		},
		summarize = {
		"§a---------------------------------",
		"$(SUM_EXPLAIN)",
		"§a---------------------------------",
		"철괴로 대상을 30칸 내에서 우클릭하여 능력을 §2복제§f합니다.",
		"웅크린 채 철괴 좌클릭으로 §2복제§f를 해제할 수 있습니다. §c쿨타임 §7: §f갖고 있던 쿨타임",
		"능력당 한 번, §2복제§f 시 해당 §b능력이 담긴 종이§f를 획득합니다.",
		"§2복제§f된 능력이 없을 때 §b능력이 담긴 종이§f를 우클릭하여 해당 능력을 §2복제§f합니다."
		})

@Tips(tip = {
        "다른 대상의 능력을 복제할 수 있어서 무한한 가능성을 가진 능력입니다.",
        "대상의 능력을 복제하여 능력을 알아차릴 수 있다던가, 강한 능력을 복제하여",
        "내 멋대로 사용이 가능하고 어느 한 능력에 멈춰있지 않는 능력입니다.",
        "다만 능력 사용에 있어 타게팅이 강제되기에 다른 대상에게 접근하는 것을",
        "유의하셔야만 합니다."
}, strong = {
        @Description(subject = "강한 능력의 대상", explain = {
                "다른 대상의 능력이 강력할수록 복제하는 본인의 능력도 강력해집니다."
        }),
        @Description(subject = "능력 파악", explain = {
                "타인의 능력을 복제하여 대상의 능력이 무엇인지 알아낼 수 있습니다."
        }),
        @Description(subject = "유동성", explain = {
                "능력을 복제할 수 있다는 것은 다시 말해, 게임 참가자의 모든 능력들을",
                "본인이 사용 가능하다는 말입니다. 여러 능력으로 바꾸며 수많은 전술을",
                "펼칠 수 있는 유동성이야말로 이 능력의 최대 강점입니다."
        })
}, weak = {
        @Description(subject = "약한 능력의 대상", explain = {
                "역으로 대상이 약한 능력을 소지하고 있다면, 복제한 자신의 능력도",
                "약해지는 꼴이 됩니다. 다른 대상의 능력을 미리 파악해두세요."
        }),
        @Description(subject = "적은 참가자 수", explain = {
                "게임 참가자 수가 적으면 결과적으로 이 능력의 최대 강점인",
                "유동성을 해치는 꼴이 됩니다. 되도록 플레이어가 많은 게임을",
                "진행하는 편이 좋습니다."
        })
}, stats = @Stats(offense = Level.ZERO, survival = Level.ZERO, crowdControl = Level.ZERO, mobility = Level.ZERO, utility = Level.TEN), difficulty = Difficulty.NORMAL)

@Materials(materials = {Material.BOOK, Material.IRON_INGOT})
public class Empty extends AbilityBase implements ActiveHandler, TargetHandler {

	public Empty(Participant participant) {
		super(participant);
	}
	
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
	
	private AbilityBase ability;
	private final Cooldown cooldown = new Cooldown(1, "공백");
	private Map<String, AbilityRegistration> abilityMap = new HashMap<>();
	
	@SuppressWarnings("serial")
	private List<String> lores = new ArrayList<String>() {
		{
			add("§7");
			add("§b> §7이 책을 우클릭해서 능력을 복제하세요.");
		}
	};
	
	@SuppressWarnings("unused")
	private final Object EXPLAIN = new Object() {
		@Override
		public String toString() {
			final StringJoiner joiner = new StringJoiner("\n");
			if (ability == null) joiner.add("능력을 복제할 수 있습니다.");
			else {
				joiner.add("§a복제한 능력 §f| §7[§b" + ability.getName() + "§7] " + ability.getRank().getRankName() + " " + ability.getSpecies().getSpeciesName());
				for (final Iterator<String> iterator = ability.getExplanation(); iterator.hasNext();) {
					joiner.add("§f" + iterator.next());
				}
			}
			return joiner.toString();
		}
	};
	
	@SuppressWarnings("unused")
	private final Object SUM_EXPLAIN = new Object() {
		@Override
		public String toString() {
			final StringJoiner joiner = new StringJoiner("\n");
			if (ability == null) joiner.add("능력을 복제할 수 있습니다.");
			else {
				joiner.add("§a복제한 능력 §f| §7[§b" + ability.getName() + "§7] " + ability.getRank().getRankName() + " " + ability.getSpecies().getSpeciesName());
				for (final Iterator<String> iterator = ability.getSummarize(); iterator.hasNext();) {
					joiner.add("§f" + iterator.next());
				}
			}
			return joiner.toString();
		}
	};
	
	@Override
	public String getDisplayName() {
		return "[" + (ability != null ? (ability.getName() + "]") : " ]");
	}
	
	@Override
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material == Material.IRON_INGOT && clickType == ClickType.LEFT_CLICK && getPlayer().isSneaking() && ability != null) {
			try {
				int cooltimeSum = 0;
				if (Empty.this.ability != null) {
					for (GameTimer timer : Empty.this.ability.getRunningTimers()) {
						if (timer instanceof Cooldown.CooldownTimer) {
							cooltimeSum += timer.getCount();
						}
					}
					Empty.this.ability.destroy();
				}
				Empty.this.ability = null;
				getParticipant().getPlayer().sendMessage("§3[§b!§3] §b당신의 능력이 §f[  ]§b으로 되돌아왔습니다.");
				cooldown.start();
				cooldown.setCount((int) (cooltimeSum / 2.0));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (ability != null) {
			return ability instanceof ActiveHandler && ((ActiveHandler) ability).ActiveSkill(material, clickType);	
		} else {
			if (clickType == ClickType.RIGHT_CLICK && !cooldown.isCooldown()) {
				if (material == Material.IRON_INGOT) {
					Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 30, predicate);
					if (player != null) {
						final Participant target = getGame().getParticipant(player);
						if (target.hasAbility() && !target.getAbility().isRestricted()) {
							final AbilityBase targetAbility = target.getAbility();
							if (getGame() instanceof AbstractMix) {
								final Mix targetMix = (Mix) targetAbility;
								if (targetMix.hasAbility()) {
									if (targetMix.hasSynergy()) {
										try {
											this.ability = AbilityBase.create(targetMix.getSynergy().getClass(), getParticipant());
											this.ability.setRestricted(false);
											getPlayer().sendMessage("§3[§b!§3] §b능력을 복제하였습니다. 당신의 능력은 " + RankColor.getColor(ability.getRank()) + ability.getName() + "§b 입니다.");
											if (!abilityMap.containsKey("§8【 " + RankColor.getColor(ability.getRank()) + ability.getName() + " §8】")) {
												abilityMap.put("§8【 " + RankColor.getColor(ability.getRank()) + ability.getName() + " §8】", ability.getRegistration());
												ItemStack book = new ItemStack(Material.BOOK, 1);
												ItemMeta bookmeta = book.getItemMeta();
												bookmeta.setDisplayName("§8【 " + RankColor.getColor(ability.getRank()) + ability.getName() + " §8】");
												bookmeta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
												bookmeta.addEnchant(Enchantment.MENDING, 1, true);
												bookmeta.setLore(lores);
												book.setItemMeta(bookmeta);
												getPlayer().getInventory().addItem(book);
											}
										} catch (ReflectiveOperationException e) {
											e.printStackTrace();
										}
									} else {
										final Mix myMix = (Mix) getParticipant().getAbility();
										final AbilityBase myFirst = myMix.getFirst(), first = targetMix.getFirst(), second = targetMix.getSecond();
										
										try {
											Class<? extends AbilityBase> clazz = (this.equals(myFirst) ? first : second).getClass();
											if (clazz != Empty.class) {
												if (clazz == NineTailFoxC.class) clazz = NineTailFox.class; 
												if (clazz == Kuro.class) clazz = KuroEye.class;
												this.ability = AbilityBase.create(clazz, getParticipant());
												this.ability.setRestricted(false);
												getPlayer().sendMessage("§3[§b!§3] §b능력을 복제하였습니다. 당신의 능력은 " + RankColor.getColor(ability.getRank()) + ability.getName() + "§b 입니다.");
												if (!abilityMap.containsKey("§f【 " + RankColor.getColor(ability.getRank()) + ability.getName() + " §f】")) {
													abilityMap.put("§f【 " + RankColor.getColor(ability.getRank()) + ability.getName() + " §f】", ability.getRegistration());
													ItemStack book = new ItemStack(Material.BOOK, 1);
													ItemMeta bookmeta = book.getItemMeta();
													bookmeta.setDisplayName("§f【 " + RankColor.getColor(ability.getRank()) + ability.getName() + " §f】");
													bookmeta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
													bookmeta.addEnchant(Enchantment.MENDING, 1, true);
													bookmeta.setLore(lores);
													book.setItemMeta(bookmeta);
													getPlayer().getInventory().addItem(book);
												}
											} else {
												getPlayer().sendMessage("§3[§b!§3] §b공백은 복제할 수 없습니다.");
											}
										} catch (ReflectiveOperationException e) {
											e.printStackTrace();
										}
									}

								}

							} else {
								try {
									Class<? extends AbilityBase> clazz = targetAbility.getClass();
									if (clazz != Empty.class) {
										if (clazz == NineTailFoxC.class) clazz = NineTailFox.class; 
										if (clazz == Kuro.class) clazz = KuroEye.class;
										this.ability = AbilityBase.create(clazz, getParticipant());
										this.ability.setRestricted(false);
										getPlayer().sendMessage("§3[§b!§3] §b능력을 복제하였습니다. 당신의 능력은 " + RankColor.getColor(ability.getRank()) + targetAbility.getName() + "§b 입니다.");
										if (!abilityMap.containsKey("§f【 " + RankColor.getColor(ability.getRank()) + ability.getName() + " §f】")) {
											abilityMap.put("§f【 " + RankColor.getColor(ability.getRank()) + ability.getName() + " §f】", ability.getRegistration());
											ItemStack book = new ItemStack(Material.BOOK, 1);
											ItemMeta bookmeta = book.getItemMeta();
											bookmeta.setDisplayName("§f【 " + RankColor.getColor(ability.getRank()) + ability.getName() + " §f】");
											bookmeta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
											bookmeta.addEnchant(Enchantment.MENDING, 1, true);
											bookmeta.setLore(lores);
											book.setItemMeta(bookmeta);
											getPlayer().getInventory().addItem(book);
										}
									} else {
										getPlayer().sendMessage("§3[§b!§3] §b공백은 복제할 수 없습니다.");
									}
								} catch (ReflectiveOperationException e) {
									e.printStackTrace();
								}
							}
						}
					}
				} else if (material == Material.BOOK) {			
					if (abilityMap.containsKey(getPlayer().getInventory().getItemInMainHand().getItemMeta().getDisplayName())) {
						try {
							this.ability = AbilityBase.create(abilityMap.get(getPlayer().getInventory().getItemInMainHand().getItemMeta().getDisplayName()), getParticipant());
							this.ability.setRestricted(false);
							getPlayer().sendMessage("§3[§b!§3] §b능력을 복제하였습니다. 당신의 능력은 " + RankColor.getColor(ability.getRank()) + ability.getName() + "§b 입니다.");
						} catch (ReflectiveOperationException e1) {
							e1.printStackTrace();
						}
					}	
				}
			}
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

	@Override
	protected void onUpdate(Update update) {
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

}