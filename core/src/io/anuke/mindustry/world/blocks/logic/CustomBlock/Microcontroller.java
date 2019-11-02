package io.anuke.mindustry.world.blocks.logic.CustomBlock;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.logic.MessageBlock;
import io.anuke.mindustry.world.blocks.logic.MessageBlock.MessageBlockEntity;

import java.util.HashMap;

public class Microcontroller extends CustomBlock {
    Tile[] pins, porta, portb;
    Tile step, run, reset , solar, vcc, code, console, vars;
    Interpreter interpreter;
    boolean stepTrigger = false, resetTrigger;
    String codeText = "";
    long lastVarUpdate = 0, varUpdateDelay = 1000000;
    String carrotReg = "[lime]>\\[\\]", carrot = "[lime]>[]";

    Microcontroller(Tile tile){
        super(tile);
        porta = new Tile[4];
        portb = new Tile[4];
        pins = new Tile[8];
        for (int i = 0; i < 4; i++){
            pins[i] = to(2+2*i,1);
            porta[i] = pins[i];
            pins[i+4] = to(2+2*i,-1);
            portb[i] = pins[i+4];
        }
        Tile[] msgs = new Tile[]{code = to(1,0),console = to(2,0), vars = to(3,0),to(4,0), to(6,0), to(8,0)};
        for (Tile t : pins) addComponent(t, Blocks.powerNode);
        for (Tile t : msgs) addComponent(t, Blocks.message);

        addComponents(Blocks.powerNode, pins);
        addComponents(Blocks.message, msgs);
        addComponents(Blocks.powerNode, step = to(0,-1), run = to(0,1), reset = to(-1,0), vcc = to(5,0));
        addComponent(solar = to(7,0), Blocks.solarPanel);
        interpreter = new Interpreter();
        ds = new DeskSquared(run,1, this);
    }

    @Override
    void logic(){
        checkLink(solar,vcc);
        if (codeText != entity.message || digitalRead(reset)){
            for (int i = 0;i < entity.lines.length; i++)
                entity.lines[i] = entity.lines[i].replace(carrot,"");
            codeText = entity.message;
            interpreter.loadCode(((MessageBlockEntity)code.entity()).lines);
        }

        boolean read = digitalRead(reset);
        if (read != resetTrigger){
            if (read) interpreter.loadCode(((MessageBlockEntity)code.entity()).lines);
            resetTrigger = read;
        }
        read = digitalRead(step);
        if (read != stepTrigger){
            if (read) {
                interpreter.step();
                updateCodeText();
                updateVarsText();
            }
            stepTrigger = read;
        }
    }

    void updateCodeText(){
        String codeMsg = "";
        String rawLines[] = ((MessageBlockEntity)code.entity()).message.split("\n");
        for (int z = 0; z < rawLines.length;z++){
            rawLines[z]=rawLines[z].replace(carrot, "");
            codeMsg += (z == interpreter.i-1 ? carrot : "") + rawLines[z] + "\n";
        }
        Call.setMessageBlockText(null,code,constrainMsg(codeMsg));
    }

    void updateVarsText(){
        varUpdateDelay = 100000000;
        if (System.nanoTime()-lastVarUpdate<varUpdateDelay) return;
        lastVarUpdate = System.nanoTime();
        String varsMsg = "[goldenrod]Variables:\n[]";
        for (String s : interpreter.vals.keySet()) varsMsg += "[accent]" + s + ":[] " + interpreter.vals.get(s) + "\n";
        Call.setMessageBlockText(null,vars,constrainMsg(varsMsg));
    }

    class Interpreter {
        String[] s, instructions;
        String[][] code;
        String consoleMsg;
        HashMap<String, Integer> vals;
        int i = 0;

        Interpreter() {
            vals = new HashMap<>();
        }

