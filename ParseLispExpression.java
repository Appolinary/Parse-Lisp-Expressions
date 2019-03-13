/*
 * @author : Fabrice Appolinary
 * 
 * 
 * */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
You are given a string expression representing a Lisp-like expression to return the integer value of.

The syntax for these expressions is given as follows.

An expression is either an integer, a let-expression, an add-expression, a mult-expression, or an assigned variable. Expressions always evaluate to a single integer.
(An integer could be positive or negative.)
A let-expression takes the form (let v1 e1 v2 e2 ... vn en expr), where let is always the string "let", then there are 1 or more pairs of alternating variables and expressions, meaning that the first variable v1 is assigned the value of the expression e1, the second variable v2 is assigned the value of the expression e2, and so on sequentially; and then the value of this let-expression is the value of the expression expr.
An add-expression takes the form (add e1 e2) where add is always the string "add", there are always two expressions e1, e2, and this expression evaluates to the addition of the evaluation of e1 and the evaluation of e2.
A mult-expression takes the form (mult e1 e2) where mult is always the string "mult", there are always two expressions e1, e2, and this expression evaluates to the multiplication of the evaluation of e1 and the evaluation of e2.
For the purposes of this question, we will use a smaller subset of variable names. A variable starts with a lowercase letter, then zero or more lowercase letters or digits. Additionally for your convenience, the names "add", "let", or "mult" are protected and will never be used as variable names.
Finally, there is the concept of scope. When an expression of a variable name is evaluated, within the context of that evaluation, the innermost scope (in terms of parentheses) is checked first for the value of that variable, and then outer scopes are checked sequentially. It is guaranteed that every expression is legal. Please see the examples for more details on scope.
Evaluation Examples:
Input: (add 1 2)
Output: 3

Input: (mult 3 (add 2 3))
Output: 15

Input: (let x 2 (mult x 5))
Output: 10

Input: (let x 2 (mult x (let x 3 y 4 (add x y))))
Output: 14
Explanation: In the expression (add x y), when checking for the value of the variable x,
we check from the innermost scope to the outermost in the context of the variable we are trying to evaluate.
Since x = 3 is found first, the value of x is 3.

Input: (let x 3 x 2 x)
Output: 2
Explanation: Assignment in let statements is processed sequentially.

Input: (let x 1 y 2 x (add x y) (add x y))
Output: 5
Explanation: The first (add x y) evaluates as 3, and is assigned to x.
The second (add x y) evaluates as 3+2 = 5.

Input: (let x 2 (add (let x 3 (let x 4 x)) x))
Output: 6
Explanation: Even though (let x 4 x) has a deeper scope, it is outside the context
of the final x in the add-expression.  That final x will equal 2.

Input: (let a1 3 b2 (add a1 1) b2) 
Output 4
Explanation: Variable names can contain digits after the first character.

Note:

The given string expression is well formatted: There are no leading or trailing spaces, there is only a single space separating different components of the string, and no space between adjacent parentheses. The expression is guaranteed to be legal and evaluate to an integer.
The length of expression is at most 2000. (It is also non-empty, as that would not be a legal expression.)
The answer and all intermediate calculations of that answer are guaranteed to fit in a 32-bit integer.


Grammar:

EXP : INT | VARIABLE | ADD | MULT | LET
INT : number(integer)
VARIABLE : String
ADD : "(" "add" EXP EXP ")"
MULT : "(" "mult" EXP EXP ")"
LET : "(" "let" VAR_INIT EXP ")"
VAR_INIT : VARIABLE EXP
		  | EPSILON

*/

enum TOKEN_KIND {
	INT, VARIABLE, OPENBRACKET, CLOSEBRACKET, LET, ADD, MULT // these are keywords needed when parsing
}

enum EXPRESSION_KIND {
	INT_KIND, VARIABLE_KIND, ADD_KIND, MULT_KIND, LET_KIND // these are different types of expressions
}

class Token {
	String value;
	TOKEN_KIND kind;

	public Token(String value, TOKEN_KIND kind) {
		this.value = value;
		this.kind = kind;
	}

	public String toString() {
		return value;
	}
}

class EXPRESSION {
	EXPRESSION_KIND kind;

	// for binary expressions
	EXPRESSION right;
	EXPRESSION left;

	// for ints and vars
	int value;
	String variable_name;

	// for let
	Map<String, EXPRESSION> let_map = new HashMap<>();
	EXPRESSION to_return;

	EXPRESSION next;

	static EXPRESSION makeAddExpression(EXPRESSION left, EXPRESSION right) {
		EXPRESSION exp = new EXPRESSION();
		exp.kind = EXPRESSION_KIND.ADD_KIND;
		exp.left = left;
		exp.right = right;
		return exp;
	}

