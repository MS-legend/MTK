import java.io.*;
import java.util.*;

public class SyntaxAnalyzer {
    // ---------- Лексический анализатор (из ЛР2) ----------
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "include", "int", "bool", "if", "else", "for", "while", "return", "true", "false"
    ));
    private static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
        "+", "-", "*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!", "<<", ">>"
    ));
    private static final Set<String> DELIMITERS = new HashSet<>(Arrays.asList(
        ";", ",", ".", ":", "{", "}", "(", ")", "#", "\""
    ));
    
    static class Token {
        String type; String value;
        Token(String type, String value) { this.type = type; this.value = value; }
        public String toString() { return "(" + type + ", " + value + ")"; }
    }
    
    static class LexicalException extends Exception {
        LexicalException(String msg, int pos) { super(msg + " на позиции " + pos); }
    }
    
    private static List<Token> tokenize(String source) throws LexicalException {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int len = source.length();
        while (pos < len) {
            char ch = source.charAt(pos);
            if (Character.isWhitespace(ch)) { pos++; continue; }
            if (ch == '"') {
                int start = pos;
                pos++;
                StringBuilder sb = new StringBuilder();
                boolean closed = false;
                while (pos < len) {
                    char c = source.charAt(pos);
                    if (c == '"') { pos++; closed = true; break; }
                    sb.append(c);
                    pos++;
                }
                if (!closed) throw new LexicalException("Незакрытая строка", start);
                tokens.add(new Token("STRING", sb.toString()));
                continue;
            }
            if (Character.isLetter(ch) || ch == '_') {
                int start = pos;
                while (pos < len && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) pos++;
                String word = source.substring(start, pos);
                if (KEYWORDS.contains(word)) tokens.add(new Token("KEYWORD", word));
                else tokens.add(new Token("IDENTIFIER", word));
                continue;
            }
            if (Character.isDigit(ch)) {
                int start = pos;
                boolean isReal = false;
                while (pos < len && Character.isDigit(source.charAt(pos))) pos++;
                if (pos < len && source.charAt(pos) == '.') {
                    isReal = true;
                    pos++;
                    while (pos < len && Character.isDigit(source.charAt(pos))) pos++;
                }
                String num = source.substring(start, pos);
                tokens.add(new Token(isReal ? "REAL_CONST" : "INT_CONST", num));
                continue;
            }
            if (pos + 1 < len) {
                String two = source.substring(pos, pos+2);
                if (OPERATORS.contains(two)) { tokens.add(new Token("OPERATOR", two)); pos += 2; continue; }
            }
            String one = String.valueOf(ch);
            if (OPERATORS.contains(one)) { tokens.add(new Token("OPERATOR", one)); pos++; continue; }
            if (DELIMITERS.contains(one)) { tokens.add(new Token("DELIMITER", one)); pos++; continue; }
            throw new LexicalException("Недопустимый символ '" + ch + "'", pos);
        }
        return tokens;
    }
    
    // ---------- AST узлы ----------
    interface AstNode {}
    static class Program implements AstNode { List<FunctionDecl> functions = new ArrayList<>(); }
    static class FunctionDecl implements AstNode { 
        String name; String returnType; List<VarDecl> params = new ArrayList<>(); CompoundStmt body;
        FunctionDecl(String name, String returnType) { this.name = name; this.returnType = returnType; }
    }
    static class VarDecl implements AstNode { 
        String name; String type;
        VarDecl(String name, String type) { this.name = name; this.type = type; }
    }
    static class CompoundStmt implements AstNode { List<Stmt> statements = new ArrayList<>(); }
    interface Stmt extends AstNode {}
    static class IfStmt implements Stmt { Expr condition; Stmt thenStmt; Stmt elseStmt; }
    static class WhileStmt implements Stmt { Expr condition; Stmt body; }
    static class ForStmt implements Stmt { Stmt init; Expr condition; Stmt update; Stmt body; }
    static class AssignStmt implements Stmt { Expr left; Expr right; }
    static class ReturnStmt implements Stmt { Expr value; }
    static class ExprStmt implements Stmt { Expr expr; } // для вызовов функций и cout
    interface Expr extends AstNode {}
    static class BinaryOp implements Expr { Expr left; Expr right; String op; }
    static class UnaryOp implements Expr { Expr expr; String op; }
    static class IntegerLiteral implements Expr { int value; IntegerLiteral(int v) { value = v; } }
    static class BooleanLiteral implements Expr { boolean value; BooleanLiteral(boolean v) { value = v; } }
    static class StringLiteral implements Expr { String value; StringLiteral(String v) { value = v; } }
    static class Identifier implements Expr { String name; Identifier(String n) { name = n; } }
    static class CallExpr implements Expr { String name; List<Expr> args = new ArrayList<>(); }
    
    // ---------- Синтаксический анализатор ----------
    private List<Token> tokens;
    private int pos;
    private List<String> errors;
    
    public SyntaxAnalyzer(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.errors = new ArrayList<>();
    }
    
    private Token peek() { return pos < tokens.size() ? tokens.get(pos) : null; }
    private Token consume() { return tokens.get(pos++); }
    private boolean match(String expectedType, String expectedValue) {
        Token t = peek();
        if (t != null && t.type.equals(expectedType) && t.value.equals(expectedValue)) {
            pos++;
            return true;
        }
        return false;
    }
    private boolean matchType(String expectedType) {
        Token t = peek();
        if (t != null && t.type.equals(expectedType)) { pos++; return true; }
        return false;
    }
    private void error(String expected) {
        Token t = peek();
        String found = (t == null) ? "конец файла" : t.type + "(" + t.value + ")";
        errors.add("Ошибка: ожидалось " + expected + ", найдено " + found);
    }
    
    // Грамматика
    public Program parseProgram() {
        Program prog = new Program();
        while (peek() != null) {
            Token t = peek();
            if (t.type.equals("KEYWORD") && (t.value.equals("int") || t.value.equals("bool"))) {
                prog.functions.add(parseFunction());
            } else if (t.type.equals("KEYWORD") && t.value.equals("include")) {
                consume(); // пропускаем #include
                if (matchType("STRING")) {}
            } else {
                break;
            }
        }
        return prog;
    }
    
    private FunctionDecl parseFunction() {
        String returnType = consume().value;
        if (!matchType("IDENTIFIER")) error("имя функции");
        String name = tokens.get(pos-1).value;
        if (!match("DELIMITER", "(")) error("'('");
        FunctionDecl func = new FunctionDecl(name, returnType);
        if (!match("DELIMITER", ")")) {
            do {
                if (!matchType("KEYWORD")) error("тип параметра");
                String ptype = tokens.get(pos-1).value;
                if (!matchType("IDENTIFIER")) error("имя параметра");
                String pname = tokens.get(pos-1).value;
                func.params.add(new VarDecl(pname, ptype));
            } while (match("DELIMITER", ","));
            if (!match("DELIMITER", ")")) error("')'");
        }
        func.body = parseCompoundStmt();
        return func;
    }
    
    private CompoundStmt parseCompoundStmt() {
        if (!match("DELIMITER", "{")) error("'{'");
        CompoundStmt block = new CompoundStmt();
        while (peek() != null && !(peek().type.equals("DELIMITER") && peek().value.equals("}"))) {
            Stmt stmt = parseStatement();
            if (stmt != null) block.statements.add(stmt);
        }
        if (!match("DELIMITER", "}")) error("'}'");
        return block;
    }
    
    private Stmt parseStatement() {
        Token t = peek();
        if (t == null) return null;
        if (t.type.equals("KEYWORD")) {
            switch (t.value) {
                case "if": return parseIfStmt();
                case "while": return parseWhileStmt();
                case "for": return parseForStmt();
                case "return": return parseReturnStmt();
                case "int": case "bool": return parseVarDeclStmt();
                default: // другие ключевые слова (например, break) не поддерживаются
                    error("оператор");
                    consume();
                    return null;
            }
        }
        // Присваивание или вызов функции / оператор <<
        Expr expr = parseExpression();
        if (match("OPERATOR", "=")) {
            Expr right = parseExpression();
            if (!match("DELIMITER", ";")) error("';'");
            AssignStmt stmt = new AssignStmt();
            stmt.left = expr;
            stmt.right = right;
            return stmt;
        } else {
            // это может быть вызов функции или оператор << (cout)
            if (match("DELIMITER", ";")) {
                ExprStmt stmt = new ExprStmt();
                stmt.expr = expr;
                return stmt;
            } else if (match("OPERATOR", "<<")) {
                // упрощённо: x << y считаем бинарным оператором
                Expr right = parseExpression();
                if (!match("DELIMITER", ";")) error("';'");
                ExprStmt stmt = new ExprStmt();
                BinaryOp bin = new BinaryOp();
                bin.left = expr;
                bin.right = right;
                bin.op = "<<";
                stmt.expr = bin;
                return stmt;
            } else {
                error("';' или оператор");
                return null;
            }
        }
    }
    
    private IfStmt parseIfStmt() {
        consume(); // if
        if (!match("DELIMITER", "(")) error("'('");
        Expr cond = parseExpression();
        if (!match("DELIMITER", ")")) error("')'");
        Stmt thenStmt = parseStatement();
        IfStmt stmt = new IfStmt();
        stmt.condition = cond;
        stmt.thenStmt = thenStmt;
        if (match("KEYWORD", "else")) {
            stmt.elseStmt = parseStatement();
        }
        return stmt;
    }
    
    private WhileStmt parseWhileStmt() {
        consume(); // while
        if (!match("DELIMITER", "(")) error("'('");
        Expr cond = parseExpression();
        if (!match("DELIMITER", ")")) error("')'");
        Stmt body = parseStatement();
        WhileStmt stmt = new WhileStmt();
        stmt.condition = cond;
        stmt.body = body;
        return stmt;
    }
    
    private ForStmt parseForStmt() {
        consume(); // for
        if (!match("DELIMITER", "(")) error("'('");
        ForStmt stmt = new ForStmt();
        // инициализация
        if (!match("DELIMITER", ";")) {
            if (peek().type.equals("KEYWORD") && (peek().value.equals("int") || peek().value.equals("bool"))) {
                stmt.init = parseVarDeclStmt();
            } else {
                Expr left = parseExpression();
                if (match("OPERATOR", "=")) {
                    Expr right = parseExpression();
                    AssignStmt ass = new AssignStmt();
                    ass.left = left;
                    ass.right = right;
                    stmt.init = ass;
                } else {
                    error("инициализация цикла");
                }
            }
            if (!match("DELIMITER", ";")) error("';'");
        } else { match("DELIMITER", ";"); }
        // условие
        if (!match("DELIMITER", ";")) {
            stmt.condition = parseExpression();
            if (!match("DELIMITER", ";")) error("';'");
        } else { match("DELIMITER", ";"); }
        // инкремент
        if (!match("DELIMITER", ")")) {
            Expr upd = parseExpression();
            AssignStmt ass = new AssignStmt();
            ass.left = upd;
            stmt.update = ass;
            if (!match("DELIMITER", ")")) error("')'");
        } else { match("DELIMITER", ")"); }
        stmt.body = parseStatement();
        return stmt;
    }
    
    private ReturnStmt parseReturnStmt() {
        consume(); // return
        Expr val = parseExpression();
        if (!match("DELIMITER", ";")) error("';'");
        ReturnStmt stmt = new ReturnStmt();
        stmt.value = val;
        return stmt;
    }
    
    private Stmt parseVarDeclStmt() {
        String type = consume().value;
        if (!matchType("IDENTIFIER")) error("имя переменной");
        String name = tokens.get(pos-1).value;
        Expr init = null;
        if (match("OPERATOR", "=")) init = parseExpression();
        if (!match("DELIMITER", ";")) error("';'");
        // Превращаем объявление в оператор присваивания, если есть инициализация
        if (init != null) {
            AssignStmt ass = new AssignStmt();
            ass.left = new Identifier(name);
            ass.right = init;
            return ass;
        } else {
            // объявление без инициализации – игнорируем (можно создать фиктивный узел)
            return null;
        }
    }
    
    // Выражения
    private Expr parseExpression() { return parseLogicalOr(); }
    private Expr parseLogicalOr() {
        Expr left = parseLogicalAnd();
        while (match("OPERATOR", "||")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseLogicalAnd();
            BinaryOp bin = new BinaryOp();
            bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseLogicalAnd() {
        Expr left = parseEquality();
        while (match("OPERATOR", "&&")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseEquality();
            BinaryOp bin = new BinaryOp();
            bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseEquality() {
        Expr left = parseRelational();
        while (match("OPERATOR", "==") || match("OPERATOR", "!=")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseRelational();
            BinaryOp bin = new BinaryOp();
            bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseRelational() {
        Expr left = parseAdditive();
        while (match("OPERATOR", "<") || match("OPERATOR", ">") || match("OPERATOR", "<=") || match("OPERATOR", ">=")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseAdditive();
            BinaryOp bin = new BinaryOp();
            bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseAdditive() {
        Expr left = parseMultiplicative();
        while (match("OPERATOR", "+") || match("OPERATOR", "-")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseMultiplicative();
            BinaryOp bin = new BinaryOp();
            bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseMultiplicative() {
        Expr left = parseUnary();
        while (match("OPERATOR", "*") || match("OPERATOR", "/") || match("OPERATOR", "%")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseUnary();
            BinaryOp bin = new BinaryOp();
            bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseUnary() {
        if (match("OPERATOR", "!") || match("OPERATOR", "-")) {
            String op = tokens.get(pos-1).value;
            Expr expr = parseUnary();
            UnaryOp un = new UnaryOp();
            un.expr = expr; un.op = op;
            return un;
        }
        return parsePrimary();
    }
    private Expr parsePrimary() {
        Token t = peek();
        if (t.type.equals("INT_CONST")) {
            consume();
            return new IntegerLiteral(Integer.parseInt(t.value));
        } else if (t.type.equals("STRING")) {
            consume();
            return new StringLiteral(t.value);
        } else if (t.type.equals("KEYWORD") && (t.value.equals("true") || t.value.equals("false"))) {
            consume();
            return new BooleanLiteral(Boolean.parseBoolean(t.value));
        } else if (t.type.equals("IDENTIFIER")) {
            consume();
            String name = t.value;
            if (match("DELIMITER", "(")) {
                CallExpr call = new CallExpr();
                call.name = name;
                if (!match("DELIMITER", ")")) {
                    do {
                        call.args.add(parseExpression());
                    } while (match("DELIMITER", ","));
                    if (!match("DELIMITER", ")")) error("')'");
                }
                return call;
            } else {
                return new Identifier(name);
            }
        } else if (match("DELIMITER", "(")) {
            Expr inner = parseExpression();
            if (!match("DELIMITER", ")")) error("')'");
            return inner;
        } else {
            error("выражение");
            return null;
        }
    }
    
    // Печать AST
    public void printAST(AstNode node, String indent) {
        if (node == null) return;
        if (node instanceof Program) {
            Program p = (Program) node;
            System.out.println(indent + "Program");
            for (FunctionDecl f : p.functions) printAST(f, indent + "  ");
        } else if (node instanceof FunctionDecl) {
            FunctionDecl f = (FunctionDecl) node;
            System.out.println(indent + "Function: " + f.name + " -> " + f.returnType);
            if (!f.params.isEmpty()) {
                System.out.println(indent + "  Params:");
                for (VarDecl v : f.params) System.out.println(indent + "    " + v.name + ": " + v.type);
            }
            printAST(f.body, indent + "  ");
        } else if (node instanceof CompoundStmt) {
            CompoundStmt c = (CompoundStmt) node;
            System.out.println(indent + "{");
            for (Stmt s : c.statements) printAST(s, indent + "  ");
            System.out.println(indent + "}");
        } else if (node instanceof IfStmt) {
            IfStmt i = (IfStmt) node;
            System.out.print(indent + "if (");
            printAST(i.condition, "");
            System.out.println(")");
            printAST(i.thenStmt, indent + "  ");
            if (i.elseStmt != null) {
                System.out.println(indent + "else");
                printAST(i.elseStmt, indent + "  ");
            }
        } else if (node instanceof WhileStmt) {
            WhileStmt w = (WhileStmt) node;
            System.out.print(indent + "while (");
            printAST(w.condition, "");
            System.out.println(")");
            printAST(w.body, indent + "  ");
        } else if (node instanceof ForStmt) {
            ForStmt f = (ForStmt) node;
            System.out.print(indent + "for (");
            if (f.init != null) printAST(f.init, "");
            System.out.print("; ");
            if (f.condition != null) printAST(f.condition, "");
            System.out.print("; ");
            if (f.update != null) printAST(f.update, "");
            System.out.println(")");
            printAST(f.body, indent + "  ");
        } else if (node instanceof AssignStmt) {
            AssignStmt a = (AssignStmt) node;
            System.out.print(indent + "assign: ");
            printAST(a.left, "");
            System.out.print(" = ");
            printAST(a.right, "");
            System.out.println();
        } else if (node instanceof ExprStmt) {
            ExprStmt e = (ExprStmt) node;
            System.out.print(indent);
            printAST(e.expr, "");
            System.out.println();
        } else if (node instanceof ReturnStmt) {
            ReturnStmt r = (ReturnStmt) node;
            System.out.print(indent + "return ");
            printAST(r.value, "");
            System.out.println();
        } else if (node instanceof BinaryOp) {
            BinaryOp b = (BinaryOp) node;
            System.out.print("(");
            printAST(b.left, "");
            System.out.print(" " + b.op + " ");
            printAST(b.right, "");
            System.out.print(")");
        } else if (node instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) node;
            System.out.print(u.op);
            printAST(u.expr, "");
        } else if (node instanceof IntegerLiteral) {
            System.out.print(((IntegerLiteral) node).value);
        } else if (node instanceof BooleanLiteral) {
            System.out.print(((BooleanLiteral) node).value);
        } else if (node instanceof StringLiteral) {
            System.out.print("\"" + ((StringLiteral) node).value + "\"");
        } else if (node instanceof Identifier) {
            System.out.print(((Identifier) node).name);
        } else if (node instanceof CallExpr) {
            CallExpr c = (CallExpr) node;
            System.out.print(c.name + "(");
            for (int i = 0; i < c.args.size(); i++) {
                printAST(c.args.get(i), "");
                if (i < c.args.size()-1) System.out.print(", ");
            }
            System.out.print(")");
        } else {
            System.out.print("?");
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java SyntaxAnalyzer <source.cpp>");
            System.exit(1);
        }
        String source = readFile(args[0]);
        List<Token> tokens;
        try {
            tokens = tokenize(source);
        } catch (LexicalException e) {
            System.err.println("Лексическая ошибка: " + e.getMessage());
            return;
        }
        SyntaxAnalyzer parser = new SyntaxAnalyzer(tokens);
        Program prog = parser.parseProgram();
        if (parser.errors.isEmpty()) {
            System.out.println("Синтаксический анализ успешно завершён. AST:");
            parser.printAST(prog, "");
            System.out.println("\nОшибок не найдено.");
        } else {
            for (String err : parser.errors) System.err.println(err);
        }
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