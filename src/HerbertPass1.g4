grammar HerbertPass1;

@header {
    import java.util.ArrayList;
}

@members{
    Type currentType;
    int loopCounter = 0;
    int receiverIndexCounter = 0;

    void print(String str){
        System.out.println(str);

        if ((str.length() > 1) && (str.charAt(0) == '['))
            Herbert.failed = true;
    }

    void putLocalVar(String name, Type type) throws ItemAlreadyExistsException {
        SymbolTable.top.put(
            new SymbolTableLocalVariableItem(
                new Variable(name, type),
                SymbolTable.top.getOffset(Register.K1)
            )
        );
    }

    void putGlobalVar(String name, Type type) throws ItemAlreadyExistsException {
        SymbolTable.top.put(
            new SymbolTableGlobalVariableItem(
                new Variable(name, type),
                SymbolTable.top.getOffset(Register.GP)
            )
        );
    }

    void putActor(String name, String capacity, SymbolTable table) throws ItemAlreadyExistsException {
        SymbolTable.top.put(
            new SymbolTableActorItem(
                new Actor(name, capacity, table)
            )
        );
    }

    void putReceiver(String name, Type[] types, SymbolTable table) throws ItemAlreadyExistsException {
        SymbolTable.top.put(
            new SymbolTableReceiverItem(
                new Receiver(name, types, table, receiverIndexCounter)
            )
        );
        receiverIndexCounter++;
    }

    void beginScope() {
    	int spOffset = 0, gpOffset = 0;
    	if(SymbolTable.top != null)
        {
        	spOffset = SymbolTable.top.getOffset(Register.SP);
            gpOffset = SymbolTable.top.getOffset(Register.GP);
        }
        SymbolTable.push(new SymbolTable(SymbolTable.top));
        SymbolTable.top.setOffset(Register.SP, spOffset);
        SymbolTable.top.setOffset(Register.GP, gpOffset);
        print("Begin Scope");
    }
    
    SymbolTable endScope() {
        SymbolTable idioTable = SymbolTable.top;

        print("End Scope: Stack offset: " + SymbolTable.top.getOffset(Register.SP) + ", " +
              "Global offset: " + SymbolTable.top.getOffset(Register.GP));
        int gp = SymbolTable.top.getOffset(Register.GP);
        SymbolTable.pop();

        if (SymbolTable.top == null)
            return null;

        SymbolTable.top.setOffset(Register.GP, gp);

        return idioTable;
    }
}

// grammar

program
    :
        { beginScope(); } // biggest scope contains actors
        EOS* actor*
        { endScope(); }
    ;

actor
    :
        ((ACTOR name = ID) cap = ACTOR_BILBILAK 
        {
            beginScope();
            String sizeBil = $cap.text;
            if (Integer.parseInt(sizeBil.substring(1, sizeBil.length()-1)) <= 0)
                print("[Line #" + $cap.line + "] Invalid actor size.");

            receiverIndexCounter = 0;
            try {
                putGlobalVar("__mailbox", new ArrayType(CharType.getInstance(), sizeBil));
                putGlobalVar("__head", IntType.getInstance());
                putGlobalVar("__tail", IntType.getInstance());
            } catch (ItemAlreadyExistsException e) {
                print("********** Fatal error occured in Herbert **********");
            }

        }
        EOS+ body END (EOS+ | EOF)
        { 
            SymbolTable actorTable = endScope();

             String actorName = $name.text;
            int retryCount = 0;
            while (true) {
                try {
                    putActor(actorName, $cap.text, actorTable);
                    break;
                }
                catch (ItemAlreadyExistsException ex) {
                    actorName = $name.text + "_Temp_" + retryCount;
                    retryCount++;
                }
            }
            if (retryCount > 0) {
                print(String.format("[Line #%s] Actor \"%s\" already exists. Will use the name \"%s\" instead.", $name.getLine(), $name.text, actorName));
            }
        }
        )
    ;

body
    :
        (global_var_def | receiver)*
    ;

