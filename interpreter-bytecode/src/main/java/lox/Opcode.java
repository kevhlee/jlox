package lox;

/**
 * @author Kevin Lee
 */
public final class Opcode {

	public static final int OP_CONSTANT = 0;
	public static final int OP_NEGATE = 1;
	public static final int OP_ADD = 2;
	public static final int OP_SUBTRACT = 3;
	public static final int OP_MULTIPLY = 4;
	public static final int OP_DIVIDE = 5;
	public static final int OP_RETURN = 6;

	private Opcode() {
		// Do not instantiate
	}

}