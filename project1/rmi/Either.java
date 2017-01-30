package rmi;

/**
 * Created by musteryu on 2017/1/28.
 */
class Either<T, E> {
    private T l;
    private E r;

    Either(T l, E r) {
        this.l = l;
        this.r = r;
    }

    static <T, E> Either<T, E> left(T obj) {
        return new Either<>(obj, null);
    }

    static <T, E> Either<T, E> right(E e) {
        return new Either<>(null, e);
    }

    boolean isLeft() {
        return this.l != null;
    }

    boolean isRight() {
        return this.r != null;
    }

    T getLeft() {
        return l;
    }

    E getRight() {
        return r;
    }

    @Override
    public String toString() {
        if (isLeft()) return "Left < " + l + " >";
        else return "Right < " + r + " >";
    }
}
