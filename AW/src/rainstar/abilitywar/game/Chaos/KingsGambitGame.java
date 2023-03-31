package rainstar.abilitywar.game.Chaos;

import com.google.common.base.Strings;
import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.config.Configuration.Settings;
import daybreak.abilitywar.config.Configuration.Settings.DeathSettings;
import daybreak.abilitywar.config.game.GameSettings.Setting;
import daybreak.abilitywar.game.AbstractGame.Observer;
import daybreak.abilitywar.game.Game;
import daybreak.abilitywar.game.GameAliases;
import daybreak.abilitywar.game.GameManifest;
import daybreak.abilitywar.game.event.GameCreditEvent;
import daybreak.abilitywar.game.manager.AbilityList;
import daybreak.abilitywar.game.manager.object.DefaultKitHandler;
import daybreak.abilitywar.game.module.DeathManager;
import daybreak.abilitywar.game.module.InfiniteDurability;
import daybreak.abilitywar.game.module.Invincibility;
import daybreak.abilitywar.game.script.manager.ScriptManager;
import daybreak.abilitywar.utils.base.Messager;
import daybreak.abilitywar.utils.base.Seasons;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.VectorUtil;
import daybreak.abilitywar.utils.base.minecraft.PlayerCollector;
import daybreak.abilitywar.utils.base.minecraft.nms.IWorldBorder;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.library.SoundLib;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.naming.OperationNotSupportedException;
import java.util.List;
import java.util.Random;

@GameManifest(name = "카오스 - 킹즈 갬빗", description = {
		"§5§kaaa§c킹즈 갬빗§5§kaaa",
		"",
		"§f모든 플레이어의 머리 위에, 검이 생성됩니다.",
		"§f검은 매 틱마다 매우 낮은 확률로 떨어질 수 있습니다.",
		"§f검이 떨어진다면 대상은 치명적인 피해를 입습니다.",
		"§f누군가의 검이 떨어질 때마다 공격력이 10%씩 증가합니다."
		})
@GameAliases({"킹즈 갬빗", "킹즈갬빗", "다모클"})
public class KingsGambitGame extends Game implements DefaultKitHandler, Observer {

	public static final Setting<Integer> CHANCE_NUMERATOR = 
			gameSettings.new Setting<Integer>(KingsGambitGame.class, "chance-numerator", 1,
			"# 확률 분자") {
		@Override
		public boolean condition(Integer value) {
			return value >= 0;
		}
	};
	
	public static final Setting<Integer> CHANCE_DENOMINATOR = 
			gameSettings.new Setting<Integer>(KingsGambitGame.class, "chance-denominator", 10000,
			"# 확률 분모") {
		@Override
		public boolean condition(Integer value) {
			return value >= 1;
		}
	};
	
	private final int nume = CHANCE_NUMERATOR.getValue(), deno = CHANCE_DENOMINATOR.getValue();
	
	public KingsGambitGame() {
		super(PlayerCollector.EVERY_PLAYER_EXCLUDING_SPECTATORS());
		setRestricted(Settings.InvincibilitySettings.isEnabled());
		attachObserver(this);
		Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
	}

