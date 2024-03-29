package rainstar.abilitywar.ability;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.GameManager;
import daybreak.abilitywar.game.list.mix.Mix;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.module.Wreck;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil;
import daybreak.abilitywar.utils.base.language.korean.KoreanUtil.Josa;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableMap;

@AbilityManifest(name = "구미호", rank = Rank.A, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §d수행§f: 매 $[PERIOD]초마다 꼬리 1개를 획득합니다.",
		" 경험하지 못한 피해 방식으로 피해입었을 때 추가로 1개 더 획득합니다.",
		" 꼬리를 보유하고 있을 때, 공격력이 꼬리당 $[DAMAGE_INCREASE]% 상승합니다.",
		" 꼬리는 2개로 시작하여 최대 9개까지 소지 가능합니다.",
		"§7철괴 좌클릭 §8- §b기록§f: 피해입은 방식에 대한 기록이 나타납니다.",
		"§7철괴 우클릭 §8- §d둔갑§f: 꼬리가 9개일 때 사용할 수 있습니다.",
		" 사용 시 둔갑하여 §e구미호(둔갑)§f 능력이 되고 모든 꼬리를 잃습니다."
		},
		summarize = {
		"미경험 피해 방법으로 피해입을 때마다 꼬리 1개를 즉시 획득합니다.",
		"꼬리가 5개 이상일 때 꼬리 하나당 주는 대미지가 0.5씩 상승합니다.",
		"꼬리가 9개일 때 §7철괴 우클릭 시§f §e구미호(둔갑)§f으로 둔갑합니다."
		})

public class NineTailFox extends AbilityBase implements ActiveHandler {
	
	private static final ImmutableMap<DamageCause, String> damageCauses = ImmutableMap.<DamageCause, String>builder()
							.put(DamageCause.BLOCK_EXPLOSION, "침대 폭발")
							.put(DamageCause.CONTACT, "선인장 접촉")
							.put(DamageCause.CRAMMING, "엔티티 사이에 끼임")
							.put(DamageCause.CUSTOM, "커스텀")
							.put(DamageCause.DROWNING, "익사")
							.put(DamageCause.ENTITY_ATTACK, "일반 공격")
							.put(DamageCause.ENTITY_EXPLOSION, "폭발")
							.put(DamageCause.ENTITY_SWEEP_ATTACK, "휩쓸기 공격")
							.put(DamageCause.FALL, "낙하")
							.put(DamageCause.FALLING_BLOCK, "떨어지는 블록에 맞음")
							.put(DamageCause.FIRE, "불")
							.put(DamageCause.FIRE_TICK, "화염")
							.put(DamageCause.FLY_INTO_WALL, "벽에 박음")
							.put(DamageCause.HOT_FLOOR, "마그마블록")
							.put(DamageCause.LAVA, "용암")
							.put(DamageCause.LIGHTNING, "번개")
							.put(DamageCause.MAGIC, "마법")
							.put(DamageCause.POISON, "독")
							.put(DamageCause.PROJECTILE, "발사체")
							.put(DamageCause.STARVATION, "굶주림")
							.put(DamageCause.SUFFOCATION, "압사")
							.put(DamageCause.THORNS, "가시 인챈트")
							.put(DamageCause.VOID, "세계 밖으로 떨어짐")
							.put(DamageCause.WITHER, "시듦")
							.build();

