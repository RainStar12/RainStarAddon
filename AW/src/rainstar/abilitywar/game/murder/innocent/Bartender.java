package rainstar.abilitywar.game.murder.innocent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import daybreak.abilitywar.AbilityWar;
import daybreak.abilitywar.ability.AbilityManifest;
import daybreak.abilitywar.ability.SubscribeEvent;
import daybreak.abilitywar.ability.AbilityManifest.Rank;
import daybreak.abilitywar.ability.AbilityManifest.Species;
import daybreak.abilitywar.game.AbstractGame.Participant;
import daybreak.abilitywar.game.AbstractGame.Participant.ActionbarNotification.ActionbarChannel;
import daybreak.abilitywar.game.list.murdermystery.Items;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery;
import daybreak.abilitywar.game.list.murdermystery.MurderMystery.ArrowKillEvent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractInnocent;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractJob;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer;
import daybreak.abilitywar.game.list.murdermystery.ability.AbstractMurderer.MurderEvent;
import daybreak.abilitywar.utils.base.collect.Pair;
import daybreak.abilitywar.utils.base.color.RGB;
import daybreak.abilitywar.utils.base.concurrent.TimeUnit;
import daybreak.abilitywar.utils.base.math.LocationUtil;
import daybreak.abilitywar.utils.base.math.geometry.Circle;
import daybreak.abilitywar.utils.base.minecraft.item.builder.ItemBuilder;
import daybreak.abilitywar.utils.base.minecraft.nms.NMS;
import daybreak.abilitywar.utils.base.minecraft.version.ServerVersion;
import daybreak.abilitywar.utils.library.MaterialX;
import daybreak.abilitywar.utils.library.ParticleLib;
import daybreak.abilitywar.utils.library.PotionEffects;
import daybreak.abilitywar.utils.library.SoundLib;
import daybreak.google.common.collect.ImmutableMap;
import rainstar.abilitywar.game.murder.system.config.statictype.CheckMurdererSword;

@AbilityManifest(name = "시민: 바텐더", rank = Rank.SPECIAL, species = Species.HUMAN, explain = {
		"§8[§a레시피§8]§f 금 우클릭으로 다음에 §d주조§f할 칵테일을 변경할 수 있습니다.",
		"§8[§d주조§8]§f 금 좌클릭으로 만들 칵테일에 따른 금을 소모합니다.",
		"해당 칵테일의 효과를 즉시 적용합니다."
})
public class Bartender extends AbstractInnocent {

	public Bartender(Participant participant) {
		super(participant);
	}
	
	private static final ImmutableMap<String, Pair<Integer, Color>> COCKTAILS 
	= ImmutableMap.<String, Pair<Integer, Color>>builder()
			.put("§7[§f마가리타§7]", Pair.of(9, Color.fromRGB(176, 205, 136)))
			.put("§7[§a미도리 사워§7]", Pair.of(5, Color.fromRGB(89, 199, 1)))
			.put("§7[§b블루 하와이§7]", Pair.of(4, Color.fromRGB(4, 176, 198)))
			.put("§7[§c블러디 메리§7]", Pair.of(30, Color.fromRGB(163, 4, 12)))
			.put("§7[§e스크류드라이버§7]", Pair.of(8, Color.fromRGB(236, 159, 1)))
			.put("§7[§5갤럭시 매직§7]", Pair.of(19, Color.fromRGB(118, 45, 210)))
			.put("§7[§d핑크 레이디§7]", Pair.of(14, Color.fromRGB(218, 113, 143)))
			.build();
	
