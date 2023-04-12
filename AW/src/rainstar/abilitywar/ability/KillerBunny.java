package rainstar.abilitywar.ability;

import java.text.DecimalFormat;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityBase.ClickType;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import rainstar.abilitywar.effect.ApparentDeath;

@AbilityManifest(name = "살인마 토끼", rank = Rank.L, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §c살의§f: §a근접 공격력§f이 §c살의§fe만§f큼 증가합니다.",
		"§7패시브 §8- §5피의 향§f: 모든 플레이어의 현재 체력 비율을 볼 수 있습니다.",
		" §a근접 공격§f 시 대상의 체력이 33% 이하라면 §c살의§f를 $[MURDER_GAIN]만큼 획득합니다.",
		" 만약 체력이 33%를 넘는다면 §c살의§f의 효과가 $[MULTIPLY]배가 되나 매번 §c살의§f를 $[MURDER_LOSS] 소모합니다.",
		"§7철괴 우클릭 §8- §4물어뜯기§f: 다음 §a근접 공격§f을 강화합니다. $[COOLDOWN]",
		" 강화된 공격은 §c살의§f 효과를 $[SKILL_MULTIPLY]배로 증폭시키고, 대상을 $[APPARENTDEATH_DURATION]초간 §7§n가사§f 상태로 만듭니다.",
		"§8[§7가사§8] §f체력이 33% 이하라면 체력이 고정되고, 공격 및 이동이 불가능해집니다.",
		" 체력이 33%를 초과한다면 받는 피해량이 25% 증가합니다."
		},
		summarize = {
		""
		})
public class KillerBunny extends AbilityBase implements ActiveHandler {
	
	public KillerBunny(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Double> MURDER_GAIN = 
			abilitySettings.new SettingObject<Double>(KillerBunny.class, "murder-gain", 2.5,
			"# 살의 획득량") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> MURDER_LOSS = 
            abilitySettings.new SettingObject<Double>(KillerBunny.class, "murder-loss", 1.0,
            "# 살의 획득량") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };

    public static final SettingObject<Double> MULTIPLY = 
            abilitySettings.new SettingObject<Double>(KillerBunny.class, "multiply", 1.2,
            "# 기본 살의 효과 배수") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
    
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(KillerBunny.class, "cooldown", 85,
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
            abilitySettings.new SettingObject<Double>(KillerBunny.class, "apparent-death-duration", 10.0,
            "# 가사 지속시간") {

        @Override
        public boolean condition(Double value) {
            return value >= 0;
        }

    };
	
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
    
    public void murderfluct(double value) {
    	murder = Math.max(0, murder + value);
    	String color = "§c";
    	if (murder < 25) color = "§c";
    	else if (murder < 50) color = "§5";
    	else if (murder < 100) color = "§4";
    	else color = "§c§l";
    	
    	ac.update("§c살의§7: " + color + df.format(murder) + "§e%");
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
			if (player.getHealth() <= maxHP / 3.0) {
				e.setDamage(e.getDamage() * (1 + power));
				if (!e.isCancelled()) murderfluct(murdergain);
			} else {
				e.setDamage(e.getDamage() * (1 + (power * multiply)));
				murderfluct(murderloss);
			}
		}
    }
    
	public boolean ActiveSkill(Material material, ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.RIGHT_CLICK)) {
			if (upgrade) getPlayer().sendMessage("§4[§c!§4]§f 다음 공격이 이미 §b강화§f 상태입니다.");
			else {
				upgrade = true;
				return cooldown.start();
			}
		}
		return false;
	}
    
    
}
