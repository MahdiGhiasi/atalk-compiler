/**
 * Created by vrasa on 12/26/2016.
 */

import java.util.*;
import java.io.*;

public class Translator {

    private File output;
    private ArrayList <String> instructions;
    private ArrayList <String> initInstructions;

    int actorCounter = 0;

    public Translator(){
        instructions = new ArrayList<String>();
        initInstructions = new ArrayList<String>();
        output = new File("out.asm");
        try {
            output.createNewFile();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void makeOutput(){
        this.addSystemCall(10);
        try {
            PrintWriter writer = new PrintWriter(output);
            writer.println("main:");
            writer.println("move $fp, $sp");
            for (int i=0;i<initInstructions.size();i++){
                writer.println(initInstructions.get(i));
            }
            for (int i=0;i<instructions.size();i++){
                writer.println(instructions.get(i));
            }
            writer.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void putInit() {
        initInstructions.add("li $k0, 1");
        initInstructions.add("");
    }

    public void putActorMailboxHandler(SymbolTableActorItem i) {
        instructions.add("# actor " + i.getActor().getName());
        instructions.add("actor" + actorCounter + "Check:");
        instructions.add("lw $t0, " + ((SymbolTableGlobalVariableItem)i.getActor().getSymbolTable().get("__head")).getOffset() + "($gp)");
        instructions.add("lw $t1, " + ((SymbolTableGlobalVariableItem)i.getActor().getSymbolTable().get("__tail")).getOffset() + "($gp)");

        if (actorCounter == 0) {
            instructions.add("bz $k0, dontTerminate");
            addSystemCall(10); //Exit
            instructions.add("dontTerminate:");
            instructions.add("li $k0, 1");
        }

        instructions.add("beq $t0, $t1, actor" + (actorCounter + 1) + "Check");
        instructions.add("li $k0, 0");
        instructions.add("lw $t2, " + ((SymbolTableGlobalVariableItem)i.getActor().getSymbolTable().get("__mailbox")).getOffset() + "($t0)");
        instructions.add("");

        int receiverIndex = 0;

        for (SymbolTableItem item : i.getActor().getSymbolTable().items.values()) {
            if (item instanceof SymbolTableReceiverItem) {
                SymbolTableReceiverItem r = (SymbolTableReceiverItem) item;

                instructions.add("# receiver " + r.getReceiver().toString());
                instructions.add("bnez $t2, actor" + actorCounter + "Receiver" + receiverIndex + "After");
                instructions.add("li $s0, " + r.getReceiver().getArgumentsTotalSize());
                instructions.add("li $s1, " + ((SymbolTableGlobalVariableItem) i.getActor().getSymbolTable().get("__mailbox")).getOffset());
                instructions.add("move $s2, $t0");
                instructions.add("li $s3, " + i.getActor().getCapacity());
                instructions.add("jal Copy");
                instructions.add("sw $s2, " + ((SymbolTableGlobalVariableItem) i.getActor().getSymbolTable().get("__head")).getOffset() + "($gp)");
                instructions.add("j [location of receiver] #TODO"); //TODO
                instructions.add("");
                instructions.add("actor" + actorCounter + "Receiver" + receiverIndex + "After:");
                instructions.add("addi $t2, t2, -1");
                instructions.add("");

                receiverIndex++;
            }
        }

        //TODO: Fohsh
        addSystemCall(10); //Exit

    }

    public void addToStack(int x){
        instructions.add("# adding a number to stack");
        instructions.add("li $a0, " + x);
        instructions.add("sw $a0, 0($sp)");
        instructions.add("addiu $sp, $sp, -4");
        instructions.add("# end of adding a number to stack");

    }

    public void addToStack(String s, int adr){
//        int adr = table.getAddress(s)*(-1);
        instructions.add("# start of adding variable to stack");
        instructions.add("lw $a0, " + adr + "($fp)");
        instructions.add("sw $a0, 0($sp)");
        instructions.add("addiu $sp, $sp, -4");
        instructions.add("# end of adding variable to stack");
    }

    public void addAddressToStack(String s, int adr) {
//        int adr = table.getAddress(s)*(-1);
        instructions.add("# start of adding address to stack");
        instructions.add("addiu $a0, $fp, " + adr);
        instructions.add("sw $a0, 0($sp)");
        instructions.add("addiu $sp, $sp, -4");
        instructions.add("# end of adding address to stack");
    }

    public void addGlobalAddressToStack(String s, int adr){
//        int adr = table.getAddress(s)*(-1);
        instructions.add("# start of adding global address to stack");
        instructions.add("addiu $a0, $gp, " + adr);
        instructions.add("sw $a0, 0($sp)");
        instructions.add("addiu $sp, $sp, -4");
        instructions.add("# end of adding global address to stack");
    }

    public void popStack(){
        instructions.add("# pop stack");
        instructions.add("addiu $sp, $sp, 4");
        instructions.add("# end of pop stack");
    }

    public void addSystemCall(int x){
        instructions.add("# start syscall " + x);
        instructions.add("li $v0, " + x);
        instructions.add("syscall");
        instructions.add("# end syscall");
    }

    public void assignCommand(){
        instructions.add("# start of assign");
        instructions.add("lw $a0, 4($sp)");
        popStack();
        instructions.add("lw $a1, 4($sp)");
        popStack();
        instructions.add("sw $a0, 0($a1)");
        instructions.add("sw $a0, 0($sp)");
        instructions.add("addiu $sp, $sp, -4");
        popStack();
        instructions.add("# end of assign");
    }

    public void operationCommand(String s){
        instructions.add("# operation " + s);
        if (s.equals("*")){
            instructions.add("lw $a0, 4($sp)");
            popStack();
            instructions.add("lw $a1, 4($sp)");
            popStack();
            instructions.add("mul $a0, $a0, $a1");
            instructions.add("sw $a0, 0($sp)");
            instructions.add("addiu $sp, $sp, -4");
        }
        else if (s.equals("/")){
            instructions.add("lw $a0, 4($sp)");
            popStack();
            instructions.add("lw $a1, 4($sp)");
            popStack();
            instructions.add("div $a0, $a1, $a0");
            instructions.add("sw $a0, 0($sp)");
            instructions.add("addiu $sp, $sp, -4");
        }
        else if (s.equals("+")){
            instructions.add("lw $a0, 4($sp)");
            popStack();
            instructions.add("lw $a1, 4($sp)");
            popStack();
            instructions.add("add $a0, $a0, $a1");
            instructions.add("sw $a0, 0($sp)");
            instructions.add("addiu $sp, $sp, -4");
        }
        else if (s.equals("-")){
            instructions.add("lw $a0, 4($sp)");
            popStack();
            instructions.add("lw $a1, 4($sp)");
            popStack();
            instructions.add("sub $a0, $a1, $a0");
            instructions.add("sw $a0, 0($sp)");
            instructions.add("addiu $sp, $sp, -4");
        }
        instructions.add("# end of operation " + s);
    }

    public void write(){
        instructions.add("# writing");
        instructions.add("lw $a0, 4($sp)");
        this.addSystemCall(1);
        popStack();
        instructions.add("addi $a0, $zero, 10");
        this.addSystemCall(11);
        instructions.add("# end of writing");
    }

    public void addGlobalToStack(int adr){
//        int adr = table.getAddress(s)*(-1);
        instructions.add("# start of adding global variable to stack");
        instructions.add("lw $a0, " + adr + "($gp)");
        instructions.add("sw $a0, 0($sp)");
        instructions.add("addiu $sp, $sp, -4");
        instructions.add("# end of adding global variable to stack");
    }

    public void addGlobalVariable(int adr, int x){
//        int adr = table.getAddress(s)*(-1);
        initInstructions.add("# adding a global variable");
        initInstructions.add("li $a0, " + x);
        initInstructions.add("sw $a0, " + adr + "($gp)");
        initInstructions.add("# end of adding a global variable");
    }
}










































