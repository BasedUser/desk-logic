package io.anuke.mindustry.world.blocks.logic.CustomBlock;

import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.distribution.Conveyor.ConveyorEntity;

import static io.anuke.mindustry.Vars.content;

class Scale extends CustomBlock {
    Tile  out, out2, convTile;
    String name = "[accent]Scale;";
    Scale(Tile tile){
        super(tile);
        addComponents(Blocks.powerNode, out = tile.getNearby(2), out2 = tile.getNearby(0));
        addComponent(convTile = tile.getNearby(1) ,Blocks.armoredConveyor);
        Call.setMessageBlockText(null,tile,name);
    }
    @Override
    void logic(){
        state = (((ConveyorEntity)convTile.entity()).getClogHeat()<0.1f);
        digitalWrite(state, out, out2);
        if (state!=prevState) {
            Call.setMessageBlockText(null,tile,name + (state? on:off));
            prevState = state;
        }
    }
}

class LiquidScale extends CustomBlock{
    Tile out, out2, conduit;
    String name = "[accent]LiquidScale;";
    LiquidScale(Tile tile){
        super(tile);
        addComponents(Blocks.powerNode, out = tile.getNearby(2), out2 = tile.getNearby(0));
        addComponent(conduit = tile.getNearby(1),Blocks.pulseConduit);
        Call.setMessageBlockText(null,tile,name);
    }
    @Override
    void logic(){
        digitalWrite((conduit.entity()).liquids.total() < conduit.block().liquidCapacity-0.003f, out, out2);
    }
}

class StorageSensor extends CustomBlock{
    Tile out, storage;
    String name = "[accent]StorageSensor;";
    int readType = 0;
    boolean updateText = false;

    StorageSensor(Tile tile){
        super(tile);
        addComponents(Blocks.powerNode, out = tile.getNearby(2));
        Call.setMessageBlockText(null, tile, name);
        int address = getArgument(1);
        if(address == -1) address = 123;

        ds = new D2(address, this);
        setText();
        storage = findStorage();
    }
    Tile findStorage(){
        Tile t;
        for (int i = 0; i <4; i++){
            t = tile.getNearby(i);
            if (t.entity() != null && t.entity().items != null) {
                return t;
            }
        }
        return null;
    }

    void setText(){
        Item i = content.items().get(readType);
        Call.setMessageBlockText(null,tile,name + " [lime]@" + ds.address +"[#" + i.color.toString()+"] " + readType + " - " + i.name);
    }

    @Override
    void logic(){
        if (updateText){
            setText();
            updateText = false;
        }
    }

    class D2 extends DeskSquared{
        D2(int address, CustomBlock c){
            super(out, address, c);
        }
        @Override
        int getData(){
            storage = findStorage();
            if (storage == null) return 0;
            return storage.entity().items.get(content.item(readType));
        }
        @Override
        void setData(int d){
            readType = Mathf.clamp(d,0,15);
            updateText = true;
        }
    }
}

class PowerSensor extends CustomBlock{
    Tile out, sens;
    String name = "[accent]StorageSensor;";
    int readType = 0;
    boolean updateText = false;
    String powerTypes[] = {"balance","needed","produced","battery","batteryCapacity","totalBatteryCapacity"};

    PowerSensor(Tile tile){
        super(tile);
        addComponents(Blocks.powerNode, out = tile.getNearby(2), sens = tile.getNearby(0));
        Call.setMessageBlockText(null, tile, name);
        int address = getArgument(1);
        if(address == -1) address = 113;

        ds = new D2(address, this);
        setText();
    }

    void setText(){
        Call.setMessageBlockText(null,tile,name + " [lime]@" + ds.address +"[accent] " + readType + " - " + powerTypes[readType]);
    }

    @Override
    void logic(){
        if (updateText){
            setText();
            updateText = false;
        }
    }

    int getReading(){
        switch(readType){
            case 0:
                return (int)(60 * sens.entity().power.graph.getPowerBalance());
            case 1:
                return (int)(60 * sens.entity().power.graph.getPowerNeeded());
            case 2:
                return (int)(60 * sens.entity().power.graph.getPowerProduced());
            case 3:
                return (int)(sens.entity().power.graph.getBatteryStored());
            case 4:
                return (int)(sens.entity().power.graph.getBatteryCapacity());
            case 5:
                return (int)(sens.entity().power.graph.getTotalBatteryCapacity());
        }
        return 0;
    }

    class D2 extends DeskSquared{

        D2(int address, CustomBlock c){
            super(out, address, c);
        }
        @Override
        int getData(){
            return getReading();
        }

        @Override
        void setData(int d){
            readType = Mathf.clamp(d,0,5);
            updateText = true;
        }
    }
}