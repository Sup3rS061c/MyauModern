package myau.command;

import myau.Myau;
import myau.command.commands.ModuleCommand;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.util.ChatUtil;
import net.minecraft.network.play.client.C01PacketChatMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager {
    public ArrayList<Command> commands;

    public CommandManager() {
        this.commands = new ArrayList<>();
    }

    public void handleCommand(String string) {
        List<String> params = Arrays.asList(string.substring(1).trim().split("\\s+"));
        ArrayList<String> arrayList = new ArrayList<>(params);
        if (params.get(0).isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sUnknown command&r", Myau.clientName).replace("&", "§"));
        } else {
            for (Command command : Myau.commandManager.commands) {
                for (String name : command.names) {
                    if (params.get(0).equalsIgnoreCase(name)) {
                        command.runCommand(arrayList);
                        return;
                    }
                }
            }
            ChatUtil.sendFormatted(String.format("%sUnknown command (&o%s&r)&r", Myau.clientName, params.get(0)).replace("&", "§"));
        }
    }

    public boolean isTypingCommand(String string) {
        if (string == null || string.length() < 2) {
            return false;
        } else {
            return string.charAt(0) == '.' && Character.isLetterOrDigit(string.charAt(1));
        }
    }

    /**
     * 获取命令自动补全建议
     * @param input 当前输入的完整命令字符串（包含命令前缀 .）
     * @return 补全建议列表
     */
    public List<String> getCompletions(String input) {
        List<String> completions = new ArrayList<>();

        // 去除命令前缀点号
        String withoutPrefix = input.substring(1).trim();

        // 分割参数
        String[] parts = withoutPrefix.split("\\s+");

        // 检查是否包含空格
        boolean hasSpace = withoutPrefix.contains(" ");

        if (parts.length == 1 && !hasSpace) {
            // 正在输入命令名 - 匹配命令名和模块名
            String partialCommand = parts[0].toLowerCase();

            // 首先匹配命令名
            for (Command command : commands) {
                for (String name : command.names) {
                    if (name.toLowerCase().startsWith(partialCommand)) {
                        completions.add("." + name);
                    }
                }
            }

            // 然后匹配模块名（用于模块命令）
            for (Module module : Myau.moduleManager.modules.values()) {
                String name = module.getName();
                if (name.toLowerCase().startsWith(partialCommand)) {
                    completions.add("." + name);
                }
            }
        } else {
            // 已经有命令名，需要获取该命令的参数补全
            String commandName = parts[0];
            Command targetCommand = null;

            // 找到对应的命令
            for (Command command : commands) {
                if (command.matches(commandName)) {
                    targetCommand = command;
                    break;
                }
            }

            // 如果没找到命令，检查是否是模块名（使用ModuleCommand处理）
            if (targetCommand == null) {
                Module module = Myau.moduleManager.getModule(commandName);
                if (module != null) {
                    // 这是模块命令，找到ModuleCommand
                    for (Command command : commands) {
                        if (command instanceof ModuleCommand) {
                            targetCommand = command;
                            break;
                        }
                    }
                }
            }

            if (targetCommand != null) {
                // 构建参数列表（排除命令名）
                ArrayList<String> args = new ArrayList<>();
                // 对于模块命令，需要把模块名作为第一个参数
                if (targetCommand instanceof ModuleCommand && Myau.moduleManager.getModule(commandName) != null) {
                    args.add(commandName);
                    for (int i = 1; i < parts.length; i++) {
                        args.add(parts[i]);
                    }
                } else {
                    for (int i = 1; i < parts.length; i++) {
                        args.add(parts[i]);
                    }
                }

                // 如果最后一个字符是空格，表示需要补全下一个参数
                if (input.endsWith(" ")) {
                    args.add("");
                }

                // 获取该命令的补全建议
                List<String> commandCompletions = targetCommand.tabComplete(args);

                // 根据当前输入构建完整建议
                int lastSpaceIndex = input.lastIndexOf(' ');
                String base;
                if (lastSpaceIndex >= 0) {
                    base = input.substring(0, lastSpaceIndex + 1);
                } else {
                    base = "." + commandName + " ";
                }
                for (String completion : commandCompletions) {
                    completions.add(base + completion);
                }
            }
        }

        return completions.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C01PacketChatMessage) {
            String msg = ((C01PacketChatMessage) event.getPacket()).getMessage();
            if (this.isTypingCommand(msg)) {
                event.setCancelled(true);
                this.handleCommand(msg);
            }
        }
    }
}
