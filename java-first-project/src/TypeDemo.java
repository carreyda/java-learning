public class TypeDemo {
    public static void main(String[] args) {
        // 整数除法
        System.out.println(10 / 3); // 3  整数除法，两个操作数都是 int，结果直接截断小数部分，输出 3。
        System.out.println(10.0 / 3); // 3.3333 浮点数除法，10.0 是 double，3 自动提升为 double，结果为 double 类型，输出 3.3333333333333335
        System.out.println((double) 10 / 3); // (double)10/3：先将10转为double，再除以3，结果同10.0/3，即3.3333333333333335。

        // 强制转换
        double d = 9.99;
        int i = (int) d;
        System.out.println(i); // 9 强制转换：double d=9.99; int i=(int)d; 强制转换截断小数部分，i=9，输出9。

        // 字符串转数字
        String s = "123";
        int n = Integer.parseInt(s);
        System.out.println(n + 1);  // 应该输出什么？ 1234
        System.out.println(s + 1);  // 应该输出什么？ 1231
    }
}
