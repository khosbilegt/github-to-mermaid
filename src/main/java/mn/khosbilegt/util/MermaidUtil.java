package mn.khosbilegt.util;

public class MermaidUtil {
    public static String generatePieChart(String title, String[] labels, int[] values) {
        StringBuilder chart = new StringBuilder("pie\n");
        chart.append("    title ").append(title).append("\n");
        for (int i = 0; i < labels.length; i++) {
            chart.append("    \"").append(labels[i]).append("\": ").append(values[i]).append("\n");
        }
        return chart.toString();
    }
}
