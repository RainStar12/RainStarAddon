package rainstar.abilitywar.system.command;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import daybreak.abilitywar.utils.base.Messager;

import java.util.List;

public class CommandTabCompleter implements Listener {

	@EventHandler
	public void onTabComplete(TabCompleteEvent e) {

		String buffer = e.getBuffer();

		String[] args = buffer.split(" ", -1);

		String first = args[0].toLowerCase().replace("/", "");

		List<String> labels = Messager.asList(
				"abilitywar",
				"ability",
				"aw",
				"va",
				"능력자"
		);

		if (!labels.contains(first)) return;

		if (args.length == 2) {
			add(e.getCompletions(), "delay");
			add(e.getCompletions(), "killreward");
			add(e.getCompletions(), "joblist");
			add(e.getCompletions(), "murder");
		}

		if (args.length == 3&& args[1].equalsIgnoreCase("murder")) {
			add(e.getCompletions(), "config");
			add(e.getCompletions(), "time");
		}

		if (args.length == 4 && args[1].equalsIgnoreCase("murder") && args[2].equalsIgnoreCase("time")) {
			add(e.getCompletions(), "add");
			add(e.getCompletions(), "set");
		}
	}

	private void add(List<String> list, String value) {
		if (!list.contains(value)) {
			list.add(value);
		}
	}
}