package lox;

/**
 * @author Kevin Lee
 */
public class Lox {

	public static void main(String[] args) {
		Debug.setDebug(true);

		var vm = new VM();
		var chunk = new Chunk();
		var constant = chunk.addConstant(1.2);

		chunk.writeByte(Opcode.OP_CONSTANT, 123);
		chunk.writeByte(constant, 123);

		constant = chunk.addConstant(3.4);
		chunk.writeByte(Opcode.OP_CONSTANT, 123);
		chunk.writeByte(constant, 123);

		chunk.writeByte(Opcode.OP_ADD, 123);

		constant = chunk.addConstant(5.6);
		chunk.writeByte(Opcode.OP_CONSTANT, 123);
		chunk.writeByte(constant, 123);

		chunk.writeByte(Opcode.OP_DIVIDE, 123);
		chunk.writeByte(Opcode.OP_NEGATE, 123);

		chunk.writeByte(Opcode.OP_RETURN, 123);

		Debug.disassembleChunk("test chunk", chunk);

		vm.interpret(chunk);
	}

}