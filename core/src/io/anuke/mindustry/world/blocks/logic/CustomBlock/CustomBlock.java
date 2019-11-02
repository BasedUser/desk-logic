package io.anuke.mindustry.world.blocks.logic.CustomBlock;

import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Build;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.mindustry.world.blocks.logic.MessageBlock;
import io.anuke.mindustry.world.blocks.logic.MessageBlock.MessageBlockEntity;
import io.anuke.mindustry.world.blocks.power.PowerGraph;
import io.anuke.mindustry.world.blocks.power.PowerNode;

import java.util.ArrayList;

import static io.anuke.mindustry.Vars.*;

public class CustomBlock{
    public ArrayList<Component> components;
    String on = "[lime]on",
    off = "[scarlet]off";
    public Tile tile;
    Team team;
    long nanoWait, lastSwitch;
    Tile preventLinks[];
    boolean state, prevState;
    public MessageBlockEntity entity;
    DeskSquared ds;

    public CustomBlock(Tile tile){
        this.tile = tile;
        team = tile.getTeam();
        nanoWait = 1;
        components = new ArrayList<>();
        entity = (MessageBlockEntity)tile.entity();
    }

    public void addComponent(Tile t, Block b){
        components.add(new Component(t, b));
    }

    public void addComponents(Block b, Tile ...tiles){
        for (Tile t : tiles) components.add(new Component(t, b));
    }

    public boolean validPlace(){
        if (components.size()==0) return true;
        for (Component component : components){
            if (!(component.tile.block().getClass().isInstance(component.block) && (component.tile.getTeam() == team))  && (!Build.validPlace(team,component.tile.x,component.tile.y,component.block,0))) return false;
        }
        return true;
    }

    public static CustomBlock selectCustom(String message, Tile tile){
        try{
            String []type = message.split(";");
            switch(type[0].toLowerCase()){
                case "liquidgate": return new LiquidGate(tile);
                case "gate": return new Gate(tile);
                case "diode": return new Diode(tile);
                case "and": return new And(tile);
                case "or": return new Or(tile);
                case "nor": return new Nor(tile);
                case "xor": return new Xor(tile);
                case "xnor": return new Xnor(tile);
                case "nand": return new Nand(tile);
                case "greater": return new Greater(tile);
                case "light": return new Light(tile);
                case "power": return new PowerInfo(tile);
                case "scale": return new Scale(tile);
                case "breaker": return new Breaker(tile);
                case "liquidscale": return new LiquidScale(tile);
                //case "memory": return new FiloMemory(tile);
                //case "fifomemory": return new FifoMemory(tile);
                case "switch": return new Switch(tile);
                case "rising": return new RisingTrigger(tile);
                case "falling": return new FallingTrigger(tile);
                case "mcu": return new Microcontroller(tile);
                case "oscillator": return new Oscillator(tile);
                case "dac": return new DAC(tile);
                case "adc": return new ADC(tile);
                case "amux": return new AMUX(tile);
                case "mux": return new MUX(tile);
                case "doordisplay": return new DoorDisplay(tile);
                case "doordisplay16": return new DoorDisplay16(tile);
                case "bit": return new Bit(tile);
                case "sorterdisplay16": return new SorterDisplay16(tile);
                case "storagesensor": return new StorageSensor(tile);
                case "sorterdisplayd2": return new SorterDisplayD2(tile);
                case "powersensor": return new PowerSensor(tile);
            }
        }
        catch(Exception e){
        }
        return null;
    }

    public void update(){
        if (System.nanoTime()-lastSwitch<nanoWait) return;
        lastSwitch = System.nanoTime();
        if (safeCheck()){
            preventLinks();
            logic();
        }
        else removeSelf();
    }

    public String constrainMsg(String m){
        return m.substring(0, Math.min(m.length(), MessageBlock.maxTextLength - 1));
    }

    public void preventLinks(){
        if (preventLinks==null) return;
        for (Tile t : preventLinks){
            for (Tile t2 : preventLinks){
                if (!t.equals(t2)) checkUnlink(t,t2);
            }
        }
    }

    public boolean safeCheck(){
        if (components!=null)
            for (Component c : components) if (!c.checkBuild()) return false;
        return true;
    }

    void logic(){
    }

    public void removeSelf(){
        Call.onDeconstructFinish(tile, Blocks.message,0);
    }

    public void removeComponents(){
        if (components == null || components.size()==0) return;
        for (Component c : components)  Call.onDeconstructFinish(c.tile, c.block,0);
    }

    public Tile to(int x, int y){
        return world.tile(tile.x+x,tile.y+y);
    }

