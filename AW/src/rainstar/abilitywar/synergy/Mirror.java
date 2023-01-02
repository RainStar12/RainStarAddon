package rainstar.abilitywar.synergy;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Note.Tone;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.mix.synergy.Synergy;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.damage.Damages;
import daybreak.abilitywar.utils.base.minecraft.entity.decorator.Deflectable;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.base.Predicate;
import daybreak.google.common.collect.ImmutableSet;

@AbilityManifest(name = "거울", rank = Rank.S, species = Species.OTHERS, explain = {
		"§7패시브 §8- §b내구도§f: §2리턴§f 효과를 발동할 때마다 §b내구도§f가 $[DURABILITY] 감소합니다.",
		" 반격 대상이 없는 피해의 경우 §b내구도§f를 2배로 감소시킵니다.",
		" §b내구도§f 전부 사용 시 모든 스킬에 쿨타임을 가집니다. $[COOLDOWN]",
		"§7패시브 §8- §2리턴§f: 어떠한 피해를 입던 방어된 만큼의 피해량의 $[RETURN]%를",
		" 피해를 입힌 대상에게 1초 후 다시 되돌려줍니다.",
		" 이후 §c반격§f으로 입힌 피해의 $[REGEN]%만큼 §d체력을 회복§f합니다.",
		"§7검 휘두르기 §8- §3에코§f: 발사체를 바라보고 튕겨낼 수 있습니다.",
		" $[ARROW_COUNT]번 튕겨낼 때마다 방어 계열 버프 중 하나를 무작위로 얻습니다.",
		" 혹은 근접 피해를 빠르게 $[DAMAGE_COUNT]번 되받아칠 때에도 이 효과를 얻습니다.",
		" 효과 발동 시마다, §b내구도§f가 $[FIX]만큼 수리됩니다."
})

public class Mirror extends Synergy {
	
