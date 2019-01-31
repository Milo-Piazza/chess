import javafx.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Board {

    private final int boardSize = 8;

    public Piece[] backrow = {Piece.Rook, Piece.Knight, Piece.Bishop, Piece.Queen, Piece.King, Piece.Bishop, Piece.Knight, Piece.Rook};
    public enum Castle {Queenside, Kingside};

    private Color currentPlayer;
    private Color opponent; // represents the player whose turn it isn't.
    private Map<Color, Map<Position, Piece>> pieces;
    private Map<Color, Position> kings;
    private Position legalEnPassant = null; // Stores the location of the square that can be attacked with en passant this turn.
    private Map<Color, Map<Castle, Boolean>> canCastle;

    // Front-end interaction
    private ChessFrontEnd frontEnd;
    private GameState state;
    private Set<Pair<Position, Position>> movesThisTurn;

    // Potential move state
    private Position oldPotentialPos;
    private Position newPotentialPos;
    private Color colorMoved;
    private Piece pieceMoved;
    private Piece pieceCaptured;
    private Position capturePos;

    public Board() {
        pieces = new HashMap<Color, Map<Position, Piece>>();
        kings = new HashMap<Color, Position>();
        pieces.put(Color.White, new HashMap<Position, Piece>());
        pieces.put(Color.Black, new HashMap<Position, Piece>());
        this.reset();
    }

    /**
     * Reset the board to its state at the start of a new game.
     */
    public void reset() {
        currentPlayer = Color.White;
        opponent = Color.Black;
        Map<Position, Piece> whitePieces = getPieces(Color.White);
        Map<Position, Piece> blackPieces = getPieces(Color.Black);
        whitePieces.clear();
        blackPieces.clear();
        kings.clear();
        for (int x = 0; x < boardSize; x++) {
            addPiece(Color.White, new Position(x, 1), Piece.Pawn);
            addPiece(Color.Black, new Position(x, boardSize - 2), Piece.Pawn);
            addPiece(Color.White, new Position(x, 0), backrow[x]);
            addPiece(Color.Black, new Position(x, boardSize - 1), backrow[x]);
            if (backrow[x] == Piece.King) {
                kings.put(Color.White, new Position(x, 0));
                kings.put(Color.Black, new Position(x, boardSize - 1));
            }
        }
        canCastle = new HashMap<Color, Map<Castle, Boolean>>();
        canCastle.put(Color.White, new HashMap<Castle, Boolean>());
        canCastle.put(Color.Black, new HashMap<Castle, Boolean>());
        canCastle.get(Color.White).put(Castle.Queenside, true);
        canCastle.get(Color.White).put(Castle.Kingside, true);
        canCastle.get(Color.Black).put(Castle.Queenside, true);
        canCastle.get(Color.Black).put(Castle.Kingside, true);
        state = GameState.Ongoing;
    }

    /**
     * Given an initial position and a final position, test to see if a move is legal.
     * If it is, make the move, update the game state and return true. Otherwise return false.
     * @param pos: initial position
     * @param newPos: final position
     * @return whether the move was made (is legal) or not
     */
    //TODO: Make this return a set of altered pieces instead of a boolean
    public boolean move(Position pos, Position newPos) {
        // A player cannot move unless it is their turn
        if (!pieceExists(currentPlayer, pos)) {
            return false;
        }
        if (inMoveRange(currentPlayer, pos, newPos)) {
            Piece movedPiece = getPiece(currentPlayer, pos);
            //Piece movedPiece = pieces.get(currentPlayer).get(pos);
            makePotentialMove(currentPlayer, pos, newPos);
            if (isKingAttacked(currentPlayer)) {
                // A player may not make a move that endangers their king.
                revertPotentialMove();
                return false;
            }
            // The move is legal since it follows moving rules and does not expose the king.
            // We hence "lock in" the move.
            // Update conditions for special rules:
            // En passant:
            // If a pawn was double moved:
            if (movedPiece == Piece.Pawn && Math.abs(newPos.y - pos.y) == 2) {
                legalEnPassant = new Position(newPos.x, newPos.y + (currentPlayer == Color.White ? 1 : -1));
            } else {
                legalEnPassant = null;
            }
            // Pawn promotion
            if (canPromote(currentPlayer, newPos)) {
                Piece promotedPiece = frontEnd.askPromotion();
                removePiece(currentPlayer, newPos);
                addPiece(currentPlayer, newPos, promotedPiece);
            }
            // Castling:
            if (movedPiece == Piece.King) {
                disableCastle(currentPlayer, Castle.Queenside);
                disableCastle(currentPlayer, Castle.Kingside);
            }
            if (movedPiece == Piece.Rook) {
                if (pos.x == 0) {
                    disableCastle(currentPlayer, Castle.Queenside);
                } else if (pos.x == boardSize - 1) {
                    disableCastle(currentPlayer, Castle.Kingside);
                }
            }
            // Check for end of the game:
            // We need to copy the opponent's pieces into a new iterator.
            // keySet returns a view into the map, and will cause an error if a key is altered.
            Set<Position> positionsToCopy = getPieces(opponent).keySet();
            Set<Position> opponentPieces = new HashSet<Position>(positionsToCopy);
            for (Position checkPos: opponentPieces) {
                Set<Position> moves = possibleMoves(opponent, checkPos);
                for (Position checkNewPos : moves) {
                    makePotentialMove(opponent, checkPos, checkNewPos);
                    if (!isKingAttacked(opponent)) {
                        revertPotentialMove();
                        // The opponent can make a legal move without their king being attacked, so the game is not over.
                        // Switch the turn player:
                        currentPlayer = getOpponent(currentPlayer);
                        opponent = getOpponent(opponent);
                        return true;
                    }
                    revertPotentialMove();
                }
            }
            // The game is over: Checkmate or draw, depending on if the opponent's king is currently attacked.
            state = !isKingAttacked(opponent) ? GameState.Draw : (currentPlayer == Color.White ? GameState.WhiteWin : GameState.BlackWin);
            return true;
        }
        return false;
    }

    public boolean move(int x, int y, int nx, int ny) {
        return move(new Position(x, y), new Position(nx, ny));
    }

    /**
     * Make a legal move and prepare state so that the move is reverted by calling revertPotentialMove().
     * @param player: the player making the move
     * @param pos: the position of the piece to move
     * @param newPos: the position to move the piece to
     */
    private void makePotentialMove(Color player, Position pos, Position newPos) {
        if (!inMoveRange(player, pos, newPos)) {
            return;
        }
        colorMoved = player;
        pieceMoved = getPiece(player, pos);
        if (pieceMoved == Piece.Pawn && newPos.equals(legalEnPassant)) {
            capturePos = legalEnPassant;
            pieceCaptured = getPiece(getOpponent(player), legalEnPassant);
            removePiece(getOpponent(player), legalEnPassant);
        } else if (pieceExists(getOpponent(player), newPos)) {
            capturePos = newPos;
            pieceCaptured = getPiece(getOpponent(player), newPos);
            removePiece(getOpponent(player), newPos);
        } else {
            capturePos = null;
            pieceCaptured = null;
        }
        // Castling special case: Need to move the corresponding rook.
        // Note that the move was already confirmed legal, and the king was already confirmed not to be attacked.
        // Hence we will never need to revert this move.
        if (pieceMoved == Piece.King) {
            // Castling kingside:
            // Move the right hand rook one space to the left of the king.
            if (newPos.x - pos.x == 2) {
                movePiece(player, new Position(boardSize - 1, pos.y), new Position(newPos.x - 1, pos.y));
            // Castling queenside:
            // Move the left hand rook one space to the right of the king.
            } else if (newPos.x - pos.x == -2) {
                movePiece(player, new Position(0, pos.y), new Position(newPos.x + 1, pos.y));
            }
        }
        oldPotentialPos = pos;
        newPotentialPos = newPos;
        movePiece(player, pos, newPos);
    }

    /**
     * Revert the most recent move made by makePotentialMove().
     * The board and game state will return to how it was before the most recent call of makePotentialMove().
     */
    private void revertPotentialMove() {
        // Revert move
        removePiece(colorMoved, newPotentialPos);
        addPiece(colorMoved, oldPotentialPos, pieceMoved);
        // Revert the capture if there was one
        if (pieceCaptured != null) {
            addPiece(getOpponent(colorMoved), capturePos, pieceCaptured);
        }
    }

    private Color getOpponent(Color player) {
        return player == Color.White ? Color.Black : Color.White;
    }

    private int signum(int value) {
        if (value > 0) {
            return 1;
        } else if (value < 0) {
            return -1;
        }
        return 0;
    }

    /**
     * Determine whether the given player's king is attacked by an opposing piece.
     * @param player: The player who the king belongs to
     * @return whether the turn player's king is currently attacked
     */
    private boolean isKingAttacked(Color player) {
        Position king = kings.get(player);
        return isAttacked(player, king);
    }

    //FIXME: isAttacked counts a forward pawn move as an attack
    private boolean isAttacked(Color player, Position pos) {
        Color opp = getOpponent(player);
        Map<Position, Piece> opponentPieces = getPieces(opp);
        for (Position oppPosition : opponentPieces.keySet()) {
            if (inMoveRange(opp, oppPosition, pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether a particular move follows the basic rules of moving.
     * DOES NOT determine whether the move exposes the king.
     * @param player: the player who the piece belongs to
     * @param pos: the position of the piece to be moved
     * @param newPos: the position to move the piece to
     * @return whether the move is possible.
     */
    private boolean inMoveRange(Color player, Position pos, Position newPos) {
        if (!pieceExists(player, pos) || pos.equals(newPos) || !canStopAt(player, newPos)) {
            return false;
        }
        Piece pieceToMove = getPiece(player, pos);
        int initialY;
        int dx = newPos.x - pos.x;
        int dy = newPos.y - pos.y;
        switch (pieceToMove) {
            case Pawn:
                int moveDirection = player == Color.White ? 1 : -1; // the direction a pawn can move: 1 is forward (white), -1 is backward (black)
                initialY = player == Color.White ? 1 : boardSize - 2; // the initial y-position of a pawn
                if (dy == moveDirection) {
                    if (dx == 0) {
                        return canPassThrough(player, newPos);
                    } else if (Math.abs(dx) == 1) {
                        // A pawn may only move diagonally by attacking or by en passant.
                        return pieceExists(getOpponent(player), newPos) || newPos.equals(legalEnPassant);
                    }
                } else if (dy == 2 * moveDirection && Math.abs(dx) == 0) {
                    // A pawn can double move only if it is in its initial position.
                    return pos.y == initialY &&
                            canPassThrough(player, new Position(pos.x, pos.y + moveDirection)) &&
                            canPassThrough(player, newPos);
                }
                return false;
            case Knight:
                return ((Math.abs(dx) == 1 && Math.abs(dy) == 2) || (Math.abs(dx) == 2 && Math.abs(dy) == 1));
            case Bishop:
                return canMoveDiagonally(player, pos, newPos);
            case Rook:
                return canMoveStraight(player, pos, newPos);
            case Queen:
                return canMoveDiagonally(player, pos, newPos) || canMoveStraight(player, pos, newPos);
            case King:
                initialY = player == Color.White ? 0 : boardSize - 1; //Initial y-position of the player's king
                // Castling:
                // In order for a player to castle, the following must hold:
                // The king and rook involved in castling must not have moved (1)
                // There may be no pieces in between the king and rook (2)
                // The king may not be in check, or be attacked on the way to its new position (3)
                if (Math.abs(dx) == 2 && Math.abs(dy) == 0) {
                    if (dx == -2) { // Castle queenside
                        return canCastle(player, Castle.Queenside) && // (1)
                                canMoveStraight(player, pos, new Position(1, initialY)) && // (2)
                                !isKingAttacked(player) && // (3)
                                !isAttacked(player, new Position(pos.x - 1, initialY)) && // (3)
                                !isAttacked(player, new Position(pos.x - 2, initialY)); // (3)
                    } else if (dx == 2) { // Castle kingside
                        return canCastle(player, Castle.Kingside) && // (1)
                                canMoveStraight(player, pos, new Position(boardSize - 2, initialY)) && // (2)
                                !isKingAttacked(player) && // (3)
                                !isAttacked(player, new Position(pos.x + 1, initialY)) && // (3)
                                !isAttacked(player, new Position(pos.x + 2, initialY)); // (3)
                    }
                }
                return ((Math.abs(dx) <= 1) && (Math.abs(dy) <= 1));
        }
        return false;
    }

    private boolean canMoveDiagonally(Color player, Position pos, Position newPos) {
        int dx = newPos.x - pos.x;
        int dy = newPos.y - pos.y;
        return Math.abs(dx) == Math.abs(dy) && canGoTo(player, pos, newPos);
    }

    private boolean canMoveStraight(Color player, Position pos, Position newPos) {
        int dx = newPos.x - pos.x;
        int dy = newPos.y - pos.y;
        return (Math.abs(dx) == 0 || Math.abs(dy) == 0) && canGoTo(player, pos, newPos);
    }

    private boolean canGoTo(Color player, Position pos, Position newPos) {
        int dirX = signum(newPos.x - pos.x);
        int dirY = signum(newPos.y - pos.y);
        Position currentPos = new Position(pos.x + dirX, pos.y + dirY);
        while (!currentPos.equals(newPos)) {
            if (!canPassThrough(player, currentPos)) {
                return false;
            }
            currentPos.set(currentPos.x + dirX, currentPos.y + dirY);
        }
        return canStopAt(player, newPos);
    }

    /**
     * Find the set of all possible moves for a certain piece.
     * @param pos: the position of the piece to be moved
     * @return the set of all legal moves
     */
    private Set<Position> possibleMoves(Color player, Position pos) {
        Set<Position> moves = new HashSet<Position>();
        Position newPos;
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                newPos = new Position(x, y);
                if (inMoveRange(player, pos, newPos)) {
                    moves.add(newPos);
                }
            }
        }
        return moves;
    }

    /**
     * Determine whether a non-knight piece of the given player's can "pass through" a given position
     * i.e. a bishop moving diagonally or a rook moving horizontally through that position.
     * @param player: The current player's color
     * @param pos: The position to move through
     * @return
     */
    private boolean canPassThrough(Color player, Position pos) {
        return !pieceExists(player, pos) && !pieceExists(getOpponent(player), pos);
    }

    /**
     * Determine whether a piece of the given player's can stop at a given position.
     * A piece may stop at a position if it can pass through it or attack it.
     * @param player: The current player's color
     * @param pos: The position to stop at
     * @return
     */
    private boolean canStopAt(Color player, Position pos) {
        return !pieceExists(player, pos);
    }

    private Map<Position, Piece> getPieces(Color player) {
        return pieces.get(player);
    }

    private Piece getPiece(Color player, Position pos) {
        return getPieces(player).get(pos);
    }

    private boolean pieceExists(Color player, Position pos) {
        return getPieces(player).containsKey(pos);
    }

    private void removePiece(Color player, Position pos) {
        getPieces(player).remove(pos);
    }

    private void movePiece(Color player, Position pos, Position newPos) {
        Piece pieceMoved = getPiece(player, pos);
        removePiece(player, pos);
        addPiece(player, newPos, pieceMoved);
    }

    private void addPiece(Color player, Position pos, Piece piece) {
        getPieces(player).put(pos, piece);
        if (piece == Piece.King) {
            kings.put(player, pos);
        }
    }

    private void disableCastle(Color player, Castle side) {
        canCastle.get(player).put(side, false);
    }

    private boolean canCastle(Color player, Castle side) {
        return canCastle.get(player).get(side);
    }

    /**
     * Determine if a player's pawn at a position can be promoted.
     * @param color: The current player's color
     * @param pos: The piece's position
     * @return Whether or not a promotion is possible
     */
    private boolean canPromote(Color color, Position pos) {
        return getPiece(color, pos) == Piece.Pawn && pos.y == (color == Color.White ? boardSize - 1 : 0);
    }

    public String boardAsString() {
        StringBuilder boardString = new StringBuilder();
        Position pos = new Position(0, 0);
        for (int y = boardSize - 1; y >= 0; y--) {
            boardString.append(y);
            boardString.append("|");
            for (int x = 0; x < boardSize; x++) {
                pos.set(x, y);
                if (pieceExists(Color.White, pos)) {
                    boardString.append(pieceAsString(getPiece(Color.White, pos)).toUpperCase());
                } else if (pieceExists(Color.Black, pos)) {
                    boardString.append(pieceAsString(getPiece(Color.Black, pos)));
                } else {
                    boardString.append("_");
                }
            }
            boardString.append("\n");
        }
        boardString.append("  01234567");
        return boardString.toString();
    }

    private String pieceAsString(Piece piece) {
        switch (piece) {
            case Pawn:
                return "p";
            case Knight:
                return "n";
            case Bishop:
                return "b";
            case Rook:
                return "r";
            case Queen:
                return "q";
            case King:
                return "k";
        }
        return "";
    }

    public void setFrontEnd(ChessFrontEnd frontEnd) {
        this.frontEnd = frontEnd;
    }

    public GameState getState() {
        return state;
    }
}
