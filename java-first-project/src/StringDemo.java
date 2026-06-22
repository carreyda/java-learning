import java.util.Arrays;

public class StringDemo {
    public static void main(String[] args) {
        // 1.去掉首尾空格后打印
        String str = " Hello, Java World! Welcome to Java. ";
        str = str.trim();
        System.out.println(str);
        // 2.统计其中"Java"出现的次数（提示：用indexof+循环）
        // 从索引 0 开始查找，每次找到后从"下一个位置"继续找
        String target = "Java";
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(target, index)) != -1) {
            count++;
            index += target.length(); // 跳过刚才找到的 "Java"，防止死循环
        }
        System.out.println(count);
        // 3.将所有"Java"替换为"Python"后打印
        str = str.replace("Java","Python");
        System.out.println(str);
        // 4.将字符串按空格分割成数组，打印每个单词
        String[] pots = str.split("");
        System.out.println(Arrays.toString(pots));
        // 5.判断字符串是否以“Hello”开头（去掉空格后判断）
        boolean startsWithHello = str.trim().startsWith("Hello"); // Java 是强类型语言，方法的返回值类型必须和接收变量的类型严格匹配;
        System.out.println(startsWithHello);
    }
}
