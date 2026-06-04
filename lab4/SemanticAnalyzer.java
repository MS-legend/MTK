import java.util.*;

public class SemanticAnalyzer {
    private SymbolTable symTab;
    private TriplesGenerator triples;
    private String currentFunctionReturnType = null;
    private boolean insideFunction = false;

    public SemanticAnalyzer() {
        symTab = new SymbolTable();
        triples = new TriplesGenerator();
    }

    // Точка входа: анализ всей программы
    public void analyze(Program prog) throws SemanticException {
        // Глобальные объявления (функции и переменные)
        for (FunctionDecl func : prog.functions) {
            symTab.declare(func.name, func.returnType, 0);
            // Параметры и тело будут обработаны при посещении функции
        }
        // Обход функций
        for (FunctionDecl func : prog.functions) {
            visitFunction(func);
        }
    }

    private void visitFunction(FunctionDecl func) throws SemanticException {
        symTab.enterScope();
        currentFunctionReturnType = func.returnType;
        insideFunction = true;
        // Параметры
        for (VarDecl p : func.params) {
            symTab.declare(p.name, p.type, 0);
            symTab.setInitialized(p.name); // параметры инициализированы
        }
        // Тело
        visitCompoundStmt(func.body);
        insideFunction = false;
        currentFunctionReturnType = null;
        symTab.leaveScope();
    }

    private void visitCompoundStmt(CompoundStmt stmt) throws SemanticException {
        symTab.enterScope();
        for (Stmt s : stmt.statements) {
            visitStatement(s);
        }
        symTab.leaveScope();
    }

    private void visitStatement(Stmt s) throws SemanticException {
        if (s instanceof AssignStmt) visitAssignStmt((AssignStmt) s);
        else if (s instanceof IfStmt) visitIfStmt((IfStmt) s);
        else if (s instanceof WhileStmt) visitWhileStmt((WhileStmt) s);
        else if (s instanceof ForStmt) visitForStmt((ForStmt) s);
        else if (s instanceof ReturnStmt) visitReturnStmt((ReturnStmt) s);
        else if (s instanceof ExprStmt) visitExprStmt((ExprStmt) s);
        else if (s == null) {}
        else throw new SemanticException("Неизвестный оператор", 0);
    }

    private void visitAssignStmt(AssignStmt stmt) throws SemanticException {
        // Левая часть должна быть идентификатором (упрощённо)
        if (!(stmt.left instanceof Identifier))
            throw new SemanticException("Левая часть присваивания не идентификатор", 0);
        String varName = ((Identifier) stmt.left).name;
        Symbol var = symTab.lookup(varName);
        if (var == null)
            throw new SemanticException("Необъявленная переменная " + varName, 0);
        int rightTemp = visitExpression(stmt.right);
        // Проверка типа (упрощённо: тип выражения и переменной должны совпадать)
        String rightType = getExprType(stmt.right); // нужно хранить типы при обходе
        if (!var.type.equals(rightType))
            throw new SemanticException("Несоответствие типов: " + varName + " (" + var.type + ") и выражения (" + rightType + ")", 0);
        // Генерируем триаду присваивания
        triples.addTriple(":=", varName, rightTemp);
        symTab.setInitialized(varName);
    }

