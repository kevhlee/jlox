package lox;

import java.util.function.BinaryOperator;

/**
 * @author Kevin Lee
 */
public class VM {

	public InterpretResult interpret(Chunk chunk) {
		this.ip = 0;
		this.chunk = chunk;
		return run();
	}

	public static final int STACK_MAX = 256;

	private InterpretResult run() {
		while (true) {
			Debug.printStack(stack, sp);
			Debug.disassembleInstruction(chunk, ip);

			var instruction = readByte();

			switch (instruction) {
				case Opcode.OP_CONSTANT:
					pushConstant(readConstant());
					break;
				case Opcode.OP_NEGATE:
					pushConstant(-popConstant());
					break;
				case Opcode.OP_ADD:
					pushBinaryOp(BINARY_OP_ADD);
					break;
				case Opcode.OP_SUBTRACT:
					pushBinaryOp(BINARY_OP_SUBTRACT);
					break;
				case Opcode.OP_MULTIPLY:
					pushBinaryOp(BINARY_OP_MULTIPLY);
					break;
				case Opcode.OP_DIVIDE:
					pushBinaryOp(BINARY_OP_DIVIDE);
					break;
				case Opcode.OP_RETURN:
					System.out.printf("%g%n", popConstant());
					return InterpretResult.OK;
			}
		}
	}

	private void pushBinaryOp(BinaryOperator<Double> operator) {
		var right = popConstant();
		var left = popConstant();
		pushConstant(operator.apply(left, right));
	}

	private double popConstant() {
		return stack[--sp];
	}

	private void pushConstant(double value) {
		stack[sp++] = value;
	}

	private int readByte() {
		return chunk.getByte(ip++);
	}

	private double readConstant() {
		return chunk.getConstant(readByte());
	}

	private static final BinaryOperator<Double> BINARY_OP_ADD = (a, b) -> a + b;
	private static final BinaryOperator<Double> BINARY_OP_SUBTRACT = (a, b) -> a - b;
	private static final BinaryOperator<Double> BINARY_OP_MULTIPLY = (a, b) -> a * b;
	private static final BinaryOperator<Double> BINARY_OP_DIVIDE = (a, b) -> a / b;

	private int ip;
	private int sp;
	private Chunk chunk;
	private final double[] stack = new double[STACK_MAX];

}