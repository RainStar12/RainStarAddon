package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.event.ParticipantPreEffectApplyEvent;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectType;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.abilitywar.utils.library.item.EnchantLib;

@EffectManifest(name = "저주", displayName = "§5저주", method = ApplicationMethod.UNIQUE_STACK, type = {
		EffectType.COMBAT_RESTRICTION
}, description = {
		"상태이상이 지속되는 동안 갑옷에 귀속 저주가 부여됩니다.",
		"저주를 제외한 받게 될 모든 상태이상 지속시간이 2배 증가합니다.",
		"이 효과는 중복으로 받으면 지속 시간이 계속해서 쌓이게 됩니다."
})
public class Cursed extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<Cursed> registration = EffectRegistry.registerEffect(Cursed.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;

	public Cursed(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 20));
		this.participant = participant;
		setPeriod(TimeUnit.SECONDS, 1);
	}
	
	@EventHandler
	private void onEffectApply(ParticipantPreEffectApplyEvent e) {
		if (e.getPlayer().equals(participant.getPlayer())) {
			if (!e.getEffectType().equals(Cursed.registration)) {
				e.setDuration(TimeUnit.TICKS, e.getDuration() * 2);
			}
		}
	}
	
	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		SoundLib.ENTITY_ELDER_GUARDIAN_CURSE.playSound(participant.getPlayer(), 0.75f, 1);
		super.onStart();
	}
	
	@Override
	protected void run(int count) {
		ParticleLib.SPELL_WITCH.spawnParticle(participant.getPlayer().getLocation(), 0.75, 0.75, 0.75, 25, 1);
		Player p = participant.getPlayer();
		ItemStack Helmet = p.getInventory().getHelmet();
		if (Helmet != null) {
			p.getInventory().setHelmet(EnchantLib.BINDING_CURSE.addEnchantment(Helmet, 1));
		}

		ItemStack Chestplate = p.getInventory().getChestplate();
		if (Chestplate != null) {
			p.getInventory().setChestplate(EnchantLib.BINDING_CURSE.addEnchantment(Chestplate, 1));
		}

		ItemStack Leggings = p.getInventory().getLeggings();
		if (Leggings != null) {
			p.getInventory().setLeggings(EnchantLib.BINDING_CURSE.addEnchantment(Leggings, 1));
		}

		ItemStack Boots = p.getInventory().getBoots();
		if (Boots != null) {
			p.getInventory().setBoots(EnchantLib.BINDING_CURSE.addEnchantment(Boots, 1));
		}
		super.run(count);
	}

	@Override
	protected void onEnd() {
		onSilentEnd();
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		Player p = participant.getPlayer();
		ItemStack Helmet = p.getInventory().getHelmet();
		if (Helmet != null) {
			p.getInventory().setHelmet(EnchantLib.BINDING_CURSE.removeEnchantment(Helmet));
		}

		ItemStack Chestplate = p.getInventory().getChestplate();
		if (Chestplate != null) {
			p.getInventory().setChestplate(EnchantLib.BINDING_CURSE.removeEnchantment(Chestplate));
		}

		ItemStack Leggings = p.getInventory().getLeggings();
		if (Leggings != null) {
			p.getInventory().setLeggings(EnchantLib.BINDING_CURSE.removeEnchantment(Leggings));
		}

		ItemStack Boots = p.getInventory().getBoots();
		if (Boots != null) {
			p.getInventory().setBoots(EnchantLib.BINDING_CURSE.removeEnchantment(Boots));
		}
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
}