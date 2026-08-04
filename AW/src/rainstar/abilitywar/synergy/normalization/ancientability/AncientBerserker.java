package rainstar.abilitywar.synergy.normalization.ancientability;

import static rainstar.abilitywar.system.config.RainStarAbilitySetting.rsAbilitySettings;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityBase;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.ability.decorator.ActiveHandler;
import daybreak.abilitywar.config.ability.AbilitySettings.SettingObject;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.utils.base.Formatter;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.minecraft.nms.IWorldBorder;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import kotlin.ranges.RangesKt;

@AbilityManifest(name = "고대의 버서커", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
        "철괴를 우클릭한 후 5초 안에 하는 다음 근접 공격이 §c강화§f됩니다. $[COOLDOWN]",
        "§c강화된 근접 공격§f은 $[MULTIPLY]배의 피해를 입히고, §c강화된 공격§f을 사용한 후",
        "$[NO_DAMAGE]초간 근접 공격을 할 수 없습니다."
		})
public class AncientBerserker extends AbilityBase implements ActiveHandler {

	public AncientBerserker(Participant participant) {
		super(participant);
	}
	
	public static final SettingObject<Integer> COOLDOWN = rsAbilitySettings.new SettingObject<Integer>(AncientBerserker.class, 
			"cooldown", 80, "# 쿨타임") {

		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}

		@Override
		public String toString() {
			return Formatter.formatCooldown(getValue());
		}

	};
	
	public static final SettingObject<Double> MULTIPLY = rsAbilitySettings.new SettingObject<Double>(AncientBerserker.class, 
			"damage-multiply", 4.0, "# 피해 증가량") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	public static final SettingObject<Double> NO_DAMAGE = rsAbilitySettings.new SettingObject<Double>(AncientBerserker.class, 
			"no-damage", 5.0, "# 근접 공격 불가", "# 단위: 초") {

		@Override
		public boolean condition(Double value) {
			return value >= 0;
		}

	};
	
	private final Cooldown cooldown = new Cooldown(COOLDOWN.getValue());
	private final double multiply = MULTIPLY.getValue();
	private final int noDamage = (int) (NO_DAMAGE.getValue() * 20);
	private Skill skill = null;
	private NoAttack noattack = null;
	
    @Override
    public boolean ActiveSkill(Material material, ClickType clickType) {
        if (material == Material.IRON_INGOT && clickType == ClickType.RIGHT_CLICK) {
            if (skill == null && !cooldown.isCooldown()) {
            	if (noattack != null) noattack.stop(false);
                new Skill(5).start();
            }
        }
        return false;
    }
	
    public class Skill extends AbilityTimer implements Listener {

        private final BossBar bossBar;
        private final IWorldBorder worldBorder;

        private Skill(int duration) {
            super(TaskType.REVERSE, duration * 20);
            setPeriod(TimeUnit.TICKS, 1);
            this.bossBar = Bukkit.createBossBar("§c근접 공격 강화", BarColor.RED, BarStyle.SEGMENTED_6);
            this.worldBorder = NMS.createWorldBorder(getPlayer().getWorld().getWorldBorder());
            worldBorder.setWarningDistance(Integer.MAX_VALUE);
            NMS.setWorldBorder(getPlayer(), worldBorder);
            AncientBerserker.this.skill = this;
        }

        @Override
        protected void onStart() {
            bossBar.setProgress(1);
            bossBar.addPlayer(getPlayer());
            bossBar.setVisible(true);
            Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        }

        @Override
        protected void run(int count) {
            bossBar.setProgress(RangesKt.coerceIn(count / (double) getMaximumCount(), 0, 1));
        }

        @EventHandler(ignoreCancelled = true)
        private void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
            if (getPlayer().equals(e.getDamager())) {
                e.setDamage(e.getDamage() * multiply);
                final Entity entity = e.getEntity();
                entity.getWorld().strikeLightningEffect(entity.getLocation());
                new NoAttack(noDamage).start();
                this.stop(false);
            }
        }

        @EventHandler
        private void onPlayerJoin(final PlayerJoinEvent e) {
            if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId()) && isRunning()) {
                bossBar.addPlayer(e.getPlayer());
            }
        }

        @EventHandler
        private void onPlayerQuit(final PlayerQuitEvent e) {
            if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
                bossBar.removePlayer(e.getPlayer());
            }
        }

        @Override
        protected void onEnd() {
            bossBar.removeAll();
            cooldown.start();
            onSilentEnd();
        }

        @Override
        protected void onSilentEnd() {
            NMS.resetWorldBorder(getPlayer());
            HandlerList.unregisterAll(this);
            AncientBerserker.this.skill = null;
            bossBar.removeAll();
        }

        @Override
        protected void onPause() {
            bossBar.removeAll();
        }

        @Override
        protected void onResume() {
            bossBar.addPlayer(getPlayer());
        }

    }
    
    public class NoAttack extends AbilityTimer implements Listener {

        private final BossBar bossBar;
        private final IWorldBorder worldBorder;

        private NoAttack(int duration) {
            super(TaskType.REVERSE, duration);
            setPeriod(TimeUnit.TICKS, 1);
            this.bossBar = Bukkit.createBossBar("§7공격 불가", BarColor.WHITE, BarStyle.SEGMENTED_6);
            this.worldBorder = NMS.createWorldBorder(getPlayer().getWorld().getWorldBorder());
            worldBorder.setWarningDistance(Integer.MAX_VALUE);
            NMS.setWorldBorder(getPlayer(), worldBorder);
            AncientBerserker.this.noattack = this;
        }

        @Override
        protected void onStart() {
            bossBar.setProgress(1);
            bossBar.addPlayer(getPlayer());
            bossBar.setVisible(true);
            Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
        }

        @Override
        protected void run(int count) {
            bossBar.setProgress(RangesKt.coerceIn(count / (double) getMaximumCount(), 0, 1));
        }

        @EventHandler
        private void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
            if (getPlayer().equals(e.getDamager())) {
                e.setCancelled(true);
            }
        }

        @EventHandler
        private void onPlayerJoin(final PlayerJoinEvent e) {
            if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId()) && isRunning()) {
                bossBar.addPlayer(e.getPlayer());
            }
        }

        @EventHandler
        private void onPlayerQuit(final PlayerQuitEvent e) {
            if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
                bossBar.removePlayer(e.getPlayer());
            }
        }

        @Override
        protected void onEnd() {
            bossBar.removeAll();
            cooldown.start();
            onSilentEnd();
        }

        @Override
        protected void onSilentEnd() {
            NMS.resetWorldBorder(getPlayer());
            HandlerList.unregisterAll(this);
            AncientBerserker.this.noattack = null;
            bossBar.removeAll();
        }

        @Override
        protected void onPause() {
            bossBar.removeAll();
        }

        @Override
        protected void onResume() {
            bossBar.addPlayer(getPlayer());
        }

    }
	
	
}
