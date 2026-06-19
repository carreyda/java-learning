public class LoopDemo {
    public static void main(String[] args) {
        int sum = 0;
        for (int i = 0; i < 101; i++) {
            sum = sum + i;
        }
        System.out.println(sum); // 5050

        int i = 1;
        while (i < 101) {
            if(i % 7 == 0) {
                System.out.println(i); // 5050
            }
            i++;
        }

        for (int j = 1; j < 6; j++) {
            for (int k = 0; k < j; k++) {
                System.out.print("*");
            }
            System.out.println();
        }

        int answer = 42;
        int[] guesses = {10, 60, 42, 30}; // 依次猜这几个数
        for (int m = 0; m < guesses.length; m++) {
            if(guesses[m] < answer) {
                System.out.println("猜小了");
            } else if (guesses[m] > answer) {
                System.out.println("猜大了");
            } else {
                System.out.println("猜中了");
                break;
            }
        }
    }
}
