/**
 * Created by vrasa on 12/26/2016.
 */

import java.util.*;
import java.io.*;

public class Translator {

    private File output;
    private ArrayList <String> instructions;

    int actorCounter = 0;
    int totalActorsCount;
    SymbolTable rootTable;
    int uniqueNum = 0;

    public Translator(SymbolTable rootTable){
        this.rootTable = rootTable;
        this.totalActorsCount = rootTable.getItemsCount();

        instructions = new ArrayList<String>();
        output = new File("out.asm");
        try {
            output.createNewFile();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public int getNextUniqueNum() {
        return ++uniqueNum;
    }

    public void addInst(String s) {
        if (s.contains(":"))
            instructions.add(s);
        else if ((s.length() > 1) && (s.toCharArray()[0] == '#'))
            instructions.add(s);
        else
            instructions.add("\t" + s);
    }

    public void makeOutput(){
        this.addSystemCall(10);
        try {
            PrintWriter writer = new PrintWriter(output);
            writer.println("main:");
            writer.println("move $fp, $sp");
            for (int i=0;i<instructions.size();i++){
                writer.println(instructions.get(i));
            }
            writer.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void putInit(SymbolTable table) {
        addInst("");
        addInst("li $k0, 1");
        addInst("addi $k1, $gp, " + table.getOffset(Register.GP) + 100);
        addInst("");
        addInst("# put init message in all mailboxes");
        addInst("li $t0, 0");
        addInst("li $t1, 1");
        addInst("");
        for (SymbolTableItem item : table.items.values()) {
            SymbolTableActorItem actor = (SymbolTableActorItem)item;
            SymbolTableReceiverItem initReceiver = (SymbolTableReceiverItem)actor.getActor().getSymbolTable().get("init ()");
            if (initReceiver == null) continue; //This actor does not have an init() receiver.

            SymbolTableGlobalVariableItem mailbox = (SymbolTableGlobalVariableItem)actor.getActor().getSymbolTable().get("__mailbox");
            SymbolTableGlobalVariableItem head = (SymbolTableGlobalVariableItem)actor.getActor().getSymbolTable().get("__head");
            SymbolTableGlobalVariableItem tail = (SymbolTableGlobalVariableItem)actor.getActor().getSymbolTable().get("__tail");

            addInst("sb $t0, " + head.getOffset() + "($gp)");
            addInst("sb $t1, " + tail.getOffset() + "($gp)");
            addInst("li $t2, " + initReceiver.getReceiver().getIndex());
            addInst("sb $t2, " + mailbox.getOffset() + "($gp)");
            addInst("");
        }
        addInst("");
        addInst("");
    }

    public void putActorMailboxHandler(SymbolTableActorItem i) {
        addInst("# actor " + i.getActor().getName());
        addInst("actor" + actorCounter + "Check:");
        addInst("lb $t0, " + ((SymbolTableGlobalVariableItem)i.getActor().getSymbolTable().get("__head")).getOffset() + "($gp)");
        addInst("lb $t1, " + ((SymbolTableGlobalVariableItem)i.getActor().getSymbolTable().get("__tail")).getOffset() + "($gp)");

        if (actorCounter == 0) {
            addInst("bnez $k0, dontTerminate");
            addSystemCall(10); //Exit
            addInst("dontTerminate:");
            addInst("li $k0, 0");
        }

        addInst("beq $t0, $t1, actor" + (actorCounter + 1) % totalActorsCount + "Check");
        addInst("li $k0, 1");
        addInst("add $t0, $t0, $gp");
        addInst("lb $t2, " + ((SymbolTableGlobalVariableItem)i.getActor().getSymbolTable().get("__mailbox")).getOffset() + "($t0)");
        addInst("");

        List<SymbolTableReceiverItem> items = new ArrayList<>();
        for (SymbolTableItem item : i.getActor().getSymbolTable().items.values()) {
            if (item instanceof SymbolTableReceiverItem) {
                items.add((SymbolTableReceiverItem)item);
            }
        }

        items.sort((l, r) -> l.getReceiver().getIndex() - r.getReceiver().getIndex());

        for (SymbolTableReceiverItem r : items) {
            addInst("# receiver " + r.getReceiver().toString() + " check");

            // Copy From Mailbox To Local Variable Space
            // s0: length of data to copy
            // s1: beginning of mailbox
            // s2: start position relative to s1
            // s3: size of mailbox

            addInst("bnez $t2, actor" + actorCounter + "Receiver" + r.getReceiver().getIndex() + "AfterCheck");
            addInst("li $s0, " + r.getReceiver().getArgumentsTotalSize());

            addInst("li $s1, " + ((SymbolTableGlobalVariableItem) i.getActor().getSymbolTable().get("__mailbox")).getOffset());
            addInst("add $s1, $s1, $gp");

            addInst("lb $t0, " + ((SymbolTableGlobalVariableItem)i.getActor().getSymbolTable().get("__head")).getOffset() + "($gp)");
            addInst("addi $s2, $t0, 1");

            addInst("li $s3, " + i.getActor().getCapacity());
            addInst("remu $s2, $s2, $s3"); // head <- head mod size

            addInst("jal CopyFromQueue");
            addInst("sb $s2, " + ((SymbolTableGlobalVariableItem) i.getActor().getSymbolTable().get("__head")).getOffset() + "($gp)");
            addInst("j actor" + actorCounter + "Receiver" + r.getReceiver().getIndex() + "Body");
            addInst("");
            addInst("actor" + actorCounter + "Receiver" + r.getReceiver().getIndex() + "AfterCheck:");
            addInst("addi $t2, $t2, -1");
            addInst("");
        }

        //TODO: Fohsh
        addSystemCall(10); //Exit

        addInst("");
    }

    public void actorFinished() {
        actorCounter++;
    }

    public void putFunctions() {
        addInst("# Functions");

        putCopyFromQueueFunction();
        putCopyToQueueFunction();
        putActorBufferOverflowError();
        putIndexOutOfBoundError();

        addInst("");
    }

    private void putIndexOutOfBoundError() {
        addInst("# - IndexOutOfBoundError");
        addInst("IndexOutOfBoundError: ");

        //TODO

        addSystemCall(10); //Exit
        addInst("");
    }

    private void putActorBufferOverflowError() {
        addInst("# - ActorBufferOverflowError");
        addInst("ActorBufferOverflowError: ");

        //TODO

        addInst("jr $ra");
        addInst("");
    }

    private void putCopyToQueueFunction() {
        // Copy From Stack To Mailbox
        // s0: length of data to copy
        // s1: beginning of mailbox
        // s2: size of mailbox
        // s3: tail of mailbox

        //NOTE: This really is $sp, don't change it!

        addInst("# - Copy From Stack To Mailbox");
        addInst("CopyToQueue: ");

        addInst("add $s4, $sp, $s0"); // $s4 <- $sp + length

        addInst("CopyToQueueLoopBegin:");
        addInst("bne $s4, $sp, CopyToQueueLoopBodyBegin");
        addInst("jr $ra");

        addInst("CopyToQueueLoopBodyBegin:");
        addInst("lb $s5, 0($s4)"); //$s5 <- M[$s4]
        addInst("add $s6, $s3, $s1"); //$s6 <- tail + beginning of mailbox
        addInst("sb $s5, 0($s6)"); //M[$s6] <- $s5

        addInst("addi $s3, $s3, 1"); //$s3++
        addInst("remu $s3, $s3, $s2"); // tail <- tail mod size

        addInst("addi $s4, $s4, -1"); // $s4--
        addInst("j CopyToQueueLoopBegin");
    }

    public void putCopyFromQueueFunction() {
        // Copy From Mailbox To Local Variable Space
        // s0: length of data to copy
        // s1: beginning of mailbox
        // s2: start position relative to s1
        // s3: size of mailbox

        addInst("# - Copy From Mailbox To Local Variable Space");

        addInst("CopyFromQueue:");
        addInst("move $s4, $k1"); //dest

        addInst("Copy1LoopBegin:");

        addInst("bnez $s0, Copy1LoopBodyBegin");
        addInst("jr $ra");

        addInst("Copy1LoopBodyBegin:");

        // s4 <- [s1 + s2]
        addInst("add $s5, $s1, $s2");
        addInst("lb $s6, 0($s5)");
        addInst("sb $s6, 0($s4)");

        addInst("addi $s2, $s2, 1");
        addInst("remu $s2, $s2, $s3"); // head <- head mod size

        addInst("addi $s4, $s4, 1");
        addInst("addi $s0, $s0, -1");
        addInst("beqz $zero, Copy1LoopBegin");

        addInst("");
    }

    public void beginReceiver(SymbolTableReceiverItem receiver) {
        addInst("# Receiver " + receiver.getReceiver().toString());

        addInst("actor" + actorCounter + "Receiver" + receiver.getReceiver().getIndex() + "Body:");
    }

    public void endReceiver() {
        addInst("j actor" + ((actorCounter + 1) % totalActorsCount) + "Check");
        addInst("");
    }

    public void sendMessage(String actorName, String receiverKey) {
        SymbolTableActorItem actor = (SymbolTableActorItem)rootTable.get(actorName);
        SymbolTableReceiverItem receiver = (SymbolTableReceiverItem)actor.getActor().getSymbolTable().get(receiverKey);
        SymbolTableGlobalVariableItem mailbox = (SymbolTableGlobalVariableItem)actor.getActor().getSymbolTable().get("__mailbox");
        SymbolTableGlobalVariableItem head = (SymbolTableGlobalVariableItem)actor.getActor().getSymbolTable().get("__head");
        SymbolTableGlobalVariableItem tail = (SymbolTableGlobalVariableItem)actor.getActor().getSymbolTable().get("__tail");

        int labelIndex = getNextUniqueNum();

        addInst("# sendMessage to " + actor.getActor().getName() + "." + receiverKey + " - unique id = " + labelIndex);

        addInst("lb $t0, " + head.getOffset() + "($gp)");
        addInst("lb $t1, " + tail.getOffset() + "($gp)");
        addInst("lb $t2, " + mailbox.getOffset() + "($gp)");

        //Check if enough space in mailbox
        // used space ($t3) = head < tail ? tail - head : total - head + tail
        // free space ($t4) = total - 1 - used space
        addInst("bltu $t0, $t1, senderCheckL1_" + labelIndex); // head < tail => jump
        addInst("addi $t3, $t1, " + actor.getCapacity()); //$t3 <- total + tail
        addInst("sub $t3, $t3, $t0"); //$t3 <- $t3 - head
        addInst("j senderCheckL2_" + labelIndex);
        addInst("senderCheckL1_" + labelIndex + ":");
        addInst("sub $t3, $t1, $t0"); //$t3 <- tail - head
        addInst("senderCheckL2_" + labelIndex + ":");

        addInst("li $t4, " + (actor.getCapacity() - 1));
        addInst("sub $t4, $t4, $t3"); // $t4 <- $t4 - used space

        addInst("li $t5, " + (receiver.getReceiver().getArgumentsTotalSize() + 1));
        addInst("bleu $t5, $t4, senderCheckSuccess" + labelIndex); // 1 + arguments <= free space: then continue
        addInst("jal ActorBufferOverflowError");
        addInst("j senderCheckFinish" + labelIndex);

        //Finish checking mailbox space

        addInst("senderCheckSuccess" + labelIndex + ":");

        //store receiver number
        addInst("add $t3, $t2, $gp"); //t3 <- mailbox offset + $gp
        addInst("add $t3, $t3, $t1"); //t3 <- t3 + tail offset = $gp + mailbox offset + tail offset
        addInst("li $t4, " + receiver.getReceiver().getIndex());
        addInst("sb $t4, 0($t3)");

        //increase tail
        addInst("addi $t1, $t1, 1");
        //mod is a little delayed to be more efficient. see 4 instructions below

        //BEGIN arguments

        // CopyToQueue
        // s0: length of data to copy
        // s1: beginning of mailbox
        // s2: size of mailbox
        // s3: tail of mailbox

        addInst("li $s0, " + receiver.getReceiver().getArgumentsTotalSize());
        addInst("addiu $s1, $gp, " + mailbox.getOffset());
        addInst("li $s2, " + actor.getCapacity());

        addInst("remu $t1, $t1, $s2"); // tail <- tail mod size

        addInst("move $s3, $t1");

        addInst("jal CopyToQueue");


        //clear stack
        addInst("addiu $sp, $sp, " + receiver.getReceiver().getArgumentsTotalSize());

        //END arguments

        //save new tail value
        addInst("sb $s3, " + tail.getOffset() + "($gp)");

        addInst("senderCheckFinish" + labelIndex + ":");
        addInst("");
    }

    public void writeInteger() {
        addInst("");
        addInst("# write integer");
        //pushInt(808530488); //test
        popInt(false);
        addInst("");
        addInst("move $a0, $v0");

        printInteger();
    }

    public void readInteger() {
        addInst("#reading int");
        this.addSystemCall(5);
        pushIntReg("$v0");
    }

    public void addToStack(byte x){
        addInst("# adding a number to stack");
        addInst("li $a0, " + x);
        addInst("sb $a0, 0($sp)");
        addInst("addiu $sp, $sp, -1");
        addInst("# end of adding a number to stack");
    }

    public void addToStack(String s, int adr){                      // need changes
//        int adr = table.getAddress(s)*(-1);
        addInst("# start of adding variable to stack");
        addInst("lb $a0, " + adr + "($fp)");
        addInst("sb $a0, 0($sp)");
        addInst("addiu $sp, $sp, -1");
        addInst("# end of adding variable to stack");
    }

    public void addAddressToStack(String s, int adr) {              // need changes
//        int adr = table.getAddress(s)*(-1);
        addInst("# start of adding address to stack");
        addInst("addiu $a0, $fp, " + adr);
        addInst("sb $a0, 0($sp)");
        addInst("addiu $sp, $sp, -1");
        addInst("# end of adding address to stack");
    }

    public void addGlobalAddressToStack(String s, int adr){         // need changes
//        int adr = table.getAddress(s)*(-1);
        addInst("# start of adding global address to stack");
        addInst("addiu $a0, $gp, " + adr);
        addInst("sb $a0, 0($sp)");
        addInst("addiu $sp, $sp, -1");
        addInst("# end of adding global address to stack");
    }

    public void pushInt(int x) {
        addInst("# pushing " + x);
        byte[] result = new byte[4];
        result[0] = (byte)((x & 0xFF000000) >> 24);
        result[1] = (byte)((x & 0x00FF0000) >> 16);
        result[2] = (byte)((x & 0x0000FF00) >> 8);
        result[3] = (byte)(x & 0x000000FF);
        for(int i = 3; i >= 0; i--) addToStack(result[i]);
    }

    public void pushIntReg(String reg) {
        addInst("# push int register " + reg + " to stack");

        addInst("lw $a0, 0($gp)"); //backup M[$gp]
        addInst("sw " + reg + ", 0($gp)");
        for (int i = 0; i < 4; i++) {
            addInst("addi $a1, $gp, " + i);
            addInst("lb $a2, 0($a1)");
            addInst("sb $a2, " + (-i) + "($sp)");
        }
        addInst("addi $sp, $sp, -4");
        addInst("sw $a0, 0($gp)"); //restore M[$gp]
        addInst("");
    }

    public void pushCharReg(String reg) {
        addInst("# push char register " + reg + " to stack");

        addInst("sb " + reg + " 0($sp)");
        addInst("addi $sp, $sp, -1");
        addInst("");
    }

    public void assignIntVar(String srcReg, SymbolTableVariableItemBase v) {
        addInst("# assign int variable " + v.getVariable().getName() + " from register " + srcReg);

        addInst("lw $a0, 0($gp)"); //backup M[$gp]
        addInst("sw " + srcReg + ", 0($gp)");
        for (int i = 0; i < 4; i++) {
            addInst("addi $a1, $gp, " + i);
            addInst("lb $a2, 0($a1)");
            addInst("sb $a2, " + (v.getOffset() + i) + "(" + v.getBaseRegister().toString() + ")");
        }
        addInst("sw $a0, 0($gp)"); //restore M[$gp]
        addInst("");
    }

    public void assignCharVar(String srcReg, SymbolTableVariableItemBase v) {
        addInst("# assign char variable " + v.getVariable().getName() + " from register " + srcReg);
        addInst("sb " + srcReg + " " + v.getOffset() + "(" + v.getBaseRegister().toString() + ")");
        addInst("");
    }

    public void pushString(String x) {
        for(int i = 0; i < x.length(); i++)
            addToStack((byte)x.charAt(i));
    }

    public void popStack() {
        addInst("# pop stack");
        addInst("addiu $sp, $sp, 4");
        addInst("# end of pop stack");
    }

    public void popChar(boolean wannaPopTwo) {          // will save into v0 or (v1 and v0)
        addInst("# pop " + (wannaPopTwo ? "2 chars" : "1 char"));
        if(wannaPopTwo) {
            addInst("lb $v1, 1($sp)");
            addInst("lb $v0, 2($sp)");
            addInst("addiu $sp, $sp, 2");
        }
        else {
            addInst("lb $v0, 1($sp)");
            addInst("addiu $sp, $sp, 1");
        }
    }

    public void popInt(boolean wannaPopTwo) {          // will save into v0 or (v1 and v0)
        addInst("# pop " + (wannaPopTwo ? "2 ints" : "1 int"));
        int rep = 1;
        if(wannaPopTwo)
            rep++;
        for(; rep > 0; rep--) {
            addInst("li $s1, 0");
            for(int i = 0; i < 4; i++) {
                addInst("sll $s1, $s1, 8");
                addInst("lb $s0, 1($sp)");
                addInst("andi $s0, $s0, 255");
                addInst("add $s1, $s1, $s0");
                addInst("addiu $sp, $sp, 1");
            }
            if(rep == 1)
                addInst("move $v0, $s1");
            else
                addInst("move $v1, $s1");
        }
    }

    public void pushVariable(SymbolTableVariableItemBase v) {
        addInst("# push variable " + v.getVariable().getName());

        for (int i = 0; i < v.getSize(); i++) {
            addInst("lb $t0, " + (v.getOffset() + i) + "(" + v.getBaseRegister().toString() + ")");
            addInst("sb $t0, " + (-i) + "($sp)");
        }
        addInst("addi $sp, $sp, " + (-v.getSize()));
        addInst("");
    }

    public void doOperation(String op, String out) {
        addInst("# do operation " + op + " for type " + out);
        if (out.equals("int")) {
            if (op.equals("+")) {
                addInst("add $t0, $v0, $v1");
                pushIntReg("$t0");
            }
            else if (op.equals("-")) {
                addInst("sub $t0, $v0, $v1");
                pushIntReg("$t0");
            }
            else if (op.equals("*")) {
                addInst("mul $t0, $v0, $v1");
                pushIntReg("$t0");
            }
            else if (op.equals("/")) {
                addInst("div $t0, $v0, $v1");
                pushIntReg("$t0");
            }
            else if (op.equals("and")) {
                addInst("and $t0, $v0, $v1");
                pushIntReg("$t0");
            }
            else if (op.equals("or")) {
                addInst("or $t0, $v0, $v1");
                pushIntReg("$t0");
            }
            else if (op.equals("<")) {
                int label = getNextUniqueNum();
                addInst("sub $t0, $v0, $v1");
                addInst("bgez $t0, notLess_" + label);
                pushInt(1);
                addInst("j less_" + label);
                addInst("notLess_" + label + ":");
                pushInt(0);
                addInst("less_" + label + ":");
            }
            else if (op.equals(">")) {
                int label = getNextUniqueNum();
                addInst("sub $t0, $v0, $v1");
                addInst("blez $t0, notGreater_" + label);
                pushInt(1);
                addInst("j greater_" + label);
                addInst("notGreater_" + label + ":");
                pushInt(0);
                addInst("greater_" + label + ":");
            }
            else if (op.equals("==")) {
                int label = getNextUniqueNum();
                addInst("sub $t0, $v0, $v1");
                addInst("bnez $t0, notEqual_" + label);
                pushInt(1);
                addInst("j equal_" + label);
                addInst("notEqual_" + label + ":");
                pushInt(0);
                addInst("equal_" + label + ":");
            }
            else if (op.equals("<>")) {
                int label = getNextUniqueNum();
                addInst("sub $t0, $v0, $v1");
                addInst("bnez $t0, notEqual_" + label);
                pushInt(0);
                addInst("j equal_" + label);
                addInst("notEqual_" + label + ":");
                pushInt(1);
                addInst("equal_" + label + ":");
            }
            else if (op.equals("--")) {
                addInst("neg $t0, $v0");
                pushIntReg("$t0");
            }
            else if (op.equals("not")) {
                addInst("not $t0, $v0");
                pushIntReg("$t0");
            }
            else {
                addInst("################################# unrecognized operator " + op + " #####################################");

            }
        }
        addInst("");
    }

    public void assign(SymbolTableVariableItemBase[] variables, Type type) {
        if (type instanceof IntType) {
            popInt(false);

            for (SymbolTableVariableItemBase v : variables) {
                assignIntVar("$v0", v);
            }
        }
        else if (type instanceof CharType) {
            popChar(false);

            for (SymbolTableVariableItemBase v : variables) {
                assignCharVar("$v0", v);
            }
        }
        else {
            addInst("##################### assignment of type " + type.toString() + " not supported ###############################");
        }
    }

    public void assign(SymbolTableVariableItemBase v) {
        ArrayList<SymbolTableVariableItemBase> vs = new ArrayList<SymbolTableVariableItemBase>();
        vs.add(v);
        assign(vs.toArray(new SymbolTableVariableItemBase[vs.size()]), v.getVariable().getType());
    }

    public void addSystemCall(int x){
        addInst("# start syscall " + x);
        addInst("li $v0, " + x);
        addInst("syscall");
        addInst("# end syscall");
    }

    public void assignCommand(){
        addInst("# start of assign");
        addInst("lb $a0, 4($sp)");
        popStack();
        addInst("lb $a1, 4($sp)");
        popStack();
        addInst("sb $a0, 0($a1)");
        addInst("sb $a0, 0($sp)");
        addInst("addiu $sp, $sp, -4");
        popStack();
        addInst("# end of assign");
    }

    public void operationCommand(String s){
        addInst("# operation " + s);
        if (s.equals("*")){
            addInst("lb $a0, 4($sp)");
            popStack();
            addInst("lb $a1, 4($sp)");
            popStack();
            addInst("mul $a0, $a0, $a1");
            addInst("sb $a0, 0($sp)");
            addInst("addiu $sp, $sp, -4");
        }
        else if (s.equals("/")){
            addInst("lb $a0, 4($sp)");
            popStack();
            addInst("lb $a1, 4($sp)");
            popStack();
            addInst("div $a0, $a1, $a0");
            addInst("sb $a0, 0($sp)");
            addInst("addiu $sp, $sp, -4");
        }
        else if (s.equals("+")){
            addInst("lb $a0, 4($sp)");
            popStack();
            addInst("lb $a1, 4($sp)");
            popStack();
            addInst("add $a0, $a0, $a1");
            addInst("sb $a0, 0($sp)");
            addInst("addiu $sp, $sp, -4");
        }
        else if (s.equals("-")){
            addInst("lb $a0, 4($sp)");
            popStack();
            addInst("lb $a1, 4($sp)");
            popStack();
            addInst("sub $a0, $a1, $a0");
            addInst("sb $a0, 0($sp)");
            addInst("addiu $sp, $sp, -4");
        }
        addInst("# end of operation " + s);
    }

    public void write(){
        addInst("# writing");
        addInst("lb $a0, 4($sp)");
        this.addSystemCall(1);
        popStack();
        addInst("addi $a0, $zero, 10");
        this.addSystemCall(11);
        addInst("# end of writing");
    }

    public void addGlobalToStack(int adr){
//        int adr = table.getAddress(s)*(-1);
        addInst("# start of adding global variable to stack");
        addInst("lb $a0, " + adr + "($gp)");
        addInst("sb $a0, 0($sp)");
        addInst("addiu $sp, $sp, -4");
        addInst("# end of adding global variable to stack");
    }

    public void addGlobalVariable(int adr, int x){
//        int adr = table.getAddress(s)*(-1);
        addInst("# adding a global variable");
        addInst("li $a0, " + x);
        addInst("sb $a0, " + adr + "($gp)");
        addInst("# end of adding a global variable");
    }

    public void printInteger(int x) {
        addInst("li $a0, " + x);
        this.addSystemCall(1);
    }

    public void printInteger() { // prints integer located at $a0
        this.addSystemCall(1);
    }

    public void ifBegin(int uid) {
        addInst("# if statement");
        popInt(false);
        addInst("beqz $v0, ifStatement_" + uid + "_1");

    }

    public void ifElseifStatement1(int uid, int ctr) {
        addInst("j ifStatement_" + uid + "_final");
        addInst("# if elseif clause");
        addInst("ifStatement_" + uid + "_" + ctr + ":");
    }

    public void ifElseifStatement2(int uid, int ctr) {
        popInt(false);
        addInst("beqz $v0, ifStatement_" + uid + "_" + (ctr + 1));
    }

    public void ifElseStatement(int uid, int ctr) {
        addInst("j ifStatement_" + uid + "_final");
        addInst("# if else clause");
        addInst("ifStatement_" + uid + "_" + ctr + ":");
    }

    public void ifFinish(int uid, int ctr) {
        addInst("# if finished");
        addInst("ifStatement_" + uid + "_final:");
    }
}










































