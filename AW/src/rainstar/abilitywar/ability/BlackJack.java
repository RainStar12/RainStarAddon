package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.event.AbilityPreActiveSkillEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "블랙잭", rank = Rank.S, species = Species.HUMAN, explain = {
		"§7철괴 우클릭 §8- §c히트§f: 카드를 한 장 뽑습니다.",
		" 카드는 §e2§7~§e10§7/§e10§7/§e10§7/§e10§f, §e1§8(§e11§8)§f로 이루어져 있습니다.",
		"§7철괴 좌클릭 §8- §7스탠드§f: 모든 카드를 버립니다. $[DURATION]초간,",
		" §8[§c버린 카드의 합계/4§8]§f만큼 공격력이 증가하지만, 카드를 뽑을 수 없습니다.",
		" 만약 버린 카드가 §e10§f과 §e11§f의 조합이라면 §d재생§f 효과도 얻습니다.",
		"§7철괴 F키 §8- §4더블 다운§f: 카드를 한 장 더 뽑고, 즉시 §7스탠드§f합니다.",
		" 그 §7스탠드§f에서는 증가치가 §8[§c합계/3§8]§f가 되고, 지속이 2배가 됩니다.",
		" 그러나 §e16§f 이하도 §c버스트!§f로 취급하고, §c버스트!§f 리스크도 2배로 가집니다.",
		"§721 초과 §8- §c버스트!§f: 즉시 HP를 $[HP_LOSS]%만큼 잃고, $[COOLDOWN]간 모든 스킬이 비활성화됩니다."
		})
public class BlackJack extends AbilityBase implements ActiveHandler {
	
	public BlackJack(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> DURATION = rsAbilitySettings.new SettingObject<Double>(BlackJack.class, 
			"duration", 5.0, "# 스탠드 지속 시간", "# 단위: 초") {
		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}
	};

