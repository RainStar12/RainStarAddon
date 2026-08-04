package rainstar.abilitywar.ability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.random.Random;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;

@AbilityManifest(name = "엔드리스", rank = Rank.L, species = Species.HUMAN, explain = {
		"§e지급 아이템§7: §f불사의 토템 $[TOTEM]개, 황금 사과 $[GOLDEN_APPLE]개,",
		" 엔더 진주 $[ENDER_PEARL]개, 투척용 힘/저항의 포션 $[POTION]개",
		"§a소모품§f을 소모할 때, $[CHANCE]% 확률로 §a소모품§f이 돌아옵니다."
		})
public class Endless extends AbilityBase {
	
	public Endless(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> TOTEM = rsAbilitySettings.new SettingObject<Integer>(Endless.class,
			"totem-amount", 1, "# 토템 지급 개수") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> GOLDEN_APPLE = rsAbilitySettings.new SettingObject<Integer>(Endless.class,
			"golden-apple-amount", 2, "# 황금 사과 지급 개수") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> ENDER_PEARL = rsAbilitySettings.new SettingObject<Integer>(Endless.class,
			"ender-pearl-amount", 4, "# 엔더 진주 지급 개수") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> POTION = rsAbilitySettings.new SettingObject<Integer>(Endless.class,
			"potion-amount", 2, "# 포션 지급 개수") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final SettingObject<Integer> CHANCE = rsAbilitySettings.new SettingObject<Integer>(Endless.class,
			"chance", 50, "# 소모품이 돌아올 확률", "# 단위: %") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	private final int chance = CHANCE.getValue();
	
	private final Random random = new Random();
	private boolean kit = true;
	
	@Override
	protected void onUpdate(Update update) {
		if (update == Update.RESTRICTION_CLEAR && kit) {		
			ItemStack totem = new ItemStack(MaterialX.TOTEM_OF_UNDYING.getMaterial(), TOTEM.getValue());
			ItemStack goldenApple = new ItemStack(Material.GOLDEN_APPLE, GOLDEN_APPLE.getValue());
			ItemStack enderPearl = new ItemStack(Material.ENDER_PEARL, ENDER_PEARL.getValue());
			ItemStack splashPotion = new ItemStack(Material.SPLASH_POTION, POTION.getValue());
			ItemMeta tmeta = totem.getItemMeta();
			ItemMeta gmeta = goldenApple.getItemMeta();
			ItemMeta emeta = enderPearl.getItemMeta();
			PotionMeta pmeta = (PotionMeta) splashPotion.getItemMeta();
			
			tmeta.setDisplayName("§3무한한 §e불사의 토템§f");
			gmeta.setDisplayName("§3무한한 §b황금 사과§f");
			emeta.setDisplayName("§3무한한 §f엔더 진주§f");
			pmeta.setDisplayName("§3무한한 §f투척용 힘 및 저항의 포션§f");
			
			pmeta.addCustomEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 800, 0), true);
			pmeta.addCustomEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 800, 0), true);
			pmeta.setColor(RGB.of(138, 78, 76).getColor());
			
			totem.setItemMeta(tmeta);
			goldenApple.setItemMeta(gmeta);
			enderPearl.setItemMeta(emeta);
			splashPotion.setItemMeta(pmeta);
			
			totem.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
			goldenApple.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
			enderPearl.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
			splashPotion.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
			
			getPlayer().getInventory().addItem(totem);
			getPlayer().getInventory().addItem(goldenApple);
			getPlayer().getInventory().addItem(enderPearl);
			getPlayer().getInventory().addItem(splashPotion);
			kit = false;
		}
	}

	@SubscribeEvent(onlyRelevant = true)
	private void onPlayerInteract(final PlayerInteractEvent e) {
		if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR)) {
			if (random.nextInt(100) < chance && !e.useItemInHand().equals(Result.DENY)) {
				boolean potion = false;
				if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.SPLASH_POTION)) potion = true;
				else if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ENDER_PEARL)) potion = false;
				else if (getPlayer().getInventory().getItemInOffHand().getType().equals(Material.SPLASH_POTION)) potion = true;
				else if (getPlayer().getInventory().getItemInOffHand().getType().equals(Material.ENDER_PEARL)) potion = false;
				
				if (potion && (getPlayer().getInventory().getItemInOffHand().getType().equals(Material.SPLASH_POTION) || getPlayer().getInventory().getItemInMainHand().getType().equals(Material.SPLASH_POTION))) {
					ItemStack splashPotion = null;
					if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.SPLASH_POTION)) splashPotion = getPlayer().getInventory().getItemInMainHand().clone();
					else splashPotion = getPlayer().getInventory().getItemInOffHand().clone();
					splashPotion.setAmount(1);
					getPlayer().getInventory().addItem(splashPotion);	
				}
				
				if (!potion && (getPlayer().getInventory().getItemInOffHand().getType().equals(Material.ENDER_PEARL) || getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ENDER_PEARL))) {
					ItemStack enderPearl = null;
					if (getPlayer().getInventory().getItemInMainHand().getType().equals(Material.ENDER_PEARL)) enderPearl = getPlayer().getInventory().getItemInMainHand().clone();
					else enderPearl = getPlayer().getInventory().getItemInOffHand().clone();
					enderPearl.setAmount(1);
					getPlayer().getInventory().addItem(enderPearl);	
				}
				
				SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(getPlayer().getLocation(), 1, 1);
				ParticleLib.PORTAL.spawnParticle(getPlayer().getEyeLocation(), 0, 0, 0, 100, 1);
			}
		}
	}
	
	@SubscribeEvent
	private void onEntityResurrect(EntityResurrectEvent e) {
		if (e.getEntity().equals(getPlayer()) && (getPlayer().getInventory().getItemInOffHand().getType().equals(MaterialX.TOTEM_OF_UNDYING.getMaterial()) || getPlayer().getInventory().getItemInMainHand().getType().equals(MaterialX.TOTEM_OF_UNDYING.getMaterial())) && !e.isCancelled() && random.nextInt(100) < chance) {
			ItemStack totem = null;
			if (getPlayer().getInventory().getItemInMainHand().getType().equals(MaterialX.TOTEM_OF_UNDYING.getMaterial())) totem = getPlayer().getInventory().getItemInMainHand().clone();
			else totem = getPlayer().getInventory().getItemInOffHand().clone();
			totem.setAmount(1);
			getPlayer().getInventory().addItem(totem);
			
			SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(getPlayer(), 1, 1);
			ParticleLib.PORTAL.spawnParticle(getPlayer().getEyeLocation(), 0, 0, 0, 100, 1);
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
		if (!e.isCancelled() && random.nextInt(100) < chance) {
			new AbilityTimer(1) {
				
				@Override
				public void onEnd() {
					ItemStack consume = e.getItem().clone();
					consume.setAmount(1);
					getPlayer().getInventory().addItem(consume);
				
					SoundLib.ENTITY_ENDER_EYE_DEATH.playSound(getPlayer().getLocation(), 1, 1);
					ParticleLib.PORTAL.spawnParticle(getPlayer().getEyeLocation(), 0, 0, 0, 100, 1);
				}
				
			}.setPeriod(TimeUnit.TICKS, 1).start();
		}
	}
	
}
