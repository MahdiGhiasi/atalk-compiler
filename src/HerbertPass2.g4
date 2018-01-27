grammar HerbertPass2;

@header {
    import java.util.ArrayList;
}

@members{
    Type currentType;
    String currentReceiver, currentActor;
    int exprFlag;

    void print(String str){
        System.out.println(str);

        if ((str.length() > 1) && (str.charAt(0) == '['))
            Herbert.failed = true;
    }

    void beginScope() {
        SymbolTable.push();
        print("Begin Scope");
    }

    void endScope() {
        print("Stack offset: " + SymbolTable.top.getOffset(Register.SP) + ", Global offset: " + SymbolTable.top.getOffset(Register.GP));
        SymbolTable.pop();
        
    }

    void putLocalVar(String name, Type type) throws ItemAlreadyExistsException {
        SymbolTable.top.put(
            new SymbolTableLocalVariableItem(
                new Variable(name, type),
                SymbolTable.top.getOffset(Register.SP)
            )
        );
    }

    Type checkVariableExistance(String id, int line) {
        SymbolTableItem item = SymbolTable.top.get(id);
        if(item == null) {
            print("[Line #" + line + "] Item " + id + " doesn't exist.");
            try {
                putLocalVar(id, NoType.getInstance());
                SymbolTable.define();
                //print("******* " + (SymbolTable.top.get(id) == null ? "Fuck" : "Ok"));
            } 
            catch(ItemAlreadyExistsException ex) {}
            return NoType.getInstance();
        }
        else {
            SymbolTableVariableItemBase var = (SymbolTableVariableItemBase) item;
            //print("[Line #" + line + "] Variable " + id + " used.\t\t" +   "Base Reg: " + var.getBaseRegister() + ", Offset: " + var.getOffset());
            return var.getVariable().getType();
        }
    }

    boolean IsString(Type type) {
        return (type instanceof ArrayType) && (((ArrayType)type).getBaseType() instanceof CharType);       
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
                currentActor = $name.text;
                beginScope();
            }
        EOS+ body END (EOS+ | EOF))
        { 
            endScope();
            currentActor = "";
        }
    ;

body
    :
        (global_var_def | receiver)*
    ;

global_var_def
    :
        (var_type = type (array_size = ARRAY_BILBILAK)* 
        var_name = ID 
        (',' var_name = ID 
        )* EOS+)
    ;

var_def
    :
        (var_type = type (array_size = ARRAY_BILBILAK)* var_def_inside(',' var_def_inside)* EOS+)
    ;

var_def_inside
    :
        (
		(var_name = ID)
		|
        (var_name = ID ('=' var_initial_value))
        )       
        { SymbolTable.define(); }

    ;

var_initial_value
    :
        (('{' var_initial_value (','var_initial_value)* '}') | expression)
    ;

