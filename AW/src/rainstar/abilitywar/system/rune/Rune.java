package rainstar.abilitywar.system.rune;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import daybreak.abilitywar.ability.AbilityBase.AbilityTimer;
import daybreak.abilitywar.game.AbstractGame.GameTimer;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.library.MaterialX;

public class Rune implements Listener {
	
	public Rune() {
	}
	
	private static MaterialX material = MaterialX.PRISMARINE_SHARD;
	private static MaterialX rainstar = MaterialX.EMERALD;
	private static MaterialX cokes = MaterialX.REDSTONE;
	private static MaterialX daybreak = MaterialX.DIAMOND;
	private static Set<ItemStack> runes = new HashSet<>();
	
	@SuppressWarnings("serial")
	enum Runes {
		HAGALAZ("하갈라즈", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §5하갈라즈")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§b§o파괴");
						add("§7");
						add("§5[§d즉발 효과§5]");
						add("§75칸 내의 플레이어의 방어구가 하나 파괴됩니다.");
						add("§7파괴된 방어구는 4초 후 되돌아옵니다.");
						add("§7");
						add("§c§l미완성 현재 무효과");
						add("§c§l미완성 현재 무효과");
						add("§c§l미완성 현재 무효과");
					}
				})
				.build()),
		JERA("제라", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §e제라")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§b§o풍요");
						add("§7");
						add("§3[§b영구 효과§3]");
						add("§7가진 모든 소모성 아이템이 2배가 됩니다.");
						add("§7이 효과로 제라는 복제할 수 없습니다.");
					}
				})
				.build()),
		EHWAZ("에와즈", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §7에와즈")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§b§o길");
						add("§7");
						add("§5[§d즉발 효과§5]");
						add("§7가장 가까운 적에게 순간 이동합니다.");
						add("§7이동 후, 10초간 신속 2를 획득합니다.");
					}
				})
				.build()),
		DAGAZ("다가즈", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §b다가즈")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§b§o순수");
						add("§7");
						add("§5[§d즉발 효과§5]");
						add("§7모든 상태이상, 포션 효과를 해제합니다.");
						add("§7해제된 효과당 1칸의 흡수 체력을 획득합니다.");
					}
				})
				.build()),
		ANSUZ("안수즈", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §a안수즈")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§b§o시야");
						add("§7");
						add("§5[§d즉발 효과§5]");
						add("§7모든 플레이어를 10초간 발광시킵니다.");
						add("§7");
						add("§3[§b영구 효과§3]");
						add("§7모든 플레이어의 체력을 볼 수 있습니다.");
					}
				})
				.build()),
		PERTHRO("페트로", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §d페트로")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§b§o변화");
						add("§7");
						add("§3[§b영구 효과§3]");
						add("§7자신의 능력을 재추첨합니다.");
					}
				})
				.build()),
		BERKANO("베르카노", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §2베르카노")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§b§o우정");
						add("§7");
						add("§5[§d즉발 효과§5]");
						add("§71분간 받는 피해량이 30% 감소합니다.");
						add("§71분간 주는 피해량이 10% 감소합니다.");
					}
				})
				.build()),
		ALGIZ("알기즈", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §f알기즈")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§b§o저항");
						add("§7");
						add("§5[§d즉발 효과§5]");
						add("§75초간 무적 상태가 됩니다.");
					}
				})
				.build()),
		BLACK_RUNE("블랙 룬", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §0블랙 룬")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§b§o공허");
						add("§7");
						add("§5[§d즉발 효과§5]");
						add("§710초간 모든 능력이 비활성화되지만,");
						add("§7공격력이 20% 증가합니다.");
					}
				})
				.build()),
		BLANK_RUNE("빈 룬", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §f빈 룬")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§b§o운명");
						add("§7");
						add("§5[§d즉발 효과§5]");
						add("§7무작위 룬 효과를 발동합니다.");
					}
				})
				.build()),
		SOUL_OF_RAINSTAR("레인스타의 영혼", new ItemBuilder(rainstar)
				.displayName("§3[§b룬§3] §a레인스타의 영혼")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§a§o유성");
						add("§7");
						add("§3[§b영구 효과§3]");
						add("§7접촉한 적의 체력을 5% 감소시키는");
						add("§7영구적인 별소나기를 하나 소환합니다.");
					}
				})
				.build()),
		SOUL_OF_COKES("코크스의 영혼", new ItemBuilder(cokes)
				.displayName("§3[§b룬§3] §c코크스의 영혼")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§c§o변칙");
						add("§7");
						add("§3[§b영구 효과§3]");
						add("§750%의 확률로 공격력이 1~30% 증가합니다.");
					}
				})
				.build()),
		SOUL_OF_DAYBREAK("새벽의 영혼", new ItemBuilder(daybreak)
				.displayName("§3[§b룬§3] §b새벽의 영혼")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§b§o창조");
						add("§7");
						add("§5[§d즉발 효과§5]");
						add("§71분간 무작위 능력을 획득합니다.");
					}
				})
				.build()),
		POWER_RUNE("힘의 룬", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §c힘의 룬")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§3[§b영구 효과§3]");
						add("§7공격력이 5% 증가합니다.");
					}
				})
				.build()),
		HEAL_RUNE("회복의 룬", new ItemBuilder(material)
				.displayName("§3[§b룬§3] §d회복의 룬")
				.lore(new ArrayList<String>() {
					{
						add("§7");
						add("§3[§b영구 효과§3]");
						add("§7매 초마다 체력을 0.1 회복합니다.");
					}
				})
				.build());
		
		Runes(String name, ItemStack item) {
			item.addEnchantment(Enchantment.MENDING, 1);
			runes.add(item);
		}
	
	}
	
	@EventHandler()
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (runes.contains(e.getItem())) {
			Player player = e.getPlayer();
			switch(e.getItem().getItemMeta().getDisplayName()) {
			case "§3[§b룬§3] §5하갈라즈":
				break;
			case "§3[§b룬§3] §e제라":
				break;
			case "§3[§b룬§3] §7에와즈":
				player.teleport(LocationUtil.getNearestEntity(Player.class, player.getLocation(), null));
				break;
			}
		}
	}
	
}
