package lox;

/**
 * @author Kevin Lee
 */
class ReturnValue extends RuntimeException {

    public ReturnValue(Object value) {
        super(null, null, false, false);
        this.value = value;
    }

    public final Object value;

}