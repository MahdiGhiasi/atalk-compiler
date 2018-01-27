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
            addInst("li $k0, 1");
        }

        addInst("beq $t0, $t1, actor" + (actorCounter + 1) % totalActorsCount + "Check");
        addInst("li $k0, 0");
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
            addInst("bnez $t2, actor" + actorCounter + "Receiver" + r.getReceiver().getIndex() + "AfterCheck");
            addInst("li $s0, " + r.getReceiver().getArgumentsTotalSize());
            addInst("li $s1, " + ((SymbolTableGlobalVariableItem) i.getActor().getSymbolTable().get("__mailbox")).getOffset());
            addInst("move $s2, $t0");
            addInst("li $s3, " + i.getActor().getCapacity());
            addInst("jal Copy");
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

        putCopyFunction();
        //TODO: ActorBufferOverflowError

        addInst("");
    }

    public void putCopyFunction() {
        // COPY
        // s0: length of data to copy
        // s1: beginning of mailbox
        // s2: start position relative to s1
        // s3: size of mailbox

        addInst("# - Copy");

        addInst("Copy:");
        addInst("move $s4, $sp"); //dest

        addInst("CopyLoopBegin:");

        addInst("beqz $s0, CopyLoopBodyBegin");
        addInst("jr $ra");

        addInst("CopyLoopBodyBegin:");

        // s4 <- [s1 + s2]
        addInst("add $s5, $s1, $s2");
        addInst("lb $s6, 0($s5)");
        addInst("sb $s6, 0($s4)");

        addInst("addi $s2, $s2, 1");
        addInst("remu $s2, $s2, $s3"); // mod of s2

        addInst("addi $s4, $s4, 1");
        addInst("addi $s0, $s0, -1");
        addInst("beqz $zero, CopyLoopBegin");

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

        addInst("lb $t0, " + head.getOffset() + "($gp)");
        addInst("lb $t1, " + tail.getOffset() + "($gp)");
        addInst("lb $t2, " + mailbox.getOffset() + "($gp)");

        //increase tail by 1
        addInst("addi $t1, $t1, 1");

        int labelIndex = getNextUniqueNum();
        addInst("bne $t1, $t0, senderCheckSuccess" + labelIndex);
        addInst("jal ActorBufferOverflowError");
        addInst("j senderCheckFinish" + labelIndex);

        addInst("senderCheckSuccess" + labelIndex + ":");

        //store receiver number
        addInst("add $t3, $t2, $gp"); //t3 <- mailbox offset + $gp
        addInst("add $t3, $t3, $t1"); //t3 <- t3 + tail offset = $gp + mailbox offset + tail offset
        addInst("li $t4, " + receiver.getReceiver().getIndex());
        addInst("sb $t4, 0($t3)");

        //save new tail value
        addInst("sb $t1, " + tail.getOffset() + "($gp)");

        //TODO: arguments

        addInst("senderCheckFinish" + labelIndex + ":");
        addInst("");
    }


    public void addToStack(int x){
        addInst("# adding a number to stack");
        addInst("li $a0, " + x);
        addInst("sb $a0, 0($sp)");
        addInst("addiu $sp, $sp, -4");
        addInst("# end of adding a number to stack");

    }

    public void addToStack(String s, int adr){
//        int adr = table.getAddress(s)*(-1);
        addInst("# start of adding variable to stack");
        addInst("lb $a0, " + adr + "($fp)");
        addInst("sb $a0, 0($sp)");
        addInst("addiu $sp, $sp, -4");
        addInst("# end of adding variable to stack");
    }

    public void addAddressToStack(String s, int adr) {
//        int adr = table.getAddress(s)*(-1);
        addInst("# start of adding address to stack");
        addInst("addiu $a0, $fp, " + adr);
        addInst("sb $a0, 0($sp)");
        addInst("addiu $sp, $sp, -4");
        addInst("# end of adding address to stack");
    }

    public void addGlobalAddressToStack(String s, int adr){
//        int adr = table.getAddress(s)*(-1);
        addInst("# start of adding global address to stack");
        addInst("addiu $a0, $gp, " + adr);
        addInst("sb $a0, 0($sp)");
        addInst("addiu $sp, $sp, -4");
        addInst("# end of adding global address to stack");
    }

    public void popStack(){
        addInst("# pop stack");
        addInst("addiu $sp, $sp, 4");
        addInst("# end of pop stack");
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
}










