	private static final ImmutableMap<String, String> COCKTAILS_DESCRIPTION 
	= ImmutableMap.<String, String>builder()
			.put("§7[§f마가리타§7]", "§73초간 무적 및 발광합니다.")
			.put("§7[§a미도리 사워§7]", "§75초간 5칸 내 플레이어와 함께#§b신속 3§7을 받습니다.")
			.put("§7[§b블루 하와이§7]", "§71.5초간 은신합니다.")
			.put("§7[§c블러디 메리§7]", "§75초간 가장 가까운 §5머더§7의#§7방향으로 나침반이 잡힙니다.#머더가 검을 얻고 나서 15초가#지나기 전에는 사용할 수 없습니다.")
			.put("§7[§e스크류드라이버§7]", "§7자신과 모든 §3탐정계 직업§7에게#§7화살을 지급합니다.")
			.put("§7[§5갤럭시 매직§7]", "§77초간 자신이 맞는 화살이#§7공격자만 사망합니다.")
			.put("§7[§d핑크 레이디§7]", "§7바라보는 방향을 15초간 대상으로 지정해#§7대상이 공격을 받으면 2초간 무적을 부여하지만#§7무적 지속 중 최초 공격자를 죽이지 못하면#§7자신이 대신 사망합니다.")
			.build();
	
	private String cocktail = "§7[§f마가리타§7]";
	private static final RGB midori = RGB.of(89, 199, 1);
	private final Circle circle = Circle.of(5, 60);
	private ActionbarChannel ac = newActionbarChannel();
	private Participant target;
	private Participant attacker;
	private BossBar bossBar = null;
	private CheckMurdererSword checkmurderersword = getGame().getModule(CheckMurdererSword.class);
	
