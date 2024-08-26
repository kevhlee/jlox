package lox;

import java.util.Arrays;

/**
 * @author Kevin Lee
 */
public class Chunk {

	public int addConstant(double value) {
		if (constantCount == constants.length) {
			constants = Arrays.copyOf(constants, constantCount * 2);
		}
		constants[constantCount++] = value;
		return constantCount - 1;
	}

	public int getByte(int index) throws IndexOutOfBoundsException {
		if (index < 0 || index >= codeCount) {
			throw new IndexOutOfBoundsException(index);
		}
		return code[index];
	}

	public double getConstant(int index) throws IndexOutOfBoundsException {
		if (index < 0 || index >= constantCount) {
			throw new IndexOutOfBoundsException(index);
		}
		return constants[index];
	}

	public int getCodeCount() {
		return codeCount;
	}

	public int getConstantCount() {
		return constantCount;
	}

	public int getLine(int index) throws IndexOutOfBoundsException {
		if (index < 0 || index >= codeCount) {
			throw new IndexOutOfBoundsException(index);
		}
		return lines[index];
	}

	public void writeByte(int b, int line) {
		if (codeCount == code.length) {
			code = Arrays.copyOf(code, codeCount * 2);
			lines = Arrays.copyOf(code, codeCount * 2);
		}

		code[codeCount] = b;
		lines[codeCount] = line;
		codeCount++;
	}

	private int codeCount;
	private int constantCount;
	private int[] code = new int[8];
	private int[] lines = new int[8];
	private double[] constants = new double[8];

}