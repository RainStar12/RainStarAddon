package RainStarAbility;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;

import org.bukkit.block.data.BlockData;

import RainStarEffect.VainDream;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "일장춘몽", rank = Rank.S, species = Species.HUMAN, explain = {
		"철괴 좌클릭 시, 자신의 체력을 최대 체력으로 만듭니다. $[COOLDOWN]",
		"이후 이 효과로 채운 체력 반 칸당 1초의 §d덧없는 꿈§f 상태이상을 받습니다.",
		"§5[§d덧없는 꿈§5]§f 누군가에게 피해입기 전까지 이동할 수 없으며,",
		" 깨어나면 공격이 절반의 확률로 빗나갑니다. 지속적으로 체력을 잃습니다.",
		" 생존 시, 덧없는 꿈 효과로 잃어버린 체력은 복구됩니다.",
		"§b[§7아이디어 제공자§b] §eYeow_ool"
		},
		summarize = {
		"철괴 좌클릭 시, 자신의 체력을 최대 체력으로 만듭니다. $[COOLDOWN]",
		"이후 이 효과로 채운 체력 반 칸당 1초의 §d덧없는 꿈§f 상태이상을 받습니다.",
		"§5[§d덧없는 꿈§5]§f 누군가에게 피해입기 전까지 이동할 수 없으며,",
		" 깨어나면 공격이 절반의 확률로 빗나갑니다. 지속적으로 체력을 잃습니다.",
		" 생존 시, 덧없는 꿈 효과로 잃어버린 체력은 복구됩니다."
		})

public class Daydream extends AbilityBase implements ActiveHandler {

	public Daydream(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = 
			abilitySettings.new SettingObject<Integer>(Daydream.class, "cooldown", 70,
            "# 쿨타임") {
        @Override
        public boolean condition(Integer value) {
            return value >= 0;
        }
        @Override
        public String toString() {
            return Formatter.formatCooldown(getValue());
        }
    };
    
    private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	
	public boolean ActiveSkill(Material material, AbilityBase.ClickType clicktype) {
		if (material.equals(Material.IRON_INGOT) && clicktype.equals(ClickType.LEFT_CLICK) && !cooldown.isCooldown()) {
			if (ServerVersion.getVersion() >= 13) {
				BlockData powder = Material.PINK_WOOL.createBlockData();
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 1, 2, 1, 150, 0, powder);
			} else {
				ItemStack powder = new ItemStack(Material.getMaterial("WOOL"), 1, (byte) 6);
				ParticleLib.FALLING_DUST.spawnParticle(getPlayer().getLocation(), 1, 2, 1, 150, 0, powder.getData());
			}
			SoundLib.ENTITY_ENDER_DRAGON_FLAP.playSound(getPlayer().getLocation(), 1f, 2f);
			double maxHP = getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			double gain = maxHP - getPlayer().getHealth();
			Healths.setHealth(getPlayer(), maxHP);
			VainDream.apply(getParticipant(), TimeUnit.SECONDS, (int) gain);
			return cooldown.start();
		}
		return false;
	}
	
}