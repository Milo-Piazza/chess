public class Position {
    public int x, y;
    public static final int boardSize = 8;
    public Position(int x, int y) {
        set(x, y);
    }

    public void set(int x, int y) {
        assert x >= 0 && x < boardSize;
        assert y >= 0 && y < boardSize;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Position) {
            Position opos = (Position) o;
            return this.x == opos.x && this.y == opos.y;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return y * boardSize + x;
    }
}
