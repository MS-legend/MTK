import java.util.ArrayList;
import java.util.List;

class Triple {
    String op;
    Object arg1;
    Object arg2;
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
    int newTemp() { return triples.size() + 1; } // следующий номер триады
    int addTriple(String op, Object arg1, Object arg2) {
        triples.add(new Triple(op, arg1, arg2));
        return triples.size(); // номер (начиная с 1)
    }
    void print() {
        for (int i = 0; i < triples.size(); i++) {
            System.out.printf("%d) %s%n", i+1, triples.get(i));
        }
    }
}