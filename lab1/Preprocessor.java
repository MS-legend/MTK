import java.io.*;
import java.util.regex.*;

public class Preprocessor {
    
    // Проверка на незакрытый многострочный комментарий
    private static boolean checkUnclosedComment(String source) {
        int openCount = 0;
        int closeCount = 0;
        int index = 0;
        while ((index = source.indexOf("/*", index)) != -1) {
            openCount++;
            index += 2;
        }
        index = 0;
        while ((index = source.indexOf("*/", index)) != -1) {
            closeCount++;
            index += 2;
        }
        return openCount != closeCount;
    }
    
    // Основной метод очистки
    public static String cleanCode(String source) {
        // Проверка только на незакрытый комментарий
        if (checkUnclosedComment(source)) {
            System.err.println("Ошибка: незакрытый многострочный комментарий");
            return null;
        }
        
        // 1. Удаление однострочных комментариев //
        String noSingleLine = source.replaceAll("//.*$", "");
        
        // 2. Удаление многострочных комментариев /* ... */
        String noComments = noSingleLine.replaceAll("/\\*.*?\\*/", "");
        
        // 3. Разбиваем на строки для построчной обработки пробелов
        String[] lines = noComments.split("\\r?\\n");
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            // Удаляем пробелы в начале и конце строки
            String trimmed = line.replaceAll("^\\s+|\\s+$", "");
            // Заменяем последовательности пробелов внутри на один пробел
            String normalized = trimmed.replaceAll("\\s+", " ");
            // Если строка не пуста после этого, добавляем её
            if (!normalized.isEmpty()) {
                result.append(normalized).append("\n");
            }
        }
        
        // Удаляем возможные лишние пустые строки в конце
        return result.toString().replaceAll("\\n\\s*\\n", "\n");
    }
    
    // Точка входа
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Использование: java Preprocessor <input file> [output file]");
            System.exit(1);
        }
        
        String inputFile = args[0];
        String outputFile = (args.length >= 2) ? args[1] : "cleaned_output.txt";
        
        try {
            // Чтение исходного файла
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            // Очистка
            String cleaned = cleanCode(content.toString());
            if (cleaned == null) {
                System.exit(2);
            }
            
            // Запись результата
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(cleaned);
            writer.close();
            
            System.out.println("Очистка завершена. Результат сохранён в " + outputFile);
            System.out.println("Ошибок не выявлено");
            
        } catch (IOException e) {
            System.err.println("Ошибка ввода-вывода: " + e.getMessage());
            System.exit(3);
        }
    }
}