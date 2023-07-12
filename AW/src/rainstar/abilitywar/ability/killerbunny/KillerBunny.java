package rainstar.abilitywar.ability.killerbunny;

import java.text.DecimalFormat;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.geometry.Line;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import rainstar.abilitywar.effect.ApparentDeath;

@AbilityManifest(name = "살인토끼", rank = Rank.L, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §c살의§f: §a근접 공격력§f이 §c살의§e%§f만큼 증가합니다.",
		" 적 처치 시 §c살의§f $[TRANS]%당 §e흡수 체력§f 반 칸으로 치환합니다.",
		"§7패시브 §8- §5피의 향§f: 모든 플레이어의 현재 체력 비율을 볼 수 있습니다.",
		" §a근접 공격§f 시 대상의 체력이 §444.44§f% 이하라면 §c살의§f를 $[MURDER_GAIN]만큼 획득합니다.",
		" 만약 체력이 §444§f%를 넘는다면 §c살의§f 효과가 $[MULTIPLY]배가 되나 매번 §c살의§f를 $[MURDER_LOSS] 소모합니다.",
		"§7철괴 우클릭 §8- §4물어뜯기§f: 다음 §a근접 공격§f을 강화합니다. $[COOLDOWN]",
		" §c살의§f 효과가 $[SKILL_MULTIPLY]배가 되고, 대상을 $[APPARENTDEATH_DURATION]초간 §7§n가사§f 상태로 만듭니다.",
		"§8[§7가사§8] §f체력이 §444.44§f% 이하라면 체력이 고정되고, 공격 및 이동이 불가능해집니다.",
		" 체력이 §444.44§f%를 초과한다면 받는 피해량이 25% 증가합니다.",
		"§b[§7아이디어 제공자§b] §dspace_kdd"
		},
		summarize = {
		"§444%§f 체력 미만 적에게 §a근접 공격§f 시 스택을 획득해 스택만큼 추가 피해를 입힙니다.",
		"§7철괴 우클릭 시§f 다음 근접 공격을 강화해서 스택 대미지를 추가시키고,",
		"대상을 §7§n가사§f 상태로 만들어 붙잡고 §444%§f 체력 이하로 떨어지지 않게 고정시킵니다."
		})
public class KillerBunny extends AbilityBase implements ActiveHandler {
	
	public KillerBunny(Participant participant) {
		super(participant);
		if (!getGame().hasModule(HealthBar.class)) {
			getGame().addModule(new HealthBar(getGame()));
		}
	}
	
	public static final SettingObject<Double> TRANS = 
			abilitySettings.new SettingObject<Double>(KillerBunny.class, "trans", 4.0,
			"# 살의 치환 비율", "# 설정값% 당 흡수 체력 반 칸입니다.") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> MURDER_GAIN = 
			abilitySettings.new SettingObject<Double>(KillerBunny.class, "murder-gain", 2.0,
			"# 살의 획득량") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> MURDER_LOSS = 
            abilitySettings.new SettingObject<Double>(KillerBunny.class, "murder-loss", 1.5,
            "# 살의 소모량") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };

    public static final SettingObject<Double> MULTIPLY = 
            abilitySettings.new SettingObject<Double>(KillerBunny.class, "multiply", 1.4,
            "# 기본 살의 효과 배수") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(KillerBunny.class, "cooldown", 100,
            "# 철괴 우클릭 쿨타임", "# 단위: 초") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
    public static final SettingObject<Double> SKILL_MULTIPLY = 
            abilitySettings.new SettingObject<Double>(KillerBunny.class, "skill-multiply", 2.0,
            "# 스킬 살의 효과 배수") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
    public static final SettingObject<Double> APPARENTDEATH_DURATION = 
            abilitySettings.new SettingObject<Double>(KillerBunny.class, "apparent-death-duration-", 4.0,
            "# 가사 지속시간") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
	
    private final double transper = TRANS.getValue();
    private final double murdergain = MURDER_GAIN.getValue();
    private final double murderloss = MURDER_LOSS.getValue();
    private final double multiply = MULTIPLY.getValue();
    private final double skillmultiply = SKILL_MULTIPLY.getValue();
    private final int apparentdeath = (int) (APPARENTDEATH_DURATION.getValue() * 20);
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
    
    private double murder = 0;
    private boolean upgrade = false;
    private ActionbarChannel ac = newActionbarChannel();
    private DecimalFormat df = new DecimalFormat("0.0");
    private final RGB redwine = RGB.of(179, 1, 81);
    
    public void murderfluct(double value) {
    	murder = Math.max(0, murder + value);
    	String color = "§c";
    	if (murder < 10) color = "§c";
    	else if (murder < 22.5) color = "§5";
    	else if (murder < 40) color = "§4";
    	else color = "§c§l";
    	
    	ac.update("§c살의§7: " + color + df.format(murder) + "§e%");
    }
    
    @Override
    public void onUpdate(Update update) {
    	if (update == Update.RESTRICTION_CLEAR) {
    		getGame().getModule(HealthBar.class).addPlayer(getPlayer());
    		murderfluct(0);
    	}
    	if (update == Update.ABILITY_DESTROY) {
	    	NMS.setAbsorptionHearts(getPlayer(), 0);
	    	getGame().getModule(HealthBar.class).removePlayer(getPlayer());
	    }
    }
    
	@SubscribeEvent
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (e.getEntity().getKiller() != null && e.getEntity().getKiller().equals(getPlayer())) {
			NMS.setAbsorptionHearts(getPlayer(), NMS.getAbsorptionHearts(getPlayer()) + (int) (murder / transper));
			murderfluct(-murder);
		}
	}
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		if (getPlayer().equals(e.getDamager()) && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			double maxHP = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			double power = (murder / 100);
			if (upgrade) {
				power *= skillmultiply;
				upgrade = false;
				new BukkitRunnable() {
					@Override
					public void run() {
						ApparentDeath.apply(getGame().getParticipant(player), TimeUnit.TICKS, apparentdeath);
					}
				}.runTaskLater(AbilityWar.getPlugin(), 10L);	
			}
			if (player.getHealth() <= maxHP * 0.4444) {
				for (Location loc : Line.between(getPlayer().getLocation().clone().add(0, 1, 0), player.getLocation().clone().add(0, 1, 0), 100).toLocations(getPlayer().getLocation().clone().add(0, 1, 0))) {
					ParticleLib.REDSTONE.spawnParticle(loc, redwine);
				}
				SoundLib.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR.playSound(getPlayer().getLocation(), 1, 0.75f);
				e.setDamage(e.getDamage() * (1 + power));
				if (!e.isCancelled()) murderfluct(murdergain);
			} else if (murder > 0) {
				SoundLib.ENTITY_ZOMBIE_ATTACK_IRON_DOOR.playSound(getPlayer().getLocation(), 1, 1.25f);
				ParticleLib.BLOCK_CRACK.spawnParticle(player.getLocation(), 0.25, 1, 0.25, 5, 1, MaterialX.NETHER_WART_BLOCK);
				e.setDamage(e.getDamage() * (1 + (power * multiply)));
				murderfluct(-murderloss);
			} else SoundLib.ENTITY_RABBIT_ATTACK.playSound(getPlayer().getLocation(), 1f, 1f);
		}
    }
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown()) {
			if (upgrade) getPlayer().sendMessage("§4[§c!§4]§f 다음 공격이 이미 §b강화§f 상태입니다.");
			else {
				SoundLib.ENTITY_RABBIT_ATTACK.playSound(getPlayer().getLocation(), 1f, 1.85f);
				upgrade = true;
				return cooldown.start();
			}
		}
		return false;
	}
    
}
