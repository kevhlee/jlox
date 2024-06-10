package lox;

/**
 * @author Kevin Lee
 */
public final class Debug {

	public static void disassembleChunk(String name, Chunk chunk) {
		if (!debug) {
			return;
		}

		System.out.printf("== %s ==%n", name);

		for (var offset = 0; offset < chunk.getCodeCount(); ) {
			offset = disassembleInstruction(chunk, offset);
		}
	}

	public static int disassembleInstruction(Chunk chunk, int offset) {
		if (!debug) {
			return offset;
		}

		System.out.printf("%04d ", offset);
		if (offset > 0 && chunk.getLine(offset) == chunk.getLine(offset - 1)) {
			System.out.printf("   | ");
		} else {
			System.out.printf("%4d ", chunk.getLine(offset));
		}

		var instruction = chunk.getByte(offset);
		switch (instruction) {
			case Opcode.OP_CONSTANT:
				return disassembleConstantInstruction("OP_CONSTANT", chunk, offset);
			case Opcode.OP_NEGATE:
				return disassembleSimpleInstruction("OP_NEGATE", offset);
			case Opcode.OP_ADD:
				return disassembleSimpleInstruction("OP_ADD", offset);
			case Opcode.OP_SUBTRACT:
				return disassembleSimpleInstruction("OP_SUBTRACT", offset);
			case Opcode.OP_MULTIPLY:
				return disassembleSimpleInstruction("OP_MULTIPLY", offset);
			case Opcode.OP_DIVIDE:
				return disassembleSimpleInstruction("OP_DIVIDE", offset);
			case Opcode.OP_RETURN:
				return disassembleSimpleInstruction("OP_RETURN", offset);
			default:
				System.out.printf("Unknown opcode %d%n", instruction);
				return offset + 1;
		}
	}

	protected static void printStack(double[] stack, int sp) {
		if (!debug) {
			return;
		}

		System.out.printf("          ");
		for (var i = 0; i < sp; i++) {
			System.out.printf("[ %g ]", stack[i]);
		}
		System.out.println();
	}

	public static void setDebug(boolean debug) {
		Debug.debug = debug;
	}

	private static int disassembleConstantInstruction(String name, Chunk chunk, int offset) {
		var constant = chunk.getByte(offset + 1);
		System.out.printf("%-16s %4d '%g'%n", name, constant, chunk.getConstant(constant));
		return offset + 2;
	}

	private static int disassembleSimpleInstruction(String name, int offset) {
		System.out.println(name);
		return offset + 1;
	}

	private static boolean debug;

	private Debug() {
		// Do not instantiate
	}

}