	static EXPRESSION makeMultExpression(EXPRESSION left, EXPRESSION right) {
		EXPRESSION exp = new EXPRESSION();
		exp.kind = EXPRESSION_KIND.MULT_KIND;
		exp.left = left;
		exp.right = right;
		return exp;
	}

	static EXPRESSION makeLetExpression() {
		EXPRESSION exp = new EXPRESSION();
		exp.kind = EXPRESSION_KIND.LET_KIND;
		return exp;
	}

	static void addLetVaribleInitialisation(EXPRESSION exp, String variable, EXPRESSION value) {
		exp.let_map.put(variable, value);
	}

	static void addLetFinalExpression(EXPRESSION exp, EXPRESSION finalExp) {
		exp.to_return = finalExp;
	}

	static EXPRESSION makeIntExpression(String integer) {
		EXPRESSION exp = new EXPRESSION();
		exp.kind = EXPRESSION_KIND.INT_KIND;
		exp.value = Integer.parseInt(integer);
		return exp;
	}

	static EXPRESSION makeVariableExpression(String var) {
		EXPRESSION exp = new EXPRESSION();
		exp.kind = EXPRESSION_KIND.VARIABLE_KIND;
		exp.variable_name = var;
		return exp;
	}

	static Integer evaluate(EXPRESSION exp, Scope scope) {

		switch (exp.kind) {
		case INT_KIND:
			return exp.value;
		case ADD_KIND:
			// need to go down in a new scope
			scope.add(exp);
			Scope newScope = scope.newScope(scope);
			return evaluate(exp.left, newScope) + evaluate(exp.right, newScope);
		case MULT_KIND: {
			scope.add(exp);
			Scope newScope1 = scope.newScope(scope);
			return evaluate(exp.left, newScope1) * evaluate(exp.right, newScope1);
		}
		case LET_KIND:
			scope.add(exp);
			Scope newS = scope.newScope(scope);
			return evaluate(exp.to_return, newS);
		case VARIABLE_KIND:
			// need to look up the scope variables
			// first check the current scope if its a let
			Scope temp = scope.parent;
			while (temp != null) {
				for (EXPRESSION e : temp.expressions) {
					if (e.kind == EXPRESSION_KIND.LET_KIND) {
						EXPRESSION value = e.let_map.get(exp.variable_name);
						if (value != null) {
							return evaluate(value, temp);
						}
					}
				}
				temp = temp.parent;
			}
			return null;
		default:
			break;
		}

		return null;
	}
}

class Scope {
	Scope parent;

	List<EXPRESSION> expressions;

	public Scope() {
		expressions = new ArrayList<>();
	}

	public void add(EXPRESSION e) {
		expressions.add(e);
	}

	public Scope newScope(Scope p) {
		Scope scope = new Scope();
		scope.parent = p;
		return scope;
	}
}

public class ParseLispExpression {

	Scope scope;

	int nextIndex = 0;
	Token[] tokens;

	public Token[] parseString(String input) {

		// first replace "(" by " ( ";
		String input1 = input.replaceAll("\\(", "( ");
		String input2 = input1.replaceAll("\\)", " ) ");

		String string_tokens[] = input2.split("\\s+");
		Token tokens[] = new Token[string_tokens.length];

		for (int i = 0; i < string_tokens.length; i++) {
			tokens[i] = stringToToken(string_tokens[i]);
		}

		return tokens;
	}

	public Token stringToToken(String str) {

		Token token = null;

		if (str.equals("let")) {
			token = new Token("let", TOKEN_KIND.LET);
		} else if (str.equals("add")) {
			token = new Token("add", TOKEN_KIND.ADD);
		} else if (str.equals("mult")) {
			token = new Token("mult", TOKEN_KIND.MULT);
		} else if (str.equals("(")) {
			token = new Token("(", TOKEN_KIND.OPENBRACKET);
		} else if (str.equals(")")) {
			token = new Token(")", TOKEN_KIND.CLOSEBRACKET);
		}

		if (token != null) {
			return token;
		}

		try {
			Integer.parseInt(str);
			token = new Token(str, TOKEN_KIND.INT);
		} catch (Exception e) {
			token = new Token(str, TOKEN_KIND.VARIABLE);
		}

		return token;
	}

	public int calculate(String input) {
		EXPRESSION exp = setInput(input);
		scope = new Scope();
		return EXPRESSION.evaluate(exp, scope);
	}

	private EXPRESSION setInput(String input) {
		tokens = parseString(input);

		return EXP();
	}