	@Override
	protected void progressGame(int seconds) {
		switch (seconds) {
			case 1:
				List<String> lines = Messager.asList("§4==== §8게임 참여자 목록 §4====");
				int count = 0;
				for (Participant p : getParticipants()) {
					count++;
					lines.add("§c" + count + ". §f" + p.getPlayer().getName());
				}
				lines.add("§6총 인원수 : " + count + "명");
				lines.add("§4===========================");

				for (String line : lines) {
					Bukkit.broadcastMessage(line);
				}

				if (getParticipants().size() < 2) {
					stop();
					Bukkit.broadcastMessage("§c최소 참가자 수를 충족하지 못하여 게임을 중지합니다. §8(§72명§8)");
				}
				break;
			case 3:
				lines = Messager.asList(
						"§5ChaosWar §f- §d혼돈의 전쟁",
						"§e버전 §7: §f4.0.1",
						"§b개발자 §7: §aRainStar_ 레인스타",
						"§9디스코드 §7: §a레인스타§7#0846"
				);

				GameCreditEvent event = new GameCreditEvent(this);
				Bukkit.getPluginManager().callEvent(event);
				lines.addAll(event.getCredits());

				for (String line : lines) {
					Bukkit.broadcastMessage(line);
				}
				break;
			case 5:
				if (Settings.getDrawAbility()) {
					for (String line : Messager.asList(
							"§f플러그인에 총 §b" + AbilityList.nameValues().size() + "개§f의 능력이 등록되어 있습니다.",
							"§7능력을 무작위로 할당합니다...")) {
						Bukkit.broadcastMessage(line);
					}
					try {
						startAbilitySelect();
					} catch (OperationNotSupportedException ignored) {
					}
				}
				break;
			case 6:
				if (Settings.getDrawAbility()) {
					Bukkit.broadcastMessage("§f모든 참가자가 능력을 §b확정§f했습니다.");
				} else {
					Bukkit.broadcastMessage("§f능력자 게임 설정에 따라 §b능력§f을 추첨하지 않습니다.");
				}
				break;
			case 8:
				Bukkit.broadcastMessage("§c검이 준비되었습니다.");
				Bukkit.broadcastMessage("§e잠시 후 게임이 시작됩니다.");
				break;
			case 10:
				Bukkit.broadcastMessage("§e게임이 §c5§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 11:
				Bukkit.broadcastMessage("§e게임이 §c4§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 12:
				Bukkit.broadcastMessage("§e게임이 §c3§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 13:
				Bukkit.broadcastMessage("§e게임이 §c2§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 14:
				Bukkit.broadcastMessage("§e게임이 §c1§e초 후에 시작됩니다.");
				SoundLib.BLOCK_NOTE_BLOCK_HARP.broadcastSound();
				break;
			case 15:
				if (Seasons.isChristmas()) {
					final String blocks = Strings.repeat("§c■§2■", 22);
					Bukkit.broadcastMessage(blocks);
					Bukkit.broadcastMessage("§f            §4KingsGambit §f- §8킹즈 갬빗  ");
					Bukkit.broadcastMessage("§f                   게임 시작                ");
					Bukkit.broadcastMessage(blocks);
				} else {
					for (String line : Messager.asList(
							"§7■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■",
							"§f             §4KingsGambit §f- §8킹즈 갬빗  ",
							"§f                    게임 시작                ",
							"§7■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■")) {
						Bukkit.broadcastMessage(line);
					}
				}

				giveDefaultKit(getParticipants());

				if (Settings.getSpawnEnable()) {
					Location spawn = Settings.getSpawnLocation().toBukkitLocation();
					for (Participant participant : getParticipants()) {
						participant.getPlayer().teleport(spawn);
					}
				}

				if (Settings.getNoHunger()) {
					Bukkit.broadcastMessage("§2배고픔 무제한§a이 적용됩니다.");
				} else {
					Bukkit.broadcastMessage("§4배고픔 무제한§c이 적용되지 않습니다.");
				}

				if (Settings.getInfiniteDurability()) {
					addModule(new InfiniteDurability());
				} else {
					Bukkit.broadcastMessage("§4내구도 무제한§c이 적용되지 않습니다.");
				}

				if (Settings.getClearWeather()) {
					for (World w : Bukkit.getWorlds()) {
						w.setStorm(false);
					}
				}

				getInvincibility().attachObserver(new Invincibility.Observer() {
					@Override
					public void onStart() {
					}

					@Override
					public void onEnd() {
						for (Participant participant : getParticipants()) {
							new KingsDamocles(participant).start();
						}
					}
				});
				
				if (isRestricted()) {
					getInvincibility().start(false);
				} else {
					Bukkit.broadcastMessage("§4초반 무적§c이 적용되지 않습니다.");
					setRestricted(false);
					for (Participant participant : getParticipants()) {
						new KingsDamocles(participant).start();
					}
				}

				ScriptManager.runAll(this);
				
				startGame();
				break;
		}
	}

	@Override
	protected @Nonnull DeathManager newDeathManager() {
		return new DeathManager(this) {
			public void Operation(Participant victim) {
				switch (DeathSettings.getOperation()) {
					case 탈락:
						eliminate(victim);
						excludedPlayers.add(victim.getPlayer().getUniqueId());
						break;
					case 관전모드:
					case 없음:
						victim.getPlayer().setGameMode(GameMode.SPECTATOR);
						excludedPlayers.add(victim.getPlayer().getUniqueId());
						break;
				}
			}
		};
	}

	@Override
	public void update(GameUpdate update) {
		if (update == GameUpdate.END) {
			HandlerList.unregisterAll(this);
		}
	}
	
	public class KingsDamocles extends GameTimer implements Listener {
		
		private final Random random = new Random();
		private final Participant participant;
		private IWorldBorder worldBorder;
	    private int time = 0;
	    private boolean falling = false;
	    private int fallingtime = 60;
		private ArmorStand armorstand = null;
		
		private KingsDamocles(Participant participant) {
			super(TaskType.INFINITE, -1);
			setPeriod(TimeUnit.TICKS, 1);
			this.participant = participant;
			if (armorstand == null) armorstand = participant.getPlayer().getLocation().getWorld().spawn(participant.getPlayer().getLocation().clone().add(0, 3, 0), ArmorStand.class);
			armorstand.setRightArmPose(new EulerAngle(Math.toRadians(80), 0, 0));
			armorstand.setMetadata("Damocles", new FixedMetadataValue(AbilityWar.getPlugin(), null));
			armorstand.setVisible(false);
			armorstand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
			armorstand.setGravity(false);
			armorstand.setInvulnerable(true);
			NMS.removeBoundingBox(armorstand);
		}
		
		@Override
		public void onStart() {
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
		}
		
		@Override
		public void run(int count) {
			if (falling) {
				if (fallingtime == 60) {
					participant.getPlayer().sendMessage("§c§o불길한 예감이 듭니다.");
					SoundLib.AMBIENT_CAVE.playSound(participant.getPlayer(), 1, 0.5f);
					worldBorder = NMS.createWorldBorder(participant.getPlayer().getWorld().getWorldBorder());
					worldBorder.setWarningDistance(Integer.MAX_VALUE);
					NMS.setWorldBorder(participant.getPlayer(), worldBorder);
				}
				if (fallingtime == 5) {
					SoundLib.ENTITY_ITEM_BREAK.playSound(armorstand.getLocation(), 1, 2f);
					armorstand.setGravity(true);
					armorstand.setVelocity(new Vector(0, -2, 0));
				}
				if (fallingtime <= 0) {
					armorstand.setGravity(true);
					armorstand.setVelocity(new Vector(0, -6, 0));
					new BukkitRunnable() {		
						@Override
						public void run() {
							armorstand.setGravity(false);
							armorstand.teleport(armorstand.getLocation().clone().add(0, -0.6, 0));
						}
					}.runTaskLater(AbilityWar.getPlugin(), 3L);
					SoundLib.BLOCK_ANVIL_LAND.playSound(armorstand.getLocation(), 1, 1.4f);
					participant.getPlayer().damage(Integer.MAX_VALUE);
					participant.getPlayer().getWorld().spawn(participant.getPlayer().getLocation().clone().add(1000, 0, 1000), Zombie.class).damage(Integer.MAX_VALUE, participant.getPlayer());
		            if (participant.getPlayer().isDead()) Bukkit.broadcastMessage("§3[§b다모클레스§3] §a" + participant.getPlayer().getName() + "§f님이 §b" + nume + "§7/§b" + deno + "§f의 확률로 §e" + (time / 20.0) + "§f초를 버티고 사망하셨습니다.");
					NMS.resetWorldBorder(participant.getPlayer());
					this.stop(false);
				}
				fallingtime--;
			} else {
				int randomvalue = random.nextInt(deno);
				if (randomvalue < nume) falling = true;
				time++;
			}
		}
		
		@Override
		protected void onEnd() {
			HandlerList.unregisterAll(this);
		}

		@Override
		protected void onSilentEnd() {
			HandlerList.unregisterAll(this);
		}
		
		@EventHandler
		private void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
			if (e.getRightClicked().hasMetadata("Damocles")) e.setCancelled(true);
		}
		
		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent e) {
			if (e.getPlayer().equals(participant.getPlayer()) && falling) {
				armorstand.setGravity(true);
				armorstand.setVelocity(new Vector(0, -6, 0));
				new BukkitRunnable() {		
					@Override
					public void run() {
						armorstand.setGravity(false);
						armorstand.teleport(armorstand.getLocation().clone().add(0, -0.6, 0));
					}
				}.runTaskLater(AbilityWar.getPlugin(), 3L);
				SoundLib.BLOCK_ANVIL_LAND.playSound(armorstand.getLocation(), 1, 1.4f);
				participant.getPlayer().setHealth(0);
				Bukkit.broadcastMessage("§c잔머리를 꾀한 자, 죽음을 §l절대§c 피하지 못하리라.");
	            Bukkit.broadcastMessage("§3[§b다모클레스§3] §a" + participant.getPlayer().getName() + "§f님이 §b" + nume + "§7/§b" + deno + "§f의 확률로 §e" + (time / 20.0) + "§f초를 버티고 사망하셨습니다.");
			}
		}
		
		@EventHandler
		public void onPlayerMove(PlayerMoveEvent e) {
			if (e.getPlayer().equals(participant.getPlayer())) {
				Location loc = participant.getPlayer().getLocation().clone().add(0, 3, 0);
				loc.add(participant.getPlayer().getLocation().getDirection().clone().setY(0).normalize().multiply(0.7));
				loc.add(VectorUtil.rotateAroundAxisY(participant.getPlayer().getLocation().getDirection().clone().setY(0).normalize().multiply(0.4), 90));
				armorstand.teleport(loc);	
			}
		}
		
	}

}
