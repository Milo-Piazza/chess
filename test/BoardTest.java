import org.junit.Test;

import static org.junit.Assert.*;

public class BoardTest {
    @Test
    public void foolsMate() {
        Board board = new Board();
        assertFalse(board.move(1, 0, 3, 1)); // knight cannot attack its own piece
        assertTrue(board.move(5, 1, 5, 2)); // pawn to f3
        assertFalse(board.move(0, 1, 0, 2)); // not white's turn
        assertTrue(board.move(4, 6, 4, 4)); // pawn to e5
        assertTrue(board.move(6, 1, 6, 3)); // pawn to g4
        assertTrue(board.move(3, 7, 7, 3)); // queen to h4
        assert board.getState() == GameState.BlackWin;
    }

    @Test
    public void castling() {
        Board board = new Board();
        assertTrue(board.move(4, 1, 4, 3)); // pawn to e4
        assertTrue(board.move(4, 6, 4, 4)); // pawn to e5
        assertFalse(board.move(4, 3, 4, 4)); // pawn cannot capture forward
        assertTrue(board.move(3, 1, 3, 3)); // pawn to d4
        assertTrue(board.move(4, 4, 3,3)); // pawn captures at d4
        assertFalse(board.move(4, 3, 5, 4)); // pawn cannot capture
        assertTrue(board.move(2, 0, 3, 1)); // bishop to d2
        assertFalse(board.move(7, 7, 7, 5)); // rook cannot move through pawn
        assertTrue(board.move(5, 7, 3, 5)); // bishop to c6
        assertTrue(board.move(3, 0,5, 2)); // queen to f3
        assertTrue(board.move(6, 7, 7, 5)); // knight to h6
        assertTrue(board.move(1, 0, 2, 2)); // knight to c3
        // Castling:
        assertTrue(board.move(4, 7 , 6, 7)); // castle kingside
        assertTrue(board.move(4, 0, 2, 0)); // castle queenside
        assertTrue(board.move(5, 7, 4, 7)); //rook to e8
        assertTrue(board.move(3, 0, 4, 0)); //rook to e1
        assertFalse(board.move(4, 7, 4, 0)); //cannot move through pawn
        assertTrue(board.move(4, 7, 4, 3)); // capture pawn
        System.out.println(board.boardAsString());
    }

    @Test
    public void enPassant() {

    }
}