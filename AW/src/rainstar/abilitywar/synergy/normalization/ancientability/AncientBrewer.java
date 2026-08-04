package rainstar.abilitywar.synergy.normalization.ancientability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionType;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.ItemLib;

@AbilityManifest(name = "고대의 양조사", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
        "철괴를 우클릭하면 임의의 §e포션§f 세 개를 얻습니다. $[COOLDOWN]"
		})
public class AncientBrewer extends AbilityBase implements ActiveHandler {

	public AncientBrewer(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(AncientBrewer.class, 
			"cooldown", 50, "# 쿨타임") {

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
	
	public boolean ActiveSkill(Material material, ClickType clickType) {
		if (material.equals(Material.IRON_INGOT) && clickType.equals(ClickType.RIGHT_CLICK) && !cooldown.isCooldown()) {
			Inventory brewingGUI = Bukkit.createInventory(null, InventoryType.BREWING, "");
			Player player = getPlayer();
			Random random = new Random();
			for (int i = 0; i < 3; i++) {
				brewingGUI.setItem(i, new ItemLib.PotionBuilder(
						PotionType.values()[random.nextInt(PotionType.values().length)],
						ItemLib.PotionBuilder.PotionShape.values()[random.nextInt(ItemLib.PotionBuilder.PotionShape.values().length)])
						.setExtended(random.nextBoolean())
						.setUpgraded(random.nextBoolean())
						.build(1));
			}
			SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(player);
			ParticleLib.SPELL_WITCH.spawnParticle(player.getLocation(), 2, 2, 2, 10);
			player.openInventory(brewingGUI);
			cooldown.start();

			return true;
		}

		return false;
	}
	
}
