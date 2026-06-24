public class MatrixDemo {
    public static void main(String[] args) {
        int[][] matrix = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
        };
        int rows = matrix.length;
        int cols = matrix[0].length;
        int [][] reserve = new int[rows][cols];
        int count = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + "\t");
                if( i == j) {
                    count += matrix[i][j];
                }
                reserve[i][j] = matrix[j][i];
            }
            System.out.println();
        }
        System.out.println(count);
        // 4. 打印转置矩阵（直接内联循环）
        System.out.println("转置矩阵：");
        for (int i = 0; i < reserve.length; i++) {
            for (int j = 0; j < reserve[i].length; j++) {
                System.out.print(reserve[i][j] + "\t");
            }
            System.out.println();
        }
    }
}
