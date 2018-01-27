import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.IOException;

// Visit https://stackoverflow.com/questions/26451636/how-do-i-use-antlr-generated-parser-and-lexer
public class Herbert {
    public static boolean failed = false;

	public static void main(String[] args) throws IOException {
		ANTLRInputStream reader = new ANTLRFileStream("src/test/18.atk");

        System.out.println("Compiling started...");
        System.out.println("");
		// Pass #1
		HerbertPass1Lexer pass1Lexer = new HerbertPass1Lexer(reader);
		CommonTokenStream pass1Tokens = new CommonTokenStream(pass1Lexer);
		HerbertPass1Parser pass1Parser = new HerbertPass1Parser(pass1Tokens);
		ParseTree pass1Tree = pass1Parser.program(); // Program is starting production rule
	    //System.out.println(pass1Tree.toStringTree());
        
        System.out.println();

        if (failed) {
            System.out.println("Compile stopped due to errors in first pass.");
            return;
        }

        failed = false;
        System.out.println("Pass 1 finished. Starting pass 2...");

        System.out.println();
		// Pass #2
		reader.reset();
		HerbertPass2Lexer pass2Lexer = new HerbertPass2Lexer(reader);
		CommonTokenStream pass2Tokens = new CommonTokenStream(pass2Lexer);
		HerbertPass2Parser pass2Parser = new HerbertPass2Parser(pass2Tokens);
        ParseTree pass2Tree = pass2Parser.program();
        

        if (failed) {
            System.out.println();
            System.out.println("Compile stopped due to errors in second pass.");
            return;
        }

        System.out.println("Pass 2 finished. Congratulations!");
	}
}