global_var_def
    :
        (var_type = type { currentType = $var_type.return_type; } 
        (array_size = ARRAY_BILBILAK 
            { 
                currentType = new ArrayType(currentType, $array_size.text);
                String sizeBil = $array_size.text;
                if (sizeBil != null)
                    if (Integer.parseInt(sizeBil.substring(1, sizeBil.length()-1)) <= 0)
                        print("[Line #" + $array_size.line + "] Invalid array size.");    
            }
        )* 
        var_name = ID 
        { 
            String varName = $var_name.text;
            int retryCount = 0;
            while (true) {
                try {
                    putGlobalVar($var_name.text, currentType);
                    break;
                }
                catch(ItemAlreadyExistsException e) {
                    varName = $var_name.text + "_Temp_" + retryCount;
                    retryCount++;                    
                }
            }
            if (retryCount > 0) {
                print(String.format("[Line #%s] Variable \"%s\" already exists. Will use the name \"%s\" instead.", $var_name.getLine(), $var_name.text, varName));
            }
        } 
        (',' var_name = ID 
        { 
            varName = $var_name.text;
            retryCount = 0;
            while (true) {
                try {
                    putGlobalVar($var_name.text, currentType);
                    break;
                }
                catch(ItemAlreadyExistsException e) {
                    varName = $var_name.text + "_Temp_" + retryCount;
                    retryCount++;                    
                }
            }
            if (retryCount > 0) {
                print(String.format("[Line #%s] Variable \"%s\" already exists. Will use the name \"%s\" instead.", $var_name.getLine(), $var_name.text, varName));
            }
        } 
        )* EOS+)
    ;

var_def
    :
        (var_type = type { currentType = $var_type.return_type; } 
        (array_size = ARRAY_BILBILAK 
            { 
                currentType = new ArrayType(currentType, $array_size.text); 
                String sizeBil = $array_size.text;
                if (Integer.parseInt(sizeBil.substring(1, sizeBil.length()-1)) <= 0)
                    print("[Line #" + $array_size.line + "] Invalid array size.");
            }
        )* 
        var_def_inside(',' var_def_inside)* EOS+)
    ;

var_def_inside
    :
        (
		(var_name = ID)
		|
        (var_name = ID ('=' var_initial_value))
        )       
        { 
            String varName = $var_name.text;
            int retryCount = 0;
            while (true) {
                try {
                    putLocalVar($var_name.text, currentType);
                    break;
                }
                catch(ItemAlreadyExistsException e) {
                    varName = $var_name.text + "_Temp_" + retryCount;
                    retryCount++;                    
                }
            }
            //print("******* " + SymbolTable.top.get($var_name.text).toString());
            //print(SymbolTable.top.get($var_name.text) == null ? "SHIT" :"FINE");
            if (retryCount > 0) {
                print(String.format("[Line #%s] Variable \"%s\" already exists. Will use the name \"%s\" instead.", $var_name.getLine(), $var_name.text, varName));
            }
        } 

    ;

var_initial_value
    :
        (('{' var_initial_value (','var_initial_value)* '}') | expression)
    ;

receiver
    :
        ((RECEIVER name = ID) { System.out.print("Receiver "); beginScope(); } 
        '(' types = receiver_var_def ')' EOS+
        receiver_block END 
        {
            SymbolTable receiverTable = endScope();

            String receiverName = $name.text;
            int retryCount = 0;
            while (true) {
                try {
                    putReceiver(receiverName, $types.types, receiverTable);
                    break;
                }
                catch (ItemAlreadyExistsException ex) {
                    receiverName = $name.text + "_Temp_" + retryCount;
                    retryCount++;
                }
            }
            if (retryCount > 0) {
                print(String.format("[Line #%s] Receiver \"%s\" already exists. Will use the name \"%s\" instead.", $name.getLine(), $name.text, receiverName));
            }
        }
        EOS+)
    ;

receiver_var_def returns [Type[] types]
    :
        {
            ArrayList<Type> ret = new ArrayList<Type>();
        }
        ((t = receiver_var_def_inside {ret.add($t.varType);} (',' t = receiver_var_def_inside {ret.add($t.varType);})*) | )
        { $types = ret.toArray(new Type[ret.size()]);}
    ;

receiver_var_def_inside returns [Type varType]
    :
        (var_type = type { currentType = $var_type.return_type; } 
        (array_size = ARRAY_BILBILAK 
            { 
                currentType = new ArrayType(currentType, $array_size.text); 
                String sizeBil = $array_size.text;
                if (Integer.parseInt(sizeBil.substring(1, sizeBil.length()-1)) <= 0)
                    print("[Line #" + $array_size.line + "] Invalid array size.");
            }
        )* 
        var_name = ID)
        { 
            String varName = $var_name.text;
            int retryCount = 0;
            while (true) {
                try {
                    putLocalVar($var_name.text, currentType);
                    break;
                }
                catch(ItemAlreadyExistsException e) {
                    varName = $var_name.text + "_Temp_" + retryCount;
                    retryCount++;                    
                }
            }
            if (retryCount > 0) {
                print(String.format("[Line #%s] Variable \"%s\" already exists. Will use the name \"%s\" instead.", $var_name.getLine(), $var_name.text, varName));
            }

            $varType = currentType;
        } 
    ;

