import java.io.IOException;
import java.util.Scanner;

public class TerminalFrontEnd implements ChessFrontEnd {
    Scanner scanner = new Scanner(System.in);
    public Piece askPromotion() {
        String input = "";
        System.out.println("Enter a piece (n, b, r, q) to promote to:\n");
        while (true) {
            input = scanner.next().toLowerCase();
            if (!input.equals("")) {
                switch (input.charAt(0)) {
                    case 'n':
                        return Piece.Knight;
                    case 'b':
                        return Piece.Bishop;
                    case 'r':
                        return Piece.Rook;
                    case 'q':
                        return Piece.Queen;
                    default:
                        System.out.println("Please enter a piece.\n");
                }
            } else {
                System.out.println("Please enter a piece.\n");
            }
        }
    }
}