	public NineTailFox(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> DAMAGE_INCREASE = abilitySettings.new SettingObject<Double>(NineTailFox.class,
			"damage-increase", 1.5, "# 꼬리 공격력 증가량", "# 단위: %") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Integer> PERIOD = abilitySettings.new SettingObject<Integer>(NineTailFox.class,
			"period", 25, "# 꼬리 자동 충전 시간", "# 단위: 초") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	private final double increasep = DAMAGE_INCREASE.getValue() * 0.01;
	private final ActionbarChannel ac = newActionbarChannel();
	private int stack = 2;	
	private final int period = (int) (Wreck.isEnabled(GameManager.getGame()) ? Wreck.calculateDecreasedAmount(25) * PERIOD.getValue() * 20 : PERIOD.getValue() * 20);
	private Set<DamageCause> damagetype = new HashSet<>();
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR) {
			ac.update("§b꼬리 수§f: " + stack + "개");
			passive.start();
		} else if (update == Update.ABILITY_DESTROY || update == Update.RESTRICTION_SET) {
			ac.unregister();
			stack = 2;
			damagetype.clear();
		}
	}
	
    private final AbilityTimer passive = new AbilityTimer() {
    	
    	@Override
		public void run(int count) {
    		if (period != 0) {
        		if (count % period == 0) stackup();
    		} else stackup();
    	}
    	
    }.setBehavior(RestrictionBehavior.PAUSE_RESUME).setPeriod(TimeUnit.TICKS, 1).register();
	
    public void stackup() {
    	if (stack < 9) {
			stack++;
			ac.update("§b꼬리 수§f: " + stack + "개");
    	}
    }
    
    @SubscribeEvent
    public void onEntityDamage(EntityDamageEvent e) {
    	if (e.getEntity().equals(getPlayer())) {
			if (!damagetype.contains(e.getCause()) && !e.getCause().equals(DamageCause.SUICIDE) && !e.getCause().equals(DamageCause.DRAGON_BREATH)) {
				damagetype.add(e.getCause());
				getPlayer().sendMessage("[§c!§f] §c" + damageCauses.get(e.getCause()) + "§f" + KoreanUtil.getJosa(damageCauses.get(e.getCause()), Josa.을를) + " 통하여 수행을 성공했습니다.");
				SoundLib.BLOCK_ENCHANTMENT_TABLE_USE.playSound(getPlayer(), 1, 1.4f);
				ParticleLib.ENCHANTMENT_TABLE.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 200, 1);
				stackup();
			}
		}
    }
    
    @SubscribeEvent
    public void onEntityDamageByBlock(EntityDamageByBlockEvent e) {
    	onEntityDamage(e);
    }
    
	@SubscribeEvent
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager)) e.setDamage(e.getDamage() * (1 + (stack * increasep)));
	}
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && stack == 9) {
    		SoundLib.ITEM_ARMOR_EQUIP_LEATHER.playSound(getPlayer());
	    	getPlayer().sendMessage("§5[§d!§5] §e둔갑에 성공하셨습니다. §7/aw check");
	    	AbilityBase ab = getParticipant().getAbility();
	    	if (ab.getClass().equals(Mix.class)) {
	    		final Mix mix = (Mix) ab;
				final AbilityBase first = mix.getFirst(), second = mix.getSecond();
				if (this.equals(first)) {
					try {
						mix.setAbility(NineTailFoxC.class, second.getClass());
					} catch (ReflectiveOperationException e) {
						e.printStackTrace();
					}
				} else if (this.equals(second)) {
					try {
						mix.setAbility(first.getClass(), NineTailFoxC.class);
					} catch (ReflectiveOperationException e) {
						e.printStackTrace();
					}
				}
	    	} else {
		    	try {
					getParticipant().setAbility(NineTailFoxC.class);
				} catch (UnsupportedOperationException | ReflectiveOperationException e) {
					e.printStackTrace();
				}
	    	}
	    	return true;
	    }
	    if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK)) {
	    	getPlayer().sendMessage("§c==== §d수행 방법 §c====");
			final StringJoiner joiner = new StringJoiner("§f, ");
			for (final Entry<DamageCause, String> entry : damageCauses.entrySet()) {
				joiner.add((damagetype.contains(entry.getKey()) ? "§e" : "§7") + entry.getValue());
			}
			getPlayer().sendMessage(joiner.toString());
			getPlayer().sendMessage("§c====================");
	    }
		return false;
	}
	
}