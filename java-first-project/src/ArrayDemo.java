import java.util.Arrays;
public class ArrayDemo {
    public static void main(String[] args) {
        int[] nums = {64, 25, 12, 22, 11};
        int sum = 0;
        int max = nums[0];
        int min = nums[0];
        for (int i = 0; i < nums.length; i++) {
            System.out.println("元素：" + nums[i]);
            System.out.println("下标：" + i);
            sum = sum + nums[i];
            if (nums[i] > max) max = nums[i];
            if (nums[i] < min) min = nums[i];
        }
        Arrays.sort(nums);
        System.out.println(Arrays.toString(nums));
        System.out.println("总和：" + sum);
        System.out.println("平均：" + sum / nums.length);
        System.out.println("最大值：" + max);
        System.out.println("最小值：" + min);

    }
}
