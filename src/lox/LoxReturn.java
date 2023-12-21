package lox;

/**
 * @author Kevin Lee
 */
public class LoxReturn extends RuntimeException {

    public LoxReturn(Object value) {
        super(null, null, false, false);
        this.value = value;
    }

    public final Object value;

}