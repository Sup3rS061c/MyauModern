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
import java.util.Locale;
import java.util.stream.Collectors;

public class CommandManager {
    public ArrayList<Command> commands;
    
    // 存储最新的自动补全结果
    private String[] latestAutoComplete = new String[0];
    
    public static final String PREFIX = ".";

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
     * 执行自动补全
     * @param input 当前输入
     * @return 是否成功获取到补全建议
     */
    public boolean autoComplete(String input) {
        latestAutoComplete = getCompletions(input);
        return input.startsWith(PREFIX) && latestAutoComplete.length > 0;
    }
    
    /**
     * 获取最新的自动补全结果
     */
    public String[] getLatestAutoComplete() {
        return latestAutoComplete;
    }

    /**
     * 获取命令自动补全建议
     * @param input 当前输入的完整命令字符串（包含命令前缀 .）
     * @return 补全建议数组
     */
    public String[] getCompletions(String input) {
        if (!input.startsWith(PREFIX)) {
            return new String[0];
        }
        
        String rawInput = input.substring(PREFIX.length());
        String[] args = rawInput.split(" ");
        
        if (args.length > 1 || rawInput.contains(" ")) {
            // 多参数：调用命令的tabComplete方法
            String commandName = args[0];
            Command command = getCommand(commandName);
            
            if (command != null) {
                // 构建参数列表（排除命令名）
                ArrayList<String> commandArgs = new ArrayList<>();
                for (int i = 1; i < args.length; i++) {
                    commandArgs.add(args[i]);
                }
                
                // 如果输入以空格结尾，添加空字符串作为下一个参数
                if (input.endsWith(" ")) {
                    commandArgs.add("");
                }
                
                List<String> completions = command.tabComplete(commandArgs);
                
                // 添加前缀到补全结果
                String base = PREFIX + commandName + " ";
                if (!commandArgs.isEmpty() && !input.endsWith(" ")) {
                    // 如果正在输入最后一个参数，保留已输入部分
                    base = input.substring(0, input.lastIndexOf(' ') + 1);
                }
                
                List<String> result = new ArrayList<>();
                for (String completion : completions) {
                    result.add(base + completion);
                }
                
                return result.toArray(new String[0]);
            }
        } else {
            // 单参数：匹配命令名/别名
            List<String> matches = new ArrayList<>();
            String partial = rawInput.toLowerCase(Locale.ROOT);
            
            for (Command command : commands) {
                for (String name : command.names) {
                    if (name.toLowerCase(Locale.ROOT).startsWith(partial)) {
                        matches.add(PREFIX + name);
                    }
                }
            }
            
            return matches.stream()
                    .distinct()
                    .sorted()
                    .toArray(String[]::new);
        }
        
        return new String[0];
    }
    
    /**
     * 根据名称查找命令
     */
    public Command getCommand(String name) {
        // 首先检查是否是模块名
        Module module = Myau.moduleManager.getModule(name);
        if (module != null) {
            // 找到ModuleCommand来处理模块命令
            for (Command command : commands) {
                if (command instanceof ModuleCommand) {
                    return command;
                }
            }
        }
        
        // 否则查找匹配的命令
        for (Command command : commands) {
            if (command.matches(name)) {
                return command;
            }
        }
        
        return null;
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
