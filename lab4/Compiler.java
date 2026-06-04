import java.io.*;
import java.util.*;

public class Compiler {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java Compiler <input.cpp>");
            System.exit(1);
        }
        String source = readFile(args[0]);

        // 1. Препроцессинг (ЛР1) – удаление комментариев, лишних пробелов
        String cleaned = Preprocessor.cleanCode(source);
        if (cleaned == null) { System.err.println("Ошибка препроцессора"); return; }

        // 2. Лексический анализ (ЛР2) -> список токенов
        List<LexicalAnalyzer.Token> tokens;
        try {
            tokens = LexicalAnalyzer.tokenize(cleaned);
        } catch (LexicalAnalyzer.LexicalException e) {
            System.err.println("Лексическая ошибка: " + e.getMessage()); return;
        }

        // 3. Синтаксический анализ (ЛР3) -> AST
        SyntaxAnalyzer parser = new SyntaxAnalyzer(tokens);
        Program ast = parser.parseProgram();
        if (!parser.errors.isEmpty()) {
            for (String err : parser.errors) System.err.println(err);
            return;
        }

        // 4. Семантический анализ и генерация триад (ЛР4)
        SemanticAnalyzer sem = new SemanticAnalyzer();
        try {
            sem.analyze(ast);
        } catch (SemanticException e) {
            System.err.println("Семантическая ошибка: " + e.getMessage());
            return;
        }

        // Вывод результатов
        System.out.println("Семантический анализ успешно завершён.");
        sem.printSymbolTable();
        System.out.println("\nПромежуточное представление (триады):");
        sem.printTriples();
    }

    private static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }
}