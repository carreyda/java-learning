public class BuilderDemo {
    public static void main(String[] args) {
        // 用循环拼接出字符串 "1-2-3-4-5-6-7-8-9-10"（注意最后没有 -）
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 11; i++) {
           if(i != 10) {
               sb.append(i + "-");
           } else {
               sb.append(i);
           }
        }
        System.out.println(sb);

        // 将一个字符串反转（用 StringBuilder.reverse()）
        StringBuilder sb1 = new StringBuilder("StringBuilder");
        sb1.reverse();
        System.out.println(sb1);

        // 给定姓名数组 {"张三", "李四", "王五", "赵六"}，拼接成 "张三、李四、王五、赵六" 格式
        String[] names = {"张三", "李四", "王五", "赵六"};
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            sb2.append(names[i]);
            if (i < names.length - 1) {   // 非最后一个元素时添加分隔符
                sb2.append("、");
            }
        }
        System.out.println(sb2.toString()); // 输出: 张三、李四、王五、赵六
    }
}