    // Обход выражения, возвращает номер триады с результатом
    private int visitExpression(Expr expr) throws SemanticException {
        if (expr instanceof IntegerLiteral) {
            int val = ((IntegerLiteral) expr).value;
            return triples.addTriple("const", val, null);
        } else if (expr instanceof Identifier) {
            String name = ((Identifier) expr).name;
            Symbol s = symTab.lookup(name);
            if (s == null) throw new SemanticException("Необъявленная переменная " + name, 0);
            if (!s.initialized) throw new SemanticException("Использование неинициализированной переменной " + name, 0);
            return triples.addTriple("load", name, null);
        } else if (expr instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) expr;
            int leftNum = visitExpression(bin.left);
            int rightNum = visitExpression(bin.right);
            return triples.addTriple(bin.op, leftNum, rightNum);
        } else if (expr instanceof UnaryOp) {
            UnaryOp un = (UnaryOp) expr;
            int operandNum = visitExpression(un.expr);
            return triples.addTriple(un.op, operandNum, null);
        } else if (expr instanceof CallExpr) {
            CallExpr call = (CallExpr) expr;
            List<Integer> argTemps = new ArrayList<>();
            for (Expr arg : call.args) argTemps.add(visitExpression(arg));
            // Проверка существования функции
            Symbol func = symTab.lookup(call.name);
            if (func == null) throw new SemanticException("Необъявленная функция " + call.name, 0);
            // Генерация триады вызова
            return triples.addTriple("call", call.name, argTemps);
        } else if (expr instanceof StringLiteral) {
            String s = ((StringLiteral) expr).value;
            return triples.addTriple("string", s, null);
        } else if (expr instanceof BooleanLiteral) {
            boolean b = ((BooleanLiteral) expr).value;
            return triples.addTriple("bool", b, null);
        }
        throw new SemanticException("Неподдерживаемое выражение", 0);
    }

    // Упрощённое определение типа выражения (в реальности нужно хранить тип при visitExpression)
    private String getExprType(Expr e) {
        if (e instanceof IntegerLiteral) return "int";
        if (e instanceof BooleanLiteral) return "bool";
        if (e instanceof StringLiteral) return "string";
        if (e instanceof Identifier) {
            Symbol s = symTab.lookup(((Identifier) e).name);
            return s == null ? "unknown" : s.type;
        }
        if (e instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) e;
            String leftType = getExprType(bin.left);
            String rightType = getExprType(bin.right);
            if (bin.op.equals("+") || bin.op.equals("-") || bin.op.equals("*") || bin.op.equals("/"))
                return "int";
            if (bin.op.equals("&&") || bin.op.equals("||") || bin.op.equals("<") || bin.op.equals(">") || bin.op.equals("=="))
                return "bool";
        }
        if (e instanceof CallExpr) {
            Symbol func = symTab.lookup(((CallExpr) e).name);
            return func == null ? "unknown" : func.type;
        }
        return "unknown";
    }

    private void visitIfStmt(IfStmt stmt) throws SemanticException {
        int condTemp = visitExpression(stmt.condition);
        String condType = getExprType(stmt.condition);
        if (!"bool".equals(condType))
            throw new SemanticException("Условие if должно быть bool, найдено " + condType, 0);
        int elseLabel = triples.newTemp(); // метка для else
        int endLabel = triples.newTemp();
        triples.addTriple("JZ", condTemp, elseLabel); // переход если false
        // then-ветка
        visitStatement(stmt.thenStmt);
        triples.addTriple("JMP", endLabel, null);
        triples.addTriple("LABEL", elseLabel, null);
        if (stmt.elseStmt != null) visitStatement(stmt.elseStmt);
        triples.addTriple("LABEL", endLabel, null);
    }

    private void visitWhileStmt(WhileStmt stmt) throws SemanticException {
        int startLabel = triples.newTemp();
        triples.addTriple("LABEL", startLabel, null);
        int condTemp = visitExpression(stmt.condition);
        String condType = getExprType(stmt.condition);
        if (!"bool".equals(condType))
            throw new SemanticException("Условие while должно быть bool", 0);
        int exitLabel = triples.newTemp();
        triples.addTriple("JZ", condTemp, exitLabel);
        visitStatement(stmt.body);
        triples.addTriple("JMP", startLabel, null);
        triples.addTriple("LABEL", exitLabel, null);
    }

    private void visitForStmt(ForStmt stmt) throws SemanticException {
        if (stmt.init != null) visitStatement(stmt.init);
        int startLabel = triples.newTemp();
        triples.addTriple("LABEL", startLabel, null);
        int condTemp = -1;
        if (stmt.condition != null) {
            condTemp = visitExpression(stmt.condition);
            String condType = getExprType(stmt.condition);
            if (!"bool".equals(condType)) throw new SemanticException("Условие for должно быть bool", 0);
            int exitLabel = triples.newTemp();
            triples.addTriple("JZ", condTemp, exitLabel);
            visitStatement(stmt.body);
            if (stmt.update != null) visitStatement(stmt.update);
            triples.addTriple("JMP", startLabel, null);
            triples.addTriple("LABEL", exitLabel, null);
        } else {
            visitStatement(stmt.body);
            if (stmt.update != null) visitStatement(stmt.update);
            triples.addTriple("JMP", startLabel, null);
        }
    }

    private void visitReturnStmt(ReturnStmt stmt) throws SemanticException {
        if (!insideFunction) throw new SemanticException("return вне функции", 0);
        if (stmt.value != null) {
            int valTemp = visitExpression(stmt.value);
            String valType = getExprType(stmt.value);
            if (!valType.equals(currentFunctionReturnType))
                throw new SemanticException("Тип возвращаемого значения " + valType + " не соответствует объявленному " + currentFunctionReturnType, 0);
            triples.addTriple("return", valTemp, null);
        } else {
            if (!"void".equals(currentFunctionReturnType))
                throw new SemanticException("Функция " + currentFunctionReturnType + " должна возвращать значение", 0);
            triples.addTriple("return", null, null);
        }
    }

    private void visitExprStmt(ExprStmt stmt) throws SemanticException {
        visitExpression(stmt.expr); // побочный эффект (например, вызов функции)
    }

    public void printSymbolTable() {
        System.out.println("Таблица символов (окончательная):");
        System.out.printf("%-12s %-8s %-12s%n", "Имя", "Тип", "Инициализирована");
        printScope(symTab.currentScope);
    }

    private void printScope(Scope scope) {
        if (scope.parent != null) printScope(scope.parent);
        for (Symbol s : scope.symbols.values()) {
            System.out.printf("%-12s %-8s %-12s%n", s.name, s.type, s.initialized ? "да" : "нет");
        }
    }

    public void printTriples() { triples.print(); }
}