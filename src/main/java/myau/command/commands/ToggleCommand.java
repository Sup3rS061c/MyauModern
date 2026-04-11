package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.module.Module;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super(new ArrayList<>(Arrays.asList("toggle", "t")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(
                    String.format("%sUsage: .%s <&omodule&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT))
            );
        } else {
            Module module = Myau.moduleManager.getModule(args.get(1));
            if (module == null) {
                ChatUtil.sendFormatted(String.format("%sModule not found (&o%s&r)&r", Myau.clientName, args.get(1)));
            } else {
                boolean changed = true;
                if (args.size() >= 3) {
                    if (args.get(2).equalsIgnoreCase("true")
                            || args.get(2).equalsIgnoreCase("on")
                            || args.get(2).equalsIgnoreCase("1")) {
                        changed = !module.isEnabled();
                    } else if (args.get(2).equalsIgnoreCase("false")
                            || args.get(2).equalsIgnoreCase("off")
                            || args.get(2).equalsIgnoreCase("0")) {
                        changed = module.isEnabled();
                    }
                }
                if (changed && module.toggle()) {
                    ChatUtil.sendFormatted(String.format("%s%s: %s&r", Myau.clientName, module.getName(), module.isEnabled() ? "&a&lON" : "&c&lOFF"));
                }
            }
        }
    }

    @Override
    public List<String> tabComplete(ArrayList<String> args) {
        List<String> completions = new ArrayList<>();

        if (args.size() <= 1) {
            // 补全模块名
            String partial = args.isEmpty() ? "" : args.get(0).toLowerCase();
            for (Module module : Myau.moduleManager.modules.values()) {
                String name = module.getName();
                if (name.toLowerCase().startsWith(partial)) {
                    completions.add(name);
                }
            }
        } else if (args.size() == 2) {
            // 补全状态值
            String partial = args.get(1).toLowerCase();
            String[] states = {"on", "off", "true", "false"};
            for (String state : states) {
                if (state.startsWith(partial)) {
                    completions.add(state);
                }
            }
        }

        return completions.stream()
                .sorted()
                .collect(Collectors.toList());
    }
}
