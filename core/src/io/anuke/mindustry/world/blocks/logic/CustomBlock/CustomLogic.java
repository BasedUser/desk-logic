package io.anuke.mindustry.world.blocks.logic.CustomBlock;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.world.Tile;

public class CustomLogic extends CustomBlock{ // template with 4 powernodes
    Tile in, in2, out, out2;
    CustomLogic(Tile tile){
        super(tile);
        addComponents(Blocks.powerNode, in = tile.getNearby(0), in2 = tile.getNearby(2), out = tile.getNearby(1),out2 = tile.getNearby(3));
    }
}

class Switch extends CustomLogic{
    boolean read;
    Switch(Tile tile){
        super(tile);
        preventLinks = new Tile[]{out,out2};
    }
    @Override
    void logic(){
        read = digitalRead(in2);
        digitalWrite(read,out,in);
        digitalWrite(!read, out2, in);
    }
}

class Or extends CustomLogic{
    Or(Tile tile){
        super(tile);
    }
    @Override
    void logic(){
        digitalWrite(digitalRead(in) || digitalRead(in2), out, out2);
    }
}

class And extends CustomLogic{
    And(Tile tile){
        super(tile);
    }
    @Override
    void logic(){
        digitalWrite(digitalRead(in) && digitalRead(in2), out, out2);
    }
}

class Nor extends CustomLogic{
    Nor(Tile tile){
        super(tile);
    }
    @Override
    void logic(){
        digitalWrite(!(digitalRead(in) || (digitalRead(in2))), out, out2);
    }
}

class Nand extends CustomLogic{
    Nand(Tile tile){
        super(tile);
    }
    @Override
    void logic(){
        digitalWrite(!(digitalRead(in) && (digitalRead(in2))),out, out2);
    }
}

class Xor extends CustomLogic{
    Xor(Tile tile){
        super(tile);
    }
    @Override
    void logic(){
        digitalWrite(digitalRead(in) != digitalRead(in2),out, out2);
    }
}

class Xnor extends CustomLogic{
    Xnor(Tile tile){
        super(tile);
    }
    @Override
    void logic(){
        digitalWrite(!((digitalRead(in) != digitalRead(in2))), out, out2);
    }
}

class Greater extends CustomLogic{
    Greater(Tile tile){
        super(tile);
    }
    @Override
    void logic(){
        digitalWrite(analogRead(in2) > analogRead(in), out, out2);
    }
}