receiver
    :
        ((RECEIVER name = ID) { beginScope(); } 
        '(' types = receiver_var_def ')' EOS+ { currentReceiver = (new Receiver($name.text, $types.types, null)).toString(); }
        receiver_block END 
        {
            endScope();
            currentReceiver = "";
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
        (var_type = type { currentType = $var_type.return_type; } (array_size = ARRAY_BILBILAK { currentType = new ArrayType(currentType, $array_size.text); })* 
        var_name = ID)
        {
            SymbolTable.define();
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
        ((QUIT | BREAK) EOS+)
    ;

send
    :
        {
            ArrayList<Type> types = new ArrayList<Type>();
        }
        (
            (sdr = SENDER SENDTO ID '(' (expression (',' expression)*)? ')' EOS+ 
                { 
                    SymbolTable actorTable = SymbolTable.top;
                    while (actorTable.pre.pre != null)
                        actorTable = actorTable.pre;

                    Receiver receiver = ((SymbolTableReceiverItem)actorTable.get(currentReceiver)).getReceiver();
                    if (receiver.toString().equals("init ()"))
                    {
                        print("[Line #" + $sdr.line + "] 'sender' can't be called from init()");
                    }
                }
            ) |
            ((SELF SENDTO receiver_name=ID '(' (ex=expression { types.add($ex.return_type); } (',' ex=expression { types.add($ex.return_type); })*)? ')' EOS+)
                {
                    SymbolTable actorTable = SymbolTable.top;
                    while (actorTable.pre.pre != null)
                        actorTable = actorTable.pre;

                    String receiverKey = (new Receiver($receiver_name.text, types.toArray(new Type[types.size()]), null)).toString();

                    SymbolTableItem receiver = actorTable.get(receiverKey);

                    if (receiver == null) {
                        print("[Line #" + $receiver_name.line + "] Receiver '" + receiverKey + "' does not exist in actor " + currentActor + ".");
                    }
                }
            ) |
            (actor_name = ID 
                { 
                    SymbolTable rootTable = SymbolTable.top;
                    while (rootTable.pre != null)
                        rootTable = rootTable.pre;

                    SymbolTableItem actor = rootTable.get($actor_name.text);
                    if (actor == null) {
                        print("[Line #" + $actor_name.line + "] Actor '" + $actor_name.text + "' does not exist.");
                    }
                } 
                (SENDTO receiver_name = ID '(' (ex=expression { types.add($ex.return_type); } (',' ex=expression { types.add($ex.return_type); })*)? ')' EOS+)
                {
                    if (actor != null) {
                        Actor theActorItself = ((SymbolTableActorItem)actor).getActor();
                        String receiverKey = (new Receiver($receiver_name.text, types.toArray(new Type[types.size()]), null)).toString();
                        SymbolTableItem receiver = theActorItself.getSymbolTable().get(receiverKey);
                        if (receiver == null) {
                            print("[Line #" + $receiver_name.line + "] Receiver '" + receiverKey + "' does not exist in actor " + currentActor + ".");
                        }
                    }
                }
            )
        )
    ;

if_block
    :
        (l=IF exp=expression EOS+ block
            {
                if ((!($exp.return_type instanceof IntType)) && (!($exp.return_type instanceof NoType)))
                    print("[Line #" + $l.line + "] Type '" + $exp.return_type.toString() + "' cannot be used as a condition.");
            }
         (l=ELSEIF exp=expression EOS+ block
            {
                if ((!($exp.return_type instanceof IntType)) && (!($exp.return_type instanceof NoType)))
                    print("[Line #" + $l.line + "] Type '" + $exp.return_type.toString() + "' cannot be used as a condition.");
            }
         )* 
         (ELSE EOS+ block)? 
         END EOS+)
    ;

foreach_block
    :
        (FOREACH el=ID IN exp=expression EOS+
            {
                beginScope();
                SymbolTable.define();
                if ($exp.return_type instanceof ArrayType) {
                    //print("a" + $el.text.length());
                    //print(SymbolTable.top.get($el.text) == null ? "SHIT" :"FINE");

                    ((SymbolTableVariableItemBase)SymbolTable.top.get($el.text)).getVariable().setType(((ArrayType)$exp.return_type).getBaseType());
                }
                else {
                    print("[Line #" + $el.line + "] Type '" + $exp.return_type.toString() + "' cannot be used as an iterative.");

                    ((SymbolTableVariableItemBase)SymbolTable.top.get($el.text)).getVariable().setType(NoType.getInstance());
                } 
            }
        for_block END EOS+)
        {
            endScope();

        }
    ;


expression returns [Type return_type] 
    :
        (t = lvl10 { $return_type = $t.return_type; })
    ;

lvl10 returns [Type return_type] 
@init {
    boolean mismatchFlag = false;
}
    :
		(t = lvl9 { $return_type = $t.return_type; })
		|
        (
            ( { exprFlag = 0; mismatchFlag = false; } t1=lvl9
            ('=' t2=lvl9 
                { 
                    if (!$t1.return_type.equals($t2.return_type)) {
                        mismatchFlag = true;
                    } 
                }
            )* eq='='
                {
                    if (exprFlag != 0) {
                        print("[Line #" + $eq.line + "] The left side of '=' must be an lvalue, you idiot.");
                        $return_type = NoType.getInstance();
                    }
                }
             t3=lvl9)
            {
                if (!$t1.return_type.equals($t3.return_type)) {
                    mismatchFlag = true;
                } 

                if (mismatchFlag) {
                    print("[Line #" + $eq.line + "] Invalid assignment.");
                    $return_type = NoType.getInstance();
                }
                else {
                    $return_type = $t1.return_type;
                }
            }
        )
    ;

lvl9 returns [Type return_type] 
    :
		(t = lvl8 { $return_type = $t.return_type; })
		|
        (t1=lvl8 (op='or' t2=lvl8)+)
            { 
                exprFlag = 1; //print("l9");
                if (($t1.return_type instanceof IntType) && ($t2.return_type instanceof IntType)) {
                    $return_type = IntType.getInstance();
                }
                else if (($t1.return_type instanceof NoType) || ($t2.return_type instanceof NoType)) {
                    $return_type = NoType.getInstance();
                }
                else {
                    print("[Line #" + $op.line + "] Operator '" + $op.text + "' cannot be applied between types '" + $t1.return_type.toString() + "' and '" + $t2.return_type.toString() + "'.");
                    $return_type = NoType.getInstance();
                }
            }
    ;

lvl8 returns [Type return_type] 
    :
		(t = lvl7 { $return_type = $t.return_type; })
		|
        (t1=lvl7 (op='and' t2=lvl7)+)
            { 
                exprFlag = 1; //print("l8");
                if (($t1.return_type instanceof IntType) && ($t2.return_type instanceof IntType)) {
                    $return_type = IntType.getInstance();
                } 
                else if (($t1.return_type instanceof NoType) || ($t2.return_type instanceof NoType)) {
                    $return_type = NoType.getInstance();
                }
                else {
                    print("[Line #" + $op.line + "] Operator '" + $op.text + "' cannot be applied between types '" + $t1.return_type.toString() + "' and '" + $t2.return_type.toString() + "'.");
                    $return_type = NoType.getInstance();
                }
            }
    ;

lvl7 returns [Type return_type] 
    :
		(t = lvl6 { $return_type = $t.return_type; })
		|
        (t1=lvl6 (op=('==' | '<>')t2=lvl6)+)
            { 
                exprFlag = 1; //print("l7");
                
                if ((($t1.return_type instanceof IntType) && ($t2.return_type instanceof IntType)) ||
                    (($t1.return_type instanceof CharType) && ($t2.return_type instanceof CharType)) ||
                    (IsString($t1.return_type) && IsString($t2.return_type))) {
                    $return_type = IntType.getInstance();
                } 
                else if (($t1.return_type instanceof NoType) || ($t2.return_type instanceof NoType)) {
                    $return_type = NoType.getInstance();
                }
                else {
                    print("[Line #" + $op.line + "] Operator '" + $op.text + "' cannot be applied between types '" + $t1.return_type.toString() + "' and '" + $t2.return_type.toString() + "'.");
                    $return_type = NoType.getInstance();
                }
            }
    ;

lvl6 returns [Type return_type] 
    :
		(t = lvl5 { $return_type = $t.return_type; })
		|
        (t1=lvl5 ( op=('<' | '>') t2=lvl5)+) { exprFlag = 1; }
            { 
                exprFlag = 1; //print("l6");
                if ((($t1.return_type instanceof IntType) && ($t2.return_type instanceof IntType)) ||
                    (($t1.return_type instanceof CharType) && ($t2.return_type instanceof CharType))) {
                    $return_type = $t1.return_type;
                } 
                else if (($t1.return_type instanceof NoType) || ($t2.return_type instanceof NoType)) {
                    $return_type = NoType.getInstance();
                }
                else {
                    print("[Line #" + $op.line + "] Operator '" + $op.text + "' cannot be applied between types '" + $t1.return_type.toString() + "' and '" + $t2.return_type.toString() + "'.");
                    $return_type = NoType.getInstance();
                }
            }
    ;

lvl5 returns [Type return_type] 
    :
		(t = lvl4 { $return_type = $t.return_type; })
		|
        (t1=lvl4 (op=('+' | '-')t2=lvl4)+) 
            { 
                exprFlag = 1; //print("l5");
                Type stringType = new ArrayType(CharType.getInstance(), 1);
                
                if (($t1.return_type instanceof IntType) && ($t2.return_type instanceof IntType)) {
                    $return_type = IntType.getInstance(); // Int - Int , Int + Int => Int
                } 
                else if (($op.text.equals("-")) && (($t2.return_type instanceof IntType) && ($t1.return_type instanceof CharType))) { 
                    $return_type = CharType.getInstance(); // Char - Int => Char
                }
                else if (($op.text.equals("+")) && ((($t1.return_type instanceof IntType) && ($t2.return_type instanceof CharType)) || 
                         (($t2.return_type instanceof IntType) && ($t1.return_type instanceof CharType)))) {
                    $return_type = CharType.getInstance(); // Char + Int , Int + Char => Char
                }
                else if (($op.text.equals("+")) && (IsString($t1.return_type) || ($t1.return_type instanceof CharType)) && (IsString($t2.return_type) || ($t2.return_type instanceof CharType))) {
                    $return_type = stringType; // Char + Char , String + Char , Char + String => String
                } 
                else if (($t1.return_type instanceof NoType) || ($t2.return_type instanceof NoType)) {
                    $return_type = NoType.getInstance();
                }
                else {
                    print("[Line #" + $op.line + "] Operator '" + $op.text + "' cannot be applied between types '" + $t1.return_type.toString() + "' and '" + $t2.return_type.toString() + "'.");
                    $return_type = NoType.getInstance();
                }
            }
    ;

lvl4 returns [Type return_type] 
    :
		(t = lvl3 { $return_type = $t.return_type; })
		|
        (t1=lvl3(op=('*'|'/')t2=lvl3)+) 
            {
                exprFlag = 1;//print("l4");
                if (($t1.return_type instanceof IntType) && ($t2.return_type instanceof IntType)) {
                    $return_type = IntType.getInstance();
                } 
                else if (($t1.return_type instanceof NoType) || ($t2.return_type instanceof NoType)) {
                    $return_type = NoType.getInstance();
                }
                else {
                    print("[Line #" + $op.line + "] Operator '" + $op.text + "' cannot be applied between types '" + $t1.return_type.toString() + "' and '" + $t2.return_type.toString() + "'.");
                    $return_type = NoType.getInstance();
                }
            }
    ;

lvl3 returns [Type return_type] 
    :
		(t = lvl1 { $return_type = $t.return_type; })
		|
        (op=('-'|'not')*t1=lvl1) 		    	// !!!!????
            {
                exprFlag = 1;//print("l3");
                if ($t1.return_type instanceof IntType) {
                    $return_type = IntType.getInstance();
                } 
                else if ($t1.return_type instanceof NoType) {
                    $return_type = NoType.getInstance();
                }
                else {
                    print("[Line #" + $op.line + "] Operator '" + $op.text + "' cannot be applied to type '" + $t1.return_type.toString() + "'.");
                    $return_type = NoType.getInstance();
                }
            }
    ;

lvl1 returns [Type return_type] 
@init {
    Type varType;
    int depth = 0;
}
    :
		('('t1=lvl10')') { exprFlag = 1;  $return_type = $t1.return_type;  }
        |
        (
            t=initial_value { exprFlag = 1; $return_type = $t.return_type; } 
            | (var_name = ID { varType = checkVariableExistance($var_name.text, $var_name.line); depth = 0; } ((ARRAY_BILBILAK | ('[' expression ']')){ depth++; })*)
                {
                    Type baseType = varType;
                    int varDepth = 0;
                    while (baseType instanceof ArrayType)
                    {
                        varDepth++;
                        baseType = ((ArrayType)baseType).getBaseType();
                    }

                    int remDepth = varDepth - depth;
                    if (remDepth < 0) {
                        print("[Line #" + $var_name.line + "] Dadach dimentions-et ziadie...");
                        $return_type = NoType.getInstance();
                    } else {
                        while (remDepth > 0) {
                            remDepth--;
                            baseType = new ArrayType(baseType, 1);
                        }

                        $return_type = baseType;
                    }
                } 
            | t3=read_function { exprFlag = 1; $return_type = $t3.return_type; }
        )
    ;

write_function
    :
        (WRITE '(' expression ')' EOS+)
    ;

read_function returns [Type return_type] 
    :
        (READ '(' len=CONST_INT ')')
        { $return_type = new ArrayType(CharType.getInstance(), 1); }
    ;

initial_value returns [Type return_type] 
	: 
		(CONST_INT { $return_type = IntType.getInstance(); } | 
        CONST_CHAR { $return_type = CharType.getInstance(); } | 
        str=CONST_STR { $return_type = new ArrayType(CharType.getInstance(), 1); })
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
SENDER : 'sender';
SELF : 'self';
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

CONST_INT : [0-9]+ ;							// NOT negative or positive, POSITIVE FTW!
CONST_CHAR : '\''~('\n')'\'';
CONST_STR : '"' (~('\n'))* '"';								// careful about #

COMMENT : '#' (~('\n'))* -> skip;