        void step() {
            try{
                if (i>=code.length-1) i = 0;
                s = code[i];

                if (s.length >= 3){
                    if(s[2].contains("portc")) vals.put("portc", digitalRead(pins));
                    else if (s[2].contains("porta")) vals.put("porta", digitalRead(porta));
                    else if (s[2].contains("portb")) vals.put("portb", digitalRead(portb));
                }

                switch(instructions[i++]){
                    case "MOV":
                        vals.put(s[1], s2());
                        break;
                    case "ADD":
                        vals.put(s[1], s1() + s2());
                        break;
                    case "SUB":
                        vals.put(s[1], s1() - s2());
                        break;
                    case "MUL":
                        vals.put(s[1], s1() * s2());
                        break;
                    case "DIV":
                        vals.put(s[1], s1() / s2());
                        break;
                    case "MOD":
                        vals.put(s[1], s1() % s2());
                    case "XOR":
                        vals.put(s[1], s1() ^ s2());
                        break;
                    case "OR":
                        vals.put(s[1], s1() | s2());
                        break;
                    case "AND":
                        vals.put(s[1], s1() & s2());
                        break;
                    case "JZ":
                        setJmpRegister();
                        if(s1() != 0) i = s2();
                        break;
                    case "JL":
                        setJmpRegister();
                        if(s1() < s2()) i = s3();
                        break;
                    case "JG":
                        setJmpRegister();
                        if(s1() > s2()) i = s3();
                        break;
                    case "JE":
                        setJmpRegister();
                        if(s1() == s2()) i = s3();
                        break;
                    case "JMP":
                        setJmpRegister();
                        i = s1();
                        break;
                    case "PL":
                        if(s[1].contains("\"")) printLine(sString(s));
                        else printLine(s1() + "");
                        break;
                    case "LN":
                        vals.put(s[1], i);
                        break;
                    case "DR":
                        vals.put(s[1], digitalRead(pins[Math.max(0,Math.min(s2(),8))])?1:0);
                        break;
                    case "DW":
                        digitalWrite(s2()>0, pins[Math.max(0,Math.min(s1(),8))],vcc);
                        break;
                    case "AR":
                        vals.put(s[1], analogRead(pins[Math.max(0,Math.min(s2(),8))]));
                        break;
                    case "PORTC":
                        vals.put("portc", digitalRead(pins));
                        break;
                    case "PORTA":
                        vals.put("porta", digitalRead(porta));
                        break;
                    case "PORTB":
                        vals.put("portb", digitalRead(portb));
                        break;
                    case "RET":
                        i = vals.get("jmp");
                        break;
                    case "SW":
                        ds.writeData(s1(),s2());
                        break;
                    case "SR":
                        vals.put(s[1], ds.readData(s2()));
                        break;
                }
            }
            catch(Exception e){
                consoleOut("[scarlet]ERROR: " + i + " - " + e.getMessage());
            }

            if (s.length >= 2){
                if(s[1].contains("portc")) digitalWriteFromNodes(vals.get("portc"), pins, vcc);
                else if (s[1].contains("porta")) digitalWriteFromNodes(vals.get("porta"), porta, vcc);
                else if (s[1].contains("portb")) digitalWriteFromNodes(vals.get("portb"), portb, vcc);
            }
        }

        void setJmpRegister(){
            vals.put("jmp", i-1);
        }

        void printLine(String s) {
            consoleOut(s+"\n");
        }

        void consoleOut(String s){
            consoleMsg += s;
            if (consoleMsg.length()> MessageBlock.maxTextLength) consoleMsg = consoleMsg.substring(consoleMsg.length()-MessageBlock.maxTextLength);
            Call.setMessageBlockText(null,console,constrainMsg(consoleMsg));
        }

        String sString(String []s){
            String ret = "";
            for (int i = 1; i < s.length; i++) ret += s[i] + " ";
            return ret;
        }

        int s1() {
            return getVal(s[1]);
        }
        int s2() {
            return getVal(s[2]);
        }
        int s3() {
            return getVal(s[3]);
        }

        int getVal(String s) {
            return vals.containsKey(s) ? vals.get(s) : Integer.parseInt(s);
        }

        void loadCode(String rawLines[]) {
            consoleMsg = "";
            consoleOut("");
            vals = new HashMap<>();
            code = new String[rawLines.length][];
            instructions = new String[rawLines.length];
            vals.put("porta",0);
            vals.put("portb",0);
            vals.put("portc",0);
            i = 0;
            for (String line : rawLines) {
                s = line.split(" ");
                code[i] = s;
                instructions[i] = s[0].replace(carrot,"").toUpperCase();
                if ((s.length>1) && !isNumeric(s[1]) && (!vals.containsKey(s[1])) && !s[1].contains("#") && !s[1].contains("\"")) {
                    if (instructions[i].equals("LN")) vals.put(s[1], i);
                    else vals.put(s[1], 0);
                }
                i++;
            }
            i = 0;
            updateCodeText();
            updateVarsText();
        }

        boolean isNumeric(String strNum) {
            return strNum.matches("-?\\d+(\\.\\d+)?");
        }
    }
}