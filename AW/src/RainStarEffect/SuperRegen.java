package RainStarEffect;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.game.AbstractGame;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.manager.effect.registry.ApplicationMethod;
import daybreak.abilitywar.game.manager.effect.registry.EffectManifest;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry;
import daybreak.abilitywar.game.manager.effect.registry.EffectRegistry.EffectRegistration;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.entity.health.Healths;
import daybreak.abilitywar.utils.library.ParticleLib;

@EffectManifest(name = "��ȸ��", displayName = "��d��ȸ��", method = ApplicationMethod.MULTIPLE, type = {
}, description = {
		"�ٸ� ��� ȸ�� ȿ���� �����ϴ� ���",
		"�� �ʸ��� ü���� 0.75 ȸ���մϴ�.",
		"ȸ�� ó���� ���� ���� ��� �������� ���մϴ�. ��7ex) ��Ȥ"
})
public class SuperRegen extends AbstractGame.Effect implements Listener {

	public static final EffectRegistration<SuperRegen> registration = EffectRegistry.registerEffect(SuperRegen.class);

	public static void apply(Participant participant, TimeUnit timeUnit, int duration) {
		registration.apply(participant, timeUnit, duration);
	}

	private final Participant participant;

	public SuperRegen(Participant participant, TimeUnit timeUnit, int duration) {
		participant.getGame().super(registration, participant, (timeUnit.toTicks(duration) / 20));
		this.participant = participant;
		setPeriod(TimeUnit.SECONDS, 1);
	}

	@Override
	protected void onStart() {
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		super.onStart();
	}
	
	@EventHandler
	private void onEntityRegainHealth(final EntityRegainHealthEvent e) {
		if (e.getEntity().getUniqueId().equals(participant.getPlayer().getUniqueId())) {
			e.setCancelled(true);
		}
	}
	
	@Override
	protected void run(int count) {
		ParticleLib.VILLAGER_HAPPY.spawnParticle(participant.getPlayer().getLocation().add(0, 1, 0), 0, 0, 0, 10, 1);
		Healths.setHealth(participant.getPlayer(), participant.getPlayer().getHealth() + 0.75);
		super.run(count);
	}

	@Override
	protected void onEnd() {
		HandlerList.unregisterAll(this);
		super.onEnd();
	}

	@Override
	protected void onSilentEnd() {
		HandlerList.unregisterAll(this);
		super.onSilentEnd();
	}
	
}