package myau.command;

import java.util.ArrayList;
import java.util.List;

public abstract class Command {
    public final ArrayList<String> names;

    public Command(ArrayList<String> arrayList) {
        this.names = arrayList;
    }

    public abstract void runCommand(ArrayList<String> args);

    /**
     * Tab自动补全功能
     * @param args 当前已输入的参数（已移除命令名）
     * @return 补全建议列表
     */
    public List<String> tabComplete(ArrayList<String> args) {
        return new ArrayList<>();
    }

    /**
     * 获取命令的主名称
     */
    public String getName() {
        return names.isEmpty() ? "" : names.get(0);
    }

    /**
     * 检查此命令是否匹配给定的名称
     */
    public boolean matches(String name) {
        for (String n : names) {
            if (n.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
