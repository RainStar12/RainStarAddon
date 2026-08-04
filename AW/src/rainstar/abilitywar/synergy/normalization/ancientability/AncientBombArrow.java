package rainstar.abilitywar.synergy.normalization.ancientability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import org.bukkit.event.entity.ProjectileHitEvent;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "고대의 폭발화살", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
        "화살을 맞췄을 때 $[CHANCE]% 확률로 폭발을 일으킵니다."
		})
public class AncientBombArrow extends AbilityBase {

	public AncientBombArrow(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> CHANCE = rsAbilitySettings.new SettingObject<Integer>(AncientBombArrow.class, 
			"chance", 50, "# 확률", "# 단위: %") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> SIZE = rsAbilitySettings.new SettingObject<Double>(AncientBombArrow.class, 
			"size", 2.0, "# 폭발 위력") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final int chance = CHANCE.getValue();
	private final float size = (float) (SIZE.getValue() * 1.0f);
	private Random random = new Random();
	
	@SubscribeEvent
	public void onProjectileHit(ProjectileHitEvent e) {
		if (getPlayer().equals(e.getEntity().getShooter())) {
			if (NMS.isArrow(e.getEntity()) && random.nextInt(100) < chance) {
				SoundLib.BLOCK_NOTE_BLOCK_BELL.playSound(getPlayer());
				e.getEntity().getLocation().getWorld().createExplosion(e.getEntity().getLocation(), size, false);
				e.getEntity().remove();
			}
		}
	}

}
