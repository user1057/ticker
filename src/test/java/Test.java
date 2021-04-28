import java.math.BigDecimal;
import java.text.DecimalFormat;

public class Test {
    @org.junit.jupiter.api.Test
    public void test(){
        DecimalFormat dfAmount = new DecimalFormat("0 000 000 000.00");
        BigDecimal bd = BigDecimal.valueOf(1123456.78);
        System.out.println(String.format("%,14.2f", bd.floatValue()));
        bd = BigDecimal.valueOf(3456.78);
        System.out.println(String.format("%,14.2f", bd.floatValue()));
    }
}
