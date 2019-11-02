package io.anuke.mindustry.world.blocks.logic.CustomBlock;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.logic.MessageBlock;
import io.anuke.mindustry.world.blocks.logic.MessageBlock.MessageBlockEntity;

class MUX extends CustomBlock{
    Tile[] port, solars, walls;
    Tile address, value;

    MUX(Tile tile){
        super(tile);
        addComponent(address = to(0,1), Blocks.powerNode);
        addComponent(value = to(0,-1), Blocks.powerNode);
        port = new Tile[16];
        solars = new Tile[16];
        walls = new Tile[16];
        for (int i = 0; i < 8; i++){
            addComponent(port[i] = to(1+i*2,2), Blocks.powerNode);
            addComponent(port[i+8] = to(2+i*2,-1), Blocks.powerNode);
            addComponent(solars[i] = to(1+i*2,0), Blocks.solarPanel);
            addComponent(solars[i+8] = to(2+i*2,1), Blocks.solarPanel);
            addComponent(walls[i] = to(1+i*2,1), Blocks.copperWall);
            addComponent(walls[i+8] = to(2+i*2,0), Blocks.copperWall);
        }
    }
    @Override
    void logic(){
        int addr = Math.max(0,Math.min(15,analogRead(address)));
        digitalWrite(digitalRead(value), solars[addr], port[addr]);
    }
}

class AMUX extends CustomBlock{
    Tile[] pinsOut;
    Tile[] pinsIn;
    Tile signal, external;

    AMUX(Tile tile){
        super(tile);
        addComponent(signal = to(1, 0), Blocks.powerNode);
        addComponent(external = to(6, 5), Blocks.powerNode);
        pinsIn = new Tile[]{to(3, 0), to(5, 0), to(7, 0), to(9, 0)};
        for(Tile t : pinsIn) addComponent(t, Blocks.powerNode);
        pinsOut = new Tile[16];
        for(int i = 0; i < 16; i++)
            addComponent(pinsOut[i] = to((i % 4) * 2 + 2 + (i / 4 % 2), (i / 4) + 1), Blocks.powerNode);
    }

    @Override
    void logic(){
        digitalWrite(digitalRead(signal), pinsOut[digitalRead(pinsIn)], external);
    }
}

class ADC extends CustomBlock{
    Tile[] pins;
    Tile analogIn, vcc;
    ADC(Tile tile){
        super(tile);
        pins = new Tile[]{ to(1,0), to(3,0), to(5,0), to(7,0)};
        addComponents(Blocks.powerNode, pins);
        addComponent(analogIn = to(0,1), Blocks.powerNode);
        addComponent(vcc = to(2,1), Blocks.solarPanel);
    }
    @Override
    void logic(){
        digitalWriteFromNodes(analogRead(analogIn), pins, vcc);
    }
}

class DAC extends CustomBlock{
    Tile[] pins, panels, panelAttach;
    Tile dacOut;
    DAC(Tile tile){
        super(tile);
        panels = new Tile[]{
        to(1,3), to(1,5), to(1,6),
        to(2,1), to(2,2), to(2,4),to(2,5),to(2,6),
        to(3,3), to(3,5), to(3,6),
        to(4,1), to(4,2), to(4,3), to(4,5)};
        panelAttach = new Tile[]{ panels[0], panels[4], panels[12] , panels[2]};
        pins = new Tile[]{ to(1,0), to(3,0), to(5,0), to(7,0)};
        addComponents(Blocks.powerNode, pins);
        addComponents(Blocks.solarPanel, panels);
        addComponent(dacOut = to(5,6), Blocks.powerNode);
    }
    @Override
    void logic(){
        digitalWriteToNode(digitalRead(pins), panelAttach, dacOut);
    }
}

class FiloMemory extends CustomBlock{
    Tile clk, dataIn, clr, dataOut,dataOut2, sel, msg, msg2;
    String mem;
    boolean clkHigh;
    String schematicTxt;
    MessageBlockEntity schematic;

    FiloMemory(Tile tile){
        super(tile);
        addComponents(Blocks.powerNode, clk = to(-1,0), dataIn = to(0,-1), clr = to(0,1), dataOut = to(2,-1), dataOut2 = to(2,1), sel  = to(3,0));
        addComponents(Blocks.message, msg, msg2);
        preventLinks = new Tile[]{clk,dataIn,clr,dataOut,sel};
        mem = "1";
        schematicTxt = "[slate]___[scarlet]clr[slate]_____[tan]dataOut\n" +
        "[lime]clk[slate]-+----------+-[royal]sel\n" +
        "[slate]_[goldenrod]dataIn[slate]____[tan]dataOut";
    }

    @Override
    void logic(){
        if (digitalRead(clk)){
            if (!clkHigh){
                if (digitalRead(sel)){ //if write selected
                    mem = mem + (digitalRead(dataIn) ? 1 : 0);
                    if(mem.length() > MessageBlock.maxTextLength - 1) mem = mem.substring(0, MessageBlock.maxTextLength - 1);
                    Call.setMessageBlockText(null, msg, mem);
                }
                else if (mem.length()>0) readMem();
            }
            clkHigh = true;
        }
        else if (clkHigh) clkHigh = false;
        //if (digitalRead(clr) && (mem!="")) Call.setMessageBlockText(null, msg, mem = "1");
        if ((schematic != msg2.entity()) && ((schematic = msg2.entity()).message != schematicTxt)) // if schematic is null get it, else if its incorrect set it
            Call.setMessageBlockText(null, msg2, schematicTxt);
    }

    void readMem(){
        digitalWrite(mem.charAt(0) == '1', dataOut, dataOut2);
        try{
            if (mem.length()>1){
                mem = mem.substring(1);
            }
        }
        catch(Exception e){
            System.out.println("test");
        }
        Call.setMessageBlockText(null, msg, mem);
    }
}

class FifoMemory extends FiloMemory{
    boolean data;
    FifoMemory(Tile tile){
        super(tile);
    }
    @Override
    void readMem(){
        if (mem.length()>1) data = mem.charAt(mem.length()-1) == '1';
        else data = true;
        digitalWrite(data, dataOut, dataOut2);
        if (mem.length()>1) mem = mem.substring(0,mem.length()-1);
        Call.setMessageBlockText(null, msg, mem);
    }
}