    public Tile nearbyBlock(Tile tile, Class blockType){ // doesnt work
        try{ // remove try catch if possible
            Tile other = null;
            for(int i = 0; i < 4; i++){
                other = tile.getNearby(i);
                if(other.isLinked()) other = other.link(); // should only get parent block if multiblock
                if ((other != null) && (other.block().getClass().isInstance(blockType))) return other; //redundant null check? sketchy class type check
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public Tile nearbyPowerBlock(Tile t){
        Tile other = null;
        for(int i = 0; i < 4; i++){
            other = t.getNearby(i);
            if(other.isLinked()) other = other.link(); // should only get parent block if multiblock
            if ((other != null) && (other.block() instanceof PowerBlock)) return other; //redundant null check? sketchy class type check
        }
        return null;
    }

    public void checkUnlink(Tile powerTile1, Tile powerTile2){ // unlink tiles if they are linked
        if (powerTile1==null || powerTile1.entity()==null || powerTile1.entity().power==null || powerTile1.entity().power.links==null) return;
        try{
            if(powerTile1.entity().power.links.contains(powerTile2.pos()))
                Call.unlinkPowerNodes(null, powerTile1, powerTile2);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void checkLink(Tile powerTile1, Tile powerTile2){ // link tiles if they are not linked
        if (powerTile1==null || powerTile1.entity()==null || powerTile1.entity().power==null || powerTile1.entity().power.links==null) return;
        try{
            if(!powerTile1.entity().power.links.contains(powerTile2.pos()))
                Call.linkPowerNodes(null, powerTile2, powerTile1);

        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getBit(int n, int k) {
        return ((n >> k) & 1)>0;
    }

    public boolean digitalRead(Tile t){
        return (powerBalance(t)>0);
    }

    public int digitalRead(Tile tiles[]){
        int accum = 0;
        for (int i = tiles.length-1; i >= 0; i--){
            accum = accum << 1;
            if (digitalRead(tiles[i])) accum++;
        }
        return accum;
    }

    public float powerBalance(Tile tile){
        return  tile.entity().power.graph.lastPowerProduced - tile.entity().power.graph.lastPowerNeeded;
    }

    public void digitalWrite(boolean write, Tile t, Tile t2){
        if (write) checkLink(t, t2);
        else checkUnlink(t, t2);
    }

    public void digitalWrite(int write, Tile t[], Tile t2){
        if (t2.block() instanceof PowerNode) digitalWriteToNode(write, t, t2);
        else digitalWriteFromNodes(write, t, t2);
    }

    public void digitalWriteFromNodes(int write, Tile t[], Tile t2){
        for (int i = 0; i < t.length; i++){
            if((write & 1) > 0) checkLink(t2, t[i]);
            else checkUnlink(t2, t[i]);
            write = write >> 1;
        }
    }
    public void digitalWriteToNode(int write, Tile t[], Tile t2){
        for (int i = 0; i < t.length; i++){
            if((write & 1) > 0) checkLink(t[i], t2);
            else checkUnlink(t[i], t2);
            write = write >> 1;
        }
    }

    public int analogRead(Tile t){
        if (t.entity().power == null || t.entity().power.graph == null) return 0;
        PowerGraph pg = t.entity().power.graph;
        float produced = 60 * pg.lastPowerProduced/pg.lastDelta;
        float needed = 60 * pg.lastPowerNeeded/pg.lastDelta;
        return Mathf.round((produced-needed)/3.6f);
    }

    public boolean isLinked(Tile t, Tile t2){
        return (t.entity().power.links.contains(t2.pos()));
    }

    public int extractSub(final int l, final int nrBits, final int offset){
        final int rightShifted = l >>> offset;
        final int mask = (1 << nrBits) - 1;
        return rightShifted & mask;
    }

    public int modifyBit(int num, int pos, int bin){
        int mask = 1 << pos;
        return (num & ~mask) | ((bin << pos) & mask);
    }

    public int getArgument(int arg){
        try{
            return Integer.parseInt((entity.message.split(";")[arg]));
        }
        catch(Exception e){
            return -1;
        }
    }

    public void removed(){
        removeComponents();
        if (ds !=null) d2registry.remove(this);
    }

    public class Component {
        public Tile tile, link;
        public Block block;
        public Component(Tile t, Block b){
            tile = t;
            block = b;
        }

        public boolean checkBuild(){ //create a block if it doesn't exist, return false if not valid
            if (!tile.block().equals(block)) {
                if (tile.link().block().getClass().isInstance(block)) return true;
                if (!Build.validPlace(team, tile.x,tile.y,block,(byte)0)) return false;
                Call.onConstructFinish(tile, block , 0, (byte)0, team);
            }
            return true;
        }
    }

    public class DeskSquared{
        int address;
        Tile node;
        DeskSquared(Tile powerTile, int addr, CustomBlock c){
            node = powerTile;
            address = addr;
            d2registry.add(c);
        }

        boolean sameNetwork(DeskSquared ds){
            return (ds.node.entity().power.graph == node.entity().power.graph);
        }

        int readData(int addr){
            for (CustomBlock c : d2registry){
                if ((sameNetwork(c.ds)) && (c.ds.address == addr)) return c.ds.getData();
            }
            return 0;
        }

        void writeData(int addr, int data){
            for (CustomBlock c : d2registry){
                if ((sameNetwork(c.ds)) &&  (c.ds.address == addr)) c.ds.setData(data);
            }
        }

        int getData(){
            return 11111;
        }

        void setData(int d){
        }
    }
}