	private EXPRESSION EXP() {

		// store the current pointer first
		int temp = nextIndex;

		// check whether it is anything
		EXPRESSION exp = INT();
		if (exp != null) {
			return exp;
		}

		nextIndex = temp;
		exp = VARIABLE();
		if (exp != null) {
			return exp;
		}

		nextIndex = temp;
		exp = ADD();
		if (exp != null) {
			return exp;
		}

		nextIndex = temp;
		exp = MULT();
		if (exp != null) {
			return exp;
		}

		nextIndex = temp;
		exp = LET();
		if (exp != null) {
			return exp;
		}

		nextIndex = temp;
		return null;
	}

	private EXPRESSION MULT() {
		int temp = nextIndex;

		if (!OPENBRACKET())
			return null;

		nextIndex++;

		if (!MULT_TOKEN())
			return null;

		nextIndex++;

		EXPRESSION exp1 = EXP();
		if (exp1 == null) {
			nextIndex = temp;
			return null;
		}

		nextIndex++;

		EXPRESSION exp2 = EXP();
		if (exp2 == null) {
			nextIndex = temp;
			return null;
		}

		nextIndex++;
		if (!CLOSEBRACKET()) {
			nextIndex = temp;
			return null;
		}

		return EXPRESSION.makeMultExpression(exp1, exp2);
	}

	private EXPRESSION LET() {

		int temp = nextIndex;

		if (!OPENBRACKET())
			return null;

		nextIndex++;

		if (!LET_TOKEN()) {
			nextIndex = temp;
			return null;
		}

		nextIndex++;

		EXPRESSION variable = VARIABLE();
		nextIndex++;
		EXPRESSION exp = EXP();

		EXPRESSION expression = EXPRESSION.makeLetExpression();

		while (variable != null && exp != null) {
			EXPRESSION.addLetVaribleInitialisation(expression, variable.variable_name, exp);
			// move right by two
			nextIndex++;
			variable = VARIABLE();

			if (variable == null) {
				break;
			}

			nextIndex++;
			exp = EXP();

			if (exp == null) {
				nextIndex--;
				break;
			}
		}

		// now get the last final expression of this as the final
		exp = EXP();
		if (exp == null) {
			nextIndex = temp;
			return null;
		}
		EXPRESSION.addLetFinalExpression(expression, exp);

		nextIndex++;
		if (!CLOSEBRACKET()) {
			nextIndex = temp;
			return null;
		}

		return expression;
	}

	private EXPRESSION ADD() {

		int temp = nextIndex;

		if (!OPENBRACKET())
			return null;

		nextIndex++;

		if (!ADD_TOKEN())
			return null;

		nextIndex++;

		EXPRESSION exp1 = EXP();
		if (exp1 == null) {
			nextIndex = temp;
			return null;
		}

		nextIndex++;

		EXPRESSION exp2 = EXP();
		if (exp2 == null) {
			nextIndex = temp;
			return null;
		}

		nextIndex++;
		if (!CLOSEBRACKET()) {
			nextIndex = temp;
			return null;
		}

		return EXPRESSION.makeAddExpression(exp1, exp2);
	}

	private EXPRESSION VARIABLE() {
		if (tokens[nextIndex].kind == TOKEN_KIND.VARIABLE) {
			return EXPRESSION.makeVariableExpression(tokens[nextIndex].value);
		}
		return null;
	}

	private EXPRESSION INT() {

		if (tokens[nextIndex].kind == TOKEN_KIND.INT) {
			return EXPRESSION.makeIntExpression(tokens[nextIndex].toString());
		}

		return null;
	}

	private boolean OPENBRACKET() {
		if (tokens[nextIndex].kind == TOKEN_KIND.OPENBRACKET) {
			return true;
		}

		return false;
	}

	private boolean CLOSEBRACKET() {
		if (tokens[nextIndex].kind == TOKEN_KIND.CLOSEBRACKET) {
			return true;
		}
		return false;
	}

	private boolean LET_TOKEN() {
		if (tokens[nextIndex].kind == TOKEN_KIND.LET) {
			return true;
		}
		return false;
	}

	private boolean ADD_TOKEN() {
		if (tokens[nextIndex].kind == TOKEN_KIND.ADD) {
			return true;
		}
		return false;
	}

	private boolean MULT_TOKEN() {
		if (tokens[nextIndex].kind == TOKEN_KIND.MULT) {
			return true;
		}
		return false;
	}

	public static void main(String args[]) {

		ParseLispExpression p = new ParseLispExpression();
		String string = "(let a1 3 b2 (add a1 1) b2)";
		Integer i = p.calculate(string);

		System.out.println(i);
	}
}
