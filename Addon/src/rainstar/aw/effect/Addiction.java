package rainstar.aw.effect;

import daybreak.abilitywar.game.AbstractGame.Effect;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableMap;
import org.bukkit.Color;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@EffectManifest(name = "포션 중독", displayName = "§5포션 중독", method = ApplicationMethod.UNIQUE_LONGEST, type = {
}, description = {
		"부정 계열의 포션 효과들의 계수가 1 높아집니다."
})
public class Addiction extends Effect {

	public static final EffectRegistration<Addiction> registration = EffectRegistry.registerEffect(Addiction.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;
	private final RGB violet = RGB.of(107, 5, 169);
	
	private static final ImmutableMap<PotionEffectType, Pair<String, Color>> POTION_TYPES_BAD 
	= ImmutableMap.<PotionEffectType, Pair<String, Color>>builder()
			.put(PotionEffectType.POISON, Pair.of("§2독", PotionEffectType.POISON.getColor()))
			.put(PotionEffectType.WEAKNESS, Pair.of("§7나약함", PotionEffectType.WEAKNESS.getColor()))
			.put(PotionEffectType.SLOW, Pair.of("§8구속", PotionEffectType.SLOW.getColor()))
			.put(PotionEffectType.HARM, Pair.of("§4고통", PotionEffectType.HARM.getColor()))
			// 이 아래는 없는 효과들
			.put(PotionEffectType.WITHER, Pair.of("§0시듦", Color.fromRGB(1, 1, 1)))
			.put(PotionEffectType.BLINDNESS, Pair.of("§7실명", Color.fromRGB(140, 140, 140)))
			.put(PotionEffectType.CONFUSION, Pair.of("§5멀미", Color.fromRGB(171, 130, 18)))
			.put(PotionEffectType.GLOWING, Pair.of("§f발광", Color.fromRGB(254, 254, 254)))
			.put(PotionEffectType.HUNGER, Pair.of("§2허기", Color.fromRGB(134, 229, 127)))
			.put(PotionEffectType.LEVITATION, Pair.of("§5공중 부양", Color.fromRGB(171, 18, 151)))
			.put(PotionEffectType.SLOW_DIGGING, Pair.of("§8채굴 피로", Color.fromRGB(93, 93, 93)))
			.put(PotionEffectType.UNLUCK, Pair.of("§a불운", Color.fromRGB(206, 242, 121)))
			.build();

	public Addiction(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, timeUnit.toTicks(duration));
		this.participant = participant;
		setPeriod(TimeUnit.TICKS, 1);
	}
	
	@Override
	protected void onStart() {
    	for (PotionEffect pe : participant.getPlayer().getActivePotionEffects()) {
    		if (POTION_TYPES_BAD.containsKey(pe.getType())) {
				participant.getPlayer().removePotionEffect(pe.getType());
				PotionEffect newpe = new PotionEffect(pe.getType(), pe.getDuration(), pe.getAmplifier() + 1, false, true);
				participant.getPlayer().addPotionEffect(newpe);
    		}
    	}
		SoundLib.ITEM_BOTTLE_FILL_DRAGONBREATH.playSound(participant.getPlayer().getLocation(), 1, 1);
		super.onStart();
	}
	
	@Override
	protected void run(int count) {
		ParticleLib.SPELL_MOB.spawnParticle(participant.getPlayer().getLocation().clone().add(0, 0.5, 0), violet);
		super.run(count);
	}

	@Override
	protected void onEnd() {
		onSilentEnd();
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
    	for (PotionEffect pe : participant.getPlayer().getActivePotionEffects()) {
    		if (POTION_TYPES_BAD.containsKey(pe.getType())) {
				participant.getPlayer().removePotionEffect(pe.getType());
				PotionEffect newpe = new PotionEffect(pe.getType(), pe.getDuration(), pe.getAmplifier() - 1, false, true);
				participant.getPlayer().addPotionEffect(newpe);
    		}
    	}
		super.onSilentEnd();
	}
	
}