package io.anuke.mindustry.world.blocks.logic.CustomBlock;

import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Pos;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.distribution.ItemBridge.ItemBridgeEntity;

class Gate extends CustomBlock {
    Tile node, bridge, bridge2;
    String name = "[accent]Gate; ";
    Block bridgeType = Blocks.itemBridge;

    Gate(Tile tile){
        super(tile);
        addComponent(node = tile.getNearby(2), Blocks.powerNode);
        addComponents(bridgeType, bridge = tile.getNearby(1), bridge2 = tile.getNearby(3));
        Call.setMessageBlockText(null,tile,name +  off);
    }

    @Override
    void logic(){
        if (digitalRead(node)){
            if (((ItemBridgeEntity)bridge2.entity()).link == Pos.invalid) {
                Call.linkItemBridge(null,bridge2,bridge);
                Call.setMessageBlockText(null,tile,name + on);
            }
        }
        else if (((ItemBridgeEntity)bridge2.entity()).link != Pos.invalid){
            Call.unlinkItemBridge(null, bridge2, bridge);
            Call.setMessageBlockText(null, tile, name + off);
        }
    }
}

class LiquidGate extends Gate {
    LiquidGate(Tile tile){
        super(tile);
        name = "[accent]LiquidGate; ";
        bridgeType = Blocks.bridgeConduit;
    }
}