	public static final SettingObject<Integer> HP_LOSS = rsAbilitySettings.new SettingObject<Integer>(BlackJack.class, 
			"hp-loss", 33, "# 버스트 시 잃는 체력", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}	
	};

	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(BlackJack.class, 
			"cooldown", 30, "# 버스트 후 모든 스킬 쿨타임") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
	};

	private final int duration = (int) (DURATION.getValue() * 20);
	private final double hpLoss = HP_LOSS.getValue() * 0.01;
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
	private final Random random = new Random();
	private List<Integer> cards = new ArrayList<>();
	private double damageIncrease = 0;
	private boolean doubledown = false;
	private final ActionbarChannel ac1 = newActionbarChannel(), ac2 = newActionbarChannel();
	private final DecimalFormat df = new DecimalFormat("0.0");
	private Set<Item> items = new HashSet<>();
	private final RGB black = RGB.of(43, 43, 43), white = RGB.of(254, 254, 254), red = RGB.of(191, 43, 4), gold = RGB.of(242, 215, 64), green = RGB.of(4, 242, 3);
	
	private static final FixedMetadataValue NULL_VALUE = new FixedMetadataValue(AbilityWar.getPlugin(), null);
	
	private final ItemStack[] istacks = {
            MaterialX.GOLD_INGOT.createItem(),
            MaterialX.EMERALD.createItem(),
            MaterialX.DIAMOND.createItem(),
            MaterialX.IRON_INGOT.createItem()
    };
	
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material == Material.IRON_INGOT && !cooldown.isCooldown()) {
			if (!stand.isRunning()) {
				if (clicktype == ClickType.RIGHT_CLICK) {
					SoundLib.ENTITY_PLAYER_ATTACK_NODAMAGE.playSound(getPlayer().getLocation(), 1, 2f);
					int number = random.nextInt(13) + 1;
					if (number > 10) number = 10;
					cards.add(number);
					doubledown = false;
					bust(doubledown);
					return true;
				} else if (clicktype == ClickType.LEFT_CLICK) {
					stand.start();
					return true;
				}	
			} else getPlayer().sendMessage("§c[§8!§c] §7스탠드§f가 아직 지속 중입니다.");
		}
		return false;
	}
	
    @SubscribeEvent(onlyRelevant = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent e) {
    	if (e.getOffHandItem().getType().equals(Material.IRON_INGOT) && e.getPlayer().equals(getPlayer())) { 		
    		if (!stand.isRunning()) {
    			if (!cooldown.isRunning()) {
            		final AbilityPreActiveSkillEvent event = new AbilityPreActiveSkillEvent(this, Material.IRON_INGOT, null);
            		Bukkit.getPluginManager().callEvent(event);
            		if (!event.isCancelled()) {
            			getPlayer().sendMessage("§c[§8!§c] §4더블 다운.");
                		SoundLib.ITEM_BOOK_PAGE_TURN.playSound(getPlayer().getLocation(), 1, 2f);
            			int number = random.nextInt(13) + 1;
            			if (number > 10) number = 10;
            			cards.add(number);
            			doubledown = true;
            			bust(doubledown);
            			if (!cooldown.isRunning()) {
            				stand.start();
            				stand.setCount(stand.getCount() * 2);
            			}
            		}
    			}
    		} else getPlayer().sendMessage("§c[§8!§c] §7스탠드§f가 아직 지속 중입니다.");
    		e.setCancelled(true);
    	}
    }
    
	@Override
	public void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			passive.start();
		}
	}
	
	private final AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			ac1.update(getCardString());
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer stand = new AbilityTimer(duration) {
		
		private double regen = 0;
		
		@Override
		public void onStart() {
			damageIncrease = getSum() / (doubledown ? 3.0 : 4.0);
			if (cards.size() == 2 && getSum() == 21) {
				regen = 0.04;
				SoundLib.UI_TOAST_CHALLENGE_COMPLETE.playSound(getPlayer().getLocation(), 1, 2);
				for (int i = 0; i < 5; i++) {
					final Firework firework = getPlayer().getWorld().spawn(getPlayer().getLocation(), Firework.class);
					final FireworkMeta meta = firework.getFireworkMeta();
					meta.addEffect(
							FireworkEffect.builder()
							.withColor(red.getColor(), gold.getColor(), green.getColor())
							.withTrail()
							.withFlicker()
							.with(random.pick(Type.class))
							.build()
					);	
					meta.setPower(random.nextInt(3));
					firework.setFireworkMeta(meta);
					firework.setMetadata("Firework", NULL_VALUE);
					new BukkitRunnable() {
						@Override
						public void run() {
							firework.detonate();
						}
					}.runTaskLater(AbilityWar.getPlugin(), (long) (1 + (i * 5)));	
				}
			} else {
				SoundLib.ENTITY_FIREWORK_ROCKET_BLAST.playSound(getPlayer().getLocation());
				final Firework firework = getPlayer().getWorld().spawn(getPlayer().getLocation(), Firework.class);
				final FireworkMeta meta = firework.getFireworkMeta();
				meta.addEffect(
						FireworkEffect.builder()
						.withColor(black.getColor(), white.getColor(), red.getColor())
						.withTrail()
						.withFlicker()
						.with(Type.BURST)
						.build()
				);	
				meta.setPower(1);
				firework.setFireworkMeta(meta);
				firework.setMetadata("Firework", NULL_VALUE);
				new BukkitRunnable() {
					@Override
					public void run() {
						firework.detonate();
					}
				}.runTaskLater(AbilityWar.getPlugin(), 1L);
			}
			cards.clear();
			ac2.update("§7스탠드§8: §c" + df.format(duration / 20.0) + "§f초 §7| §b+" + df.format(damageIncrease));
		}
		
		@Override
		public void run(int count) {
			ac2.update("§7스탠드§8: §c" + df.format(count / 20.0) + "§f초 §7| §b+" + df.format(damageIncrease));
			if (count % (regen > 0 ? 2 : 4) == 0) {
            	SoundLib.ENTITY_ITEM_PICKUP.playSound(getPlayer().getLocation(), 0.5f, 1.3f);
                final Item item = getPlayer().getWorld().dropItem(getPlayer().getLocation(), random.pick(istacks));
                items.add(item);
                new AbilityTimer(12) {
                    @Override
                    protected void run(int count) {
                        if (item.isOnGround()) {
                            stop(true);
                        }
                    }
                    @Override
                    protected void onEnd() {
                        item.remove();
                    }
                    @Override
                    protected void onSilentEnd() {
                        item.remove();
                    }
                }.setPeriod(TimeUnit.TICKS, 5).start();
                item.setPickupDelay(Integer.MAX_VALUE);
                item.setVelocity(new Vector(((random.nextDouble() * 2) - 1) / 2, .7, ((random.nextDouble() * 2) - 1) / 2));
			}
		}
		
		@Override
		public void onEnd() {
			onSilentEnd();
		}
		
		@Override
		public void onSilentEnd() {
			regen = 0;
			doubledown = false;
			ac2.update(null);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private int getSum() {
		int sum = 0;
		int ace = 0;

		for (int card : cards) {
			if (card == 1) {
				ace++;
				sum += 11;
			} else {
				sum += card;
			}
		}

		while (sum > 21 && ace > 0) {
			sum -= 10;
			ace--;
		}

		return sum;
	}
	
	private void bust(boolean doubledown) {
		if (getSum() > 21) {
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 1, 1f);
			cards.clear();
			Healths.setHealth(getPlayer(), Math.max(1, getPlayer().getHealth() - (getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * hpLoss * (doubledown ? 2 : 1))));
			NMS.broadcastEntityEffect(getPlayer(), (byte) 2);
			cooldown.start();
			if (doubledown) cooldown.setCount(cooldown.getCooldown() * 2);
		} else if (doubledown && getSum() < 17) {
			SoundLib.ENTITY_GENERIC_EXPLODE.playSound(getPlayer().getLocation(), 1, 1f);
			cards.clear();
			Healths.setHealth(getPlayer(), Math.max(1, getPlayer().getHealth() - (getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * hpLoss * 2)));
			NMS.broadcastEntityEffect(getPlayer(), (byte) 2);
			cooldown.start();
			cooldown.setCount(cooldown.getCooldown() * 2);
		}
	}
	
	public String getCardString() {
		StringBuilder builder = new StringBuilder();

		for (int card : cards) {
			builder.append("§7[§e").append(card).append("§7] ");
		}

		builder.append(" §8(§a").append(getSum()).append("§8)");

		return builder.toString();
	}
	
	@SubscribeEvent()
	private void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (e.getDamager() != null && e.getDamager().hasMetadata("Firework")) {
			e.setCancelled(true);
		}
		
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && stand.isRunning()) {
			e.setDamage(e.getDamage() + damageIncrease);
			final Firework firework = e.getEntity().getWorld().spawn(e.getEntity().getLocation(), Firework.class);
			final FireworkMeta meta = firework.getFireworkMeta();
			meta.addEffect(
					FireworkEffect.builder()
					.withColor(white.getColor(), red.getColor(), black.getColor())
					.with(Type.BURST)
					.build()
			);	
			meta.setPower(0);
			firework.setFireworkMeta(meta);
			firework.setMetadata("Firework", NULL_VALUE);
			new BukkitRunnable() {
				@Override
				public void run() {
					firework.detonate();
				}
			}.runTaskLater(AbilityWar.getPlugin(), 1L);
		}
	}
	
}