	public Mirror(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = synergySettings.new SettingObject<Integer>(Mirror.class,
			"cooldown", 40, "# 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}
	};
	
	public static final SettingObject<Integer> DURABILITY = synergySettings.new SettingObject<Integer>(Mirror.class,
			"durability-decrease", 1, "# 내구도 감소") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> FIX = synergySettings.new SettingObject<Integer>(Mirror.class,
			"fix", 5, "# 내구도 수리") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> MAX_DURABILITY = synergySettings.new SettingObject<Integer>(Mirror.class,
			"max-durability", 30, "# 총 내구도") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> ARROW_COUNT = synergySettings.new SettingObject<Integer>(Mirror.class,
			"arrow-count", 2, "# 화살을 튕겨내는 횟수") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> DAMAGE_COUNT = synergySettings.new SettingObject<Integer>(Mirror.class,
			"damage-count", 4, "# 근접 공격을 반격하는 횟수") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> REGEN = synergySettings.new SettingObject<Integer>(Mirror.class,
			"regen", 15, "# 회복량", "# 단위: %") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	public static final SettingObject<Integer> RETURN = synergySettings.new SettingObject<Integer>(Mirror.class,
			"return", 75, "# 반격 피해량", "# 단위: %") {
		
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
		
	};
	
	protected void onUpdate(AbilityBase.Update update) {
		if (update == AbilityBase.Update.RESTRICTION_CLEAR) {
			actionbar.start();
			durabilityupdater.start();
		}
	}
	
	private int arrowcount = 0;
	private int damagecount = 0;
	private final static int needArrowCount = ARROW_COUNT.getValue();
	private final static int needDamageCount = DAMAGE_COUNT.getValue();
	private boolean block = false;
	private boolean effectblock = false;
	private PotionEffect resistance = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 300, 0, false, true);
	private PotionEffect fireresistance = new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 0, false, true);
	private int stack = 0;
	private final int returns = RETURN.getValue();
	private final int regen = REGEN.getValue();
	private ActionbarChannel ac = newActionbarChannel();
	private final int maxDurability = MAX_DURABILITY.getValue();
	private final int decDurability = DURABILITY.getValue();
	private final int fixDurability = FIX.getValue();
	private double durability = maxDurability;
	private final DecimalFormat df = new DecimalFormat("0");
	private BossBar bossBar = null;
	
	private Player target;
	
	private static final Set<Material> swords;
	
	private final Predicate<Effect> effectpredicate = new Predicate<Effect>() {
		@Override
		public boolean test(Effect effect) {
			return true;
		}

		@Override
		public boolean apply(@Nullable Effect arg0) {
			return false;
		}
	};
	
	static {
		if (MaterialX.NETHERITE_SWORD.isSupported()) {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD, MaterialX.NETHERITE_SWORD.getMaterial());
		} else {
			swords = ImmutableSet.of(MaterialX.WOODEN_SWORD.getMaterial(), Material.STONE_SWORD, Material.IRON_SWORD, MaterialX.GOLDEN_SWORD.getMaterial(), Material.DIAMOND_SWORD);
		}
	}
	
	private final AbilityTimer cooldown = new AbilityTimer(COOLDOWN.getValue() * 20) {
		
		@Override
		public void onEnd() {
			durability = maxDurability;
			durabilityupdater.start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer durabilityupdater = new AbilityTimer() {
		
    	@Override
    	public void onStart() {
    		bossBar = Bukkit.createBossBar("§b내구도 §f: §7" + df.format(durability), BarColor.WHITE, BarStyle.SOLID);
    		bossBar.setProgress(durability / maxDurability);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    	}
    	
    	@Override 
		public void run(int count) {
    		if (cooldown.isRunning()) {
    			bossBar.setColor(BarColor.RED);
    			bossBar.setProgress(cooldown.getCount() / (double) (COOLDOWN.getValue() * 20));
    		} else {
        		bossBar.setTitle("§b내구도 §f: §7" + df.format(durability));
        		bossBar.setProgress(durability / maxDurability);
    		}
    	}
    	
		@Override
		public void onEnd() {
			bossBar.removeAll();
		}

		@Override
		public void onSilentEnd() {
			bossBar.removeAll();
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer actionbar = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			String A, B, C;
			A = block ? "§bO" : "§cX";
			B = effectblock ? "§bO" : "§cX";
			C = undying.isRunning() ? "§bO" : "§cX";
			ac.update("§3피해 무시§f: " + A + " §7| §6상태이상 무시§f: " + B + " §7| §a불사의 토템§f: " + C);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer undying = new AbilityTimer(600) {
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer counter = new AbilityTimer(10) {
		
    }.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer defenceUp = new AbilityTimer(10) {
		
		private AttributeModifier defenceup;
		
		@Override
		public void onStart() {
			defenceup = new AttributeModifier(UUID.randomUUID(), "defenceup", 6, Operation.ADD_NUMBER);
			getPlayer().getAttribute(Attribute.GENERIC_ARMOR).addModifier(defenceup);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			getPlayer().getAttribute(Attribute.GENERIC_ARMOR).removeModifier(defenceup);
		}
		
	}.setPeriod(TimeUnit.SECONDS, 1).register();
	
	@SubscribeEvent
	public void onEntityResurrectEvent(EntityResurrectEvent e) {
		if (undying.isRunning() && e.getEntity().equals(getPlayer())) {
			ItemStack leftHand = getPlayer().getInventory().getItemInOffHand();
			e.setCancelled(false);
			getPlayer().getInventory().setItemInOffHand(leftHand);
			undying.stop(false);
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onParticipantEffectApply(ParticipantPreEffectApplyEvent e) {
		if (effectblock) {
			SoundLib.ENTITY_PLAYER_LEVELUP.playSound(getPlayer().getLocation());
			getPlayer().sendMessage("§3[§b!§3] §f상태이상을 무효화시켰습니다.");
			e.setCancelled(true);
			effectblock = false;
		}
	}
	
	@SubscribeEvent
	public void onEntityDamage(EntityDamageEvent e) {
		if (e.getEntity().equals(getPlayer()) && !e.isCancelled()) {
			if (block) {
				SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation());
				e.setCancelled(true);
				block = false;	
			} else {
				if (!cooldown.isRunning()) {
					if (!e.getCause().equals(DamageCause.ENTITY_ATTACK) && !e.getCause().equals(DamageCause.ENTITY_SWEEP_ATTACK) &&
							!e.getCause().equals(DamageCause.MAGIC) && !e.getCause().equals(DamageCause.PROJECTILE)) {
						durability = Math.max(0, durability - (decDurability * 2));
						if (durability <= 0) {
							SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer(), 1, 1);
							SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer(), 1, 0.5f);
							SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer(), 1, 0.75f);
							ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0, 2, 0, 50, 0.5, MaterialX.GLASS);
							cooldown.start();
						}
					}	
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
		onEntityDamage(e);
	}
	
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		onEntityDamage(e);
		if (!cooldown.isRunning()) {
			if (e.getEntity().equals(getPlayer()) && e.getDamager() instanceof Player) {
				target = (Player) e.getDamager();
				counter.start();	
			}
			if (target != null) {
				if (e.getEntity().equals(target.getPlayer()) && e.getDamager().equals(getPlayer()) && counter.isRunning()) {
					damagecount++;
					if (damagecount >= needDamageCount) {
						roulette.start();
						damagecount = 0;
					}
					SoundLib.BLOCK_ANVIL_LAND.playSound(target.getLocation(), 1, 2f);
				}
			}
			
			if (e.getEntity().equals(getPlayer()) && e.getDamager() != null && !getPlayer().equals(e.getDamager())) {
				if (e.getDamager() instanceof Projectile) {
					Projectile p = (Projectile) e.getDamager();
					if (p.getShooter() != null) {
						if (p.getShooter() instanceof Damageable && !getPlayer().equals(p.getShooter())) {
							Damageable d = (Damageable) p.getShooter();
							double returnDamage = (e.getDamage() - e.getFinalDamage());
							new AbilityTimer(20) {
								
								@Override
								public void onEnd() {
									onSilentEnd();
								}
								
								@Override
								public void onSilentEnd() {
									if (d != null) {
										if (e.getCause().equals(DamageCause.MAGIC)) Damages.damageMagic(d, getPlayer(), true, (float) returnDamage);
										else Damages.damageArrow(d, getPlayer(), (float) returnDamage);
										final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), (returnDamage * ((double) regen / 100)), RegainReason.CUSTOM);
										Bukkit.getPluginManager().callEvent(event);
										if (!event.isCancelled() && !getPlayer().isDead()) {
											Healths.setHealth(getPlayer(), getPlayer().getHealth() + (returnDamage * ((double) regen / 100)));	
										}
										ParticleLib.SWEEP_ATTACK.spawnParticle(d.getLocation().clone().add(0, 1, 0));
										SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(d.getLocation());
										durability = Math.max(0, durability - decDurability);
										if (durability <= 0) {
											SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer().getLocation(), 1, 1);
											SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer().getLocation(), 1, 0.5f);
											SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer().getLocation(), 1, 0.75f);
											ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0, 2, 0, 50, 0.5, MaterialX.GLASS);
											cooldown.start();
										}
									}
								}
								
							}.setPeriod(TimeUnit.TICKS, 1).start();	
						}
					}
				} else if (e.getDamager() instanceof Damageable) {
					Damageable d = (Damageable) e.getDamager();
					double returnDamage = (e.getDamage() - e.getFinalDamage());
					new AbilityTimer(20) {
						
						@Override
						public void onEnd() {
							onSilentEnd();
						}
						
						@Override
						public void onSilentEnd() {
							if (d != null) {
								d.damage((returnDamage * ((double) returns / 100)), getPlayer());
								final EntityRegainHealthEvent event = new EntityRegainHealthEvent(getPlayer(), (returnDamage * ((double) regen / 100)), RegainReason.CUSTOM);
								Bukkit.getPluginManager().callEvent(event);
								if (!event.isCancelled() && !getPlayer().isDead()) {
									Healths.setHealth(getPlayer(), getPlayer().getHealth() + (returnDamage * ((double) regen / 100)));	
								}
								ParticleLib.SWEEP_ATTACK.spawnParticle(d.getLocation().clone().add(0, 1, 0));
								SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(d.getLocation());
								durability = Math.max(0, durability - decDurability);
								if (durability <= 0) {
									SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer().getLocation(), 1, 1);
									SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer().getLocation(), 1, 0.5f);
									SoundLib.BLOCK_GLASS_BREAK.playSound(getPlayer().getLocation(), 1, 0.75f);
									ParticleLib.BLOCK_CRACK.spawnParticle(e.getEntity().getLocation(), 0, 2, 0, 50, 0.5, MaterialX.GLASS);
									cooldown.start();
								}
							}
						}
						
					}.setPeriod(TimeUnit.TICKS, 1).start();	
				}
			}	
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(PlayerInteractEvent e) {
		if (!cooldown.isRunning()) {
			if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && e.getItem() != null && swords.contains(e.getItem().getType())) {
				if (!deflect(LocationUtil.getEntityLookingAt(Entity.class, getPlayer(), 5, .75, null), true)) {
					deflect(LocationUtil.getCustomEntityLookingAt(Deflectable.class, getGame(), getPlayer(), 5, .75,  null), true);
				}
			}	
		}
	}
	
	private final AbilityTimer roulette = new AbilityTimer(7) {
		
		private int number;
		private Random random = new Random();
		
		@Override
		protected void onStart() {
			durability = Math.min(maxDurability, durability + fixDurability);
			number = random.nextInt(7);
		}
		
		@Override
		protected void run(int count) {
			stack++;
			double temp = (double) stack;
			int soundnumber = (int) (temp - (Math.ceil(temp / SOUND_RUNNABLES.size()) - 1) * SOUND_RUNNABLES.size()) - 1;
			SOUND_RUNNABLES.get(soundnumber).run();
			String string = null;
			switch(random.nextInt(7)) {
			case 0:
				string = "§7저항";
				break;
			case 1:
				string = "§3방어력";
				break;
			case 2:
				string = "§e흡수 체력";
				break;
			case 3:
				string = "§c화염 저항";
				break;
			case 4:
				string = "§b피해 무시";
				break;
			case 5:
				string = "§a불사의 토템";
				break;
			case 6:
				string = "§6상태이상 해제";
				break;
			}
			NMS.sendTitle(getPlayer(), "§d무작위 버프", string, 0, 6, 0);
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			new AbilityTimer(5) {
				
				@Override
				public void onStart() {
					stack++;
					double temp = (double) stack;
					int soundnumber = (int) (temp - (Math.ceil(temp / SOUND_RUNNABLES.size()) - 1) * SOUND_RUNNABLES.size()) - 1;
					SOUND_RUNNABLES.get(soundnumber).run();
					String string = null;
					switch(number) {
					case 0:
						string = "§7저항";
						if (getPlayer().hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
							PotionEffect pe = getPlayer().getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
							if (pe.getAmplifier() < 1 || pe.getDuration() <= 300) {
								getPlayer().removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
							}
						}
						getPlayer().addPotionEffect(resistance);
						getPlayer().sendMessage("§3[§b!§3] §7저항 1 15초 버프!");
						break;
					case 1:
						string = "§3방어력";
						defenceUp.start();
						getPlayer().sendMessage("§3[§b!§3] §310초간 방어력 3칸 상승!");
						break;
					case 2:
						string = "§e흡수 체력";
						NMS.setAbsorptionHearts(getPlayer(), (float) (NMS.getAbsorptionHearts(getPlayer()) + 5));
						getPlayer().sendMessage("§3[§b!§3] §e영구 흡수체력 2.5칸 증가!");
						break;
					case 3:
						string = "§c화염 저항";
						if (getPlayer().hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
							PotionEffect pe = getPlayer().getPotionEffect(PotionEffectType.FIRE_RESISTANCE);
							if (pe.getDuration() <= 300) {
								getPlayer().removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
							}
						}
						getPlayer().addPotionEffect(fireresistance);
						getPlayer().sendMessage("§3[§b!§3] §c화염 저항 1분 버프!");
						break;
					case 4:
						string = "§b피해 무시";
						block = true;
						getPlayer().sendMessage("§3[§b!§3] §b다음 피해 1회 무시!");
						break;
					case 5:
						string = "§a불사의 토템";
						if (undying.isRunning()) undying.setCount(600);
						else undying.start();
						getPlayer().sendMessage("§3[§b!§3] §a30초 내에 불사의 토템 1회 발동!");
						break;
					case 6:
						string = "§6상태이상 해제";
						effectblock = true;
						getParticipant().removeEffects(effectpredicate);
						getPlayer().sendMessage("§3[§b!§3] §6상태이상 즉시 해제 및 다음 상태이상 1회 무시!");
						break;
					}
					NMS.sendTitle(getPlayer(), "§d무작위 버프", string, 0, 6, 0);
				}
				
				@Override
				public void onEnd() {
					onSilentEnd();
				}
				
				@Override
				public void onSilentEnd() {
					NMS.clearTitle(getPlayer());
				}
				
			}.setPeriod(TimeUnit.TICKS, 1).start();
		}
		
	}.setPeriod(TimeUnit.TICKS, 3).register();
	
	private boolean deflect(Entity entity, boolean playerDirection) {
		if (entity == null) return false;
		if (entity instanceof Projectile) {
			return deflect((Projectile) entity, playerDirection);
		} else {
			final List<MetadataValue> metadatas = entity.getMetadata("deflectable");
			if (!metadatas.isEmpty()) {
				final Object value = metadatas.get(0).value();
				if (value instanceof Deflectable) {
					return deflect((Deflectable) value, playerDirection);
				}
			}
		}
		return false;
	}

	private boolean deflect(Projectile projectile, boolean playerDirection) {
		if (projectile != null && !projectile.isOnGround() && projectile.isValid() && !getPlayer().equals(projectile.getShooter())) {
			if (projectile.hasMetadata("flector")) {
				if (getPlayer().getUniqueId().equals(projectile.getMetadata("flector").get(0).value())) {
					return false;
				} else {
					projectile.removeMetadata("flector", AbilityWar.getPlugin());
				}
			}
			projectile.setVelocity(playerDirection ? getPlayer().getLocation().getDirection().multiply(2.2 * NMS.getAttackCooldown(getPlayer())) : projectile.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().add(projectile.getVelocity().multiply(-1)));
			projectile.setMetadata("flector", new FixedMetadataValue(AbilityWar.getPlugin(), getPlayer().getUniqueId()));
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer());
			ParticleLib.SWEEP_ATTACK.spawnParticle(projectile.getLocation());
			arrowcount++;
			if (arrowcount >= needArrowCount) {
				roulette.start();
				arrowcount = 0;
			}
			return true;
		}
		return false;
	}

	private boolean deflect(Deflectable deflectable, boolean playerDirection) {
		if (deflectable != null && !getPlayer().equals(deflectable.getShooter())) {
			deflectable.onDeflect(getParticipant(), playerDirection ? getPlayer().getLocation().getDirection().multiply(2.2 * NMS.getAttackCooldown(getPlayer())) : deflectable.getLocation().toVector().subtract(getPlayer().getLocation().toVector()).normalize().add(deflectable.getDirection().multiply(-1)));
			SoundLib.ENTITY_PLAYER_ATTACK_SWEEP.playSound(getPlayer());
			ParticleLib.SWEEP_ATTACK.spawnParticle(deflectable.getLocation());
			arrowcount++;
			if (arrowcount >= needArrowCount) {
				roulette.start();
				arrowcount = 0;
			}
			return true;
		}
		return false;
	}
	
	private final List<Runnable> SOUND_RUNNABLES = Arrays.asList(
			
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.G));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.G));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.G));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.F));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.G));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(1, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(1, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(1, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(1, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.F));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.C));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(0, Tone.F));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.C));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.C));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.F));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.C));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.G));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.F));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.sharp(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.F));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.F));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.F));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.D));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.A));
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(1, Tone.A));
			},
			() -> {
					SoundLib.PIANO.playInstrument(getPlayer(), Note.natural(0, Tone.D));
					stack = 0;
			}
			
	);

}