import java.util.List;

public interface ASTNode {}

class Program implements ASTNode { List<FunctionDecl> functions; }
class FunctionDecl implements ASTNode { String name; String returnType; List<VarDecl> params; CompoundStmt body; }
class VarDecl implements ASTNode { String name; String type; }
class CompoundStmt implements ASTNode { List<Stmt> statements; }
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
class IntegerLiteral implements Expr { int value; }
class BooleanLiteral implements Expr { boolean value; }
class StringLiteral implements Expr { String value; }
class Identifier implements Expr { String name; }
class CallExpr implements Expr { String name; List<Expr> args; }