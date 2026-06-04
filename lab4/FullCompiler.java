import java.io.*;
import java.util.*;

// ===================== ЛР1: Препроцессор =====================
class Preprocessor {
    public static String cleanCode(String source) {
        if (checkUnclosedComment(source)) {
            System.err.println("Ошибка: незакрытый многострочный комментарий");
            return null;
        }
        String noSingleLine = source.replaceAll("//.*$", "");
        String noComments = noSingleLine.replaceAll("/\\*.*?\\*/", "");
        String[] lines = noComments.split("\\r?\\n");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.replaceAll("^\\s+|\\s+$", "");
            String normalized = trimmed.replaceAll("\\s+", " ");
            if (!normalized.isEmpty()) result.append(normalized).append("\n");
        }
        return result.toString().replaceAll("\\n\\s*\\n", "\n");
    }
    private static boolean checkUnclosedComment(String source) {
        int open = 0, close = 0, idx = 0;
        while ((idx = source.indexOf("/*", idx)) != -1) { open++; idx += 2; }
        idx = 0;
        while ((idx = source.indexOf("*/", idx)) != -1) { close++; idx += 2; }
        return open != close;
    }
}

// ===================== ЛР2: Лексический анализатор =====================
class LexicalAnalyzer {
    static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "include", "int", "bool", "if", "else", "for", "while", "return", "true", "false"
    ));
    static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
        "+", "-", "*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!", "<<", ">>"
    ));
    static final Set<String> DELIMITERS = new HashSet<>(Arrays.asList(
        ";", ",", ".", ":", "{", "}", "(", ")", "#", "\""
    ));
    static class Token {
        String type, value;
        Token(String type, String value) { this.type = type; this.value = value; }
        public String toString() { return "(" + type + ", " + value + ")"; }
    }
    static class LexicalException extends Exception {
        LexicalException(String msg, int pos) { super(msg + " на позиции " + pos); }
    }
    static List<Token> tokenize(String source) throws LexicalException {
        List<Token> tokens = new ArrayList<>();
        int pos = 0, len = source.length();
        while (pos < len) {
            char ch = source.charAt(pos);
            if (Character.isWhitespace(ch)) { pos++; continue; }
            if (ch == '"') {
                int start = pos++;
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
}

// ===================== ЛР3: Синтаксический анализатор и AST =====================
interface ASTNode {}
class Program implements ASTNode { List<FunctionDecl> functions = new ArrayList<>(); }
class FunctionDecl implements ASTNode { String name; String returnType; List<VarDecl> params = new ArrayList<>(); CompoundStmt body; }
class VarDecl implements ASTNode { String name; String type; VarDecl(String name, String type) { this.name = name; this.type = type; } }
class CompoundStmt implements ASTNode { List<Stmt> statements = new ArrayList<>(); }
interface Stmt extends ASTNode {}
class IfStmt implements Stmt { Expr condition; Stmt thenStmt; Stmt elseStmt; }
class WhileStmt implements Stmt { Expr condition; Stmt body; }
class ForStmt implements Stmt { Stmt init; Expr condition; Stmt update; Stmt body; }
class AssignStmt implements Stmt { Expr left; Expr right; }
class ReturnStmt implements Stmt { Expr value; }
class ExprStmt implements Stmt { Expr expr; }
interface Expr extends ASTNode {}
class BinaryOp implements Expr { Expr left; Expr right; String op; }
class UnaryOp implements Expr { Expr expr; String op; }
class IntegerLiteral implements Expr { int value; IntegerLiteral(int v) { value = v; } }
class BooleanLiteral implements Expr { boolean value; BooleanLiteral(boolean v) { value = v; } }
class StringLiteral implements Expr { String value; StringLiteral(String v) { value = v; } }
class Identifier implements Expr { String name; Identifier(String n) { name = n; } }
class CallExpr implements Expr { String name; List<Expr> args = new ArrayList<>(); }

class SyntaxAnalyzer {
    private List<LexicalAnalyzer.Token> tokens;
    private int pos;
    List<String> errors = new ArrayList<>();
    SyntaxAnalyzer(List<LexicalAnalyzer.Token> tokens) { this.tokens = tokens; pos = 0; }
    private LexicalAnalyzer.Token peek() { return pos < tokens.size() ? tokens.get(pos) : null; }
    private LexicalAnalyzer.Token consume() { return tokens.get(pos++); }
    private boolean match(String type, String value) {
        LexicalAnalyzer.Token t = peek();
        if (t != null && t.type.equals(type) && t.value.equals(value)) { pos++; return true; }
        return false;
    }
    private boolean matchType(String type) {
        LexicalAnalyzer.Token t = peek();
        if (t != null && t.type.equals(type)) { pos++; return true; }
        return false;
    }
    private void error(String expected) {
        LexicalAnalyzer.Token t = peek();
        String found = (t == null) ? "конец файла" : t.type + "(" + t.value + ")";
        errors.add("Ошибка: ожидалось " + expected + ", найдено " + found);
    }
    Program parseProgram() {
        Program prog = new Program();
        while (peek() != null) {
            LexicalAnalyzer.Token t = peek();
            if (t.type.equals("KEYWORD") && (t.value.equals("int") || t.value.equals("bool"))) {
                prog.functions.add(parseFunction());
            } else if (t.type.equals("KEYWORD") && t.value.equals("include")) {
                consume();
                if (matchType("STRING")) {}
            } else {
                // Пропускаем неизвестные глобальные конструкции (например, using namespace)
                consume();
            }
        }
        return prog;
    }
    private FunctionDecl parseFunction() {
        String retType = consume().value;
        if (!matchType("IDENTIFIER")) error("имя функции");
        String name = tokens.get(pos-1).value;
        if (!match("DELIMITER", "(")) error("'('");
        FunctionDecl func = new FunctionDecl();
        func.name = name; func.returnType = retType;
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
            Stmt st = parseStatement();
            if (st != null) block.statements.add(st);
        }
        if (!match("DELIMITER", "}")) error("'}'");
        return block;
    }
    private Stmt parseStatement() {
        LexicalAnalyzer.Token t = peek();
        if (t == null) return null;
        if (t.type.equals("KEYWORD")) {
            switch (t.value) {
                case "if": return parseIfStmt();
                case "while": return parseWhileStmt();
                case "for": return parseForStmt();
                case "return": return parseReturnStmt();
                case "int": case "bool": return parseVarDeclStmt();
                default: error("оператор"); consume(); return null;
            }
        }
        Expr expr = parseExpression();
        if (match("OPERATOR", "=")) {
            Expr right = parseExpression();
            if (!match("DELIMITER", ";")) error("';'");
            AssignStmt as = new AssignStmt(); as.left = expr; as.right = right;
            return as;
        } else {
            if (match("DELIMITER", ";")) {
                ExprStmt es = new ExprStmt(); es.expr = expr;
                return es;
            } else if (match("OPERATOR", "<<")) {
                Expr right = parseExpression();
                if (!match("DELIMITER", ";")) error("';'");
                BinaryOp bin = new BinaryOp(); bin.left = expr; bin.right = right; bin.op = "<<";
                ExprStmt es = new ExprStmt(); es.expr = bin;
                return es;
            } else {
                error("';' или оператор");
                return null;
            }
        }
    }
    private IfStmt parseIfStmt() {
        consume();
        if (!match("DELIMITER", "(")) error("'('");
        IfStmt stmt = new IfStmt();
        stmt.condition = parseExpression();
        if (!match("DELIMITER", ")")) error("')'");
        stmt.thenStmt = parseStatement();
        if (match("KEYWORD", "else")) stmt.elseStmt = parseStatement();
        return stmt;
    }
    private WhileStmt parseWhileStmt() {
        consume();
        if (!match("DELIMITER", "(")) error("'('");
        WhileStmt stmt = new WhileStmt();
        stmt.condition = parseExpression();
        if (!match("DELIMITER", ")")) error("')'");
        stmt.body = parseStatement();
        return stmt;
    }
    private ForStmt parseForStmt() {
        consume();
        if (!match("DELIMITER", "(")) error("'('");
        ForStmt stmt = new ForStmt();
        if (!match("DELIMITER", ";")) {
            if (peek().type.equals("KEYWORD") && (peek().value.equals("int") || peek().value.equals("bool")))
                stmt.init = parseVarDeclStmt();
            else {
                Expr left = parseExpression();
                if (match("OPERATOR", "=")) {
                    Expr right = parseExpression();
                    AssignStmt as = new AssignStmt(); as.left = left; as.right = right;
                    stmt.init = as;
                } else error("инициализация");
            }
            if (!match("DELIMITER", ";")) error("';'");
        } else match("DELIMITER", ";");
        if (!match("DELIMITER", ";")) {
            stmt.condition = parseExpression();
            if (!match("DELIMITER", ";")) error("';'");
        } else match("DELIMITER", ";");
        if (!match("DELIMITER", ")")) {
            Expr upd = parseExpression();
            AssignStmt as = new AssignStmt(); as.left = upd;
            stmt.update = as;
            if (!match("DELIMITER", ")")) error("')'");
        } else match("DELIMITER", ")");
        stmt.body = parseStatement();
        return stmt;
    }
    private ReturnStmt parseReturnStmt() {
        consume();
        ReturnStmt stmt = new ReturnStmt();
        stmt.value = parseExpression();
        if (!match("DELIMITER", ";")) error("';'");
        return stmt;
    }
    private Stmt parseVarDeclStmt() {
        String type = consume().value;
        if (!matchType("IDENTIFIER")) error("имя переменной");
        String name = tokens.get(pos-1).value;
        Expr init = null;
        if (match("OPERATOR", "=")) init = parseExpression();
        if (!match("DELIMITER", ";")) error("';'");
        if (init != null) {
            AssignStmt as = new AssignStmt(); as.left = new Identifier(name); as.right = init;
            // Также возвращаем как оператор, но для таблицы символов важно, что переменная объявлена
            // Семантический анализатор сам добавит её при встрече AssignStmt или явном объявлении
            return as;
        } else {
            // Объявление без инициализации – просто игнорируем, но должны зарегистрировать переменную.
            // Для этого семантический анализатор должен обрабатывать такие узлы. Упростим: пусть возвращает специальный Stmt
            // или обработаем в семантике. Для простоты будем считать, что переменная объявлена и не инициализирована.
            // Создадим фиктивный AssignStmt, чтобы семантический анализатор зарегистрировал переменную.
            AssignStmt as = new AssignStmt(); as.left = new Identifier(name); as.right = null;
            return as;
        }
    }
    private Expr parseExpression() { return parseLogicalOr(); }
    private Expr parseLogicalOr() {
        Expr left = parseLogicalAnd();
        while (match("OPERATOR", "||")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseLogicalAnd();
            BinaryOp bin = new BinaryOp(); bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseLogicalAnd() {
        Expr left = parseEquality();
        while (match("OPERATOR", "&&")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseEquality();
            BinaryOp bin = new BinaryOp(); bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseEquality() {
        Expr left = parseRelational();
        while (match("OPERATOR", "==") || match("OPERATOR", "!=")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseRelational();
            BinaryOp bin = new BinaryOp(); bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseRelational() {
        Expr left = parseAdditive();
        while (match("OPERATOR", "<") || match("OPERATOR", ">") || match("OPERATOR", "<=") || match("OPERATOR", ">=")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseAdditive();
            BinaryOp bin = new BinaryOp(); bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseAdditive() {
        Expr left = parseMultiplicative();
        while (match("OPERATOR", "+") || match("OPERATOR", "-")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseMultiplicative();
            BinaryOp bin = new BinaryOp(); bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseMultiplicative() {
        Expr left = parseUnary();
        while (match("OPERATOR", "*") || match("OPERATOR", "/") || match("OPERATOR", "%")) {
            String op = tokens.get(pos-1).value;
            Expr right = parseUnary();
            BinaryOp bin = new BinaryOp(); bin.left = left; bin.right = right; bin.op = op;
            left = bin;
        }
        return left;
    }
    private Expr parseUnary() {
        if (match("OPERATOR", "!") || match("OPERATOR", "-")) {
            String op = tokens.get(pos-1).value;
            Expr expr = parseUnary();
            UnaryOp un = new UnaryOp(); un.expr = expr; un.op = op;
            return un;
        }
        return parsePrimary();
    }
    private Expr parsePrimary() {
        LexicalAnalyzer.Token t = peek();
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
                CallExpr call = new CallExpr(); call.name = name;
                if (!match("DELIMITER", ")")) {
                    do { call.args.add(parseExpression()); } while (match("DELIMITER", ","));
                    if (!match("DELIMITER", ")")) error("')'");
                }
                return call;
            } else return new Identifier(name);
        } else if (match("DELIMITER", "(")) {
            Expr inner = parseExpression();
            if (!match("DELIMITER", ")")) error("')'");
            return inner;
        } else {
            error("выражение");
            return null;
        }
    }
}

// ===================== ЛР4: Семантический анализатор, таблица символов, триады =====================
class Symbol {
    String name, type;
    boolean initialized;
    Symbol(String name, String type) { this.name = name; this.type = type; initialized = false; }
}
class Scope {
    Map<String, Symbol> symbols = new HashMap<>();
    Scope parent;
    Scope(Scope parent) { this.parent = parent; }
    void put(Symbol s) { symbols.put(s.name, s); }
    Symbol get(String name) {
        for (Scope sc = this; sc != null; sc = sc.parent)
            if (sc.symbols.containsKey(name)) return sc.symbols.get(name);
        return null;
    }
    boolean containsLocal(String name) { return symbols.containsKey(name); }
}
class SymbolTable {
    Scope currentScope;
    SymbolTable() { currentScope = new Scope(null); }
    void enterScope() { currentScope = new Scope(currentScope); }
    void leaveScope() { currentScope = currentScope.parent; }
    void declare(String name, String type) throws SemanticException {
        if (currentScope.containsLocal(name))
            throw new SemanticException("Повторное объявление переменной " + name, 0);
        currentScope.put(new Symbol(name, type));
    }
    Symbol lookup(String name) { return currentScope.get(name); }
    void setInitialized(String name) { Symbol s = lookup(name); if (s != null) s.initialized = true; }
    boolean isInitialized(String name) { Symbol s = lookup(name); return s != null && s.initialized; }
}
class SemanticException extends Exception {
    String msg; int line;
    SemanticException(String msg, int line) { super(msg); this.msg = msg; this.line = line; }
}
class Triple {
    String op; Object arg1, arg2;
    Triple(String op, Object a1, Object a2) { this.op = op; this.arg1 = a1; this.arg2 = a2; }
    public String toString() {
        return "(" + op + ", " + argToString(arg1) + ", " + argToString(arg2) + ")";
    }
    private String argToString(Object o) {
        if (o instanceof Integer) return "^" + o;
        return o == null ? "-" : o.toString();
    }
}
class TriplesGenerator {
    List<Triple> triples = new ArrayList<>();
    int newTemp() { return triples.size() + 1; }
    int addTriple(String op, Object a1, Object a2) { triples.add(new Triple(op, a1, a2)); return triples.size(); }
    void print() { for (int i=0; i<triples.size(); i++) System.out.printf("%d) %s%n", i+1, triples.get(i)); }
}
class SemanticAnalyzer {
    private SymbolTable symTab = new SymbolTable();
    private TriplesGenerator triples = new TriplesGenerator();
    private String currentReturnType = null;
    private boolean insideFunction = false;

    void analyze(Program prog) throws SemanticException {
        // Сначала регистрируем функции
        for (FunctionDecl f : prog.functions) {
            symTab.declare(f.name, f.returnType);
        }
        // Затем обрабатываем тела функций
        for (FunctionDecl f : prog.functions) {
            visitFunction(f);
        }
    }
    private void visitFunction(FunctionDecl f) throws SemanticException {
        symTab.enterScope();
        currentReturnType = f.returnType;
        insideFunction = true;
        // Параметры
        for (VarDecl p : f.params) {
            symTab.declare(p.name, p.type);
            symTab.setInitialized(p.name);
        }
        visitCompoundStmt(f.body);
        insideFunction = false;
        currentReturnType = null;
        symTab.leaveScope();
    }
    private void visitCompoundStmt(CompoundStmt c) throws SemanticException {
        symTab.enterScope();
        for (Stmt s : c.statements) visitStatement(s);
        symTab.leaveScope();
    }
    private void visitStatement(Stmt s) throws SemanticException {
        if (s == null) return;
        if (s instanceof AssignStmt) visitAssignStmt((AssignStmt)s);
        else if (s instanceof IfStmt) visitIfStmt((IfStmt)s);
        else if (s instanceof WhileStmt) visitWhileStmt((WhileStmt)s);
        else if (s instanceof ForStmt) visitForStmt((ForStmt)s);
        else if (s instanceof ReturnStmt) visitReturnStmt((ReturnStmt)s);
        else if (s instanceof ExprStmt) visitExprStmt((ExprStmt)s);
        else throw new SemanticException("Неизвестный оператор", 0);
    }
    private void visitAssignStmt(AssignStmt a) throws SemanticException {
        if (!(a.left instanceof Identifier))
            throw new SemanticException("Левая часть не идентификатор", 0);
        String var = ((Identifier)a.left).name;
        Symbol s = symTab.lookup(var);
        if (s == null) {
            // Если переменная не объявлена, попробуем угадать тип (по умолчанию int) – но лучше объявить заранее
            // Для простоты будем считать, что все переменные должны быть объявлены. Если нет – ошибка.
            throw new SemanticException("Необъявленная переменная " + var, 0);
        }
        int rightTemp = -1;
        String rightType = "int";
        if (a.right != null) {
            rightTemp = visitExpression(a.right);
            rightType = getExprType(a.right);
        } else {
            // Объявление без инициализации: ничего не делаем, переменная уже есть в таблице
            return;
        }
        if (!s.type.equals(rightType))
            throw new SemanticException("Несоответствие типов: " + var + " (" + s.type + ") != (" + rightType + ")", 0);
        triples.addTriple(":=", var, rightTemp);
        symTab.setInitialized(var);
    }
    private int visitExpression(Expr e) throws SemanticException {
        if (e instanceof IntegerLiteral) {
            int v = ((IntegerLiteral)e).value;
            return triples.addTriple("const", v, null);
        } else if (e instanceof Identifier) {
            String name = ((Identifier)e).name;
            Symbol s = symTab.lookup(name);
            if (s == null) throw new SemanticException("Необъявленная переменная " + name, 0);
            if (!s.initialized) throw new SemanticException("Использование неинициализированной переменной " + name, 0);
            return triples.addTriple("load", name, null);
        } else if (e instanceof BinaryOp) {
            BinaryOp b = (BinaryOp)e;
            int left = visitExpression(b.left);
            int right = visitExpression(b.right);
            return triples.addTriple(b.op, left, right);
        } else if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp)e;
            int operand = visitExpression(u.expr);
            return triples.addTriple(u.op, operand, null);
        } else if (e instanceof CallExpr) {
            CallExpr c = (CallExpr)e;
            List<Integer> args = new ArrayList<>();
            for (Expr arg : c.args) args.add(visitExpression(arg));
            Symbol func = symTab.lookup(c.name);
            if (func == null) throw new SemanticException("Необъявленная функция " + c.name, 0);
            return triples.addTriple("call", c.name, args);
        } else if (e instanceof StringLiteral) {
            String s = ((StringLiteral)e).value;
            return triples.addTriple("string", s, null);
        } else if (e instanceof BooleanLiteral) {
            boolean b = ((BooleanLiteral)e).value;
            return triples.addTriple("bool", b, null);
        }
        throw new SemanticException("Неподдерживаемое выражение", 0);
    }
    private String getExprType(Expr e) {
        if (e instanceof IntegerLiteral) return "int";
        if (e instanceof BooleanLiteral) return "bool";
        if (e instanceof StringLiteral) return "string";
        if (e instanceof Identifier) {
            Symbol s = symTab.lookup(((Identifier)e).name);
            return s == null ? "unknown" : s.type;
        }
        if (e instanceof BinaryOp) {
            BinaryOp b = (BinaryOp)e;
            if (b.op.equals("+")||b.op.equals("-")||b.op.equals("*")||b.op.equals("/")) return "int";
            return "bool";
        }
        if (e instanceof CallExpr) {
            Symbol s = symTab.lookup(((CallExpr)e).name);
            return s == null ? "unknown" : s.type;
        }
        return "unknown";
    }
    private void visitIfStmt(IfStmt i) throws SemanticException {
        int cond = visitExpression(i.condition);
        String t = getExprType(i.condition);
        if (!"bool".equals(t)) throw new SemanticException("Условие if должно быть bool", 0);
        int elseLabel = triples.newTemp();
        int endLabel = triples.newTemp();
        triples.addTriple("JZ", cond, elseLabel);
        visitStatement(i.thenStmt);
        triples.addTriple("JMP", endLabel, null);
        triples.addTriple("LABEL", elseLabel, null);
        if (i.elseStmt != null) visitStatement(i.elseStmt);
        triples.addTriple("LABEL", endLabel, null);
    }
    private void visitWhileStmt(WhileStmt w) throws SemanticException {
        int start = triples.newTemp();
        triples.addTriple("LABEL", start, null);
        int cond = visitExpression(w.condition);
        String t = getExprType(w.condition);
        if (!"bool".equals(t)) throw new SemanticException("Условие while должно быть bool", 0);
        int exit = triples.newTemp();
        triples.addTriple("JZ", cond, exit);
        visitStatement(w.body);
        triples.addTriple("JMP", start, null);
        triples.addTriple("LABEL", exit, null);
    }
    private void visitForStmt(ForStmt f) throws SemanticException {
        if (f.init != null) visitStatement(f.init);
        int start = triples.newTemp();
        triples.addTriple("LABEL", start, null);
        if (f.condition != null) {
            int cond = visitExpression(f.condition);
            String t = getExprType(f.condition);
            if (!"bool".equals(t)) throw new SemanticException("Условие for должно быть bool", 0);
            int exit = triples.newTemp();
            triples.addTriple("JZ", cond, exit);
            visitStatement(f.body);
            if (f.update != null) visitStatement(f.update);
            triples.addTriple("JMP", start, null);
            triples.addTriple("LABEL", exit, null);
        } else {
            visitStatement(f.body);
            if (f.update != null) visitStatement(f.update);
            triples.addTriple("JMP", start, null);
        }
    }
    private void visitReturnStmt(ReturnStmt r) throws SemanticException {
        if (!insideFunction) throw new SemanticException("return вне функции", 0);
        if (r.value != null) {
            int val = visitExpression(r.value);
            String t = getExprType(r.value);
            if (!t.equals(currentReturnType))
                throw new SemanticException("Несоответствие типа возврата: " + t + " vs " + currentReturnType, 0);
            triples.addTriple("return", val, null);
        } else {
            triples.addTriple("return", null, null);
        }
    }
    private void visitExprStmt(ExprStmt e) throws SemanticException {
        visitExpression(e.expr);
    }
    void printSymbolTable() {
        System.out.println("\n=== Таблица символов ===");
        System.out.printf("%-12s %-8s %-12s%n", "Имя", "Тип", "Инициализирована");
        printScope(symTab.currentScope);
    }
    private void printScope(Scope s) {
        if (s.parent != null) printScope(s.parent);
        for (Symbol sym : s.symbols.values())
            System.out.printf("%-12s %-8s %-12s%n", sym.name, sym.type, sym.initialized ? "да" : "нет");
    }
    void printTriples() { System.out.println("\n=== Триады ==="); triples.print(); }
}

// ===================== Главный компилятор =====================
public class FullCompiler {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java FullCompiler <source.cpp>");
            System.exit(1);
        }
        String source = readFile(args[0]);

        // 1. Препроцессинг
        String cleaned = Preprocessor.cleanCode(source);
        if (cleaned == null) { System.err.println("Ошибка препроцессора"); return; }

        // 2. Лексический анализ
        List<LexicalAnalyzer.Token> tokens;
        try {
            tokens = LexicalAnalyzer.tokenize(cleaned);
        } catch (LexicalAnalyzer.LexicalException e) {
            System.err.println("Лексическая ошибка: " + e.getMessage());
            return;
        }

        // 3. Синтаксический анализ
        SyntaxAnalyzer parser = new SyntaxAnalyzer(tokens);
        Program ast = parser.parseProgram();
        if (!parser.errors.isEmpty()) {
            for (String err : parser.errors) System.err.println(err);
            return;
        }

        // 4. Семантический анализ
        SemanticAnalyzer sem = new SemanticAnalyzer();
        try {
            sem.analyze(ast);
        } catch (SemanticException e) {
            System.err.println("Семантическая ошибка: " + e.getMessage());
            return;
        }

        System.out.println("Семантический анализ успешно завершён.");
        sem.printSymbolTable();
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