	private final Predicate<Entity> predicate = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId()));
		}
	};
	
	private final Predicate<Entity> predicate2 = new Predicate<Entity>() {
		@Override
		public boolean test(Entity entity) {
			if (entity.equals(getPlayer())) return false;
			return !(entity instanceof Player) || (getGame().isParticipating(entity.getUniqueId())
					&& !((MurderMystery) getGame()).isDead(entity.getUniqueId()))
					&& getGame().getParticipant((Player) entity).hasAbility()
					&& getGame().getParticipant((Player) entity).getAbility() instanceof AbstractMurderer;
		}
	};
	
	@Override
	protected void onUpdate(Update update) {
		super.onUpdate(update);
		if (update == Update.RESTRICTION_CLEAR) {
			NMS.sendTitle(getPlayer(), "§e역할§f: §a바텐더", "§f여러가지 술로 변수를 창출해보세요!", 10, 80, 10);
			new AbilityTimer(1) {
				@Override
				protected void run(int count) {
				}

				@Override
				protected void onEnd() {
					NMS.clearTitle(getPlayer());
				}
			}.setInitialDelay(TimeUnit.SECONDS, 5).start();
			ac.update(cocktail);
			passive.start();
		}
	}
	
	@SubscribeEvent(onlyRelevant = true)
	private void onInteract(PlayerInteractEvent e) {
		if (Items.isGold(e.getItem())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				new SkillGUI().start();
			} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (bossBar == null) {
					int cost = COCKTAILS.get(cocktail).getLeft();
					final MurderMystery murderMystery = (MurderMystery) getGame();
					if (cocktail.equals("§7[§d핑크 레이디§7]")) {
						Player player = LocationUtil.getEntityLookingAt(Player.class, getPlayer(), 15, predicate);
						if (player != null) {
							if (murderMystery.consumeGold(getParticipant(), cost)) {
								target = getGame().getParticipant(player);
								getPlayer().sendMessage("§4[§c!§4] §d" + target.getPlayer().getName() + "§f님을 지정하였습니다.");
								pinklady.start();
								SoundLib.BLOCK_BREWING_STAND_BREW.playSound(getPlayer().getLocation(), 1, 0.5f);
							}
						} else getPlayer().sendMessage("§4[§c!§4] §c바라보는 방향에 사람이 없습니다.");	
					} else if (cocktail.equals("§7[§c블러디 메리§7]")) {
						if (checkmurderersword.isCheckable()) {
							 if (murderMystery.consumeGold(getParticipant(), cost)) {
								 bloodymary.start();
							 }
						} else getPlayer().sendMessage("§4[§c!§4] §c이 칵테일은 지금 사용할 수 없습니다.");
					} else if (murderMystery.consumeGold(getParticipant(), cost)) {
						if (cocktail.equals("§7[§f마가리타§7]")) magarita.start();
						else if (cocktail.equals("§7[§a미도리 사워§7]")) midorisour.start();
						else if (cocktail.equals("§7[§b블루 하와이§7]")) bluehawaii.start();
						else if (cocktail.equals("§7[§e스크류드라이버§7]")) screwdriver.start();
						else if (cocktail.equals("§7[§5갤럭시 매직§7]")) galaxymagic.start();
						SoundLib.BLOCK_BREWING_STAND_BREW.playSound(getPlayer().getLocation(), 1, 0.5f);
					}	
				} else getPlayer().sendMessage("§4[§c!§4] §c아직 지속 중인 칵테일이 있습니다.");	
			}
		}
	}
	
	private final AbilityTimer magarita = new AbilityTimer(60) {

		@Override
		protected void onStart() {
			bossBar = Bukkit.createBossBar("§f마가리타", BarColor.WHITE, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    		PotionEffects.GLOWING.addPotionEffect(getPlayer(), 60, 1, false, false, true);
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			bossBar.removeAll();
			bossBar = null;
		}

		@Override
		protected void run(int count) {
			bossBar.setProgress(count / 60.0);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer midorisour = new AbilityTimer(100) {

		@Override
		protected void onStart() {
			bossBar = Bukkit.createBossBar("§a미도리 사워", BarColor.GREEN, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    		PotionEffects.SPEED.addPotionEffect(getPlayer(), 100, 2, false, false, true);
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			bossBar.removeAll();
			bossBar = null;
		}

		@Override
		protected void run(int count) {
			for (Player player : LocationUtil.getEntitiesInCircle(Player.class, getPlayer().getLocation(), 5, predicate)) {
				PotionEffects.SPEED.addPotionEffect(player, 3, 2, true);
			}
			if (count % 2 == 0) {
				for (final Location locs : circle.toLocations(getPlayer().getLocation()).floor(getPlayer().getLocation().getY())) {
					ParticleLib.REDSTONE.spawnParticle(locs, midori);
				}	
			}
			bossBar.setProgress(count / 100.0);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer bluehawaii = new AbilityTimer(30) {

		@Override
		protected void onStart() {
			bossBar = Bukkit.createBossBar("§b블루 하와이", BarColor.BLUE, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
			getParticipant().attributes().TARGETABLE.setValue(false);
			NMS.setInvisible(getPlayer(), true);
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			getParticipant().attributes().TARGETABLE.setValue(true);
			NMS.setInvisible(getPlayer(), false);
			bossBar.removeAll();
			bossBar = null;
		}

		@Override
		protected void run(int count) {
			bossBar.setProgress(count / 30.0);
			NMS.setInvisible(getPlayer(), true);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer bloodymary = new AbilityTimer(100) {

		@Override
		protected void onStart() {
			bossBar = Bukkit.createBossBar("§c블러디 메리", BarColor.RED, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			bossBar.removeAll();
			bossBar = null;
		}

		@Override
		protected void run(int count) {
			bossBar.setProgress(count / 100.0);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer screwdriver = new AbilityTimer(10) {

		@Override
		protected void onStart() {
			bossBar = Bukkit.createBossBar("§e스크류드라이버", BarColor.YELLOW, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
    		for (Participant participant : getGame().getParticipants()) {
    			if (participant.hasAbility()) {
        			if (getParticipant().equals(participant) || participant.getAbility().getClass().getSimpleName().contains("Detective")) {
        				ItemStack stack = participant.getPlayer().getInventory().getItem(3);
        				if (stack != null && stack.getType() == Material.ARROW) {
        					if (stack.getAmount() < 64) {
        						stack.setAmount(stack.getAmount() + 1);
        						participant.getPlayer().getInventory().setItem(3, stack);
        						participant.getPlayer().sendMessage("§8+ §f1 화살");
        					}
        				} else {
        					participant.getPlayer().getInventory().setItem(3, new ItemStack(Material.ARROW));
        					participant.getPlayer().sendMessage("§8+ §f1 화살");
        				}
        				if (!((AbstractJob) participant.getAbility()).hasBow()) {
        					participant.getPlayer().getInventory().setItem(2, Items.NORMAL_BOW.getStack());
        				}
        			}	
    			}
    		}
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			bossBar.removeAll();
			bossBar = null;
		}

		@Override
		protected void run(int count) {
			bossBar.setProgress(1);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer galaxymagic = new AbilityTimer(140) {

		@Override
		protected void onStart() {
			bossBar = Bukkit.createBossBar("§5갤럭시 매직", BarColor.PURPLE, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			bossBar.removeAll();
			bossBar = null;
		}

		@Override
		protected void run(int count) {
			bossBar.setProgress(count / 140.0);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer pinklady = new AbilityTimer(300) {

		@Override
		protected void onStart() {
			bossBar = Bukkit.createBossBar("§d핑크 레이디", BarColor.PINK, BarStyle.SOLID);
    		bossBar.setProgress(1);
    		bossBar.addPlayer(getPlayer());
    		if (ServerVersion.getVersion() >= 10) bossBar.setVisible(true);
		}

		@Override
		protected void onEnd() {
			onSilentEnd();
		}

		@Override
		protected void onSilentEnd() {
			bossBar.removeAll();
			bossBar = null;
			target = null;
		}

		@Override
		protected void run(int count) {
			if (pinkladyinv.isRunning()) {
				bossBar.setProgress(Math.min(1, count / 40.0));
				bossBar.setTitle("§d핑크 레이디 발동 중");
				if (count % 2 == 0) bossBar.setColor(BarColor.PINK);
				else bossBar.setColor(BarColor.WHITE);
			} else bossBar.setProgress(count / 300.0);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	private final AbilityTimer pinkladyinv = new AbilityTimer(40) {
		
		@Override
		protected void onStart() {
			pinklady.setCount(40);
		}
		
		@Override
		protected void onEnd() {
			getPlayer().setHealth(0);
		}
		
		@Override
		protected void onSilentEnd() {
			if (pinklady.isRunning()) pinklady.stop(false);
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	public AbilityTimer passive = new AbilityTimer() {
		
		@Override
		public void run(int count) {
			if (bloodymary.isRunning()) {
				if (LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate2) != null) {
					getPlayer().setCompassTarget(LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate2).getLocation());	
				}
			} else {
				if (LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate) != null) {
					getPlayer().setCompassTarget(LocationUtil.getNearestEntity(Player.class, getPlayer().getLocation(), predicate).getLocation());	
				}
			}
		}
		
	}.setPeriod(TimeUnit.TICKS, 1).register();
	
	@SubscribeEvent
	private void onPlayerJoin(final PlayerJoinEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.addPlayer(e.getPlayer());
		}
	}

	@SubscribeEvent
	private void onPlayerQuit(final PlayerQuitEvent e) {
		if (getPlayer().getUniqueId().equals(e.getPlayer().getUniqueId())) {
			if (bossBar != null) bossBar.removePlayer(e.getPlayer());
		}
	}
	
	@SubscribeEvent(priority = 1)
	private void onMurderMagarita(MurderEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled() && magarita.isRunning()) {
			e.setCancelled(true);
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
			ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
		}
	}
	
	@SubscribeEvent(priority = 7)
	private void onMurderPinkLady(MurderEvent e) {
		if (e.getTarget().equals(target) && !e.isCancelled() && pinklady.isRunning()) {
			e.setCancelled(true);
			if (!pinkladyinv.isRunning()) pinkladyinv.start();
			if (attacker == null) attacker = e.getMurderer().getParticipant();
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
			ParticleLib.SPELL_WITCH.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
		}
	}
	
	@SubscribeEvent(priority = 10)
	private void onMurderPinkLadyKilled(MurderEvent e) {
		if (attacker != null && !e.isCancelled() && pinkladyinv.isRunning()) {
			if (e.getTarget().equals(attacker)) pinkladyinv.stop(true);
		}
	}
	
	@SubscribeEvent(priority = 1)
	private void onArrowKillMagarita(ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled() && magarita.isRunning()) {
			e.setCancelled(true);
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
			ParticleLib.CRIT_MAGIC.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
		}
	}
	
	@SubscribeEvent(priority = 7)
	private void onArrowKillPinkLady(ArrowKillEvent e) {
		if (e.getTarget().equals(target) && !e.isCancelled() && pinklady.isRunning()) {
			e.setCancelled(true);
			if (!pinkladyinv.isRunning()) pinkladyinv.start();
			if (attacker == null) attacker = e.getShooter();
			SoundLib.ITEM_SHIELD_BLOCK.playSound(getPlayer().getLocation(), 1 , 1.25f);
			ParticleLib.SPELL_WITCH.spawnParticle(getPlayer().getLocation(), 0.5, 1, 0.5, 10, 0);
		}
	}
	
	@SubscribeEvent(priority = 10)
	private void onArrowKilledPinkLady(ArrowKillEvent e) {
		if (attacker != null && !e.isCancelled() && pinkladyinv.isRunning()) {
			if (e.getTarget().equals(attacker)) pinkladyinv.stop(true);
		}
	}
	
	@SubscribeEvent(priority = 9)
	private void onArrowKillGalaxyMagic(ArrowKillEvent e) {
		if (e.getTarget().equals(getParticipant()) && !e.isCancelled()) {
			if (galaxymagic.isRunning()) {
				e.setCancelled(true);
				ParticleLib.PORTAL.spawnParticle(e.getShooter().getPlayer().getEyeLocation(), 0, 0, 0, 150, 3);
				e.getShooter().getPlayer().setHealth(0);
			}
		}
	}
	
	public class SkillGUI extends AbilityTimer implements Listener {
		
		private Map<Integer, String> slots = new HashMap<>();
		private String selected;
		private final Inventory gui = Bukkit.createInventory(null, 45, "§0만들 음료를 선택해주세요.");
		
	    private final List<Integer> slotlocations = new ArrayList<Integer>(Arrays.asList(10, 12, 14, 16, 28, 30, 32, 34));
	    private final ItemStack NULL = (new ItemBuilder(MaterialX.GRAY_STAINED_GLASS_PANE)).displayName(" ").build();
		
		public SkillGUI() {
			super(TaskType.REVERSE, 600);
			setPeriod(TimeUnit.TICKS, 1);
			Bukkit.getPluginManager().registerEvents(this, AbilityWar.getPlugin());
			openGUI();
		}
		
		private void openGUI() {
	    	gui.clear();
	    	int slot = 0;
	       	for (String cocktails : COCKTAILS.keySet()) {
				ItemStack item = new ItemBuilder(MaterialX.POTION).build();
				PotionMeta meta = (PotionMeta) item.getItemMeta();
				meta.setDisplayName(cocktails);
				meta.setColor(COCKTAILS.get(cocktails).getRight());
				String[] description = COCKTAILS_DESCRIPTION.get(cocktails).split("#");
				List<String> lore = new ArrayList<>();
				lore.add("§a필요 금§7: §e" + COCKTAILS.get(cocktails).getLeft() + "§7개");
				lore.add("§7");
				for (int a = 0; a < description.length; a++) {
					lore.add(description[a]);
				}
				lore.add("§7");
				lore.add("§5» §d앞으로 이 칵테일을 주조하려면 선택하세요.");
				meta.setLore(lore);
				item.setItemMeta(meta);
    			gui.setItem(slotlocations.get(slot), item);
				slots.put(slotlocations.get(slot), cocktails);
    			slot++;	
	    	}
	       	for (int a = 0; a < 45; a++) {
	       		if (!slotlocations.contains(a)) gui.setItem(a, NULL);
	       	}
	        getPlayer().openInventory(gui);
	    }
		
	    @EventHandler()
	    public void onInventoryClick(InventoryClickEvent e) {
	    	if (e.getClickedInventory().equals(gui)) {
	    		e.setCancelled(true);
	    		if (slots.containsKey(e.getSlot())) {
	    			selected = slots.get(e.getSlot());
		    		getPlayer().closeInventory();
		    		stop(false);
	    		}
	    	}
	    }
	    
		@Override
		protected void onEnd() {
			onSilentEnd();
		}
		
		@Override
		protected void onSilentEnd() {
			if (selected != null) {
				cocktail = selected;
				ac.update(cocktail);
				SoundLib.ENTITY_ILLUSIONER_CAST_SPELL.playSound(getPlayer(), 1, 1.8f);
			}
			HandlerList.unregisterAll(this);
			getPlayer().closeInventory();
			selected = null;
		}
		
	}
	
}
