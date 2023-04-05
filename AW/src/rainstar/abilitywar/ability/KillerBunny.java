package rainstar.abilitywar.ability;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;

@AbilityManifest(name = "살인마 토끼", rank = Rank.L, species = Species.ANIMAL, explain = {
		"§7패시브 §8- §c살의§f: 근접 공격력이 §c살의§f%만큼 증가합니다.",
		"§7패시브 §8- §5피의 향§f: 모든 플레이어의 현재 체력 비율을 볼 수 있습니다.",
		" 근접 공격 시 대상의 체력이 33% 이하라면 §c살의§f를 $[MURDER_GAIN]만큼 획득합니다.",
		" 만약 체력이 33%를 넘는다면 §c살의§f의 효과가 $[MULTIPLY]배가 되나 매번 §c살의§f를 $[MURDER_LOSS] 소모합니다.",
		"§7철괴 우클릭 §8- §4물어뜯기§f: 다음 근접 공격을 강화합니다. $[COOLDOWN]",
		" 강화된 공격은 §c살의§f 효과를 $[SKILL_MULTIPLY]배로 증폭시키고, 대상을 $[APPARENTDEATH_DURATION]초간 §7§n가사§f 상태로 만듭니다.",
		"§8[§7가사§8] §f체력이 33% 이하라면 체력이 고정되고, 공격 및 이동이 불가능해집니다.",
		" 체력이 33%를 초과한다면 받는 피해량이 25% 증가합니다."
		},
		summarize = {
		""
		})
public class KillerBunny extends AbilityBase {
	
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
    
    private double murder = 0;
    
    @SubscribeEvent
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player damager = null;
		if (e.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) e.getDamager();
			if (projectile.getShooter() instanceof Player) damager = (Player) projectile.getShooter();
		} else if (e.getDamager() instanceof Player) damager = (Player) e.getDamager();
		
		if (getPlayer().equals(damager) && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			double maxHP = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			double power = (murder / 100);
			if (player.getHealth() <= maxHP / 3.0) {
				e.setDamage(e.getDamage() * (1 + power));
			} else {
				e.setDamage(e.getDamage() * (1 + (power * multiply)));
			}
		}
    }
    
    
}
