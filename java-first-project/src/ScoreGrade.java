public class ScoreGrade {
    public static void main(String[] args) {
        int score = 65;
        if(score >= 90 && score <= 100) {
            System.out.println("优秀"); // 打印一行并换行
        } else if(score >= 75 && score <=89) {
            System.out.println("良好");
        } else if(score >= 60 && score <=74) {
            System.out.println("良好");
        } else if(score >= 0 && score <=59) {
            System.out.println("良好");
        } else {
            System.out.println("错误输入");
        }
    }
}
