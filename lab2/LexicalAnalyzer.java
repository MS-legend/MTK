import java.io.*;
import java.util.*;

public class LexicalAnalyzer {
    // Таблицы лексем для Java 8/11
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "include", "int", "bool", "if", "else", "for", "while", "return", "true", "false"
    ));
    private static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
        "+", "-", "*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!", "<<", ">>", "::", "+=", "++"
    ));
    private static final Set<String> DELIMITERS = new HashSet<>(Arrays.asList(
        ";", ",", ".", ":", "{", "}", "(", ")", "#", "\""
    ));
    
    // Токен
    static class Token {
        String type;
        String value;
        Token(String type, String value) { this.type = type; this.value = value; }
        @Override
        public String toString() {
            return "(" + type + ", " + value + ")";
        }
    }
    
    // Исключение лексической ошибки
    static class LexicalException extends Exception {
        String message;
        int position;
        LexicalException(String msg, int pos) {
            super(msg + " на позиции " + pos);
            this.message = msg;
            this.position = pos;
        }
    }
    
    // Основной метод разбора
    public static List<Token> tokenize(String source) throws LexicalException {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int len = source.length();
        
        while (pos < len) {
            char ch = source.charAt(pos);
            if (Character.isWhitespace(ch)) {
                pos++;
                continue;
            }
            
            // Строковые константы
            if (ch == '"') {
                int start = pos;
                pos++; // пропускаем "
                StringBuilder sb = new StringBuilder();
                boolean closed = false;
                while (pos < len) {
                    char c = source.charAt(pos);
                    if (c == '"') {
                        pos++;
                        closed = true;
                        break;
                    }
                    sb.append(c);
                    pos++;
                }
                if (!closed) {
                    throw new LexicalException("Незакрытая строковая константа", start);
                }
                tokens.add(new Token("STRING", sb.toString()));
                continue;
            }
            
            // Идентификаторы и ключевые слова
            if (Character.isLetter(ch) || ch == '_') {
                int start = pos;
                while (pos < len && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
                    pos++;
                }
                String word = source.substring(start, pos);
                if (KEYWORDS.contains(word)) {
                    tokens.add(new Token("KEYWORD", word));
                } else {
                    tokens.add(new Token("IDENTIFIER", word));
                }
                continue;
            }
            
            // Числа (целые и вещественные)
            if (Character.isDigit(ch)) {
                int start = pos;
                boolean isReal = false;
                while (pos < len && Character.isDigit(source.charAt(pos))) {
                    pos++;
                }
                if (pos < len && source.charAt(pos) == '.') {
                    isReal = true;
                    pos++;
                    while (pos < len && Character.isDigit(source.charAt(pos))) {
                        pos++;
                    }
                }
                String num = source.substring(start, pos);
                if (isReal) {
                    tokens.add(new Token("REAL_CONST", num));
                } else {
                    tokens.add(new Token("INT_CONST", num));
                }
                continue;
            }
            
            // Многобуквенные операторы (2 символа)
            boolean found = false;
            if (pos + 1 < len) {
                String two = source.substring(pos, pos+2);
                if (OPERATORS.contains(two)) {
                    tokens.add(new Token("OPERATOR", two));
                    pos += 2;
                    found = true;
                    continue;
                }
            }
            // Однобуквенные операторы
            String one = String.valueOf(ch);
            if (OPERATORS.contains(one)) {
                tokens.add(new Token("OPERATOR", one));
                pos++;
                continue;
            }
            // Разделители
            if (DELIMITERS.contains(one)) {
                tokens.add(new Token("DELIMITER", one));
                pos++;
                continue;
            }
            // Недопустимый символ
            throw new LexicalException("Недопустимый символ '" + ch + "'", pos);
        }
        return tokens;
    }
    
    // Чтение файла
    private static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
    
    // Точка входа
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Использование: java LexicalAnalyzer <input file>");
            System.exit(1);
        }
        try {
            String content = readFile(args[0]);
            List<Token> tokens = tokenize(content);
            System.out.println("Лексема | Тип");
            System.out.println("--------|----------");
            for (Token t : tokens) {
                System.out.printf("%-7s | %s%n", t.value, t.type);
            }
            System.out.println("\nПоследовательность токенов:");
            System.out.println(tokens);
            System.out.printf("\nЛексический анализ завершён успешно. Обнаружено %d токенов. Ошибок не найдено.%n", tokens.size());
        } catch (IOException e) {
            System.err.println("Ошибка ввода-вывода: " + e.getMessage());
        } catch (LexicalException e) {
            System.err.println("Лексическая ошибка: " + e.getMessage());
        }
    }
}