receiver_block
    :
        (var_def | statement)*
    ;

for_block
    :
        (var_def | statement)*
    ;

block 
    :
        { beginScope(); }
        (var_def | statement)*
        { endScope(); }
    ;

statement
	:
		(( expression EOS+) | send | if_block | foreach_block | control_flow | new_block | write_function)
	;
	
new_block
    :
        (BEGIN EOS+ block END EOS+)
        
    ;

control_flow
    :
        ((QUIT | 
        br=BREAK{
                    if (loopCounter <= 0) {
                        print("[Line #" + $br.line + "] invalid 'break' statement.");
                    }
                }
        ) EOS+)
    ;

send
    :
        ((SENDERLF | ID) SENDTO ID '(' (expression (',' expression)*)? ')' EOS+)
    ;

if_block
    :
        (IF expression EOS+ block
         (ELSEIF expression EOS+ block)* 
         (ELSE EOS+ block)? 
         END EOS+)
    ;

foreach_block
    :
        { loopCounter++; }
        (FOREACH el=ID IN exp=expression EOS+
            {
                beginScope();
                try {
                putLocalVar($el.text, UnknownType.getInstance());
                //print(SymbolTable.top.get($el.text) == null ? "SHIT" :"FINE");
                //print("F " + $el.text.length());
                }
                catch (ItemAlreadyExistsException ex) { }
            }
        for_block END EOS+)
        {
            endScope();
            loopCounter--; 
        }
    ;

expression
    :
        (lvl10)
    ;

lvl10
    :
		(lvl9)
		|
        (
            lvl9 ('=' lvl9 )* eq='=' lvl9
        )
    ;

lvl9
    :
		(lvl8)
		|
        (lvl8 (op='or' lvl9))
    ;

lvl8
    :
		(lvl7)
		|
        (lvl7 (op='and' lvl8))
    ;

lvl7
    :
		(lvl6)
		|
        (lvl6 (op=('==' | '<>')lvl7))
    ;

lvl6
    :
		(lvl5)
		|
        (lvl5 ( op=('<' | '>') lvl6))
    ;

lvl5
    :
		(lvl4)
		|
        (lvl4 (op=('+' | '-')lvl5))
    ;

lvl4
    :
		(lvl3)
		|
        (lvl3(op=('*'|'/')lvl4))
    ;

lvl3
    :
		(lvl1)
		|
        (op=('-'|'not')*lvl1)							// !!!!????
    ;

lvl1
    :
		('('lvl10')')
        |
        (initial_value | (ID (ARRAY_BILBILAK | ('[' expression ']'))*) | read_function)
    ;

write_function
    :
        (WRITE '(' expression ')' EOS+)
    ;

read_function
    :
        (READ '(' CONST_INT ')')
    ;

initial_value
	: 
		(CONST_INT | CONST_CHAR | CONST_STR)
    ;

type returns [Type return_type] 
    :
        ('int' { $return_type = IntType.getInstance(); }) |
        ('char' { $return_type = CharType.getInstance(); })
    ;

// lexer

ACTOR : 'actor';
RECEIVER : 'receiver';
QUIT : 'quit';
IF : 'if';
ELSEIF : 'elseif';
ELSE : 'else';
SENDERLF : 'sender' | 'self';
IN : 'in';
BEGIN : 'begin';
END : 'end';
READ : 'read';
WRITE : 'write';
SENDTO : '<<';
FOREACH : 'foreach';
BREAK : 'break';

WS : [ \r\t]+ -> skip;
EOS : '\n';

ID : [a-zA-Z_][a-zA-Z0-9_]* ;

ACTOR_BILBILAK : '<'('-'?)[0-9]+'>' ;
ARRAY_BILBILAK : '['('-'?)[0-9]+']' ;

CONST_INT : [0-9]+ ;							// negative or positive
CONST_CHAR : '\''~('\n')'\'';
CONST_STR : '"' (~('\n'))* '"';								// careful about #

COMMENT : '#' (~('\n'))* -> skip;